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
- **Status:** Completed
- **Dependencies:** US-03-04 (needs RAG service)
- **Acceptance Criteria:**
  - [x] RAG endpoint created
  - [x] Path `/api/v1/rag` defined
  - [x] Supports streaming and non-streaming
  - [x] CDI injection working
- **Technical Notes:** Use Quarkus RESTEasy Reactive. Create `RagResource` class or add to existing resource. Annotate with `@Path("/api/v1/rag")`. Implemented: `RagResource` exists at `@Path("/rag")` with `MegaBrainApplication` `@ApplicationPath("/api/v1")`, so effective path is `/api/v1/rag`. POST supports `stream=true` (Multi SSE) and `stream=false` (Uni&lt;Response&gt;). CDI constructor injection for RagService.

### T2: Implement POST with SSE response
- **Description:** Implement POST endpoint that accepts question in request body and returns SSE stream of tokens. Handle `stream` query parameter (default: true). Format tokens as SSE events.
- **Estimated Hours:** 4 hours
- **Assignee:** TBD
- **Status:** Completed
- **Dependencies:** T1 (needs endpoint), US-03-04 (needs streaming)
- **Acceptance Criteria:**
  - [x] POST endpoint accepts question
  - [x] Returns SSE stream when stream=true
  - [x] Tokens formatted as SSE events
  - [x] Stream parameter handled
- **Technical Notes:** Use `@POST` with `@Produces(MediaType.SERVER_SENT_EVENTS)`. Return `Multi<StreamEvent>`. Format: `event: token\ndata: {"token": "..."}\n\n`. Implemented: main POST returns Multi&lt;String&gt; of SSE lines when stream=true (class-level @Produces includes SERVER_SENT_EVENTS). TokenStreamEvent serialized as event: token + data JSON. stream query param @DefaultValue("true"). Unit tests in RagResourceTest cover streaming, stream=false, and error recovery.

### T3: Create RAG request/response DTOs
- **Description:** Create `RagRequest` DTO (question, context_limit, model) and `RagResponse` DTO (answer, sources, model_used). Use Bean Validation. Make serializable to/from JSON.
- **Estimated Hours:** 3 hours
- **Assignee:** TBD
- **Status:** Completed
- **Dependencies:** T2 (needs endpoint)
- **Acceptance Criteria:**
  - [x] RagRequest DTO created
  - [x] RagResponse DTO created
  - [x] Validation annotations applied
  - [x] Serializable to/from JSON
- **Technical Notes:** Use Java records or POJOs with Jackson annotations. Validate question is not empty. Include optional fields (context_limit, model).

### T4: Integrate with RagService
- **Description:** Integrate endpoint with RagService. Call RAG service with question and parameters. Handle streaming and non-streaming modes. Convert service responses to DTOs.
- **Estimated Hours:** 4 hours
- **Assignee:** TBD
- **Status:** Completed
- **Dependencies:** T2, T3 (needs endpoint and DTOs), US-03-04 (needs RAG service)
- **Acceptance Criteria:**
  - [x] Endpoint calls RAG service
  - [x] Streaming mode integrated
  - [x] Non-streaming mode integrated
  - [x] Errors handled gracefully
- **Technical Notes:** Inject RagService. Call `ask()` method with question. Handle streaming via Multi, non-streaming via completion. Map responses to DTOs. Implemented: RagResource calls ragService.ask(question) for non-streaming and ragService.streamTokens(question) for streaming; streaming uses onFailure.recoverWithItem to emit error SSE event; non-streaming uses onFailure.recoverWithItem to return 503 with safe JSON body. context_limit and model from RagRequest documented as deferred (not yet passed to RagService).

### T5: Add non-streaming option
- **Description:** Implement non-streaming response option when `stream=false`. Collect all tokens and return complete response in single JSON response. Maintain same response format.
- **Estimated Hours:** 3 hours
- **Assignee:** TBD
- **Status:** Completed
- **Dependencies:** T4 (needs service integration)
- **Acceptance Criteria:**
  - [x] Non-streaming option supported
  - [x] Complete response returned
  - [x] Same response format as streaming (when complete)
  - [x] Performance acceptable
- **Technical Notes:** When stream=false, buffer tokens and return complete RagResponse. Use `@Produces(MediaType.APPLICATION_JSON)` for non-streaming. Implemented: RagResource branches on stream=false and returns ragService.ask(question).map(r -> Response.ok(r).type(MediaType.APPLICATION_JSON).build()). RagService.ask() buffers tokens via streamTokens().collect().asList() and returns RagResponse (answer, sources, source_metadata, model_used). Unit test added for Content-Type application/json.

### T6: Write integration tests
- **Description:** Create integration tests for RAG endpoint. Test POST requests, streaming responses, non-streaming responses, error handling, and source attribution. Use REST Assured or similar.
- **Estimated Hours:** 4 hours
- **Assignee:** TBD
- **Status:** Completed
- **Dependencies:** T1-T5 (needs complete implementation)
- **Acceptance Criteria:**
  - [x] Integration tests for endpoint
  - [x] Tests cover streaming mode
  - [x] Tests cover non-streaming mode
  - [x] Tests verify source attribution
  - [x] Test coverage >80%
- **Technical Notes:** Use REST Assured or Quarkus test framework. Test with mock RAG service. Verify SSE events and complete responses. Implemented: RagResourceIT in src/test/java/io/megabrain/api with @QuarkusTest and @InjectMock RagService. Tests hit POST /api/v1/rag (Accept: application/json for non-streaming) and POST /api/v1/rag/stream for SSE. RagResource refactored to ragJson() (Uni&lt;Response&gt;) and ragStream() (Multi&lt;String&gt;) for correct Quarkus serialization; content negotiation by Accept. Unit tests RagResourceTest updated to call ragStream/ragJson.

---

## Summary
- **Total Tasks:** 6
- **Total Estimated Hours:** 20 hours
- **Story Points:** 3 (1 SP ≈ 6.7 hours, aligns with estimate)

