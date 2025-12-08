# Tasks for US-03-05: Source Attribution in Answers

## Story Reference
- **Epic:** EPIC-03 (RAG Answer Generation)
- **Story:** US-03-05
- **Story Points:** 3
- **Sprint Target:** Sprint 5

## Task List

### T1: Update prompt template to require citations
- **Description:** Update prompt template to include strong instructions for LLM to cite sources in answers. Specify citation format: `[Source: path/to/file.java:42]`. Include examples of proper citations. Emphasize importance of accurate citations.
- **Estimated Hours:** 3 hours
- **Assignee:** TBD
- **Status:** Not Started
- **Dependencies:** US-03-03 (needs prompt template)
- **Acceptance Criteria:**
  - [ ] Prompt includes citation instructions
  - [ ] Citation format specified
  - [ ] Examples provided in prompt
  - [ ] Instructions are clear and strong
- **Technical Notes:** Add citation section to prompt template. Format: "Always cite sources using [Source: filepath:line]. Example: [Source: src/auth/AuthService.java:25]". Include in system prompt for emphasis.

### T2: Parse LLM response for citation extraction
- **Description:** Implement citation parser that extracts citations from LLM-generated answers. Parse citation format `[Source: path:line]` and extract file path, entity name, and line numbers. Handle multiple citations per answer. Validate extracted citations.
- **Estimated Hours:** 5 hours
- **Assignee:** TBD
- **Status:** Not Started
- **Dependencies:** T1 (needs citation format), US-03-04 (needs LLM responses)
- **Acceptance Criteria:**
  - [ ] Citations extracted from answer text
  - [ ] File path, line numbers extracted
  - [ ] Multiple citations handled
  - [ ] Invalid citations handled gracefully
- **Technical Notes:** Use regex or parser to extract citations: `\[Source:\s*([^\]]+)\]`. Parse path and line numbers. Validate citations match actual source chunks. Handle malformed citations.

### T3: Attach source metadata to response
- **Description:** Attach full source metadata to RAG response. Include all source chunks used in context, their relevance scores, and metadata (file path, entity name, line ranges). Include both cited sources and all context sources.
- **Estimated Hours:** 4 hours
- **Assignee:** TBD
- **Status:** Not Started
- **Dependencies:** T2 (needs citation extraction), US-03-03 (needs source chunks)
- **Acceptance Criteria:**
  - [ ] Source metadata attached to response
  - [ ] Includes all context sources
  - [ ] Includes relevance scores
  - [ ] Metadata is complete and accurate
- **Technical Notes:** Extend RagResponse DTO with `sources` field. Include: file_path, entity_name, line_range, relevance_score, chunk_id. Include both cited and non-cited sources for transparency.

### T4: Create source DTO with all metadata
- **Description:** Create SourceDTO class that represents source information in responses. Include all metadata fields: file path, entity name, line numbers, relevance score, repository, language. Make it serializable to JSON.
- **Estimated Hours:** 2 hours
- **Assignee:** TBD
- **Status:** Not Started
- **Dependencies:** T3 (needs source metadata)
- **Acceptance Criteria:**
  - [ ] SourceDTO class created
  - [ ] Includes all required metadata fields
  - [ ] Serializable to JSON
  - [ ] Fields are well-documented
- **Technical Notes:** Use Java record or POJO with Jackson annotations. Fields: file_path, entity_name, line_start, line_end, relevance_score, repository, language, chunk_id. Include optional fields for future use.

### T5: Write tests for citation parsing
- **Description:** Create comprehensive tests for citation extraction and parsing. Test various citation formats, multiple citations, malformed citations, and edge cases. Verify citations are correctly extracted and validated.
- **Estimated Hours:** 3 hours
- **Assignee:** TBD
- **Status:** Not Started
- **Dependencies:** T1-T4 (needs complete implementation)
- **Acceptance Criteria:**
  - [ ] Tests for citation extraction
  - [ ] Tests for various citation formats
  - [ ] Tests for malformed citations
  - [ ] Tests for citation validation
  - [ ] Test coverage >80%
- **Technical Notes:** Use JUnit 5. Test with sample answer texts containing citations. Test edge cases: no citations, multiple citations, invalid format, missing file paths. Verify extracted data is correct.

---

## Summary
- **Total Tasks:** 5
- **Total Estimated Hours:** 17 hours
- **Story Points:** 3 (1 SP ≈ 5.7 hours, aligns with estimate)

