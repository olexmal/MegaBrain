/*
 * Copyright (c) 2025 MegaBrain Contributors
 * Licensed under the MIT License - see LICENSE file for details.
 */

package io.megabrain.core;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * A/B testing harness for comparing hybrid ranking against keyword-only and vector-only
 * approaches (US-02-03, T8).
 * <p>
 * Measures relevance metrics (precision@k, recall) and provides framework for weight tuning
 * based on test results.
 */
@ApplicationScoped
public class ABTestHarness {

    private static final Logger LOG = Logger.getLogger(ABTestHarness.class);

    @Inject
    @IndexType(IndexType.Type.HYBRID)
    HybridIndexService hybridIndexService;

    @Inject
    HybridScorer hybridScorer;

    /**
     * Test query with expected relevant results for evaluation.
     */
    public record TestQuery(
            String query,
            Set<String> expectedRelevantChunkIds,
            String description
    ) {
        /**
         * Creates a test query.
         *
         * @param query the search query string
         * @param expectedRelevantChunkIds set of chunk IDs that should be in top results
         * @param description human-readable description of the test case
         */
        public TestQuery {
            if (query == null || query.isBlank()) {
                throw new IllegalArgumentException("Query cannot be null or blank");
            }
            if (expectedRelevantChunkIds == null) {
                throw new IllegalArgumentException("Expected relevant chunk IDs cannot be null");
            }
        }
    }

    /**
     * Relevance metrics for a search approach.
     */
    public record RelevanceMetrics(
            double precisionAt5,
            double precisionAt10,
            double recall,
            int totalRelevant,
            int retrievedRelevant,
            int totalRetrieved
    ) {
        /**
         * Creates relevance metrics.
         *
         * @param precisionAt5 precision at top 5 results
         * @param precisionAt10 precision at top 10 results
         * @param recall overall recall
         * @param totalRelevant total number of relevant items in corpus
         * @param retrievedRelevant number of relevant items retrieved
         * @param totalRetrieved total number of items retrieved
         */
        public RelevanceMetrics {
            if (precisionAt5 < 0.0 || precisionAt5 > 1.0) {
                throw new IllegalArgumentException("precisionAt5 must be in [0.0, 1.0]");
            }
            if (precisionAt10 < 0.0 || precisionAt10 > 1.0) {
                throw new IllegalArgumentException("precisionAt10 must be in [0.0, 1.0]");
            }
            if (recall < 0.0 || recall > 1.0) {
                throw new IllegalArgumentException("recall must be in [0.0, 1.0]");
            }
        }
    }

    /**
     * Comparison result for a single test query across different search modes.
     */
    public record ComparisonResult(
            TestQuery testQuery,
            Map<SearchMode, RelevanceMetrics> metricsByMode,
            SearchMode bestMode,
            double improvementOverKeyword,
            double improvementOverVector
    ) {
        /**
         * Creates a comparison result.
         *
         * @param testQuery the test query
         * @param metricsByMode metrics for each search mode
         * @param bestMode the mode with highest precision@10
         * @param improvementOverKeyword improvement of hybrid over keyword (as percentage)
         * @param improvementOverVector improvement of hybrid over vector (as percentage)
         */
        public ComparisonResult {
            if (testQuery == null) {
                throw new IllegalArgumentException("Test query cannot be null");
            }
            if (metricsByMode == null || metricsByMode.isEmpty()) {
                throw new IllegalArgumentException("Metrics by mode cannot be null or empty");
            }
        }
    }

