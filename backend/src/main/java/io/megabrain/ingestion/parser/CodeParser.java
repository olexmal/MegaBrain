/*
 * Copyright (c) 2025 MegaBrain Contributors
 * Licensed under the MIT License - see LICENSE file for details.
 */

package io.megabrain.ingestion.parser;

import java.nio.file.Path;
import java.util.List;

/**
 * Generic interface for language-specific code parsers.
 * Implementations convert source files into structured {@link TextChunk} records.
 */
public interface CodeParser {

    /**
     * Checks whether this parser supports the provided file path.
     *
     * @param filePath the path to the source file
     * @return true if the parser can handle the file, false otherwise
     */
    boolean supports(Path filePath);

    /**
     * Parse the given file into structured text chunks.
     * Implementations should be resilient: parsing errors should be logged and
     * represented gracefully rather than throwing unless the input is invalid.
     *
     * @param filePath path to the source file
     * @return list of extracted chunks (possibly empty if parsing failed)
     */
    List<TextChunk> parse(Path filePath);

    /**
     * Language identifier for this parser (e.g., "java", "python").
     *
     * @return language code string
     */
    String language();
}

