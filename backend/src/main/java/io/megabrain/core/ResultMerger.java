/*
 * Copyright (c) 2025 MegaBrain Contributors
 * Licensed under the MIT License - see LICENSE file for details.
 */

package io.megabrain.core;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.apache.lucene.document.Document;
import org.jboss.logging.Logger;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Merges and deduplicates results from Lucene keyword search and vector similarity search
 * for hybrid ranking (US-02-03, T4).
 * <p>
 * When a chunk appears in both result sets, combines their normalized scores using
 * {@link HybridScorer}. Final results are sorted by combined score (descending).
 * <p>
 * Deduplication uses chunk identifier: (file_path, entity_name) or document_id when available.
 */
@ApplicationScoped
public class ResultMerger {

    private static final Logger LOG = Logger.getLogger(ResultMerger.class);

    @Inject
    HybridScorer hybridScorer;

    /**
     * Merged result containing a chunk with its combined score and source information (US-02-05, T4: optional fieldMatch).
     */
    public record MergedResult(
            String chunkId,
            Document luceneDocument,
            VectorStore.SearchResult vectorResult,
            double combinedScore,
            boolean fromBothSources,
            FieldMatchInfo fieldMatch
    ) {
        /**
         * Creates a merged result from Lucene only.
         */
        public static MergedResult fromLucene(String chunkId, Document document, double score) {
            return fromLucene(chunkId, document, score, null);
        }

        /**
         * Creates a merged result from Lucene only with optional field match info (US-02-05, T4).
         */
        public static MergedResult fromLucene(String chunkId, Document document, double score, FieldMatchInfo fieldMatch) {
            return new MergedResult(chunkId, document, null, score, false, fieldMatch);
        }

        /**
         * Creates a merged result from vector only.
         */
        public static MergedResult fromVector(String chunkId, VectorStore.SearchResult result, double score) {
            return new MergedResult(chunkId, null, result, score, false, null);
        }

        /**
         * Creates a merged result from both sources with combined score.
         */
        public static MergedResult fromBoth(String chunkId, Document document,
                                             VectorStore.SearchResult vectorResult, double combinedScore) {
            return fromBoth(chunkId, document, vectorResult, combinedScore, null);
        }

        /**
         * Creates a merged result from both sources with combined score and optional field match (US-02-05, T4).
         */
        public static MergedResult fromBoth(String chunkId, Document document,
                                             VectorStore.SearchResult vectorResult, double combinedScore,
                                             FieldMatchInfo fieldMatch) {
            return new MergedResult(chunkId, document, vectorResult, combinedScore, true, fieldMatch);
        }
    }

    /**
     * Merges normalized Lucene and vector search results, deduplicating chunks that appear
     * in both result sets. When a chunk appears in both, uses combined score from HybridScorer.
     * Final results are sorted by combined score (descending).
     *
     * @param luceneResults normalized Lucene search results (may be null or empty)
     * @param vectorResults normalized vector search results (may be null or empty)
     * @return merged and sorted results, never null
     */
    public List<MergedResult> merge(List<LuceneIndexService.LuceneScoredResult> luceneResults,
                                    List<VectorStore.SearchResult> vectorResults) {
        if ((luceneResults == null || luceneResults.isEmpty()) &&
            (vectorResults == null || vectorResults.isEmpty())) {
            LOG.debug("Both result sets are empty, returning empty list");
            return List.of();
        }

        Map<String, MergedResult> mergedMap = new HashMap<>();

        // Process Lucene results
        if (luceneResults != null && !luceneResults.isEmpty()) {
            for (LuceneIndexService.LuceneScoredResult luceneResult : luceneResults) {
                String chunkId = extractChunkId(luceneResult.document());
                double luceneScore = luceneResult.score();

                mergedMap.compute(chunkId, (id, existing) -> {
                    FieldMatchInfo fieldMatch = luceneResult.fieldMatch();
                    if (existing == null) {
                        // New Lucene-only result
                        return MergedResult.fromLucene(id, luceneResult.document(), luceneScore, fieldMatch);
                    } else {
                        // Already exists from vector search - combine scores
                        double combinedScore = hybridScorer.combine(luceneScore, existing.combinedScore());
                        return MergedResult.fromBoth(id, luceneResult.document(),
                                existing.vectorResult(), combinedScore, fieldMatch);
                    }
                });
            }
        }

        // Process vector results
        if (vectorResults != null && !vectorResults.isEmpty()) {
            for (VectorStore.SearchResult vectorResult : vectorResults) {
                String chunkId = extractChunkId(vectorResult);
                double vectorScore = vectorResult.similarity();

                mergedMap.compute(chunkId, (id, existing) -> {
                    if (existing == null) {
                        // New vector-only result
                        return MergedResult.fromVector(id, vectorResult, vectorScore);
                    } else {
                        // Already exists from Lucene search - combine scores; preserve field match from Lucene
                        double combinedScore = hybridScorer.combine(existing.combinedScore(), vectorScore);
                        return MergedResult.fromBoth(id, existing.luceneDocument(),
                                vectorResult, combinedScore, existing.fieldMatch());
                    }
                });
            }
        }

        // Sort by combined score (descending) and return
        List<MergedResult> sortedResults = new ArrayList<>(mergedMap.values());
        sortedResults.sort(Comparator.comparing(MergedResult::combinedScore).reversed());

        LOG.debugf("Merged %d Lucene and %d vector results into %d unique chunks",
                luceneResults != null ? luceneResults.size() : 0,
                vectorResults != null ? vectorResults.size() : 0,
                sortedResults.size());

        return sortedResults;
    }