    /**
     * Weight tuning recommendation based on A/B test results.
     */
    public record WeightTuningRecommendation(
            double recommendedKeywordWeight,
            double recommendedVectorWeight,
            String rationale,
            List<String> insights
    ) {
        /**
         * Creates a weight tuning recommendation.
         *
         * @param recommendedKeywordWeight recommended keyword weight (0.0-1.0)
         * @param recommendedVectorWeight recommended vector weight (0.0-1.0)
         * @param rationale explanation of the recommendation
         * @param insights list of insights from the analysis
         */
        public WeightTuningRecommendation {
            if (recommendedKeywordWeight < 0.0 || recommendedKeywordWeight > 1.0) {
                throw new IllegalArgumentException("Recommended keyword weight must be in [0.0, 1.0]");
            }
            if (recommendedVectorWeight < 0.0 || recommendedVectorWeight > 1.0) {
                throw new IllegalArgumentException("Recommended vector weight must be in [0.0, 1.0]");
            }
            if (Math.abs(recommendedKeywordWeight + recommendedVectorWeight - 1.0) > 1e-9) {
                throw new IllegalArgumentException("Weights must sum to 1.0");
            }
        }
    }

    /**
     * Runs A/B test comparing hybrid, keyword-only, and vector-only search modes
     * for a list of test queries.
     *
     * @param testQueries list of test queries with expected relevant results
     * @param limit maximum number of results to retrieve per query
     * @return list of comparison results, one per test query
     */
    public Uni<List<ComparisonResult>> runABTest(List<TestQuery> testQueries, int limit) {
        LOG.infof("Running A/B test with %d test queries, limit=%d", testQueries.size(), limit);

        List<Uni<ComparisonResult>> comparisonUnis = new ArrayList<>();

        for (TestQuery testQuery : testQueries) {
            Uni<ComparisonResult> comparisonUni = compareSearchModes(testQuery, limit);
            comparisonUnis.add(comparisonUni);
        }

        if (comparisonUnis.isEmpty()) {
            return Uni.createFrom().item(List.<ComparisonResult>of());
        }
        
        // Collect all Unis sequentially
        Uni<List<ComparisonResult>> result = Uni.createFrom().item(new ArrayList<>());
        for (Uni<ComparisonResult> comparisonUni : comparisonUnis) {
            result = result.flatMap(list -> comparisonUni
                    .map(item -> {
                        List<ComparisonResult> newList = new ArrayList<>(list);
                        newList.add(item);
                        return newList;
                    }));
        }
        return result;
    }

    /**
     * Compares different search modes for a single test query.
     *
     * @param testQuery the test query with expected results
     * @param limit maximum number of results to retrieve
     * @return comparison result with metrics for each mode
     */
    public Uni<ComparisonResult> compareSearchModes(TestQuery testQuery, int limit) {
        LOG.debugf("Comparing search modes for query: %s", testQuery.query());

        // Execute searches in all three modes
        Uni<List<ResultMerger.MergedResult>> hybridResults = hybridIndexService.search(
                testQuery.query(), limit, SearchMode.HYBRID);
        Uni<List<ResultMerger.MergedResult>> keywordResults = hybridIndexService.search(
                testQuery.query(), limit, SearchMode.KEYWORD);
        Uni<List<ResultMerger.MergedResult>> vectorResults = hybridIndexService.search(
                testQuery.query(), limit, SearchMode.VECTOR);

        // Combine all results and compute metrics
        return Uni.combine().all().unis(hybridResults, keywordResults, vectorResults)
                .asTuple()
                .map(tuple -> {
                    List<ResultMerger.MergedResult> hybrid = tuple.getItem1();
                    List<ResultMerger.MergedResult> keyword = tuple.getItem2();
                    List<ResultMerger.MergedResult> vector = tuple.getItem3();

                    // Compute metrics for each mode
                    RelevanceMetrics hybridMetrics = computeMetrics(hybrid, testQuery.expectedRelevantChunkIds());
                    RelevanceMetrics keywordMetrics = computeMetrics(keyword, testQuery.expectedRelevantChunkIds());
                    RelevanceMetrics vectorMetrics = computeMetrics(vector, testQuery.expectedRelevantChunkIds());

                    Map<SearchMode, RelevanceMetrics> metricsByMode = new HashMap<>();
                    metricsByMode.put(SearchMode.HYBRID, hybridMetrics);
                    metricsByMode.put(SearchMode.KEYWORD, keywordMetrics);
                    metricsByMode.put(SearchMode.VECTOR, vectorMetrics);

                    // Determine best mode (highest precision@10)
                    SearchMode bestMode = determineBestMode(metricsByMode);

                    // Calculate improvements
                    double improvementOverKeyword = calculateImprovement(
                            hybridMetrics.precisionAt10(), keywordMetrics.precisionAt10());
                    double improvementOverVector = calculateImprovement(
                            hybridMetrics.precisionAt10(), vectorMetrics.precisionAt10());

                    return new ComparisonResult(
                            testQuery,
                            metricsByMode,
                            bestMode,
                            improvementOverKeyword,
                            improvementOverVector
                    );
                });
    }

