/*
 * Copyright (c) 2025 MegaBrain Contributors
 * Licensed under the MIT License - see LICENSE file for details.
 */

package io.megabrain.ingestion;

/**
 * Progress event emitted during repository operations.
 *
 * @param stage the current stage (e.g., "CLONING", "EXTRACTING", "COMPLETE")
 * @param message a descriptive message
 * @param percentage the completion percentage (0-100)
 */
public record ProgressEvent(
        String stage,
        String message,
        int percentage
) {
    public static ProgressEvent of(String stage, String message, int percentage) {
        return new ProgressEvent(stage, message, percentage);
    }
}

