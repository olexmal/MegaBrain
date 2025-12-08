/*
 * Copyright (c) 2025 MegaBrain Contributors
 * Licensed under the MIT License - see LICENSE file for details.
 */

package io.megabrain.ingestion.parser;

import java.util.Map;
import java.util.Objects;

/**
 * Immutable representation of a parsed code fragment with metadata.
 *
 * @param content     the raw source content for this chunk
 * @param language    language identifier (e.g., "java")
 * @param entityType  entity type (e.g., class, method, field)
 * @param entityName  fully qualified entity name (includes nesting/package)
 * @param sourceFile  path of the source file that produced this chunk
 * @param startLine   starting line number (1-based, inclusive)
 * @param endLine     ending line number (1-based, inclusive)
 * @param startByte   starting byte offset in the file (0-based, inclusive)
 * @param endByte     ending byte offset in the file (0-based, exclusive)
 * @param attributes  additional metadata such as modifiers or signatures
 */
public record TextChunk(
        String content,
        String language,
        String entityType,
        String entityName,
        String sourceFile,
        int startLine,
        int endLine,
        int startByte,
        int endByte,
        Map<String, String> attributes
) {

    public TextChunk {
        Objects.requireNonNull(content, "content must not be null");
        Objects.requireNonNull(language, "language must not be null");
        Objects.requireNonNull(entityType, "entityType must not be null");
        Objects.requireNonNull(entityName, "entityName must not be null");
        Objects.requireNonNull(sourceFile, "sourceFile must not be null");
        attributes = attributes == null ? Map.of() : Map.copyOf(attributes);
        if (startLine < 1 || endLine < startLine) {
            throw new IllegalArgumentException("Line numbers must be positive and endLine >= startLine");
        }
        if (startByte < 0 || endByte < startByte) {
            throw new IllegalArgumentException("Byte offsets must be non-negative and endByte >= startByte");
        }
    }
}

