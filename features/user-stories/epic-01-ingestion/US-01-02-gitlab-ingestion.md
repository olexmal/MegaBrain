# US-01-02: GitLab Repository Ingestion

## Story
**As a** DevOps engineer  
**I want** to ingest code from GitLab repositories  
**So that** our GitLab-hosted code is searchable in MegaBrain

## Story Points: 3
## Priority: High
## Sprint Target: Sprint 1

---

## Acceptance Criteria

- [x] **AC1:** User can provide GitLab repository URL (gitlab.com or self-hosted)
- [x] **AC2:** System clones repository to temporary storage
- [x] **AC3:** System extracts all source files from the repository
- [x] **AC4:** Repository metadata captured (name, namespace, branch, commit SHA)
- [x] **AC5:** Private repository access works with GitLab token
- [x] **AC6:** Self-hosted GitLab instances supported via base URL config
- [x] **AC7:** Errors handled gracefully with clear messages

---

## Demo Script

### Setup
1. Ensure MegaBrain backend is running
2. Have a test GitLab repository URL ready
3. For self-hosted: have instance URL and access token

### Demo Steps
1. **Trigger Ingestion:** Call the ingestion API with GitLab repository URL
   ```bash
   curl -X POST "http://localhost:8080/api/v1/ingestion/repositories" \
     -H "Content-Type: application/json" \
     -d '{"repositoryUrl": "https://gitlab.com/gitlab-org/gitlab-foss", "branch": "master"}'
   ```
2. **Show Progress:** Display clone progress events
3. **Self-Hosted Demo:** Show configuration for self-hosted instance
4. **Verify Files:** Query database to show extracted files

### Expected Outcome
- GitLab repository successfully cloned
- Works for both gitlab.com and self-hosted
- Metadata visible in database

---

## Technical Tasks

- [x] **T1:** Implement `GitLabSourceControlClient` class (backend)
- [x] **T2:** Create GitLab API integration for metadata (backend)
- [x] **T3:** Add self-hosted GitLab URL configuration (backend)
- [x] **T4:** Implement token-based authentication (backend)
- [x] **T5:** Write unit tests for GitLabSourceControlClient (test)

---

## Test Scenarios

| Scenario | Given | When | Then |
|:---------|:------|:-----|:-----|
| gitlab.com repo | Public gitlab.com URL | Ingestion triggered | Repository cloned |
| Self-hosted repo | Self-hosted URL + token | Ingestion triggered | Repository cloned |
| Invalid token | Private repo + bad token | Ingestion triggered | Auth error returned |

---

## Dependencies

- **Blocked by:** US-01-01 (reuses SourceControlClient interface)
- **Enables:** US-01-04, US-01-05

---

## Risks

| Risk | Impact | Likelihood | Mitigation |
|:-----|:-------|:-----------|:-----------|
| Self-hosted SSL issues | Connection failures | Medium | Allow custom CA certs |
| API version differences | Compatibility issues | Low | Test against multiple versions |

---

## Definition of Ready

- [x] Acceptance criteria clear
- [x] Dependencies identified
- [x] Tech tasks estimated
- [x] Test scenarios defined
- [x] Demo script approved
- [x] No blockers

---

## Definition of Done

- [x] All acceptance criteria met (AC1-AC7)
- [x] All technical tasks completed (T1-T5)
- [x] Unit tests pass with >80% coverage
- [x] Integration with CompositeSourceControlClient verified
- [x] Self-hosted GitLab configuration tested
- [x] Error handling validated for all scenarios
- [x] Documentation updated with correct API endpoints

