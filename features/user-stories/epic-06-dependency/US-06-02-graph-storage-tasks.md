# Tasks for US-06-02: Neo4j Graph Storage

## Story Reference
- **Epic:** EPIC-06 (Dependency Graph Analysis)
- **Story:** US-06-02
- **Story Points:** 5
- **Sprint Target:** Sprint 3

## Task List

### T1: Add Neo4j dependency
- **Description:** Add Neo4j Java driver dependency to project build file. Choose between Neo4j Embedded (for single-instance) or Neo4j Server (for distributed). Configure dependency version and transitive dependencies.
- **Estimated Hours:** 1 hour
- **Assignee:** TBD
- **Status:** Not Started
- **Dependencies:** None
- **Acceptance Criteria:**
  - [ ] Neo4j dependency added
  - [ ] Dependency resolves correctly
  - [ ] No version conflicts
- **Technical Notes:** Use Neo4j Java Driver 5.x. For embedded: use neo4j-embedded. For server: use neo4j-java-driver. Configure in pom.xml or build.gradle.

### T2: Create Neo4j configuration
- **Description:** Create Neo4j configuration class for connection settings. Support both embedded and server modes. Configure connection URI, username, password, and connection pool settings. Load from application.properties.
- **Estimated Hours:** 3 hours
- **Assignee:** TBD
- **Status:** Not Started
- **Dependencies:** T1 (needs dependency)
- **Acceptance Criteria:**
  - [ ] Neo4j configuration created
  - [ ] Embedded and server modes supported
  - [ ] Configuration loaded from properties
  - [ ] Connection settings configurable
- **Technical Notes:** Use Quarkus configuration. Support `neo4j.uri`, `neo4j.username`, `neo4j.password`. Create Driver instance with configuration.

### T3: Create GraphQueryService class
- **Description:** Create `GraphQueryService` class that provides methods for graph operations. Include methods for creating nodes, creating relationships, querying relationships, and executing Cypher queries. Use Neo4j driver for operations.
- **Estimated Hours:** 5 hours
- **Assignee:** TBD
- **Status:** Not Started
- **Dependencies:** T1, T2 (needs dependency and configuration)
- **Acceptance Criteria:**
  - [ ] GraphQueryService class created
  - [ ] Methods for node creation
  - [ ] Methods for relationship creation
  - [ ] Methods for querying
  - [ ] Cypher query execution
- **Technical Notes:** Use Neo4j Driver API. Create Session for operations. Use parameterized Cypher queries. Handle transactions properly.

### T4: Implement node creation during indexing
- **Description:** Implement node creation logic that creates Neo4j nodes for code entities during indexing. Create nodes with labels (Class, Interface, Method, etc.) and properties (name, file_path, language, etc.). Handle node updates for existing entities.
- **Estimated Hours:** 5 hours
- **Assignee:** TBD
- **Status:** Not Started
- **Dependencies:** T3 (needs GraphQueryService), US-06-01 (needs entities)
- **Acceptance Criteria:**
  - [ ] Nodes created for entities
  - [ ] Node labels and properties set
  - [ ] Node updates handled
  - [ ] Performance acceptable
- **Technical Notes:** Use MERGE for node creation (create if not exists). Set labels: Class, Interface, Method, Function. Set properties: name, file_path, language, entity_id. Batch operations for performance.

### T5: Implement edge creation for relationships
- **Description:** Implement edge creation logic that creates Neo4j relationships (edges) for extracted relationships. Create relationships with types (IMPLEMENTS, EXTENDS, IMPORTS, CALLS, etc.) and properties (line_number, etc.). Handle relationship updates.
- **Estimated Hours:** 5 hours
- **Assignee:** TBD
- **Status:** Not Started
- **Dependencies:** T3, T4 (needs GraphQueryService and nodes), US-06-01 (needs relationships)
- **Acceptance Criteria:**
  - [ ] Relationships created as edges
  - [ ] Relationship types and properties set
  - [ ] Relationship updates handled
  - [ ] Performance acceptable
- **Technical Notes:** Use MERGE for relationship creation. Create relationships: (source)-[:IMPLEMENTS]->(target). Set properties: line_number. Batch operations for performance.

### T6: Create indexes on entity properties
- **Description:** Create Neo4j indexes on entity properties for fast lookups. Index entity names, file paths, and entity IDs. Create indexes on startup or via migration. Verify indexes are created.
- **Estimated Hours:** 3 hours
- **Assignee:** TBD
- **Status:** Not Started
- **Dependencies:** T3 (needs GraphQueryService)
- **Acceptance Criteria:**
  - [ ] Indexes created on entity names
  - [ ] Indexes created on file paths
  - [ ] Indexes created on entity IDs
  - [ ] Indexes verified
- **Technical Notes:** Use CREATE INDEX Cypher commands. Index: `CREATE INDEX entity_name_index FOR (n:Class) ON (n.name)`. Create indexes on startup or via migration script.

### T7: Write integration tests
- **Description:** Create integration tests for Neo4j graph storage. Test node creation, relationship creation, querying, and indexing. Use Testcontainers with Neo4j or embedded Neo4j for testing.
- **Estimated Hours:** 4 hours
- **Assignee:** TBD
- **Status:** Not Started
- **Dependencies:** T1-T6 (needs complete implementation)
- **Acceptance Criteria:**
  - [ ] Integration tests for graph storage
  - [ ] Tests cover node creation
  - [ ] Tests cover relationship creation
  - [ ] Tests cover querying
  - [ ] Test coverage >80%
- **Technical Notes:** Use Testcontainers with Neo4j container or embedded Neo4j. Test with known data. Verify nodes and relationships are created correctly.

---

## Summary
- **Total Tasks:** 7
- **Total Estimated Hours:** 26 hours
- **Story Points:** 5 (1 SP ≈ 5.2 hours, aligns with estimate)

