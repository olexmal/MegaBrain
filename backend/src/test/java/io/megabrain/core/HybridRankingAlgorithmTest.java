/*
 * Copyright (c) 2025 MegaBrain Contributors
 * Licensed under the MIT License - see LICENSE file for details.
 */

package io.megabrain.core;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.assertj.core.data.Offset;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Comprehensive unit tests for the hybrid ranking algorithm (US-02-03, T7).
 * Covers score normalization (Lucene and vector), weighted combination,
 * result merging, deduplication, and edge cases using mock search results
 * with known scores.
 */
@QuarkusTest
class HybridRankingAlgorithmTest {

    private static final float[] DUMMY_VECTOR = new float[384];

    @Inject
    HybridScorer hybridScorer;

    @Inject
    ResultMerger resultMerger;

    // --- Helpers for mock search results ---

    private static Document createLuceneDocument(String filePath, String entityName, String documentId) {
        Document doc = new Document();
        doc.add(new Field(LuceneSchema.FIELD_FILE_PATH, filePath, LuceneSchema.KEYWORD_FIELD_TYPE));
        doc.add(new Field(LuceneSchema.FIELD_ENTITY_NAME, entityName, LuceneSchema.KEYWORD_FIELD_TYPE));
        doc.add(new Field(LuceneSchema.FIELD_DOCUMENT_ID, documentId, LuceneSchema.KEYWORD_FIELD_TYPE));
        return doc;
    }

    private static VectorStore.SearchResult createVectorResult(String filePath, String entityName,
                                                               double similarity, Integer startLine, Integer endLine) {
        VectorStore.VectorMetadata metadata = new VectorStore.VectorMetadata(
                "content",
                "java",
                "class",
                entityName,
                filePath,
                startLine != null ? startLine : 1,
                endLine != null ? endLine : 10,
                0,
                100
        );
        return new VectorStore.SearchResult(
                "vector-id-" + filePath + ":" + entityName,
                DUMMY_VECTOR,
                metadata,
                similarity
        );
    }

    private static VectorStore.SearchResult createVectorResult(String filePath, String entityName, double similarity) {
        return createVectorResult(filePath, entityName, similarity, null, null);
    }

    // --- Score normalization ---

    @Nested
    @DisplayName("Score normalization")
    class ScoreNormalization {

        @Test
        @DisplayName("Lucene: known raw scores normalize to 0.0–1.0 range")
        void luceneKnownScores_producesZeroToOneRange() {
            Document d1 = createLuceneDocument("a.java", "A", "a.java:A:1:10");
            Document d2 = createLuceneDocument("b.java", "B", "b.java:B:2:20");
            Document d3 = createLuceneDocument("c.java", "C", "c.java:C:3:30");

            List<LuceneIndexService.LuceneScoredResult> raw = List.of(
                    new LuceneIndexService.LuceneScoredResult(d1, 0.3f),
                    new LuceneIndexService.LuceneScoredResult(d2, 0.6f),
                    new LuceneIndexService.LuceneScoredResult(d3, 0.9f)
            );

            List<LuceneIndexService.LuceneScoredResult> normalized =
                    LuceneIndexService.normalizeScores(raw);

            assertThat(normalized).hasSize(3);
            assertThat(normalized.get(0).score()).isEqualTo(0.0f);
            assertThat(normalized.get(1).score()).isCloseTo(0.5f, Offset.offset(0.001f));
            assertThat(normalized.get(2).score()).isEqualTo(1.0f);
        }

        @Test
        @DisplayName("Lucene: empty list returns empty")
        void luceneEmptyList_returnsEmpty() {
            List<LuceneIndexService.LuceneScoredResult> normalized =
                    LuceneIndexService.normalizeScores(List.of());
            assertThat(normalized).isEmpty();
        }

        @Test
        @DisplayName("Lucene: single result gets score 1.0")
        void luceneSingleResult_getsOne() {
            Document d = createLuceneDocument("x.java", "X", "x.java:X:1:10");
            List<LuceneIndexService.LuceneScoredResult> raw =
                    List.of(new LuceneIndexService.LuceneScoredResult(d, 0.5f));
            List<LuceneIndexService.LuceneScoredResult> normalized = LuceneIndexService.normalizeScores(raw);
            assertThat(normalized).hasSize(1);
            assertThat(normalized.get(0).score()).isEqualTo(1.0f);
        }

