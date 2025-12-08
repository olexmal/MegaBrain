# US-05-04: Code Preview Panel

## Story
**As a** developer  
**I want** to preview code in context from search results  
**So that** I can understand code without leaving the dashboard

## Story Points: 3
## Priority: High
## Sprint Target: Sprint 5

---

## Acceptance Criteria

- [ ] **AC1:** Click search result opens preview panel
- [ ] **AC2:** Full file content with matched section highlighted
- [ ] **AC3:** Syntax highlighting for all languages
- [ ] **AC4:** Line numbers displayed
- [ ] **AC5:** Copy code button
- [ ] **AC6:** Link to source repository

---

## Demo Script

### Demo Steps
1. **Search:** Find a result
2. **Click Result:** Open preview panel
3. **Show Context:** Full file with highlight
4. **Copy Code:** Use copy button
5. **Open Source:** Link to GitHub

### Expected Outcome
- Code preview is readable
- Context helps understanding
- Actions work

---

## Technical Tasks

- [ ] **T1:** Create code preview component (frontend)
- [ ] **T2:** Integrate syntax highlighting library (frontend)
- [ ] **T3:** Add line numbers display (frontend)
- [ ] **T4:** Implement copy to clipboard (frontend)
- [ ] **T5:** Add source repository link (frontend)
- [ ] **T6:** Write component tests (test)

---

## Dependencies

- **Blocked by:** US-05-03 (search interface)
- **Enables:** Better code exploration

---

## Definition of Ready

- [x] All criteria met

