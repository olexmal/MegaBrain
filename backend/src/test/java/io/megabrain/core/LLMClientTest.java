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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Unit tests for LLMClient interface contract (US-03-01, T2, T3).
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class LLMClientTest {

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
        when(modelAvailabilityService.isModelAvailable(anyString(), anyString()))
                .thenReturn(Uni.createFrom().item(true));
        client = new OllamaLLMClient(config, modelAvailabilityService);
    }

    @Test
    @DisplayName("OllamaLLMClient is an implementation of LLMClient")
    void ollamaLLMClient_implementsLLMClient() {
        assertThat(client).isInstanceOf(LLMClient.class);
        assertThat(client.isAvailable()).isFalse();
    }
}
