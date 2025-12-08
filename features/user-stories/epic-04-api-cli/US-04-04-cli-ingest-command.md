# US-04-04: CLI Ingest Command

## Story
**As a** DevOps engineer  
**I want** to ingest repositories from the command line  
**So that** I can script and automate indexing

## Story Points: 3
## Priority: High
## Sprint Target: Sprint 3

---

## Acceptance Criteria

- [ ] **AC1:** Command: `megabrain ingest --source github --repo owner/repo`
- [ ] **AC2:** Supports: `--branch`, `--token`, `--incremental`
- [ ] **AC3:** Progress displayed in terminal (progress bar)
- [ ] **AC4:** Exit code: 0 (success), non-zero (failure)
- [ ] **AC5:** Verbose mode with `--verbose`
- [ ] **AC6:** Help text with `--help`

---

## Demo Script

### Setup
1. CLI binary available
2. MegaBrain server running (or embedded)

### Demo Steps
1. **Show Help:**
   ```bash
   megabrain ingest --help
   ```
2. **Ingest Repository:**
   ```bash
   megabrain ingest --source github --repo octocat/Hello-World
   ```
3. **Show Progress:** Progress bar updating
4. **Incremental:**
   ```bash
   megabrain ingest --source github --repo octocat/Hello-World --incremental
   ```
5. **Check Exit Code:** `echo $?`

### Expected Outcome
- Progress visible in terminal
- Exit code indicates success/failure
- Help is comprehensive

---

## Technical Tasks

- [ ] **T1:** Create `IngestCommand` Picocli class (backend)
- [ ] **T2:** Add source, repo, branch options (backend)
- [ ] **T3:** Implement progress display (backend)
- [ ] **T4:** Handle exit codes (backend)
- [ ] **T5:** Add verbose logging option (backend)
- [ ] **T6:** Write command tests (test)

---

## Dependencies

- **Blocked by:** US-01-01 (ingestion service)
- **Enables:** CI/CD automation

---

## Definition of Ready

- [x] Acceptance criteria clear
- [x] Dependencies identified
- [x] Tech tasks estimated
- [x] Test scenarios defined
- [x] Demo script approved
- [x] No blockers

