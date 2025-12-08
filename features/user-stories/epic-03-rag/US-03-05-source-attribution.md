# US-03-05: Source Attribution in Answers

## Story
**As a** developer  
**I want** answers to cite specific source files and entities  
**So that** I can verify the answer and explore further

## Story Points: 3
## Priority: High
## Sprint Target: Sprint 5

---

## Acceptance Criteria

- [ ] **AC1:** Each answer includes list of source chunks used
- [ ] **AC2:** Sources include: file path, entity name, line numbers
- [ ] **AC3:** LLM instructed to reference sources in answer text
- [ ] **AC4:** Inline citations: `[Source: path/to/file.java:42]`
- [ ] **AC5:** Source relevance scores included
- [ ] **AC6:** Sources clickable/linkable in UI (data provided)

---

## Demo Script

### Setup
1. RAG pipeline fully working
2. Question that references multiple code areas

### Demo Steps
1. **Ask Question:** Submit question about authentication
2. **Show Answer:** Display answer with inline citations
   ```
   The authentication is handled by the AuthService class 
   [Source: src/auth/AuthService.java:25]. It validates 
   credentials using the UserRepository [Source: src/data/UserRepository.java:42].
   ```
3. **Show Source List:** Display full source metadata
   ```json
   {
     "sources": [
       {
         "file": "src/auth/AuthService.java",
         "entity": "AuthService.authenticate",
         "lines": [25, 45],
         "score": 0.92
       }
     ]
   }
   ```
4. **Verify Citations:** Check that cited code is accurate

### Expected Outcome
- Inline citations in answer text
- Full source list with metadata
- Citations are accurate

---

## Technical Tasks

- [ ] **T1:** Update prompt template to require citations (backend)
- [ ] **T2:** Parse LLM response for citation extraction (backend)
- [ ] **T3:** Attach source metadata to response (backend)
- [ ] **T4:** Create source DTO with all metadata (backend)
- [ ] **T5:** Write tests for citation parsing (test)

---

## Test Scenarios

| Scenario | Given | When | Then |
|:---------|:------|:-----|:-----|
| Single source | One relevant chunk | Ask question | One citation in answer |
| Multiple sources | Many relevant chunks | Ask question | Multiple citations |
| Citation format | Answer generated | Check format | Matches expected format |
| Source accuracy | Citations present | Verify | Citations match context |

---

## Dependencies

- **Blocked by:** US-03-03 (prompt construction)
- **Enables:** US-05-05 (Chat UI links to sources)

---

## Risks

| Risk | Impact | Likelihood | Mitigation |
|:-----|:-------|:-----------|:-----------|
| LLM ignores citation instructions | No citations | Medium | Strong prompt; validation |
| Incorrect citations | Misleading | Low | Post-processing validation |

---

## Definition of Ready

- [x] Acceptance criteria clear
- [x] Dependencies identified
- [x] Tech tasks estimated
- [x] Test scenarios defined
- [x] Demo script approved
- [x] No blockers

