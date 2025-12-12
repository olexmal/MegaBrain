# US-01-05: Tree-sitter Multi-Language Parsing

## Story
**As a** developer working with Python, JS, C++, or other languages  
**I want** my code parsed into logical chunks using Tree-sitter  
**So that** all our polyglot code is searchable with proper structure

## Story Points: 8
## Priority: Critical
## Sprint Target: Sprint 2

---

## Acceptance Criteria

- [x] **AC1:** Python files parsed (functions, classes, methods)
- [x] **AC2:** JavaScript/TypeScript files parsed (functions, classes, methods)
- [x] **AC3:** Java files parsed (classes, interfaces, methods)
- [x] **AC4:** C/C++ files parsed (functions, structs, classes)
- [x] **AC5:** File extension routing to correct Tree-sitter grammar
- [x] **AC6:** Each chunk includes: language, entity_type, entity_name, source_file, line range
- [x] **AC7:** Tree-sitter grammars loaded dynamically
- [x] **AC8:** Parsing errors logged but don't stop ingestion
- [x] **AC9:** Performance: >10,000 LOC per minute

---

## Demo Script

### Setup
1. Ingest the MegaBrain repository (contains multiple languages)
2. Ensure all languages are indexed

### Demo Steps
1. **Show Python Parsing:** Query for Python entities
   ```sql
   SELECT entity_name, entity_type FROM chunks WHERE language = 'python' LIMIT 10;
   ```
2. **Show JavaScript Parsing:** Query for JS entities
3. **Show C++ Parsing:** Query for C++ entities
4. **Compare:** Show same logical structure across languages
5. **Performance:** Show ingestion stats (LOC/minute)

### Expected Outcome
- All three languages correctly parsed
- Consistent entity types across languages
- Meets performance target

---

## Technical Tasks

- [x] **T1:** Implement `TreeSitterParser` base class (backend)
- [x] **T2:** Create Python grammar integration and queries (backend)
- [x] **T3:** Create JavaScript/TS grammar integration and queries (backend)
- [x] **T4:** Create C/C++ grammar integration and queries (backend)
- [x] **T5:** Implement file extension to grammar routing (backend)
- [x] **T6:** Add dynamic grammar loading (backend)
- [x] **T7:** Create TextChunk with consistent metadata (backend)
- [x] **T8:** Write unit tests per language (test)
- [x] **T9:** Performance benchmark test (test)
- [x] **T10:** Create Java grammar integration and queries (backend)

---

## Test Scenarios

| Scenario | Given | When | Then |
|:---------|:------|:-----|:-----|
| Python function | .py file with def | Parse file | Function extracted |
| Python class | .py file with class | Parse file | Class and methods extracted |
| JS function | .js file with function | Parse file | Function extracted |
| JS class (ES6) | .js file with class | Parse file | Class extracted |
| TypeScript | .ts file | Parse file | Types and functions extracted |
| Java class | .java file with annotations and generics | Parse file | Classes/interfaces/methods extracted |
| C function | .c file with functions | Parse file | Functions extracted |
| C++ class | .cpp file with class | Parse file | Class and methods extracted |
| Mixed repo | Repo with multiple langs | Ingest | All languages parsed correctly |

---

## Dependencies

- **Blocked by:** US-01-01 (needs files to parse)
- **Enables:** US-02-01 (search needs chunks), US-01-08 (grammar management)

---

## Risks

| Risk | Impact | Likelihood | Mitigation |
|:-----|:-------|:-----------|:-----------|
| Native library loading | Platform issues | Medium | Pre-built binaries; Docker support |
| Grammar version mismatch | Parsing failures | Low | Pin grammar versions |
| Complex syntax edge cases | Incomplete parsing | Medium | Comprehensive test suite |

---

## Definition of Ready

- [x] Acceptance criteria clear
- [x] Dependencies identified
- [x] Tech tasks estimated
- [x] Test scenarios defined
- [x] Demo script approved
- [x] No blockers

---

## Notes
- Use java-tree-sitter binding
- Grammars needed: tree-sitter-python, tree-sitter-javascript, tree-sitter-typescript, tree-sitter-java, tree-sitter-c, tree-sitter-cpp
- Consider bundling grammars vs dynamic download

