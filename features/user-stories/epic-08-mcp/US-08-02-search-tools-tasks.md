# Tasks for US-08-02: Code Search MCP Tools

## Story Reference
- **Epic:** EPIC-08 (MCP Tool Server)
- **Story:** US-08-02
- **Story Points:** 5
- **Sprint Target:** Sprint 6

## Task List

### T1: Implement search_code tool handler
- **Description:** Implement `search_code` MCP tool handler that performs code search. Tool should accept query, language, repository filters and return search results. Integrate with existing search API.
- **Estimated Hours:** 4 hours
- **Assignee:** TBD
- **Status:** Not Started
- **Dependencies:** US-08-01 (needs MCP server), US-04-02 (needs search API)
- **Acceptance Criteria:**
  - [ ] search_code tool handler implemented
  - [ ] Accepts query and filters
  - [ ] Returns search results
  - [ ] Tool latency <1s
- **Technical Notes:** Create tool handler class. Accept parameters: query (required), language (optional), repository (optional). Call search API. Format results for MCP response.

### T2: Implement search_by_entity tool handler
- **Description:** Implement `search_by_entity` MCP tool handler that searches for entities by name and type. Tool should accept entity name and entity type filters. Return matching entities.
- **Estimated Hours:** 3 hours
- **Assignee:** TBD
- **Status:** Not Started
- **Dependencies:** US-08-01 (needs MCP server), US-04-02 (needs search API)
- **Acceptance Criteria:**
  - [ ] search_by_entity tool handler implemented
  - [ ] Accepts entity name and type
  - [ ] Returns matching entities
  - [ ] Tool latency <1s
- **Technical Notes:** Create tool handler. Accept parameters: entity_name (required), entity_type (optional). Use entity_name filter in search. Return entity results.

### T3: Implement get_file_content tool handler
- **Description:** Implement `get_file_content` MCP tool handler that retrieves full file content by path. Tool should accept file path and return file content with syntax highlighting metadata.
- **Estimated Hours:** 3 hours
- **Assignee:** TBD
- **Status:** Not Started
- **Dependencies:** US-08-01 (needs MCP server)
- **Acceptance Criteria:**
  - [ ] get_file_content tool handler implemented
  - [ ] Accepts file path
  - [ ] Returns file content
  - [ ] Includes metadata
- **Technical Notes:** Create tool handler. Accept parameter: file_path (required). Fetch file content from storage or repository. Return content with language metadata.

### T4: Implement list_repositories tool handler
- **Description:** Implement `list_repositories` MCP tool handler that returns list of indexed repositories. Tool should return repository metadata (name, language, last indexed date).
- **Estimated Hours:** 2 hours
- **Assignee:** TBD
- **Status:** Not Started
- **Dependencies:** US-08-01 (needs MCP server)
- **Acceptance Criteria:**
  - [ ] list_repositories tool handler implemented
  - [ ] Returns repository list
  - [ ] Includes metadata
  - [ ] Fast response
- **Technical Notes:** Create tool handler. Query repository metadata from database. Return list of repositories with metadata. Format as MCP response.

### T5: Implement list_entities tool handler
- **Description:** Implement `list_entities` MCP tool handler that returns list of entities in a file. Tool should accept file path and return entities (classes, methods, functions) in that file.
- **Estimated Hours:** 3 hours
- **Assignee:** TBD
- **Status:** Not Started
- **Dependencies:** US-08-01 (needs MCP server)
- **Acceptance Criteria:**
  - [ ] list_entities tool handler implemented
  - [ ] Accepts file path
  - [ ] Returns entity list
  - [ ] Includes entity metadata
- **Technical Notes:** Create tool handler. Accept parameter: file_path (required). Query entities for file from database. Return entity list with metadata (name, type, line numbers).

### T6: Register tools with MCPToolRegistry
- **Description:** Register all search tools with MCPToolRegistry. Define tool metadata (name, description, parameter schemas). Register tool handlers. Ensure tools are discoverable via tool/list.
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
- **Description:** Create comprehensive tests for all search tools. Test each tool handler with various parameters. Test error handling and response formatting. Use MCP protocol test framework.
- **Estimated Hours:** 4 hours
- **Assignee:** TBD
- **Status:** Not Started
- **Dependencies:** T1-T6 (needs complete implementation)
- **Acceptance Criteria:**
  - [ ] Tests for all tools
  - [ ] Tests cover various parameters
  - [ ] Tests cover error handling
  - [ ] Test coverage >80%
- **Technical Notes:** Use MCP test framework or create custom tests. Test each tool with valid and invalid parameters. Mock search API. Verify response format.

---

## Summary
- **Total Tasks:** 7
- **Total Estimated Hours:** 22 hours
- **Story Points:** 5 (1 SP ≈ 4.4 hours, aligns with estimate)

