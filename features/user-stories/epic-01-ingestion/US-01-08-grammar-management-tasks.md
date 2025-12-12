# Tasks for US-01-08: Dynamic Grammar Management

## Story Reference
- **Epic:** EPIC-01 (Code Ingestion & Indexing)
- **Story:** US-01-08
- **Story Points:** 5
- **Sprint Target:** Sprint 3

## Task List

### T1: Implement ParserRegistry class
- **Description:** Create the `ParserRegistry` class that serves as a central registry for all code parsers (Tree-sitter and JavaParser). The registry should map file extensions to parser instances, handle parser instantiation, and provide lookup functionality. Support dynamic registration of parsers.
- **Estimated Hours:** 4 hours
- **Assignee:** TBD
- **Status:** Completed
- **Dependencies:** US-01-04, US-01-05 (needs parser implementations)
- **Acceptance Criteria:**
  - [x] Registry maps file extensions to parsers
  - [x] Supports dynamic parser registration
  - [x] Provides fast lookup (<10ms)
  - [x] Handles multiple extensions per parser
- **Technical Notes:** Use Map<String, ParserFactory> for extension mapping. Support both single extensions (.java) and multiple extensions (.js, .jsx). Use CDI for dependency injection of parsers.

### T2: Implement GrammarManager class
- **Description:** Create the `GrammarManager` class that handles Tree-sitter grammar lifecycle: downloading, caching, version management, and loading. The manager should download grammars from GitHub releases, cache them locally, track versions, and provide grammar instances to parsers.
- **Estimated Hours:** 6 hours
- **Assignee:** TBD
- **Status:** Completed
- **Dependencies:** T1 (needs ParserRegistry structure)
- **Acceptance Criteria:**
  - [x] Downloads grammars from GitHub releases
  - [x] Caches grammars locally in configurable directory
  - [x] Tracks grammar versions
  - [x] Provides grammar instances to parsers
  - [x] Grammar loading <500ms (cold start)
- **Technical Notes:** Download from https://github.com/tree-sitter/tree-sitter-{language}/releases. Cache in `~/.megabrain/grammars/` or configurable path. Store version metadata in JSON file. Handle platform-specific binaries (.so, .dylib, .dll).

### T3: Create grammar download logic from GitHub releases
- **Description:** Implement grammar downloading functionality that fetches Tree-sitter grammar binaries from GitHub releases. Support version specification, download progress tracking, and error handling. Verify downloaded files (checksums if available).
- **Estimated Hours:** 5 hours
- **Assignee:** TBD
- **Status:** Completed
- **Dependencies:** T2 (needs GrammarManager structure)
- **Acceptance Criteria:**
  - [x] Downloads grammar binaries from GitHub releases
  - [x] Supports version specification
  - [x] Handles download failures gracefully
  - [x] Verifies downloaded files
  - [x] Shows download progress
- **Technical Notes:** Use GitHub API to list releases. Download platform-specific binaries. Support retry logic for network failures. Verify file integrity (size check, checksum if available).

### T4: Implement local caching with version tracking
- **Description:** Implement local file system caching for downloaded grammars. Store grammars in organized directory structure with version information. Track which versions are cached and provide fast lookup. Handle cache cleanup for old versions.
- **Estimated Hours:** 4 hours
- **Assignee:** TBD
- **Status:** Completed
- **Dependencies:** T2, T3 (needs GrammarManager and download logic)
- **Acceptance Criteria:**
  - [x] Grammars cached in organized directory structure
  - [x] Version information stored with cached grammars
  - [x] Fast lookup of cached grammars
  - [x] Cache cleanup for old versions (optional)
- **Technical Notes:** Use directory structure: `{cache_dir}/{language}/{version}/{platform}/grammar.{ext}`. Store version metadata in JSON file. Implement cache lookup before download.

### T5: Add version pinning configuration
- **Description:** Implement configuration support for grammar version pinning. Allow users to specify exact grammar versions in configuration file to prevent unexpected updates. Support both global and per-language version pinning.
- **Estimated Hours:** 3 hours
- **Assignee:** TBD
- **Status:** Completed
- **Dependencies:** T2, T4 (needs GrammarManager and caching)
- **Acceptance Criteria:**
  - [x] Version pinning configuration supported
  - [x] Global default version configuration
  - [x] Per-language version override
  - [x] Pinned versions respected during download
- **Technical Notes:** Use Quarkus configuration. Format: `megabrain.grammars.{language}.version=1.2.3`. Support default version for all languages. Validate version format.

