# EPIC-06: Dependency Graph Analysis

## Epic Overview

| Attribute | Value |
|:----------|:------|
| **Epic ID** | EPIC-06 |
| **Priority** | High |
| **Estimated Scope** | L |
| **Dependencies** | EPIC-01 (Code Ingestion & Indexing) |
| **Spec Reference** | Section 4.5 (FR-DEP) |
| **Status** | Planned |

## Business Value

This epic enables code intelligence features that go beyond text search:

- **Find all implementations:** Including transitive inheritance chains
- **Call graph analysis:** What calls this function? What does it call?
- **Impact analysis:** Understand blast radius before making changes
- **Dead code detection:** Identify unused code across repositories

These capabilities are essential for accurate answers to structural questions from LLMs.

---

## User Stories

### US-06-01: Entity Relationship Extraction

**As a** system, **I want** to extract relationships between code entities during parsing, **so that** a dependency graph can be built.

**Acceptance Criteria:**
- [ ] Extract `imports` relationships (module/package dependencies)
- [ ] Extract `extends` relationships (class inheritance)
- [ ] Extract `implements` relationships (interface implementation)
- [ ] Extract `calls` relationships (function/method invocations)
- [ ] Extract `instantiates` relationships (object creation)
- [ ] Extract `references` relationships (variable/constant usage)
- [ ] Relationships include source entity, target entity, relationship type
- [ ] Extraction integrated into parsing pipeline

**Spec Reference:** FR-DEP-01

---

### US-06-02: Graph Storage

**As a** system, **I want** to persist relationships in a graph database, **so that** traversal queries are efficient.

**Acceptance Criteria:**
- [ ] Neo4j Embedded or JanusGraph integration
- [ ] Entity nodes with properties: id, name, type, file, repository
- [ ] Relationship edges with type labels
- [ ] Indexes on commonly queried properties
- [ ] Transaction support for bulk updates
- [ ] Graph data survives server restart

**Spec Reference:** FR-DEP-02 (Storage)

---

### US-06-03: Incoming Dependency Queries

**As a** developer, **I want** to find what calls a function or uses a class, **so that** I understand how code is used.

**Acceptance Criteria:**
- [ ] Query: "What calls function X?"
- [ ] Query: "What classes use class Y?"
- [ ] Query: "What files import module Z?"
- [ ] Results include: caller entity, file path, line number
- [ ] Configurable depth for transitive callers
- [ ] Query latency <200ms for depth 3

**Spec Reference:** FR-DEP-02 (Query Capabilities)

---

### US-06-04: Outgoing Dependency Queries

**As a** developer, **I want** to find what a class or function depends on, **so that** I understand its requirements.

**Acceptance Criteria:**
- [ ] Query: "What does class X depend on?"
- [ ] Query: "What does function Y call?"
- [ ] Query: "What does file Z import?"
- [ ] Results include: dependency entity, relationship type
- [ ] Configurable depth for transitive dependencies
- [ ] Query latency <200ms for depth 3

**Spec Reference:** FR-DEP-02 (Query Capabilities)

---

### US-06-05: Inheritance Hierarchy

**As a** developer, **I want** to see the full inheritance hierarchy of a class, **so that** I understand the type system.

**Acceptance Criteria:**
- [ ] Query: "Show inheritance tree for class X"
- [ ] Both upward (parents) and downward (children) traversal
- [ ] Includes both `extends` and `implements` relationships
- [ ] Transitive closure computed correctly
- [ ] Visualization-ready output format (tree structure)

**Spec Reference:** FR-DEP-02 (Show inheritance hierarchy)

---

### US-06-06: Find All Implementations (Transitive)

**As a** developer, **I want** to find all implementations of an interface including transitive subclasses, **so that** I don't miss implementations through abstract classes.

**Acceptance Criteria:**
- [ ] Query: "Find all implementations of interface I"
- [ ] Includes: classes directly implementing I
- [ ] Includes: classes extending abstract classes that implement I
- [ ] Includes: classes extending classes that extend implementers
- [ ] Correct handling of multi-level hierarchies
- [ ] Cross-repository search supported

**Spec Reference:** FR-DEP-04 (transitive parameter)

---

### US-06-07: Impact Analysis

**As a** developer, **I want** to calculate the blast radius of proposed changes, **so that** I can assess refactoring risk.

**Acceptance Criteria:**
- [ ] Input: set of entities to be modified
- [ ] Output: all transitively affected entities
- [ ] Affected = anything that depends on modified entities
- [ ] Report includes: entity name, relationship chain, file path
- [ ] Summary statistics: number of affected files, classes, functions
- [ ] Exportable report format (JSON, CSV)

