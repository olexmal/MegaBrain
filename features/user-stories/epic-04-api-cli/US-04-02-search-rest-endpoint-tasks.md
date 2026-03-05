# Tasks for US-04-02: Search REST Endpoint

## Story Reference
- **Epic:** EPIC-04 (REST API & CLI)
- **Story:** US-04-02
- **Story Points:** 3
- **Sprint Target:** Sprint 3

## Task List

### T1: Create SearchResource JAX-RS class
- **Description:** Create `SearchResource` JAX-RS REST resource class using Quarkus RESTEasy Reactive. Define base path `/api/v1/search`. Use CDI for dependency injection. Follow RESTful conventions.
- **Estimated Hours:** 2 hours
- **Assignee:** TBD
- **Status:** Completed
- **Dependencies:** US-02-03 (needs search service)
- **Acceptance Criteria:**
  - [x] SearchResource class created
  - [x] Base path `/api/v1/search` defined
  - [x] CDI injection working
  - [x] Follows REST conventions
- **Technical Notes:** Use Quarkus RESTEasy Reactive. Annotate with `@Path("/api/v1/search")`. Inject search service via CDI. **Implementation note:** SearchResource already existed at `backend/src/main/java/io/megabrain/api/SearchResource.java` with `@Path("/search")`; effective path is `/api/v1/search` via `MegaBrainApplication@ApplicationPath("/api/v1")`. CDI constructor injection of `SearchOrchestrator` in place. Verified with `mvn compile` and `SearchResourceTest`.

### T2: Implement GET endpoint with query params
- **Description:** Implement GET endpoint with query parameters: `q` (query), `language`, `repository`, `entity_type`, `limit`, `offset`, `mode` (hybrid/keyword/vector). Parse and validate parameters. Handle optional parameters.
- **Estimated Hours:** 4 hours
- **Assignee:** TBD
- **Status:** Completed
- **Dependencies:** T1 (needs resource class)
- **Acceptance Criteria:**
  - [x] GET endpoint with query parameters implemented
  - [x] All parameters parsed correctly
  - [x] Optional parameters handled
  - [x] Parameter validation applied
- **Technical Notes:** Use `@GET` with `@QueryParam` annotations. Validate query string is not empty. Set default values for limit (10) and offset (0). **Implementation note:** SearchResource already had GET `search()` with `@QueryParam` for q, language, repository, file_path, entity_type, limit, offset, mode; query validated (required, non-blank); limit/offset default to 10 and 0; optional filters and mode (hybrid/keyword/vector) with default HYBRID; SearchRequest.validate() and depth validation for transitive. Verified with mvn compile and SearchResourceTest.

### T3: Create search response DTO
- **Description:** Create `SearchResponse` DTO class for response. Include fields: results (List<SearchResult>), total (long), page (int), size (int), facets (Map<String, List<FacetValue>>). Make it serializable to JSON.
- **Estimated Hours:** 3 hours
- **Assignee:** TBD
- **Status:** Completed
- **Dependencies:** T2 (needs endpoint)
- **Acceptance Criteria:**
  - [x] SearchResponse DTO created
  - [x] Includes all required fields
  - [x] Serializable to JSON
  - [x] Includes pagination metadata
- **Technical Notes:** Use Java record or POJO with Jackson annotations. Include SearchResult list, total count, pagination info, and facets. Use proper JSON property names. **Implementation note:** SearchResponse already existed at `backend/src/main/java/io/megabrain/api/SearchResponse.java` with all required fields (results, total, page, size, facets), Jackson @JsonProperty, no-arg constructor for deserialization, and pagination helpers (hasNextPage, hasPreviousPage, getTotalPages). Verified with `mvn compile` and `SearchResponseTest`.

### T4: Integrate with MegaBrainOrchestrator
- **Description:** Integrate endpoint with search orchestrator/service. Call search service with query parameters. Convert search results to DTOs. Handle errors and return appropriate HTTP status codes.
- **Estimated Hours:** 4 hours
- **Assignee:** TBD
- **Status:** Completed
- **Dependencies:** T2, T3 (needs endpoint and DTO), US-02-03 (needs search service)
- **Acceptance Criteria:**
  - [x] Endpoint calls search service
  - [x] Query parameters passed correctly
  - [x] Results converted to DTOs
  - [x] Errors handled gracefully
- **Technical Notes:** Inject search service. Call search method with parameters. Map results to SearchResult DTOs. Handle exceptions and return 500 for server errors. **Implementation note:** SearchResource injects SearchOrchestrator, calls orchestrate(searchRequest, searchMode, facetLimit, effectiveDepth), maps MergedResult to SearchResult via convertToSearchResult(), and uses onFailure().recoverWithItem() to return 500 with ErrorResponse. Verified with mvn compile and SearchResourceTest.

### T5: Add pagination logic
- **Description:** Implement pagination logic using offset and limit parameters. Calculate total count, page number, and page size. Apply pagination to search results before returning.
- **Estimated Hours:** 3 hours
- **Assignee:** TBD
- **Status:** Completed
- **Dependencies:** T4 (needs service integration)
- **Acceptance Criteria:**
  - [x] Pagination implemented
  - [x] Offset and limit applied correctly
  - [x] Total count calculated
  - [x] Page metadata included in response
- **Technical Notes:** Apply limit and offset to search results. Calculate total count from search service. Include pagination metadata in response (total, page, size, hasMore). **Implementation note:** SearchResource applies pagination: uses offset/limit from SearchRequest, computes page = offset/limit, applies subList(fromIndex, toIndex) to results, builds SearchResponse with total (from results.size()), page, and size. SearchResponse includes total, page, size and helpers hasNextPage(), hasPreviousPage(), getTotalPages(). Verified with mvn compile and SearchResourceTest (including search_withPagination_shouldApplyOffsetAndLimit).

### T6: Write integration tests
- **Description:** Create integration tests for search endpoint. Test GET requests, query parameters, filters, pagination, error handling, and response format. Use REST Assured or similar.
- **Estimated Hours:** 3 hours
- **Assignee:** TBD
- **Status:** Completed
- **Dependencies:** T1-T5 (needs complete implementation)
- **Acceptance Criteria:**
  - [x] Integration tests for endpoint
  - [x] Tests cover all query parameters
  - [x] Tests verify pagination
  - [x] Tests cover error scenarios
  - [x] Test coverage >80%
- **Technical Notes:** Use REST Assured or Quarkus test framework. Test with mock search service. Verify response structure and pagination. **Implementation note:** Added SearchResourceIntegrationTest (QuarkusTest + REST Assured, @InjectMock SearchOrchestrator). Tests cover GET /api/v1/search with query params, pagination, error scenarios (missing q, invalid depth), modes, facets, and service failure. Combined with SearchResourceTest for >80% coverage.

---

## Summary
- **Total Tasks:** 6
- **Total Estimated Hours:** 19 hours
- **Story Points:** 3 (1 SP ≈ 6.3 hours, aligns with estimate)

