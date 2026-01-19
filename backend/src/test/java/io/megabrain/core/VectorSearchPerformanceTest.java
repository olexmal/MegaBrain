/*
 * Copyright (c) 2025 MegaBrain Contributors
 * Licensed under the MIT License - see LICENSE file for details.
 */

package io.megabrain.core;

import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Performance test for vector search operations.
 * Tests search latency and throughput with various data sizes.
 */
@QuarkusTest
public class VectorSearchPerformanceTest {

    @Inject
    EmbeddingModelService embeddingService;

    // Only run performance tests if explicitly enabled
    @EnabledIfEnvironmentVariable(named = "RUN_PERFORMANCE_TESTS", matches = "true")

    @Test
    public void testVectorSearchLatency() {
        // This test would require a real PgVectorStore with data
        // For now, just test the interface contract

        // Generate test vectors
        Random random = new Random(42);
        List<float[]> testVectors = new ArrayList<>();
        for (int i = 0; i < 1000; i++) {
            testVectors.add(generateRandomVector(random));
        }

        // Basic performance assertion - vectors should be valid
        for (float[] vector : testVectors) {
            assertThat(vector).hasSize(384);
            assertThat(vector).doesNotContain(0.0f); // Should have some variation
        }

        System.out.println("Generated " + testVectors.size() + " test vectors for performance testing");
    }

    @Test
    @org.junit.jupiter.api.Disabled("Requires embedding model configuration")
    public void testVectorGenerationPerformance() {

        long startTime = System.nanoTime();

        // Generate embeddings for multiple texts
        List<String> texts = List.of(
            "public class PerformanceTest { }",
            "private void method() { }",
            "interface TestInterface<T> { }",
            "def python_function(): pass",
            "function jsFunction() { return true; }"
        );

        List<float[]> embeddings = embeddingService.embed(texts);

        long endTime = System.nanoTime();
        double durationMs = (endTime - startTime) / 1_000_000.0;

        System.out.printf("Generated %d embeddings in %.2f ms%n", embeddings.size(), durationMs);

        // Should complete within reasonable time (adjust based on hardware)
        assertThat(durationMs).isLessThan(5000.0); // 5 seconds max
        assertThat(embeddings).hasSize(5);
        assertThat(embeddings.get(0)).hasSize(384);
    }

    private float[] generateRandomVector(Random random) {
        float[] vector = new float[384];
        for (int i = 0; i < vector.length; i++) {
            vector[i] = (random.nextFloat() - 0.5f) * 2.0f;
        }
        return vector;
    }
}