# Tasks for US-02-03: Hybrid Ranking Algorithm

## Story Reference
- **Epic:** EPIC-02 (Hybrid Search & Retrieval)
- **Story:** US-02-03
- **Story Points:** 5
- **Sprint Target:** Sprint 3

## Task List

### T1: Implement score normalization for Lucene results
- **Description:** Implement score normalization for Lucene search results to convert raw Lucene scores (which can vary widely) into a normalized range (e.g., 0.0-1.0). This ensures fair combination with vector similarity scores. Handle edge cases like zero scores and very high scores.
- **Estimated Hours:** 4 hours
- **Assignee:** TBD
- **Status:** Completed
- **Dependencies:** US-02-01 (needs Lucene search working)
- **Acceptance Criteria:**
  - [x] Lucene scores normalized to 0.0-1.0 range
  - [x] Normalization is consistent and reproducible
  - [x] Handles edge cases (zero scores, single result)
  - [x] Normalization function is well-tested
- **Technical Notes:** Use min-max normalization or sigmoid function. Consider using Lucene's Explanation API to understand score distribution. Test with various query types and result sets.

### T2: Implement score normalization for vector results
- **Description:** Implement score normalization for vector similarity results. Convert cosine distance (or similarity) scores into normalized range (0.0-1.0) to match Lucene score format. Ensure normalization is consistent with Lucene normalization.
- **Estimated Hours:** 3 hours
- **Assignee:** TBD
- **Status:** Completed
- **Dependencies:** US-02-02 (needs vector search working)
- **Acceptance Criteria:**
  - [x] Vector similarity scores normalized to 0.0-1.0 range
  - [x] Normalization consistent with Lucene normalization
  - [x] Handles edge cases (identical vectors, orthogonal vectors)
  - [x] Normalization function is well-tested
- **Technical Notes:** Cosine similarity is already 0.0-1.0, but may need adjustment. Cosine distance (1 - similarity) needs conversion. Ensure both systems use same scale for fair combination. **Implementation:** `VectorScoreNormalizer` in `io.megabrain.core` uses min-max normalization (same as T1) on `List<VectorStore.SearchResult>`; 12 unit tests cover null/empty, single, multiple, equal scores, orthogonal/identical vectors, order preservation, reproducibility.

### T3: Create weighted score combination algorithm
- **Description:** Implement algorithm that combines normalized Lucene and vector scores using configurable weights. Default weights: 0.6 keyword, 0.4 vector. Formula: final_score = (keyword_weight * lucene_score) + (vector_weight * vector_score). Ensure weights sum to 1.0.
- **Estimated Hours:** 4 hours
- **Assignee:** TBD
- **Status:** Completed
- **Dependencies:** T1, T2 (needs normalized scores)
- **Acceptance Criteria:**
  - [x] Weighted combination algorithm implemented
  - [x] Configurable weights supported
  - [x] Weights validated (sum to 1.0)
  - [x] Algorithm is efficient (no performance impact)
- **Technical Notes:** Create HybridScorer class. Validate weights on configuration load. Support per-request weight override. Cache weight configuration for performance. **Implementation:** `HybridScorer` in `io.megabrain.core` with `HybridWeights` record; `@ConfigProperty` for `megabrain.search.hybrid.keyword-weight` / `vector-weight`; `combine(lucene, vector)` and `combine(lucene, vector, kw, vw)`; 23 unit tests (HybridScorerTest, HybridWeightsTest).

### T4: Implement result set merging and deduplication
- **Description:** Implement logic to merge results from Lucene and vector searches, deduplicating chunks that appear in both result sets. When a chunk appears in both, use the combined score. Sort final results by combined score.
- **Estimated Hours:** 5 hours
- **Assignee:** TBD
- **Status:** Completed
- **Dependencies:** T3 (needs score combination)
- **Acceptance Criteria:**
  - [x] Results from both searches merged
  - [x] Duplicate chunks appear only once
  - [x] Combined score used for duplicates
  - [x] Final results sorted by combined score
- **Technical Notes:** Use chunk ID or (file_path, entity_name) as deduplication key. Merge using Map for O(1) lookup. Sort results by final combined score. Handle cases where one system returns no results. **Implementation:** `ResultMerger` in `io.megabrain.core` with `merge()` method; deduplication using chunk ID (document_id or file_path + entity_name); combines scores via `HybridScorer` when duplicates found; sorts by combined score descending; 14 comprehensive unit tests covering all scenarios (>80% coverage).

