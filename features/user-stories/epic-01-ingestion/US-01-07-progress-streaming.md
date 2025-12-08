# US-01-07: Real-Time Progress Streaming

## Story
**As a** user triggering ingestion  
**I want** to see real-time progress during ingestion  
**So that** I know the system is working and can estimate completion

## Story Points: 3
## Priority: High
## Sprint Target: Sprint 2

---

## Acceptance Criteria

- [ ] **AC1:** SSE endpoint streams progress events
- [ ] **AC2:** Events include: stage, message, percentage, timestamp
- [ ] **AC3:** Stages: "Cloning", "Parsing", "Indexing", "Complete", "Failed"
- [ ] **AC4:** Progress updates at least every 5 seconds during processing
- [ ] **AC5:** File-level progress shown during parsing (X of Y files)
- [ ] **AC6:** Error events streamed with details if ingestion fails
- [ ] **AC7:** Client can receive events for in-progress jobs

---

## Demo Script

### Setup
1. Ensure MegaBrain backend is running
2. Have a medium-sized repository ready (100+ files)

### Demo Steps
1. **Start Ingestion:** Trigger ingestion and capture SSE stream
   ```bash
   curl -N "http://localhost:8080/api/v1/ingest/github?stream=true" \
     -d '{"repository": "..."}'
   ```
2. **Show Events:** Display real-time events in terminal
   ```
   event: progress
   data: {"stage": "Cloning", "message": "Cloning repository...", "percentage": 10}
   
   event: progress
   data: {"stage": "Parsing", "message": "Parsing file 50/200", "percentage": 35}
   ```
3. **Show Dashboard:** (If available) Show progress bar updating
4. **Error Demo:** Trigger a failure and show error event

### Expected Outcome
- Continuous progress updates during ingestion
- Clear stage transitions
- Accurate percentage completion

---

## Technical Tasks

- [ ] **T1:** Define `StreamEvent` record/class (backend)
- [ ] **T2:** Implement SSE endpoint using Mutiny Multi (backend)
- [ ] **T3:** Emit progress events during cloning (backend)
- [ ] **T4:** Emit progress events during parsing (backend)
- [ ] **T5:** Emit progress events during indexing (backend)
- [ ] **T6:** Emit completion/error events (backend)
- [ ] **T7:** Write integration test for SSE streaming (test)

---

## Test Scenarios

| Scenario | Given | When | Then |
|:---------|:------|:-----|:-----|
| Normal ingestion | Valid repository | Ingestion starts | Progress events streamed |
| Clone progress | Large repo cloning | During clone | Clone percentage updates |
| Parse progress | Many files parsing | During parse | File count progress |
| Completion | Ingestion finishes | On complete | Complete event sent |
| Failure | Invalid repo URL | Ingestion fails | Error event with message |
| Reconnect | Client disconnects/reconnects | During ingestion | Can resume receiving events |

---

## Dependencies

- **Blocked by:** US-01-01 (needs ingestion to stream)
- **Enables:** US-05-01 (Dashboard needs progress), US-04-01 (API returns stream)

---

## Risks

| Risk | Impact | Likelihood | Mitigation |
|:-----|:-------|:-----------|:-----------|
| SSE connection drops | Lost progress visibility | Medium | Client reconnection logic |
| Too many events | Network overhead | Low | Throttle to max 1/second |
| Percentage inaccuracy | Misleading progress | Low | Calculate based on file count |

---

## Definition of Ready

- [x] Acceptance criteria clear
- [x] Dependencies identified
- [x] Tech tasks estimated
- [x] Test scenarios defined
- [x] Demo script approved
- [x] No blockers

