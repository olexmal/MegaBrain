# Tasks for US-06-05: Inheritance Hierarchy Queries

## Story Reference
- **Epic:** EPIC-06 (Dependency Graph Analysis)
- **Story:** US-06-05
- **Story Points:** 3
- **Sprint Target:** Sprint 4

## Task List

### T1: Implement getInheritanceTree query
- **Description:** Implement Cypher query to retrieve full inheritance tree starting from any node. Query should return tree structure with parent-child relationships. Support querying from root (interface/class) or leaf (concrete class). Include interfaces, abstract classes, and concrete classes.
- **Estimated Hours:** 5 hours
- **Assignee:** TBD
- **Status:** Not Started
- **Dependencies:** US-06-02 (needs graph storage)
- **Acceptance Criteria:**
  - [ ] getInheritanceTree query implemented
  - [ ] Returns full tree structure
  - [ ] Includes all node types
  - [ ] Works from root or leaf
- **Technical Notes:** Use Cypher: `MATCH path = (root {id: $id})<-[:EXTENDS|IMPLEMENTS*]-(child) RETURN path`. Build tree structure from paths. Include both upward (from leaf) and downward (from root) traversal.

### T2: Create tree structure response DTO
- **Description:** Create tree structure DTO that represents inheritance hierarchy. Include node information (id, name, type) and parent-child relationships. Format as nested tree structure or flat structure with parent references.
- **Estimated Hours:** 3 hours
- **Assignee:** TBD
- **Status:** Not Started
- **Dependencies:** T1 (needs query)
- **Acceptance Criteria:**
  - [ ] Tree structure DTO created
  - [ ] Includes node information
  - [ ] Includes relationships
  - [ ] Serializable to JSON
- **Technical Notes:** Use nested structure: `{node: {...}, children: [...]}`. Include node: id, name, type, file_path. Include children array for nested structure.

### T3: Handle multiple inheritance paths
- **Description:** Implement logic to handle multiple inheritance paths (e.g., class implements multiple interfaces). Build tree structure that includes all paths. Deduplicate nodes that appear in multiple paths. Handle cycles gracefully.
- **Estimated Hours:** 4 hours
- **Assignee:** TBD
- **Status:** Not Started
- **Dependencies:** T1, T2 (needs query and DTO)
- **Acceptance Criteria:**
  - [ ] Multiple inheritance paths handled
  - [ ] Nodes deduplicated
  - [ ] Cycles handled gracefully
  - [ ] Tree structure correct
- **Technical Notes:** Collect all paths from Cypher query. Build tree structure from paths. Deduplicate nodes. Detect and handle cycles (log warning, stop traversal).

### T4: Write tests for complex hierarchies
- **Description:** Create comprehensive tests for inheritance hierarchy queries. Test with complex hierarchies (multiple interfaces, abstract classes, concrete classes). Test querying from root and leaf. Verify tree structure is correct.
- **Estimated Hours:** 3 hours
- **Assignee:** TBD
- **Status:** Not Started
- **Dependencies:** T1-T3 (needs complete implementation)
- **Acceptance Criteria:**
  - [ ] Tests for complex hierarchies
  - [ ] Tests for root and leaf queries
  - [ ] Tests verify tree structure
  - [ ] Test coverage >80%
- **Technical Notes:** Create test graph with complex hierarchy. Test querying from interface (root) and concrete class (leaf). Verify tree includes all expected nodes and relationships.

---

## Summary
- **Total Tasks:** 4
- **Total Estimated Hours:** 15 hours
- **Story Points:** 3 (1 SP ≈ 5 hours, aligns with estimate)

