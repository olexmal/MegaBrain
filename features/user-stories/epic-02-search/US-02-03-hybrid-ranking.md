# US-02-03: Hybrid Ranking Algorithm

## Story
**As a** developer  
**I want** search results to combine keyword and vector relevance  
**So that** I get the best of both search approaches

## Story Points: 5
## Priority: High
## Sprint Target: Sprint 3

---

## Acceptance Criteria

- [x] **AC1:** Final score = weighted combination of Lucene and vector scores
- [x] **AC2:** Weights configurable (default: 0.6 keyword, 0.4 vector)
- [x] **AC3:** Results deduplicated when same chunk in both result sets
- [x] **AC4:** Option to use keyword-only or vector-only mode
- [x] **AC5:** Hybrid improves relevance over either approach alone
- [x] **AC6:** Score normalization for fair combination

---

## Demo Script

### Setup
1. Index with both Lucene and vector embeddings
2. Prepare queries that benefit from hybrid approach

### Demo Steps
1. **Hybrid Search:** Run default hybrid search
   ```bash
   curl "http://localhost:8080/api/v1/search?q=user+authentication"
   ```
2. **Compare Modes:** Run same query in keyword-only, vector-only, hybrid
3. **Show Scores:** Display component scores and final hybrid score
4. **Tune Weights:** Demonstrate weight configuration impact
5. **Edge Cases:** Show where hybrid beats single approach

### Expected Outcome
- Hybrid finds results that keyword or vector alone might miss
- Deduplication works correctly
- Configurable weights change ranking

---

## Technical Tasks

- [x] **T1:** Implement score normalization for Lucene results (backend)
- [x] **T2:** Implement score normalization for vector results (backend)
- [x] **T3:** Create weighted score combination algorithm (backend)
- [x] **T4:** Implement result set merging and deduplication (backend)
- [x] **T5:** Add configuration for weight parameters (backend)
- [x] **T6:** Add search mode parameter (hybrid/keyword/vector) (backend)
- [x] **T7:** Write unit tests for ranking algorithm (test)
- [x] **T8:** A/B test harness for relevance comparison (test)

---

## Test Scenarios

| Scenario | Given | When | Then |
|:---------|:------|:-----|:-----|
| Equal weights | 0.5/0.5 weights | Search | Balanced ranking |
| Keyword bias | 0.8/0.2 weights | Search | Keyword matches rank higher |
| Vector bias | 0.2/0.8 weights | Search | Semantic matches rank higher |
| Deduplication | Chunk in both sets | Search | Appears once with combined score |
| Keyword-only | mode=keyword | Search | Only Lucene results |
| Vector-only | mode=vector | Search | Only vector results |

---

## Dependencies

- **Blocked by:** US-02-01 (keyword search), US-02-02 (vector search)
- **Enables:** US-03-03 (RAG needs search), US-04-02 (API uses hybrid)

---

## Risks

| Risk | Impact | Likelihood | Mitigation |
|:-----|:-------|:-----------|:-----------|
| Score normalization issues | Biased ranking | Medium | Thorough testing; tuning |
| One system unavailable | Degraded results | Low | Graceful fallback to available |
| Optimal weights unknown | Subpar relevance | Medium | A/B testing; user feedback |

---

## Definition of Ready

- [x] Acceptance criteria clear
- [x] Dependencies identified
- [x] Tech tasks estimated
- [x] Test scenarios defined
- [x] Demo script approved
- [x] No blockers