### T5: Add configuration for weight parameters
- **Description:** Add configuration support for hybrid ranking weights. Allow global configuration via application.properties and per-request override via API parameters. Validate weights (must be 0.0-1.0, must sum to 1.0 for both weights).
- **Estimated Hours:** 3 hours
- **Assignee:** TBD
- **Status:** Completed
- **Dependencies:** T3 (needs score combination)
- **Acceptance Criteria:**
  - [x] Weight configuration via application.properties
  - [x] Per-request weight override via API
  - [x] Weight validation on configuration load
  - [x] Default weights applied if not specified
- **Technical Notes:** Use Quarkus configuration: `megabrain.search.hybrid.keyword-weight=0.6`, `megabrain.search.hybrid.vector-weight=0.4`. Validate on startup. Support runtime weight changes (reload configuration). **Implementation:** Configuration enabled in `application.properties`; `HybridScorer` uses `@ConfigProperty` with defaults (0.6/0.4); validation in `@PostConstruct init()` via `HybridWeights.of()`; per-request override supported via `combine(lucene, vector, kw, vw)` method (ready for API integration in US-04-02); comprehensive tests: `HybridScorerConfigurationTest` (default config), `HybridScorerCustomConfigurationTest` (custom config via test profile), `HybridWeightsTest` (validation logic).

### T6: Add search mode parameter (hybrid/keyword/vector)
- **Description:** Add search mode parameter to search API that allows users to select search mode: hybrid (default), keyword-only, or vector-only. When keyword-only, skip vector search. When vector-only, skip Lucene search.
- **Estimated Hours:** 3 hours
- **Assignee:** TBD
- **Status:** Completed
- **Dependencies:** T4 (needs result merging)
- **Acceptance Criteria:**
  - [x] Search mode parameter added to API
  - [x] hybrid mode uses both searches
  - [x] keyword mode uses only Lucene
  - [x] vector mode uses only vector search
- **Technical Notes:** Add `mode` query parameter to search API. Enum values: HYBRID, KEYWORD, VECTOR. Default to HYBRID. Update API documentation. **Implementation:** Created `SearchMode` enum (HYBRID, KEYWORD, VECTOR) in `io.megabrain.core`; updated `HybridIndexService` with `search(query, limit, mode)` method that conditionally executes Lucene and/or vector searches based on mode; handles zero/negative limits gracefully; comprehensive unit tests (11 tests) covering all modes, edge cases, and datasource unavailability scenarios.

### T7: Write unit tests for ranking algorithm
- **Description:** Create comprehensive unit tests for hybrid ranking algorithm. Test score normalization, weighted combination, result merging, deduplication, and different search modes. Use mock search results for testing.
- **Estimated Hours:** 4 hours
- **Assignee:** TBD
- **Status:** Completed
- **Dependencies:** T1-T6 (needs complete implementation)
- **Acceptance Criteria:**
  - [x] Unit tests for score normalization
  - [x] Unit tests for weighted combination
  - [x] Unit tests for result merging
  - [x] Unit tests for deduplication
  - [x] Test coverage >80%
- **Technical Notes:** Use JUnit 5 and mocks. Create test data with known scores. Test edge cases: empty results, single result, all duplicates. Verify score calculations are correct. **Implementation:** Added `HybridRankingAlgorithmTest` (US-02-03, T7) in `io.megabrain.core`: nested suites for score normalization (Lucene + vector), weighted combination, result merging, deduplication, and edge cases; uses mock Lucene/vector results with known scores; >80% coverage for `VectorScoreNormalizer`, `HybridScorer`, and `ResultMerger`. Complements existing `VectorScoreNormalizerTest`, `LuceneIndexServiceTest` (normalizeScores), `HybridScorerTest`, `HybridWeightsTest`, `ResultMergerTest`, and `HybridIndexServiceSearchModeTest`.

### T8: A/B test harness for relevance comparison
- **Description:** Create A/B testing harness to compare hybrid ranking against keyword-only and vector-only approaches. Measure relevance metrics (e.g., precision@k, recall) and collect user feedback. Provide framework for tuning weights based on results.
- **Estimated Hours:** 4 hours
- **Assignee:** TBD
- **Status:** Not Started
- **Dependencies:** T1-T7 (needs complete implementation)
- **Acceptance Criteria:**
  - [ ] A/B test harness implemented
  - [ ] Can compare different ranking approaches
  - [ ] Collects relevance metrics
  - [ ] Framework for weight tuning
- **Technical Notes:** Create test queries and expected results. Measure precision@5, precision@10, recall. Compare hybrid vs single-approach results. Provide weight tuning recommendations based on metrics.

---

## Summary
- **Total Tasks:** 8
- **Total Estimated Hours:** 30 hours
- **Story Points:** 5 (1 SP ≈ 6 hours, aligns with estimate)

