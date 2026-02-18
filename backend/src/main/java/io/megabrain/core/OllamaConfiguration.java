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
     * Request timeout in seconds.
     */
    @WithDefault("60")
    int timeoutSeconds();
}
