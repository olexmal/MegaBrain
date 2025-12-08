# US-06-04: Outgoing Dependency Queries

## Story
**As a** developer  
**I want** to find what a given entity depends on  
**So that** I understand upstream dependencies

## Story Points: 3
## Priority: High
## Sprint Target: Sprint 4

---

## Acceptance Criteria

- [ ] **AC1:** Query "what interfaces does X implement?"
- [ ] **AC2:** Query "what does X extend?"
- [ ] **AC3:** Query "what does method X call?"
- [ ] **AC4:** Support depth parameter
- [ ] **AC5:** Query latency <200ms

---

## Demo Script

### Demo Steps
1. **Find Dependencies:**
   ```bash
   curl "http://localhost:8080/api/v1/graph/dependencies?class=UserService&depth=2"
   ```
2. **Show Results:** Interfaces, parent classes
3. **Method Dependencies:** What this method calls

### Expected Outcome
- Accurate dependency list
- Transitive works correctly

---

## Technical Tasks

- [ ] **T1:** Implement `findDependencies` query (backend)
- [ ] **T2:** Implement `findParents` query (backend)
- [ ] **T3:** Implement `findCallees` query (backend)
- [ ] **T4:** Write tests for queries (test)

---

## Dependencies

- **Blocked by:** US-06-02 (graph storage)
- **Enables:** US-08-03 (MCP tools)

---

## Definition of Ready

- [x] All criteria met