    /**
     * Computes relevance metrics for a set of search results.
     *
     * @param results the search results
     * @param expectedRelevantChunkIds set of chunk IDs that are considered relevant
     * @return relevance metrics
     */
    public RelevanceMetrics computeMetrics(List<ResultMerger.MergedResult> results,
                                           Set<String> expectedRelevantChunkIds) {
        if (results == null || results.isEmpty()) {
            return new RelevanceMetrics(0.0, 0.0, 0.0,
                    expectedRelevantChunkIds.size(), 0, 0);
        }

        // Extract chunk IDs from results
        List<String> retrievedChunkIds = results.stream()
                .map(ResultMerger.MergedResult::chunkId)
                .collect(Collectors.toList());

        // Count relevant items retrieved
        int retrievedRelevant = (int) retrievedChunkIds.stream()
                .filter(expectedRelevantChunkIds::contains)
                .count();

        // Compute precision@k
        double precisionAt5 = computePrecisionAtK(retrievedChunkIds, expectedRelevantChunkIds, 5);
        double precisionAt10 = computePrecisionAtK(retrievedChunkIds, expectedRelevantChunkIds, 10);

        // Compute recall
        double recall = expectedRelevantChunkIds.isEmpty() ? 0.0 :
                (double) retrievedRelevant / expectedRelevantChunkIds.size();

        return new RelevanceMetrics(
                precisionAt5,
                precisionAt10,
                recall,
                expectedRelevantChunkIds.size(),
                retrievedRelevant,
                retrievedChunkIds.size()
        );
    }

    /**
     * Computes precision@k: fraction of top-k results that are relevant.
     *
     * @param retrievedChunkIds list of retrieved chunk IDs (ordered by relevance)
     * @param expectedRelevantChunkIds set of relevant chunk IDs
     * @param k number of top results to consider
     * @return precision@k (0.0-1.0)
     */
    private double computePrecisionAtK(List<String> retrievedChunkIds,
                                       Set<String> expectedRelevantChunkIds,
                                       int k) {
        if (retrievedChunkIds.isEmpty() || k <= 0) {
            return 0.0;
        }

        int topK = Math.min(k, retrievedChunkIds.size());
        long relevantInTopK = retrievedChunkIds.subList(0, topK).stream()
                .filter(expectedRelevantChunkIds::contains)
                .count();

        return (double) relevantInTopK / topK;
    }

    /**
     * Determines the best search mode based on precision@10.
     *
     * @param metricsByMode metrics for each search mode
     * @return the mode with highest precision@10
     */
    private SearchMode determineBestMode(Map<SearchMode, RelevanceMetrics> metricsByMode) {
        SearchMode bestMode = SearchMode.HYBRID;
        double bestPrecision = metricsByMode.get(SearchMode.HYBRID).precisionAt10();

        for (Map.Entry<SearchMode, RelevanceMetrics> entry : metricsByMode.entrySet()) {
            if (entry.getValue().precisionAt10() > bestPrecision) {
                bestPrecision = entry.getValue().precisionAt10();
                bestMode = entry.getKey();
            }
        }

        return bestMode;
    }

    /**
     * Calculates improvement percentage: (new - old) / old * 100.
     *
     * @param newValue the new value
     * @param oldValue the old value
     * @return improvement as percentage (can be negative)
     */
    private double calculateImprovement(double newValue, double oldValue) {
        if (oldValue == 0.0) {
            return newValue > 0.0 ? 100.0 : 0.0;
        }
        return ((newValue - oldValue) / oldValue) * 100.0;
    }

