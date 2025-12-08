# EPIC-01: Code Ingestion & Indexing

## Epic Overview

| Attribute | Value |
|:----------|:------|
| **Epic ID** | EPIC-01 |
| **Priority** | Critical |
| **Estimated Scope** | XL |
| **Dependencies** | None (Foundation Epic) |
| **Spec Reference** | Section 4.1 (FR-ING) |
| **Status** | Planned |

## Business Value

This is the **foundation epic** upon which all other MegaBrain capabilities depend. Without robust code ingestion and indexing, no search, RAG, or analysis features can function. This epic enables:

- Automatic indexing of an organization's entire codebase from multiple source control systems
- Structure-aware parsing that understands code semantics (classes, functions, methods)
- Support for 14+ programming languages through Tree-sitter and JavaParser
- Efficient incremental updates to keep the index current with active development

---

## User Stories

### US-01-01: Multi-Source Repository Ingestion

**As a** DevOps engineer, **I want** to ingest code from GitHub, GitLab, Bitbucket, and local Git repositories, **so that** all our organization's code is searchable regardless of where it's hosted.

**Acceptance Criteria:**
- [ ] Support GitHub repositories via GitHub API/clone
- [ ] Support GitLab repositories via GitLab API/clone
- [ ] Support Bitbucket repositories via Bitbucket API/clone
- [ ] Support local Git repositories via filesystem path
- [ ] Unified `SourceControlClient` interface abstracts provider differences
- [ ] Credentials managed securely via environment variables or vault
- [ ] Repository metadata (name, branch, commit SHA) captured during ingestion

**Spec Reference:** FR-ING-01

---

### US-01-02: Structure-Aware Code Parsing

**As a** developer, **I want** the system to parse code into logical chunks (functions, classes, methods), **so that** search results are meaningful code units rather than arbitrary text fragments.

**Acceptance Criteria:**
- [ ] Java files parsed via JavaParser extracting classes, methods, fields
- [ ] Python, C, C++, JS, TS files parsed via Tree-sitter
- [ ] Each chunk includes metadata: `language`, `entity_type`, `entity_name`, `source_file`, `byte_range`
- [ ] Parser correctly handles nested structures (inner classes, nested functions)
- [ ] Malformed files handled gracefully with partial parsing or skip
- [ ] Parsing throughput meets NFR: >10,000 LOC per minute

**Spec Reference:** FR-ING-02

---

### US-01-03: Incremental Indexing

**As a** system administrator, **I want** incremental indexing based on git diff, **so that** daily updates are fast and don't require re-indexing the entire codebase.

**Acceptance Criteria:**
- [ ] Full index rebuild supported for initial ingestion
- [ ] Incremental mode detects changed files via `git diff`
- [ ] Only modified/added files are re-parsed and re-indexed
- [ ] Deleted files are removed from the index
- [ ] Scheduler (Quartz) triggers jobs on configurable schedule
- [ ] Manual trigger available via API

**Spec Reference:** FR-ING-03

---

### US-01-04: Real-Time Progress Streaming

**As a** user, **I want** to see real-time progress during ingestion, **so that** I know the system is working and can estimate completion time.

**Acceptance Criteria:**
- [ ] SSE endpoint streams progress events
- [ ] Events include: `stage` (Cloning/Parsing/Embedding/Storing/Complete), `message`, `percentage`
- [ ] Progress updates at least every 5 seconds during active processing
- [ ] Error events streamed if ingestion fails
- [ ] Client can cancel in-progress ingestion

**Spec Reference:** FR-ING-04

---

### US-01-05: Extended Language Support

**As a** developer working with Go/Rust/Kotlin/etc., **I want** my language to be supported, **so that** all our polyglot codebase is searchable.

**Acceptance Criteria:**
- [ ] Go files parsed via Tree-sitter
- [ ] Rust files parsed via Tree-sitter
- [ ] Kotlin files parsed via Tree-sitter (+ Kotlin Compiler API for advanced analysis)
- [ ] Ruby files parsed via Tree-sitter
- [ ] Scala files parsed via Tree-sitter
- [ ] Swift files parsed via Tree-sitter
- [ ] PHP files parsed via Tree-sitter
- [ ] C# files parsed via Tree-sitter
- [ ] Grammar downloading on-demand with local caching
- [ ] Grammar version pinning for reproducibility
- [ ] Grammar cold start <500ms per language

