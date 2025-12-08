# Tasks for US-04-05: CLI Search Command

## Story Reference
- **Epic:** EPIC-04 (REST API & CLI)
- **Story:** US-04-05
- **Story Points:** 2
- **Sprint Target:** Sprint 4

## Task List

### T1: Create SearchCommand Picocli class
- **Description:** Create `SearchCommand` class using Picocli framework. Define command name `search`. Set up command structure with query parameter and filter options. Integrate with CLI framework.
- **Estimated Hours:** 2 hours
- **Assignee:** TBD
- **Status:** Not Started
- **Dependencies:** US-04-02 (needs search API)
- **Acceptance Criteria:**
  - [ ] SearchCommand class created
  - [ ] Command name `search` defined
  - [ ] Picocli integration working
  - [ ] Help text generated
- **Technical Notes:** Use Picocli for CLI framework. Annotate with `@Command(name = "search")`. Add query as `@Parameters`.

### T2: Add filter options
- **Description:** Add command options: `--language`, `--repo`, `--type` (entity_type), `--limit`, `--json` (output format), `--quiet` (minimal output). Parse and validate options.
- **Estimated Hours:** 3 hours
- **Assignee:** TBD
- **Status:** Not Started
- **Dependencies:** T1 (needs command class)
- **Acceptance Criteria:**
  - [ ] All filter options added
  - [ ] Options validated
  - [ ] Default values set
  - [ ] Help text includes all options
- **Technical Notes:** Use `@Option` annotations. Validate language and entity_type enums. Set default limit to 10. Support multiple values for filters if needed.

### T3: Implement result formatting
- **Description:** Implement human-readable result formatting for terminal output. Format search results with file path, entity name, code snippet, and relevance score. Use clear, readable layout.
- **Estimated Hours:** 4 hours
- **Assignee:** TBD
- **Status:** Not Started
- **Dependencies:** T1, T2 (needs command and options), US-04-02 (needs search API)
- **Acceptance Criteria:**
  - [ ] Results formatted for terminal
  - [ ] Clear, readable layout
  - [ ] Includes file path, entity, snippet
  - [ ] Handles long lines gracefully
- **Technical Notes:** Format: `File: path/to/file.java\nEntity: EntityName.method()\nScore: 0.95\n\n<code snippet>\n\n---\n`. Truncate long snippets. Use proper spacing.

### T4: Add syntax highlighting
- **Description:** Implement syntax highlighting for code snippets in terminal output. Use library like Jansi or similar for ANSI color codes. Highlight code based on language. Support color/no-color modes.
- **Estimated Hours:** 4 hours
- **Assignee:** TBD
- **Status:** Not Started
- **Dependencies:** T3 (needs result formatting)
- **Acceptance Criteria:**
  - [ ] Syntax highlighting implemented
  - [ ] Works for multiple languages
  - [ ] Color output when terminal supports it
  - [ ] No-color mode supported
- **Technical Notes:** Use library like `org.fusesource.jansi:jansi` or `com.github.javaparser:javaparser-core` for syntax highlighting. Detect terminal color support. Support `--no-color` flag.

### T5: Add JSON output mode
- **Description:** Implement JSON output mode when `--json` flag is set. Output search results as JSON matching API response format. Ensure JSON is valid and properly formatted. Support `--quiet` for minimal JSON.
- **Estimated Hours:** 3 hours
- **Assignee:** TBD
- **Status:** Not Started
- **Dependencies:** T1, T2 (needs command and options)
- **Acceptance Criteria:**
  - [ ] JSON output mode implemented
  - [ ] JSON matches API format
  - [ ] Valid JSON output
  - [ ] Quiet mode for minimal JSON
- **Technical Notes:** Use Jackson for JSON serialization. Output same format as SearchResponse DTO. Support `--quiet` for just results array. Pretty print or compact based on flag.

### T6: Write command tests
- **Description:** Create unit tests for SearchCommand. Test query parsing, filter options, output formatting, JSON mode, and help text. Use Picocli's testing utilities.
- **Estimated Hours:** 3 hours
- **Assignee:** TBD
- **Status:** Not Started
- **Dependencies:** T1-T5 (needs complete implementation)
- **Acceptance Criteria:**
  - [ ] Unit tests for command
  - [ ] Tests cover option parsing
  - [ ] Tests cover output formatting
  - [ ] Tests verify JSON mode
  - [ ] Test coverage >80%
- **Technical Notes:** Use Picocli's `CommandLine.execute()` for testing. Mock search API. Verify command output and formatting.

---

## Summary
- **Total Tasks:** 6
- **Total Estimated Hours:** 19 hours
- **Story Points:** 2 (1 SP ≈ 9.5 hours, note: this includes syntax highlighting which is complex)

