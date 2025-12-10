/*
 * Copyright (c) 2025 MegaBrain Contributors
 * Licensed under the MIT License - see LICENSE file for details.
 */

package io.megabrain.ingestion.parser.java;

import com.github.javaparser.Position;
import com.github.javaparser.Range;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SourceCoordinatesTest {

    @Test
    void computesByteOffsetsAndSlices() {
        String source = "abc\n"
                + "def\n"
                + "ghi";

        SourceCoordinates coordinates = new SourceCoordinates(source);

        assertThat(coordinates.toByteOffset(new Position(1, 1))).isZero();      // 'a'
        assertThat(coordinates.toByteOffset(new Position(2, 2))).isEqualTo(5);      // 'e'
        assertThat(coordinates.toByteOffset(new Position(3, 3))).isEqualTo(10);     // 'h'

        Range secondLineRange = new Range(new Position(2, 1), new Position(2, 2));  // "de"
        assertThat(coordinates.slice(secondLineRange, source)).isEqualTo("de");
    }
}

