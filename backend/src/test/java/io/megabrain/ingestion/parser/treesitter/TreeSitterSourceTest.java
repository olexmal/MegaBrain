/*
 * Copyright (c) 2025 MegaBrain Contributors
 * Licensed under the MIT License - see LICENSE file for details.
 */

package io.megabrain.ingestion.parser.treesitter;

import io.github.treesitter.jtreesitter.Point;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class TreeSitterSourceTest {

    @Test
    void convertsPointToOneBasedLineNumber() {
        TreeSitterSource source = new TreeSitterSource("line1\nline2", Path.of("Sample.test"), StandardCharsets.UTF_8);

        assertThat(source.toLineNumber(new Point(0, 0))).isOne();
        assertThat(source.toLineNumber(new Point(3, 5))).isEqualTo(4);
    }

    @Test
    void slicesBytesSafelyWithinBounds() {
        String content = "alpha\nbeta\ngamma";
        TreeSitterSource source = new TreeSitterSource(content, Path.of("Sample.test"), StandardCharsets.UTF_8);

        assertThat(source.slice(0, 5)).isEqualTo("alpha");
        assertThat(source.slice(6, 10)).isEqualTo("beta");
        // Gracefully clamp beyond bounds
        assertThat(source.slice(-10, 1)).isEqualTo("a");
        assertThat(source.slice(content.getBytes(StandardCharsets.UTF_8).length, 999)).isEmpty();
    }
}

