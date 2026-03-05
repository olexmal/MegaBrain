/*
 * Copyright (c) 2025 MegaBrain Contributors
 * Licensed under the MIT License - see LICENSE file for details.
 */

package io.megabrain.api;

import io.megabrain.core.RagService;
import io.megabrain.api.CancelledEvent;
import io.megabrain.api.ErrorStreamEvent;
import io.megabrain.api.RagResponse;
import io.megabrain.api.SseStreamEvent;
import io.megabrain.api.TokenStreamEvent;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for RagResource SSE endpoint (US-03-04 T1).
 */
@ExtendWith(MockitoExtension.class)
class RagResourceTest {

    @Mock
    RagService ragService;

    @InjectMocks
    RagResource ragResource;

    @Test
    @DisplayName("stream returns SSE lines with event token and data payload")
    void stream_withValidRequest_returnsSseFormattedLines() {
        // Given
        RagRequest request = new RagRequest("Explain caching");
        when(ragService.streamTokens(anyString())).thenReturn(
                Multi.createFrom().items(
                        new TokenStreamEvent("The"),
                        new TokenStreamEvent(" answer")
                )
        );

        // When
        Multi<String> sseStream = (Multi<String>) ragResource.rag(request, true);
        List<String> lines = sseStream.collect().asList().await().indefinitely();

        // Then
        assertThat(lines).hasSize(2);
        assertThat(lines.get(0)).startsWith("event: token\n").contains("data: ");
        assertThat(lines.get(0)).contains("\"token\":\"The\"");
        assertThat(lines.get(1)).contains("\"token\":\" answer\"");
        verify(ragService).streamTokens("Explain caching");
    }

    @Test
    @DisplayName("stream uses trimmed question")
    void stream_withRequest_usesTrimmedQuestion() {
        // Given
        RagRequest request = new RagRequest("  hello  ");
        when(ragService.streamTokens(anyString())).thenReturn(Multi.createFrom().empty());

        // When
        ((Multi<String>) ragResource.rag(request, true)).collect().asList().await().indefinitely();

        // Then
        verify(ragService).streamTokens("hello");
    }

    @Test
    @DisplayName("stream formats cancelled event as event: cancelled")
    void stream_withCancelledEvent_returnsCancelledSseLine() {
        // Given: service returns token then cancelled
        RagRequest request = new RagRequest("Stop");
        when(ragService.streamTokens(anyString())).thenReturn(
                Multi.createFrom().items(
                        new TokenStreamEvent("Partial"),
                        new CancelledEvent()
                )
        );

        // When
        Multi<String> sseStream = (Multi<String>) ragResource.rag(request, true);
        List<String> lines = sseStream.collect().asList().await().indefinitely();

        // Then
        assertThat(lines).hasSize(2);
        assertThat(lines.get(0)).startsWith("event: token\n").contains("\"token\":\"Partial\"");
        assertThat(lines.get(1)).isEqualTo("event: cancelled\ndata: {}\n\n");
        verify(ragService).streamTokens("Stop");
    }

    @Test
    @DisplayName("stream formats error event as event: error with message and code")
    void stream_withErrorEvent_returnsErrorSseLine() {
        RagRequest request = new RagRequest("Fail");
        when(ragService.streamTokens(anyString())).thenReturn(
                Multi.createFrom().items(
                        new ErrorStreamEvent("Something went wrong", "LLM_ERROR")
                )
        );

        Multi<String> sseStream = (Multi<String>) ragResource.rag(request, true);
        List<String> lines = sseStream.collect().asList().await().indefinitely();

        assertThat(lines).hasSize(1);
        assertThat(lines.get(0)).startsWith("event: error\n").contains("data: ");
        assertThat(lines.get(0)).contains("\"message\":\"Something went wrong\"").contains("\"code\":\"LLM_ERROR\"");
        assertThat(lines.get(0)).endsWith("\n\n");
        verify(ragService).streamTokens("Fail");
    }

    @Test
    @DisplayName("stream recovers from failure with error event containing message and code")
    void stream_whenServiceFails_returnsErrorSseLineAndCompletes() {
        RagRequest request = new RagRequest("x");
        when(ragService.streamTokens(anyString())).thenReturn(
                Multi.createFrom().failure(new RuntimeException("Backend error"))
        );

        Multi<String> sseStream = (Multi<String>) ragResource.rag(request, true);
        List<String> lines = sseStream.collect().asList().await().indefinitely();

        assertThat(lines).hasSize(1);
        assertThat(lines.get(0)).startsWith("event: error\n").contains("data: ");
        assertThat(lines.get(0)).contains("\"message\":\"Backend error\"").contains("\"code\":\"STREAM_ERROR\"");
        assertThat(lines.get(0)).endsWith("\n\n");
    }

    @Test
    @DisplayName("rag with stream=false returns complete RagResponse as JSON")
    void rag_streamFalse_returnsCompleteResponse() {
        RagRequest request = new RagRequest("What is auth?");
        RagResponse response = RagResponse.of("Authentication is the process of verifying identity.");
        when(ragService.ask(anyString())).thenReturn(Uni.createFrom().item(response));

        Object result = ragResource.rag(request, false);
        Response res = ((Uni<Response>) result).await().indefinitely();

        assertThat(res.getStatus()).isEqualTo(200);
        assertThat(res.getEntity()).isInstanceOf(RagResponse.class);
        RagResponse body = (RagResponse) res.getEntity();
        assertThat(body.answer()).isEqualTo("Authentication is the process of verifying identity.");
        assertThat(body.sources()).isEmpty();
        verify(ragService).ask("What is auth?");
    }

    @Test
    @DisplayName("rag with stream=false uses trimmed question")
    void rag_streamFalse_usesTrimmedQuestion() {
        RagRequest request = new RagRequest("  hello  ");
        when(ragService.ask(anyString())).thenReturn(Uni.createFrom().item(RagResponse.of("Hi")));

        Object result = ragResource.rag(request, false);
        ((Uni<?>) result).await().indefinitely();

        verify(ragService).ask("hello");
    }

    @Test
    @DisplayName("rag with stream=false recovers from service failure with 503 and safe error body")
    void rag_streamFalse_whenAskFails_returns503WithErrorBody() {
        RagRequest request = new RagRequest("Fail");
        when(ragService.ask(anyString())).thenReturn(
                Uni.createFrom().failure(new RuntimeException("LLM unavailable")));

        Object result = ragResource.rag(request, false);
        Response res = ((Uni<Response>) result).await().indefinitely();

        assertThat(res.getStatus()).isEqualTo(503);
        assertThat(res.getEntity()).isNotNull();
        assertThat(res.getEntity().toString()).contains("RAG request failed");
        verify(ragService).ask("Fail");
    }
}
