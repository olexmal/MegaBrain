# Tasks for US-04-04: CLI Ingest Command

## Story Reference
- **Epic:** EPIC-04 (REST API & CLI)
- **Story:** US-04-04
- **Story Points:** 3
- **Sprint Target:** Sprint 3

## Task List

### T1: Create IngestCommand Picocli class
- **Description:** Create `IngestCommand` class using Picocli framework. Define command name `ingest`. Set up command structure with options and parameters. Integrate with Quarkus CLI or standalone CLI.
- **Estimated Hours:** 3 hours
- **Assignee:** TBD
- **Status:** Not Started
- **Dependencies:** US-01-01 (needs ingestion service)
- **Acceptance Criteria:**
  - [ ] IngestCommand class created
  - [ ] Command name `ingest` defined
  - [ ] Picocli integration working
  - [ ] Help text generated
- **Technical Notes:** Use Picocli for CLI framework. Annotate class with `@Command(name = "ingest")`. Add `@Option` and `@Parameters` annotations. Generate help with `--help`.

### T2: Add source, repo, branch options
- **Description:** Add command options: `--source` (github/gitlab/bitbucket/local), `--repo` (repository URL/identifier), `--branch` (optional, default: main/master), `--token` (optional, for private repos), `--incremental` (boolean flag).
- **Estimated Hours:** 3 hours
- **Assignee:** TBD
- **Status:** Not Started
- **Dependencies:** T1 (needs command class)
- **Acceptance Criteria:**
  - [ ] All options added
  - [ ] Options validated
  - [ ] Default values set
  - [ ] Help text includes all options
- **Technical Notes:** Use `@Option` annotations. Validate source enum. Make branch optional with default. Use `@Option(names = "--incremental", defaultValue = "false")`.

### T3: Implement progress display
- **Description:** Implement progress display in terminal using progress bar or status messages. Show ingestion progress (cloning, parsing, indexing). Update progress in real-time. Handle terminal width and formatting.
- **Estimated Hours:** 4 hours
- **Assignee:** TBD
- **Status:** Not Started
- **Dependencies:** T2 (needs options), US-01-07 (needs progress events)
- **Acceptance Criteria:**
  - [ ] Progress displayed in terminal
  - [ ] Real-time updates
  - [ ] Clear progress indication
  - [ ] Handles terminal formatting
- **Technical Notes:** Use library like `com.github.lalyos:jfiglet` or `org.jline:jline3` for progress bars. Subscribe to progress events from ingestion service. Update terminal output.

### T4: Handle exit codes
- **Description:** Implement proper exit code handling. Return exit code 0 on success, non-zero on failure. Map different error types to appropriate exit codes. Ensure exit codes are set correctly.
- **Estimated Hours:** 2 hours
- **Assignee:** TBD
- **Status:** Not Started
- **Dependencies:** T1-T3 (needs command working)
- **Acceptance Criteria:**
  - [ ] Exit code 0 on success
  - [ ] Non-zero exit codes on failure
  - [ ] Appropriate exit codes for different errors
  - [ ] Exit codes documented
- **Technical Notes:** Use `System.exit(code)` or Picocli's exit code handling. Map exceptions to exit codes (1: general error, 2: invalid arguments, etc.).

### T5: Add verbose logging option
- **Description:** Add `--verbose` option for detailed logging. When enabled, show detailed progress information, debug messages, and stack traces on errors. Control log level based on verbose flag.
- **Estimated Hours:** 2 hours
- **Assignee:** TBD
- **Status:** Not Started
- **Dependencies:** T1 (needs command class)
- **Acceptance Criteria:**
  - [ ] --verbose option added
  - [ ] Verbose mode shows detailed logs
  - [ ] Log level controlled by flag
  - [ ] Help text explains verbose mode
- **Technical Notes:** Use `@Option(names = "--verbose")`. Set log level to DEBUG when verbose. Show additional progress details.

### T6: Write command tests
- **Description:** Create unit tests for IngestCommand. Test option parsing, validation, progress display, exit codes, and help text. Use Picocli's testing utilities.
- **Estimated Hours:** 3 hours
- **Assignee:** TBD
- **Status:** Not Started
- **Dependencies:** T1-T5 (needs complete implementation)
- **Acceptance Criteria:**
  - [ ] Unit tests for command
  - [ ] Tests cover option parsing
  - [ ] Tests cover validation
  - [ ] Tests verify exit codes
  - [ ] Test coverage >80%
- **Technical Notes:** Use Picocli's `CommandLine.execute()` for testing. Mock ingestion service. Verify command behavior and output.

---

## Summary
- **Total Tasks:** 6
- **Total Estimated Hours:** 17 hours
- **Story Points:** 3 (1 SP ≈ 5.7 hours, aligns with estimate)

