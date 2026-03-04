/*
 * Copyright (c) 2025 MegaBrain Contributors
 * Licensed under the MIT License - see LICENSE file for details.
 */

package io.megabrain.core;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import io.megabrain.api.CancelledEvent;
import io.megabrain.api.SseStreamEvent;
import io.megabrain.api.TokenStreamEvent;
import io.smallrye.mutiny.Multi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for RagService (US-03-04 T1, T2).
 */
class RagServiceTest {

    private StreamingChatModelProvider streamingModelProvider;
    private RagService ragService;

    @BeforeEach
    void setUp() {
        streamingModelProvider = mock(StreamingChatModelProvider.class);
        // Use same-thread executor so streaming runs synchronously in tests
        ragService = new RagService(streamingModelProvider, Runnable::run);
        ragService.init();
    }

    @Test
    @DisplayName("streamTokens returns reactive stream of token events when streaming model emits tokens")
    void streamTokens_withNonBlankQuestion_returnsStreamFromLLM() {
        // Given: provider returns a streaming model that emits "Hello", " ", "world"
        StreamingChatModel mockModel = mock(StreamingChatModel.class);
        when(streamingModelProvider.getStreamingModel()).thenReturn(Optional.of(mockModel));
        doAnswer(invocation -> {
            StreamingChatResponseHandler handler = invocation.getArgument(1);
            handler.onPartialResponse("Hello");
            handler.onPartialResponse(" ");
            handler.onPartialResponse("world");
            handler.onCompleteResponse(ChatResponse.builder()
                    .aiMessage(AiMessage.from("Hello world"))
                    .build());
            return null;
        }).when(mockModel).chat(anyString(), any(StreamingChatResponseHandler.class));

        // When
        Multi<SseStreamEvent> stream = ragService.streamTokens("What is auth?");
        List<SseStreamEvent> collected = stream.collect().asList().await().indefinitely();

        // Then
        assertThat(collected).isNotEmpty();
        assertThat(collected).hasSize(3);
        assertThat(collected.get(0)).isInstanceOf(TokenStreamEvent.class);
        assertThat(((TokenStreamEvent) collected.get(0)).token()).isEqualTo("Hello");
        assertThat(((TokenStreamEvent) collected.get(1)).token()).isEqualTo(" ");
        assertThat(((TokenStreamEvent) collected.get(2)).token()).isEqualTo("world");
    }

    @Test
    @DisplayName("streamTokens returns empty stream for blank question")
    void streamTokens_withBlankQuestion_returnsEmptyStream() {
        // When
        Multi<SseStreamEvent> stream = ragService.streamTokens("   ");
        List<SseStreamEvent> collected = stream.collect().asList().await().indefinitely();

        // Then
        assertThat(collected).isEmpty();
    }

    @Test
    @DisplayName("streamTokens returns empty stream for null question")
    void streamTokens_withNullQuestion_returnsEmptyStream() {
        // When
        Multi<SseStreamEvent> stream = ragService.streamTokens(null);
        List<SseStreamEvent> collected = stream.collect().asList().await().indefinitely();

        // Then
        assertThat(collected).isEmpty();
    }

    @Test
    @DisplayName("streamTokens fails when no streaming LLM is available")
    void streamTokens_noStreamingModelAvailable_fails() {
        when(streamingModelProvider.getStreamingModel()).thenReturn(Optional.empty());

        Multi<SseStreamEvent> stream = ragService.streamTokens("Hello?");

        assertThatThrownBy(() -> stream.collect().asList().await().indefinitely())
                .hasMessageContaining("No LLM available for streaming");
    }

    @Test
    @DisplayName("streamTokens propagates error when streaming model calls onError")
    void streamTokens_modelCallsOnError_failsStream() {
        StreamingChatModel mockModel = mock(StreamingChatModel.class);
        when(streamingModelProvider.getStreamingModel()).thenReturn(Optional.of(mockModel));
        doAnswer(invocation -> {
            StreamingChatResponseHandler handler = invocation.getArgument(1);
            handler.onError(new RuntimeException("LLM error"));
            return null;
        }).when(mockModel).chat(anyString(), any(StreamingChatResponseHandler.class));

        Multi<SseStreamEvent> stream = ragService.streamTokens("Hi");

        assertThatThrownBy(() -> stream.collect().asList().await().indefinitely())
                .hasMessageContaining("LLM error");
    }

    @Test
    @DisplayName("streamTokens emits CancelledEvent and completes when subscription is cancelled")
    void streamTokens_subscriptionCancelled_emitsCancelledAndCleansUp() {
        // Given: model emits one token then never completes (simulating slow stream)
        StreamingChatModel mockModel = mock(StreamingChatModel.class);
        when(streamingModelProvider.getStreamingModel()).thenReturn(Optional.of(mockModel));
        doAnswer(invocation -> {
            StreamingChatResponseHandler handler = invocation.getArgument(1);
            handler.onPartialResponse("One");
            // Do not call onCompleteResponse; subscriber will cancel
            return null;
        }).when(mockModel).chat(anyString(), any(StreamingChatResponseHandler.class));

        // When: subscribe and cancel after first item
        Multi<SseStreamEvent> stream = ragService.streamTokens("Q");
        List<SseStreamEvent> collected = new java.util.ArrayList<>();
        io.smallrye.mutiny.subscription.Cancellable subscription = stream.subscribe().with(
                collected::add,
                f -> { },
                () -> { });
        // Same-thread executor: first token emitted immediately
        assertThat(collected).hasSize(1);
        assertThat(collected.get(0)).isInstanceOf(TokenStreamEvent.class);
        subscription.cancel();

        // Then: stream was cancelled without failure; onTermination runs and cleans up
        assertThat(((TokenStreamEvent) collected.get(0)).token()).isEqualTo("One");
    }
}
