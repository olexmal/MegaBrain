/*
 * Copyright (c) 2025 MegaBrain Contributors
 * Licensed under the MIT License - see LICENSE file for details.
 */

package io.megabrain.core;

import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.mutiny.Uni;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test for the VectorStore interface contract.
 * Uses a mock implementation to verify the interface works correctly.
 */
@QuarkusTest
public class VectorStoreTest {

    @Test
    public void testVectorStoreInterfaceContract() {
        // Create a mock implementation for testing
        VectorStore mockStore = new MockVectorStore();

        // Test basic operations
        assertThat(mockStore.getVectorDimension()).isEqualTo(384);

        // Test metadata record
        var metadata = new VectorStore.VectorMetadata(
            "public class Test {}", "java", "class", "Test",
            "/path/Test.java", 1, 1, 0, 20
        );
        assertThat(metadata.content()).isEqualTo("public class Test {}");
        assertThat(metadata.language()).isEqualTo("java");

        // Test vector entry record
        float[] vector = new float[]{1.0f, 2.0f, 3.0f};
        var entry = new VectorStore.VectorEntry("test-id", vector, metadata);
        assertThat(entry.id()).isEqualTo("test-id");
        assertThat(entry.vector()).isEqualTo(vector);
        assertThat(entry.metadata()).isEqualTo(metadata);

        // Test search result record
        var result = new VectorStore.SearchResult("test-id", vector, metadata, 0.95);
        assertThat(result.id()).isEqualTo("test-id");
        assertThat(result.similarity()).isEqualTo(0.95);

        // Test stats record
        var stats = new VectorStore.VectorStoreStats(1000, 384, "mock", 1024000);
        assertThat(stats.totalVectors()).isEqualTo(1000);
        assertThat(stats.vectorDimension()).isEqualTo(384);
        assertThat(stats.backendType()).isEqualTo("mock");
    }

    /**
     * Mock implementation of VectorStore for testing the interface.
     */
    private static class MockVectorStore implements VectorStore {

        @Override
        public Uni<Void> store(String id, float[] vector, VectorMetadata metadata) {
            return Uni.createFrom().voidItem();
        }

        @Override
        public Uni<Void> storeBatch(List<VectorEntry> vectors) {
            return Uni.createFrom().voidItem();
        }

        @Override
        public Uni<List<SearchResult>> search(float[] queryVector, int k) {
            return Uni.createFrom().item(List.of());
        }

        @Override
        public Uni<List<SearchResult>> search(float[] queryVector, int k, double similarityThreshold) {
            return Uni.createFrom().item(List.of());
        }

        @Override
        public Uni<Boolean> delete(String id) {
            return Uni.createFrom().item(false);
        }

        @Override
        public Uni<Integer> deleteBatch(List<String> ids) {
            return Uni.createFrom().item(0);
        }

        @Override
        public int getVectorDimension() {
            return 384;
        }

        @Override
        public Uni<Boolean> healthCheck() {
            return Uni.createFrom().item(true);
        }

        @Override
        public Uni<VectorStoreStats> getStats() {
            return Uni.createFrom().item(new VectorStoreStats(0, 384, "mock", 0));
        }
    }
}