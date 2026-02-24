/*
 * Copyright (c) 2025 MegaBrain Contributors
 * Licensed under the MIT License - see LICENSE file for details.
 */

package io.megabrain.core;

import io.smallrye.mutiny.Uni;

/**
 * Common interface for LLM clients (Ollama, OpenAI, Anthropic).
 * Provides a unified contract for non-streaming chat generation across providers.
 */
public interface LLMClient {

    /**
     * Generates a completion for the given user message.
     *
     * @param userMessage the user prompt or question
     * @return Uni that emits the model response text, or fails on connection or model errors
     */
    Uni<String> generate(String userMessage);

    /**
     * Generates a completion for the given user message with optional per-request model override.
     * When {@code modelOverride} is null or blank, uses the configured default model.
     *
     * @param userMessage the user prompt or question
     * @param modelOverride optional model name override (e.g. mistral, codellama); null/blank to use default
     * @return Uni that emits the model response text, or fails on connection or model errors
     */
    default Uni<String> generate(String userMessage, String modelOverride) {
        return generate(userMessage);
    }

    /**
     * Returns whether this client is available (e.g. endpoint reachable and configured).
     * Used by health checks and feature flags.
     *
     * @return true if the client can be used for generation
     */
    boolean isAvailable();
}
