# US-06-03: Incoming Dependency Queries

## Story
**As a** developer  
**I want** to find what depends on a given entity  
**So that** I understand downstream impact

## Story Points: 3
## Priority: High
## Sprint Target: Sprint 4

---

## Acceptance Criteria

- [ ] **AC1:** Query "what implements interface X?" 
- [ ] **AC2:** Query "what extends class X?"
- [ ] **AC3:** Query "what calls method X?"
- [ ] **AC4:** Support depth parameter (1 for direct, N for transitive)
- [ ] **AC5:** Results include relationship paths
- [ ] **AC6:** Query latency <200ms

---

## Demo Script

### Demo Steps
1. **Find Implementations:**
   ```bash
   curl "http://localhost:8080/api/v1/graph/implements?interface=IRepository&depth=3"
   ```
2. **Show Results:** List of implementing classes
3. **Transitive:** Show classes that extend abstract implementers
4. **Find Callers:** What calls this method

### Expected Outcome
- Correct results for transitive queries
- Fast response time

---

## Technical Tasks

- [ ] **T1:** Implement `findImplementations` query (backend)
- [ ] **T2:** Implement `findSubclasses` query (backend)
- [ ] **T3:** Implement `findCallers` query (backend)
- [ ] **T4:** Add depth parameter handling (backend)
- [ ] **T5:** Write tests for transitive queries (test)

---

## Dependencies

- **Blocked by:** US-06-02 (graph storage)
- **Enables:** US-02-06 (transitive search), US-08-03 (MCP tools)

---

## Definition of Ready

- [x] All criteria met

