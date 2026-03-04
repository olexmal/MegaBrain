/*
 * Copyright (c) 2025 MegaBrain Contributors
 * Licensed under the MIT License - see LICENSE file for details.
 */

package io.megabrain.api;

/**
 * Marker for SSE stream events in RAG token streaming (US-03-04).
 * Allows emitting token events, cancellation event, or error event when the stream fails.
 */
public sealed interface SseStreamEvent permits TokenStreamEvent, CancelledEvent, ErrorStreamEvent {
}
