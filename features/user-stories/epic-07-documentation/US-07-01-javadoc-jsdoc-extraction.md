# US-07-01: Javadoc/JSDoc Extraction

## Story
**As a** developer  
**I want** Javadoc and JSDoc comments extracted and searchable  
**So that** API documentation is included in search results

## Story Points: 5
## Priority: High
## Sprint Target: Sprint 3

---

## Acceptance Criteria

- [ ] **AC1:** Javadoc parsed for Java classes/methods
- [ ] **AC2:** JSDoc parsed for JavaScript/TypeScript
- [ ] **AC3:** Extract: description, @param, @return, @throws
- [ ] **AC4:** Documentation linked to code entity
- [ ] **AC5:** Documentation stored as separate searchable field
- [ ] **AC6:** Handle missing documentation gracefully

---

## Demo Script

### Demo Steps
1. **Ingest Repository:** Index a documented Java/JS project
2. **Search for Docs:** Search "validates user input"
3. **Show Docs:** Display Javadoc content in results
4. **Entity Link:** Show docs linked to method

### Expected Outcome
- Documentation visible in search results
- Params and returns extracted
- Links to code entities

---

## Technical Tasks

- [ ] **T1:** Implement Javadoc parser (backend)
- [ ] **T2:** Implement JSDoc parser (backend)
- [ ] **T3:** Create `DocCommentParser` interface (backend)
- [ ] **T4:** Link docs to code entities (backend)
- [ ] **T5:** Add doc fields to index schema (backend)
- [ ] **T6:** Write tests for various doc formats (test)

---

## Dependencies

- **Blocked by:** US-01-04 (Java parsing)
- **Enables:** US-07-03 (documentation indexing)

---

## Definition of Ready

- [x] All criteria met

