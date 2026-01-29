/*
 * Copyright (c) 2025 MegaBrain Contributors
 * Licensed under the MIT License - see LICENSE file for details.
 */

package io.megabrain.core;

import java.util.List;
import java.util.Map;

/**
 * Core representation of which index fields matched and their score contributions (US-02-05, T4).
 * Used by LuceneIndexService and ResultMerger; converted to {@link io.megabrain.api.FieldMatchInfo}
 * in the API layer for the search response.
 */
public record FieldMatchInfo(List<String> matchedFields, Map<String, Float> scores) {
    public FieldMatchInfo {
        matchedFields = matchedFields != null ? List.copyOf(matchedFields) : List.of();
        scores = scores != null ? Map.copyOf(scores) : Map.of();
    }
}
