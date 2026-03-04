/*
 * Copyright (c) 2025 MegaBrain Contributors
 * Licensed under the MIT License - see LICENSE file for details.
 */

package io.megabrain.api;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Source metadata for a single chunk used in RAG context or cited in the answer (US-03-05 T3).
 * Included in RagResponse to provide full provenance: file path, entity, line range, relevance, chunk id.
 */
public record RagSourceMetadata(
        @JsonProperty("file_path") String filePath,
        @JsonProperty("entity_name") String entityName,
        @JsonProperty("line_range") LineRange lineRange,
        @JsonProperty("relevance_score") Float relevanceScore,
        @JsonProperty("chunk_id") String chunkId
) {
    /**
     * Creates source metadata with required fields; optional fields may be null.
     */
    public RagSourceMetadata {
        if (filePath == null) {
            filePath = "";
        }
    }
}
