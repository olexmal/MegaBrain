# Tasks for US-04-03: RAG REST Endpoint

## Story Reference
- **Epic:** EPIC-04 (REST API & CLI)
- **Story:** US-04-03
- **Story Points:** 3
- **Sprint Target:** Sprint 5

## Task List

### T1: Create RAG endpoint in resource class
- **Description:** Create RAG endpoint in existing resource class or new `RagResource` class. Define path `/api/v1/rag`. Use Quarkus RESTEasy Reactive. Support both streaming and non-streaming responses.
- **Estimated Hours:** 2 hours
- **Assignee:** TBD
- **Status:** Not Started
- **Dependencies:** US-03-04 (needs RAG service)
- **Acceptance Criteria:**
  - [ ] RAG endpoint created
  - [ ] Path `/api/v1/rag` defined
  - [ ] Supports streaming and non-streaming
  - [ ] CDI injection working
- **Technical Notes:** Use Quarkus RESTEasy Reactive. Create `RagResource` class or add to existing resource. Annotate with `@Path("/api/v1/rag")`.

### T2: Implement POST with SSE response
- **Description:** Implement POST endpoint that accepts question in request body and returns SSE stream of tokens. Handle `stream` query parameter (default: true). Format tokens as SSE events.
- **Estimated Hours:** 4 hours
- **Assignee:** TBD
- **Status:** Not Started
- **Dependencies:** T1 (needs endpoint), US-03-04 (needs streaming)
- **Acceptance Criteria:**
  - [ ] POST endpoint accepts question
  - [ ] Returns SSE stream when stream=true
  - [ ] Tokens formatted as SSE events
  - [ ] Stream parameter handled
- **Technical Notes:** Use `@POST` with `@Produces(MediaType.SERVER_SENT_EVENTS)`. Return `Multi<StreamEvent>`. Format: `event: token\ndata: {"token": "..."}\n\n`.

### T3: Create RAG request/response DTOs
- **Description:** Create `RagRequest` DTO (question, context_limit, model) and `RagResponse` DTO (answer, sources, model_used). Use Bean Validation. Make serializable to/from JSON.
- **Estimated Hours:** 3 hours
- **Assignee:** TBD
- **Status:** Not Started
- **Dependencies:** T2 (needs endpoint)
- **Acceptance Criteria:**
  - [ ] RagRequest DTO created
  - [ ] RagResponse DTO created
  - [ ] Validation annotations applied
  - [ ] Serializable to/from JSON
- **Technical Notes:** Use Java records or POJOs with Jackson annotations. Validate question is not empty. Include optional fields (context_limit, model).

### T4: Integrate with RagService
- **Description:** Integrate endpoint with RagService. Call RAG service with question and parameters. Handle streaming and non-streaming modes. Convert service responses to DTOs.
- **Estimated Hours:** 4 hours
- **Assignee:** TBD
- **Status:** Not Started
- **Dependencies:** T2, T3 (needs endpoint and DTOs), US-03-04 (needs RAG service)
- **Acceptance Criteria:**
  - [ ] Endpoint calls RAG service
  - [ ] Streaming mode integrated
  - [ ] Non-streaming mode integrated
  - [ ] Errors handled gracefully
- **Technical Notes:** Inject RagService. Call `ask()` method with question. Handle streaming via Multi, non-streaming via completion. Map responses to DTOs.

### T5: Add non-streaming option
- **Description:** Implement non-streaming response option when `stream=false`. Collect all tokens and return complete response in single JSON response. Maintain same response format.
- **Estimated Hours:** 3 hours
- **Assignee:** TBD
- **Status:** Not Started
- **Dependencies:** T4 (needs service integration)
- **Acceptance Criteria:**
  - [ ] Non-streaming option supported
  - [ ] Complete response returned
  - [ ] Same response format as streaming (when complete)
  - [ ] Performance acceptable
- **Technical Notes:** When stream=false, buffer tokens and return complete RagResponse. Use `@Produces(MediaType.APPLICATION_JSON)` for non-streaming.

### T6: Write integration tests
- **Description:** Create integration tests for RAG endpoint. Test POST requests, streaming responses, non-streaming responses, error handling, and source attribution. Use REST Assured or similar.
- **Estimated Hours:** 4 hours
- **Assignee:** TBD
- **Status:** Not Started
- **Dependencies:** T1-T5 (needs complete implementation)
- **Acceptance Criteria:**
  - [ ] Integration tests for endpoint
  - [ ] Tests cover streaming mode
  - [ ] Tests cover non-streaming mode
  - [ ] Tests verify source attribution
  - [ ] Test coverage >80%
- **Technical Notes:** Use REST Assured or Quarkus test framework. Test with mock RAG service. Verify SSE events and complete responses.

---

## Summary
- **Total Tasks:** 6
- **Total Estimated Hours:** 20 hours
- **Story Points:** 3 (1 SP ≈ 6.7 hours, aligns with estimate)

