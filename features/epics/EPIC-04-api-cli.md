# EPIC-04: REST API & CLI

## Epic Overview

| Attribute | Value |
|:----------|:------|
| **Epic ID** | EPIC-04 |
| **Priority** | High |
| **Estimated Scope** | M |
| **Dependencies** | EPIC-01 (Ingestion), EPIC-02 (Search), EPIC-03 (RAG) |
| **Spec Reference** | Section 4.4 (FR-IFC-01, FR-IFC-02) |
| **Status** | Planned |

## Business Value

This epic exposes MegaBrain's capabilities through programmatic interfaces:

- **REST API:** Enables integration with other tools, CI/CD pipelines, and custom applications
- **CLI:** Provides automation, scripting, and headless operation for power users and DevOps

These interfaces are the foundation for all external interactions with MegaBrain, including the Web Dashboard (EPIC-05) and MCP Server (EPIC-08).

---

## User Stories

### US-04-01: Ingestion REST Endpoint

**As a** DevOps engineer, **I want** to trigger code ingestion via REST API, **so that** I can automate indexing in CI/CD pipelines.

**Acceptance Criteria:**
- [ ] `POST /api/v1/ingest/{source}` initiates ingestion
- [ ] Request body accepts: repository URL, branch, credentials reference
- [ ] Response is SSE stream with progress events
- [ ] Source parameter supports: `github`, `gitlab`, `bitbucket`, `local`
- [ ] Authentication required (API key or token)
- [ ] Concurrent ingestion requests handled gracefully

**Spec Reference:** FR-IFC-01

---

### US-04-02: Search REST Endpoint

**As a** developer, **I want** to search code via REST API, **so that** I can build custom integrations and tools.

**Acceptance Criteria:**
- [ ] `GET /api/v1/search` accepts query parameters
- [ ] Parameters: `q` (query), `language`, `repository`, `entity_type`, `limit`, `transitive`
- [ ] Response includes: chunks with metadata, relevance scores, total count
- [ ] Pagination support via `offset` and `limit`
- [ ] Response format: JSON
- [ ] Query latency <500ms for 95th percentile

**Spec Reference:** FR-IFC-01

---

### US-04-03: RAG REST Endpoint

**As a** developer, **I want** to ask questions via REST API, **so that** I can integrate code Q&A into my workflow.

**Acceptance Criteria:**
- [ ] `POST /api/v1/rag` accepts question in request body
- [ ] Request body: `{ "question": "...", "context_limit": N }`
- [ ] Response is SSE stream of tokens
- [ ] Final response includes source attributions
- [ ] Option for non-streaming response (wait for complete answer)
- [ ] First token within 2s

**Spec Reference:** FR-IFC-01

---

### US-04-04: CLI Ingest Command

**As a** DevOps engineer, **I want** to ingest repositories from the command line, **so that** I can script and automate indexing.

**Acceptance Criteria:**
- [ ] Command: `megabrain ingest --source github --repo owner/repo`
- [ ] Supports: `--branch`, `--credentials`, `--incremental`
- [ ] Progress displayed in terminal (spinner or progress bar)
- [ ] Exit code indicates success (0) or failure (non-zero)
- [ ] Verbose mode with `--verbose` flag
- [ ] Help text with `--help`

**Spec Reference:** FR-IFC-02

---

### US-04-05: CLI Search Command

**As a** developer, **I want** to search from the command line, **so that** I can quickly find code without leaving my terminal.

**Acceptance Criteria:**
- [ ] Command: `megabrain search "query string"`
- [ ] Supports: `--language`, `--repo`, `--type`, `--limit`
- [ ] Results displayed with file path, entity name, snippet
- [ ] Syntax highlighting for code snippets
- [ ] Output formats: human-readable (default), JSON (`--json`)
- [ ] Pipe-friendly output with `--quiet`

**Spec Reference:** FR-IFC-02

---

### US-04-06: CLI Ask Command

