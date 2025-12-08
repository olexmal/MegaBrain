# US-05-02: Trigger Ingestion Form

## Story
**As a** user  
**I want** to start ingestion from the dashboard  
**So that** I don't need CLI or API to index repos

## Story Points: 3
## Priority: High
## Sprint Target: Sprint 4

---

## Acceptance Criteria

- [ ] **AC1:** "New Ingestion" button opens form
- [ ] **AC2:** Form fields: source type, repository URL, branch
- [ ] **AC3:** Source type dropdown: GitHub, GitLab, Bitbucket
- [ ] **AC4:** Validation before submission
- [ ] **AC5:** Job appears in dashboard immediately
- [ ] **AC6:** Helpful error messages

---

## Demo Script

### Demo Steps
1. **Open Form:** Click "New Ingestion" button
2. **Fill Form:** Select GitHub, enter `olexmal/MegaBrain`
3. **Submit:** Click submit and watch job appear
4. **Validation:** Show validation errors for bad input

### Expected Outcome
- Form is intuitive
- Job starts successfully
- Validation prevents errors

---

## Technical Tasks

- [ ] **T1:** Create ingestion form component (frontend)
- [ ] **T2:** Add form validation (frontend)
- [ ] **T3:** Integrate with ingestion API (frontend)
- [ ] **T4:** Add source type selector (frontend)
- [ ] **T5:** Write form tests (test)

---

## Dependencies

- **Blocked by:** US-05-01 (dashboard exists)
- **Enables:** Self-service ingestion

---

## Definition of Ready

- [x] All criteria met

