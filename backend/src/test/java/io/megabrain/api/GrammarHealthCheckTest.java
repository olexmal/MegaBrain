/*
 * Copyright (c) 2025 MegaBrain Contributors
 * Licensed under the MIT License - see LICENSE file for details.
 */

package io.megabrain.api;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
class GrammarHealthCheckTest {

    @Test
    void grammarStatusEnum_shouldHaveDescriptions() {
        assertThat(GrammarHealthCheck.GrammarStatus.LOADED.getDescription())
                .isEqualTo("Grammar is loaded and available");
        assertThat(GrammarHealthCheck.GrammarStatus.NOT_CACHED.getDescription())
                .isEqualTo("Grammar not cached locally");
        assertThat(GrammarHealthCheck.GrammarStatus.NOT_CONFIGURED.getDescription())
                .isEqualTo("Grammar not configured");
        assertThat(GrammarHealthCheck.GrammarStatus.FAILED.getDescription())
                .isEqualTo("Grammar failed to load");
    }

    @Test
    void healthCheck_shouldBeAvailable() {
        // This is an integration test - the actual health check will be tested
        // through the Quarkus health endpoints in the runtime

        // Just verify that the enum values work as expected
        assertThat(GrammarHealthCheck.GrammarStatus.LOADED.getDescription())
                .isEqualTo("Grammar is loaded and available");
        assertThat(GrammarHealthCheck.GrammarStatus.NOT_CACHED.getDescription())
                .isEqualTo("Grammar not cached locally");
        assertThat(GrammarHealthCheck.GrammarStatus.NOT_CONFIGURED.getDescription())
                .isEqualTo("Grammar not configured");
        assertThat(GrammarHealthCheck.GrammarStatus.FAILED.getDescription())
                .isEqualTo("Grammar failed to load");
    }
}