### T6: Add rollback/downgrade capability
- **Description:** Implement rollback functionality to revert to previous grammar version if new version causes issues. Track version history and allow manual or automatic rollback. Preserve previous versions in cache for rollback.
- **Estimated Hours:** 4 hours
- **Assignee:** TBD
- **Status:** Completed
- **Dependencies:** T2, T4, T5 (needs GrammarManager, caching, version tracking)
- **Acceptance Criteria:**
  - [x] Previous versions preserved in cache
  - [x] Rollback to previous version supported
  - [x] Version history tracked
  - [x] Rollback can be triggered manually or automatically
- **Technical Notes:** Keep last N versions in cache (e.g., last 3). Implement rollback API endpoint or configuration. Test rollback with known problematic grammar versions.

### T7: Create health check for grammar status
- **Description:** Implement health check endpoint that verifies all required grammars are loaded and available. Check grammar loading status, version information, and report any missing or failed grammars. Integrate with Quarkus health checks.
- **Estimated Hours:** 3 hours
- **Assignee:** TBD
- **Status:** Not Started
- **Dependencies:** T1, T2 (needs ParserRegistry and GrammarManager)
- **Acceptance Criteria:**
  - [ ] Health check endpoint reports grammar status
  - [ ] Lists all required grammars and their status
  - [ ] Reports missing or failed grammars
  - [ ] Integrates with `/q/health/ready` endpoint
- **Technical Notes:** Use Quarkus SmallRye Health. Create custom health check that queries GrammarManager. Report status: loaded, missing, failed. Include version information in health response.

### T8: Add grammars: Go, Rust, Kotlin, Ruby
- **Description:** Integrate Tree-sitter grammars for Go, Rust, Kotlin, and Ruby languages. Create parser implementations for each language following the TreeSitterParser pattern. Define queries to extract functions, classes, and other entities.
- **Estimated Hours:** 8 hours
- **Assignee:** TBD
- **Status:** Not Started
- **Dependencies:** T1, T2 (needs ParserRegistry and GrammarManager), US-01-05 (needs base TreeSitterParser)
- **Acceptance Criteria:**
  - [ ] Go grammar integrated and parser implemented
  - [ ] Rust grammar integrated and parser implemented
  - [ ] Kotlin grammar integrated and parser implemented
  - [ ] Ruby grammar integrated and parser implemented
  - [ ] All languages extract functions and classes correctly
- **Technical Notes:** Use tree-sitter-go, tree-sitter-rust, tree-sitter-kotlin, tree-sitter-ruby grammars. Create language-specific queries for each. Handle language-specific constructs (e.g., Rust traits, Go interfaces).

### T9: Add grammars: Scala, Swift, PHP, C#
- **Description:** Integrate Tree-sitter grammars for Scala, Swift, PHP, and C# languages. Create parser implementations for each language following the TreeSitterParser pattern. Define queries to extract functions, classes, and other entities.
- **Estimated Hours:** 8 hours
- **Assignee:** TBD
- **Status:** Not Started
- **Dependencies:** T1, T2 (needs ParserRegistry and GrammarManager), US-01-05 (needs base TreeSitterParser)
- **Acceptance Criteria:**
  - [ ] Scala grammar integrated and parser implemented
  - [ ] Swift grammar integrated and parser implemented
  - [ ] PHP grammar integrated and parser implemented
  - [ ] C# grammar integrated and parser implemented
  - [ ] All languages extract functions and classes correctly
- **Technical Notes:** Use tree-sitter-scala, tree-sitter-swift, tree-sitter-php, tree-sitter-csharp grammars. Create language-specific queries for each. Handle language-specific constructs (e.g., C# properties, Swift extensions).

### T10: Write unit tests for grammar management
- **Description:** Create comprehensive unit tests for GrammarManager and ParserRegistry. Test grammar downloading, caching, version management, rollback, and health checks. Use mocks for external dependencies (GitHub API, file system).
- **Estimated Hours:** 4 hours
- **Assignee:** TBD
- **Status:** Not Started
- **Dependencies:** T1-T9 (needs complete implementation)
- **Acceptance Criteria:**
  - [ ] Unit tests cover GrammarManager operations
  - [ ] Unit tests cover ParserRegistry operations
  - [ ] Tests use mocks for external dependencies
  - [ ] Test coverage >80%
  - [ ] Tests include error scenarios
- **Technical Notes:** Use JUnit 5 and Mockito. Mock GitHub API calls and file system operations. Test both success and failure paths. Include tests for version pinning and rollback.

---

## Summary
- **Total Tasks:** 10
- **Total Estimated Hours:** 49 hours
- **Story Points:** 5 (1 SP ≈ 9.8 hours, note: this is a complex story with many languages)

