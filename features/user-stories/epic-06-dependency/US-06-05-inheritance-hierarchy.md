# US-06-05: Inheritance Hierarchy Queries

## Story
**As a** developer  
**I want** to visualize inheritance hierarchies  
**So that** I understand class relationships

## Story Points: 3
## Priority: High
## Sprint Target: Sprint 4

---

## Acceptance Criteria

- [ ] **AC1:** Query returns full inheritance tree
- [ ] **AC2:** Includes interfaces, abstract classes, concrete classes
- [ ] **AC3:** Tree structure in response (parent-child relationships)
- [ ] **AC4:** Query by any node in tree
- [ ] **AC5:** Handles multiple inheritance (interfaces)

---

## Demo Script

### Demo Steps
1. **Get Hierarchy:**
   ```bash
   curl "http://localhost:8080/api/v1/graph/hierarchy?root=IRepository"
   ```
2. **Show Tree:** Display inheritance tree
3. **From Leaf:** Query from concrete class, show upward tree

### Expected Outcome
- Complete hierarchy shown
- Multiple interfaces handled

---

## Technical Tasks

- [ ] **T1:** Implement `getInheritanceTree` query (backend)
- [ ] **T2:** Create tree structure response DTO (backend)
- [ ] **T3:** Handle multiple inheritance paths (backend)
- [ ] **T4:** Write tests for complex hierarchies (test)

---

## Dependencies

- **Blocked by:** US-06-02 (graph storage)
- **Enables:** US-08-03 (MCP hierarchy tool)

---

## Definition of Ready

- [x] All criteria met

