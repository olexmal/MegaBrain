# US-04-05: CLI Search Command

## Story
**As a** developer  
**I want** to search from the command line  
**So that** I can quickly find code without leaving my terminal

## Story Points: 2
## Priority: Medium
## Sprint Target: Sprint 4

---

## Acceptance Criteria

- [ ] **AC1:** Command: `megabrain search "query string"`
- [ ] **AC2:** Supports: `--language`, `--repo`, `--type`, `--limit`
- [ ] **AC3:** Results: file path, entity name, code snippet
- [ ] **AC4:** Syntax highlighting for snippets
- [ ] **AC5:** Output formats: human-readable (default), JSON (`--json`)
- [ ] **AC6:** Pipe-friendly with `--quiet`

---

## Demo Script

### Setup
1. CLI and indexed data available

### Demo Steps
1. **Basic Search:**
   ```bash
   megabrain search "authentication"
   ```
2. **With Filters:**
   ```bash
   megabrain search "service" --language java --limit 5
   ```
3. **JSON Output:**
   ```bash
   megabrain search "user" --json | jq '.results[0]'
   ```
4. **Syntax Highlighting:** Show colored output

### Expected Outcome
- Results displayed nicely
- Filters work
- JSON mode for scripting

---

## Technical Tasks

- [ ] **T1:** Create `SearchCommand` Picocli class (backend)
- [ ] **T2:** Add filter options (backend)
- [ ] **T3:** Implement result formatting (backend)
- [ ] **T4:** Add syntax highlighting (backend)
- [ ] **T5:** Add JSON output mode (backend)
- [ ] **T6:** Write command tests (test)

---

## Dependencies

- **Blocked by:** US-04-02 (search API)
- **Enables:** Developer CLI workflow

---

## Definition of Ready

- [x] Acceptance criteria clear
- [x] Dependencies identified
- [x] Tech tasks estimated
- [x] Test scenarios defined
- [x] Demo script approved
- [x] No blockers

