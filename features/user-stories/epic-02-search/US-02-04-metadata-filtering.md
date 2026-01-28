# US-02-04: Metadata Facet Filtering

## Story
**As a** developer  
**I want** to filter search results by language, repository, and entity type  
**So that** I can narrow down results to relevant code

## Story Points: 3
## Priority: High
## Sprint Target: Sprint 3

---

## Acceptance Criteria

- [x] **AC1:** Filter by `language` (Java, Python, etc.)
- [x] **AC2:** Filter by `repository` name
- [x] **AC3:** Filter by `file_path` (prefix match)
- [x] **AC4:** Filter by `entity_type` (class, method, function)
- [x] **AC5:** Filters combinable with AND logic
- [x] **AC6:** Filters applied efficiently before ranking
- [x] **AC7:** Available filter values returned as facets

---

## Demo Script

### Setup
1. Index a polyglot repository with multiple languages
2. Ensure variety of entity types indexed

### Demo Steps
1. **Language Filter:** Search only Java files
   ```bash
   curl "http://localhost:8080/api/v1/search?q=service&language=java"
   ```
2. **Repository Filter:** Search only in specific repo
3. **Entity Type Filter:** Search only methods
4. **Combined Filters:** Language + entity type
5. **Show Facets:** Display available filter values
   ```json
   {
     "facets": {
       "language": ["java", "python", "javascript"],
       "entity_type": ["class", "method", "function"]
     }
   }
   ```

### Expected Outcome
- Results correctly filtered
- Multiple filters work together
- Facet values accurate

---

## Technical Tasks

- [x] **T1:** Add filter parameters to search API (backend)
- [x] **T2:** Implement Lucene filter queries (backend)
- [x] **T3:** Implement facet aggregation (backend)
- [x] **T4:** Optimize filter application before scoring (backend)
- [x] **T5:** Add facet counts to response (backend)
- [x] **T6:** Write tests for each filter type (test)

---

## Test Scenarios

| Scenario | Given | When | Then |
|:---------|:------|:-----|:-----|
| Language filter | Mixed language index | Filter language=python | Only Python results |
| Repository filter | Multi-repo index | Filter repository=backend | Only backend repo |
| Entity type filter | Mixed entities | Filter entity_type=class | Only classes |
| Combined filters | Mixed index | language=java+type=method | Java methods only |
| Path prefix | Various paths | Filter path=src/auth/ | Only auth directory |
| No matches | Filter excludes all | Applied filter | Empty results |

---

## Dependencies

- **Blocked by:** US-02-01 (Lucene index must exist)
- **Enables:** US-05-03 (UI faceted search)

---

## Risks

| Risk | Impact | Likelihood | Mitigation |
|:-----|:-------|:-----------|:-----------|
| Slow facet computation | Latency increase | Medium | Caching; pre-computed facets |
| Filter mismatch | No results | Low | Validate filters; suggest corrections |

---

## Definition of Ready

- [x] Acceptance criteria clear
- [x] Dependencies identified
- [x] Tech tasks estimated
- [x] Test scenarios defined
- [x] Demo script approved
- [x] No blockers

