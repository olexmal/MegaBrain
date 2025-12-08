# US-04-01: Ingestion REST Endpoint

## Story
**As a** DevOps engineer  
**I want** to trigger code ingestion via REST API  
**So that** I can automate indexing in CI/CD pipelines

## Story Points: 3
## Priority: High
## Sprint Target: Sprint 2

---

## Acceptance Criteria

- [ ] **AC1:** `POST /api/v1/ingest/{source}` initiates ingestion
- [ ] **AC2:** Request body accepts: repository URL, branch, credentials reference
- [ ] **AC3:** Response is SSE stream with progress events
- [ ] **AC4:** Source parameter supports: `github`, `gitlab`, `bitbucket`, `local`
- [ ] **AC5:** Returns job ID for tracking
- [ ] **AC6:** Concurrent ingestion requests handled

---

## Demo Script

### Setup
1. MegaBrain backend running
2. MegaBrain repository URL ready: `github.com/olexmal/MegaBrain`

### Demo Steps
1. **Trigger Ingestion:**
   ```bash
   curl -X POST "http://localhost:8080/api/v1/ingest/github" \
     -H "Content-Type: application/json" \
     -d '{"repository": "olexmal/MegaBrain", "branch": "main"}'
   ```
2. **Show SSE Stream:** Progress events flowing
3. **Show Job ID:** Job tracking identifier
4. **Concurrent Request:** Start second ingestion

### Expected Outcome
- Ingestion starts via API
- Progress streamed in real-time
- Multiple jobs supported

---

## Technical Tasks

- [ ] **T1:** Create `IngestionResource` JAX-RS class (backend)
- [ ] **T2:** Implement POST endpoint with path parameter (backend)
- [ ] **T3:** Create ingestion request DTO (backend)
- [ ] **T4:** Integrate with RepositoryIngestionService (backend)
- [ ] **T5:** Return SSE stream from Mutiny Multi (backend)
- [ ] **T6:** Write integration tests (test)

---

## Dependencies

- **Blocked by:** US-01-01 (ingestion service)
- **Enables:** US-05-02 (UI triggers via API)

---

## Definition of Ready

- [x] Acceptance criteria clear
- [x] Dependencies identified
- [x] Tech tasks estimated
- [x] Test scenarios defined
- [x] Demo script approved
- [x] No blockers

