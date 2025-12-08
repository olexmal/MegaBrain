# US-02-05: Relevance Tuning Configuration

## Story
**As a** search administrator  
**I want** to configure field boost weights  
**So that** matches in entity names rank higher than matches in comments

## Story Points: 2
## Priority: Medium
## Sprint Target: Sprint 4

---

## Acceptance Criteria

- [ ] **AC1:** Configurable boost factors per index field
- [ ] **AC2:** Default boosts: entity_name (3.0), doc_summary (2.0), content (1.0)
- [ ] **AC3:** Configuration via application.properties
- [ ] **AC4:** Changes apply without reindexing (query-time boosts)
- [ ] **AC5:** Boost configuration validated on startup

---

## Demo Script

### Setup
1. Index with default boost configuration
2. Have queries that match in multiple fields

### Demo Steps
1. **Default Ranking:** Search and show ranking with defaults
2. **Show Config:** Display current boost configuration
3. **Modify Config:** Change entity_name boost to 5.0
4. **Re-Search:** Show ranking change after config update
5. **Field Match:** Highlight which field each result matched

### Expected Outcome
- Entity name matches rank highest
- Config changes affect ranking
- Field match visibility

---

## Technical Tasks

- [ ] **T1:** Create boost configuration class (backend)
- [ ] **T2:** Load boosts from application.properties (backend)
- [ ] **T3:** Apply boosts at query time in Lucene (backend)
- [ ] **T4:** Add field match explanation to results (backend)
- [ ] **T5:** Write tests for boost application (test)

---

## Test Scenarios

| Scenario | Given | When | Then |
|:---------|:------|:-----|:-----|
| Default boosts | Default config | Match in entity_name | High score |
| Custom boosts | entity_name=5.0 | Match in entity_name | Higher score than default |
| Content match | Match only in content | Search | Lower score than name match |
| Multi-field match | Match in name and content | Search | Combined boosted score |

---

## Dependencies

- **Blocked by:** US-02-01 (Lucene index)
- **Enables:** Better search relevance

---

## Risks

| Risk | Impact | Likelihood | Mitigation |
|:-----|:-------|:-----------|:-----------|
| Bad configuration | Poor relevance | Low | Validation; defaults |
| Unclear optimal values | Subpar results | Medium | A/B testing guidance |

---

## Definition of Ready

- [x] Acceptance criteria clear
- [x] Dependencies identified
- [x] Tech tasks estimated
- [x] Test scenarios defined
- [x] Demo script approved
- [x] No blockers

