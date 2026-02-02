# Tasks for US-02-06: Transitive Search Integration

## Story Reference
- **Epic:** EPIC-02 (Hybrid Search & Retrieval)
- **Story:** US-02-06
- **Story Points:** 5
- **Sprint Target:** Sprint 5

## Task List

### T1: Add transitive parameter to search API
- **Description:** Add `transitive` boolean parameter to search API endpoint. When true, enable transitive relationship traversal for structural queries (implements, extends). Default to false for backward compatibility.
- **Estimated Hours:** 2 hours
- **Assignee:** TBD
- **Status:** Completed
- **Dependencies:** US-02-03 (needs search API)
- **Acceptance Criteria:**
  - [x] `transitive` parameter added to API
  - [x] Parameter defaults to false
  - [x] Parameter validated
  - [x] API documentation updated
- **Technical Notes:** Update SearchRequest DTO. Add parameter validation. Update OpenAPI/Swagger documentation. Support both query parameter and request body parameter.

### T2: Integrate GraphQueryService into search
- **Description:** Integrate GraphQueryService into the search pipeline. When transitive=true, use graph queries to find related entities, then include those entities in search results. Combine graph results with regular search results.
- **Estimated Hours:** 5 hours
- **Assignee:** TBD
- **Status:** Completed
- **Dependencies:** T1 (needs transitive parameter), US-06-02, US-06-03 (needs graph queries)
- **Acceptance Criteria:**
  - [x] GraphQueryService integrated into search
  - [x] Graph queries executed when transitive=true
  - [x] Graph results combined with search results
  - [x] Integration is efficient
- **Technical Notes:** Create SearchOrchestrator that coordinates Lucene search and graph queries. Execute graph queries in parallel with search if possible. Merge results appropriately.
- **Implementation Notes:** Added GraphQueryService interface and GraphQueryServiceStub (returns empty until US-06-02/06-03). SearchOrchestrator runs hybrid search and graph findRelatedEntities in parallel when transitive=true; graph entity names are resolved via LuceneIndexService.lookupByEntityNames and merged with hybrid results (dedupe by chunk id, sort by score). SearchResource delegates all search to SearchOrchestrator.

### T3: Implement transitive closure for implements
- **Description:** Implement transitive closure logic for "implements" relationships. Given an interface, find all classes that implement it directly or transitively (through abstract classes). Use graph traversal with depth limit.
- **Estimated Hours:** 4 hours
- **Assignee:** TBD
- **Status:** Not Started
- **Dependencies:** T2 (needs graph integration), US-06-03 (needs graph queries)
- **Acceptance Criteria:**
  - [ ] Finds direct implementations
  - [ ] Finds transitive implementations (via abstract classes)
  - [ ] Respects depth limit
  - [ ] Results are accurate and complete
- **Technical Notes:** Use Neo4j Cypher: `MATCH (i:Interface {name: $name})<-[:IMPLEMENTS|EXTENDS*1..5]-(c) RETURN c`. Handle cycles in inheritance graph. Limit depth to prevent performance issues.

### T4: Implement transitive closure for extends
- **Description:** Implement transitive closure logic for "extends" relationships. Given a class, find all subclasses directly or transitively. Use graph traversal with depth limit. Handle multiple inheritance paths.
- **Estimated Hours:** 4 hours
- **Assignee:** TBD
- **Status:** Not Started
- **Dependencies:** T2 (needs graph integration), US-06-03 (needs graph queries)
- **Acceptance Criteria:**
  - [ ] Finds direct subclasses
  - [ ] Finds transitive subclasses
  - [ ] Respects depth limit
  - [ ] Handles multiple inheritance paths
- **Technical Notes:** Use Neo4j Cypher: `MATCH (c:Class {name: $name})<-[:EXTENDS*1..5]-(sub) RETURN sub`. Deduplicate results if multiple paths exist. Limit depth appropriately.

### T5: Add depth limit configuration
- **Description:** Add configuration for maximum traversal depth in transitive queries. Default depth: 5. Allow per-request override via API parameter. Validate depth limits to prevent performance issues.
- **Estimated Hours:** 2 hours
- **Assignee:** TBD
- **Status:** Not Started
- **Dependencies:** T3, T4 (needs transitive closures)
- **Acceptance Criteria:**
  - [ ] Depth limit configurable
  - [ ] Default depth: 5
  - [ ] Per-request depth override supported
  - [ ] Depth validation (max limit enforced)
- **Technical Notes:** Add `depth` parameter to search API. Validate depth is 1-10 (configurable max). Use depth in graph queries. Document depth impact on performance.

### T6: Mark transitive results in response
- **Description:** Add metadata to search results indicating which results were found via transitive traversal vs direct search. Include relationship path information for transitive results. This helps users understand result provenance.
- **Estimated Hours:** 3 hours
- **Assignee:** TBD
- **Status:** Not Started
- **Dependencies:** T2-T5 (needs transitive queries working)
- **Acceptance Criteria:**
  - [ ] Transitive results marked in response
  - [ ] Relationship path included
  - [ ] Clear distinction between direct and transitive
  - [ ] Metadata is useful for debugging
- **Technical Notes:** Add `is_transitive` flag to SearchResult. Include `relationship_path` array showing traversal path. Format: `["Interface", "AbstractClass", "ConcreteClass"]`. Make optional for performance.

### T7: Write tests for transitive queries
- **Description:** Create comprehensive tests for transitive search functionality. Test implements transitive closure, extends transitive closure, depth limits, and result marking. Use test graph with known relationships.
- **Estimated Hours:** 4 hours
- **Assignee:** TBD
- **Status:** Not Started
- **Dependencies:** T1-T6 (needs complete implementation)
- **Acceptance Criteria:**
  - [ ] Tests for implements transitive closure
  - [ ] Tests for extends transitive closure
  - [ ] Tests for depth limits
  - [ ] Tests for result marking
  - [ ] Test coverage >80%
- **Technical Notes:** Use test Neo4j database with known relationship graph. Create test data with interface → abstract → concrete hierarchy. Verify transitive results are correct. Test edge cases (cycles, multiple paths).

---

## Summary
- **Total Tasks:** 7
- **Total Estimated Hours:** 24 hours
- **Story Points:** 5 (1 SP ≈ 4.8 hours, aligns with estimate)

