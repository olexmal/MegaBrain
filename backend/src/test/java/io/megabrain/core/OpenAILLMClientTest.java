/*
 * Copyright (c) 2025 MegaBrain Contributors
 * Licensed under the MIT License - see LICENSE file for details.
 */

package io.megabrain.core;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * Unit tests for OpenAILLMClient (US-03-02 T2).
 * Tests validation, availability, and LLMClient contract without calling OpenAI API.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class OpenAILLMClientTest {

    @Mock
    private OpenAIConfiguration config;

    @Mock
    private LLMRetryHelper retryHelper;

    private OpenAILLMClient client;

    @BeforeEach
    void setUp() {
        when(config.apiKey()).thenReturn("");
        when(config.model()).thenReturn("gpt-4");
        when(config.timeoutSeconds()).thenReturn(60);
        when(config.maxRetries()).thenReturn(4);
        when(config.baseDelayMs()).thenReturn(1000L);
        client = new OpenAILLMClient(config, retryHelper);
    }

    @Test
    @DisplayName("implements LLMClient interface")
    void openAILLMClient_implementsLLMClient() {
        assertThat(client).isInstanceOf(LLMClient.class);
    }

    @Test
    @DisplayName("isAvailable is false when not initialized")
    void isAvailable_notInitialized_returnsFalse() {
        assertThat(client.isAvailable()).isFalse();
    }

    @Test
    @DisplayName("isAvailable is false when API key is blank after init")
    void isAvailable_blankApiKey_returnsFalse() {
        when(config.apiKey()).thenReturn("   ");
        client.init();
        assertThat(client.isAvailable()).isFalse();
    }

    @Test
    @DisplayName("isAvailable is false when API key is null after init")
    void isAvailable_nullApiKey_returnsFalse() {
        when(config.apiKey()).thenReturn(null);
        client.init();
        assertThat(client.isAvailable()).isFalse();
    }

    @Test
    @DisplayName("isAvailable is true when API key is set and init succeeds")
    void isAvailable_validApiKey_returnsTrue() {
        when(config.apiKey()).thenReturn("sk-valid-key");
        when(config.model()).thenReturn("gpt-4");
        when(config.timeoutSeconds()).thenReturn(60);
        when(config.maxRetries()).thenReturn(4);
        when(config.baseDelayMs()).thenReturn(1000L);
        client.init();
        assertThat(client.isAvailable()).isTrue();
    }

    @Test
    @DisplayName("init throws IllegalStateException when API key has invalid format")
    void init_invalidKeyFormat_throwsIllegalStateException() {
        when(config.apiKey()).thenReturn("invalid-key");
        when(config.model()).thenReturn("gpt-4");
        when(config.timeoutSeconds()).thenReturn(60);
        assertThatThrownBy(() -> client.init())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("invalid format")
                .hasMessageContaining("sk-")
                .hasMessageContaining("OPENAI_API_KEY");
    }

    @Test
    @DisplayName("generate with null message fails with IllegalArgumentException")
    void generate_nullMessage_failsWithIAE() {
        var result = client.generate(null);
        assertThatThrownBy(() -> result.await().indefinitely())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("userMessage must not be blank");
    }

    @Test
    @DisplayName("generate with empty string fails with IllegalArgumentException")
    void generate_emptyMessage_failsWithIAE() {
        var result = client.generate("");
        assertThatThrownBy(() -> result.await().indefinitely())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("userMessage must not be blank");
    }

    @Test
    @DisplayName("generate with blank string fails with IllegalArgumentException")
    void generate_blankMessage_failsWithIAE() {
        var result = client.generate("   ");
        assertThatThrownBy(() -> result.await().indefinitely())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("userMessage must not be blank");
    }

    @Test
    @DisplayName("generate when not available fails with IllegalStateException")
    void generate_whenNotAvailable_failsWithISE() {
        var result = client.generate("hello");
        assertThatThrownBy(() -> result.await().indefinitely())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("OpenAI LLM client is not available");
    }
}
