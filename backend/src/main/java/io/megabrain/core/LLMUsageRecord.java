/*
 * Copyright (c) 2025 MegaBrain Contributors
 * Licensed under the MIT License - see LICENSE file for details.
 */

package io.megabrain.core;

import java.time.Instant;

/**
 * Immutable record of a single LLM API call usage (US-03-02 T6).
 * Used for cost tracking, reporting, and billing.
 *
 * @param provider       provider name (e.g. "openai", "anthropic")
 * @param model          model name (e.g. "gpt-4", "claude-3-5-sonnet-20241022")
 * @param inputTokens    number of input/prompt tokens
 * @param outputTokens   number of output/completion tokens
 * @param costEstimate   estimated cost in USD
 * @param timestamp      when the call completed
 */
public record LLMUsageRecord(
        String provider,
        String model,
        int inputTokens,
        int outputTokens,
        double costEstimate,
        Instant timestamp
) {
    public LLMUsageRecord {
        if (provider == null) provider = "";
        if (model == null) model = "";
        if (timestamp == null) timestamp = Instant.now();
    }
}
