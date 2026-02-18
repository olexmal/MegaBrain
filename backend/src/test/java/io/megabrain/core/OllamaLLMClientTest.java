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
import static org.mockito.Mockito.when;

/**
 * Unit tests for OllamaLLMClient (US-03-01, T2).
 * Tests validation, availability, and contract without starting Ollama.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class OllamaLLMClientTest {

    @Mock
    private OllamaConfiguration config;

    private OllamaLLMClient client;

    @BeforeEach
    void setUp() {
        when(config.baseUrl()).thenReturn("http://localhost:11434");
        when(config.model()).thenReturn("codellama");
        when(config.timeoutSeconds()).thenReturn(60);
        // Client is constructed but @PostConstruct init() is not called (no CDI in this test)
        client = new OllamaLLMClient(config);
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
}
