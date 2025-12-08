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
- **Status:** Not Started
- **Dependencies:** US-02-03 (needs search service)
- **Acceptance Criteria:**
  - [ ] SearchResource class created
  - [ ] Base path `/api/v1/search` defined
  - [ ] CDI injection working
  - [ ] Follows REST conventions
- **Technical Notes:** Use Quarkus RESTEasy Reactive. Annotate with `@Path("/api/v1/search")`. Inject search service via CDI.

### T2: Implement GET endpoint with query params
- **Description:** Implement GET endpoint with query parameters: `q` (query), `language`, `repository`, `entity_type`, `limit`, `offset`, `mode` (hybrid/keyword/vector). Parse and validate parameters. Handle optional parameters.
- **Estimated Hours:** 4 hours
- **Assignee:** TBD
- **Status:** Not Started
- **Dependencies:** T1 (needs resource class)
- **Acceptance Criteria:**
  - [ ] GET endpoint with query parameters implemented
  - [ ] All parameters parsed correctly
  - [ ] Optional parameters handled
  - [ ] Parameter validation applied
- **Technical Notes:** Use `@GET` with `@QueryParam` annotations. Validate query string is not empty. Set default values for limit (10) and offset (0).

### T3: Create search response DTO
- **Description:** Create `SearchResponse` DTO class for response. Include fields: results (List<SearchResult>), total (long), page (int), size (int), facets (Map<String, List<FacetValue>>). Make it serializable to JSON.
- **Estimated Hours:** 3 hours
- **Assignee:** TBD
- **Status:** Not Started
- **Dependencies:** T2 (needs endpoint)
- **Acceptance Criteria:**
  - [ ] SearchResponse DTO created
  - [ ] Includes all required fields
  - [ ] Serializable to JSON
  - [ ] Includes pagination metadata
- **Technical Notes:** Use Java record or POJO with Jackson annotations. Include SearchResult list, total count, pagination info, and facets. Use proper JSON property names.

### T4: Integrate with MegaBrainOrchestrator
- **Description:** Integrate endpoint with search orchestrator/service. Call search service with query parameters. Convert search results to DTOs. Handle errors and return appropriate HTTP status codes.
- **Estimated Hours:** 4 hours
- **Assignee:** TBD
- **Status:** Not Started
- **Dependencies:** T2, T3 (needs endpoint and DTO), US-02-03 (needs search service)
- **Acceptance Criteria:**
  - [ ] Endpoint calls search service
  - [ ] Query parameters passed correctly
  - [ ] Results converted to DTOs
  - [ ] Errors handled gracefully
- **Technical Notes:** Inject search service. Call search method with parameters. Map results to SearchResult DTOs. Handle exceptions and return 500 for server errors.

### T5: Add pagination logic
- **Description:** Implement pagination logic using offset and limit parameters. Calculate total count, page number, and page size. Apply pagination to search results before returning.
- **Estimated Hours:** 3 hours
- **Assignee:** TBD
- **Status:** Not Started
- **Dependencies:** T4 (needs service integration)
- **Acceptance Criteria:**
  - [ ] Pagination implemented
  - [ ] Offset and limit applied correctly
  - [ ] Total count calculated
  - [ ] Page metadata included in response
- **Technical Notes:** Apply limit and offset to search results. Calculate total count from search service. Include pagination metadata in response (total, page, size, hasMore).

### T6: Write integration tests
- **Description:** Create integration tests for search endpoint. Test GET requests, query parameters, filters, pagination, error handling, and response format. Use REST Assured or similar.
- **Estimated Hours:** 3 hours
- **Assignee:** TBD
- **Status:** Not Started
- **Dependencies:** T1-T5 (needs complete implementation)
- **Acceptance Criteria:**
  - [ ] Integration tests for endpoint
  - [ ] Tests cover all query parameters
  - [ ] Tests verify pagination
  - [ ] Tests cover error scenarios
  - [ ] Test coverage >80%
- **Technical Notes:** Use REST Assured or Quarkus test framework. Test with mock search service. Verify response structure and pagination.

---

## Summary
- **Total Tasks:** 6
- **Total Estimated Hours:** 19 hours
- **Story Points:** 3 (1 SP ≈ 6.3 hours, aligns with estimate)

