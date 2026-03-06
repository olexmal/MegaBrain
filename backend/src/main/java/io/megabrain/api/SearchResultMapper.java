/*
 * Copyright (c) 2025 MegaBrain Contributors
 * Licensed under the MIT License - see LICENSE file for details.
 */

package io.megabrain.api;

import io.megabrain.core.ResultMerger;
import io.megabrain.core.VectorStore;
import org.apache.lucene.document.Document;

import java.util.List;

/**
 * Maps internal search result types to API DTOs.
 * Shared by REST SearchResource and CLI SearchCommand.
 */
public final class SearchResultMapper {

    private SearchResultMapper() {
    }

    /**
     * Converts a MergedResult to a SearchResult DTO.
     *
     * @param mergedResult the merged result from hybrid search
     * @return SearchResult DTO
     */
    public static SearchResult toSearchResult(ResultMerger.MergedResult mergedResult) {
        String content;
        String entityName;
        String entityType;
        String sourceFile;
        String language;
        String repository;
        float score;
        LineRange lineRange;

        if (mergedResult.luceneDocument() != null) {
            Document luceneDoc = mergedResult.luceneDocument();
            content = luceneDoc.get("content");
            entityName = luceneDoc.get("entity_name");
            entityType = luceneDoc.get("entity_type");
            sourceFile = luceneDoc.get("source_file");
            language = luceneDoc.get("language");
            repository = luceneDoc.get("repository");
            score = (float) mergedResult.combinedScore();
            int startLine = getIntField(luceneDoc, "start_line", 1);
            int endLine = getIntField(luceneDoc, "end_line", 1);
            lineRange = new LineRange(startLine, endLine);
        } else if (mergedResult.vectorResult() != null) {
            VectorStore.VectorMetadata vectorMeta = mergedResult.vectorResult().metadata();
            content = vectorMeta != null ? vectorMeta.content() : "";
            entityName = vectorMeta != null ? vectorMeta.entityName() : "";
            entityType = vectorMeta != null ? vectorMeta.entityType() : "";
            sourceFile = vectorMeta != null ? vectorMeta.sourceFile() : "";
            language = vectorMeta != null ? vectorMeta.language() : "";
            repository = "";
            score = (float) mergedResult.combinedScore();
            int start = vectorMeta != null ? vectorMeta.startLine() : 1;
            int end = vectorMeta != null ? vectorMeta.endLine() : 1;
            lineRange = new LineRange(start, end);
        } else {
            content = "";
            entityName = "";
            entityType = "";
            sourceFile = "";
            language = "";
            repository = "";
            score = 0.0f;
            lineRange = new LineRange(1, 1);
        }

        FieldMatchInfo apiFieldMatch = mergedResult.fieldMatch() != null
                ? new FieldMatchInfo(mergedResult.fieldMatch().matchedFields(), mergedResult.fieldMatch().scores())
                : null;
        boolean isTransitive = mergedResult.transitivePath() != null;
        List<String> relationshipPath = mergedResult.transitivePath() != null && !mergedResult.transitivePath().isEmpty()
                ? mergedResult.transitivePath()
                : null;
        return new SearchResult(content, entityName, entityType, sourceFile,
                language, repository, score, lineRange, null, apiFieldMatch, isTransitive, relationshipPath);
    }

    /**
     * Gets an integer field from a Lucene document, with a default value.
     */
    public static int getIntField(Document doc, String fieldName, int defaultValue) {
        String value = doc.get(fieldName);
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException _) {
            return defaultValue;
        }
    }
}
