/*
 * Copyright (c) 2025 MegaBrain Contributors
 * Licensed under the MIT License - see LICENSE file for details.
 */

package io.megabrain.core;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for LLMApiKeyValidator (US-03-02 T4).
 */
class LLMApiKeyValidatorTest {

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = { "   ", "\t" })
    @DisplayName("isValidOpenAIKey returns false for null or blank")
    void isValidOpenAIKey_nullOrBlank_returnsFalse(String key) {
        assertThat(LLMApiKeyValidator.isValidOpenAIKey(key)).isFalse();
    }

    @Test
    @DisplayName("isValidOpenAIKey returns false when key does not start with sk-")
    void isValidOpenAIKey_invalidPrefix_returnsFalse() {
        assertThat(LLMApiKeyValidator.isValidOpenAIKey("invalid")).isFalse();
        assertThat(LLMApiKeyValidator.isValidOpenAIKey("xk-abc")).isFalse();
        assertThat(LLMApiKeyValidator.isValidOpenAIKey("sk")).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = { "sk-", "sk-x", "sk-abc123", "  sk-xyz  " })
    @DisplayName("isValidOpenAIKey returns true when key starts with sk-")
    void isValidOpenAIKey_validPrefix_returnsTrue(String key) {
        assertThat(LLMApiKeyValidator.isValidOpenAIKey(key)).isTrue();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = { "   ", "\t" })
    @DisplayName("isValidAnthropicKey returns false for null or blank")
    void isValidAnthropicKey_nullOrBlank_returnsFalse(String key) {
        assertThat(LLMApiKeyValidator.isValidAnthropicKey(key)).isFalse();
    }

    @Test
    @DisplayName("isValidAnthropicKey returns false when key does not start with sk-ant-")
    void isValidAnthropicKey_invalidPrefix_returnsFalse() {
        assertThat(LLMApiKeyValidator.isValidAnthropicKey("sk-")).isFalse();
        assertThat(LLMApiKeyValidator.isValidAnthropicKey("sk-abc")).isFalse();
        assertThat(LLMApiKeyValidator.isValidAnthropicKey("sk-ant")).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = { "sk-ant-", "sk-ant-x", "sk-ant-api03-abc", "  sk-ant-xyz  " })
    @DisplayName("isValidAnthropicKey returns true when key starts with sk-ant-")
    void isValidAnthropicKey_validPrefix_returnsTrue(String key) {
        assertThat(LLMApiKeyValidator.isValidAnthropicKey(key)).isTrue();
    }

    @Test
    @DisplayName("maskOpenAIKey returns <not set> for null or blank")
    void maskOpenAIKey_nullOrBlank_returnsNotSet() {
        assertThat(LLMApiKeyValidator.maskOpenAIKey(null)).isEqualTo("<not set>");
        assertThat(LLMApiKeyValidator.maskOpenAIKey("")).isEqualTo("<not set>");
        assertThat(LLMApiKeyValidator.maskOpenAIKey("   ")).isEqualTo("<not set>");
    }

    @Test
    @DisplayName("maskOpenAIKey returns sk-*** for any non-blank key")
    void maskOpenAIKey_nonBlank_returnsMasked() {
        assertThat(LLMApiKeyValidator.maskOpenAIKey("sk-secret")).isEqualTo("sk-***");
        assertThat(LLMApiKeyValidator.maskOpenAIKey("sk-")).isEqualTo("sk-***");
    }

    @Test
    @DisplayName("maskAnthropicKey returns <not set> for null or blank")
    void maskAnthropicKey_nullOrBlank_returnsNotSet() {
        assertThat(LLMApiKeyValidator.maskAnthropicKey(null)).isEqualTo("<not set>");
        assertThat(LLMApiKeyValidator.maskAnthropicKey("")).isEqualTo("<not set>");
        assertThat(LLMApiKeyValidator.maskAnthropicKey("   ")).isEqualTo("<not set>");
    }

    @Test
    @DisplayName("maskAnthropicKey returns sk-ant-*** for any non-blank key")
    void maskAnthropicKey_nonBlank_returnsMasked() {
        assertThat(LLMApiKeyValidator.maskAnthropicKey("sk-ant-secret")).isEqualTo("sk-ant-***");
        assertThat(LLMApiKeyValidator.maskAnthropicKey("sk-ant-")).isEqualTo("sk-ant-***");
    }
}
