/*
 * Copyright (c) 2025 MegaBrain Contributors
 * Licensed under the MIT License - see LICENSE file for details.
 */

package io.megabrain.api;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents a range of lines in a source file.
 *
 * This class encapsulates the start and end line numbers for a code entity,
 * providing a convenient way to represent line ranges in search results.
 */
public class LineRange {

    @JsonProperty("start")
    private final int startLine;

    @JsonProperty("end")
    private final int endLine;

    // Default constructor for Jackson deserialization
    public LineRange() {
        this.startLine = 1;
        this.endLine = 1;
    }

    /**
     * Creates a new LineRange.
     *
     * @param startLine the starting line number (1-based)
     * @param endLine the ending line number (1-based, inclusive)
     */
    public LineRange(int startLine, int endLine) {
        if (startLine < 1 || endLine < 1) {
            throw new IllegalArgumentException("Line numbers must be positive");
        }
        if (startLine > endLine) {
            throw new IllegalArgumentException("Start line cannot be greater than end line");
        }
        this.startLine = startLine;
        this.endLine = endLine;
    }

    public int getStartLine() {
        return startLine;
    }

    public int getEndLine() {
        return endLine;
    }

    @JsonIgnore
    public int getLineCount() {
        return endLine - startLine + 1;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        LineRange lineRange = (LineRange) obj;
        return startLine == lineRange.startLine && endLine == lineRange.endLine;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(startLine) * 31 + Integer.hashCode(endLine);
    }

    @Override
    public String toString() {
        return startLine + "-" + endLine;
    }
}