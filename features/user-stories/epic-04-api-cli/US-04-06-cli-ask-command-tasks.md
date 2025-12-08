# Tasks for US-04-06: CLI Ask Command

## Story Reference
- **Epic:** EPIC-04 (REST API & CLI)
- **Story:** US-04-06
- **Story Points:** 2
- **Sprint Target:** Sprint 5

## Task List

### T1: Create AskCommand Picocli class
- **Description:** Create `AskCommand` class using Picocli framework. Define command name `ask`. Set up command structure with question parameter and options. Integrate with CLI framework.
- **Estimated Hours:** 2 hours
- **Assignee:** TBD
- **Status:** Not Started
- **Dependencies:** US-04-03 (needs RAG API)
- **Acceptance Criteria:**
  - [ ] AskCommand class created
  - [ ] Command name `ask` defined
  - [ ] Picocli integration working
  - [ ] Help text generated
- **Technical Notes:** Use Picocli for CLI framework. Annotate with `@Command(name = "ask")`. Add question as `@Parameters`.

### T2: Implement streaming terminal output
- **Description:** Implement streaming output in terminal that displays answer tokens as they're generated. Update terminal output in real-time. Handle terminal formatting and line wrapping. Support cancellation (Ctrl+C).
- **Estimated Hours:** 5 hours
- **Assignee:** TBD
- **Status:** Not Started
- **Dependencies:** T1 (needs command class), US-04-03 (needs streaming API)
- **Acceptance Criteria:**
  - [ ] Streaming output implemented
  - [ ] Tokens displayed as generated
  - [ ] Terminal formatting handled
  - [ ] Cancellation supported
- **Technical Notes:** Subscribe to SSE stream from RAG API. Print tokens as received. Handle terminal width and wrapping. Support Ctrl+C for cancellation.

### T3: Display sources after answer
- **Description:** Implement source display after answer is complete. Show source list with file paths, entity names, line numbers, and relevance scores. Format sources clearly and readably.
- **Estimated Hours:** 3 hours
- **Assignee:** TBD
- **Status:** Not Started
- **Dependencies:** T2 (needs streaming), US-03-05 (needs source attribution)
- **Acceptance Criteria:**
  - [ ] Sources displayed after answer
  - [ ] Clear, readable format
  - [ ] Includes file path, entity, lines
  - [ ] Relevance scores shown
- **Technical Notes:** Format: `\nSources:\n1. path/to/file.java - EntityName.method() (lines 25-45) [score: 0.92]\n`. Use clear separator between answer and sources.

### T4: Add model option
- **Description:** Add `--model` option to specify LLM model (e.g., gpt-4, codellama, claude-sonnet). Pass model to RAG API. Validate model name if possible.
- **Estimated Hours:** 2 hours
- **Assignee:** TBD
- **Status:** Not Started
- **Dependencies:** T1 (needs command class)
- **Acceptance Criteria:**
  - [ ] --model option added
  - [ ] Model passed to API
  - [ ] Option documented in help
  - [ ] Validation if possible
- **Technical Notes:** Use `@Option(names = "--model")`. Pass model in RAG request. Support model names: gpt-4, gpt-3.5-turbo, codellama, claude-sonnet, etc.

### T5: Add non-streaming mode
- **Description:** Implement non-streaming mode with `--no-stream` flag. When set, wait for complete answer and display all at once. Show sources after complete answer.
- **Estimated Hours:** 2 hours
- **Assignee:** TBD
- **Status:** Not Started
- **Dependencies:** T2 (needs streaming), US-04-03 (needs non-streaming API)
- **Acceptance Criteria:**
  - [ ] --no-stream option added
  - [ ] Non-streaming mode works
  - [ ] Complete answer displayed
  - [ ] Sources shown after answer
- **Technical Notes:** Use `@Option(names = "--no-stream")`. Call RAG API with stream=false. Display complete answer when received.

### T6: Write command tests
- **Description:** Create unit tests for AskCommand. Test question parsing, streaming output, source display, model option, and help text. Use Picocli's testing utilities.
- **Estimated Hours:** 3 hours
- **Assignee:** TBD
- **Status:** Not Started
- **Dependencies:** T1-T5 (needs complete implementation)
- **Acceptance Criteria:**
  - [ ] Unit tests for command
  - [ ] Tests cover option parsing
  - [ ] Tests cover output formatting
  - [ ] Tests verify streaming and non-streaming
  - [ ] Test coverage >80%
- **Technical Notes:** Use Picocli's `CommandLine.execute()` for testing. Mock RAG API. Verify command output and behavior.

---

## Summary
- **Total Tasks:** 6
- **Total Estimated Hours:** 17 hours
- **Story Points:** 2 (1 SP ≈ 8.5 hours, aligns with estimate)

