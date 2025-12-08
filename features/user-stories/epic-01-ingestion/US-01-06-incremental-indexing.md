# US-01-06: Incremental Git-Diff Indexing

## Story
**As a** system administrator  
**I want** incremental indexing based on git diff  
**So that** daily updates are fast and don't require full re-indexing

## Story Points: 5
## Priority: High
## Sprint Target: Sprint 3

---

## Acceptance Criteria

- [ ] **AC1:** Full index rebuild supported for initial ingestion
- [ ] **AC2:** Incremental mode detects changed files via `git diff`
- [ ] **AC3:** Only modified/added files are re-parsed and re-indexed
- [ ] **AC4:** Deleted files are removed from the index
- [ ] **AC5:** Renamed files handled correctly (delete old + add new)
- [ ] **AC6:** Last indexed commit SHA stored per repository
- [ ] **AC7:** Incremental takes <10% time of full index
- [ ] **AC8:** Manual full re-index can be forced

---

## Demo Script

### Setup
1. Ingest a repository (full index)
2. Make changes to the repository (add, modify, delete files)
3. Commit the changes

### Demo Steps
1. **Show Initial Index:** Display indexed file count and commit SHA
2. **Make Changes:** Show git log with recent commits
3. **Trigger Incremental:** Run incremental indexing
   ```bash
   curl -X POST "http://localhost:8080/api/v1/ingest/github" \
     -d '{"repository": "...", "incremental": true}'
   ```
4. **Show Diff:** Display which files were processed
5. **Compare Times:** Show full vs incremental duration
6. **Verify Deletions:** Confirm deleted files removed from index

### Expected Outcome
- Only changed files processed
- Incremental completes in fraction of full time
- Index remains consistent

---

## Technical Tasks

- [ ] **T1:** Store last indexed commit SHA per repository (backend)
- [ ] **T2:** Implement git diff detection for changed files (backend)
- [ ] **T3:** Handle file additions in incremental mode (backend)
- [ ] **T4:** Handle file modifications in incremental mode (backend)
- [ ] **T5:** Handle file deletions in incremental mode (backend)
- [ ] **T6:** Handle file renames in incremental mode (backend)
- [ ] **T7:** Add `incremental` flag to ingestion API (backend)
- [ ] **T8:** Write tests for each change type (test)

---

## Test Scenarios

| Scenario | Given | When | Then |
|:---------|:------|:-----|:-----|
| New file added | Repo with new file since last index | Incremental run | Only new file indexed |
| File modified | Repo with modified file | Incremental run | File re-indexed with new content |
| File deleted | Repo with deleted file | Incremental run | File removed from index |
| File renamed | Repo with renamed file | Incremental run | Old removed, new added |
| No changes | Repo unchanged | Incremental run | No processing, quick return |
| Force full | Repo unchanged + force flag | Full index | All files re-indexed |

---

## Dependencies

- **Blocked by:** US-01-01 (needs initial index capability)
- **Enables:** Scheduled indexing (future)

---

## Risks

| Risk | Impact | Likelihood | Mitigation |
|:-----|:-------|:-----------|:-----------|
| Corrupt state after failed incremental | Inconsistent index | Medium | Transaction rollback; full re-index option |
| Large number of changes | Slow incremental | Low | Threshold to trigger full re-index |
| Branch switching | Unexpected diffs | Medium | Track branch with commit SHA |

---

## Definition of Ready

- [x] Acceptance criteria clear
- [x] Dependencies identified
- [x] Tech tasks estimated
- [x] Test scenarios defined
- [x] Demo script approved
- [x] No blockers