**Spec Reference:** FR-DEP-03 (Change Impact Report)

---

### US-06-08: Dead Code Detection

**As a** tech lead, **I want** to identify unreferenced code, **so that** I can prioritize cleanup.

**Acceptance Criteria:**
- [ ] Identify public functions with no callers
- [ ] Identify classes with no instantiations or references
- [ ] Exclude entry points (main, handlers, tests)
- [ ] Configurable exclusion patterns
- [ ] Report grouped by repository/module
- [ ] Confidence score based on analysis completeness

**Spec Reference:** FR-DEP-03 (Dead Code Detection)

---

### US-06-09: Circular Dependency Detection

**As a** architect, **I want** to find circular dependencies, **so that** I can improve code structure.

**Acceptance Criteria:**
- [ ] Detect cycles in import/dependency graph
- [ ] Report cycle paths: A → B → C → A
- [ ] Detect at module/package level
- [ ] Detect at class level
- [ ] Severity classification (module cycles more severe)

**Spec Reference:** FR-DEP-02 (Find circular dependencies)

---

## Technical Notes

### Key Components
- **`DependencyExtractor`:** AST visitor that extracts relationships during parsing
- **`GraphQueryService`:** Abstraction over Neo4j/JanusGraph for queries

### Technology Stack
| Component | Technology |
|:----------|:-----------|
| Graph Database | Neo4j Embedded or JanusGraph |
| Query Language | Cypher (Neo4j) or Gremlin (JanusGraph) |
| Integration | Neo4j Java Driver / JanusGraph Gremlin |

### Graph Schema

**Nodes:**
```
(:Entity {
  id: string,
  name: string,
  type: "class" | "interface" | "function" | "method" | "module",
  file_path: string,
  repository: string,
  language: string
})
```

**Relationships:**
```
(:Entity)-[:IMPORTS]->(:Entity)
(:Entity)-[:EXTENDS]->(:Entity)
(:Entity)-[:IMPLEMENTS]->(:Entity)
(:Entity)-[:CALLS]->(:Entity)
(:Entity)-[:INSTANTIATES]->(:Entity)
(:Entity)-[:REFERENCES]->(:Entity)
```

### Example Cypher Queries

**Find all implementations of interface (transitive):**
```cypher
MATCH (i:Entity {name: 'IRepository', type: 'interface'})
      <-[:IMPLEMENTS|EXTENDS*]-(impl:Entity)
WHERE impl.type IN ['class', 'abstract_class']
RETURN impl
```

**Impact analysis:**
```cypher
MATCH (changed:Entity)
WHERE changed.name IN ['ServiceA', 'ServiceB']
MATCH (changed)<-[:CALLS|IMPORTS|REFERENCES*1..5]-(affected:Entity)
RETURN DISTINCT affected
```

---

## Risks & Mitigations

| Risk | Impact | Likelihood | Mitigation |
|:-----|:-------|:-----------|:-----------|
| Incomplete relationship extraction | Missing dependencies | Medium | Comprehensive AST visitors; validation tests |
| Graph query performance | Slow queries | Medium | Index tuning; query optimization; depth limits |
| Neo4j memory usage | OOM on large codebases | Medium | External Neo4j option; pagination; streaming |
| Cross-language dependencies | Incomplete graph | High | Focus on intra-language; document limitations |

---

## Non-Functional Requirements

| NFR | Target | Validation |
|:----|:-------|:-----------|
| Graph query latency (depth 3) | <200ms | Performance tests |
| Graph storage capacity | 50M relationships | Scale testing |
| Extraction overhead | <20% of parsing time | Benchmark |
| Incremental update time | <5s for typical PR | Timing tests |

---

## Definition of Done

- [ ] All user stories complete and accepted
- [ ] Relationship extraction for all supported languages
- [ ] Neo4j/JanusGraph integration operational
- [ ] All query types implemented and tested
- [ ] Impact analysis working end-to-end
- [ ] Dead code detection producing actionable reports
- [ ] Query latency NFRs met
- [ ] Unit tests (>80% coverage)
- [ ] Integration tests with real codebases
- [ ] Documentation updated

---

## Open Questions

1. Should we support cross-language dependencies (Java calling Python via API)?
2. How do we handle dynamic dispatch / runtime polymorphism?
3. Should impact analysis consider test code separately?
4. Do we need graph visualization in the UI?

---

**Epic Owner:** TBD  
**Created:** December 2025  
**Last Updated:** December 2025

