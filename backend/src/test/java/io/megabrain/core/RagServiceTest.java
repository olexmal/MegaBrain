/*
 * Copyright (c) 2025 MegaBrain Contributors
 * Licensed under the MIT License - see LICENSE file for details.
 */

package io.megabrain.core;

import io.megabrain.api.TokenStreamEvent;
import io.smallrye.mutiny.Multi;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for RagService (US-03-04 T1).
 */
class RagServiceTest {

    private final RagService ragService = new RagService();

    @Test
    @DisplayName("streamTokens returns reactive stream of token events for non-blank question")
    void streamTokens_withNonBlankQuestion_returnsNonEmptyStream() {
        // When
        Multi<TokenStreamEvent> stream = ragService.streamTokens("What is auth?");
        List<TokenStreamEvent> collected = stream.collect().asList().await().indefinitely();

        // Then
        assertThat(collected).isNotEmpty();
        assertThat(collected).allMatch(e -> e.token() != null);
        assertThat(collected.get(0).token()).isEqualTo("Streaming");
    }

    @Test
    @DisplayName("streamTokens returns empty stream for blank question")
    void streamTokens_withBlankQuestion_returnsEmptyStream() {
        // When
        Multi<TokenStreamEvent> stream = ragService.streamTokens("   ");
        List<TokenStreamEvent> collected = stream.collect().asList().await().indefinitely();

        // Then
        assertThat(collected).isEmpty();
    }

    @Test
    @DisplayName("streamTokens returns empty stream for null question")
    void streamTokens_withNullQuestion_returnsEmptyStream() {
        // When
        Multi<TokenStreamEvent> stream = ragService.streamTokens(null);
        List<TokenStreamEvent> collected = stream.collect().asList().await().indefinitely();

        // Then
        assertThat(collected).isEmpty();
    }
}
