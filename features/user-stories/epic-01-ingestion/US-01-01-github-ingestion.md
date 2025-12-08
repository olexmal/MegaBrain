# US-01-01: GitHub Repository Ingestion

## Story
**As a** DevOps engineer  
**I want** to ingest code from GitHub repositories  
**So that** our GitHub-hosted code is searchable in MegaBrain

## Story Points: 5
## Priority: Critical
## Sprint Target: Sprint 1

---

## Acceptance Criteria

- [ ] **AC1:** User can provide GitHub repository URL (e.g., `github.com/owner/repo`)
- [ ] **AC2:** System clones repository to temporary storage
- [ ] **AC3:** System extracts all source files from the repository
- [ ] **AC4:** Repository metadata captured (name, owner, branch, commit SHA)
- [ ] **AC5:** Private repository access works with GitHub token
- [ ] **AC6:** Progress events emitted during clone operation
- [ ] **AC7:** Errors handled gracefully with clear messages

---

## Demo Script

### Setup
1. Ensure MegaBrain backend is running
2. Have a test GitHub repository URL ready (public or private with token)
3. Clear any existing data for clean demo

### Demo Steps
1. **Trigger Ingestion:** Call the ingestion API with GitHub repository URL
   ```bash
   curl -X POST "http://localhost:8080/api/v1/ingest/github" \
     -H "Content-Type: application/json" \
     -d '{"repository": "octocat/Hello-World", "branch": "main"}'
   ```
2. **Show Progress:** Display SSE stream showing clone progress
3. **Verify Files:** Query database to show extracted files
4. **Show Metadata:** Display captured repository metadata
5. **Private Repo (Optional):** Demonstrate with private repo using token

### Expected Outcome
- Repository successfully cloned
- All source files extracted and stored
- Metadata visible in database
- Demo completes in <2 minutes for small repo

---

## Technical Tasks

- [ ] **T1:** Implement `GitHubSourceControlClient` class (backend)
- [ ] **T2:** Create GitHub API integration for metadata fetch (backend)
- [ ] **T3:** Implement repository cloning via JGit (backend)
- [ ] **T4:** Add file extraction from cloned repository (backend)
- [ ] **T5:** Implement token-based authentication (backend)
- [ ] **T6:** Add progress event emission (backend)
- [ ] **T7:** Write unit tests for GitHubSourceControlClient (test)
- [ ] **T8:** Write integration test with real GitHub API (test)

---

## Test Scenarios

| Scenario | Given | When | Then |
|:---------|:------|:-----|:-----|
| Public repo clone | Public GitHub repo URL | Ingestion triggered | Repository cloned successfully |
| Private repo clone | Private repo + valid token | Ingestion triggered | Repository cloned successfully |
| Invalid token | Private repo + invalid token | Ingestion triggered | Clear authentication error |
| Invalid URL | Malformed GitHub URL | Ingestion triggered | Validation error returned |
| Network failure | GitHub unreachable | Ingestion triggered | Timeout error with retry suggestion |
| Large repo | Repo with 10K+ files | Ingestion triggered | Progress shown, completes within timeout |

---

## Dependencies

- **Blocked by:** None (foundation story)
- **Enables:** US-01-04 (Java Parsing), US-01-05 (Tree-sitter Parsing)

---

## Risks

| Risk | Impact | Likelihood | Mitigation |
|:-----|:-------|:-----------|:-----------|
| GitHub API rate limiting | Ingestion blocked | Medium | Implement backoff; prefer clone over API |
| Large repository timeout | Failed ingestion | Low | Streaming clone; configurable timeout |
| Token security exposure | Security breach | Low | Never log tokens; use env vars |

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
- Consider GitHub App authentication for enterprise deployments
- Clone to temp directory, clean up after processing
- Support branch specification (default: main/master)

