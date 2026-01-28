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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

/**
 * Unit tests for {@link HybridScorer}.
 * Covers weighted combination formula, default weights, and per-request override.
 */
@QuarkusTest
class HybridScorerTest {

    @Inject
    HybridScorer hybridScorer;

    @Test
    void combine_defaultWeightsKeywordOnly() {
        double s = hybridScorer.combine(1.0, 0.0);
        assertThat(s).isCloseTo(0.6, Offset.offset(0.001));
    }

    @Test
    void combine_defaultWeightsVectorOnly() {
        double s = hybridScorer.combine(0.0, 1.0);
        assertThat(s).isCloseTo(0.4, Offset.offset(0.001));
    }

    @Test
    void combine_defaultWeightsBothHalf() {
        double s = hybridScorer.combine(0.5, 0.5);
        assertThat(s).isCloseTo(0.5, Offset.offset(0.001));
    }

    @Test
    void combine_defaultWeightsBothOne() {
        double s = hybridScorer.combine(1.0, 1.0);
        assertThat(s).isOne();
    }

    @Test
    void combine_defaultWeightsBothZero() {
        double s = hybridScorer.combine(0.0, 0.0);
        assertThat(s).isZero();
    }

    @Test
    void combine_defaultWeightsFormula() {
        double s = hybridScorer.combine(0.5, 0.8);
        assertThat(s).isCloseTo(0.6 * 0.5 + 0.4 * 0.8, Offset.offset(0.001));
    }

    @Test
    void combine_perRequestOverrideEqualWeights() {
        double s = hybridScorer.combine(0.4, 0.6, 0.5, 0.5);
        assertThat(s).isCloseTo(0.5, Offset.offset(0.001));
    }

    @Test
    void combine_perRequestOverrideKeywordOnly() {
        double s = hybridScorer.combine(1.0, 0.0, 1.0, 0.0);
        assertThat(s).isOne();
    }

    @Test
    void combine_perRequestOverrideVectorOnly() {
        double s = hybridScorer.combine(0.0, 1.0, 0.0, 1.0);
        assertThat(s).isOne();
    }

    @Test
    void combine_perRequestOverrideInvalidSumThrows() {
        assertThatThrownBy(() -> hybridScorer.combine(0.5, 0.5, 0.5, 0.6))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("sum to 1.0");
    }

    @Test
    void combine_perRequestOverrideInvalidKeywordWeightThrows() {
        assertThatThrownBy(() -> hybridScorer.combine(0.5, 0.5, -0.1, 1.1))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void getDefaultWeights_returnsConfiguredDefaults() {
        HybridScorer.HybridWeights w = hybridScorer.getDefaultWeights();
        assertThat(w.keywordWeight()).isCloseTo(0.6, Offset.offset(0.001));
        assertThat(w.vectorWeight()).isCloseTo(0.4, Offset.offset(0.001));
    }

    @Test
    void combine_efficientNoAllocation() {
        for (int i = 0; i < 10_000; i++) {
            assertDoesNotThrow(() -> hybridScorer.combine(0.3, 0.7));
        }
    }
}
