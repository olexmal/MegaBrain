/*
 * Copyright (c) 2025 MegaBrain Contributors
 * Licensed under the MIT License - see LICENSE file for details.
 */

package io.megabrain.core;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.quarkus.test.junit.QuarkusTestProfile;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
@TestProfile(BoostConfigurationOverrideTest.OverrideBoostsProfile.class)
class BoostConfigurationOverrideTest {

    public static class OverrideBoostsProfile implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of(
                "megabrain.search.boost.entity-name", "10.0",
                "megabrain.search.boost.doc-summary", "4.5",
                "megabrain.search.boost.content", "0.9"
            );
        }
    }

    @Inject
    BoostConfiguration boostConfig;

    @Test
    void shouldLoadBoostsFromConfigurationOverrides() {
        assertThat(boostConfig.entityName()).isEqualTo(10.0f);
        assertThat(boostConfig.docSummary()).isEqualTo(4.5f);
        assertThat(boostConfig.content()).isEqualTo(0.9f);
    }
}

