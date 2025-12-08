# US-01-03: Bitbucket Repository Ingestion

## Story
**As a** DevOps engineer  
**I want** to ingest code from Bitbucket repositories  
**So that** our Bitbucket-hosted code is searchable in MegaBrain

## Story Points: 3
## Priority: High
## Sprint Target: Sprint 2

---

## Acceptance Criteria

- [x] **AC1:** User can provide Bitbucket Cloud repository URL
- [x] **AC2:** User can provide Bitbucket Server/Data Center URL
- [x] **AC3:** System clones repository to temporary storage
- [ ] **AC4:** System extracts all source files from the repository *(pending .gitignore support; hidden/binary filtered)*
- [x] **AC5:** Repository metadata captured (name, project, branch, commit SHA)
- [x] **AC6:** App password authentication supported (Cloud)
- [x] **AC7:** Personal access token authentication supported (Server)

---

## Demo Script

### Setup
1. Ensure MegaBrain backend is running
2. Have a test Bitbucket repository URL ready
3. Have app password or PAT configured

### Demo Steps
1. **Trigger Ingestion:** Call the ingestion API with Bitbucket repository URL
   ```bash
   curl -X POST "http://localhost:8080/api/v1/ingest/bitbucket" \
     -H "Content-Type: application/json" \
     -d '{"repository": "atlassian/python-bitbucket", "branch": "master"}'
   ```
2. **Show Progress:** Display clone progress events
3. **Verify Files:** Query database to show extracted files

### Expected Outcome
- Bitbucket repository successfully cloned
- Works for both Cloud and Server editions
- Metadata visible in database

---

## Technical Tasks

- [x] **T1:** Implement `BitbucketSourceControlClient` class (backend)
- [x] **T2:** Create Bitbucket Cloud API integration (backend)
- [x] **T3:** Create Bitbucket Server API integration (backend)
- [x] **T4:** Implement authentication for both platforms (backend)
- [x] **T5:** Write unit tests for BitbucketSourceControlClient (test)

---

## Test Scenarios

| Scenario | Given | When | Then |
|:---------|:------|:-----|:-----|
| Cloud public repo | bitbucket.org URL | Ingestion triggered | Repository cloned |
| Cloud private repo | URL + app password | Ingestion triggered | Repository cloned |
| Server repo | Server URL + PAT | Ingestion triggered | Repository cloned |

---

## Dependencies

- **Blocked by:** US-01-01 (reuses SourceControlClient interface)
- **Enables:** US-01-04, US-01-05

---

## Risks

| Risk | Impact | Likelihood | Mitigation |
|:-----|:-------|:-----------|:-----------|
| Cloud/Server API differences | Compatibility issues | Medium | Abstract behind common interface |
| Workspace/Project confusion | User errors | Low | Clear documentation |

---

## Definition of Ready

- [x] Acceptance criteria clear
- [x] Dependencies identified
- [x] Tech tasks estimated
- [x] Test scenarios defined
- [x] Demo script approved
- [x] No blockers

