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
- **Status:** Completed
- **Dependencies:** T2 (needs graph integration), US-06-03 (needs graph queries)
- **Acceptance Criteria:**
  - [x] Finds direct implementations
  - [x] Finds transitive implementations (via abstract classes)
  - [x] Respects depth limit
  - [x] Results are accurate and complete
- **Technical Notes:** Use Neo4j Cypher: `MATCH (i:Interface {name: $name})<-[:IMPLEMENTS|EXTENDS*1..5]-(c) RETURN c`. Handle cycles in inheritance graph. Limit depth to prevent performance issues.
- **Implementation Notes:** Added `ImplementsClosureQuery` interface and `Neo4jImplementsClosureQuery` (Cypher `MATCH (i:Interface {name: $interfaceName})<-[:IMPLEMENTS|EXTENDS*1..depth]-(c) RETURN DISTINCT c`, depth 1–10, returns empty when `megabrain.neo4j.uri` unset). `StructuralQueryParser` parses `implements:InterfaceName` and `extends:ClassName`. `GraphQueryServiceStub` delegates implements-predicate queries to `ImplementsClosureQuery`. Unit tests: StructuralQueryParserTest, GraphQueryServiceStubTest (delegation), ImplementsClosureQueryTest, Neo4jImplementsClosureQueryTest.

### T4: Implement transitive closure for extends
- **Description:** Implement transitive closure logic for "extends" relationships. Given a class, find all subclasses directly or transitively. Use graph traversal with depth limit. Handle multiple inheritance paths.
- **Estimated Hours:** 4 hours
- **Assignee:** TBD
- **Status:** Completed
- **Dependencies:** T2 (needs graph integration), US-06-03 (needs graph queries)
- **Acceptance Criteria:**
  - [x] Finds direct subclasses
  - [x] Finds transitive subclasses
  - [x] Respects depth limit
  - [x] Handles multiple inheritance paths
- **Technical Notes:** Use Neo4j Cypher: `MATCH (c:Class {name: $name})<-[:EXTENDS*1..5]-(sub) RETURN sub`. Deduplicate results if multiple paths exist. Limit depth appropriately.
- **Implementation Notes:** Added `ExtendsClosureQuery` interface and `Neo4jExtendsClosureQuery` (Cypher `MATCH (c:Class {name: $className})<-[:EXTENDS*1..depth]-(sub) RETURN DISTINCT sub`, depth 1–10, returns empty when `megabrain.neo4j.uri` unset). `GraphQueryServiceStub` delegates extends-predicate queries to `ExtendsClosureQuery`. Unit tests: ExtendsClosureQueryTest, Neo4jExtendsClosureQueryTest, GraphQueryServiceStubTest (extends delegation).

### T5: Add depth limit configuration
- **Description:** Add configuration for maximum traversal depth in transitive queries. Default depth: 5. Allow per-request override via API parameter. Validate depth limits to prevent performance issues.
- **Estimated Hours:** 2 hours
- **Assignee:** TBD
- **Status:** Completed
- **Dependencies:** T3, T4 (needs transitive closures)
- **Acceptance Criteria:**
  - [x] Depth limit configurable
  - [x] Default depth: 5
  - [x] Per-request depth override supported
  - [x] Depth validation (max limit enforced)
- **Technical Notes:** Add `depth` parameter to search API. Validate depth is 1-10 (configurable max). Use depth in graph queries. Document depth impact on performance.
- **Implementation Notes:** Added `depth` (Integer, optional) to SearchRequest. Config: `megabrain.search.transitive.default-depth=5`, `megabrain.search.transitive.max-depth=10`. SearchResource accepts `depth` query param, validates 1..max, resolves effective depth (request depth or default) and passes to SearchOrchestrator. Orchestrator uses transitiveDepth in graphQueryService.findRelatedEntities(). Validation returns 400 when depth &lt; 1 or &gt; max. Unit tests: SearchRequestTest (depth get/set, toString), SearchResourceTest (depth validation 400, transitive with depth), SearchOrchestratorTest (passes requested depth to graph).

