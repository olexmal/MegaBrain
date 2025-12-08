# US-03-03: Context-Aware Prompt Construction

## Story
**As a** system  
**I want** well-designed prompts that maximize answer quality  
**So that** users get accurate, helpful responses

## Story Points: 5
## Priority: Critical
## Sprint Target: Sprint 4

---

## Acceptance Criteria

- [ ] **AC1:** System prompt establishes role and constraints
- [ ] **AC2:** Context window efficiently utilized
- [ ] **AC3:** Code chunks clearly delineated in prompt
- [ ] **AC4:** Instructions to cite sources included
- [ ] **AC5:** Instructions to admit uncertainty when context insufficient
- [ ] **AC6:** Prompt templates configurable

---

## Demo Script

### Setup
1. Have search results ready for a question
2. LLM integration working

### Demo Steps
1. **Show Prompt Template:** Display the system prompt structure
2. **Show Context Formatting:** How code chunks are formatted
   ```
   [Source: src/auth/AuthService.java - AuthService.login()]
   public boolean login(String user, String pass) {
     // ... code ...
   }
   ```
3. **Ask Question:** Submit question and trace prompt construction
4. **Show Citations:** Verify answer cites sources
5. **Uncertainty Handling:** Ask question with no relevant context

### Expected Outcome
- Prompt is well-structured
- Citations appear in answers
- Uncertainty handled gracefully

---

## Technical Tasks

- [ ] **T1:** Design prompt template structure (backend)
- [ ] **T2:** Implement context formatter for code chunks (backend)
- [ ] **T3:** Implement token counting for context window (backend)
- [ ] **T4:** Add chunk selection based on relevance and token budget (backend)
- [ ] **T5:** Create configurable prompt template system (backend)
- [ ] **T6:** Write tests for prompt construction (test)

---

## Test Scenarios

| Scenario | Given | When | Then |
|:---------|:------|:-----|:-----|
| Standard prompt | 5 relevant chunks | Build prompt | All chunks in context |
| Token limit | Too many chunks | Build prompt | Top chunks selected |
| Source formatting | Java chunk | Format | Clear source citation |
| Empty context | No relevant chunks | Build prompt | Uncertainty instruction |

---

## Dependencies

- **Blocked by:** US-02-03 (search provides chunks)
- **Enables:** US-03-04 (streaming), US-03-05 (attribution)

---

## Risks

| Risk | Impact | Likelihood | Mitigation |
|:-----|:-------|:-----------|:-----------|
| Token overflow | Truncated context | Medium | Token counting; chunk selection |
| Poor prompt design | Bad answers | Medium | Iterate on prompts; A/B testing |

---

## Definition of Ready

- [x] Acceptance criteria clear
- [x] Dependencies identified
- [x] Tech tasks estimated
- [x] Test scenarios defined
- [x] Demo script approved
- [x] No blockers

