/*
 * Copyright (c) 2025 MegaBrain Contributors
 * Licensed under the MIT License - see LICENSE file for details.
 */

package io.megabrain.core;

/**
 * A single citation extracted from LLM answer text (US-03-05 T2).
 * Format in text: [Source: path:line] or [Source: path:lineStart-lineEnd].
 */
public record ExtractedCitation(
        String filePath,
        int lineStart,
        int lineEnd,
        String rawSegment
) {
    /**
     * Single-line citation (lineStart == lineEnd).
     */
    public static ExtractedCitation of(String filePath, int line, String rawSegment) {
        return new ExtractedCitation(filePath, line, line, rawSegment);
    }

    /**
     * Line range citation.
     */
    public static ExtractedCitation of(String filePath, int lineStart, int lineEnd, String rawSegment) {
        return new ExtractedCitation(filePath, lineStart, lineEnd, rawSegment);
    }

    /**
     * Serializes to "path:lineStart" or "path:lineStart-lineEnd" for response sources.
     */
    public String toSourceString() {
        if (lineStart == lineEnd) {
            return filePath + ":" + lineStart;
        }
        return filePath + ":" + lineStart + "-" + lineEnd;
    }
}
