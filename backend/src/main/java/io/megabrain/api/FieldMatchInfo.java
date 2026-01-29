/*
 * Copyright (c) 2025 MegaBrain Contributors
 * Licensed under the MIT License - see LICENSE file for details.
 */

package io.megabrain.api;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

/**
 * Field match information for a search result (US-02-05, T4).
 * <p>
 * Shows which index fields matched the query and their per-field score contributions,
 * helping users understand why a result ranked as it did.
 * <p>
 * JSON format: {@code {"matched_fields": ["entity_name", "content"], "scores": {"entity_name": 2.1, "content": 0.5}}}
 */
public class FieldMatchInfo {

    @JsonProperty("matched_fields")
    private final List<String> matchedFields;

    @JsonProperty("scores")
    private final Map<String, Float> scores;

    /**
     * Default constructor for Jackson deserialization.
     */
    public FieldMatchInfo() {
        this.matchedFields = List.of();
        this.scores = Map.of();
    }

    /**
     * Creates field match info.
     *
     * @param matchedFields list of field names that contributed to the match (e.g. entity_name, content)
     * @param scores map of field name to score contribution
     */
    public FieldMatchInfo(List<String> matchedFields, Map<String, Float> scores) {
        this.matchedFields = matchedFields != null ? List.copyOf(matchedFields) : List.of();
        this.scores = scores != null ? Map.copyOf(scores) : Map.of();
    }

    public List<String> getMatchedFields() {
        return matchedFields;
    }

    public Map<String, Float> getScores() {
        return scores;
    }
}
