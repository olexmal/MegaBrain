# Tasks for US-08-04: Documentation MCP Tools

## Story Reference
- **Epic:** EPIC-08 (MCP Tool Server)
- **Story:** US-08-04
- **Story Points:** 3
- **Sprint Target:** Sprint 6

## Task List

### T1: Implement get_documentation tool
- **Description:** Implement `get_documentation` MCP tool handler that retrieves documentation for an entity. Tool should accept entity identifier and return documentation (description, params, returns, throws).
- **Estimated Hours:** 4 hours
- **Assignee:** TBD
- **Status:** Not Started
- **Dependencies:** US-08-01 (needs MCP server), US-07-03 (needs doc indexing)
- **Acceptance Criteria:**
  - [ ] get_documentation tool implemented
  - [ ] Accepts entity identifier
  - [ ] Returns documentation
  - [ ] Handles undocumented entities
- **Technical Notes:** Create tool handler. Accept parameter: entity (required, entity name or ID). Query documentation from index. Return structured documentation (description, params, returns, throws). Handle missing documentation gracefully.

### T2: Implement find_examples tool
- **Description:** Implement `find_examples` MCP tool handler that finds code examples for an entity. Tool should accept entity identifier and return code examples from documentation.
- **Estimated Hours:** 4 hours
- **Assignee:** TBD
- **Status:** Not Started
- **Dependencies:** US-08-01 (needs MCP server), US-07-06 (needs example extraction)
- **Acceptance Criteria:**
  - [ ] find_examples tool implemented
  - [ ] Accepts entity identifier
  - [ ] Returns code examples
  - [ ] Examples linked to entities
- **Technical Notes:** Create tool handler. Accept parameter: entity (required). Query example chunks for entity. Return code examples with metadata. Link examples to documentation.

### T3: Implement get_doc_coverage tool
- **Description:** Implement `get_doc_coverage` MCP tool handler that returns documentation coverage report for a repository. Tool should accept repository name and return coverage metrics.
- **Estimated Hours:** 4 hours
- **Assignee:** TBD
- **Status:** Not Started
- **Dependencies:** US-08-01 (needs MCP server), US-07-04 (needs coverage metrics)
- **Acceptance Criteria:**
  - [ ] get_doc_coverage tool implemented
  - [ ] Accepts repository name
  - [ ] Returns coverage metrics
  - [ ] Includes breakdown
- **Technical Notes:** Create tool handler. Accept parameter: repository (optional). Call documentation quality analyzer. Return coverage metrics (overall, by language, by type). Include undocumented entities list.

### T4: Register tools with MCPToolRegistry
- **Description:** Register all documentation tools with MCPToolRegistry. Define tool metadata (name, description, parameter schemas). Register tool handlers. Ensure tools are discoverable.
- **Estimated Hours:** 2 hours
- **Assignee:** TBD
- **Status:** Not Started
- **Dependencies:** T1-T3 (needs all tool handlers), US-08-01 (needs registry)
- **Acceptance Criteria:**
  - [ ] All tools registered
  - [ ] Tool metadata defined
  - [ ] Parameter schemas included
  - [ ] Tools discoverable
- **Technical Notes:** Register each tool with MCPToolRegistry. Define JSON Schema for parameters. Include tool descriptions. Ensure tools appear in tool/list response.

### T5: Write tool tests
- **Description:** Create comprehensive tests for all documentation tools. Test each tool handler with various parameters. Test error handling (undocumented entities, missing examples). Use MCP protocol test framework.
- **Estimated Hours:** 3 hours
- **Assignee:** TBD
- **Status:** Not Started
- **Dependencies:** T1-T4 (needs complete implementation)
- **Acceptance Criteria:**
  - [ ] Tests for all tools
  - [ ] Tests cover various parameters
  - [ ] Tests cover error handling
  - [ ] Test coverage >80%
- **Technical Notes:** Use MCP test framework or create custom tests. Test each tool with valid and invalid parameters. Mock documentation service. Verify response format.

---

## Summary
- **Total Tasks:** 5
- **Total Estimated Hours:** 17 hours
- **Story Points:** 3 (1 SP ≈ 5.7 hours, aligns with estimate)

