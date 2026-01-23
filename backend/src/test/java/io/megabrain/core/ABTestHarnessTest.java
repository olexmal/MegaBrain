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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for A/B testing harness (US-02-03, T8).
 * Tests relevance metrics computation, search mode comparison, and weight tuning recommendations.
 */
@QuarkusTest
class ABTestHarnessTest {

    private static final float[] DUMMY_VECTOR = new float[384];

    @Inject
    ABTestHarness abTestHarness;


    // --- Helper methods ---

    private static Document createLuceneDocument(String filePath, String entityName, String documentId) {
        Document doc = new Document();
        doc.add(new Field(LuceneSchema.FIELD_FILE_PATH, filePath, LuceneSchema.KEYWORD_FIELD_TYPE));
        doc.add(new Field(LuceneSchema.FIELD_ENTITY_NAME, entityName, LuceneSchema.KEYWORD_FIELD_TYPE));
        doc.add(new Field(LuceneSchema.FIELD_DOCUMENT_ID, documentId, LuceneSchema.KEYWORD_FIELD_TYPE));
        return doc;
    }

    private static VectorStore.SearchResult createVectorResult(String chunkId, double similarity) {
        VectorStore.VectorMetadata metadata = new VectorStore.VectorMetadata(
                "content",
                "java",
                "class",
                "Entity",
                "file.java",
                1,
                10,
                0,
                100
        );
        return new VectorStore.SearchResult(
                chunkId,
                DUMMY_VECTOR,
                metadata,
                similarity
        );
    }

    private static ResultMerger.MergedResult createMergedResult(String chunkId, double score) {
        Document doc = createLuceneDocument("file.java", "Entity", chunkId);
        return ResultMerger.MergedResult.fromLucene(chunkId, doc, score);
    }

    // --- TestQuery validation ---

    @Nested
    @DisplayName("TestQuery validation")
    class TestQueryValidation {

