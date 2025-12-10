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
- **Status:** Done
- **Dependencies:** US-01-01 (needs files to parse)
- **Acceptance Criteria:**
  - [x] Base class implements `CodeParser` interface
  - [x] Provides grammar loading mechanism
  - [x] Handles tree parsing and traversal
  - [x] Defines abstract methods for language-specific queries
  - [x] Error handling for parsing failures
- **Technical Notes:** Use java-tree-sitter binding. Create abstract methods for query definitions that subclasses will implement. Handle native library loading and platform-specific binaries.

### T2: Create Python grammar integration and queries
- **Description:** Implement Python-specific parser by extending `TreeSitterParser`. Create Tree-sitter queries to extract functions, classes, and methods from Python AST. Handle Python-specific constructs like decorators, async functions, and type hints.
- **Estimated Hours:** 5 hours
- **Assignee:** TBD
- **Status:** Done
- **Dependencies:** T1 (needs base TreeSitterParser)
- **Acceptance Criteria:**
  - [x] Python functions extracted with name and parameters
  - [x] Python classes extracted with name and methods
  - [x] Decorators and type hints preserved in metadata
  - [x] Async functions handled correctly
- **Technical Notes:** Use tree-sitter-python grammar. Create queries for `function_definition`, `class_definition`, `method_definition`. Extract docstrings if present.

### T3: Create JavaScript/TS grammar integration and queries
- **Description:** Implement JavaScript and TypeScript parsers by extending `TreeSitterParser`. Create Tree-sitter queries to extract functions, classes, methods, and type definitions. Handle both ES5 and ES6+ syntax, including arrow functions and async/await.
- **Estimated Hours:** 6 hours
- **Assignee:** TBD
- **Status:** Done
- **Dependencies:** T1 (needs base TreeSitterParser)
- **Acceptance Criteria:**
  - [x] JavaScript functions extracted (both function declarations and arrow functions)
  - [x] ES6 classes extracted with methods
  - [x] TypeScript types and interfaces extracted
  - [x] Async/await syntax handled
- **Technical Notes:** Use tree-sitter-javascript and tree-sitter-typescript grammars. Create queries for `function_declaration`, `class_declaration`, `method_definition`, `interface_declaration`. Handle both .js and .ts files.

### T4: Create C/C++ grammar integration and queries
- **Description:** Implement C and C++ parsers by extending `TreeSitterParser`. Create Tree-sitter queries to extract functions, structs, classes, and methods. Handle C++-specific features like namespaces, templates, and operator overloading.
- **Estimated Hours:** 6 hours
- **Assignee:** TBD
- **Status:** Done
- **Dependencies:** T1 (needs base TreeSitterParser)
- **Acceptance Criteria:**
  - [x] C functions extracted with name and parameters
  - [x] C structs extracted
  - [x] C++ classes extracted with methods
  - [x] C++ namespaces and templates handled
- **Technical Notes:** Use tree-sitter-c and tree-sitter-cpp grammars. Create queries for `function_definition`, `struct_specifier`, `class_specifier`, `method_definition`. Handle both .c/.h and .cpp/.hpp files.

### T5: Implement file extension to grammar routing
- **Description:** Create a routing mechanism that maps file extensions to appropriate Tree-sitter grammars and parser instances. Support multiple extensions per language (e.g., .js, .jsx for JavaScript). Handle ambiguous extensions gracefully.
- **Estimated Hours:** 3 hours
- **Assignee:** TBD
- **Status:** Done
- **Dependencies:** T1-T4 (needs parser implementations)
- **Acceptance Criteria:**
  - [x] File extensions mapped to correct grammars
  - [x] Multiple extensions per language supported
  - [x] Unknown extensions handled gracefully
  - [x] Routing is fast (<10ms lookup)
- **Technical Notes:** Use a Map<String, ParserFactory> for extension routing. Support .py, .js, .ts, .jsx, .tsx, .c, .cpp, .h, .hpp extensions. Consider file content inspection for ambiguous cases.

