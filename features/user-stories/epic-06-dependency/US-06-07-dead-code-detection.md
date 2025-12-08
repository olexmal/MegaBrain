# US-06-07: Dead Code Detection

## Story
**As a** tech lead  
**I want** to identify unused code  
**So that** we can reduce maintenance burden

## Story Points: 5
## Priority: Medium
## Sprint Target: Sprint 6

---

## Acceptance Criteria

- [ ] **AC1:** Identify classes with no incoming references
- [ ] **AC2:** Identify methods with no callers
- [ ] **AC3:** Exclude entry points (main, tests, frameworks)
- [ ] **AC4:** Configurable exclusion patterns
- [ ] **AC5:** Report with confidence scores
- [ ] **AC6:** Export list for review

---

## Demo Script

### Demo Steps
1. **Run Detection:**
   ```bash
   curl "http://localhost:8080/api/v1/graph/dead-code?repository=olexmal/MegaBrain"
   ```
2. **Show Results:** List unused classes/methods
3. **Exclusions:** Show configured exclusion patterns
4. **Export:** Download CSV of candidates

### Expected Outcome
- Unused code identified
- False positives minimized
- Exportable report

---

## Technical Tasks

- [ ] **T1:** Implement dead code query (backend)
- [ ] **T2:** Add entry point detection (backend)
- [ ] **T3:** Create exclusion pattern configuration (backend)
- [ ] **T4:** Add confidence scoring (backend)
- [ ] **T5:** Create export functionality (backend)
- [ ] **T6:** Write tests with known dead code (test)

---

## Dependencies

- **Blocked by:** US-06-03 (incoming queries)
- **Enables:** Code quality improvement

---

## Definition of Ready

- [x] All criteria met