### T6: Mark transitive results in response
- **Description:** Add metadata to search results indicating which results were found via transitive traversal vs direct search. Include relationship path information for transitive results. This helps users understand result provenance.
- **Estimated Hours:** 3 hours
- **Assignee:** TBD
- **Status:** Completed
- **Dependencies:** T2-T5 (needs transitive queries working)
- **Acceptance Criteria:**
  - [x] Transitive results marked in response
  - [x] Relationship path included
  - [x] Clear distinction between direct and transitive
  - [x] Metadata is useful for debugging
- **Technical Notes:** Add `is_transitive` flag to SearchResult. Include `relationship_path` array showing traversal path. Format: `["Interface", "AbstractClass", "ConcreteClass"]`. Make optional for performance.
- **Implementation Notes:** Added `is_transitive` and `relationship_path` to SearchResult DTO (JSON: is_transitive, relationship_path). Added optional `relationshipPath` to GraphRelatedEntity. Added optional `transitivePath` to ResultMerger.MergedResult with `withTransitivePath(List)` and `fromLuceneTransitive(...)`. SearchOrchestrator builds entityName→path from relatedEntities and enriches graph MergedResults with path; convertToSearchResult sets isTransitive and relationshipPath from transitivePath. Unit tests: SearchResultTest (transitive/non-transitive), SearchResourceTest (transitive response marking, non-transitive no path).

### T7: Write tests for transitive queries
- **Description:** Create comprehensive tests for transitive search functionality. Test implements transitive closure, extends transitive closure, depth limits, and result marking. Use test graph with known relationships.
- **Estimated Hours:** 4 hours
- **Assignee:** TBD
- **Status:** Completed
- **Dependencies:** T1-T6 (needs complete implementation)
- **Acceptance Criteria:**
  - [x] Tests for implements transitive closure
  - [x] Tests for extends transitive closure
  - [x] Tests for depth limits
  - [x] Tests for result marking
  - [x] Test coverage >80%
- **Technical Notes:** Use test Neo4j database with known relationship graph. Create test data with interface → abstract → concrete hierarchy. Verify transitive results are correct. Test edge cases (cycles, multiple paths).
- **Implementation Notes:** Added TransitiveSearchComprehensiveTest (implements/extends closure with mocked graph hierarchy, depth limits, result marking). Extended SearchOrchestratorTest with orchestrate_whenTransitiveTrueAndGraphReturnsEntitiesWithPath_marksMergedResultsWithTransitivePath. Extended GraphQueryServiceStubTest with non-structural query returns empty, implements with relationship path, extends with multiple entities/paths. Extended SearchResourceTest with valid depth boundaries (depth=1, depth=10). All transitive-related tests (StructuralQueryParser, Implements/ExtendsClosureQuery, Neo4j*ClosureQuery, GraphQueryServiceStub, SearchOrchestrator, SearchResource, SearchResult) contribute to >80% coverage for transitive search.

---

## AC3: Find usages including polymorphic call sites (no separate task)

- **Implementation:** Added `usages:TypeName` structural predicate. When `transitive=true`, GraphQueryServiceStub returns the type plus all implementations (ImplementsClosureQuery) and all subclasses (ExtendsClosureQuery), so search results include the type and every subtype—covering polymorphic call sites. StructuralQueryParser.parseUsagesTarget(); GraphQueryServiceStub handles usages by combining both closures and the root type (deduped). API doc and demo script updated.
- **Tests:** StructuralQueryParserTest (parseUsagesTarget); GraphQueryServiceStubTest (usages returns type+impls+subclasses, type+subclasses only, root only when both empty).

---

## Summary
- **Total Tasks:** 7
- **Total Estimated Hours:** 24 hours
- **Story Points:** 5 (1 SP ≈ 4.8 hours, aligns with estimate)

