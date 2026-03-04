/*
 * Copyright (c) 2025 MegaBrain Contributors
 * Licensed under the MIT License - see LICENSE file for details.
 */

package io.megabrain.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import io.megabrain.core.ExtractedCitation;

/**
 * DTO representing a single source reference in RAG answers (US-03-05 T4).
 * Exposes file path, entity, line range, relevance, repository, language, and chunk id
 * for display, linking, and verification in the UI.
 * <p>
 * Serializable to JSON with snake_case property names. Optional fields are omitted
 * from JSON when null via {@link JsonInclude#Include#NON_NULL}.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record SourceDTO(
        /** Source file path (e.g. {@code src/auth/AuthService.java}). Never null; default empty string. */
        @JsonProperty("file_path") String filePath,
        /** Name of the code entity (class, method, etc.). Optional. */
        @JsonProperty("entity_name") String entityName,
        /** Start line number (1-based). Optional. */
        @JsonProperty("line_start") Integer lineStart,
        /** End line number (1-based, inclusive). Optional. */
        @JsonProperty("line_end") Integer lineEnd,
        /** Relevance score from search (0.0–1.0). Optional. */
        @JsonProperty("relevance_score") Float relevanceScore,
        /** Repository identifier. Optional. */
        @JsonProperty("repository") String repository,
        /** Programming language. Optional. */
        @JsonProperty("language") String language,
        /** Chunk identifier for deduplication and reference. Optional. */
        @JsonProperty("chunk_id") String chunkId
) {
    public SourceDTO {
        if (filePath == null) {
            filePath = "";
        }
    }

    /**
     * Builds a SourceDTO from search result context (includes repository and language).
     */
    public static SourceDTO fromSearchResult(SearchResult r, String chunkId) {
        LineRange lr = r != null ? r.getLineRange() : null;
        return new SourceDTO(
                r != null ? r.getSourceFile() : "",
                r != null ? r.getEntityName() : null,
                lr != null ? lr.getStartLine() : null,
                lr != null ? lr.getEndLine() : null,
                r != null ? r.getScore() : null,
                r != null ? r.getRepository() : null,
                r != null ? r.getLanguage() : null,
                chunkId);
    }

    /**
     * Builds a SourceDTO from extracted citation (no repository/language/chunk_id).
     */
    public static SourceDTO fromCitation(ExtractedCitation c) {
        if (c == null) {
            return new SourceDTO("", null, null, null, null, null, null, null);
        }
        return new SourceDTO(
                c.filePath(),
                null,
                c.lineStart(),
                c.lineEnd(),
                null,
                null,
                null,
                null);
    }

    /**
     * Builds a SourceDTO from RagSourceMetadata with optional repository and language.
     */
    public static SourceDTO fromRagSourceMetadata(RagSourceMetadata meta, String repository, String language) {
        if (meta == null) {
            return new SourceDTO("", null, null, null, null, repository, language, null);
        }
        LineRange lr = meta.lineRange();
        return new SourceDTO(
                meta.filePath(),
                meta.entityName(),
                lr != null ? lr.getStartLine() : null,
                lr != null ? lr.getEndLine() : null,
                meta.relevanceScore(),
                repository,
                language,
                meta.chunkId());
    }
}
