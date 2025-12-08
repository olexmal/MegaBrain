# Tasks for US-08-03: Dependency Analysis MCP Tools

## Story Reference
- **Epic:** EPIC-08 (MCP Tool Server)
- **Story:** US-08-03
- **Story Points:** 5
- **Sprint Target:** Sprint 6

## Task List

### T1: Implement find_implementations tool
- **Description:** Implement `find_implementations` MCP tool handler that finds all classes implementing an interface (transitive). Tool should accept interface name and transitive flag. Return implementing classes.
- **Estimated Hours:** 4 hours
- **Assignee:** TBD
- **Status:** Not Started
- **Dependencies:** US-08-01 (needs MCP server), US-06-03 (needs graph queries)
- **Acceptance Criteria:**
  - [ ] find_implementations tool implemented
  - [ ] Accepts interface name
  - [ ] Supports transitive flag
  - [ ] Returns implementing classes
  - [ ] Tool latency <3s
- **Technical Notes:** Create tool handler. Accept parameters: interface (required), transitive (optional, default: false). Call graph query service. Return implementing classes with metadata.

### T2: Implement find_usages tool
- **Description:** Implement `find_usages` MCP tool handler that finds all usages of an entity (callers, references). Tool should accept entity name and return usages. Support depth parameter.
- **Estimated Hours:** 4 hours
- **Assignee:** TBD
- **Status:** Not Started
- **Dependencies:** US-08-01 (needs MCP server), US-06-03 (needs graph queries)
- **Acceptance Criteria:**
  - [ ] find_usages tool implemented
  - [ ] Accepts entity name
  - [ ] Returns usages
  - [ ] Depth parameter supported
- **Technical Notes:** Create tool handler. Accept parameters: entity (required), depth (optional). Call graph query service for callers and references. Return usage list.

### T3: Implement find_callers tool
- **Description:** Implement `find_callers` MCP tool handler that finds all methods/functions that call a given method/function. Tool should accept method name and return callers. Support transitive calls.
- **Estimated Hours:** 4 hours
- **Assignee:** TBD
- **Status:** Not Started
- **Dependencies:** US-08-01 (needs MCP server), US-06-03 (needs graph queries)
- **Acceptance Criteria:**
  - [ ] find_callers tool implemented
  - [ ] Accepts method name
  - [ ] Returns callers
  - [ ] Transitive calls supported
- **Technical Notes:** Create tool handler. Accept parameters: method (required), transitive (optional). Call graph query service for callers. Return caller list with metadata.

### T4: Implement find_dependencies tool
- **Description:** Implement `find_dependencies` MCP tool handler that finds all dependencies of an entity (what it depends on). Tool should accept entity name and return dependencies. Support depth parameter.
- **Estimated Hours:** 4 hours
- **Assignee:** TBD
- **Status:** Not Started
- **Dependencies:** US-08-01 (needs MCP server), US-06-04 (needs graph queries)
- **Acceptance Criteria:**
  - [ ] find_dependencies tool implemented
  - [ ] Accepts entity name
  - [ ] Returns dependencies
  - [ ] Depth parameter supported
- **Technical Notes:** Create tool handler. Accept parameters: entity (required), depth (optional). Call graph query service for dependencies. Return dependency list.

### T5: Implement get_inheritance_tree tool
- **Description:** Implement `get_inheritance_tree` MCP tool handler that returns inheritance hierarchy for a class or interface. Tool should accept entity name and return tree structure.
- **Estimated Hours:** 4 hours
- **Assignee:** TBD
- **Status:** Not Started
- **Dependencies:** US-08-01 (needs MCP server), US-06-05 (needs hierarchy queries)
- **Acceptance Criteria:**
  - [ ] get_inheritance_tree tool implemented
  - [ ] Accepts entity name
  - [ ] Returns tree structure
  - [ ] Tree format is clear
- **Technical Notes:** Create tool handler. Accept parameter: entity (required). Call graph query service for inheritance tree. Return tree structure (nested or flat with parent references).

### T6: Register tools with MCPToolRegistry
- **Description:** Register all dependency analysis tools with MCPToolRegistry. Define tool metadata (name, description, parameter schemas). Register tool handlers. Ensure tools are discoverable.
- **Estimated Hours:** 3 hours
- **Assignee:** TBD
- **Status:** Not Started
- **Dependencies:** T1-T5 (needs all tool handlers), US-08-01 (needs registry)
- **Acceptance Criteria:**
  - [ ] All tools registered
  - [ ] Tool metadata defined
  - [ ] Parameter schemas included
  - [ ] Tools discoverable
- **Technical Notes:** Register each tool with MCPToolRegistry. Define JSON Schema for parameters. Include tool descriptions. Ensure tools appear in tool/list response.

### T7: Write tool tests
- **Description:** Create comprehensive tests for all dependency analysis tools. Test each tool handler with various parameters. Test transitive queries and error handling. Use MCP protocol test framework.
- **Estimated Hours:** 4 hours
- **Assignee:** TBD
- **Status:** Not Started
- **Dependencies:** T1-T6 (needs complete implementation)
- **Acceptance Criteria:**
  - [ ] Tests for all tools
  - [ ] Tests cover transitive queries
  - [ ] Tests cover error handling
  - [ ] Test coverage >80%
- **Technical Notes:** Use MCP test framework or create custom tests. Test each tool with valid and invalid parameters. Mock graph query service. Verify response format.

---

## Summary
- **Total Tasks:** 7
- **Total Estimated Hours:** 27 hours
- **Story Points:** 5 (1 SP ≈ 5.4 hours, aligns with estimate)

