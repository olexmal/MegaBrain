# US-01-04: Java Parsing with JavaParser

## Story
**As a** Java developer  
**I want** my Java code parsed into logical chunks (classes, methods)  
**So that** search results are meaningful code units

## Story Points: 5
## Priority: Critical
## Sprint Target: Sprint 1

---

## Acceptance Criteria

- [x] **AC1:** `.java` files detected and routed to JavaParser
- [x] **AC2:** Classes extracted with name, package, modifiers
- [x] **AC3:** Methods extracted with name, parameters, return type
- [x] **AC4:** Fields extracted with name, type, modifiers
- [x] **AC5:** Inner classes and nested types handled correctly
- [x] **AC6:** Each chunk includes: language, entity_type, entity_name, source_file, line range
- [x] **AC7:** Parsing errors logged but don't stop ingestion
- [x] **AC8:** Performance: >10,000 LOC per minute

---

## Demo Script

### Setup
1. Ingest the MegaBrain repository (contains Java code)
2. Ensure repository is fully indexed

### Demo Steps
1. **Show Parsed Entities:** Query database for Java entities
   ```sql
   SELECT entity_name, entity_type, source_file 
   FROM chunks WHERE language = 'java' LIMIT 20;
   ```
2. **Show Class Details:** Display a parsed class with its methods
3. **Show Nested Class:** Demonstrate inner class extraction
4. **Show Method Metadata:** Display method with parameters
5. **Show Line Numbers:** Verify accurate byte/line ranges

### Expected Outcome
- All Java constructs correctly parsed
- Meaningful entity names (not arbitrary splits)
- Nested structures properly represented

---

## Technical Tasks

- [x] **T1:** Implement `JavaParserService` class (backend)
- [x] **T2:** Create AST visitor for class extraction (backend)
- [x] **T3:** Create AST visitor for method extraction (backend)
- [x] **T4:** Create AST visitor for field extraction (backend)
- [x] **T5:** Handle inner classes and anonymous classes (backend)
- [x] **T6:** Implement TextChunk creation with metadata (backend)
- [x] **T7:** Add error handling for malformed Java files (backend)
- [x] **T8:** Write unit tests with sample Java files (test)
- [x] **T9:** Performance benchmark test (test)

---

## Test Scenarios

| Scenario | Given | When | Then |
|:---------|:------|:-----|:-----|
| Simple class | Java file with one class | Parse file | Class extracted with correct name |
| Class with methods | Java file with methods | Parse file | All methods extracted |
| Inner class | Java file with inner class | Parse file | Inner class as separate entity |
| Interface | Java interface file | Parse file | Interface extracted correctly |
| Enum | Java enum file | Parse file | Enum and constants extracted |
| Malformed file | Java file with syntax errors | Parse file | Error logged, partial parse if possible |
| Large file | 5000+ line Java file | Parse file | Completes within performance target |

---

## Dependencies

- **Blocked by:** US-01-01 (needs files to parse)
- **Enables:** US-02-01 (search needs chunks)

---

## Risks

| Risk | Impact | Likelihood | Mitigation |
|:-----|:-------|:-----------|:-----------|
| Complex generics parsing | Metadata incomplete | Low | Test with complex cases |
| Annotation processing | Missing metadata | Low | Include annotation info |
| Performance on large files | Slow parsing | Medium | Optimize visitors; benchmark |

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
- JavaParser version: 3.25+ recommended
- Consider extracting Javadoc here for US-07-01

