# Tasks for US-08-05: RAG Query MCP Tool

## Story Reference
- **Epic:** EPIC-08 (MCP Tool Server)
- **Story:** US-08-05
- **Story Points:** 3
- **Sprint Target:** Sprint 6

## Task List

### T1: Implement ask_codebase tool
- **Description:** Implement `ask_codebase` MCP tool handler that performs RAG query on codebase. Tool should accept natural language question and return synthesized answer with citations. Integrate with RAG service.
- **Estimated Hours:** 5 hours
- **Assignee:** TBD
- **Status:** Not Started
- **Dependencies:** US-08-01 (needs MCP server), US-03-05 (needs RAG with attribution)
- **Acceptance Criteria:**
  - [ ] ask_codebase tool implemented
  - [ ] Accepts natural language question
  - [ ] Returns synthesized answer
  - [ ] Citations included
- **Technical Notes:** Create tool handler. Accept parameter: question (required), context_limit (optional). Call RAG service. Return answer with sources. Format citations for MCP response.

### T2: Integrate with RagService
- **Description:** Integrate ask_codebase tool with RagService. Call RAG service with question and parameters. Handle streaming and non-streaming responses. Convert RAG response to MCP tool response format.
- **Estimated Hours:** 4 hours
- **Assignee:** TBD
- **Status:** Not Started
- **Dependencies:** T1 (needs tool handler), US-04-03 (needs RAG API)
- **Acceptance Criteria:**
  - [ ] RagService integrated
  - [ ] Question passed to service
  - [ ] Response converted to MCP format
  - [ ] Sources included
- **Technical Notes:** Call RagService.ask() method. Handle both streaming and non-streaming. Convert response to MCP tool response. Include sources in response.

### T3: Add streaming support for MCP
- **Description:** Implement streaming support for MCP tool calls. Stream answer tokens as they're generated. Use MCP streaming protocol if supported. Handle stream completion and errors.
- **Estimated Hours:** 5 hours
- **Assignee:** TBD
- **Status:** Not Started
- **Dependencies:** T1, T2 (needs tool and RAG integration), US-03-04 (needs streaming)
- **Acceptance Criteria:**
  - [ ] Streaming support implemented
  - [ ] Tokens streamed as generated
  - [ ] Stream completion handled
  - [ ] Errors handled gracefully
- **Technical Notes:** Use MCP streaming protocol if available. Stream tokens via progress updates or partial results. Handle stream completion. Send final response with sources.

### T4: Format citations for MCP response
- **Description:** Format source citations for MCP tool response. Include source file paths, entity names, line numbers, and relevance scores. Format citations in clear, structured format.
- **Estimated Hours:** 3 hours
- **Assignee:** TBD
- **Status:** Not Started
- **Dependencies:** T1, T2 (needs tool and RAG integration)
- **Acceptance Criteria:**
  - [ ] Citations formatted for MCP
  - [ ] Source information included
  - [ ] Format is clear
  - [ ] Citations are useful
- **Technical Notes:** Format citations as structured data. Include: file_path, entity_name, line_range, relevance_score. Add citations to tool response. Make citations accessible.

### T5: Write tool tests
- **Description:** Create comprehensive tests for ask_codebase tool. Test question handling, answer generation, citation formatting, and streaming. Use MCP protocol test framework.
- **Estimated Hours:** 4 hours
- **Assignee:** TBD
- **Status:** Not Started
- **Dependencies:** T1-T4 (needs complete implementation)
- **Acceptance Criteria:**
  - [ ] Tests for ask_codebase tool
  - [ ] Tests cover question handling
  - [ ] Tests cover citations
  - [ ] Tests cover streaming
  - [ ] Test coverage >80%
- **Technical Notes:** Use MCP test framework or create custom tests. Mock RAG service. Test with various questions. Verify answer quality and citations.

---

## Summary
- **Total Tasks:** 5
- **Total Estimated Hours:** 21 hours
- **Story Points:** 3 (1 SP ≈ 7 hours, note: includes streaming which is complex)