    /**
     * Extracts chunk identifier from a Lucene Document.
     * Uses document_id field if available, otherwise constructs from file_path + entity_name.
     *
     * @param document the Lucene document
     * @return chunk identifier for deduplication
     */
    private String extractChunkId(Document document) {
        // Try document_id first (most reliable)
        String documentId = document.get(LuceneSchema.FIELD_DOCUMENT_ID);
        if (documentId != null && !documentId.isBlank()) {
            return documentId;
        }

        // Fallback to file_path + entity_name
        String filePath = document.get(LuceneSchema.FIELD_FILE_PATH);
        String entityName = document.get(LuceneSchema.FIELD_ENTITY_NAME);
        if (filePath != null && entityName != null) {
            return filePath + ":" + entityName;
        }

        // Last resort: use file_path only
        if (filePath != null) {
            return filePath;
        }

        // Should not happen in practice, but handle gracefully
        LOG.warnf("Could not extract chunk ID from Lucene document, using hash");
        return String.valueOf(document.hashCode());
    }

    /**
     * Extracts chunk identifier from a VectorStore.SearchResult.
     * Uses the result's id field if it matches document_id format, otherwise constructs
     * from metadata (sourceFile + entityName).
     *
     * @param result the vector search result
     * @return chunk identifier for deduplication
     */
    private String extractChunkId(VectorStore.SearchResult result) {
        // Try to match document_id format: filePath:entityName:startLine:endLine
        String vectorId = result.id();
        if (vectorId != null && !vectorId.isBlank()) {
            // Check if it matches document_id format (has entity name in it)
            // Vector ID format: filePath:startLine:startByte:endByte
            // Document ID format: filePath:entityName:startLine:endLine
            // We can't directly match, so use metadata instead
        }

        // Use metadata to construct document_id-like identifier
        VectorStore.VectorMetadata metadata = result.metadata();
        if (metadata != null) {
            String filePath = metadata.sourceFile();
            String entityName = metadata.entityName();
            if (filePath != null && entityName != null) {
                // Try to match document_id format: filePath:entityName:startLine:endLine
                Integer startLine = metadata.startLine();
                Integer endLine = metadata.endLine();
                if (startLine != null && endLine != null) {
                    return String.format("%s:%s:%d:%d", filePath, entityName, startLine, endLine);
                }
                // Fallback to file_path + entity_name
                return filePath + ":" + entityName;
            }
            // Last resort: use file_path only
            if (filePath != null) {
                return filePath;
            }
        }

        // Last resort: use vector ID
        if (vectorId != null && !vectorId.isBlank()) {
            return vectorId;
        }

        // Should not happen in practice, but handle gracefully
        LOG.warnf("Could not extract chunk ID from vector result, using hash");
        return String.valueOf(result.hashCode());
    }
}
