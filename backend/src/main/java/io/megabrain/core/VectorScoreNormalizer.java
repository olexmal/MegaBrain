/*
 * Copyright (c) 2025 MegaBrain Contributors
 * Licensed under the MIT License - see LICENSE file for details.
 */

package io.megabrain.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Normalizes vector similarity scores to a 0.0–1.0 range using min-max normalization.
 * Uses the same approach as {@link LuceneIndexService#normalizeScores} so that
 * vector and Lucene scores can be combined fairly in hybrid ranking (e.g. US-02-03).
 * <p>
 * Cosine similarity is typically already in [0, 1]; cosine distance (1 − similarity)
 * would need conversion by callers before normalization. This normalizer works on
 * whatever score scale the caller provides, mapping the result set to [0, 1].
 */
public final class VectorScoreNormalizer {

    private VectorScoreNormalizer() {
        // utility class
    }

    /**
     * Normalizes vector similarity scores to 0.0–1.0 using min-max normalization.
     * Behavior is aligned with Lucene normalization: null/empty → empty list;
     * single result → score 1.0; all equal scores → all 1.0; otherwise min-max.
     *
     * @param results list of vector search results to normalize (may be null or empty)
     * @return list of results with normalized similarity (0.0–1.0); never null
     */
    public static List<VectorStore.SearchResult> normalizeScores(List<VectorStore.SearchResult> results) {
        if (results == null || results.isEmpty()) {
            return results != null ? results : Collections.emptyList();
        }

        if (results.size() == 1) {
            VectorStore.SearchResult r = results.get(0);
            return List.of(withSimilarity(r, 1.0));
        }

        double minScore = Double.MAX_VALUE;
        double maxScore = -Double.MAX_VALUE;

        for (VectorStore.SearchResult r : results) {
            double s = r.similarity();
            if (s < minScore) minScore = s;
            if (s > maxScore) maxScore = s;
        }

        if (maxScore == minScore) {
            return results.stream()
                    .map(r -> withSimilarity(r, 1.0))
                    .toList();
        }

        double range = maxScore - minScore;
        List<VectorStore.SearchResult> normalized = new ArrayList<>(results.size());
        for (VectorStore.SearchResult r : results) {
            double normalizedScore = (r.similarity() - minScore) / range;
            normalized.add(withSimilarity(r, normalizedScore));
        }
        return normalized;
    }

    private static VectorStore.SearchResult withSimilarity(VectorStore.SearchResult r, double similarity) {
        return new VectorStore.SearchResult(r.id(), r.vector(), r.metadata(), similarity);
    }
}