        @Test
        @DisplayName("Vector: known raw scores normalize to 0.0–1.0 range")
        void vectorKnownScores_producesZeroToOneRange() {
            List<VectorStore.SearchResult> raw = List.of(
                    createVectorResult("a.java", "A", 0.2),
                    createVectorResult("b.java", "B", 0.5),
                    createVectorResult("c.java", "C", 0.8)
            );

            List<VectorStore.SearchResult> normalized = VectorScoreNormalizer.normalizeScores(raw);

            assertThat(normalized).hasSize(3);
            assertThat(normalized.get(0).similarity()).isEqualTo(0.0);
            assertThat(normalized.get(1).similarity()).isCloseTo(0.5, Offset.offset(0.001));
            assertThat(normalized.get(2).similarity()).isEqualTo(1.0);
        }

        @Test
        @DisplayName("Vector: empty list returns empty")
        void vectorEmptyList_returnsEmpty() {
            List<VectorStore.SearchResult> normalized = VectorScoreNormalizer.normalizeScores(List.of());
            assertThat(normalized).isEmpty();
        }

        @Test
        @DisplayName("Vector: single result gets similarity 1.0")
        void vectorSingleResult_getsOne() {
            VectorStore.SearchResult r = createVectorResult("x.java", "X", 0.7);
            List<VectorStore.SearchResult> normalized = VectorScoreNormalizer.normalizeScores(List.of(r));
            assertThat(normalized).hasSize(1);
            assertThat(normalized.get(0).similarity()).isEqualTo(1.0);
        }

        @Test
        @DisplayName("Lucene and vector use consistent 0–1 scale for combination")
        void bothNormalizers_consistentScale() {
            Document d1 = createLuceneDocument("p.java", "P", "p.java:P:1:10");
            Document d2 = createLuceneDocument("q.java", "Q", "q.java:Q:2:20");

            List<LuceneIndexService.LuceneScoredResult> luceneRaw = List.of(
                    new LuceneIndexService.LuceneScoredResult(d1, 0.4f),
                    new LuceneIndexService.LuceneScoredResult(d2, 0.8f)
            );
            List<LuceneIndexService.LuceneScoredResult> luceneNorm = LuceneIndexService.normalizeScores(luceneRaw);

            List<VectorStore.SearchResult> vectorRaw = List.of(
                    createVectorResult("p.java", "P", 0.3),
                    createVectorResult("q.java", "Q", 0.9)
            );
            List<VectorStore.SearchResult> vectorNorm = VectorScoreNormalizer.normalizeScores(vectorRaw);

            assertThat(luceneNorm.get(0).score()).isEqualTo(0.0f);
            assertThat(luceneNorm.get(1).score()).isEqualTo(1.0f);
            assertThat(vectorNorm.get(0).similarity()).isEqualTo(0.0);
            assertThat(vectorNorm.get(1).similarity()).isEqualTo(1.0);
        }
    }

    // --- Weighted combination ---

    @Nested
    @DisplayName("Weighted combination")
    class WeightedCombination {

        @Test
        @DisplayName("Default weights: formula verified with known scores")
        void defaultWeights_formulaVerified() {
            double lucene = 0.5;
            double vector = 0.8;
            double got = hybridScorer.combine(lucene, vector);
            double expected = 0.6 * lucene + 0.4 * vector;
            assertThat(got).isCloseTo(expected, Offset.offset(0.001));
        }

        @Test
        @DisplayName("Per-request weights: formula verified")
        void perRequestWeights_formulaVerified() {
            double lucene = 0.4;
            double vector = 0.6;
            double kw = 0.5;
            double vw = 0.5;
            double got = hybridScorer.combine(lucene, vector, kw, vw);
            assertThat(got).isCloseTo(0.5, Offset.offset(0.001));
        }

        @Test
        @DisplayName("Keyword-only weight: vector score ignored")
        void keywordOnly_vectorIgnored() {
            double got = hybridScorer.combine(0.7, 0.2, 1.0, 0.0);
            assertThat(got).isCloseTo(0.7, Offset.offset(0.001));
        }

        @Test
        @DisplayName("Vector-only weight: keyword score ignored")
        void vectorOnly_keywordIgnored() {
            double got = hybridScorer.combine(0.2, 0.9, 0.0, 1.0);
            assertThat(got).isCloseTo(0.9, Offset.offset(0.001));
        }

        @Test
        @DisplayName("Both zero yields zero")
        void bothZero_yieldsZero() {
            assertThat(hybridScorer.combine(0.0, 0.0)).isEqualTo(0.0);
        }

        @Test
        @DisplayName("Both one yields one")
        void bothOne_yieldsOne() {
            assertThat(hybridScorer.combine(1.0, 1.0)).isEqualTo(1.0);
        }
    }

    // --- Result merging ---

    @Nested
    @DisplayName("Result merging")
    class ResultMerging {

