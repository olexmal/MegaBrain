# Tasks for US-02-04: Metadata Facet Filtering

## Story Reference
- **Epic:** EPIC-02 (Hybrid Search & Retrieval)
- **Story:** US-02-04
- **Story Points:** 3
- **Sprint Target:** Sprint 3

## Task List

### T1: Add filter parameters to search API
- **Description:** Add filter query parameters to search API endpoint. Parameters: language, repository, file_path, entity_type. Parse parameters from request and validate values. Pass filters to search service.
- **Estimated Hours:** 2 hours
- **Assignee:** TBD
- **Status:** Not Started
- **Dependencies:** US-02-01 (needs search API)
- **Acceptance Criteria:**
  - [ ] Filter parameters added to API
  - [ ] Parameters parsed and validated
  - [ ] Multiple filters supported
  - [ ] Invalid filter values rejected with clear errors
- **Technical Notes:** Update SearchRequest DTO. Add validation annotations. Support multiple values per filter (e.g., language=java&language=python). Update OpenAPI documentation.

### T2: Implement Lucene filter queries
- **Description:** Implement Lucene filter queries for metadata fields. Create TermQuery or BooleanQuery filters for language, repository, file_path (prefix), and entity_type. Apply filters before scoring for efficiency.
- **Estimated Hours:** 4 hours
- **Assignee:** TBD
- **Status:** Not Started
- **Dependencies:** T1 (needs filter parameters), US-02-01 (needs Lucene index)
- **Acceptance Criteria:**
  - [ ] Language filter implemented
  - [ ] Repository filter implemented
  - [ ] File path prefix filter implemented
  - [ ] Entity type filter implemented
  - [ ] Filters applied before scoring
- **Technical Notes:** Use Lucene's TermQuery for exact matches, PrefixQuery for path prefixes. Combine multiple filters with BooleanQuery (MUST clauses). Use Filter API for efficiency (filters don't affect scoring).

### T3: Implement facet aggregation
- **Description:** Implement facet aggregation to compute available filter values and their counts. Use Lucene's Facets API to aggregate values for language, repository, entity_type fields. Return facet information in search response.
- **Estimated Hours:** 5 hours
- **Assignee:** TBD
- **Status:** Not Started
- **Dependencies:** T2 (needs filter queries), US-02-01 (needs Lucene index)
- **Acceptance Criteria:**
  - [ ] Facet aggregation for language field
  - [ ] Facet aggregation for repository field
  - [ ] Facet aggregation for entity_type field
  - [ ] Facet counts returned in response
  - [ ] Facet computation is efficient
- **Technical Notes:** Use Lucene's Facets API with SortedSetDocValuesFacetCounts. Aggregate top N values per field. Cache facet results if possible. Include facet counts in SearchResponse DTO.

### T4: Optimize filter application before scoring
- **Description:** Optimize filter application to run before expensive scoring operations. Use Lucene's Filter API which applies filters efficiently using bitsets. Ensure filters don't impact search performance significantly.
- **Estimated Hours:** 3 hours
- **Assignee:** TBD
- **Status:** Not Started
- **Dependencies:** T2 (needs filter queries)
- **Acceptance Criteria:**
  - [ ] Filters applied before scoring
  - [ ] Filter performance is acceptable (<50ms overhead)
  - [ ] Filters use efficient bitset operations
  - [ ] No performance regression
- **Technical Notes:** Use Lucene's Filter API with CachingWrapperFilter for repeated filters. Apply filters using IndexSearcher.search(query, filter, limit). Profile filter performance.

### T5: Add facet counts to response
- **Description:** Extend SearchResponse DTO to include facet information. Include available filter values and their counts for each facet field. Format facets as JSON object with field names as keys.
- **Estimated Hours:** 2 hours
- **Assignee:** TBD
- **Status:** Not Started
- **Dependencies:** T3 (needs facet aggregation)
- **Acceptance Criteria:**
  - [ ] Facet information in SearchResponse
  - [ ] Facets formatted as JSON
  - [ ] Includes value and count for each facet
  - [ ] Response structure is clear
- **Technical Notes:** Create FacetInfo DTO with field name, values, and counts. Include in SearchResponse. Use Jackson for JSON serialization. Example: `{"language": [{"value": "java", "count": 150}]}`.

### T6: Write tests for each filter type
- **Description:** Create comprehensive tests for each filter type (language, repository, file_path, entity_type). Test single filters, combined filters, and edge cases. Verify filters work correctly with search queries.
- **Estimated Hours:** 3 hours
- **Assignee:** TBD
- **Status:** Not Started
- **Dependencies:** T1-T5 (needs complete implementation)
- **Acceptance Criteria:**
  - [ ] Tests for language filter
  - [ ] Tests for repository filter
  - [ ] Tests for file_path filter
  - [ ] Tests for entity_type filter
  - [ ] Tests for combined filters
  - [ ] Test coverage >80%
- **Technical Notes:** Use JUnit 5. Create test index with known data. Test filters in isolation and combination. Verify results are correctly filtered. Test edge cases (empty results, all filtered out).

---

## Summary
- **Total Tasks:** 6
- **Total Estimated Hours:** 19 hours
- **Story Points:** 3 (1 SP ≈ 6.3 hours, aligns with estimate)

