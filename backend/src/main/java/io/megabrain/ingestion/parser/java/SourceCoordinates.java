/*
 * Copyright (c) 2025 MegaBrain Contributors
 * Licensed under the MIT License - see LICENSE file for details.
 */

package io.megabrain.ingestion.parser.java;

import com.github.javaparser.Position;
import com.github.javaparser.Range;

import java.nio.charset.StandardCharsets;

final class SourceCoordinates {

    private final String[] lines;
    private final int[] lineStartBytes;
    private final int[] lineStartChars;

    SourceCoordinates(String source) {
        this.lines = source.split("\n", -1);
        this.lineStartBytes = new int[lines.length];
        this.lineStartChars = new int[lines.length];

        int byteCursor = 0;
        int charCursor = 0;
        for (int i = 0; i < lines.length; i++) {
            lineStartBytes[i] = byteCursor;
            lineStartChars[i] = charCursor;

            byteCursor += lines[i].getBytes(StandardCharsets.UTF_8).length;
            charCursor += lines[i].length();

            if (i < lines.length - 1) {
                byteCursor += 1; // account for '\n'
                charCursor += 1;
            }
        }
    }

    int toByteOffset(Position position) {
        int lineIndex = clampLine(position.line);
        int column = Math.max(position.column, 1);
        String line = lines[lineIndex];
        int safeColumn = Math.min(column - 1, line.length());
        int prefixBytes = line.substring(0, safeColumn).getBytes(StandardCharsets.UTF_8).length;
        return lineStartBytes[lineIndex] + prefixBytes;
    }

    int toCharOffset(Position position) {
        int lineIndex = clampLine(position.line);
        int column = Math.max(position.column, 1);
        String line = lines[lineIndex];
        int safeColumn = Math.min(column - 1, line.length());
        return lineStartChars[lineIndex] + safeColumn;
    }

    String slice(Range range, String source) {
        int start = toCharOffset(range.begin);
        int endExclusive = Math.min(source.length(), toCharOffset(range.end) + 1);
        if (start >= endExclusive || start < 0 || endExclusive > source.length()) {
            return "";
        }
        return source.substring(start, endExclusive);
    }

    private int clampLine(int line) {
        if (lines.length == 0) {
            return 0;
        }
        return Math.min(Math.max(line - 1, 0), lines.length - 1);
    }
}