        @Test
        @DisplayName("Merge normalized Lucene and vector results; sorted by combined score")
        void mergeNormalized_sortedByCombinedScore() {
            Document d1 = createLuceneDocument("f1.java", "C1", "f1.java:C1:10:20");
            Document d2 = createLuceneDocument("f2.java", "M1", "f2.java:M1:5:15");

            List<LuceneIndexService.LuceneScoredResult> lucene = List.of(
                    new LuceneIndexService.LuceneScoredResult(d1, 0.9f),
                    new LuceneIndexService.LuceneScoredResult(d2, 0.3f)
            );

            List<VectorStore.SearchResult> vector = List.of(
                    createVectorResult("f3.java", "C2", 0.85),
                    createVectorResult("f4.java", "M2", 0.6)
            );

            List<ResultMerger.MergedResult> merged = resultMerger.merge(lucene, vector);

            assertThat(merged).hasSize(4);
            assertThat(merged.get(0).combinedScore()).isGreaterThanOrEqualTo(merged.get(1).combinedScore());
            assertThat(merged.get(1).combinedScore()).isGreaterThanOrEqualTo(merged.get(2).combinedScore());
            assertThat(merged.get(2).combinedScore()).isGreaterThanOrEqualTo(merged.get(3).combinedScore());
        }

        @Test
        @DisplayName("Lucene-only merge preserves order by score")
        void luceneOnly_preservesOrderByScore() {
            Document d1 = createLuceneDocument("a.java", "A", "a.java:A:1:10");
            Document d2 = createLuceneDocument("b.java", "B", "b.java:B:2:20");

            List<LuceneIndexService.LuceneScoredResult> lucene = List.of(
                    new LuceneIndexService.LuceneScoredResult(d1, 0.9f),
                    new LuceneIndexService.LuceneScoredResult(d2, 0.5f)
            );

            List<ResultMerger.MergedResult> merged = resultMerger.merge(lucene, List.of());

            assertThat(merged).hasSize(2);
            assertThat(merged.get(0).combinedScore()).isCloseTo(0.9, Offset.offset(0.001));
            assertThat(merged.get(1).combinedScore()).isCloseTo(0.5, Offset.offset(0.001));
        }

        @Test
        @DisplayName("Vector-only merge preserves order by similarity")
        void vectorOnly_preservesOrderBySimilarity() {
            List<VectorStore.SearchResult> vector = List.of(
                    createVectorResult("a.java", "A", 0.95),
                    createVectorResult("b.java", "B", 0.6)
            );

            List<ResultMerger.MergedResult> merged = resultMerger.merge(List.of(), vector);

            assertThat(merged).hasSize(2);
            assertThat(merged.get(0).combinedScore()).isCloseTo(0.95, Offset.offset(0.001));
            assertThat(merged.get(1).combinedScore()).isCloseTo(0.6, Offset.offset(0.001));
        }
    }

    // --- Deduplication ---

    @Nested
    @DisplayName("Deduplication")
    class Deduplication {

        @Test
        @DisplayName("Duplicate chunk appears once with combined score")
        void duplicateChunk_appearsOnceWithCombinedScore() {
            String docId = "f1.java:C1:10:20";
            Document doc = createLuceneDocument("f1.java", "C1", docId);
            VectorStore.SearchResult vec = createVectorResult("f1.java", "C1", 0.9, 10, 20);

            List<LuceneIndexService.LuceneScoredResult> lucene = List.of(
                    new LuceneIndexService.LuceneScoredResult(doc, 0.8f)
            );
            List<VectorStore.SearchResult> vector = List.of(vec);

            List<ResultMerger.MergedResult> merged = resultMerger.merge(lucene, vector);

            assertThat(merged).hasSize(1);
            ResultMerger.MergedResult m = merged.get(0);
            assertThat(m.fromBothSources()).isTrue();
            double expected = hybridScorer.combine(0.8, 0.9);
            assertThat(m.combinedScore()).isCloseTo(expected, Offset.offset(0.001));
        }

        @Test
        @DisplayName("All duplicates: deduplicated and combined")
        void allDuplicates_deduplicated() {
            Document d1 = createLuceneDocument("f1.java", "A", "f1.java:A:10:20");
            Document d2 = createLuceneDocument("f2.java", "B", "f2.java:B:5:15");

            VectorStore.SearchResult v1 = createVectorResult("f1.java", "A", 0.9, 10, 20);
            VectorStore.SearchResult v2 = createVectorResult("f2.java", "B", 0.7, 5, 15);

            List<LuceneIndexService.LuceneScoredResult> lucene = List.of(
                    new LuceneIndexService.LuceneScoredResult(d1, 0.8f),
                    new LuceneIndexService.LuceneScoredResult(d2, 0.6f)
            );
            List<VectorStore.SearchResult> vector = List.of(v1, v2);

            List<ResultMerger.MergedResult> merged = resultMerger.merge(lucene, vector);

            assertThat(merged).hasSize(2);
            assertThat(merged.get(0).fromBothSources()).isTrue();
            assertThat(merged.get(1).fromBothSources()).isTrue();
        }

