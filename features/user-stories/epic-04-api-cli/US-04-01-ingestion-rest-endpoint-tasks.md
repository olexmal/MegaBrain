# Tasks for US-04-01: Ingestion REST Endpoint

## Story Reference
- **Epic:** EPIC-04 (REST API & CLI)
- **Story:** US-04-01
- **Story Points:** 3
- **Sprint Target:** Sprint 2

## Task List

### T1: Create IngestionResource JAX-RS class
- **Description:** Create `IngestionResource` JAX-RS REST resource class using Quarkus RESTEasy Reactive. Define base path `/api/v1/ingest`. Use CDI for dependency injection. Follow RESTful conventions.
- **Estimated Hours:** 2 hours
- **Assignee:** TBD
- **Status:** Not Started
- **Dependencies:** US-01-01 (needs ingestion service)
- **Acceptance Criteria:**
  - [ ] IngestionResource class created
  - [ ] Base path `/api/v1/ingest` defined
  - [ ] CDI injection working
  - [ ] Follows REST conventions
- **Technical Notes:** Use Quarkus RESTEasy Reactive. Annotate with `@Path("/api/v1/ingest")`. Inject RepositoryIngestionService via CDI.

### T2: Implement POST endpoint with path parameter
- **Description:** Implement POST endpoint with path parameter for source type: `/api/v1/ingest/{source}`. Support sources: github, gitlab, bitbucket, local. Validate source parameter. Route to appropriate ingestion service.
- **Estimated Hours:** 3 hours
- **Assignee:** TBD
- **Status:** Not Started
- **Dependencies:** T1 (needs resource class)
- **Acceptance Criteria:**
  - [ ] POST endpoint with path parameter implemented
  - [ ] Supports github, gitlab, bitbucket, local sources
  - [ ] Source parameter validated
  - [ ] Routes to correct service
- **Technical Notes:** Use `@POST @Path("/{source}")` annotation. Validate source enum. Return 400 Bad Request for invalid source.

### T3: Create ingestion request DTO
- **Description:** Create `IngestionRequest` DTO class for request body. Include fields: repository (String), branch (String, optional), token (String, optional), incremental (boolean, optional). Use Bean Validation annotations.
- **Estimated Hours:** 2 hours
- **Assignee:** TBD
- **Status:** Not Started
- **Dependencies:** T2 (needs endpoint)
- **Acceptance Criteria:**
  - [ ] IngestionRequest DTO created
  - [ ] Includes all required fields
  - [ ] Validation annotations applied
  - [ ] Serializable from JSON
- **Technical Notes:** Use Java record or POJO with Jackson annotations. Add `@NotNull` for repository. Use `@JsonProperty` for JSON mapping.

### T4: Integrate with RepositoryIngestionService
- **Description:** Integrate endpoint with RepositoryIngestionService. Call ingestion service with request parameters. Handle service responses and errors. Return appropriate HTTP status codes.
- **Estimated Hours:** 3 hours
- **Assignee:** TBD
- **Status:** Not Started
- **Dependencies:** T2, T3 (needs endpoint and DTO), US-01-01 (needs service)
- **Acceptance Criteria:**
  - [ ] Endpoint calls ingestion service
  - [ ] Request parameters passed correctly
  - [ ] Errors handled gracefully
  - [ ] Appropriate HTTP status codes returned
- **Technical Notes:** Inject RepositoryIngestionService. Call `ingest()` method with parameters. Handle exceptions and map to HTTP status codes (400, 500, etc.).

### T5: Return SSE stream from Mutiny Multi
- **Description:** Implement Server-Sent Events (SSE) response for progress streaming. Return `Multi<StreamEvent>` from endpoint. Format as SSE events. Handle client disconnections.
- **Estimated Hours:** 4 hours
- **Assignee:** TBD
- **Status:** Not Started
- **Dependencies:** T4 (needs service integration), US-01-07 (needs progress streaming)
- **Acceptance Criteria:**
  - [ ] SSE stream returned from endpoint
  - [ ] Progress events streamed correctly
  - [ ] Proper SSE headers set
  - [ ] Client disconnections handled
- **Technical Notes:** Use `@Produces(MediaType.SERVER_SENT_EVENTS)`. Return `Multi<StreamEvent>`. Format events as SSE: `event: progress\ndata: {...}\n\n`.

### T6: Write integration tests
- **Description:** Create integration tests for ingestion endpoint. Test POST requests, source validation, SSE streaming, error handling, and concurrent requests. Use REST Assured or similar.
- **Estimated Hours:** 3 hours
- **Assignee:** TBD
- **Status:** Not Started
- **Dependencies:** T1-T5 (needs complete implementation)
- **Acceptance Criteria:**
  - [ ] Integration tests for endpoint
  - [ ] Tests cover all source types
  - [ ] Tests verify SSE streaming
  - [ ] Tests cover error scenarios
  - [ ] Test coverage >80%
- **Technical Notes:** Use REST Assured or Quarkus test framework. Test with mock ingestion service. Verify SSE events are received correctly.

---

## Summary
- **Total Tasks:** 6
- **Total Estimated Hours:** 17 hours
- **Story Points:** 3 (1 SP ≈ 5.7 hours, aligns with estimate)

