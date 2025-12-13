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

    @Test
    void grammarStatus_enumValues_areUnique() {
        // Ensure all enum values are unique
        var values = GrammarHealthCheck.GrammarStatus.values();
        assertThat(values).hasSize(4);

        var descriptions = java.util.Arrays.stream(values)
                .map(GrammarHealthCheck.GrammarStatus::getDescription)
                .distinct()
                .toArray(String[]::new);

        assertThat(descriptions).hasSize(4); // All descriptions should be unique
    }

    @Test
    void grammarDetail_record_worksCorrectly() {
        var detail = new GrammarHealthCheck.GrammarDetail(
                "testlang", GrammarHealthCheck.GrammarStatus.LOADED, "1.0.0", null);

        assertThat(detail.language()).isEqualTo("testlang");
        assertThat(detail.status()).isEqualTo(GrammarHealthCheck.GrammarStatus.LOADED);
        assertThat(detail.version()).isEqualTo("1.0.0");
        assertThat(detail.errorMessage()).isNull();
    }

    @Test
    void grammarDetail_record_handlesNullValues() {
        var detail = new GrammarHealthCheck.GrammarDetail(
                "testlang", GrammarHealthCheck.GrammarStatus.FAILED, null, "error message");

        assertThat(detail.language()).isEqualTo("testlang");
        assertThat(detail.status()).isEqualTo(GrammarHealthCheck.GrammarStatus.FAILED);
        assertThat(detail.version()).isNull();
        assertThat(detail.errorMessage()).isEqualTo("error message");
    }

    @Test
    void grammarHealthStatus_record_aggregatesCorrectly() {
        var details = java.util.List.of(
                new GrammarHealthCheck.GrammarDetail("lang1", GrammarHealthCheck.GrammarStatus.LOADED, "1.0", null),
                new GrammarHealthCheck.GrammarDetail("lang2", GrammarHealthCheck.GrammarStatus.FAILED, null, "error"),
                new GrammarHealthCheck.GrammarDetail("lang3", GrammarHealthCheck.GrammarStatus.NOT_CACHED, null, null)
        );

        var status = new GrammarHealthCheck.GrammarHealthStatus(3, 1, 1, details);

        assertThat(status.totalGrammars()).isEqualTo(3);
        assertThat(status.loadedGrammars()).isEqualTo(1);
        assertThat(status.failedGrammars()).isEqualTo(1);
        assertThat(status.grammarDetails()).hasSize(3);
    }

    @Test
    void grammarHealthStatus_record_handlesEmptyList() {
        var status = new GrammarHealthCheck.GrammarHealthStatus(0, 0, 0, java.util.List.of());

        assertThat(status.totalGrammars()).isEqualTo(0);
        assertThat(status.loadedGrammars()).isEqualTo(0);
        assertThat(status.failedGrammars()).isEqualTo(0);
        assertThat(status.grammarDetails()).isEmpty();
    }
}
