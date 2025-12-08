# US-06-06: Impact Analysis Report

## Story
**As a** developer  
**I want** to know what might break if I change an entity  
**So that** I can assess change risk

## Story Points: 5
## Priority: High
## Sprint Target: Sprint 5

---

## Acceptance Criteria

- [ ] **AC1:** Query: "What is impacted if I change X?"
- [ ] **AC2:** Returns direct dependents
- [ ] **AC3:** Returns transitive dependents (configurable depth)
- [ ] **AC4:** Categorized by impact type (compilation, runtime)
- [ ] **AC5:** Cross-repository impact detection
- [ ] **AC6:** Report format: JSON with impact scores

---

## Demo Script

### Demo Steps
1. **Analyze Impact:**
   ```bash
   curl "http://localhost:8080/api/v1/graph/impact?entity=IRepository&depth=5"
   ```
2. **Show Report:** Display affected classes/methods
3. **Categories:** Show compilation vs runtime impact
4. **Cross-Repo:** Show impact in other repositories

### Expected Outcome
- Comprehensive impact list
- Clear categorization
- Risk assessment visible

---

## Technical Tasks

- [ ] **T1:** Implement `analyzeImpact` method (backend)
- [ ] **T2:** Categorize impact by type (backend)
- [ ] **T3:** Add cross-repository graph queries (backend)
- [ ] **T4:** Create impact report DTO (backend)
- [ ] **T5:** Write integration tests (test)

---

## Dependencies

- **Blocked by:** US-06-03, US-06-04 (query capabilities)
- **Enables:** Risk-aware development

---

## Definition of Ready

- [x] All criteria met

