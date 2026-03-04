/*
 * Copyright (c) 2025 MegaBrain Contributors
 * Licensed under the MIT License - see LICENSE file for details.
 */

package io.megabrain.api;

/**
 * Marker for SSE stream events in RAG token streaming (US-03-04).
 * Allows emitting token events or a cancellation event when the client disconnects.
 */
public sealed interface SseStreamEvent permits TokenStreamEvent, CancelledEvent {
}
