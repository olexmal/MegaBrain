# Tasks for US-07-03: Documentation Indexing

## Story Reference
- **Epic:** EPIC-07 (Documentation Intelligence)
- **Story:** US-07-03
- **Story Points:** 3
- **Sprint Target:** Sprint 4

## Task List

### T1: Add doc_summary field to index
- **Description:** Add `doc_summary` field to Lucene index schema. Field should store documentation content (description, params, returns). Make field searchable and stored. Update index mapping.
- **Estimated Hours:** 2 hours
- **Assignee:** TBD
- **Status:** Not Started
- **Dependencies:** US-07-01, US-07-02 (needs doc extraction), US-02-01 (needs Lucene index)
- **Acceptance Criteria:**
  - [ ] doc_summary field added
  - [ ] Field is searchable
  - [ ] Field is stored
  - [ ] Schema updated
- **Technical Notes:** Add doc_summary field to Lucene Document. Use TextField for searchable content. Store field for retrieval. Update index schema.

### T2: Configure boost for doc field
- **Description:** Configure boost factor for doc_summary field in search queries. Set default boost to 2.0x (documentation matches rank higher than code matches). Make boost configurable.
- **Estimated Hours:** 2 hours
- **Assignee:** TBD
- **Status:** Not Started
- **Dependencies:** T1 (needs doc field), US-02-05 (needs boost configuration)
- **Acceptance Criteria:**
  - [ ] Boost configured for doc field
  - [ ] Default boost: 2.0x
  - [ ] Boost is configurable
  - [ ] Boost applied in queries
- **Technical Notes:** Use BoostQuery for doc_summary field. Set boost factor to 2.0. Load boost from configuration. Apply boost in query construction.

### T3: Add docs_only filter parameter
- **Description:** Add `docs_only` filter parameter to search API. When true, search only in doc_summary field. Filter out results without documentation. Return only documented entities.
- **Estimated Hours:** 3 hours
- **Assignee:** TBD
- **Status:** Not Started
- **Dependencies:** T1 (needs doc field), US-04-02 (needs search API)
- **Acceptance Criteria:**
  - [ ] docs_only parameter added
  - [ ] Filters to doc field only
  - [ ] Undocumented entities excluded
  - [ ] Parameter works correctly
- **Technical Notes:** Add docs_only query parameter. Modify search query to search only doc_summary field when true. Filter results to only documented entities.

### T4: Update search to include docs
- **Description:** Update search implementation to include documentation in search results. Search both code content and documentation. Return documentation snippets in results. Highlight documentation matches.
- **Estimated Hours:** 4 hours
- **Assignee:** TBD
- **Status:** Not Started
- **Dependencies:** T1, T2 (needs doc field and boost)
- **Acceptance Criteria:**
  - [ ] Documentation included in search
  - [ ] Documentation snippets returned
  - [ ] Matches highlighted
  - [ ] Results include doc content
- **Technical Notes:** Search both content and doc_summary fields. Return documentation snippets in search results. Highlight matches in documentation. Include doc metadata.

### T5: Write tests for doc search
- **Description:** Create comprehensive tests for documentation search. Test docs_only filter, documentation boost, and documentation in results. Use test data with known documentation.
- **Estimated Hours:** 3 hours
- **Assignee:** TBD
- **Status:** Not Started
- **Dependencies:** T1-T4 (needs complete implementation)
- **Acceptance Criteria:**
  - [ ] Tests for docs_only filter
  - [ ] Tests for documentation boost
  - [ ] Tests for documentation in results
  - [ ] Test coverage >80%
- **Technical Notes:** Create test data with documentation. Test docs_only filter. Test boost effect on ranking. Verify documentation in results.

---

## Summary
- **Total Tasks:** 5
- **Total Estimated Hours:** 14 hours
- **Story Points:** 3 (1 SP ≈ 4.7 hours, aligns with estimate)