### T6: Add dynamic grammar loading
- **Description:** Implement dynamic grammar loading mechanism that loads Tree-sitter grammars on-demand when needed. Handle native library loading, grammar initialization, and error recovery. Support both bundled and downloaded grammars.
- **Estimated Hours:** 4 hours
- **Assignee:** TBD
- **Status:** Done
- **Dependencies:** T1 (needs base parser structure), US-01-08 (grammar management)
- **Acceptance Criteria:**
  - [x] Grammars loaded on first use
  - [x] Loading errors handled gracefully
  - [x] Grammar initialization is fast (<500ms)
  - [x] Supports both bundled and external grammars
- **Technical Notes:** Integrate with GrammarManager from US-01-08. Load native libraries from classpath or filesystem. Handle platform-specific library loading (Linux, Mac, Windows).

### T7: Create TextChunk with consistent metadata
- **Description:** Implement TextChunk creation from Tree-sitter parse results with consistent metadata format across all languages. Extract entity name, type, source file, and accurate line ranges. Include code content snippets.
- **Estimated Hours:** 4 hours
- **Assignee:** TBD
- **Status:** Done
- **Dependencies:** T2, T3, T4 (needs language-specific extraction working)
- **Acceptance Criteria:**
  - [x] TextChunk includes all required metadata fields
  - [x] Language correctly set (python, javascript, typescript, c, cpp)
  - [x] Entity type consistently formatted (function, class, method)
  - [x] Line ranges are accurate
  - [x] Code content snippet included
- **Technical Notes:** Use Tree-sitter's node range information for line numbers. Extract code snippet from source using start/end byte positions. Ensure metadata format matches Java parser output.

### T8: Write unit tests per language
- **Description:** Create comprehensive unit tests for each language parser (Python, JavaScript, TypeScript, C, C++). Test various language constructs, edge cases, and error scenarios. Verify correct entity extraction and metadata.
- **Estimated Hours:** 6 hours
- **Assignee:** TBD
- **Status:** Done
- **Dependencies:** T1-T7 (needs complete implementation)
- **Acceptance Criteria:**
  - [x] Unit tests for each language parser
  - [x] Tests cover various language constructs
  - [x] Tests verify metadata correctness
  - [x] Test coverage >80% per language
  - [x] Tests include error scenarios
- **Technical Notes:** Create test source files in test resources for each language. Use JUnit 5 and assertions to verify extracted entities. Test both simple and complex code structures.

### T9: Performance benchmark test
- **Description:** Create performance benchmark tests to verify parsing meets the >10,000 LOC per minute requirement across all supported languages. Test with various file sizes and complexities. Measure and report parsing throughput per language.
- **Estimated Hours:** 3 hours
- **Assignee:** TBD
- **Status:** Done
- **Dependencies:** T1-T8 (needs complete implementation and tests)
- **Acceptance Criteria:**
  - [x] Benchmark test measures LOC per minute per language
  - [x] Performance meets >10,000 LOC/minute target
  - [x] Benchmark includes files of various sizes
  - [x] Results documented and reproducible
- **Technical Notes:** Use JMH for accurate benchmarking. Test with small, medium, and large files for each language. Measure both parsing time and memory usage. Compare performance across languages.

### T10: Create Java grammar integration and queries
- **Description:** Implement a Java-specific parser by extending `TreeSitterParser`. Create Tree-sitter queries to extract classes, interfaces, enums, methods (including constructors), and handle Java-specific constructs like annotations, generics, and nested types.
- **Estimated Hours:** 6 hours
- **Assignee:** TBD
- **Status:** Done
- **Dependencies:** T1 (base parser), T5 (extension routing)
- **Acceptance Criteria:**
  - [x] Java classes, interfaces, and enums extracted with names and modifiers
  - [x] Methods and constructors extracted with parameters and return types
  - [x] Annotations and generics preserved in metadata
  - [x] Nested/inner classes handled correctly
- **Technical Notes:** Use `tree-sitter-java` grammar. Create queries for `class_declaration`, `interface_declaration`, `enum_declaration`, `constructor_declaration`, and `method_declaration`. Capture package name and import metadata when available. Ensure byte/line ranges map cleanly for TextChunk creation.

---

## Summary
- **Total Tasks:** 10
- **Total Estimated Hours:** 49 hours
- **Story Points:** 8 (kept at 8 SP; additional task increases effort buffer)
- **Completed:** T1, T2, T3, T4, T5, T6, T7, T8, T9, T10

