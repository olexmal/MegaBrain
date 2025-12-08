# Tasks for US-03-04: SSE Token Streaming

## Story Reference
- **Epic:** EPIC-03 (RAG Answer Generation)
- **Story:** US-03-04
- **Story Points:** 3
- **Sprint Target:** Sprint 4

## Task List

### T1: Implement SSE response builder in RagService
- **Description:** Implement Server-Sent Events (SSE) response builder in RagService. Create endpoint that returns `Multi<StreamEvent>` for streaming LLM responses. Handle SSE format (event: token, data: {...}). Integrate with Quarkus reactive REST.
- **Estimated Hours:** 4 hours
- **Assignee:** TBD
- **Status:** Not Started
- **Dependencies:** US-03-01 or US-03-02 (needs LLM client)
- **Acceptance Criteria:**
  - [ ] SSE endpoint returns streaming response
  - [ ] Events formatted correctly (event: token, data: {...})
  - [ ] Proper SSE headers set
  - [ ] Response is reactive (non-blocking)
- **Technical Notes:** Use Quarkus RESTEasy Reactive with `@Produces(MediaType.SERVER_SENT_EVENTS)`. Use Mutiny's `Multi` for reactive streams. Format: `event: token\ndata: {"token": "..."}\n\n`.

### T2: Integrate LangChain4j streaming callback
- **Description:** Integrate LangChain4j's streaming callback to receive tokens as they're generated from LLM. Convert streaming tokens into SSE events. Handle both OpenAI and Ollama streaming formats.
- **Estimated Hours:** 5 hours
- **Assignee:** TBD
- **Status:** Not Started
- **Dependencies:** T1 (needs SSE endpoint), US-03-01, US-03-02 (needs LLM clients)
- **Acceptance Criteria:**
  - [ ] Streaming callback integrated
  - [ ] Tokens converted to SSE events
  - [ ] Works with OpenAI streaming
  - [ ] Works with Ollama streaming
- **Technical Notes:** Use LangChain4j's StreamingResponseHandler. Implement TokenStreamHandler that emits SSE events. Handle different streaming formats (OpenAI delta, Ollama response chunks).

### T3: Handle stream cancellation
- **Description:** Implement stream cancellation handling. Allow clients to cancel ongoing generation via connection close or explicit cancel request. Clean up resources (cancel LLM request, close connections) when stream is cancelled.
- **Estimated Hours:** 3 hours
- **Assignee:** TBD
- **Status:** Not Started
- **Dependencies:** T1, T2 (needs streaming working)
- **Acceptance Criteria:**
  - [ ] Stream cancellation supported
  - [ ] Resources cleaned up on cancellation
  - [ ] LLM request cancelled if possible
  - [ ] Client notified of cancellation
- **Technical Notes:** Handle connection close events. Cancel underlying LLM request if API supports it. Emit cancellation event to client. Clean up any resources (connections, buffers).

### T4: Add error event for failures
- **Description:** Implement error event emission for stream failures. When LLM generation fails or stream is interrupted, emit error event with error details. Ensure error events are properly formatted and client can handle them.
- **Estimated Hours:** 3 hours
- **Assignee:** TBD
- **Status:** Not Started
- **Dependencies:** T1, T2 (needs streaming working)
- **Acceptance Criteria:**
  - [ ] Error events emitted on failures
  - [ ] Error details included in event
  - [ ] Error events properly formatted
  - [ ] Stream closed after error
- **Technical Notes:** Emit error event: `event: error\ndata: {"message": "...", "code": "..."}\n\n`. Include error type, message, and optional stack trace. Close stream after error.

### T5: Add non-streaming fallback option
- **Description:** Implement non-streaming response option. When `stream=false` parameter is provided, collect all tokens and return complete response in single JSON response. Maintain same response format for consistency.
- **Estimated Hours:** 3 hours
- **Assignee:** TBD
- **Status:** Not Started
- **Dependencies:** T1, T2 (needs streaming working)
- **Acceptance Criteria:**
  - [ ] Non-streaming option supported
  - [ ] Complete response returned
  - [ ] Same response format as streaming (when complete)
  - [ ] Performance acceptable
- **Technical Notes:** Add `stream` query parameter (default: true). When false, buffer tokens and return complete response. Use same RagResponse DTO for consistency.

### T6: Write integration tests for streaming
- **Description:** Create integration tests for SSE streaming functionality. Test token streaming, cancellation, error handling, and non-streaming fallback. Use HTTP client that supports SSE for testing.
- **Estimated Hours:** 4 hours
- **Assignee:** TBD
- **Status:** Not Started
- **Dependencies:** T1-T5 (needs complete implementation)
- **Acceptance Criteria:**
  - [ ] Integration tests for streaming
  - [ ] Tests verify token events
  - [ ] Tests verify cancellation
  - [ ] Tests verify error handling
  - [ ] Test coverage >80%
- **Technical Notes:** Use Java 11+ HttpClient or OkHttp for SSE testing. Parse SSE events and verify content. Test with mock LLM responses. Test both streaming and non-streaming modes.

---

## Summary
- **Total Tasks:** 6
- **Total Estimated Hours:** 22 hours
- **Story Points:** 3 (1 SP ≈ 7.3 hours, aligns with estimate)

