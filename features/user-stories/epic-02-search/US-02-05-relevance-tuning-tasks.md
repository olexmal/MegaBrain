# Tasks for US-02-05: Relevance Tuning Configuration

## Story Reference
- **Epic:** EPIC-02 (Hybrid Search & Retrieval)
- **Story:** US-02-05
- **Story Points:** 2
- **Sprint Target:** Sprint 4

## Task List

### T1: Create boost configuration class
- **Description:** Create a BoostConfiguration class that holds boost values for different index fields. Fields: entity_name, doc_summary, content. Load configuration from application.properties with defaults. Make configuration immutable and validated.
- **Estimated Hours:** 2 hours
- **Assignee:** TBD
- **Status:** Completed
- **Dependencies:** US-02-01 (needs Lucene index)
- **Acceptance Criteria:**
  - [x] BoostConfiguration class created
  - [x] Holds boost values for all fields
  - [x] Default values: entity_name=3.0, doc_summary=2.0, content=1.0
  - [x] Configuration is validated
- **Technical Notes:** Use Quarkus configuration. Create @ConfigProperties class. Validate boost values are positive. Use Java record for immutability if possible.

### T2: Load boosts from application.properties
- **Description:** Implement configuration loading from application.properties file. Support field-specific boost configuration: `megabrain.search.boost.entity-name=3.0`. Load and validate on application startup.
- **Estimated Hours:** 2 hours
- **Assignee:** TBD
- **Status:** Completed
- **Dependencies:** T1 (needs BoostConfiguration class)
- **Acceptance Criteria:**
  - [x] Boosts loaded from application.properties
  - [x] Default values used if not specified
  - [x] Configuration validated on startup
  - [x] Clear error messages for invalid values
- **Technical Notes:** Use Quarkus @ConfigProperty annotations. Support environment variable overrides. Validate boost values are positive numbers. Log loaded configuration.

### T3: Apply boosts at query time in Lucene
- **Description:** Modify Lucene query construction to apply field boosts from configuration. Use BoostQuery to wrap field queries with appropriate boost values. Apply boosts dynamically based on configuration.
- **Estimated Hours:** 3 hours
- **Assignee:** TBD
- **Status:** Not Started
- **Dependencies:** T1, T2 (needs configuration), US-02-01 (needs query construction)
- **Acceptance Criteria:**
  - [ ] Field boosts applied to queries
  - [ ] Boosts from configuration used
  - [ ] Query-time boosts (no reindexing needed)
  - [ ] Boosts affect ranking correctly
- **Technical Notes:** Modify query construction in LuceneIndexService. Use BoostQuery to wrap field queries. Apply boosts: entity_name * 3.0, doc_summary * 2.0, content * 1.0. Test with various queries.

### T4: Add field match explanation to results
- **Description:** Add field match information to search results showing which fields matched the query. Include field names and match scores per field. This helps users understand why results ranked as they did.
- **Estimated Hours:** 3 hours
- **Assignee:** TBD
- **Status:** Not Started
- **Dependencies:** T3 (needs boosted queries)
- **Acceptance Criteria:**
  - [ ] Field match information in results
  - [ ] Shows which fields matched
  - [ ] Includes per-field scores
  - [ ] Information is clear and useful
- **Technical Notes:** Use Lucene's Explanation API to extract field matches. Include in SearchResult DTO as optional field. Format as JSON: `{"matched_fields": ["entity_name", "content"], "scores": {...}}`. Make it optional for performance.

### T5: Write tests for boost application
- **Description:** Create unit tests to verify boost configuration loading and application. Test that boosts are applied correctly, default values work, and configuration validation. Test ranking changes with different boost values.
- **Estimated Hours:** 2 hours
- **Assignee:** TBD
- **Status:** Not Started
- **Dependencies:** T1-T4 (needs complete implementation)
- **Acceptance Criteria:**
  - [ ] Tests for configuration loading
  - [ ] Tests for boost application
  - [ ] Tests verify ranking changes
  - [ ] Test coverage >80%
- **Technical Notes:** Use JUnit 5. Test with known data and queries. Verify entity_name matches rank higher than content matches. Test with different boost configurations.

---

## Summary
- **Total Tasks:** 5
- **Total Estimated Hours:** 12 hours
- **Story Points:** 2 (1 SP ≈ 6 hours, aligns with estimate)

