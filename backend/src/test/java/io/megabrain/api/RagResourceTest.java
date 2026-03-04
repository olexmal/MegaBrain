/*
 * Copyright (c) 2025 MegaBrain Contributors
 * Licensed under the MIT License - see LICENSE file for details.
 */

package io.megabrain.api;

import io.megabrain.core.RagService;
import io.smallrye.mutiny.Multi;
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
        Multi<String> sseStream = ragResource.stream(request);
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
        ragResource.stream(request).collect().asList().await().indefinitely();

        // Then
        verify(ragService).streamTokens("hello");
    }
}
