/*
 * Copyright (c) 2025 MegaBrain Contributors
 * Licensed under the MIT License - see LICENSE file for details.
 */

package io.megabrain.core;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BoostConfigurationValidatorTest {

    @Test
    void shouldRejectZero() {
        assertThatThrownBy(() -> BoostConfigurationValidator.validatePositiveFinite("p", 0.0f))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("p")
            .hasMessageContaining("finite, positive");
    }

    @Test
    void shouldRejectNegative() {
        assertThatThrownBy(() -> BoostConfigurationValidator.validatePositiveFinite("p", -1.0f))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("p");
    }

    @Test
    void shouldRejectNaN() {
        assertThatThrownBy(() -> BoostConfigurationValidator.validatePositiveFinite("p", Float.NaN))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("p")
            .hasMessageContaining("NaN");
    }

    @Test
    void shouldRejectInfinity() {
        assertThatThrownBy(() -> BoostConfigurationValidator.validatePositiveFinite("p", Float.POSITIVE_INFINITY))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("p")
            .hasMessageContaining("Infinity");
    }
}

