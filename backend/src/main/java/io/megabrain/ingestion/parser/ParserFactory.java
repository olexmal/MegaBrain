/*
 * Copyright (c) 2025 MegaBrain Contributors
 * Licensed under the MIT License - see LICENSE file for details.
 */

package io.megabrain.ingestion.parser;

/**
 * Factory interface for creating {@link CodeParser} instances.
 * Allows for lazy instantiation and dynamic parser registration.
 */
public interface ParserFactory {

    /**
     * Creates a new instance of the parser.
     *
     * @return a new parser instance
     */
    CodeParser createParser();

    /**
     * Gets the language identifier for parsers created by this factory.
     *
     * @return language code string (e.g., "java", "python")
     */
    String language();
}
