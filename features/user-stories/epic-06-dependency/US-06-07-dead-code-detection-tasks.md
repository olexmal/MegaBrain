# Tasks for US-06-07: Dead Code Detection

## Story Reference
- **Epic:** EPIC-06 (Dependency Graph Analysis)
- **Story:** US-06-07
- **Story Points:** 5
- **Sprint Target:** Sprint 6

## Task List

### T1: Implement dead code query
- **Description:** Implement Cypher query to identify dead code (unused entities). Find classes with no incoming references (no implements, extends, instantiates, calls). Find methods with no callers. Exclude entry points and framework code.
- **Estimated Hours:** 6 hours
- **Assignee:** TBD
- **Status:** Not Started
- **Dependencies:** US-06-03 (needs incoming queries)
- **Acceptance Criteria:**
  - [ ] Dead code query implemented
  - [ ] Classes with no references found
  - [ ] Methods with no callers found
  - [ ] Entry points excluded
- **Technical Notes:** Use Cypher: `MATCH (e:Class) WHERE NOT (e)<-[:IMPLEMENTS|EXTENDS|INSTANTIATES|CALLS]-() RETURN e`. Combine with entry point exclusion. Return unused entities.

### T2: Add entry point detection
- **Description:** Implement entry point detection logic that identifies code entry points (main methods, test methods, framework entry points). Exclude entry points from dead code detection. Support configurable entry point patterns.
- **Estimated Hours:** 4 hours
- **Assignee:** TBD
- **Status:** Not Started
- **Dependencies:** T1 (needs dead code query)
- **Acceptance Criteria:**
  - [ ] Entry point detection implemented
  - [ ] Main methods detected
  - [ ] Test methods detected
  - [ ] Framework entry points detected
- **Technical Notes:** Detect entry points: methods named "main", test methods (JUnit, TestNG), framework entry points (Spring @Component, etc.). Exclude from dead code results.

### T3: Create exclusion pattern configuration
- **Description:** Implement configuration for exclusion patterns to exclude specific code from dead code detection. Support patterns for package names, class names, method names. Load patterns from configuration file.
- **Estimated Hours:** 3 hours
- **Assignee:** TBD
- **Status:** Not Started
- **Dependencies:** T1 (needs dead code query)
- **Acceptance Criteria:**
  - [ ] Exclusion patterns configurable
  - [ ] Patterns loaded from config
  - [ ] Patterns applied to results
  - [ ] Default patterns provided
- **Technical Notes:** Use Quarkus configuration. Support regex patterns for package/class/method names. Apply patterns to filter dead code results. Provide default patterns (tests, examples, etc.).

### T4: Add confidence scoring
- **Description:** Implement confidence scoring for dead code candidates. Assign confidence scores based on factors: no references, no tests, not in public API, etc. Higher confidence = more likely to be dead code.
- **Estimated Hours:** 4 hours
- **Assignee:** TBD
- **Status:** Not Started
- **Dependencies:** T1-T3 (needs dead code detection)
- **Acceptance Criteria:**
  - [ ] Confidence scoring implemented
  - [ ] Scores based on multiple factors
  - [ ] Scores range 0-100
  - [ ] Higher scores = more likely dead
- **Technical Notes:** Calculate confidence: no references (50 points), no tests (20 points), not public (20 points), unused imports (10 points). Sum scores. Return entities with scores.

### T5: Create export functionality
- **Description:** Implement export functionality to export dead code report as CSV or JSON. Include entity information, confidence scores, and exclusion reasons. Support filtering by confidence threshold.
- **Estimated Hours:** 3 hours
- **Assignee:** TBD
- **Status:** Not Started
- **Dependencies:** T1-T4 (needs dead code detection)
- **Acceptance Criteria:**
  - [ ] Export to CSV implemented
  - [ ] Export to JSON implemented
  - [ ] Confidence threshold filtering
  - [ ] Export includes all data
- **Technical Notes:** Use CSV library (OpenCSV) or Jackson for JSON. Include: entity_name, entity_type, file_path, confidence_score, exclusion_reason. Support filtering by confidence threshold.

### T6: Write tests with known dead code
- **Description:** Create comprehensive tests for dead code detection. Use test codebase with known dead code. Verify dead code is detected correctly. Test entry point exclusion and confidence scoring.
- **Estimated Hours:** 4 hours
- **Assignee:** TBD
- **Status:** Not Started
- **Dependencies:** T1-T5 (needs complete implementation)
- **Acceptance Criteria:**
  - [ ] Tests with known dead code
  - [ ] Tests verify detection
  - [ ] Tests verify exclusions
  - [ ] Test coverage >80%
- **Technical Notes:** Create test codebase with unused classes/methods. Verify dead code detection finds them. Test entry point exclusion. Verify confidence scores.

---

## Summary
- **Total Tasks:** 6
- **Total Estimated Hours:** 24 hours
- **Story Points:** 5 (1 SP ≈ 4.8 hours, aligns with estimate)

