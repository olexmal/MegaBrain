/*
 * Copyright (c) 2025 MegaBrain Contributors
 * Licensed under the MIT License - see LICENSE file for details.
 */

package io.megabrain.core;

/**
 * Estimates cost in USD for LLM API usage based on provider/model and token counts (US-03-02 T6).
 * Uses approximate current pricing; update when provider pricing changes.
 */
public final class LLMCostEstimator {

    private LLMCostEstimator() {
    }

    // OpenAI: per 1k tokens (approximate 2024/2025)
    private static final double OPENAI_GPT4_INPUT_PER_1K = 0.03;
    private static final double OPENAI_GPT4_OUTPUT_PER_1K = 0.06;
    private static final double OPENAI_GPT35_INPUT_PER_1K = 0.0005;
    private static final double OPENAI_GPT35_OUTPUT_PER_1K = 0.0015;

    // Anthropic: per 1k tokens (approximate 2024/2025)
    private static final double ANTHROPIC_SONNET_INPUT_PER_1K = 0.003;
    private static final double ANTHROPIC_SONNET_OUTPUT_PER_1K = 0.015;
    private static final double ANTHROPIC_OPUS_INPUT_PER_1K = 0.015;
    private static final double ANTHROPIC_OPUS_OUTPUT_PER_1K = 0.075;

    /**
     * Estimates cost in USD for the given usage.
     *
     * @param provider     "openai" or "anthropic"
     * @param model        model name (e.g. gpt-4, gpt-3.5-turbo, claude-3-5-sonnet, claude-3-opus)
     * @param inputTokens  input token count
     * @param outputTokens output token count
     * @return estimated cost in USD (0 if unknown provider/model)
     */
    public static double estimateCost(String provider, String model, int inputTokens, int outputTokens) {
        if (provider == null) provider = "";
        if (model == null) model = "";
        String p = provider.toLowerCase().trim();
        String m = model.toLowerCase();

        if (p.contains("openai")) {
            if (m.contains("gpt-4") || m.contains("gpt-4o")) {
                return (inputTokens * OPENAI_GPT4_INPUT_PER_1K + outputTokens * OPENAI_GPT4_OUTPUT_PER_1K) / 1000.0;
            }
            if (m.contains("gpt-3.5") || m.contains("gpt-35")) {
                return (inputTokens * OPENAI_GPT35_INPUT_PER_1K + outputTokens * OPENAI_GPT35_OUTPUT_PER_1K) / 1000.0;
            }
            // default to GPT-4-like for unknown OpenAI models
            return (inputTokens * OPENAI_GPT4_INPUT_PER_1K + outputTokens * OPENAI_GPT4_OUTPUT_PER_1K) / 1000.0;
        }

        if (p.contains("anthropic")) {
            if (m.contains("opus")) {
                return (inputTokens * ANTHROPIC_OPUS_INPUT_PER_1K + outputTokens * ANTHROPIC_OPUS_OUTPUT_PER_1K) / 1000.0;
            }
            // Sonnet and others
            return (inputTokens * ANTHROPIC_SONNET_INPUT_PER_1K + outputTokens * ANTHROPIC_SONNET_OUTPUT_PER_1K) / 1000.0;
        }

        return 0.0;
    }

    /**
     * Rough token count from text length (~4 chars per token for English/code).
     *
     * @param text input or output text
     * @return estimated token count (at least 0)
     */
    public static int estimateTokens(String text) {
        if (text == null || text.isEmpty()) return 0;
        return Math.max(1, (text.length() + 2) / 4);
    }
}
