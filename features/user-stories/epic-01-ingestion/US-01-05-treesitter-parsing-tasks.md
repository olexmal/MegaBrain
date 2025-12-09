# Tasks for US-01-05: Tree-sitter Multi-Language Parsing

## Story Reference
- **Epic:** EPIC-01 (Code Ingestion & Indexing)
- **Story:** US-01-05
- **Story Points:** 8
- **Sprint Target:** Sprint 2

## Task List

### T1: Implement TreeSitterParser base class
- **Description:** Create the base `TreeSitterParser` abstract class that implements the `CodeParser` interface. This class will provide common functionality for all Tree-sitter-based parsers including grammar loading, tree parsing, and error handling. Define the interface for language-specific query implementations.
- **Estimated Hours:** 6 hours
- **Assignee:** TBD
- **Status:** In Progress
- **Dependencies:** US-01-01 (needs files to parse)
- **Acceptance Criteria:**
  - [ ] Base class implements `CodeParser` interface
  - [ ] Provides grammar loading mechanism
  - [ ] Handles tree parsing and traversal
  - [ ] Defines abstract methods for language-specific queries
  - [ ] Error handling for parsing failures
- **Technical Notes:** Use java-tree-sitter binding. Create abstract methods for query definitions that subclasses will implement. Handle native library loading and platform-specific binaries.

### T2: Create Python grammar integration and queries
- **Description:** Implement Python-specific parser by extending `TreeSitterParser`. Create Tree-sitter queries to extract functions, classes, and methods from Python AST. Handle Python-specific constructs like decorators, async functions, and type hints.
- **Estimated Hours:** 5 hours
- **Assignee:** TBD
- **Status:** Not Started
- **Dependencies:** T1 (needs base TreeSitterParser)
- **Acceptance Criteria:**
  - [ ] Python functions extracted with name and parameters
  - [ ] Python classes extracted with name and methods
  - [ ] Decorators and type hints preserved in metadata
  - [ ] Async functions handled correctly
- **Technical Notes:** Use tree-sitter-python grammar. Create queries for `function_definition`, `class_definition`, `method_definition`. Extract docstrings if present.

### T3: Create JavaScript/TS grammar integration and queries
- **Description:** Implement JavaScript and TypeScript parsers by extending `TreeSitterParser`. Create Tree-sitter queries to extract functions, classes, methods, and type definitions. Handle both ES5 and ES6+ syntax, including arrow functions and async/await.
- **Estimated Hours:** 6 hours
- **Assignee:** TBD
- **Status:** Not Started
- **Dependencies:** T1 (needs base TreeSitterParser)
- **Acceptance Criteria:**
  - [ ] JavaScript functions extracted (both function declarations and arrow functions)
  - [ ] ES6 classes extracted with methods
  - [ ] TypeScript types and interfaces extracted
  - [ ] Async/await syntax handled
- **Technical Notes:** Use tree-sitter-javascript and tree-sitter-typescript grammars. Create queries for `function_declaration`, `class_declaration`, `method_definition`, `interface_declaration`. Handle both .js and .ts files.

### T4: Create C/C++ grammar integration and queries
- **Description:** Implement C and C++ parsers by extending `TreeSitterParser`. Create Tree-sitter queries to extract functions, structs, classes, and methods. Handle C++-specific features like namespaces, templates, and operator overloading.
- **Estimated Hours:** 6 hours
- **Assignee:** TBD
- **Status:** Not Started
- **Dependencies:** T1 (needs base TreeSitterParser)
- **Acceptance Criteria:**
  - [ ] C functions extracted with name and parameters
  - [ ] C structs extracted
  - [ ] C++ classes extracted with methods
  - [ ] C++ namespaces and templates handled
- **Technical Notes:** Use tree-sitter-c and tree-sitter-cpp grammars. Create queries for `function_definition`, `struct_specifier`, `class_specifier`, `method_definition`. Handle both .c/.h and .cpp/.hpp files.

