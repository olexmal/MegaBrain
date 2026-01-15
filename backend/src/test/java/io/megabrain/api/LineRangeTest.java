/*
 * Copyright (c) 2025 MegaBrain Contributors
 * Licensed under the MIT License - see LICENSE file for details.
 */

package io.megabrain.api;

import com.fasterxml.jackson.databind.ObjectMapper;
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
    void shouldCreateValidLineRange() {
        // Given
        int startLine = 10;
        int endLine = 15;

        // When
        LineRange range = new LineRange(startLine, endLine);

        // Then
        assertThat(range.getStartLine()).isEqualTo(startLine);
        assertThat(range.getEndLine()).isEqualTo(endLine);
        assertThat(range.getLineCount()).isEqualTo(6); // 15 - 10 + 1
    }

    @Test
    void shouldCreateSingleLineRange() {
        // Given
        int line = 42;

        // When
        LineRange range = new LineRange(line, line);

        // Then
        assertThat(range.getStartLine()).isEqualTo(line);
        assertThat(range.getEndLine()).isEqualTo(line);
        assertThat(range.getLineCount()).isEqualTo(1);
    }

    @Test
    void shouldRejectInvalidStartLine() {
        assertThatThrownBy(() -> new LineRange(0, 5))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage(POSITIVE_LINE_NUMBERS_MESSAGE);

        assertThatThrownBy(() -> new LineRange(-1, 5))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage(POSITIVE_LINE_NUMBERS_MESSAGE);
    }

    @Test
    void shouldRejectInvalidEndLine() {
        assertThatThrownBy(() -> new LineRange(5, 0))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage(POSITIVE_LINE_NUMBERS_MESSAGE);

        assertThatThrownBy(() -> new LineRange(5, -1))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage(POSITIVE_LINE_NUMBERS_MESSAGE);
    }

    @Test
    void shouldRejectStartGreaterThanEnd() {
        assertThatThrownBy(() -> new LineRange(10, 5))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage(START_GREATER_THAN_END_MESSAGE);
    }

    @Test
    void shouldImplementEqualsAndHashCode() {
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
    void shouldSerializeToJson() throws Exception {
        // Given
        LineRange range = new LineRange(100, 150);

        // When
        String json = objectMapper.writeValueAsString(range);

        // Then
        assertThat(json).isEqualTo("{\"start\":100,\"end\":150}");
    }

    @Test
    void shouldDeserializeFromJson() throws Exception {
        // Given
        String json = "{\"start\":25,\"end\":30}";

        // When
        LineRange range = objectMapper.readValue(json, LineRange.class);

        // Then
        assertThat(range.getStartLine()).isEqualTo(25);
        assertThat(range.getEndLine()).isEqualTo(30);
        assertThat(range.getLineCount()).isEqualTo(6);
    }

    @Test
    void toStringShouldReturnRangeFormat() {
        // Given
        LineRange range = new LineRange(5, 12);

        // When
        String string = range.toString();

        // Then
        assertThat(string).isEqualTo("5-12");
    }
}