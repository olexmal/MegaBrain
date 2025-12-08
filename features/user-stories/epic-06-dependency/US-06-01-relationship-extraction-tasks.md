# Tasks for US-06-01: Entity Relationship Extraction

## Story Reference
- **Epic:** EPIC-06 (Dependency Graph Analysis)
- **Story:** US-06-01
- **Story Points:** 8
- **Sprint Target:** Sprint 3

## Task List

### T1: Create DependencyExtractor interface
- **Description:** Create `DependencyExtractor` interface that defines methods for extracting relationships from parsed code. Interface should support different relationship types (implements, extends, imports, calls, instantiates, references). Make it language-agnostic.
- **Estimated Hours:** 3 hours
- **Assignee:** TBD
- **Status:** Not Started
- **Dependencies:** US-01-04, US-01-05 (needs parsers)
- **Acceptance Criteria:**
  - [ ] DependencyExtractor interface created
  - [ ] Methods for all relationship types
  - [ ] Language-agnostic design
  - [ ] Interface well-documented
- **Technical Notes:** Define interface with methods: extractImplements(), extractExtends(), extractImports(), extractCalls(), extractInstantiates(), extractReferences(). Use generic types for flexibility.

### T2: Implement Java relationship extraction
- **Description:** Implement Java-specific relationship extraction using JavaParser AST. Extract implements (class → interface), extends (class → class), imports (file → package), calls (method → method), instantiates (code → class), and references (code → type). Handle inner classes and nested types.
- **Estimated Hours:** 8 hours
- **Assignee:** TBD
- **Status:** Not Started
- **Dependencies:** T1 (needs interface), US-01-04 (needs JavaParser)
- **Acceptance Criteria:**
  - [ ] All relationship types extracted for Java
  - [ ] Handles inner classes
  - [ ] Handles nested types
  - [ ] Relationships include entity IDs
- **Technical Notes:** Use JavaParser AST visitors. Visit ClassOrInterfaceDeclaration for implements/extends. Visit ImportDeclaration for imports. Visit MethodCallExpr for calls. Track entity IDs for relationships.

### T3: Implement Python relationship extraction
- **Description:** Implement Python-specific relationship extraction using Tree-sitter. Extract imports (file → module), calls (function → function), instantiates (code → class), and references (code → type). Handle Python-specific constructs (decorators, type hints).
- **Estimated Hours:** 6 hours
- **Assignee:** TBD
- **Status:** Not Started
- **Dependencies:** T1 (needs interface), US-01-05 (needs Tree-sitter)
- **Acceptance Criteria:**
  - [ ] All relationship types extracted for Python
  - [ ] Handles imports correctly
  - [ ] Handles function calls
  - [ ] Relationships include entity IDs
- **Technical Notes:** Use Tree-sitter queries for Python. Query import_statement for imports. Query call for function calls. Track entity IDs for relationships.

### T4: Implement JS/TS relationship extraction
- **Description:** Implement JavaScript/TypeScript relationship extraction using Tree-sitter. Extract imports (file → module), extends (class → class), implements (class → interface), calls (function → function), and references (code → type). Handle ES6 modules and TypeScript types.
- **Estimated Hours:** 6 hours
- **Assignee:** TBD
- **Status:** Not Started
- **Dependencies:** T1 (needs interface), US-01-05 (needs Tree-sitter)
- **Acceptance Criteria:**
  - [ ] All relationship types extracted for JS/TS
  - [ ] Handles ES6 modules
  - [ ] Handles TypeScript types
  - [ ] Relationships include entity IDs
- **Technical Notes:** Use Tree-sitter queries for JavaScript/TypeScript. Query import_statement for imports. Query class_declaration for extends/implements. Track entity IDs for relationships.

### T5: Create relationship model/DTO
- **Description:** Create Relationship model/DTO class that represents extracted relationships. Include fields: source_entity_id, target_entity_id, relationship_type, source_file, target_file, line_number. Make it serializable and storable.
- **Estimated Hours:** 3 hours
- **Assignee:** TBD
- **Status:** Not Started
- **Dependencies:** T1 (needs interface)
- **Acceptance Criteria:**
  - [ ] Relationship model created
  - [ ] Includes all required fields
  - [ ] Serializable to JSON
  - [ ] Storable in database
- **Technical Notes:** Use Java record or POJO. Include relationship type enum. Use entity IDs for source/target. Include file paths and line numbers for context.

### T6: Store relationships during parsing
- **Description:** Integrate relationship extraction into parsing pipeline. Store extracted relationships during code parsing. Collect relationships and persist them for later graph storage. Handle relationship deduplication.
- **Estimated Hours:** 4 hours
- **Assignee:** TBD
- **Status:** Not Started
- **Dependencies:** T2, T3, T4, T5 (needs extractors and model)
- **Acceptance Criteria:**
  - [ ] Relationships stored during parsing
  - [ ] Relationships persisted
  - [ ] Deduplication works
  - [ ] Performance acceptable
- **Technical Notes:** Collect relationships in memory during parsing. Batch persist to database. Deduplicate by (source_id, target_id, type). Handle large relationship sets efficiently.

### T7: Write tests for each relationship type
- **Description:** Create comprehensive tests for each relationship type and language. Test implements, extends, imports, calls, instantiates, and references for Java, Python, and JS/TS. Use test code files with known relationships.
- **Estimated Hours:** 6 hours
- **Assignee:** TBD
- **Status:** Not Started
- **Dependencies:** T1-T6 (needs complete implementation)
- **Acceptance Criteria:**
  - [ ] Tests for all relationship types
  - [ ] Tests for all languages
  - [ ] Tests use known test data
  - [ ] Test coverage >80%
- **Technical Notes:** Create test code files with known relationships. Use JUnit 5. Verify relationships are extracted correctly. Test edge cases (nested classes, multiple inheritance, etc.).

---

## Summary
- **Total Tasks:** 7
- **Total Estimated Hours:** 36 hours
- **Story Points:** 8 (1 SP ≈ 4.5 hours, aligns with estimate)

