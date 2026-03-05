# US-04-03: RAG REST Endpoint

## Story
**As a** developer  
**I want** to ask questions via REST API  
**So that** I can integrate code Q&A into my workflow

## Story Points: 3
## Priority: High
## Sprint Target: Sprint 5

---

## Acceptance Criteria

- [x] **AC1:** `POST /api/v1/rag` accepts question in request body
- [x] **AC2:** Request body: `{"question": "...", "context_limit": N}`
- [x] **AC3:** Response is SSE stream of tokens
- [x] **AC4:** Final response includes source attributions
- [x] **AC5:** Option for non-streaming response (`stream=false`)
- [ ] **AC6:** First token within 2s

---

## Demo Script

### Setup
1. Full RAG pipeline working
2. LLM configured

### Demo Steps
1. **Streaming Request:**
   ```bash
   curl -N -X POST "http://localhost:8080/api/v1/rag" \
     -d '{"question": "How is caching implemented?"}'
   ```
2. **Show Token Stream:** Tokens arriving incrementally
3. **Show Sources:** Final sources event
4. **Non-Streaming:**
   ```bash
   curl -X POST "http://localhost:8080/api/v1/rag?stream=false" \
     -d '{"question": "..."}'
   ```

### Expected Outcome
- Streaming answer with tokens
- Sources included
- Non-streaming option works

---

## Technical Tasks

- [x] **T1:** Create RAG endpoint in resource class (backend)
- [x] **T2:** Implement POST with SSE response (backend)
- [x] **T3:** Create RAG request/response DTOs (backend)
- [x] **T4:** Integrate with RagService (backend)
- [x] **T5:** Add non-streaming option (backend)
- [x] **T6:** Write integration tests (test)

---

## Dependencies

- **Blocked by:** US-03-04 (streaming), US-03-05 (attribution)
- **Enables:** US-05-05 (Chat UI), US-08-05 (MCP RAG tool)

---

## Definition of Ready

- [x] Acceptance criteria clear
- [x] Dependencies identified
- [x] Tech tasks estimated
- [x] Test scenarios defined
- [x] Demo script approved
- [x] No blockers

