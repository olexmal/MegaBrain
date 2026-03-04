/*
 * Copyright (c) 2025 MegaBrain Contributors
 * Licensed under the MIT License - see LICENSE file for details.
 */

package io.megabrain.api;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * SSE event emitted when the RAG token stream fails (US-03-04 T4).
 * Sent as {@code event: error} with {@code data: {"message": "...", "code": "..."}}
 * so the client can handle failures. Stream is closed after this event.
 * Message and code must not contain sensitive data (per security guidelines).
 */
public record ErrorStreamEvent(
        @JsonProperty("message") String message,
        @JsonProperty("code") String code
) implements SseStreamEvent {
    public ErrorStreamEvent {
        message = message != null ? message : "An error occurred";
        code = code != null ? code : "STREAM_ERROR";
    }
}
