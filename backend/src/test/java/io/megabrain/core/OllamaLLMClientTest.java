/*
 * Copyright (c) 2025 MegaBrain Contributors
 * Licensed under the MIT License - see LICENSE file for details.
 */

package io.megabrain.core;

import io.smallrye.mutiny.Uni;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.quality.Strictness;
import org.mockito.junit.jupiter.MockitoSettings;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Unit tests for OllamaLLMClient (US-03-01, T2, T3).
 * Tests validation, availability, model selection, and contract without starting Ollama.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class OllamaLLMClientTest {

    @Mock
    private OllamaConfiguration config;

    @Mock
    private OllamaModelAvailabilityService modelAvailabilityService;

    private OllamaLLMClient client;

    @BeforeEach
    void setUp() {
        when(config.baseUrl()).thenReturn("http://localhost:11434");
        when(config.model()).thenReturn("codellama");
        when(config.timeoutSeconds()).thenReturn(60);
        when(config.modelAvailabilityCacheSeconds()).thenReturn(60);
        when(modelAvailabilityService.isModelAvailable(anyString(), any()))
                .thenAnswer(inv -> {
                    String model = inv.getArgument(1);
                    return Uni.createFrom().item(model != null && !model.isBlank());
                });
        // Client is constructed but @PostConstruct init() is not called (no CDI in this test)
        client = new OllamaLLMClient(config, modelAvailabilityService);
    }

    @Test
    @DisplayName("implements LLMClient interface")
    void ollamaLLMClient_implementsLLMClient() {
        assertThat(client).isInstanceOf(LLMClient.class);
    }

    @Test
    @DisplayName("isAvailable is false when not initialized")
    void isAvailable_notInitialized_returnsFalse() {
        boolean actual = client.isAvailable();
        assertThat(actual).isFalse();
    }

    @Test
    @DisplayName("generate with null message fails with IllegalArgumentException")
    void generate_nullMessage_failsWithIAE() {
        Uni<String> result = client.generate(null);
        assertThatThrownBy(() -> result.await().indefinitely())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("userMessage must not be blank");
    }

    @Test
    @DisplayName("generate with empty string fails with IllegalArgumentException")
    void generate_emptyMessage_failsWithIAE() {
        Uni<String> result = client.generate("");
        assertThatThrownBy(() -> result.await().indefinitely())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("userMessage must not be blank");
    }

    @Test
    @DisplayName("generate with blank string fails with IllegalArgumentException")
    void generate_blankMessage_failsWithIAE() {
        Uni<String> result = client.generate("   ");
        assertThatThrownBy(() -> result.await().indefinitely())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("userMessage must not be blank");
    }

    @Test
    @DisplayName("generate when not available fails with IllegalStateException")
    void generate_whenNotAvailable_failsWithISE() {
        Uni<String> result = client.generate("hello");
        assertThatThrownBy(() -> result.await().indefinitely())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Ollama LLM client is not available");
    }

    @Test
    @DisplayName("isModelAvailable with null returns false")
    void isModelAvailable_null_returnsFalse() {
        Uni<Boolean> result = client.isModelAvailable(null);
        assertThat(result.await().indefinitely()).isFalse();
    }

    @Test
    @DisplayName("isModelAvailable with blank string returns false")
    void isModelAvailable_blank_returnsFalse() {
        Uni<Boolean> result = client.isModelAvailable("   ");
        assertThat(result.await().indefinitely()).isFalse();
    }

    @Test
    @DisplayName("generate with model override when model not available fails with IllegalArgumentException")
    void generate_modelOverride_notAvailable_failsWithIAE() {
        client.init();
        when(modelAvailabilityService.isModelAvailable(anyString(), eq("mistral")))
                .thenReturn(Uni.createFrom().item(false));

        Uni<String> result = client.generate("hello", "mistral");
        assertThatThrownBy(() -> result.await().indefinitely())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Model 'mistral' is not available on Ollama");
    }

    @Test
    @DisplayName("generate with null model override delegates to default model")
    void generate_nullModelOverride_usesDefaultModel() {
        client.init();
        // Default mock returns true for non-blank model; codellama is default
        Uni<String> result = client.generate("hello", (String) null);
        // Fails at chat (no Ollama) but not at model validation
        assertThatThrownBy(() -> result.await().indefinitely())
                .hasMessageNotContaining("Model 'codellama' is not available");
    }
}