        @Test
        @DisplayName("TestQuery rejects null query")
        void testQuery_rejectsNullQuery() {
            assertThatThrownBy(() -> new ABTestHarness.TestQuery(
                    null, Set.of("id1"), "description"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Query cannot be null or blank");
        }

        @Test
        @DisplayName("TestQuery rejects blank query")
        void testQuery_rejectsBlankQuery() {
            assertThatThrownBy(() -> new ABTestHarness.TestQuery(
                    "   ", Set.of("id1"), "description"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Query cannot be null or blank");
        }

        @Test
        @DisplayName("TestQuery rejects null expected results")
        void testQuery_rejectsNullExpectedResults() {
            assertThatThrownBy(() -> new ABTestHarness.TestQuery(
                    "query", null, "description"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Expected relevant chunk IDs cannot be null");
        }

        @Test
        @DisplayName("TestQuery accepts valid input")
        void testQuery_acceptsValidInput() {
            ABTestHarness.TestQuery query = new ABTestHarness.TestQuery(
                    "test query", Set.of("id1", "id2"), "description");
            assertThat(query.query()).isEqualTo("test query");
            assertThat(query.expectedRelevantChunkIds()).containsExactlyInAnyOrder("id1", "id2");
            assertThat(query.description()).isEqualTo("description");
        }
    }

    // --- RelevanceMetrics computation ---

    @Nested
    @DisplayName("Relevance metrics computation")
    class RelevanceMetricsComputation {

        @Test
        @DisplayName("computeMetrics: perfect precision when all top results are relevant")
        void computeMetrics_perfectPrecision() {
            List<ResultMerger.MergedResult> results = List.of(
                    createMergedResult("id1", 0.9),
                    createMergedResult("id2", 0.8),
                    createMergedResult("id3", 0.7)
            );
            Set<String> expectedRelevant = Set.of("id1", "id2", "id3");

            ABTestHarness.RelevanceMetrics metrics = abTestHarness.computeMetrics(results, expectedRelevant);

            assertThat(metrics.precisionAt5()).isCloseTo(1.0, Offset.offset(0.001));
            assertThat(metrics.precisionAt10()).isCloseTo(1.0, Offset.offset(0.001));
            assertThat(metrics.recall()).isCloseTo(1.0, Offset.offset(0.001));
            assertThat(metrics.retrievedRelevant()).isEqualTo(3);
            assertThat(metrics.totalRetrieved()).isEqualTo(3);
        }

        @Test
        @DisplayName("computeMetrics: zero precision when no results are relevant")
        void computeMetrics_zeroPrecision() {
            List<ResultMerger.MergedResult> results = List.of(
                    createMergedResult("id1", 0.9),
                    createMergedResult("id2", 0.8)
            );
            Set<String> expectedRelevant = Set.of("id3", "id4");

            ABTestHarness.RelevanceMetrics metrics = abTestHarness.computeMetrics(results, expectedRelevant);

            assertThat(metrics.precisionAt5()).isEqualTo(0.0);
            assertThat(metrics.precisionAt10()).isEqualTo(0.0);
            assertThat(metrics.recall()).isEqualTo(0.0);
            assertThat(metrics.retrievedRelevant()).isEqualTo(0);
        }

        @Test
        @DisplayName("computeMetrics: partial precision when some results are relevant")
        void computeMetrics_partialPrecision() {
            List<ResultMerger.MergedResult> results = List.of(
                    createMergedResult("id1", 0.9),  // relevant
                    createMergedResult("id2", 0.8),  // not relevant
                    createMergedResult("id3", 0.7),  // relevant
                    createMergedResult("id4", 0.6),  // not relevant
                    createMergedResult("id5", 0.5)   // relevant
            );
            Set<String> expectedRelevant = Set.of("id1", "id3", "id5", "id6");

            ABTestHarness.RelevanceMetrics metrics = abTestHarness.computeMetrics(results, expectedRelevant);

            // precision@5: 3 relevant out of 5 = 0.6
            assertThat(metrics.precisionAt5()).isCloseTo(0.6, Offset.offset(0.001));
            // precision@10: same (only 5 results)
            assertThat(metrics.precisionAt10()).isCloseTo(0.6, Offset.offset(0.001));
            // recall: 3 retrieved out of 4 expected = 0.75
            assertThat(metrics.recall()).isCloseTo(0.75, Offset.offset(0.001));
            assertThat(metrics.retrievedRelevant()).isEqualTo(3);
            assertThat(metrics.totalRelevant()).isEqualTo(4);
        }

        @Test
        @DisplayName("computeMetrics: precision@5 considers only top 5")
        void computeMetrics_precisionAt5ConsidersTop5() {
            List<ResultMerger.MergedResult> results = List.of(
                    createMergedResult("id1", 0.9),  // relevant
                    createMergedResult("id2", 0.8),  // relevant
                    createMergedResult("id3", 0.7),  // relevant
                    createMergedResult("id4", 0.6),  // relevant
                    createMergedResult("id5", 0.5), // relevant
                    createMergedResult("id6", 0.4), // not relevant (6th, not in top 5)
                    createMergedResult("id7", 0.3)  // not relevant (7th, not in top 5)
            );
            // Only top 5 are relevant, id6 and id7 are not
            Set<String> expectedRelevant = Set.of("id1", "id2", "id3", "id4", "id5");

            ABTestHarness.RelevanceMetrics metrics = abTestHarness.computeMetrics(results, expectedRelevant);

            // precision@5: all 5 top results are relevant = 1.0
            assertThat(metrics.precisionAt5()).isCloseTo(1.0, Offset.offset(0.001));
            // precision@10: 5 relevant out of 7 = 0.714
            assertThat(metrics.precisionAt10()).isCloseTo(5.0 / 7.0, Offset.offset(0.001));
        }

        @Test
        @DisplayName("computeMetrics: handles empty results")
        void computeMetrics_handlesEmptyResults() {
            List<ResultMerger.MergedResult> results = List.of();
            Set<String> expectedRelevant = Set.of("id1", "id2");

            ABTestHarness.RelevanceMetrics metrics = abTestHarness.computeMetrics(results, expectedRelevant);

            assertThat(metrics.precisionAt5()).isEqualTo(0.0);
            assertThat(metrics.precisionAt10()).isEqualTo(0.0);
            assertThat(metrics.recall()).isEqualTo(0.0);
            assertThat(metrics.retrievedRelevant()).isEqualTo(0);
            assertThat(metrics.totalRetrieved()).isEqualTo(0);
        }

        @Test
        @DisplayName("computeMetrics: handles empty expected relevant set")
        void computeMetrics_handlesEmptyExpectedRelevant() {
            List<ResultMerger.MergedResult> results = List.of(
                    createMergedResult("id1", 0.9),
                    createMergedResult("id2", 0.8)
            );
            Set<String> expectedRelevant = Set.of();

            ABTestHarness.RelevanceMetrics metrics = abTestHarness.computeMetrics(results, expectedRelevant);

            assertThat(metrics.precisionAt5()).isEqualTo(0.0);
            assertThat(metrics.precisionAt10()).isEqualTo(0.0);
            assertThat(metrics.recall()).isEqualTo(0.0);
            assertThat(metrics.totalRelevant()).isEqualTo(0);
        }

        @Test
        @DisplayName("computeMetrics: handles null results")
        void computeMetrics_handlesNullResults() {
            Set<String> expectedRelevant = Set.of("id1", "id2");

            ABTestHarness.RelevanceMetrics metrics = abTestHarness.computeMetrics(null, expectedRelevant);

            assertThat(metrics.precisionAt5()).isEqualTo(0.0);
            assertThat(metrics.precisionAt10()).isEqualTo(0.0);
            assertThat(metrics.recall()).isEqualTo(0.0);
            assertThat(metrics.retrievedRelevant()).isEqualTo(0);
            assertThat(metrics.totalRetrieved()).isEqualTo(0);
        }
    }

    // --- RelevanceMetrics validation ---

    @Nested
    @DisplayName("RelevanceMetrics validation")
    class RelevanceMetricsValidation {

        @Test
        @DisplayName("RelevanceMetrics rejects invalid precision values")
        void relevanceMetrics_rejectsInvalidPrecision() {
            assertThatThrownBy(() -> new ABTestHarness.RelevanceMetrics(
                    -0.1, 0.5, 0.5, 10, 5, 10))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("precisionAt5 must be in [0.0, 1.0]");

            assertThatThrownBy(() -> new ABTestHarness.RelevanceMetrics(
                    1.1, 0.5, 0.5, 10, 5, 10))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("precisionAt5 must be in [0.0, 1.0]");
        }

        @Test
        @DisplayName("RelevanceMetrics rejects invalid recall")
        void relevanceMetrics_rejectsInvalidRecall() {
            assertThatThrownBy(() -> new ABTestHarness.RelevanceMetrics(
                    0.5, 0.5, -0.1, 10, 5, 10))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("recall must be in [0.0, 1.0]");
        }
    }

    // --- Weight tuning recommendations ---

    @Nested
    @DisplayName("Weight tuning recommendations")
    class WeightTuningRecommendations {

        @Test
        @DisplayName("generateWeightRecommendation: recommends higher keyword weight when keyword performs better")
        void generateWeightRecommendation_keywordPerformsBetter() {
            // Create comparison results where keyword performs better
            ABTestHarness.TestQuery query1 = new ABTestHarness.TestQuery(
                    "query1", Set.of("id1", "id2"), "test1");
            ABTestHarness.TestQuery query2 = new ABTestHarness.TestQuery(
                    "query2", Set.of("id3", "id4"), "test2");

            Map<SearchMode, ABTestHarness.RelevanceMetrics> metrics1 = new java.util.HashMap<>();
            metrics1.put(SearchMode.HYBRID, new ABTestHarness.RelevanceMetrics(0.6, 0.6, 0.5, 4, 2, 4));
            metrics1.put(SearchMode.KEYWORD, new ABTestHarness.RelevanceMetrics(0.8, 0.8, 0.6, 4, 2, 4));
            metrics1.put(SearchMode.VECTOR, new ABTestHarness.RelevanceMetrics(0.4, 0.4, 0.3, 4, 1, 4));

            Map<SearchMode, ABTestHarness.RelevanceMetrics> metrics2 = new java.util.HashMap<>();
            metrics2.put(SearchMode.HYBRID, new ABTestHarness.RelevanceMetrics(0.5, 0.5, 0.4, 4, 2, 4));
            metrics2.put(SearchMode.KEYWORD, new ABTestHarness.RelevanceMetrics(0.7, 0.7, 0.5, 4, 2, 4));
            metrics2.put(SearchMode.VECTOR, new ABTestHarness.RelevanceMetrics(0.3, 0.3, 0.2, 4, 1, 4));

            List<ABTestHarness.ComparisonResult> comparisonResults = List.of(
                    new ABTestHarness.ComparisonResult(query1, metrics1, SearchMode.KEYWORD, 0.0, 0.0),
                    new ABTestHarness.ComparisonResult(query2, metrics2, SearchMode.KEYWORD, 0.0, 0.0)
            );

            ABTestHarness.WeightTuningRecommendation recommendation =
                    abTestHarness.generateWeightRecommendation(comparisonResults);

            assertThat(recommendation.recommendedKeywordWeight()).isCloseTo(0.7, Offset.offset(0.001));
            assertThat(recommendation.recommendedVectorWeight()).isCloseTo(0.3, Offset.offset(0.001));
            assertThat(recommendation.rationale()).contains("Keyword search performs better");
        }

        @Test
        @DisplayName("generateWeightRecommendation: recommends higher vector weight when vector performs better")
        void generateWeightRecommendation_vectorPerformsBetter() {
            // Create comparison results where vector performs better
            ABTestHarness.TestQuery query1 = new ABTestHarness.TestQuery(
                    "query1", Set.of("id1", "id2"), "test1");

            Map<SearchMode, ABTestHarness.RelevanceMetrics> metrics1 = new java.util.HashMap<>();
            metrics1.put(SearchMode.HYBRID, new ABTestHarness.RelevanceMetrics(0.5, 0.5, 0.4, 4, 2, 4));
            metrics1.put(SearchMode.KEYWORD, new ABTestHarness.RelevanceMetrics(0.3, 0.3, 0.2, 4, 1, 4));
            metrics1.put(SearchMode.VECTOR, new ABTestHarness.RelevanceMetrics(0.8, 0.8, 0.6, 4, 2, 4));

            List<ABTestHarness.ComparisonResult> comparisonResults = List.of(
                    new ABTestHarness.ComparisonResult(query1, metrics1, SearchMode.VECTOR, 0.0, 0.0)
            );

            ABTestHarness.WeightTuningRecommendation recommendation =
                    abTestHarness.generateWeightRecommendation(comparisonResults);

            assertThat(recommendation.recommendedKeywordWeight()).isCloseTo(0.4, Offset.offset(0.001));
            assertThat(recommendation.recommendedVectorWeight()).isCloseTo(0.6, Offset.offset(0.001));
            assertThat(recommendation.rationale()).contains("Vector search performs better");
        }

        @Test
        @DisplayName("generateWeightRecommendation: recommends balanced weights when hybrid performs best")
        void generateWeightRecommendation_hybridPerformsBest() {
            // Create comparison results where hybrid performs best
            ABTestHarness.TestQuery query1 = new ABTestHarness.TestQuery(
                    "query1", Set.of("id1", "id2"), "test1");

            Map<SearchMode, ABTestHarness.RelevanceMetrics> metrics1 = new java.util.HashMap<>();
            metrics1.put(SearchMode.HYBRID, new ABTestHarness.RelevanceMetrics(0.8, 0.8, 0.6, 4, 2, 4));
            metrics1.put(SearchMode.KEYWORD, new ABTestHarness.RelevanceMetrics(0.5, 0.5, 0.4, 4, 2, 4));
            metrics1.put(SearchMode.VECTOR, new ABTestHarness.RelevanceMetrics(0.5, 0.5, 0.4, 4, 2, 4));

            List<ABTestHarness.ComparisonResult> comparisonResults = List.of(
                    new ABTestHarness.ComparisonResult(query1, metrics1, SearchMode.HYBRID, 0.0, 0.0)
            );

            ABTestHarness.WeightTuningRecommendation recommendation =
                    abTestHarness.generateWeightRecommendation(comparisonResults);

            assertThat(recommendation.recommendedKeywordWeight()).isCloseTo(0.6, Offset.offset(0.001));
            assertThat(recommendation.recommendedVectorWeight()).isCloseTo(0.4, Offset.offset(0.001));
            assertThat(recommendation.rationale()).contains("Hybrid approach performs best");
        }

        @Test
        @DisplayName("generateWeightRecommendation: handles empty results")
        void generateWeightRecommendation_handlesEmptyResults() {
            ABTestHarness.WeightTuningRecommendation recommendation =
                    abTestHarness.generateWeightRecommendation(List.of());

            assertThat(recommendation.recommendedKeywordWeight()).isCloseTo(0.6, Offset.offset(0.001));
            assertThat(recommendation.recommendedVectorWeight()).isCloseTo(0.4, Offset.offset(0.001));
            assertThat(recommendation.rationale()).contains("No test data available");
        }

        @Test
        @DisplayName("generateWeightRecommendation: handles null results")
        void generateWeightRecommendation_handlesNullResults() {
            ABTestHarness.WeightTuningRecommendation recommendation =
                    abTestHarness.generateWeightRecommendation(null);

            assertThat(recommendation.recommendedKeywordWeight()).isCloseTo(0.6, Offset.offset(0.001));
            assertThat(recommendation.recommendedVectorWeight()).isCloseTo(0.4, Offset.offset(0.001));
            assertThat(recommendation.rationale()).contains("No test data available");
        }
    }

    // --- WeightTuningRecommendation validation ---

    @Nested
    @DisplayName("WeightTuningRecommendation validation")
    class WeightTuningRecommendationValidation {

        @Test
        @DisplayName("WeightTuningRecommendation rejects invalid weights")
        void weightTuningRecommendation_rejectsInvalidWeights() {
            assertThatThrownBy(() -> new ABTestHarness.WeightTuningRecommendation(
                    -0.1, 0.5, "rationale", List.of()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Recommended keyword weight must be in [0.0, 1.0]");

            assertThatThrownBy(() -> new ABTestHarness.WeightTuningRecommendation(
                    0.6, 0.5, "rationale", List.of()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Weights must sum to 1.0");
        }
    }

    // --- ComparisonResult validation ---

    @Nested
    @DisplayName("ComparisonResult validation")
    class ComparisonResultValidation {

        @Test
        @DisplayName("ComparisonResult rejects null test query")
        void comparisonResult_rejectsNullTestQuery() {
            assertThatThrownBy(() -> new ABTestHarness.ComparisonResult(
                    null, new java.util.HashMap<>(), SearchMode.HYBRID, 0.0, 0.0))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Test query cannot be null");
        }

        @Test
        @DisplayName("ComparisonResult rejects null metrics")
        void comparisonResult_rejectsNullMetrics() {
            ABTestHarness.TestQuery query = new ABTestHarness.TestQuery(
                    "query", Set.of("id1"), "description");
            assertThatThrownBy(() -> new ABTestHarness.ComparisonResult(
                    query, null, SearchMode.HYBRID, 0.0, 0.0))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Metrics by mode cannot be null or empty");
        }

        @Test
        @DisplayName("ComparisonResult rejects empty metrics")
        void comparisonResult_rejectsEmptyMetrics() {
            ABTestHarness.TestQuery query = new ABTestHarness.TestQuery(
                    "query", Set.of("id1"), "description");
            assertThatThrownBy(() -> new ABTestHarness.ComparisonResult(
                    query, new java.util.HashMap<>(), SearchMode.HYBRID, 0.0, 0.0))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Metrics by mode cannot be null or empty");
        }
    }
}
