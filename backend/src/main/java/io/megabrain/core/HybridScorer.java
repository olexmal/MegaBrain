/*
 * Copyright (c) 2025 MegaBrain Contributors
 * Licensed under the MIT License - see LICENSE file for details.
 */

package io.megabrain.core;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

/**
 * Combines normalized Lucene (keyword) and vector similarity scores using configurable
 * weights for hybrid ranking (US-02-03).
 * <p>
 * Formula: {@code final_score = (keywordWeight * luceneScore) + (vectorWeight * vectorScore)}.
 * Weights must sum to 1.0 and each be in [0.0, 1.0]. Defaults: 0.6 keyword, 0.4 vector.
 * Supports per-request weight overrides.
 */
@ApplicationScoped
public class HybridScorer {

    private static final Logger LOG = Logger.getLogger(HybridScorer.class);

    private static final double WEIGHT_SUM_TOLERANCE = 1e-9;

    @ConfigProperty(name = "megabrain.search.hybrid.keyword-weight", defaultValue = "0.6")
    double configuredKeywordWeight;

    @ConfigProperty(name = "megabrain.search.hybrid.vector-weight", defaultValue = "0.4")
    double configuredVectorWeight;

    /** Cached validated default weights; set in {@link #init()}. */
    private HybridWeights defaultWeights;

    @PostConstruct
    void init() {
        defaultWeights = HybridWeights.of(configuredKeywordWeight, configuredVectorWeight);
        LOG.infof("HybridScorer initialized: keywordWeight=%.2f, vectorWeight=%.2f",
                defaultWeights.keywordWeight(), defaultWeights.vectorWeight());
    }

    /**
     * Combines normalized Lucene and vector scores using default weights.
     *
     * @param luceneScore normalized keyword score (0.0–1.0)
     * @param vectorScore normalized vector similarity (0.0–1.0)
     * @return weighted combination in [0.0, 1.0]
     */
    public double combine(double luceneScore, double vectorScore) {
        return combine(luceneScore, vectorScore, defaultWeights.keywordWeight(), defaultWeights.vectorWeight());
    }

    /**
     * Combines normalized Lucene and vector scores using per-request weights.
     * Weights are validated (must sum to 1.0, each in [0.0, 1.0]).
     *
     * @param luceneScore   normalized keyword score (0.0–1.0)
     * @param vectorScore   normalized vector similarity (0.0–1.0)
     * @param keywordWeight weight for keyword score
     * @param vectorWeight  weight for vector score
     * @return weighted combination in [0.0, 1.0]
     * @throws IllegalArgumentException if weights are invalid
     */
    public double combine(double luceneScore, double vectorScore,
                          double keywordWeight, double vectorWeight) {
        HybridWeights w = HybridWeights.of(keywordWeight, vectorWeight);
        return (w.keywordWeight() * luceneScore) + (w.vectorWeight() * vectorScore);
    }

    /**
     * Returns the cached default weights used for {@link #combine(double, double)}.
     *
     * @return validated default weights
     */
    public HybridWeights getDefaultWeights() {
        return defaultWeights;
    }

    /**
     * Validated keyword and vector weights. Use {@link #of(double, double)} to create.
     */
    public record HybridWeights(double keywordWeight, double vectorWeight) {

        /**
         * Creates validated weights. Both must be in [0.0, 1.0] and sum to 1.0.
         *
         * @param keywordWeight weight for keyword (Lucene) score
         * @param vectorWeight  weight for vector score
         * @return validated weights
         * @throws IllegalArgumentException if invalid
         */
        public static HybridWeights of(double keywordWeight, double vectorWeight) {
            if (keywordWeight < 0.0 || keywordWeight > 1.0) {
                throw new IllegalArgumentException(
                        "keywordWeight must be in [0.0, 1.0], got: " + keywordWeight);
            }
            if (vectorWeight < 0.0 || vectorWeight > 1.0) {
                throw new IllegalArgumentException(
                        "vectorWeight must be in [0.0, 1.0], got: " + vectorWeight);
            }
            double sum = keywordWeight + vectorWeight;
            if (Math.abs(sum - 1.0) > WEIGHT_SUM_TOLERANCE) {
                throw new IllegalArgumentException(
                        "Weights must sum to 1.0, got keywordWeight=" + keywordWeight
                                + " vectorWeight=" + vectorWeight + " sum=" + sum);
            }
            return new HybridWeights(keywordWeight, vectorWeight);
        }
    }
}
