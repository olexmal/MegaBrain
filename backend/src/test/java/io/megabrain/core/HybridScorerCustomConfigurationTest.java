/*
 * Copyright (c) 2025 MegaBrain Contributors
 * Licensed under the MIT License - see LICENSE file for details.
 */

package io.megabrain.core;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import jakarta.inject.Inject;
import org.assertj.core.data.Offset;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link HybridScorer} with custom weight configuration.
 * Uses a test profile to override default weights.
 */
@QuarkusTest
@TestProfile(HybridScorerCustomConfigurationTest.CustomWeightsProfile.class)
class HybridScorerCustomConfigurationTest {

    @Inject
    HybridScorer hybridScorer;

    @Test
    void configuration_loadsCustomWeights() {
        // Verify custom weights from test profile are loaded
        HybridScorer.HybridWeights weights = hybridScorer.getDefaultWeights();
        // Custom weights: 0.7 keyword, 0.3 vector (from test profile)
        assertThat(weights.keywordWeight()).isCloseTo(0.7, Offset.offset(0.001));
        assertThat(weights.vectorWeight()).isCloseTo(0.3, Offset.offset(0.001));
    }

    @Test
    void configuration_customWeightsUsedInScoring() {
        // Verify custom weights are actually used in scoring
        double score = hybridScorer.combine(1.0, 0.0);
        // Should be: 0.7 * 1.0 + 0.3 * 0.0 = 0.7
        assertThat(score).isCloseTo(0.7, Offset.offset(0.001));

        score = hybridScorer.combine(0.0, 1.0);
        // Should be: 0.7 * 0.0 + 0.3 * 1.0 = 0.3
        assertThat(score).isCloseTo(0.3, Offset.offset(0.001));
    }

    @Test
    void configuration_customWeightsSumToExactlyOne() {
        // Verify custom weights sum to 1.0
        HybridScorer.HybridWeights weights = hybridScorer.getDefaultWeights();
        double sum = weights.keywordWeight() + weights.vectorWeight();
        assertThat(sum).isCloseTo(1.0, Offset.offset(0.001));
    }

    /**
     * Test profile that overrides default hybrid ranking weights.
     */
    public static class CustomWeightsProfile implements io.quarkus.test.junit.QuarkusTestProfile {
        @Override
        public String getConfigProfile() {
            return "custom-weights";
        }

        @Override
        public java.util.Map<String, String> getConfigOverrides() {
            return java.util.Map.of(
                    "megabrain.search.hybrid.keyword-weight", "0.7",
                    "megabrain.search.hybrid.vector-weight", "0.3"
            );
        }
    }
}
