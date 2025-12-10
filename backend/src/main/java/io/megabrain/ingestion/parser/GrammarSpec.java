/*
 * Copyright (c) 2025 MegaBrain Contributors
 * Licensed under the MIT License - see LICENSE file for details.
 */

package io.megabrain.ingestion.parser;

import java.util.Objects;

/**
 * Immutable metadata describing how to load a Tree-sitter grammar.
 */
public record GrammarSpec(
        String language,
        String symbol,
        String libraryName,
        String propertyKey,
        String envKey,
        String repository,
        String version
) {
    public GrammarSpec {
        Objects.requireNonNull(language, "language");
        Objects.requireNonNull(symbol, "symbol");
        Objects.requireNonNull(libraryName, "libraryName");
        Objects.requireNonNull(propertyKey, "propertyKey");
        Objects.requireNonNull(envKey, "envKey");
        Objects.requireNonNull(repository, "repository");
        Objects.requireNonNull(version, "version");
    }
}

