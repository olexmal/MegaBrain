/*
 * Copyright (c) 2025 MegaBrain Contributors
 * Licensed under the MIT License - see LICENSE file for details.
 */

package io.megabrain.core;

/**
 * Represents a single facet value and its document count.
 *
 * @param value the facet value label
 * @param count the number of matching documents
 */
public record FacetValue(String value, long count) {
}
