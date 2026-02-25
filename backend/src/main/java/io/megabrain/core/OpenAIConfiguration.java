/*
 * Copyright (c) 2025 MegaBrain Contributors
 * Licensed under the MIT License - see LICENSE file for details.
 */

package io.megabrain.core;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

/**
 * Configuration for OpenAI API connection.
 * Used by {@link OpenAILLMClient} for GPT-4 and GPT-3.5-turbo (US-03-02).
 * API key should be set via environment variable; never log or expose the key.
 */
@ConfigMapping(prefix = "megabrain.llm.openai")
public interface OpenAIConfiguration {

    /**
     * OpenAI API key (e.g. from OPENAI_API_KEY). When blank, client is disabled.
     */
    @WithDefault("")
    String apiKey();

    /**
     * Model name (e.g. gpt-4, gpt-3.5-turbo).
     */
    @WithDefault("gpt-4")
    String model();

    /**
     * Request timeout in seconds for OpenAI API calls.
     */
    @WithDefault("60")
    int timeoutSeconds();
}
