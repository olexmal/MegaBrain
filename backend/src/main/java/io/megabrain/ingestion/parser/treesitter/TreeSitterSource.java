/*
 * Copyright (c) 2025 MegaBrain Contributors
 * Licensed under the MIT License - see LICENSE file for details.
 */

package io.megabrain.ingestion.parser.treesitter;

import io.github.treesitter.jtreesitter.Point;

import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.Objects;

final class TreeSitterSource {

    private final String source;
    private final Path filePath;
    private final Charset charset;
    private final byte[] bytes;

    TreeSitterSource(String source, Path filePath, Charset charset) {
        this.source = Objects.requireNonNull(source, "source");
        this.filePath = Objects.requireNonNull(filePath, "filePath");
        this.charset = Objects.requireNonNull(charset, "charset");
        this.bytes = this.source.getBytes(this.charset);
    }

    String source() {
        return source;
    }

    Path filePath() {
        return filePath;
    }

    Charset charset() {
        return charset;
    }

    int toLineNumber(Point point) {
        return point.row() + 1;
    }

    String slice(int startByte, int endByte) {
        int safeStart = Math.max(0, Math.min(startByte, bytes.length));
        int safeEnd = Math.max(safeStart, Math.min(endByte, bytes.length));
        return new String(bytes, safeStart, safeEnd - safeStart, charset);
    }
}

