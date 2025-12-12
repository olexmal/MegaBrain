/*
 * Copyright (c) 2025 MegaBrain Contributors
 * Licensed under the MIT License - see LICENSE file for details.
 */

package io.megabrain.ingestion;

/**
 * Progress event emitted during repository operations.
 *
 * @param message a descriptive message
 * @param progress the completion progress (0.0-100.0)
 */
public record ProgressEvent(
        String message,
        double progress
) {
    /**
     * Creates a ProgressEvent with a message and progress percentage.
     */
    public static ProgressEvent of(String message, double progress) {
        return new ProgressEvent(message, progress);
    }

    /**
     * Converts this event to a JSON string for SSE transmission.
     */
    public String toJson() {
        return String.format("{\"message\": \"%s\", \"progress\": %.1f}",
                message.replace("\"", "\\\""), // Escape quotes
                progress);
    }
}