        @Test
        @DisplayName("Chunk ID matching uses file_path:entity_name:startLine:endLine")
        void chunkIdMatching_forDeduplication() {
            Document doc = createLuceneDocument("p.java", "X", "p.java:X:10:20");
            VectorStore.SearchResult vec = createVectorResult("p.java", "X", 0.85, 10, 20);

            List<ResultMerger.MergedResult> merged = resultMerger.merge(
                    List.of(new LuceneIndexService.LuceneScoredResult(doc, 0.75f)),
                    List.of(vec)
            );

            assertThat(merged).hasSize(1);
            assertThat(merged.get(0).fromBothSources()).isTrue();
        }
    }

    // --- Edge cases ---

    @Nested
    @DisplayName("Edge cases")
    class EdgeCases {

        @Test
        @DisplayName("Empty both: returns empty list")
        void emptyBoth_returnsEmpty() {
            List<ResultMerger.MergedResult> merged = resultMerger.merge(List.of(), List.of());
            assertThat(merged).isEmpty();
        }

        @Test
        @DisplayName("Null both: returns empty list")
        void nullBoth_returnsEmpty() {
            List<ResultMerger.MergedResult> merged = resultMerger.merge(null, null);
            assertThat(merged).isEmpty();
        }

        @Test
        @DisplayName("Single result Lucene-only")
        void singleResult_luceneOnly() {
            Document d = createLuceneDocument("s.java", "S", "s.java:S:1:10");
            List<ResultMerger.MergedResult> merged = resultMerger.merge(
                    List.of(new LuceneIndexService.LuceneScoredResult(d, 0.7f)),
                    List.of()
            );
            assertThat(merged).hasSize(1);
            assertThat(merged.get(0).combinedScore()).isCloseTo(0.7, Offset.offset(0.001));
            assertThat(merged.get(0).fromBothSources()).isFalse();
        }

        @Test
        @DisplayName("Single result vector-only")
        void singleResult_vectorOnly() {
            VectorStore.SearchResult r = createVectorResult("s.java", "S", 0.75);
            List<ResultMerger.MergedResult> merged = resultMerger.merge(List.of(), List.of(r));
            assertThat(merged).hasSize(1);
            assertThat(merged.get(0).combinedScore()).isCloseTo(0.75, Offset.offset(0.001));
            assertThat(merged.get(0).fromBothSources()).isFalse();
        }

        @Test
        @DisplayName("Single result in both: duplicate with combined score")
        void singleResult_bothSourcesDuplicate() {
            Document d = createLuceneDocument("s.java", "S", "s.java:S:1:10");
            VectorStore.SearchResult v = createVectorResult("s.java", "S", 0.8, 1, 10);

            List<ResultMerger.MergedResult> merged = resultMerger.merge(
                    List.of(new LuceneIndexService.LuceneScoredResult(d, 0.6f)),
                    List.of(v)
            );

            assertThat(merged).hasSize(1);
            assertThat(merged.get(0).fromBothSources()).isTrue();
            double expected = hybridScorer.combine(0.6, 0.8);
            assertThat(merged.get(0).combinedScore()).isCloseTo(expected, Offset.offset(0.001));
        }

        @Test
        @DisplayName("Lucene empty, vector non-empty")
        void luceneEmptyVectorNonEmpty() {
            VectorStore.SearchResult r = createVectorResult("v.java", "V", 0.88);
            List<ResultMerger.MergedResult> merged = resultMerger.merge(List.of(), List.of(r));
            assertThat(merged).hasSize(1);
            assertThat(merged.get(0).vectorResult()).isNotNull();
            assertThat(merged.get(0).luceneDocument()).isNull();
        }

        @Test
        @DisplayName("Lucene non-empty, vector empty")
        void luceneNonEmptyVectorEmpty() {
            Document d = createLuceneDocument("l.java", "L", "l.java:L:1:10");
            List<ResultMerger.MergedResult> merged = resultMerger.merge(
                    List.of(new LuceneIndexService.LuceneScoredResult(d, 0.72f)),
                    List.of()
            );
            assertThat(merged).hasSize(1);
            assertThat(merged.get(0).luceneDocument()).isNotNull();
            assertThat(merged.get(0).vectorResult()).isNull();
        }
    }
}
