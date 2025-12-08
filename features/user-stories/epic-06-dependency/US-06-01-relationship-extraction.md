# US-06-01: Entity Relationship Extraction

## Story
**As a** system  
**I want** to extract relationships between code entities  
**So that** dependency queries are possible

## Story Points: 8
## Priority: Critical
## Sprint Target: Sprint 3

---

## Acceptance Criteria

- [ ] **AC1:** Extract "implements" relationships (class → interface)
- [ ] **AC2:** Extract "extends" relationships (class → class)
- [ ] **AC3:** Extract "imports" relationships (file → file/package)
- [ ] **AC4:** Extract "calls" relationships (method → method)
- [ ] **AC5:** Extract "instantiates" relationships (code → class)
- [ ] **AC6:** Extract "references" relationships (code → type)
- [ ] **AC7:** Relationships include source/target entity IDs

---

## Demo Script

### Demo Steps
1. **Ingest Repository:** Index a Java project
2. **Query Relationships:** Show extracted relationships
   ```sql
   SELECT * FROM relationships WHERE type = 'implements';
   ```
3. **Show Types:** List all relationship types
4. **Method Calls:** Show call graph for a method

### Expected Outcome
- All relationship types extracted
- Relationships link entities correctly
- Data is queryable

---

## Technical Tasks

- [ ] **T1:** Create `DependencyExtractor` interface (backend)
- [ ] **T2:** Implement Java relationship extraction (backend)
- [ ] **T3:** Implement Python relationship extraction (backend)
- [ ] **T4:** Implement JS/TS relationship extraction (backend)
- [ ] **T5:** Create relationship model/DTO (backend)
- [ ] **T6:** Store relationships during parsing (backend)
- [ ] **T7:** Write tests for each relationship type (test)

---

## Dependencies

- **Blocked by:** US-01-04 (Java parsing), US-01-05 (Tree-sitter)
- **Enables:** US-06-02 (graph storage)

---

## Risks

| Risk | Impact | Likelihood | Mitigation |
|:-----|:-------|:-----------|:-----------|
| Complex resolution | Missing relationships | Medium | Comprehensive test cases |
| Cross-file references | Incomplete graph | Medium | Multi-pass extraction |

---

## Definition of Ready

- [x] All criteria met

