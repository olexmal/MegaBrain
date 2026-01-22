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
- **Status:** Not Started
- **Dependencies:** US-02-02 (needs vector search working)
- **Acceptance Criteria:**
  - [ ] Vector similarity scores normalized to 0.0-1.0 range
  - [ ] Normalization consistent with Lucene normalization
  - [ ] Handles edge cases (identical vectors, orthogonal vectors)
  - [ ] Normalization function is well-tested
- **Technical Notes:** Cosine similarity is already 0.0-1.0, but may need adjustment. Cosine distance (1 - similarity) needs conversion. Ensure both systems use same scale for fair combination.

### T3: Create weighted score combination algorithm
- **Description:** Implement algorithm that combines normalized Lucene and vector scores using configurable weights. Default weights: 0.6 keyword, 0.4 vector. Formula: final_score = (keyword_weight * lucene_score) + (vector_weight * vector_score). Ensure weights sum to 1.0.
- **Estimated Hours:** 4 hours
- **Assignee:** TBD
- **Status:** Not Started
- **Dependencies:** T1, T2 (needs normalized scores)
- **Acceptance Criteria:**
  - [ ] Weighted combination algorithm implemented
  - [ ] Configurable weights supported
  - [ ] Weights validated (sum to 1.0)
  - [ ] Algorithm is efficient (no performance impact)
- **Technical Notes:** Create HybridScorer class. Validate weights on configuration load. Support per-request weight override. Cache weight configuration for performance.

### T4: Implement result set merging and deduplication
- **Description:** Implement logic to merge results from Lucene and vector searches, deduplicating chunks that appear in both result sets. When a chunk appears in both, use the combined score. Sort final results by combined score.
- **Estimated Hours:** 5 hours
- **Assignee:** TBD
- **Status:** Not Started
- **Dependencies:** T3 (needs score combination)
- **Acceptance Criteria:**
  - [ ] Results from both searches merged
  - [ ] Duplicate chunks appear only once
  - [ ] Combined score used for duplicates
  - [ ] Final results sorted by combined score
- **Technical Notes:** Use chunk ID or (file_path, entity_name) as deduplication key. Merge using Map for O(1) lookup. Sort results by final combined score. Handle cases where one system returns no results.

### T5: Add configuration for weight parameters
- **Description:** Add configuration support for hybrid ranking weights. Allow global configuration via application.properties and per-request override via API parameters. Validate weights (must be 0.0-1.0, must sum to 1.0 for both weights).
- **Estimated Hours:** 3 hours
- **Assignee:** TBD
- **Status:** Not Started
- **Dependencies:** T3 (needs score combination)
- **Acceptance Criteria:**
  - [ ] Weight configuration via application.properties
  - [ ] Per-request weight override via API
  - [ ] Weight validation on configuration load
  - [ ] Default weights applied if not specified
- **Technical Notes:** Use Quarkus configuration: `megabrain.search.hybrid.keyword-weight=0.6`, `megabrain.search.hybrid.vector-weight=0.4`. Validate on startup. Support runtime weight changes (reload configuration).

### T6: Add search mode parameter (hybrid/keyword/vector)
- **Description:** Add search mode parameter to search API that allows users to select search mode: hybrid (default), keyword-only, or vector-only. When keyword-only, skip vector search. When vector-only, skip Lucene search.
- **Estimated Hours:** 3 hours
- **Assignee:** TBD
- **Status:** Not Started
- **Dependencies:** T4 (needs result merging)
- **Acceptance Criteria:**
  - [ ] Search mode parameter added to API
  - [ ] hybrid mode uses both searches
  - [ ] keyword mode uses only Lucene
  - [ ] vector mode uses only vector search
- **Technical Notes:** Add `mode` query parameter to search API. Enum values: HYBRID, KEYWORD, VECTOR. Default to HYBRID. Update API documentation.

### T7: Write unit tests for ranking algorithm
- **Description:** Create comprehensive unit tests for hybrid ranking algorithm. Test score normalization, weighted combination, result merging, deduplication, and different search modes. Use mock search results for testing.
- **Estimated Hours:** 4 hours
- **Assignee:** TBD
- **Status:** Not Started
- **Dependencies:** T1-T6 (needs complete implementation)
- **Acceptance Criteria:**
  - [ ] Unit tests for score normalization
  - [ ] Unit tests for weighted combination
  - [ ] Unit tests for result merging
  - [ ] Unit tests for deduplication
  - [ ] Test coverage >80%
- **Technical Notes:** Use JUnit 5 and mocks. Create test data with known scores. Test edge cases: empty results, single result, all duplicates. Verify score calculations are correct.

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

