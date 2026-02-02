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

- [x] **AC1:** Configurable boost factors per index field
- [x] **AC2:** Default boosts: entity_name (3.0), doc_summary (2.0), content (1.0)
- [x] **AC3:** Configuration via application.properties
- [x] **AC4:** Changes apply without reindexing (query-time boosts)
- [x] **AC5:** Boost configuration validated on startup

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

- [x] **T1:** Create boost configuration class (backend)
- [x] **T2:** Load boosts from application.properties (backend)
- [x] **T3:** Apply boosts at query time in Lucene (backend)
- [x] **T4:** Add field match explanation to results (backend)
- [x] **T5:** Write tests for boost application (test)

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

---

## Implementation Notes

- **AC & tasks:** All acceptance criteria and technical tasks (T1–T5) are implemented; see `US-02-05-relevance-tuning-tasks.md` for details.
- **Demo "Show Config":** Current boost configuration is read from `application.properties` (and env overrides). There is no REST endpoint that returns boost values; for the demo, "Display current boost configuration" can be done by showing the config file or env. If a dashboard needs to display boosts via API, add a GET endpoint (e.g. `/api/v1/search/config` or `/api/v1/config/search/boost`) that returns `BoostConfiguration` values.