**Spec Reference:** FR-ING-05

---

### US-01-06: Dynamic Grammar Management

**As a** system administrator, **I want** Tree-sitter grammars managed automatically, **so that** I don't need to manually bundle grammars for each language.

**Acceptance Criteria:**
- [ ] `ParserRegistry` dynamically loads grammars based on file extension
- [ ] `GrammarManager` downloads grammars from official Tree-sitter repositories
- [ ] Downloaded grammars cached locally
- [ ] Version pinning configuration prevents unexpected grammar updates
- [ ] Rollback capability if new grammar version causes issues
- [ ] Health check verifies all required grammars are loaded

**Spec Reference:** FR-ING-05 (Grammar Management)

---

## Technical Notes

### Key Components
- **`RepositoryIngestionService`:** Core orchestrator coordinating the ingestion pipeline
- **`SourceControlClientFactory`:** Factory pattern for creating SCM-specific clients
- **`TreeSitterParser`:** Generic parser using Tree-sitter for multiple languages
- **`JavaParserService`:** Specialized parser for Java using JavaParser library
- **`ParserRegistry`:** Central registry mapping file extensions to parsers
- **`GrammarManager`:** Handles Tree-sitter grammar lifecycle

### Technology Stack
| Component | Technology |
|:----------|:-----------|
| SCM Integration | JGit, GitHub API, GitLab API, Bitbucket API |
| Java Parsing | JavaParser 3.x |
| Polyglot Parsing | Tree-sitter via java-tree-sitter binding |
| Scheduling | Quartz Scheduler |
| Streaming | Mutiny Multi, SSE |

### Architecture Considerations
- All operations must be non-blocking using Mutiny reactive streams
- Large repositories should be processed in chunks to manage memory
- Parser errors should be isolated - one bad file shouldn't stop entire ingestion
- Consider parallel parsing for performance on multi-core systems

### Data Model
```java
record TextChunk(
    String id,
    String content,
    String language,
    String entityType,      // class, method, function, etc.
    String entityName,
    String sourceFile,
    String repository,
    String branch,
    int startLine,
    int endLine,
    int startByte,
    int endByte
)
```

---

## Risks & Mitigations

| Risk | Impact | Likelihood | Mitigation |
|:-----|:-------|:-----------|:-----------|
| Tree-sitter grammar incompatibility | Parser fails for certain language constructs | Medium | Pin grammar versions; maintain fallback to raw text chunking |
| Large repository memory exhaustion | OOM during ingestion | Medium | Stream processing; chunk-based parsing; configurable batch sizes |
| SCM API rate limiting | Ingestion blocked by GitHub/GitLab | Medium | Implement backoff; use clone over API when possible; cache |
| Malformed source files | Parser crashes | High | Graceful error handling; skip malformed files with logging |
| Native library loading (Tree-sitter) | Platform-specific failures | Medium | Pre-built binaries for Linux/Mac/Windows; container deployment |

---

## Non-Functional Requirements

| NFR | Target | Validation |
|:----|:-------|:-----------|
| Indexing throughput | >10,000 LOC/minute | Benchmark with representative codebase |
| Grammar cold start | <500ms per language | Measure first-parse latency |
| Memory usage | <4GB for 1M LOC repository | Monitor during integration tests |
| Incremental update time | <10% of full index time | Compare timings |

---

## Definition of Done

- [ ] All user stories complete and accepted
- [ ] Unit tests for all parsers (>80% coverage)
- [ ] Integration tests with real Git repositories
- [ ] All 14 languages parsing correctly
- [ ] SSE progress streaming verified end-to-end
- [ ] Incremental indexing working with git diff
- [ ] NFRs validated through performance tests
- [ ] Documentation updated (API docs, configuration guide)
- [ ] Code reviewed and merged to main branch

---

## Open Questions

1. Should we support monorepo-style ingestion (multiple logical repos in one Git repo)?
2. What's the retention policy for old index versions after incremental updates?
3. Should we support branch-specific indexing (index multiple branches of same repo)?

---

**Epic Owner:** TBD  
**Created:** December 2025  
**Last Updated:** December 2025

