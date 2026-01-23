/*
 * Copyright (c) 2025 MegaBrain Contributors
 * Licensed under the MIT License - see LICENSE file for details.
 */

package io.megabrain.core;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link HybridScorer.HybridWeights} validation.
 * Covers weight sum-to-1.0 and range [0,1] rules.
 */
class HybridWeightsTest {

    @Test
    void of_defaultWeightsValid() {
        HybridScorer.HybridWeights w = HybridScorer.HybridWeights.of(0.6, 0.4);
        assertThat(w.keywordWeight()).isEqualTo(0.6);
        assertThat(w.vectorWeight()).isEqualTo(0.4);
    }

    @Test
    void of_equalWeightsValid() {
        HybridScorer.HybridWeights w = HybridScorer.HybridWeights.of(0.5, 0.5);
        assertThat(w.keywordWeight()).isEqualTo(0.5);
        assertThat(w.vectorWeight()).isEqualTo(0.5);
    }

    @Test
    void of_keywordOnlyValid() {
        HybridScorer.HybridWeights w = HybridScorer.HybridWeights.of(1.0, 0.0);
        assertThat(w.keywordWeight()).isEqualTo(1.0);
        assertThat(w.vectorWeight()).isEqualTo(0.0);
    }

    @Test
    void of_vectorOnlyValid() {
        HybridScorer.HybridWeights w = HybridScorer.HybridWeights.of(0.0, 1.0);
        assertThat(w.keywordWeight()).isEqualTo(0.0);
        assertThat(w.vectorWeight()).isEqualTo(1.0);
    }

    @Test
    void of_sumNotOneThrows() {
        assertThatThrownBy(() -> HybridScorer.HybridWeights.of(0.5, 0.6))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("sum to 1.0");
    }

    @Test
    void of_keywordWeightNegativeThrows() {
        assertThatThrownBy(() -> HybridScorer.HybridWeights.of(-0.1, 1.1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("keywordWeight");
    }

    @Test
    void of_keywordWeightAboveOneThrows() {
        assertThatThrownBy(() -> HybridScorer.HybridWeights.of(1.1, 0.0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("keywordWeight");
    }

    @Test
    void of_vectorWeightNegativeThrows() {
        assertThatThrownBy(() -> HybridScorer.HybridWeights.of(1.0, -0.1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("vectorWeight");
    }

    @Test
    void of_vectorWeightAboveOneThrows() {
        assertThatThrownBy(() -> HybridScorer.HybridWeights.of(0.0, 1.1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("vectorWeight");
    }

    @Test
    void of_bothZeroThrows() {
        assertThatThrownBy(() -> HybridScorer.HybridWeights.of(0.0, 0.0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("sum to 1.0");
    }
}
