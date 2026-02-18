# US-02-06: Transitive Search Integration

## Story
**As a** developer  
**I want** to find all implementations of an interface including transitive subclasses  
**So that** I don't miss implementations through abstract classes

## Story Points: 5
## Priority: High
## Sprint Target: Sprint 5

---

## Acceptance Criteria

- [x] **AC1:** Search API supports `transitive=true` parameter
- [x] **AC2:** "Find implementations of X" includes classes extending abstract implementers
- [x] **AC3:** "Find usages of X" includes polymorphic call sites
- [x] **AC4:** Graph traversal integrated into search pipeline
- [x] **AC5:** Transitive results marked in response
- [x] **AC6:** Depth limit configurable (default: 5)

---

## Demo Script

### Setup
1. Index a codebase with interface → abstract class → concrete class hierarchy
2. Ensure graph database populated with relationships

### Demo Steps
1. **Non-Transitive Search:** Find direct implementations
   ```bash
   curl "http://localhost:8080/api/v1/search?q=implements:IRepository"
   ```
2. **Transitive Search:** Find all implementations
   ```bash
   curl "http://localhost:8080/api/v1/search?q=implements:IRepository&transitive=true"
   ```
3. **Find usages (AC3):** Type + all implementations/subclasses (polymorphic call sites)
   ```bash
   curl "http://localhost:8080/api/v1/search?q=usages:IRepository&transitive=true"
   ```
4. **Compare Results:** Show additional classes found
5. **Show Hierarchy:** Display inheritance chain
6. **Depth Limit:** Demonstrate depth parameter

### Expected Outcome
- Transitive search finds more results
- Inheritance chain visible
- Abstract implementers included

---

## Technical Tasks

- [x] **T1:** Add `transitive` parameter to search API (backend)
- [x] **T2:** Integrate GraphQueryService into search (backend)
- [x] **T3:** Implement transitive closure for implements (backend)
- [x] **T4:** Implement transitive closure for extends (backend)
- [x] **T5:** Add depth limit configuration (backend)
- [x] **T6:** Mark transitive results in response (backend)
- [x] **T7:** Write tests for transitive queries (test)

---

## Test Scenarios

| Scenario | Given | When | Then |
|:---------|:------|:-----|:-----|
| Direct implements | Class implements Interface | Non-transitive | Found |
| Transitive via abstract | Class extends Abstract implements Interface | Transitive | Found |
| Find usages (AC3) | Interface + implementations/subclasses | q=usages:X&transitive=true | Type + all subtypes (polymorphic) |
| Depth 1 | Chain length 1 | depth=1 | Direct only |
| Depth 3 | Chain length 3 | depth=3 | All found |
| Circular safe | Circular inheritance | Transitive | No infinite loop |

---

## Dependencies

- **Blocked by:** US-06-02 (graph storage), US-02-03 (hybrid search)
- **Enables:** US-08-03 (MCP dependency tools)

---

## Risks

| Risk | Impact | Likelihood | Mitigation |
|:-----|:-------|:-----------|:-----------|
| Graph query slow | Latency spike | Medium | Depth limits; caching |
| Incomplete graph | Missing results | Medium | Ensure extraction complete |

---

## Definition of Ready

- [x] Acceptance criteria clear
- [x] Dependencies identified
- [x] Tech tasks estimated
- [x] Test scenarios defined
- [x] Demo script approved
- [x] No blockers

