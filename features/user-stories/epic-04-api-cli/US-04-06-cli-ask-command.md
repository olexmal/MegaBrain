# US-04-06: CLI Ask Command

## Story
**As a** developer  
**I want** to ask questions from the command line  
**So that** I can get answers without context switching

## Story Points: 2
## Priority: Medium
## Sprint Target: Sprint 5

---

## Acceptance Criteria

- [ ] **AC1:** Command: `megabrain ask "question"`
- [ ] **AC2:** Streaming output shows answer as generated
- [ ] **AC3:** Sources displayed after answer
- [ ] **AC4:** Supports: `--model`, `--context-limit`
- [ ] **AC5:** Non-streaming mode: `--no-stream`
- [ ] **AC6:** Markdown rendering (optional)

---

## Demo Script

### Setup
1. CLI and RAG pipeline available

### Demo Steps
1. **Ask Question:**
   ```bash
   megabrain ask "How does the authentication service work?"
   ```
2. **Show Streaming:** Answer appearing word by word
3. **Show Sources:** Sources listed after answer
4. **Model Selection:**
   ```bash
   megabrain ask "Explain caching" --model gpt-4
   ```

### Expected Outcome
- Streaming answer in terminal
- Sources shown
- Model selection works

---

## Technical Tasks

- [ ] **T1:** Create `AskCommand` Picocli class (backend)
- [ ] **T2:** Implement streaming terminal output (backend)
- [ ] **T3:** Display sources after answer (backend)
- [ ] **T4:** Add model option (backend)
- [ ] **T5:** Add non-streaming mode (backend)
- [ ] **T6:** Write command tests (test)

---

## Dependencies

- **Blocked by:** US-04-03 (RAG API)
- **Enables:** Developer CLI workflow

---

## Definition of Ready

- [x] Acceptance criteria clear
- [x] Dependencies identified
- [x] Tech tasks estimated
- [x] Test scenarios defined
- [x] Demo script approved
- [x] No blockers

