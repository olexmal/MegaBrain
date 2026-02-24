/*
 * Copyright (c) 2025 MegaBrain Contributors
 * Licensed under the MIT License - see LICENSE file for details.
 */

package io.megabrain.core;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

/**
 * Configuration for Ollama LLM connection.
 * Used by {@link OllamaLLMClient} to connect to a local or remote Ollama instance.
 */
@ConfigMapping(prefix = "megabrain.llm.ollama")
public interface OllamaConfiguration {

    /**
     * Ollama API base URL (e.g. http://localhost:11434).
     */
    @WithDefault("http://localhost:11434")
    String baseUrl();

    /**
     * Model name (e.g. codellama, mistral, llama2).
     */
    @WithDefault("codellama")
    String model();

    /**
     * Request timeout in seconds for Ollama API calls.
     */
    @WithDefault("60")
    int timeoutSeconds();

    /**
     * Number of retry attempts on connection/transient failure (0 = no retries).
     */
    @WithDefault("0")
    int retryAttempts();

    /**
     * Delay in seconds between retry attempts.
     */
    @WithDefault("2")
    int retryDelaySeconds();

    /**
     * Cache TTL in seconds for model availability checks (Ollama /api/tags).
     * Reduces repeated API calls when validating model selection.
     */
    @WithDefault("60")
    int modelAvailabilityCacheSeconds();
}
