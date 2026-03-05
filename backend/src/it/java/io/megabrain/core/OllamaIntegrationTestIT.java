/*
 * Copyright (c) 2025 MegaBrain Contributors
 * Licensed under the MIT License - see LICENSE file for details.
 */

package io.megabrain.core;

import io.megabrain.api.OllamaHealthCheck;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Readiness;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Integration tests for Ollama client with a real Ollama Testcontainer (US-03-01 T6).
 * Verifies model selection, endpoint configuration, health checks, and LLM generation.
 */
@QuarkusTest
@QuarkusTestResource(OllamaTestResource.class)
class OllamaIntegrationTestIT {

    private static final Duration AWAIT_TIMEOUT = Duration.ofSeconds(120);

    @Inject
    OllamaLLMClient ollamaClient;

    @Inject
    OllamaConfiguration config;

    @Inject
    @Readiness
    OllamaHealthCheck ollamaHealthCheck;

    @Test
    @DisplayName("client is available when Ollama container is running")
    void ollamaClient_isAvailable_returnsTrue() {
        assertThat(ollamaClient.isAvailable()).isTrue();
    }

    @Test
    @DisplayName("endpoint configuration is set from test resource")
    void endpointConfiguration_isFromTestResource() {
        assertThat(config.baseUrl()).isNotBlank();
        assertThat(config.baseUrl()).startsWith("http://");
        assertThat(config.model()).isEqualTo("tinyllama");
    }

    @Test
    @DisplayName("configured model is reported as available")
    void isModelAvailable_configuredModel_returnsTrue() {
        Boolean available = ollamaClient.isModelAvailable(config.model()).await().atMost(AWAIT_TIMEOUT);
        assertThat(available).isTrue();
    }

    @Test
    @DisplayName("unknown model is reported as not available")
    void isModelAvailable_unknownModel_returnsFalse() {
        Boolean available = ollamaClient.isModelAvailable("nonexistent-model-xyz").await().atMost(AWAIT_TIMEOUT);
        assertThat(available).isFalse();
    }

    @Test
    @DisplayName("health check returns UP when Ollama and model are available")
    void healthCheck_ollamaAvailable_returnsUp() {
        HealthCheckResponse response = ollamaHealthCheck.call();

        assertThat(response.getStatus()).isEqualTo(HealthCheckResponse.Status.UP);
        assertThat(response.getName()).isEqualTo("ollama");
        assertThat(response.getData()).isPresent();
        response.getData().ifPresent(data -> {
            assertThat(data).containsKey("endpoint");
            assertThat(data).containsKey("model");
            assertThat(data.get("message")).asString().contains("Ollama reachable");
        });
    }

    @Test
    @DisplayName("generate returns non-empty response for simple prompt")
    void generate_simplePrompt_returnsNonEmptyResponse() {
        String response = ollamaClient.generate("Say hello in one word.").await().atMost(AWAIT_TIMEOUT);

        assertThat(response).isNotBlank();
    }

    @Test
    @DisplayName("generate with model override uses requested model when available")
    void generate_modelOverride_usesOverride() {
        // tinyllama is the only model we pulled; use it explicitly
        String response = ollamaClient.generate("Reply with the number 42.", "tinyllama")
                .await().atMost(AWAIT_TIMEOUT);

        assertThat(response).isNotBlank();
    }
}
