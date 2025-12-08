# US-01-08: Dynamic Grammar Management

## Story
**As a** system administrator  
**I want** Tree-sitter grammars managed automatically  
**So that** I don't need to manually bundle grammars for each language

## Story Points: 5
## Priority: High
## Sprint Target: Sprint 3

---

## Acceptance Criteria

- [ ] **AC1:** `ParserRegistry` dynamically loads grammars based on file extension
- [ ] **AC2:** `GrammarManager` downloads grammars from Tree-sitter repositories
- [ ] **AC3:** Downloaded grammars cached locally in configurable directory
- [ ] **AC4:** Version pinning via configuration prevents unexpected updates
- [ ] **AC5:** Grammar loading time <500ms per language (cold start)
- [ ] **AC6:** Rollback capability if new grammar causes issues
- [ ] **AC7:** Health check endpoint verifies all required grammars loaded
- [ ] **AC8:** Support for 8 additional languages: Go, Rust, Kotlin, Ruby, Scala, Swift, PHP, C#

---

## Demo Script

### Setup
1. Start MegaBrain without pre-bundled grammars
2. Ensure network access to grammar repositories

### Demo Steps
1. **Show Empty Cache:** Display empty grammar cache directory
2. **Trigger Ingestion:** Ingest a Go or Rust repository
3. **Show Download:** Watch grammar being downloaded
4. **Show Cache:** Display cached grammar files
5. **Re-Ingest:** Show fast loading from cache (no download)
6. **Health Check:** Call health endpoint to verify grammars
   ```bash
   curl http://localhost:8080/q/health/ready
   ```
7. **Version Pin:** Show configuration for version pinning

### Expected Outcome
- Grammars downloaded on first use
- Subsequent loads fast from cache
- Health check confirms all grammars ready

---

## Technical Tasks

- [ ] **T1:** Implement `ParserRegistry` class (backend)
- [ ] **T2:** Implement `GrammarManager` class (backend)
- [ ] **T3:** Create grammar download logic from GitHub releases (backend)
- [ ] **T4:** Implement local caching with version tracking (backend)
- [ ] **T5:** Add version pinning configuration (backend)
- [ ] **T6:** Add rollback/downgrade capability (backend)
- [ ] **T7:** Create health check for grammar status (backend)
- [ ] **T8:** Add grammars: Go, Rust, Kotlin, Ruby (backend)
- [ ] **T9:** Add grammars: Scala, Swift, PHP, C# (backend)
- [ ] **T10:** Write unit tests for grammar management (test)

---

## Test Scenarios

| Scenario | Given | When | Then |
|:---------|:------|:-----|:-----|
| First load | No cached grammar | Parse Go file | Grammar downloaded, file parsed |
| Cached load | Grammar in cache | Parse Go file | Grammar loaded from cache, fast |
| Version pin | Specific version configured | Download triggered | Exact version downloaded |
| Rollback | Bad grammar version | Rollback requested | Previous version restored |
| Health check | All grammars loaded | Health endpoint called | Reports healthy |
| Missing grammar | Unknown file extension | Parse file | Graceful skip with warning |
| Offline mode | No network, grammar cached | Parse file | Works from cache |

---

## Dependencies

- **Blocked by:** US-01-05 (base Tree-sitter implementation)
- **Enables:** Extended language support for all projects

---

## Risks

| Risk | Impact | Likelihood | Mitigation |
|:-----|:-------|:-----------|:-----------|
| Grammar download fails | Language not supported | Medium | Bundle critical grammars; retry logic |
| Native binary compatibility | Platform issues | Medium | Pre-built for Linux/Mac/Windows |
| Grammar ABI changes | Parsing failures | Low | Pin versions; test before update |

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
- Grammar source: https://github.com/tree-sitter
- Cache directory: `~/.megabrain/grammars/` or configurable
- Consider bundling essential grammars in Docker image

