/*
 * Copyright (c) 2025 MegaBrain Contributors
 * Licensed under the MIT License - see LICENSE file for details.
 */

package io.megabrain.core;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for LLMCostEstimator (US-03-02 T6).
 */
class LLMCostEstimatorTest {

    @Test
    @DisplayName("estimateTokens null or empty returns 0 or 1")
    void estimateTokens_nullOrEmpty_returnsZeroOrOne() {
        assertThat(LLMCostEstimator.estimateTokens(null)).isZero();
        assertThat(LLMCostEstimator.estimateTokens("")).isZero();
        assertThat(LLMCostEstimator.estimateTokens("x")).isEqualTo(1);
    }

    @Test
    @DisplayName("estimateTokens approximates 4 chars per token")
    void estimateTokens_fourChars_returnsOne() {
        assertThat(LLMCostEstimator.estimateTokens("1234")).isEqualTo(1);
        assertThat(LLMCostEstimator.estimateTokens("12345678")).isEqualTo(2);
        assertThat(LLMCostEstimator.estimateTokens("hello world")).isGreaterThanOrEqualTo(2);
    }

    @ParameterizedTest
    @CsvSource({
            "openai, gpt-4, 1000, 500",
            "OpenAI, gpt-4o, 1000, 500",
            "openai, gpt-3.5-turbo, 1000, 500",
            "anthropic, claude-3-5-sonnet-20241022, 1000, 500",
            "anthropic, claude-3-opus, 1000, 500"
    })
    @DisplayName("estimateCost returns positive for known provider and model")
    void estimateCost_knownProvider_returnsPositive(String provider, String model, int in, int out) {
        double cost = LLMCostEstimator.estimateCost(provider, model, in, out);
        assertThat(cost).isPositive();
    }

    @Test
    @DisplayName("estimateCost GPT-4: 1k input + 1k output is about 0.09")
    void estimateCost_gpt4_1kIn1kOut_approximatelyCorrect() {
        double cost = LLMCostEstimator.estimateCost("openai", "gpt-4", 1000, 1000);
        // 0.03 + 0.06 = 0.09 per 1k each
        assertThat(cost).isBetween(0.08, 0.10);
    }

    @Test
    @DisplayName("estimateCost GPT-3.5 is cheaper than GPT-4 for same tokens")
    void estimateCost_gpt35_cheaperThanGpt4() {
        double gpt4 = LLMCostEstimator.estimateCost("openai", "gpt-4", 1000, 1000);
        double gpt35 = LLMCostEstimator.estimateCost("openai", "gpt-3.5-turbo", 1000, 1000);
        assertThat(gpt35).isLessThan(gpt4);
    }

    @Test
    @DisplayName("estimateCost unknown provider returns 0")
    void estimateCost_unknownProvider_returnsZero() {
        assertThat(LLMCostEstimator.estimateCost("unknown", "some-model", 1000, 500)).isZero();
    }

    @Test
    @DisplayName("estimateCost zero tokens returns 0")
    void estimateCost_zeroTokens_returnsZero() {
        assertThat(LLMCostEstimator.estimateCost("openai", "gpt-4", 0, 0)).isZero();
    }
}
