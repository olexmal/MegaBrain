# US-06-02: Neo4j Graph Storage

## Story
**As a** system  
**I want** relationships stored in a graph database  
**So that** traversal queries are efficient

## Story Points: 5
## Priority: Critical
## Sprint Target: Sprint 3

---

## Acceptance Criteria

- [ ] **AC1:** Neo4j embedded or server connection
- [ ] **AC2:** Nodes represent code entities
- [ ] **AC3:** Edges represent relationships with types
- [ ] **AC4:** Graph populated during ingestion
- [ ] **AC5:** Basic Cypher queries functional
- [ ] **AC6:** Index on entity names for fast lookup

---

## Demo Script

### Demo Steps
1. **Connect Neo4j:** Show connection
2. **View Graph:** Query in Neo4j browser
   ```cypher
   MATCH (n:Class)-[r:IMPLEMENTS]->(i:Interface)
   RETURN n, r, i LIMIT 10
   ```
3. **Show Indexes:** Display created indexes

### Expected Outcome
- Graph populated
- Queries work
- Performance acceptable

---

## Technical Tasks

- [ ] **T1:** Add Neo4j dependency (backend)
- [ ] **T2:** Create Neo4j configuration (backend)
- [ ] **T3:** Create `GraphQueryService` class (backend)
- [ ] **T4:** Implement node creation during indexing (backend)
- [ ] **T5:** Implement edge creation for relationships (backend)
- [ ] **T6:** Create indexes on entity properties (backend)
- [ ] **T7:** Write integration tests (test)

---

## Dependencies

- **Blocked by:** US-06-01 (relationships to store)
- **Enables:** US-06-03-05 (query capabilities)

---

## Definition of Ready

- [x] All criteria met

