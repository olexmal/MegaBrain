/*
 * Copyright (c) 2025 MegaBrain Contributors
 * Licensed under the MIT License - see LICENSE file for details.
 */

package io.megabrain.core;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for OllamaConfiguration (US-03-01, T2).
 * Verifies configuration loading and defaults.
 */
@QuarkusTest
class OllamaConfigurationTest {

    @Inject
    OllamaConfiguration config;

    @Test
    @DisplayName("configuration is injected")
    void ollamaConfiguration_injected_notNull() {
        assertThat(config).isNotNull();
    }

    @Test
    @DisplayName("baseUrl has default or configured value")
    void baseUrl_returnsDefaultOrConfigured() {
        String baseUrl = config.baseUrl();
        assertThat(baseUrl).isNotBlank();
        assertThat(baseUrl).startsWith("http");
    }

    @Test
    @DisplayName("model has default or configured value")
    void model_returnsDefaultOrConfigured() {
        String model = config.model();
        assertThat(model).isNotBlank();
    }

    @Test
    @DisplayName("timeoutSeconds is positive")
    void timeoutSeconds_isPositive() {
        int timeout = config.timeoutSeconds();
        assertThat(timeout).isPositive();
    }
}
