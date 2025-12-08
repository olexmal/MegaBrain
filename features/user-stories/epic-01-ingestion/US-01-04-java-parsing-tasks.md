# Tasks for US-01-04: Java Parsing with JavaParser

## Story Reference
- **Epic:** EPIC-01 (Code Ingestion & Indexing)
- **Story:** US-01-04
- **Story Points:** 5
- **Sprint Target:** Sprint 1

## Task List

### T1: Implement JavaParserService class
- **Description:** Create the `JavaParserService` class that implements the `CodeParser` interface. This service will use JavaParser library to parse Java source files into AST (Abstract Syntax Tree) and extract structured code entities. The service should handle file detection, parsing, and error recovery.
- **Estimated Hours:** 4 hours
- **Assignee:** TBD
- **Status:** Not Started
- **Dependencies:** US-01-01 (needs files to parse)
- **Acceptance Criteria:**
  - [ ] Class implements `CodeParser` interface
  - [ ] Detects `.java` files and routes to JavaParser
  - [ ] Handles parsing errors gracefully
  - [ ] Returns structured chunks with metadata
- **Technical Notes:** Use JavaParser 3.25+ library. Implement as Quarkus CDI service. Handle both compilation units and individual types.

### T2: Create AST visitor for class extraction
- **Description:** Implement a custom AST visitor that traverses the Java AST and extracts class declarations. Extract class name, package, modifiers (public, private, abstract, etc.), and position information (line numbers, byte ranges).
- **Estimated Hours:** 4 hours
- **Assignee:** TBD
- **Status:** Not Started
- **Dependencies:** T1 (needs JavaParserService structure)
- **Acceptance Criteria:**
  - [ ] Extracts class name and fully qualified name
  - [ ] Extracts package declaration
  - [ ] Extracts class modifiers (public, abstract, final, etc.)
  - [ ] Captures accurate line ranges for each class
- **Technical Notes:** Use JavaParser's `VoidVisitorAdapter` or `GenericVisitorAdapter`. Handle both top-level and nested classes. Store position information for later chunk creation.

### T3: Create AST visitor for method extraction
- **Description:** Implement a custom AST visitor that extracts method declarations from classes. Extract method name, parameters (with types), return type, modifiers, and position information. Handle constructors, static methods, and instance methods.
- **Estimated Hours:** 5 hours
- **Assignee:** TBD
- **Status:** Not Started
- **Dependencies:** T1, T2 (needs JavaParserService and class context)
- **Acceptance Criteria:**
  - [ ] Extracts method name and signature
  - [ ] Extracts parameter names and types
  - [ ] Extracts return type
  - [ ] Extracts method modifiers (public, static, etc.)
  - [ ] Captures accurate line ranges for each method
- **Technical Notes:** Visit `MethodDeclaration` nodes. Handle method overloading (same name, different parameters). Include constructor extraction. Preserve parameter order and types.

### T4: Create AST visitor for field extraction
- **Description:** Implement a custom AST visitor that extracts field (variable) declarations from classes. Extract field name, type, modifiers, and position information. Handle both instance fields and static fields.
- **Estimated Hours:** 3 hours
- **Assignee:** TBD
- **Status:** Not Started
- **Dependencies:** T1, T2 (needs JavaParserService and class context)
- **Acceptance Criteria:**
  - [ ] Extracts field name and type
  - [ ] Extracts field modifiers (public, private, static, final, etc.)
  - [ ] Captures accurate line ranges for each field
  - [ ] Handles field initialization expressions
- **Technical Notes:** Visit `FieldDeclaration` nodes. Handle multiple fields declared in one statement. Extract initialization values if present.

### T5: Handle inner classes and anonymous classes
- **Description:** Extend the AST visitors to properly handle inner classes (non-static nested classes), static nested classes, and anonymous classes. Each should be extracted as a separate entity with proper parent-child relationships.
- **Estimated Hours:** 4 hours
- **Assignee:** TBD
- **Status:** Not Started
- **Dependencies:** T1, T2 (needs class extraction working)
- **Acceptance Criteria:**
  - [ ] Inner classes extracted as separate entities
  - [ ] Static nested classes extracted as separate entities
  - [ ] Anonymous classes handled (if applicable)
  - [ ] Parent-child relationships preserved