    /**
     * Generates weight tuning recommendations based on A/B test results.
     *
     * @param comparisonResults list of comparison results from A/B test
     * @return weight tuning recommendation
     */
    public WeightTuningRecommendation generateWeightRecommendation(List<ComparisonResult> comparisonResults) {
        if (comparisonResults == null || comparisonResults.isEmpty()) {
            return new WeightTuningRecommendation(
                    0.6, 0.4,
                    "No test data available, using default weights",
                    List.of("Run A/B test with test queries to get recommendations")
            );
        }

        // Analyze results
        double avgHybridPrecision = comparisonResults.stream()
                .mapToDouble(r -> r.metricsByMode().get(SearchMode.HYBRID).precisionAt10())
                .average()
                .orElse(0.0);

        double avgKeywordPrecision = comparisonResults.stream()
                .mapToDouble(r -> r.metricsByMode().get(SearchMode.KEYWORD).precisionAt10())
                .average()
                .orElse(0.0);

        double avgVectorPrecision = comparisonResults.stream()
                .mapToDouble(r -> r.metricsByMode().get(SearchMode.VECTOR).precisionAt10())
                .average()
                .orElse(0.0);

        // Count how often each mode wins
        long hybridWins = comparisonResults.stream()
                .filter(r -> r.bestMode() == SearchMode.HYBRID)
                .count();
        long keywordWins = comparisonResults.stream()
                .filter(r -> r.bestMode() == SearchMode.KEYWORD)
                .count();
        long vectorWins = comparisonResults.stream()
                .filter(r -> r.bestMode() == SearchMode.VECTOR)
                .count();

        // Generate recommendation based on analysis
        List<String> insights = new ArrayList<>();
        insights.add(String.format("Hybrid precision@10: %.3f", avgHybridPrecision));
        insights.add(String.format("Keyword precision@10: %.3f", avgKeywordPrecision));
        insights.add(String.format("Vector precision@10: %.3f", avgVectorPrecision));
        insights.add(String.format("Hybrid wins: %d/%d queries", hybridWins, comparisonResults.size()));

        double recommendedKeywordWeight;
        double recommendedVectorWeight;
        String rationale;

        // If keyword performs significantly better, increase keyword weight
        if (avgKeywordPrecision > avgVectorPrecision + 0.1 && keywordWins > vectorWins) {
            recommendedKeywordWeight = 0.7;
            recommendedVectorWeight = 0.3;
            rationale = "Keyword search performs better; recommend increasing keyword weight to 0.7";
            insights.add("Keyword search shows superior performance");
        }
        // If vector performs significantly better, increase vector weight
        else if (avgVectorPrecision > avgKeywordPrecision + 0.1 && vectorWins > keywordWins) {
            recommendedKeywordWeight = 0.4;
            recommendedVectorWeight = 0.6;
            rationale = "Vector search performs better; recommend increasing vector weight to 0.6";
            insights.add("Vector search shows superior performance");
        }
        // If hybrid performs best, use balanced weights
        else if (avgHybridPrecision >= Math.max(avgKeywordPrecision, avgVectorPrecision)) {
            recommendedKeywordWeight = 0.6;
            recommendedVectorWeight = 0.4;
            rationale = "Hybrid approach performs best; recommend balanced weights (0.6/0.4)";
            insights.add("Hybrid approach shows best overall performance");
        }
        // Default: use current weights
        else {
            HybridScorer.HybridWeights currentWeights = hybridScorer.getDefaultWeights();
            recommendedKeywordWeight = currentWeights.keywordWeight();
            recommendedVectorWeight = currentWeights.vectorWeight();
            rationale = "No clear winner; recommend keeping current weights";
            insights.add("Performance is similar across modes");
        }

        return new WeightTuningRecommendation(
                recommendedKeywordWeight,
                recommendedVectorWeight,
                rationale,
                insights
        );
    }
}
