# US-07-04: Coverage Metrics Dashboard

## Story
**As a** tech lead  
**I want** to see documentation coverage metrics  
**So that** I can improve documentation quality

## Story Points: 5
## Priority: High
## Sprint Target: Sprint 5

---

## Acceptance Criteria

- [ ] **AC1:** Coverage % calculated (entities with docs / total)
- [ ] **AC2:** Breakdown by: repository, language, entity type
- [ ] **AC3:** List undocumented public entities
- [ ] **AC4:** Historical trend tracking
- [ ] **AC5:** Export report as CSV/JSON

---

## Demo Script

### Demo Steps
1. **View Metrics:**
   ```bash
   curl "http://localhost:8080/api/v1/docs/coverage?repository=backend"
   ```
2. **Show Breakdown:** By language, by entity type
3. **Undocumented List:** Show undocumented public methods
4. **Export:** Download CSV report

### Expected Outcome
- Coverage metrics visible
- Actionable undocumented list

---

## Technical Tasks

- [ ] **T1:** Create `DocumentationQualityAnalyzer` (backend)
- [ ] **T2:** Calculate coverage metrics (backend)
- [ ] **T3:** Identify undocumented public entities (backend)
- [ ] **T4:** Create report DTO (backend)
- [ ] **T5:** Add export functionality (backend)
- [ ] **T6:** Write tests (test)

---

## Dependencies

- **Blocked by:** US-07-03 (documentation indexed)
- **Enables:** US-07-05 (quality alerts)

---

## Definition of Ready

- [x] All criteria met

