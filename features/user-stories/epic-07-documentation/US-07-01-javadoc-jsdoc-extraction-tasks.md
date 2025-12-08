# Tasks for US-07-01: Javadoc/JSDoc Extraction

## Story Reference
- **Epic:** EPIC-07 (Documentation Intelligence)
- **Story:** US-07-01
- **Story Points:** 5
- **Sprint Target:** Sprint 3

## Task List

### T1: Implement Javadoc parser
- **Description:** Implement Javadoc parser that extracts documentation from Java source files. Parse Javadoc comments (/** ... */) and extract description, @param tags, @return tags, @throws tags. Use JavaParser or custom parser.
- **Estimated Hours:** 6 hours
- **Assignee:** TBD
- **Status:** Not Started
- **Dependencies:** US-01-04 (needs Java parsing)
- **Acceptance Criteria:**
  - [ ] Javadoc parser implemented
  - [ ] Description extracted
  - [ ] @param tags extracted
  - [ ] @return tags extracted
  - [ ] @throws tags extracted
- **Technical Notes:** Use JavaParser's JavadocComment or custom parser. Parse Javadoc AST. Extract tags and descriptions. Handle multi-line descriptions.

### T2: Implement JSDoc parser
- **Description:** Implement JSDoc parser that extracts documentation from JavaScript/TypeScript source files. Parse JSDoc comments (/** ... */) and extract description, @param tags, @return tags, @throws tags. Handle TypeScript-specific tags.
- **Estimated Hours:** 5 hours
- **Assignee:** TBD
- **Status:** Not Started
- **Dependencies:** US-01-05 (needs Tree-sitter parsing)
- **Acceptance Criteria:**
  - [ ] JSDoc parser implemented
  - [ ] Description extracted
  - [ ] @param tags extracted
  - [ ] @return tags extracted
  - [ ] TypeScript tags handled
- **Technical Notes:** Parse JSDoc comments from source. Extract tags using regex or parser. Handle TypeScript @type, @interface tags. Store parsed documentation.

### T3: Create DocCommentParser interface
- **Description:** Create `DocCommentParser` interface that abstracts documentation parsing across languages. Define methods for parsing documentation comments and extracting structured information. Support multiple documentation formats.
- **Estimated Hours:** 3 hours
- **Assignee:** TBD
- **Status:** Not Started
- **Dependencies:** T1, T2 (needs parsers)
- **Acceptance Criteria:**
  - [ ] DocCommentParser interface created
  - [ ] Methods for parsing defined
  - [ ] Language-agnostic design
  - [ ] Interface well-documented
- **Technical Notes:** Define interface with methods: parse(comment, language), extractDescription(), extractParams(), extractReturns(), extractThrows(). Use factory pattern for language-specific parsers.

### T4: Link docs to code entities
- **Description:** Implement logic to link extracted documentation to code entities (classes, methods, functions). Match documentation comments to their associated entities. Store documentation with entity metadata.
- **Estimated Hours:** 4 hours
- **Assignee:** TBD
- **Status:** Not Started
- **Dependencies:** T1, T2, T3 (needs parsers and interface)
- **Acceptance Criteria:**
  - [ ] Documentation linked to entities
  - [ ] Matching logic works
  - [ ] Documentation stored with entities
  - [ ] Links are accurate
- **Technical Notes:** Match documentation comments to entities by position (comments before entity). Store documentation in entity metadata. Handle cases where documentation is missing.

### T5: Add doc fields to index schema
- **Description:** Add documentation fields to search index schema. Add `doc_summary` field for documentation content. Add fields for @param, @return, @throws if needed. Update index mapping.
- **Estimated Hours:** 3 hours
- **Assignee:** TBD
- **Status:** Not Started
- **Dependencies:** T4 (needs documentation linked), US-02-01 (needs Lucene index)
- **Acceptance Criteria:**
  - [ ] doc_summary field added
  - [ ] Documentation indexed
  - [ ] Fields searchable
  - [ ] Schema updated
- **Technical Notes:** Add doc_summary field to Lucene index. Index documentation content. Support full-text search on documentation. Update index mapping.

### T6: Write tests for various doc formats
- **Description:** Create comprehensive tests for Javadoc and JSDoc parsing. Test various documentation formats, edge cases (missing tags, malformed comments), and entity linking. Use test files with known documentation.
- **Estimated Hours:** 4 hours
- **Assignee:** TBD
- **Status:** Not Started
- **Dependencies:** T1-T5 (needs complete implementation)
- **Acceptance Criteria:**
  - [ ] Tests for Javadoc parsing
  - [ ] Tests for JSDoc parsing
  - [ ] Tests for entity linking
  - [ ] Test coverage >80%
- **Technical Notes:** Create test files with various documentation formats. Test parsing accuracy. Test entity linking. Test edge cases.

---

## Summary
- **Total Tasks:** 6
- **Total Estimated Hours:** 25 hours
- **Story Points:** 5 (1 SP ≈ 5 hours, aligns with estimate)

