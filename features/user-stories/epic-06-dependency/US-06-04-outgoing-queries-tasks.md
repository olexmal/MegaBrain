# Tasks for US-06-04: Outgoing Dependency Queries

## Story Reference
- **Epic:** EPIC-06 (Dependency Graph Analysis)
- **Story:** US-06-04
- **Story Points:** 3
- **Sprint Target:** Sprint 4

## Task List

### T1: Implement findDependencies query
- **Description:** Implement Cypher query to find all dependencies of a given entity (what it depends on). Find interfaces it implements, classes it extends, and modules it imports. Support depth parameter for transitive dependencies.
- **Estimated Hours:** 4 hours
- **Assignee:** TBD
- **Status:** Not Started
- **Dependencies:** US-06-02 (needs graph storage)
- **Acceptance Criteria:**
  - [ ] findDependencies query implemented
  - [ ] Interfaces found
  - [ ] Parent classes found
  - [ ] Imports found
  - [ ] Depth parameter supported
- **Technical Notes:** Use Cypher: `MATCH (e {id: $id})-[:IMPLEMENTS|EXTENDS|IMPORTS*1..$depth]->(dep) RETURN dep`. Combine multiple relationship types. Return dependency details.

### T2: Implement findParents query
- **Description:** Implement Cypher query to find parent classes and interfaces of a given class. Find direct parents (extends, implements) and transitive parents. Support depth parameter.
- **Estimated Hours:** 3 hours
- **Assignee:** TBD
- **Status:** Not Started
- **Dependencies:** US-06-02 (needs graph storage)
- **Acceptance Criteria:**
  - [ ] findParents query implemented
  - [ ] Direct parents found
  - [ ] Transitive parents found
  - [ ] Depth parameter supported
- **Technical Notes:** Use Cypher: `MATCH (c:Class {id: $id})-[:EXTENDS|IMPLEMENTS*1..$depth]->(parent) RETURN parent`. Handle both extends and implements. Return parent details.

### T3: Implement findCallees query
- **Description:** Implement Cypher query to find all methods/functions called by a given method/function. Support depth parameter for transitive call chains. Return callees with relationship paths.
- **Estimated Hours:** 4 hours
- **Assignee:** TBD
- **Status:** Not Started
- **Dependencies:** US-06-02 (needs graph storage)
- **Acceptance Criteria:**
  - [ ] findCallees query implemented
  - [ ] Direct callees found
  - [ ] Transitive callees found (if depth > 1)
  - [ ] Depth parameter supported
- **Technical Notes:** Use Cypher: `MATCH (m:Method {id: $id})-[:CALLS*1..$depth]->(callee) RETURN callee, relationships(path)`. Handle depth parameter. Return entity details.

### T4: Write tests for queries
- **Description:** Create comprehensive tests for outgoing dependency queries. Test findDependencies, findParents, and findCallees with various depth values. Test with known relationship graphs. Verify results are correct.
- **Estimated Hours:** 3 hours
- **Assignee:** TBD
- **Status:** Not Started
- **Dependencies:** T1-T3 (needs complete implementation)
- **Acceptance Criteria:**
  - [ ] Tests for all query types
  - [ ] Tests for various depth values
  - [ ] Tests verify results
  - [ ] Test coverage >80%
- **Technical Notes:** Create test graph with known relationships. Test depth=1 (direct) and depth>1 (transitive). Verify results include all expected dependencies.

---

## Summary
- **Total Tasks:** 4
- **Total Estimated Hours:** 14 hours
- **Story Points:** 3 (1 SP ≈ 4.7 hours, aligns with estimate)

