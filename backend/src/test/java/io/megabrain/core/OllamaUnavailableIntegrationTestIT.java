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
 * Integration tests for Ollama when the endpoint is unreachable (offline/unavailable scenario).
 * Verifies clear error messages and health DOWN (US-03-01 T6).
 */
@QuarkusTest
@QuarkusTestResource(OllamaUnavailableTestResource.class)
class OllamaUnavailableIntegrationTestIT {

    private static final Duration AWAIT_TIMEOUT = Duration.ofSeconds(5);

    @Inject
    OllamaLLMClient ollamaClient;

    @Inject
    @Readiness
    OllamaHealthCheck ollamaHealthCheck;

    @Test
    @DisplayName("health check returns DOWN when Ollama endpoint is unreachable")
    void healthCheck_ollamaUnreachable_returnsDownWithClearMessage() {
        HealthCheckResponse response = ollamaHealthCheck.call();

        assertThat(response.getStatus()).isEqualTo(HealthCheckResponse.Status.DOWN);
        assertThat(response.getName()).isEqualTo("ollama");
        assertThat(response.getData()).isPresent();
        response.getData().ifPresent(data -> {
            assertThat(data).containsKey("message");
            String message = data.get("message").toString();
            assertThat(message).containsAnyOf("unreachable", "not available", "Configured model not available", "endpoint");
        });
    }

    @Test
    @DisplayName("generate fails with clear error when Ollama is unavailable")
    void generate_ollamaUnavailable_failsWithClearError() {
        assertThatThrownBy(() ->
                ollamaClient.generate("Hello").await().atMost(AWAIT_TIMEOUT))
                .hasMessageContaining("Ollama")
                .satisfies(t -> {
                    String msg = t.getMessage();
                    assertThat(msg).containsAnyOf("not available", "unreachable", "Connection", "refused", "model");
                });
    }

    @Test
    @DisplayName("model availability check returns false for unreachable endpoint")
    void isModelAvailable_unreachableEndpoint_returnsFalse() {
        Boolean available = ollamaClient.isModelAvailable("codellama").await().atMost(AWAIT_TIMEOUT);
        assertThat(available).isFalse();
    }
}