### T5: Implement file extension to grammar routing
- **Description:** Create a routing mechanism that maps file extensions to appropriate Tree-sitter grammars and parser instances. Support multiple extensions per language (e.g., .js, .jsx for JavaScript). Handle ambiguous extensions gracefully.
- **Estimated Hours:** 3 hours
- **Assignee:** TBD
- **Status:** Not Started
- **Dependencies:** T1-T4 (needs parser implementations)
- **Acceptance Criteria:**
  - [ ] File extensions mapped to correct grammars
  - [ ] Multiple extensions per language supported
  - [ ] Unknown extensions handled gracefully
  - [ ] Routing is fast (<10ms lookup)
- **Technical Notes:** Use a Map<String, ParserFactory> for extension routing. Support .py, .js, .ts, .jsx, .tsx, .c, .cpp, .h, .hpp extensions. Consider file content inspection for ambiguous cases.

### T6: Add dynamic grammar loading
- **Description:** Implement dynamic grammar loading mechanism that loads Tree-sitter grammars on-demand when needed. Handle native library loading, grammar initialization, and error recovery. Support both bundled and downloaded grammars.
- **Estimated Hours:** 4 hours
- **Assignee:** TBD
- **Status:** Not Started
- **Dependencies:** T1 (needs base parser structure), US-01-08 (grammar management)
- **Acceptance Criteria:**
  - [ ] Grammars loaded on first use
  - [ ] Loading errors handled gracefully
  - [ ] Grammar initialization is fast (<500ms)
  - [ ] Supports both bundled and external grammars
- **Technical Notes:** Integrate with GrammarManager from US-01-08. Load native libraries from classpath or filesystem. Handle platform-specific library loading (Linux, Mac, Windows).

### T7: Create TextChunk with consistent metadata
- **Description:** Implement TextChunk creation from Tree-sitter parse results with consistent metadata format across all languages. Extract entity name, type, source file, and accurate line ranges. Include code content snippets.
- **Estimated Hours:** 4 hours
- **Assignee:** TBD
- **Status:** Not Started
- **Dependencies:** T2, T3, T4 (needs language-specific extraction working)
- **Acceptance Criteria:**
  - [ ] TextChunk includes all required metadata fields
  - [ ] Language correctly set (python, javascript, typescript, c, cpp)
  - [ ] Entity type consistently formatted (function, class, method)
  - [ ] Line ranges are accurate
  - [ ] Code content snippet included
- **Technical Notes:** Use Tree-sitter's node range information for line numbers. Extract code snippet from source using start/end byte positions. Ensure metadata format matches Java parser output.

### T8: Write unit tests per language
- **Description:** Create comprehensive unit tests for each language parser (Python, JavaScript, TypeScript, C, C++). Test various language constructs, edge cases, and error scenarios. Verify correct entity extraction and metadata.
- **Estimated Hours:** 6 hours
- **Assignee:** TBD
- **Status:** Not Started
- **Dependencies:** T1-T7 (needs complete implementation)
- **Acceptance Criteria:**
  - [ ] Unit tests for each language parser
  - [ ] Tests cover various language constructs
  - [ ] Tests verify metadata correctness
  - [ ] Test coverage >80% per language
  - [ ] Tests include error scenarios
- **Technical Notes:** Create test source files in test resources for each language. Use JUnit 5 and assertions to verify extracted entities. Test both simple and complex code structures.

### T9: Performance benchmark test
- **Description:** Create performance benchmark tests to verify parsing meets the >10,000 LOC per minute requirement across all supported languages. Test with various file sizes and complexities. Measure and report parsing throughput per language.
- **Estimated Hours:** 3 hours
- **Assignee:** TBD
- **Status:** Not Started
- **Dependencies:** T1-T8 (needs complete implementation and tests)
- **Acceptance Criteria:**
  - [ ] Benchmark test measures LOC per minute per language
  - [ ] Performance meets >10,000 LOC/minute target
  - [ ] Benchmark includes files of various sizes
  - [ ] Results documented and reproducible
- **Technical Notes:** Use JMH for accurate benchmarking. Test with small, medium, and large files for each language. Measure both parsing time and memory usage. Compare performance across languages.

---

## Summary
- **Total Tasks:** 9
- **Total Estimated Hours:** 43 hours
- **Story Points:** 8 (1 SP ≈ 5.4 hours, aligns with estimate)