**As a** developer, **I want** to ask questions from the command line, **so that** I can get answers without context switching.

**Acceptance Criteria:**
- [ ] Command: `megabrain ask "How is authentication implemented?"`
- [ ] Streaming output shows answer as it generates
- [ ] Sources displayed after answer
- [ ] Supports: `--model`, `--context-limit`
- [ ] Non-streaming mode with `--no-stream`
- [ ] Markdown rendering in terminal (optional)

**Spec Reference:** FR-IFC-02

---

### US-04-07: API Authentication

**As a** security administrator, **I want** API endpoints protected by authentication, **so that** unauthorized access is prevented.

**Acceptance Criteria:**
- [ ] API key authentication supported
- [ ] API keys managed via configuration
- [ ] Unauthorized requests return 401
- [ ] Rate limiting per API key
- [ ] API key rotation supported
- [ ] Audit logging of API access

**Spec Reference:** NFR - Privacy & Security

---

## Technical Notes

### Key Components
- **`IngestionResource`:** JAX-RS endpoint for ingestion operations
- **`SearchResource`:** JAX-RS endpoint for search operations
- **Picocli Commands:** CLI command implementations

### Technology Stack
| Component | Technology |
|:----------|:-----------|
| REST Framework | Quarkus RESTEasy Reactive |
| CLI Framework | Picocli with Quarkus Integration |
| Serialization | Jackson (JSON) |
| Streaming | Mutiny Multi → SSE |
| Authentication | API Key / Bearer Token |

### API Response Formats

**Search Response:**
```json
{
  "query": "authentication",
  "total": 42,
  "results": [
    {
      "id": "chunk-123",
      "entity_name": "AuthService",
      "entity_type": "class",
      "file_path": "src/auth/AuthService.java",
      "repository": "backend",
      "language": "java",
      "snippet": "public class AuthService { ... }",
      "score": 0.95,
      "line_start": 10,
      "line_end": 50
    }
  ]
}
```

**RAG SSE Stream:**
```
event: token
data: {"token": "The"}

event: token
data: {"token": " authentication"}

event: sources
data: {"sources": [{"file": "...", "entity": "..."}]}

event: done
data: {}
```

### CLI Architecture
- Native executable via GraalVM for fast startup
- Configuration via `~/.megabrain/config.yaml` or env vars
- Server URL configurable for remote MegaBrain instances

---

## Risks & Mitigations

| Risk | Impact | Likelihood | Mitigation |
|:-----|:-------|:-----------|:-----------|
| SSE connection drops | Lost progress/tokens | Medium | Reconnection logic; idempotent operations |
| API key compromise | Unauthorized access | Low | Key rotation; IP allowlists; audit logging |
| CLI native image issues | Platform compatibility | Medium | Fallback to JVM mode; thorough testing |
| Rate limiting too aggressive | Blocks legitimate use | Low | Configurable limits; burst allowance |

---

## Non-Functional Requirements

| NFR | Target | Validation |
|:----|:-------|:-----------|
| API response time (search) | <500ms p95 | Load testing |
| CLI startup time (native) | <100ms | Timing measurements |
| Concurrent API connections | 100+ | Load testing |
| API availability | 99.9% uptime | Monitoring |

---

## Definition of Done

- [ ] All REST endpoints implemented and documented
- [ ] All CLI commands implemented with help text
- [ ] OpenAPI spec generated and accurate
- [ ] API authentication working
- [ ] SSE streaming tested end-to-end
- [ ] CLI native image builds successfully
- [ ] Unit tests (>80% coverage)
- [ ] Integration tests for all endpoints
- [ ] API documentation published
- [ ] CLI man pages / help complete

---

## Open Questions

1. Should we support WebSocket as an alternative to SSE?
2. Do we need API versioning strategy beyond `/v1/`?
3. Should CLI support interactive mode (REPL)?
4. How do we handle API backwards compatibility?

---

**Epic Owner:** TBD  
**Created:** December 2025  
**Last Updated:** December 2025

