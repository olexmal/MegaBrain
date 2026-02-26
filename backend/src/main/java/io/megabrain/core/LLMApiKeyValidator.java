/*
 * Copyright (c) 2025 MegaBrain Contributors
 * Licensed under the MIT License - see LICENSE file for details.
 */

package io.megabrain.core;

/**
 * Utility for LLM API key validation and masking (US-03-02 T4).
 * Never log or expose raw keys; use mask methods when logging key presence/format.
 */
public final class LLMApiKeyValidator {

    private static final String OPENAI_PREFIX = "sk-";
    private static final String ANTHROPIC_PREFIX = "sk-ant-";
    private static final String MASK_SUFFIX = "***";

    private LLMApiKeyValidator() {
    }

    /**
     * Validates OpenAI API key format. Valid keys start with {@code sk-}.
     *
     * @param key API key (may be null or blank)
     * @return true if key is non-blank and starts with sk-
     */
    public static boolean isValidOpenAIKey(String key) {
        return key != null && !key.isBlank() && key.trim().startsWith(OPENAI_PREFIX);
    }

    /**
     * Validates Anthropic API key format. Valid keys start with {@code sk-ant-}.
     *
     * @param key API key (may be null or blank)
     * @return true if key is non-blank and starts with sk-ant-
     */
    public static boolean isValidAnthropicKey(String key) {
        return key != null && !key.isBlank() && key.trim().startsWith(ANTHROPIC_PREFIX);
    }

    /**
     * Returns a masked representation of an OpenAI key for logging (e.g. "sk-***").
     * Never pass the raw key to logs.
     *
     * @param key API key (may be null)
     * @return masked string, or "&lt;not set&gt;" if null/blank
     */
    public static String maskOpenAIKey(String key) {
        if (key == null || key.isBlank()) {
            return "<not set>";
        }
        return OPENAI_PREFIX + MASK_SUFFIX;
    }

    /**
     * Returns a masked representation of an Anthropic key for logging (e.g. "sk-ant-***").
     * Never pass the raw key to logs.
     *
     * @param key API key (may be null)
     * @return masked string, or "&lt;not set&gt;" if null/blank
     */
    public static String maskAnthropicKey(String key) {
        if (key == null || key.isBlank()) {
            return "<not set>";
        }
        return ANTHROPIC_PREFIX + MASK_SUFFIX;
    }
}
