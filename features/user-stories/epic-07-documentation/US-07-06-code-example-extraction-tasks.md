# Tasks for US-07-06: Code Example Extraction

## Story Reference
- **Epic:** EPIC-07 (Documentation Intelligence)
- **Story:** US-07-06
- **Story Points:** 3
- **Sprint Target:** Sprint 6

## Task List

### T1: Extract @example from Javadoc/JSDoc
- **Description:** Implement extraction of code examples from @example tags in Javadoc and JSDoc comments. Parse @example tag content. Extract code blocks. Link examples to documented entities.
- **Estimated Hours:** 4 hours
- **Assignee:** TBD
- **Status:** Not Started
- **Dependencies:** US-07-01 (needs Javadoc/JSDoc parsing)
- **Acceptance Criteria:**
  - [ ] @example tags extracted
  - [ ] Code blocks extracted
  - [ ] Examples linked to entities
  - [ ] Multiple examples supported
- **Technical Notes:** Parse @example tags from Javadoc/JSDoc. Extract code block content. Store examples with entity references. Support multiple @example tags per entity.

### T2: Extract doctest from Python
- **Description:** Implement extraction of code examples from Python doctest comments. Parse doctest format (>>> code). Extract example code. Link examples to documented functions/classes.
- **Estimated Hours:** 4 hours
- **Assignee:** TBD
- **Status:** Not Started
- **Dependencies:** US-07-02 (needs Python docstring parsing)
- **Acceptance Criteria:**
  - [ ] doctest examples extracted
  - [ ] Code blocks extracted
  - [ ] Examples linked to entities
  - [ ] doctest format parsed correctly
- **Technical Notes:** Parse doctest format (>>> code, expected output). Extract example code. Store examples with entity references. Handle multi-line examples.

### T3: Create example chunk type
- **Description:** Create example chunk type for storing code examples separately from documentation. Define ExampleChunk model with example code, language, linked entity, and metadata. Store examples as separate searchable chunks.
- **Estimated Hours:** 3 hours
- **Assignee:** TBD
- **Status:** Not Started
- **Dependencies:** T1, T2 (needs example extraction)
- **Acceptance Criteria:**
  - [ ] ExampleChunk model created
  - [ ] Examples stored separately
  - [ ] Examples are searchable
  - [ ] Linked to entities
- **Technical Notes:** Create ExampleChunk class. Include: example_code, language, entity_id, entity_name, source_file. Store as separate chunks in index. Link to documented entities.

### T4: Link examples to entities
- **Description:** Implement logic to link extracted examples to documented entities. Match examples to their associated entities. Store entity references in examples. Support examples without explicit entity links.
- **Estimated Hours:** 3 hours
- **Assignee:** TBD
- **Status:** Not Started
- **Dependencies:** T1, T2, T3 (needs extraction and chunk type)
- **Acceptance Criteria:**
  - [ ] Examples linked to entities
  - [ ] Entity references stored
  - [ ] Links are accurate
  - [ ] Examples without links handled
- **Technical Notes:** Match examples to entities by position (examples in entity documentation). Store entity_id in examples. Handle examples without explicit links (link to containing entity).

### T5: Add examples_only filter
- **Description:** Add `examples_only` filter parameter to search API. When true, search only in example chunks. Filter out non-example results. Return only code examples.
- **Estimated Hours:** 3 hours
- **Assignee:** TBD
- **Status:** Not Started
- **Dependencies:** T3 (needs example chunks), US-04-02 (needs search API)
- **Acceptance Criteria:**
  - [ ] examples_only parameter added
  - [ ] Filters to examples only
  - [ ] Non-examples excluded
  - [ ] Parameter works correctly
- **Technical Notes:** Add examples_only query parameter. Modify search query to search only example chunks when true. Filter results to only examples. Return example code in results.

### T6: Write tests
- **Description:** Create comprehensive tests for code example extraction. Test @example extraction, doctest extraction, example linking, and examples_only filter. Use test files with known examples.
- **Estimated Hours:** 3 hours
- **Assignee:** TBD
- **Status:** Not Started
- **Dependencies:** T1-T5 (needs complete implementation)
- **Acceptance Criteria:**
  - [ ] Tests for @example extraction
  - [ ] Tests for doctest extraction
  - [ ] Tests for example linking
  - [ ] Tests for examples_only filter
  - [ ] Test coverage >80%
- **Technical Notes:** Create test files with code examples. Test extraction accuracy. Test example linking. Test examples_only filter. Verify examples in search results.

---

## Summary
- **Total Tasks:** 6
- **Total Estimated Hours:** 20 hours
- **Story Points:** 3 (1 SP ≈ 6.7 hours, aligns with estimate)

