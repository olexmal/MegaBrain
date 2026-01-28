/*
 * Copyright (c) 2025 MegaBrain Contributors
 * Licensed under the MIT License - see LICENSE file for details.
 */

package io.megabrain.core;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

/**
 * Configuration for search field boost values in Lucene queries.
 * <p>
 * Defines boost multipliers for different index fields to control relevance scoring.
 * Higher boost values increase the importance of matches in that field.
 * </p>
 */
@ConfigMapping(prefix = "megabrain.search.boost")
public interface BoostConfiguration {

    /**
     * Boost value for entity name field matches.
     * <p>
     * Default: 3.0 (highest priority for exact entity matches)
     * </p>
     */
    @WithDefault("3.0")
    float entityName();

    /**
     * Boost value for documentation summary field matches.
     * <p>
     * Default: 2.0 (important for documentation-driven searches)
     * </p>
     */
    @WithDefault("2.0")
    float docSummary();

    /**
     * Boost value for content field matches.
     * <p>
     * Default: 1.0 (baseline relevance for general content matches)
     * </p>
     */
    @WithDefault("1.0")
    float content();

    /**
     * Gets the boost value for a specific field name.
     * <p>
     * Returns the configured boost for known fields, or 1.0 for unknown fields.
     * </p>
     *
     * @param fieldName the field name
     * @return the boost value for the field
     */
    default float getBoostForField(String fieldName) {
        if (fieldName == null || fieldName.isBlank()) {
            return 1.0f;
        }
        return switch (fieldName) {
            case LuceneSchema.FIELD_ENTITY_NAME -> entityName();
            case LuceneSchema.FIELD_DOC_SUMMARY -> docSummary();
            case LuceneSchema.FIELD_CONTENT -> content();
            default -> 1.0f;
        };
    }
}