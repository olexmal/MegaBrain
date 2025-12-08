# US-07-03: Documentation Indexing

## Story
**As a** system  
**I want** documentation indexed separately with boost  
**So that** doc searches find relevant results

## Story Points: 3
## Priority: High
## Sprint Target: Sprint 4

---

## Acceptance Criteria

- [ ] **AC1:** Documentation stored in separate `doc_summary` field
- [ ] **AC2:** Boost factor applied (default 2.0x)
- [ ] **AC3:** "docs only" search filter available
- [ ] **AC4:** Documentation matches ranked appropriately
- [ ] **AC5:** Full-text search includes doc content

---

## Demo Script

### Demo Steps
1. **Search Code:** Regular search shows code
2. **Search Docs Only:**
   ```bash
   curl "http://localhost:8080/api/v1/search?q=validates&docs_only=true"
   ```
3. **Show Boost:** Doc matches rank higher
4. **Combined Search:** Show blended results

### Expected Outcome
- Doc-specific search works
- Boost improves relevance

---

## Technical Tasks

- [ ] **T1:** Add doc_summary field to index (backend)
- [ ] **T2:** Configure boost for doc field (backend)
- [ ] **T3:** Add docs_only filter parameter (backend)
- [ ] **T4:** Update search to include docs (backend)
- [ ] **T5:** Write tests for doc search (test)

---

## Dependencies

- **Blocked by:** US-07-01, US-07-02 (doc extraction)
- **Enables:** Better search relevance

---

## Definition of Ready

- [x] All criteria met

