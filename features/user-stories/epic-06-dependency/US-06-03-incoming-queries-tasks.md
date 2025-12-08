# Tasks for US-06-03: Incoming Dependency Queries

## Story Reference
- **Epic:** EPIC-06 (Dependency Graph Analysis)
- **Story:** US-06-03
- **Story Points:** 3
- **Sprint Target:** Sprint 4

## Task List

### T1: Implement findImplementations query
- **Description:** Implement Cypher query to find all classes that implement a given interface, including transitive implementations (through abstract classes). Support depth parameter for transitive traversal. Return implementing classes with relationship paths.
- **Estimated Hours:** 4 hours
- **Assignee:** TBD
- **Status:** Not Started
- **Dependencies:** US-06-02 (needs graph storage)
- **Acceptance Criteria:**
  - [ ] findImplementations query implemented
  - [ ] Direct implementations found
  - [ ] Transitive implementations found
  - [ ] Depth parameter supported
- **Technical Notes:** Use Cypher: `MATCH (i:Interface {name: $name})<-[:IMPLEMENTS|EXTENDS*1..$depth]-(c) RETURN c, relationships(path)`. Handle depth parameter. Return entity details.

### T2: Implement findSubclasses query
- **Description:** Implement Cypher query to find all subclasses of a given class, including transitive subclasses. Support depth parameter for transitive traversal. Return subclasses with relationship paths.
- **Estimated Hours:** 3 hours
- **Assignee:** TBD
- **Status:** Not Started
- **Dependencies:** US-06-02 (needs graph storage)
- **Acceptance Criteria:**
  - [ ] findSubclasses query implemented
  - [ ] Direct subclasses found
  - [ ] Transitive subclasses found
  - [ ] Depth parameter supported
- **Technical Notes:** Use Cypher: `MATCH (c:Class {name: $name})<-[:EXTENDS*1..$depth]-(sub) RETURN sub, relationships(path)`. Handle depth parameter. Return entity details.

### T3: Implement findCallers query
- **Description:** Implement Cypher query to find all methods/functions that call a given method/function. Support depth parameter for transitive call chains. Return callers with relationship paths.
- **Estimated Hours:** 4 hours
- **Assignee:** TBD
- **Status:** Not Started
- **Dependencies:** US-06-02 (needs graph storage)
- **Acceptance Criteria:**
  - [ ] findCallers query implemented
  - [ ] Direct callers found
  - [ ] Transitive callers found (if depth > 1)
  - [ ] Depth parameter supported
- **Technical Notes:** Use Cypher: `MATCH (m:Method {name: $name})<-[:CALLS*1..$depth]-(caller) RETURN caller, relationships(path)`. Handle depth parameter. Return entity details.

### T4: Add depth parameter handling
- **Description:** Implement depth parameter handling for all incoming dependency queries. Validate depth parameter (1-N, default: 1). Apply depth to Cypher queries. Handle depth limits to prevent performance issues.
- **Estimated Hours:** 2 hours
- **Assignee:** TBD
- **Status:** Not Started
- **Dependencies:** T1, T2, T3 (needs query implementations)
- **Acceptance Criteria:**
  - [ ] Depth parameter validated
  - [ ] Default depth: 1
  - [ ] Depth limits enforced
  - [ ] Depth applied to queries
- **Technical Notes:** Validate depth is 1-10 (configurable max). Set default depth to 1. Apply depth to Cypher relationship patterns.

### T5: Write tests for transitive queries
- **Description:** Create comprehensive tests for transitive dependency queries. Test findImplementations, findSubclasses, and findCallers with various depth values. Test with known relationship graphs. Verify transitive results are correct.
- **Estimated Hours:** 3 hours
- **Assignee:** TBD
- **Status:** Not Started
- **Dependencies:** T1-T4 (needs complete implementation)
- **Acceptance Criteria:**
  - [ ] Tests for all query types
  - [ ] Tests for various depth values
  - [ ] Tests verify transitive results
  - [ ] Test coverage >80%
- **Technical Notes:** Create test graph with known relationships. Test depth=1 (direct) and depth>1 (transitive). Verify results include all expected entities.

---

## Summary
- **Total Tasks:** 5
- **Total Estimated Hours:** 16 hours
- **Story Points:** 3 (1 SP ≈ 5.3 hours, aligns with estimate)

