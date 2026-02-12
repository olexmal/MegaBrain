/*
 * Copyright (c) 2025 MegaBrain Contributors
 * Licensed under the MIT License - see LICENSE file for details.
 */

package io.megabrain.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for LineRange class.
 */
class LineRangeTest {

    private static final String POSITIVE_LINE_NUMBERS_MESSAGE = "Line numbers must be positive";
    private static final String START_GREATER_THAN_END_MESSAGE = "Start line cannot be greater than end line";

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("creates range with start, end and correct line count")
    void constructor_validRange_setsStartEndAndLineCount() {
        // Given
        int startLine = 10;
        int endLine = 15;

        // When
        LineRange actual = new LineRange(startLine, endLine);

        // Then
        assertThat(actual.getStartLine()).isEqualTo(startLine);
        assertThat(actual.getEndLine()).isEqualTo(endLine);
        assertThat(actual.getLineCount()).isEqualTo(6);
    }

    @Test
    @DisplayName("creates single-line range")
    void constructor_sameStartAndEnd_setsLineCountOne() {
        // Given
        int line = 42;

        // When
        LineRange actual = new LineRange(line, line);

        // Then
        assertThat(actual.getStartLine()).isEqualTo(line);
        assertThat(actual.getEndLine()).isEqualTo(line);
        assertThat(actual.getLineCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("throws when start line is zero")
    void constructor_startLineZero_throwsIllegalArgument() {
        // When/Then
        assertThatThrownBy(() -> new LineRange(0, 5))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage(POSITIVE_LINE_NUMBERS_MESSAGE);
    }

    @Test
    @DisplayName("throws when start line is negative")
    void constructor_startLineNegative_throwsIllegalArgument() {
        // When/Then
        assertThatThrownBy(() -> new LineRange(-1, 5))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage(POSITIVE_LINE_NUMBERS_MESSAGE);
    }

    @Test
    @DisplayName("throws when end line is zero")
    void constructor_endLineZero_throwsIllegalArgument() {
        // When/Then
        assertThatThrownBy(() -> new LineRange(5, 0))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage(POSITIVE_LINE_NUMBERS_MESSAGE);
    }

    @Test
    @DisplayName("throws when end line is negative")
    void constructor_endLineNegative_throwsIllegalArgument() {
        // When/Then
        assertThatThrownBy(() -> new LineRange(5, -1))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage(POSITIVE_LINE_NUMBERS_MESSAGE);
    }

    @Test
    @DisplayName("throws when start greater than end")
    void constructor_startGreaterThanEnd_throwsIllegalArgument() {
        // When/Then
        assertThatThrownBy(() -> new LineRange(10, 5))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage(START_GREATER_THAN_END_MESSAGE);
    }

    @Test
    @DisplayName("equals and hashCode consistent for same range")
    void equals_sameStartEnd_returnsTrueAndSameHashCode() {
        // Given
        LineRange range1 = new LineRange(10, 20);
        LineRange range2 = new LineRange(10, 20);
        LineRange range3 = new LineRange(15, 25);

        // Then
        assertThat(range1).isEqualTo(range2);
        assertThat(range1).isNotEqualTo(range3);
        assertThat(range1.hashCode()).isEqualTo(range2.hashCode());
        assertThat(range1.hashCode()).isNotEqualTo(range3.hashCode());
    }

    @Test
    @DisplayName("serializes to JSON with start and end")
    void writeValueAsString_serializesToJson() throws Exception {
        // Given
        LineRange range = new LineRange(100, 150);

        // When
        String actual = objectMapper.writeValueAsString(range);

        // Then
        String expected = "{\"start\":100,\"end\":150}";
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    @DisplayName("deserializes from JSON")
    void readValue_deserializesFromJson() throws Exception {
        // Given
        String json = "{\"start\":25,\"end\":30}";

        // When
        LineRange actual = objectMapper.readValue(json, LineRange.class);

        // Then
        assertThat(actual.getStartLine()).isEqualTo(25);
        assertThat(actual.getEndLine()).isEqualTo(30);
        assertThat(actual.getLineCount()).isEqualTo(6);
    }

    @Test
    @DisplayName("toString returns start-end format")
    void toString_returnsRangeFormat() {
        // Given
        LineRange range = new LineRange(5, 12);

        // When
        String actual = range.toString();

        // Then
        assertThat(actual).isEqualTo("5-12");
    }
}