# US-07-06: Code Example Extraction

## Story
**As a** developer  
**I want** code examples from docs indexed separately  
**So that** I can search for usage examples

## Story Points: 3
## Priority: Medium
## Sprint Target: Sprint 6

---

## Acceptance Criteria

- [ ] **AC1:** Extract code blocks from @example tags
- [ ] **AC2:** Extract code blocks from docstring examples
- [ ] **AC3:** Examples stored as separate chunks
- [ ] **AC4:** Link examples to documented entity
- [ ] **AC5:** Search filter for examples only

---

## Demo Script

### Demo Steps
1. **Search Examples:**
   ```bash
   curl "http://localhost:8080/api/v1/search?q=HTTP+request&examples_only=true"
   ```
2. **Show Examples:** Display code examples
3. **Entity Link:** Show linked documentation

### Expected Outcome
- Examples searchable separately
- Linked to documentation

---

## Technical Tasks

- [ ] **T1:** Extract @example from Javadoc/JSDoc (backend)
- [ ] **T2:** Extract doctest from Python (backend)
- [ ] **T3:** Create example chunk type (backend)
- [ ] **T4:** Link examples to entities (backend)
- [ ] **T5:** Add examples_only filter (backend)
- [ ] **T6:** Write tests (test)

---

## Dependencies

- **Blocked by:** US-07-01, US-07-02 (doc extraction)
- **Enables:** Better example discovery

---

## Definition of Ready

- [x] All criteria met

