/*
 * Copyright (c) 2025 MegaBrain Contributors
 * Licensed under the MIT License - see LICENSE file for details.
 */

package io.megabrain.ingestion.parser;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithName;
import java.util.Map;
import java.util.Optional;

/**
 * Configuration for Tree-sitter grammar version pinning.
 * Supports both global default versions and per-language overrides.
 */
@ConfigMapping(prefix = "megabrain.grammars")
public interface GrammarConfig {

    /**
     * Default grammar version to use for all languages when no specific version is configured.
     * If not specified, uses the version defined in the GrammarSpec.
     */
    Optional<String> defaultVersion();

    /**
     * Per-language version overrides.
     * Format: language-name -> version-string
     */
    @WithName("versions")
    Map<String, String> languageVersions();

    /**
     * Get the effective version for a language, considering both default and language-specific overrides.
     *
     * @param language the language name
     * @param defaultSpecVersion the default version from the GrammarSpec
     * @return the effective version to use
     */
    default String getEffectiveVersion(String language, String defaultSpecVersion) {
        // Check for language-specific override first
        String languageSpecificVersion = languageVersions().get(language);
        if (languageSpecificVersion != null && !languageSpecificVersion.trim().isEmpty()) {
            return languageSpecificVersion.trim();
        }

        // Fall back to global default if configured
        if (defaultVersion().isPresent()) {
            return defaultVersion().get();
        }

        // Fall back to the version from GrammarSpec
        return defaultSpecVersion;
    }
}
