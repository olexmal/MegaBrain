/*
 * Copyright (c) 2025 MegaBrain Contributors
 * Licensed under the MIT License - see LICENSE file for details.
 */

package io.megabrain.core;

import org.assertj.core.data.Offset;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link VectorScoreNormalizer}.
 * Covers normalization consistency with Lucene (T1), edge cases (identical vectors,
 * orthogonal vectors), and reproducible behavior.
 */
class VectorScoreNormalizerTest {

    private static final float[] DUMMY_VECTOR = new float[]{1.0f, 0.0f};

    private static VectorStore.SearchResult result(String id, double similarity) {
        var metadata = new VectorStore.VectorMetadata(
            "content", "java", "class", id, "/path/" + id + ".java", 1, 1, 0, 10
        );
        return new VectorStore.SearchResult(id, DUMMY_VECTOR, metadata, similarity);
    }

    @Test
    void testNormalizeScoresEmptyList() {
        List<VectorStore.SearchResult> normalized = VectorScoreNormalizer.normalizeScores(List.of());
        assertThat(normalized).isEmpty();
    }

    @Test
    void testNormalizeScoresNullList() {
        List<VectorStore.SearchResult> normalized = VectorScoreNormalizer.normalizeScores(null);
        assertThat(normalized).isEmpty();
    }

    @Test
    void testNormalizeScoresSingleResult() {
        VectorStore.SearchResult r = result("a", 0.8);
        List<VectorStore.SearchResult> normalized = VectorScoreNormalizer.normalizeScores(List.of(r));

        assertThat(normalized).hasSize(1);
        assertThat(normalized.get(0).similarity()).isEqualTo(1.0);
        assertThat(normalized.get(0).id()).isEqualTo("a");
    }

    @Test
    void testNormalizeScoresMultipleResults() {
        List<VectorStore.SearchResult> results = List.of(
            result("a", 0.5),
            result("b", 0.8),
            result("c", 1.0)
        );

        List<VectorStore.SearchResult> normalized = VectorScoreNormalizer.normalizeScores(results);

        assertThat(normalized).hasSize(3);
        assertThat(normalized.get(0).similarity()).isEqualTo(0.0);
        assertThat(normalized.get(1).similarity()).isCloseTo(0.6, Offset.offset(0.001));
        assertThat(normalized.get(2).similarity()).isEqualTo(1.0);
    }

    @Test
    void testNormalizeScoresEqualScoresIdenticalVectors() {
        List<VectorStore.SearchResult> results = List.of(
            result("a", 1.0),
            result("b", 1.0),
            result("c", 1.0)
        );

        List<VectorStore.SearchResult> normalized = VectorScoreNormalizer.normalizeScores(results);

        assertThat(normalized).hasSize(3);
        assertThat(normalized.get(0).similarity()).isEqualTo(1.0);
        assertThat(normalized.get(1).similarity()).isEqualTo(1.0);
        assertThat(normalized.get(2).similarity()).isEqualTo(1.0);
    }

    @Test
    void testNormalizeScoresOrthogonalVectorsAllZeroSimilarity() {
        List<VectorStore.SearchResult> results = List.of(
            result("a", 0.0),
            result("b", 0.0),
            result("c", 0.0)
        );

        List<VectorStore.SearchResult> normalized = VectorScoreNormalizer.normalizeScores(results);

        assertThat(normalized).hasSize(3);
        assertThat(normalized.get(0).similarity()).isEqualTo(1.0);
        assertThat(normalized.get(1).similarity()).isEqualTo(1.0);
        assertThat(normalized.get(2).similarity()).isEqualTo(1.0);
    }

    @Test
    void testNormalizeScoresOrthogonalAndIdenticalMixedZeroAndOne() {
        List<VectorStore.SearchResult> results = List.of(
            result("orthogonal", 0.0),
            result("identical", 1.0)
        );

        List<VectorStore.SearchResult> normalized = VectorScoreNormalizer.normalizeScores(results);

        assertThat(normalized).hasSize(2);
        assertThat(normalized.get(0).similarity()).isEqualTo(0.0);
        assertThat(normalized.get(1).similarity()).isEqualTo(1.0);
    }

    @Test
    void testNormalizeScoresZeroScores() {
        List<VectorStore.SearchResult> results = List.of(
            result("a", 0.0),
            result("b", 0.2)
        );

        List<VectorStore.SearchResult> normalized = VectorScoreNormalizer.normalizeScores(results);

        assertThat(normalized).hasSize(2);
        assertThat(normalized.get(0).similarity()).isEqualTo(0.0);
        assertThat(normalized.get(1).similarity()).isEqualTo(1.0);
    }

    @Test
    void testNormalizeScoresNegativeScores() {
        List<VectorStore.SearchResult> results = List.of(
            result("a", -0.1),
            result("b", 0.9)
        );

        List<VectorStore.SearchResult> normalized = VectorScoreNormalizer.normalizeScores(results);

        assertThat(normalized).hasSize(2);
        assertThat(normalized.get(0).similarity()).isEqualTo(0.0);
        assertThat(normalized.get(1).similarity()).isEqualTo(1.0);
    }

    @Test
    void testNormalizeScoresPreservesOrder() {
        List<VectorStore.SearchResult> results = List.of(
            result("1", 0.3),
            result("2", 0.6),
            result("3", 0.9)
        );

        List<VectorStore.SearchResult> normalized = VectorScoreNormalizer.normalizeScores(results);

        assertThat(normalized).hasSize(3);
        assertThat(normalized.get(0).id()).isEqualTo("1");
        assertThat(normalized.get(0).similarity()).isEqualTo(0.0);

        assertThat(normalized.get(1).id()).isEqualTo("2");
        assertThat(normalized.get(1).similarity()).isCloseTo(0.5, Offset.offset(0.001));

        assertThat(normalized.get(2).id()).isEqualTo("3");
        assertThat(normalized.get(2).similarity()).isEqualTo(1.0);
    }

    @Test
    void testNormalizeScoresReproducible() {
        List<VectorStore.SearchResult> results = List.of(
            result("a", 0.4),
            result("b", 0.7),
            result("c", 0.9)
        );

        List<VectorStore.SearchResult> n1 = VectorScoreNormalizer.normalizeScores(results);
        List<VectorStore.SearchResult> n2 = VectorScoreNormalizer.normalizeScores(results);

        assertThat(n1).hasSize(n2.size());
        for (int i = 0; i < n1.size(); i++) {
            assertThat(n1.get(i).similarity()).isEqualTo(n2.get(i).similarity());
            assertThat(n1.get(i).id()).isEqualTo(n2.get(i).id());
        }
    }

    @Test
    void testNormalizeScoresConsistentWithLuceneScale() {
        List<VectorStore.SearchResult> results = List.of(
            result("min", 0.2),
            result("mid", 0.5),
            result("max", 0.8)
        );

        List<VectorStore.SearchResult> normalized = VectorScoreNormalizer.normalizeScores(results);

        assertThat(normalized.get(0).similarity()).isEqualTo(0.0);
        assertThat(normalized.get(1).similarity()).isCloseTo(0.5, Offset.offset(0.001));
        assertThat(normalized.get(2).similarity()).isEqualTo(1.0);
    }
}
