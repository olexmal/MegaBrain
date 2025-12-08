# Tasks for US-06-06: Impact Analysis Report

## Story Reference
- **Epic:** EPIC-06 (Dependency Graph Analysis)
- **Story:** US-06-06
- **Story Points:** 5
- **Sprint Target:** Sprint 5

## Task List

### T1: Implement analyzeImpact method
- **Description:** Implement `analyzeImpact` method that finds all entities that depend on a given entity (direct and transitive dependents). Use graph traversal to find dependents through various relationship types. Support depth parameter for transitive analysis.
- **Estimated Hours:** 6 hours
- **Assignee:** TBD
- **Status:** Not Started
- **Dependencies:** US-06-03, US-06-04 (needs query capabilities)
- **Acceptance Criteria:**
  - [ ] analyzeImpact method implemented
  - [ ] Finds direct dependents
  - [ ] Finds transitive dependents
  - [ ] Depth parameter supported
- **Technical Notes:** Use Cypher: `MATCH (e {id: $id})<-[:IMPLEMENTS|EXTENDS|CALLS|REFERENCES*1..$depth]-(dependent) RETURN dependent`. Combine multiple relationship types. Return dependent entities.

### T2: Categorize impact by type
- **Description:** Implement impact categorization logic that categorizes dependents by impact type (compilation impact, runtime impact). Compilation impact: entities that directly reference the changed entity. Runtime impact: entities that use the changed entity at runtime.
- **Estimated Hours:** 4 hours
- **Assignee:** TBD
- **Status:** Not Started
- **Dependencies:** T1 (needs impact analysis)
- **Acceptance Criteria:**
  - [ ] Impact categorized by type
  - [ ] Compilation impact identified
  - [ ] Runtime impact identified
  - [ ] Categories are accurate
- **Technical Notes:** Categorize based on relationship type: IMPLEMENTS, EXTENDS → compilation; CALLS, REFERENCES → runtime. Assign impact scores based on category.

### T3: Add cross-repository graph queries
- **Description:** Extend graph queries to support cross-repository impact analysis. Query dependents across multiple repositories. Include repository information in results. Filter by repository if needed.
- **Estimated Hours:** 5 hours
- **Assignee:** TBD
- **Status:** Not Started
- **Dependencies:** T1 (needs impact analysis)
- **Acceptance Criteria:**
  - [ ] Cross-repository queries supported
  - [ ] Repository information included
  - [ ] Filtering by repository works
  - [ ] Performance acceptable
- **Technical Notes:** Include repository property in nodes. Query across repositories: `MATCH (e {id: $id, repository: $repo})<-[:*]-(dependent) RETURN dependent`. Filter results by repository if needed.

### T4: Create impact report DTO
- **Description:** Create ImpactReport DTO that represents impact analysis results. Include affected entities, impact categories, impact scores, and relationship paths. Format as JSON with structured data.
- **Estimated Hours:** 3 hours
- **Assignee:** TBD
- **Status:** Not Started
- **Dependencies:** T1, T2 (needs impact analysis and categorization)
- **Acceptance Criteria:**
  - [ ] ImpactReport DTO created
  - [ ] Includes all required fields
  - [ ] Serializable to JSON
  - [ ] Format is clear
- **Technical Notes:** Use Java record or POJO. Include: entity_id, entity_name, impact_type, impact_score, relationship_path, repository. Format as JSON array of impact items.

### T5: Write integration tests
- **Description:** Create integration tests for impact analysis. Test with known dependency graphs. Verify impact analysis finds all dependents correctly. Test categorization and cross-repository queries.
- **Estimated Hours:** 4 hours
- **Assignee:** TBD
- **Status:** Not Started
- **Dependencies:** T1-T4 (needs complete implementation)
- **Acceptance Criteria:**
  - [ ] Integration tests for impact analysis
  - [ ] Tests verify dependents found
  - [ ] Tests verify categorization
  - [ ] Test coverage >80%
- **Technical Notes:** Create test graph with known dependencies. Test impact analysis on various entities. Verify results include all expected dependents.

---

## Summary
- **Total Tasks:** 5
- **Total Estimated Hours:** 22 hours
- **Story Points:** 5 (1 SP ≈ 4.4 hours, aligns with estimate)

