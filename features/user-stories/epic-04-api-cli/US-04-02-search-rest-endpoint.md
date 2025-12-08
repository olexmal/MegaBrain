# US-04-02: Search REST Endpoint

## Story
**As a** developer  
**I want** to search code via REST API  
**So that** I can build custom integrations and tools

## Story Points: 3
## Priority: High
## Sprint Target: Sprint 3

---

## Acceptance Criteria

- [ ] **AC1:** `GET /api/v1/search` accepts query parameters
- [ ] **AC2:** Parameters: `q`, `language`, `repository`, `entity_type`, `limit`, `offset`
- [ ] **AC3:** Response includes: chunks, metadata, scores, total count
- [ ] **AC4:** Pagination support via offset/limit
- [ ] **AC5:** Response format: JSON
- [ ] **AC6:** Latency <500ms for 95th percentile

---

## Demo Script

### Setup
1. Indexed repository available
2. API ready

### Demo Steps
1. **Basic Search:**
   ```bash
   curl "http://localhost:8080/api/v1/search?q=authentication"
   ```
2. **With Filters:**
   ```bash
   curl "http://localhost:8080/api/v1/search?q=service&language=java&limit=5"
   ```
3. **Pagination:**
   ```bash
   curl "http://localhost:8080/api/v1/search?q=user&offset=10&limit=10"
   ```
4. **Show Response:** Display JSON structure

### Expected Outcome
- Search results returned as JSON
- Filters work correctly
- Pagination functional

---

## Technical Tasks

- [ ] **T1:** Create `SearchResource` JAX-RS class (backend)
- [ ] **T2:** Implement GET endpoint with query params (backend)
- [ ] **T3:** Create search response DTO (backend)
- [ ] **T4:** Integrate with MegaBrainOrchestrator (backend)
- [ ] **T5:** Add pagination logic (backend)
- [ ] **T6:** Write integration tests (test)

---

## Dependencies

- **Blocked by:** US-02-03 (hybrid search)
- **Enables:** US-05-03 (UI search), US-08-02 (MCP search tools)

---

## Definition of Ready

- [x] Acceptance criteria clear
- [x] Dependencies identified
- [x] Tech tasks estimated
- [x] Test scenarios defined
- [x] Demo script approved
- [x] No blockers

