/*
 * Copyright (c) 2025 MegaBrain Contributors
 * Licensed under the MIT License - see LICENSE file for details.
 */

package io.megabrain.core;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.assertj.core.data.Offset;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link HybridScorer} configuration loading and validation.
 * Verifies that weights are loaded from application.properties and validated on startup.
 */
@QuarkusTest
class HybridScorerConfigurationTest {

    @Inject
    HybridScorer hybridScorer;

    @Test
    void configuration_loadsDefaultWeights() {
        // Verify default weights from application.properties are loaded
        HybridScorer.HybridWeights weights = hybridScorer.getDefaultWeights();
        assertThat(weights.keywordWeight()).isCloseTo(0.6, Offset.offset(0.001));
        assertThat(weights.vectorWeight()).isCloseTo(0.4, Offset.offset(0.001));
    }

    @Test
    void configuration_weightsUsedInScoring() {
        // Verify configured weights are actually used in scoring
        double score = hybridScorer.combine(1.0, 0.0);
        // Should be: 0.6 * 1.0 + 0.4 * 0.0 = 0.6
        assertThat(score).isCloseTo(0.6, Offset.offset(0.001));

        score = hybridScorer.combine(0.0, 1.0);
        // Should be: 0.6 * 0.0 + 0.4 * 1.0 = 0.4
        assertThat(score).isCloseTo(0.4, Offset.offset(0.001));
    }

    @Test
    void configuration_weightsSumToExactlyOne() {
        // Verify weights sum to 1.0 (required for valid configuration)
        HybridScorer.HybridWeights weights = hybridScorer.getDefaultWeights();
        double sum = weights.keywordWeight() + weights.vectorWeight();
        assertThat(sum).isCloseTo(1.0, Offset.offset(0.001));
    }

    @Test
    void configuration_weightsInValidRange() {
        // Verify weights are in [0.0, 1.0] range
        HybridScorer.HybridWeights weights = hybridScorer.getDefaultWeights();
        assertThat(weights.keywordWeight()).isBetween(0.0, 1.0);
        assertThat(weights.vectorWeight()).isBetween(0.0, 1.0);
    }
}
