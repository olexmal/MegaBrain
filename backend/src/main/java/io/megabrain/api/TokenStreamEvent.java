/*
 * Copyright (c) 2025 MegaBrain Contributors
 * Licensed under the MIT License - see LICENSE file for details.
 */

package io.megabrain.api;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * SSE event payload for a single token in RAG token streaming (US-03-04).
 * Emitted as {@code event: token} with {@code data: {"token": "..."}}.
 */
public record TokenStreamEvent(
        @JsonProperty("token") String token
) {
    public TokenStreamEvent {
        token = token != null ? token : "";
    }
}
