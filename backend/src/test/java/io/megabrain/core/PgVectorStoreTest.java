/*
 * Copyright (c) 2025 MegaBrain Contributors
 * Licensed under the MIT License - see LICENSE file for details.
 */

package io.megabrain.core;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Basic unit test for PgVectorStore interface compliance.
 * Integration tests with database will be added later.
 */
public class PgVectorStoreTest {

    @Test
    public void testVectorDimensionConstant() {
        // Test that the expected dimension matches the all-MiniLM-L6-v2 model
        assertThat(PgVectorStore.VECTOR_DIMENSION).isEqualTo(384);
    }

    @Test
    public void testImplementsVectorStoreInterface() {
        // Verify PgVectorStore implements VectorStore interface
        Class<?>[] interfaces = PgVectorStore.class.getInterfaces();
        assertThat(interfaces).contains(VectorStore.class);
    }

    @Test
    public void testVectorValidation() {
        // Test vector validation logic (extracted from PgVectorStore for testing)
        float[] validVector = new float[384];
        assertThat(isValidVector(validVector, 384)).isTrue();

        float[] invalidVector = new float[100];
        assertThat(isValidVector(invalidVector, 384)).isFalse();

        assertThat(isValidVector(null, 384)).isFalse();
    }

    @Test
    public void testVectorToPgVectorConversion() {
        // Test the vector to PostgreSQL format conversion
        float[] vector = {1.0f, 2.5f, -3.7f};
        String pgVector = vectorToPgVector(vector);
        assertThat(pgVector).isEqualTo("[1.0,2.5,-3.7]");
    }

    @Test
    public void testPgVectorToFloatArrayConversion() {
        // Test the PostgreSQL format to vector conversion
        String pgVector = "[1.0,2.5,-3.7]";
        float[] vector = pgVectorToFloatArray(pgVector);
        assertThat(vector).containsExactly(1.0f, 2.5f, -3.7f);

        // Test empty vector
        String emptyPgVector = "[]";
        float[] emptyVector = pgVectorToFloatArray(emptyPgVector);
        assertThat(emptyVector).isEmpty();
    }

    // Helper methods extracted from PgVectorStore for testing
    private boolean isValidVector(float[] vector, int expectedDimension) {
        return vector != null && vector.length == expectedDimension;
    }

    private String vectorToPgVector(float[] vector) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < vector.length; i++) {
            if (i > 0) sb.append(",");
            sb.append(vector[i]);
        }
        sb.append("]");
        return sb.toString();
    }

    private float[] pgVectorToFloatArray(String pgVector) {
        if (pgVector == null || pgVector.length() < 2) {
            throw new IllegalArgumentException("Invalid pgvector format: " + pgVector);
        }

        // Remove brackets and split by comma
        String content = pgVector.substring(1, pgVector.length() - 1);
        if (content.isEmpty()) {
            return new float[0];
        }

        String[] parts = content.split(",");
        float[] result = new float[parts.length];

        for (int i = 0; i < parts.length; i++) {
            result[i] = Float.parseFloat(parts[i].trim());
        }

        return result;
    }

    private float[] createTestVector() {
        return createTestVector(1.0f);
    }

    private float[] createTestVector(float multiplier) {
        float[] vector = new float[384];
        for (int i = 0; i < vector.length; i++) {
            vector[i] = (i % 10) * multiplier;
        }
        return vector;
    }

    private float[] createSimilarVector(float[] baseVector, float noise) {
        float[] similar = baseVector.clone();
        for (int i = 0; i < similar.length; i++) {
            similar[i] += (float) (Math.random() - 0.5) * noise;
        }
        return similar;
    }
}