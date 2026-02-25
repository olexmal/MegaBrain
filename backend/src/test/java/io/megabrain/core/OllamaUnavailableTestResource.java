/*
 * Copyright (c) 2025 MegaBrain Contributors
 * Licensed under the MIT License - see LICENSE file for details.
 */

package io.megabrain.core;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;

import java.util.Map;

/**
 * Quarkus test resource that configures Ollama base URL to an unreachable address.
 * Used to test offline/unavailable scenarios (US-03-01 T6).
 */
public class OllamaUnavailableTestResource implements QuarkusTestResourceLifecycleManager {

    private static final String UNREACHABLE_BASE_URL = "http://127.0.0.1:19999";

    @Override
    public Map<String, String> start() {
        return Map.of(
                "megabrain.llm.ollama.base-url", UNREACHABLE_BASE_URL,
                "megabrain.llm.ollama.model", "codellama",
                "megabrain.llm.ollama.timeout-seconds", "2"
        );
    }

    @Override
    public void stop() {
        // No container to stop
    }
}
