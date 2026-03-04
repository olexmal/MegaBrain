/*
 * Copyright (c) 2025 MegaBrain Contributors
 * Licensed under the MIT License - see LICENSE file for details.
 */

package io.megabrain.api;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Collections;
import java.util.List;

/**
 * Response DTO for non-streaming RAG answers (US-03-04 T5).
 * Same shape as the logical "complete" payload when streaming ends:
 * full answer text, optional sources, optional model identifier.
 */
public record RagResponse(
        @JsonProperty("answer") String answer,
        @JsonProperty("sources") List<String> sources,
        @JsonProperty("model_used") String modelUsed
) {
    public RagResponse {
        if (answer == null) {
            answer = "";
        }
        if (sources == null) {
            sources = Collections.emptyList();
        }
    }

    /**
     * Creates a response with only the answer (no sources or model).
     */
    public static RagResponse of(String answer) {
        return new RagResponse(answer, Collections.emptyList(), null);
    }
}
