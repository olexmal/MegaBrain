# Tasks for US-05-05: RAG Chat Interface

## Story Reference
- **Epic:** EPIC-05 (Web Dashboard)
- **Story:** US-05-05
- **Story Points:** 5
- **Sprint Target:** Sprint 6

## Task List

### T1: Create chat page component
- **Description:** Create main chat page Angular component with message list and input area. Component should display conversation history, handle message input, and manage chat state. Use Angular Material for layout.
- **Estimated Hours:** 4 hours
- **Assignee:** TBD
- **Status:** Not Started
- **Dependencies:** US-04-03 (needs RAG API)
- **Acceptance Criteria:**
  - [ ] Chat page component created
  - [ ] Message list displayed
  - [ ] Input area at bottom
  - [ ] Responsive layout
- **Technical Notes:** Use Angular component with Material layout. Create message list and input area. Use flexbox for layout (input at bottom).

### T2: Create message component
- **Description:** Create reusable message component for displaying chat messages. Component should show user questions and AI answers. Support different message types (user, assistant). Format messages with proper styling.
- **Estimated Hours:** 3 hours
- **Assignee:** TBD
- **Status:** Not Started
- **Dependencies:** T1 (needs chat page)
- **Acceptance Criteria:**
  - [ ] Message component created
  - [ ] User and assistant messages styled
  - [ ] Message formatting works
  - [ ] Clear visual distinction
- **Technical Notes:** Use Angular component with Material card or list item. Style user messages (right-aligned) and assistant messages (left-aligned). Support markdown rendering.

### T3: Implement SSE streaming for tokens
- **Description:** Implement Server-Sent Events (SSE) integration for streaming token-by-token responses. Connect to RAG streaming endpoint. Update message content as tokens arrive. Handle stream completion and errors.
- **Estimated Hours:** 5 hours
- **Assignee:** TBD
- **Status:** Not Started
- **Dependencies:** T1, T2 (needs chat page and messages), US-04-03 (needs streaming API)
- **Acceptance Criteria:**
  - [ ] SSE streaming implemented
  - [ ] Tokens displayed as received
  - [ ] Stream completion handled
  - [ ] Errors handled gracefully
- **Technical Notes:** Use EventSource or Angular HTTP client with SSE. Append tokens to message content. Show loading indicator during streaming.

### T4: Create citation component with link
- **Description:** Create citation component that displays source citations with clickable links. Component should parse citations from answer text, display them as links, and allow clicking to view source code. Format citations clearly.
- **Estimated Hours:** 4 hours
- **Assignee:** TBD
- **Status:** Not Started
- **Dependencies:** T2 (needs message component), US-03-05 (needs source attribution)
- **Acceptance Criteria:**
  - [ ] Citation component created
  - [ ] Citations parsed from text
  - [ ] Citations displayed as links
  - [ ] Click opens code preview
- **Technical Notes:** Parse citation format `[Source: path:line]` from answer text. Create clickable links. Open code preview on click.

### T5: Add conversation state management
- **Description:** Implement conversation state management to maintain chat history. Store messages in component state or service. Support clearing conversation. Persist conversation in local storage (optional). Handle conversation context.
- **Estimated Hours:** 4 hours
- **Assignee:** TBD
- **Status:** Not Started
- **Dependencies:** T1, T2 (needs chat page and messages)
- **Acceptance Criteria:**
  - [ ] Conversation state managed
  - [ ] Chat history maintained
  - [ ] Clear conversation works
  - [ ] State persists (optional)
- **Technical Notes:** Use Angular service or component state. Store messages array. Add clear button. Optionally persist to local storage.

### T6: Write component tests
- **Description:** Create unit tests for chat interface components. Test message rendering, SSE streaming, citation parsing, conversation management, and API integration. Use Angular testing utilities.
- **Estimated Hours:** 4 hours
- **Assignee:** TBD
- **Status:** Not Started
- **Dependencies:** T1-T5 (needs complete implementation)
- **Acceptance Criteria:**
  - [ ] Unit tests for all components
  - [ ] Tests cover streaming
  - [ ] Tests cover citations
  - [ ] Test coverage >80%
- **Technical Notes:** Use Angular TestBed. Mock SSE service and API calls. Test component interactions and state changes.

---

## Summary
- **Total Tasks:** 6
- **Total Estimated Hours:** 24 hours
- **Story Points:** 5 (1 SP ≈ 4.8 hours, aligns with estimate)

