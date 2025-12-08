# US-05-01: Ingestion Dashboard View

## Story
**As a** system administrator  
**I want** to monitor ingestion jobs in real-time  
**So that** I can track progress and identify issues

## Story Points: 5
## Priority: High
## Sprint Target: Sprint 4

---

## Acceptance Criteria

- [ ] **AC1:** Dashboard shows list of active and recent ingestion jobs
- [ ] **AC2:** Each job displays: repository, status, progress %, duration
- [ ] **AC3:** Real-time progress updates via SSE
- [ ] **AC4:** Visual progress bar for each job
- [ ] **AC5:** Error details displayed for failed jobs
- [ ] **AC6:** Job history with filtering by status/date

---

## Demo Script

### Setup
1. Angular app running
2. Backend with SSE endpoints ready

### Demo Steps
1. **Open Dashboard:** Navigate to ingestion view
2. **Start Job:** Trigger ingestion (or have one running)
3. **Show Progress:** Watch progress bar update
4. **Show History:** Display completed/failed jobs
5. **Error Details:** Click failed job to see error

### Expected Outcome
- Live progress updates visible
- Job history accessible
- Errors clearly displayed

---

## Technical Tasks

- [ ] **T1:** Create ingestion dashboard component (frontend)
- [ ] **T2:** Create job list component (frontend)
- [ ] **T3:** Implement SSE service for progress (frontend)
- [ ] **T4:** Create progress bar component (frontend)
- [ ] **T5:** Add job history view with filters (frontend)
- [ ] **T6:** Write component tests (test)

---

## Dependencies

- **Blocked by:** US-04-01 (ingestion API with SSE)
- **Enables:** US-05-02 (trigger form)

---

## Definition of Ready

- [x] Acceptance criteria clear
- [x] Dependencies identified
- [x] Tech tasks estimated
- [x] Test scenarios defined
- [x] Demo script approved
- [x] No blockers

