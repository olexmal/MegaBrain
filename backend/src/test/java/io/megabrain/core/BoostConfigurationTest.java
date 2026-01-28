/*
 * Copyright (c) 2025 MegaBrain Contributors
 * Licensed under the MIT License - see LICENSE file for details.
 */

package io.megabrain.core;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for BoostConfiguration.
 * <p>
 * Tests configuration loading, default values, and field boost mapping.
 */
@QuarkusTest
class BoostConfigurationTest {

    @Inject
    BoostConfiguration boostConfig;

    @Test
    void testDefaultValues() {
        // Verify default boost values are loaded correctly
        assertThat(boostConfig.entityName()).isEqualTo(3.0f);
        assertThat(boostConfig.docSummary()).isEqualTo(2.0f);
        assertThat(boostConfig.content()).isEqualTo(1.0f);
    }

    @Test
    void testGetBoostForField() {
        // Test boost values for known fields
        assertThat(boostConfig.getBoostForField(LuceneSchema.FIELD_ENTITY_NAME)).isEqualTo(3.0f);
        assertThat(boostConfig.getBoostForField(LuceneSchema.FIELD_DOC_SUMMARY)).isEqualTo(2.0f);
        assertThat(boostConfig.getBoostForField(LuceneSchema.FIELD_CONTENT)).isEqualTo(1.0f);

        // Test default boost for unknown field
        assertThat(boostConfig.getBoostForField("unknown_field")).isEqualTo(1.0f);
    }

    @Test
    void testGetBoostForFieldWithNullInput() {
        // Test that null field name returns default boost
        assertThat(boostConfig.getBoostForField(null)).isEqualTo(1.0f);
    }

    @Test
    void testGetBoostForFieldWithEmptyInput() {
        // Test that empty field name returns default boost
        assertThat(boostConfig.getBoostForField("")).isEqualTo(1.0f);
    }

    @Test
    void testGetBoostForFieldWithBlankInput() {
        // Test that blank field name returns default boost
        assertThat(boostConfig.getBoostForField("   ")).isEqualTo(1.0f);
    }

    @Test
    void testAllBoostValuesArePositive() {
        // Test that all configured boost values are positive (business requirement)
        assertThat(boostConfig.entityName()).isPositive();
        assertThat(boostConfig.docSummary()).isPositive();
        assertThat(boostConfig.content()).isPositive();
    }

    @Test
    void testFieldConstantsCoverage() {
        // Test that all boost-related field constants are covered
        // This ensures the switch statement in getBoostForField covers all expected fields
        String[] expectedFields = {
            LuceneSchema.FIELD_ENTITY_NAME,
            LuceneSchema.FIELD_DOC_SUMMARY,
            LuceneSchema.FIELD_CONTENT
        };

        for (String field : expectedFields) {
            float boost = boostConfig.getBoostForField(field);
            assertThat(boost).isPositive();
        }
    }

    @Test
    void testConfigurationInjection() {
        // Test that BoostConfiguration is properly injected by CDI
        assertThat(boostConfig).isNotNull();

        // Verify all methods return valid values
        assertThat(boostConfig.entityName()).isPositive();
        assertThat(boostConfig.docSummary()).isPositive();
        assertThat(boostConfig.content()).isPositive();
    }

}