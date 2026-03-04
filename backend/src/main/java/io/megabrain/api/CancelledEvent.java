/*
 * Copyright (c) 2025 MegaBrain Contributors
 * Licensed under the MIT License - see LICENSE file for details.
 */

package io.megabrain.api;

/**
 * SSE event emitted when the RAG token stream is cancelled (US-03-04 T3).
 * Sent as {@code event: cancelled} so the client knows generation was stopped.
 */
public record CancelledEvent() implements SseStreamEvent {
}
