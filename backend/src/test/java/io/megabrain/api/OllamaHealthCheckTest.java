/*
 * Copyright (c) 2025 MegaBrain Contributors
 * Licensed under the MIT License - see LICENSE file for details.
 */

package io.megabrain.api;

import io.megabrain.core.OllamaConfiguration;
import io.megabrain.core.OllamaLLMClient;
import io.smallrye.mutiny.Uni;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Unit tests for OllamaHealthCheck (US-03-01 T5).
 */
@ExtendWith(MockitoExtension.class)
class OllamaHealthCheckTest {

    @Mock
    private OllamaLLMClient ollamaClient;

    @Mock
    private OllamaConfiguration config;

    private OllamaHealthCheck healthCheck;

    @BeforeEach
    void setUp() {
        when(config.baseUrl()).thenReturn("http://localhost:11434");
        when(config.model()).thenReturn("codellama");
        healthCheck = new OllamaHealthCheck(ollamaClient, config);
    }

    @Test
    @DisplayName("returns DOWN when client is not available")
    void call_clientNotAvailable_returnsDownWithMessage() {
        when(ollamaClient.isAvailable()).thenReturn(false);

        HealthCheckResponse response = healthCheck.call();

        assertThat(response.getStatus()).isEqualTo(HealthCheckResponse.Status.DOWN);
        assertThat(response.getName()).isEqualTo("ollama");
        Map<String, Object> data = response.getData().orElseThrow();
        assertThat(data.get("message")).asString().contains("Ollama client not available");
        assertThat(data.get("endpoint")).isEqualTo("http://localhost:11434");
        assertThat(data.get("model")).isEqualTo("codellama");
    }

    @Test
    @DisplayName("returns UP when endpoint and model are available")
    void call_clientAvailable_modelAvailable_returnsUpWithMessage() {
        when(ollamaClient.isAvailable()).thenReturn(true);
        when(ollamaClient.isModelAvailable("codellama")).thenReturn(Uni.createFrom().item(true));

        HealthCheckResponse response = healthCheck.call();

        assertThat(response.getStatus()).isEqualTo(HealthCheckResponse.Status.UP);
        assertThat(response.getName()).isEqualTo("ollama");
        Map<String, Object> data = response.getData().orElseThrow();
        assertThat(data.get("message")).asString().contains("Ollama reachable");
        assertThat(data.get("endpoint")).isEqualTo("http://localhost:11434");
        assertThat(data.get("model")).isEqualTo("codellama");
    }

    @Test
    @DisplayName("returns DOWN when model is not available")
    void call_clientAvailable_modelNotAvailable_returnsDownWithMessage() {
        when(ollamaClient.isAvailable()).thenReturn(true);
        when(ollamaClient.isModelAvailable("codellama")).thenReturn(Uni.createFrom().item(false));

        HealthCheckResponse response = healthCheck.call();

        assertThat(response.getStatus()).isEqualTo(HealthCheckResponse.Status.DOWN);
        Map<String, Object> data = response.getData().orElseThrow();
        assertThat(data.get("message")).asString().contains("Configured model not available");
        assertThat(data.get("message")).asString().contains("ollama pull");
    }

    @Test
    @DisplayName("returns DOWN when model availability check throws")
    void call_modelAvailabilityThrows_returnsDownWithErrorMessage() {
        when(ollamaClient.isAvailable()).thenReturn(true);
        when(ollamaClient.isModelAvailable("codellama"))
                .thenReturn(Uni.createFrom().failure(new RuntimeException("Connection refused")));

        HealthCheckResponse response = healthCheck.call();

        assertThat(response.getStatus()).isEqualTo(HealthCheckResponse.Status.DOWN);
        Map<String, Object> data = response.getData().orElseThrow();
        assertThat(data.get("message")).asString().contains("unreachable");
    }

    @Test
    @DisplayName("response includes endpoint and model in data")
    void call_responseIncludesEndpointAndModel() {
        when(ollamaClient.isAvailable()).thenReturn(true);
        when(ollamaClient.isModelAvailable("codellama")).thenReturn(Uni.createFrom().item(true));

        HealthCheckResponse response = healthCheck.call();

        Map<String, Object> data = response.getData().orElseThrow();
        assertThat(data).containsKey("endpoint");
        assertThat(data).containsKey("model");
        assertThat(data).containsKey("checkTimeMs");
        assertThat(data.get("endpoint")).isEqualTo("http://localhost:11434");
        assertThat(data.get("model")).isEqualTo("codellama");
    }
}
