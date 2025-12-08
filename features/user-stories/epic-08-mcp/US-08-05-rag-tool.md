# US-08-05: RAG Query MCP Tool

## Story
**As an** LLM (Cursor/Claude)  
**I want** a RAG query tool via MCP  
**So that** I can get synthesized answers about code

## Story Points: 3
## Priority: High
## Sprint Target: Sprint 6

---

## Acceptance Criteria

- [ ] **AC1:** `ask_codebase` tool accepts natural language question
- [ ] **AC2:** Returns synthesized answer with citations
- [ ] **AC3:** Context_limit parameter supported
- [ ] **AC4:** Sources included in response
- [ ] **AC5:** Streaming supported for long answers

---

## Demo Script

### Demo Steps
1. **Ask Question:**
   ```json
   {"tool": "ask_codebase", "arguments": {"question": "How does authentication work?"}}
   ```
2. **Show Answer:** Display synthesized response
3. **Show Sources:** Display cited files/methods
4. **Streaming:** Demo streaming response

### Expected Outcome
- Natural language answers
- Citations included
- Streaming works

---

## Technical Tasks

- [ ] **T1:** Implement `ask_codebase` tool (backend)
- [ ] **T2:** Integrate with RagService (backend)
- [ ] **T3:** Add streaming support for MCP (backend)
- [ ] **T4:** Format citations for MCP response (backend)
- [ ] **T5:** Write tool tests (test)

---

## Dependencies

- **Blocked by:** US-08-01 (MCP server), US-03-05 (RAG with attribution)
- **Enables:** Complete LLM integration

---

## Definition of Ready

- [x] All criteria met

