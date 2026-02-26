/*
 * Copyright (c) 2025 MegaBrain Contributors
 * Licensed under the MIT License - see LICENSE file for details.
 */

package io.megabrain.core;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

/**
 * Configuration for Anthropic Claude API connection.
 * Used by {@link AnthropicLLMClient} for Claude 3.5 Sonnet and Claude 3 Opus (US-03-02 T3).
 * API key should be set via environment variable; never log or expose the key.
 */
@ConfigMapping(prefix = "megabrain.llm.anthropic")
public interface AnthropicConfiguration {

    /**
     * Anthropic API key (e.g. from ANTHROPIC_API_KEY). When blank, client is disabled.
     */
    @WithDefault("")
    String apiKey();

    /**
     * Model name (e.g. claude-3-5-sonnet-20241022, claude-3-opus-20240229).
     */
    @WithDefault("claude-3-5-sonnet-20241022")
    String model();

    /**
     * Request timeout in seconds for Anthropic API calls.
     */
    @WithDefault("60")
    int timeoutSeconds();

    /**
     * Maximum number of retry attempts for rate limit (429) and server errors (5xx).
     * Total attempts = maxRetries + 1. Default 4 (5 attempts).
     */
    @WithDefault("4")
    int maxRetries();

    /**
     * Base delay in milliseconds for exponential backoff. Delays: baseDelayMs, 2x, 4x, 8x.
     */
    @WithDefault("1000")
    long baseDelayMs();
}