- **Technical Notes:** Track nesting level during AST traversal. Use qualified names to show nesting (OuterClass.InnerClass). Consider extracting anonymous classes as separate entities or including them in parent method/class.

### T6: Implement TextChunk creation with metadata
- **Description:** Create `TextChunk` objects from extracted AST entities with all required metadata. Each chunk should include: language="java", entity_type (class/method/field), entity_name, source_file path, and accurate byte/line ranges. Include code content snippet.
- **Estimated Hours:** 4 hours
- **Assignee:** TBD
- **Status:** Not Started
- **Dependencies:** T2, T3, T4, T5 (needs all entity extraction working)
- **Acceptance Criteria:**
  - [ ] TextChunk includes all required metadata fields
  - [ ] Language set to "java"
  - [ ] Entity type correctly set (class/method/field)
  - [ ] Entity name is fully qualified where appropriate
  - [ ] Line ranges are accurate
  - [ ] Code content snippet included
- **Technical Notes:** Use JavaParser's `Range` class for position information. Extract code snippet from source using line ranges. Ensure metadata is consistent across all chunk types.

### T7: Add error handling for malformed Java files
- **Description:** Implement robust error handling for Java files with syntax errors, incomplete code, or unsupported features. Log parsing errors but continue processing other files. Attempt partial parsing when possible.
- **Estimated Hours:** 3 hours
- **Assignee:** TBD
- **Status:** Not Started
- **Dependencies:** T1-T6 (needs parsing implementation)
- **Acceptance Criteria:**
  - [ ] Syntax errors logged with file path and error details
  - [ ] Parsing continues for other files after error
  - [ ] Partial parsing attempted when possible
  - [ ] Error messages are clear and actionable
- **Technical Notes:** Use try-catch around parsing operations. Log errors with context (file path, line number if available). Consider using JavaParser's recovery mode if available.

### T8: Write unit tests with sample Java files
- **Description:** Create comprehensive unit tests using sample Java files covering various scenarios: simple classes, classes with methods, inner classes, interfaces, enums, abstract classes. Verify all entities are extracted correctly with proper metadata.
- **Estimated Hours:** 4 hours
- **Assignee:** TBD
- **Status:** Not Started
- **Dependencies:** T1-T7 (needs complete implementation)
- **Acceptance Criteria:**
  - [ ] Unit tests cover all entity types (class, method, field)
  - [ ] Tests include inner classes, interfaces, enums
  - [ ] Tests verify metadata correctness
  - [ ] Test coverage >80%
  - [ ] Tests include edge cases (empty classes, single-line methods, etc.)
- **Technical Notes:** Create test Java files in test resources. Use JUnit 5 and assertions to verify extracted entities. Test both simple and complex Java constructs.

### T9: Performance benchmark test
- **Description:** Create performance benchmark tests to verify parsing meets the >10,000 LOC per minute requirement. Test with various file sizes and complexities. Measure and report parsing throughput.
- **Estimated Hours:** 2 hours
- **Assignee:** TBD
- **Status:** Not Started
- **Dependencies:** T1-T8 (needs complete implementation and tests)
- **Acceptance Criteria:**
  - [ ] Benchmark test measures LOC per minute
  - [ ] Performance meets >10,000 LOC/minute target
  - [ ] Benchmark includes files of various sizes
  - [ ] Results documented and reproducible
- **Technical Notes:** Use JMH (Java Microbenchmark Harness) for accurate benchmarking. Test with small, medium, and large Java files. Measure both parsing time and memory usage.

---

## Summary
- **Total Tasks:** 9
- **Total Estimated Hours:** 33 hours
- **Story Points:** 5 (1 SP ≈ 6.6 hours, aligns with estimate)

