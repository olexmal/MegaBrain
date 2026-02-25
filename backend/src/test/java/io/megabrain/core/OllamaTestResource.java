/*
 * Copyright (c) 2025 MegaBrain Contributors
 * Licensed under the MIT License - see LICENSE file for details.
 */

package io.megabrain.core;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import org.testcontainers.ollama.OllamaContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.Map;

/**
 * Quarkus test resource that starts an Ollama Testcontainer and configures the app to use it.
 * Used by Ollama integration tests (US-03-01 T6).
 */
public class OllamaTestResource implements QuarkusTestResourceLifecycleManager {

    private OllamaContainer ollama;

    @Override
    public Map<String, String> start() {
        ollama = new OllamaContainer(DockerImageName.parse("ollama/ollama:0.1.26"));
        ollama.start();

        String baseUrl = ollama.getEndpoint();

        // Pull a small model so generation tests can run without long wait
        try {
            ollama.execInContainer("ollama", "pull", "tinyllama");
        } catch (Exception e) {
            throw new RuntimeException("Failed to pull tinyllama in Ollama container", e);
        }

        return Map.of(
                "megabrain.llm.ollama.base-url", baseUrl,
                "megabrain.llm.ollama.model", "tinyllama",
                "megabrain.llm.ollama.timeout-seconds", "120"
        );
    }

    @Override
    public void stop() {
        if (ollama != null) {
            ollama.stop();
        }
    }
}
