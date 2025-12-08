# Tasks for US-08-01: MCP Server Core Implementation

## Story Reference
- **Epic:** EPIC-08 (MCP Tool Server)
- **Story:** US-08-01
- **Story Points:** 8
- **Sprint Target:** Sprint 5

## Task List

### T1: Create MCPServer main class
- **Description:** Create main `MCPServer` class that implements MCP protocol server. Class should handle server lifecycle, protocol initialization, and request routing. Support both stdio and SSE transports.
- **Estimated Hours:** 6 hours
- **Assignee:** TBD
- **Status:** Not Started
- **Dependencies:** US-04-02, US-04-03 (needs search and RAG APIs)
- **Acceptance Criteria:**
  - [ ] MCPServer class created
  - [ ] Server lifecycle managed
  - [ ] Protocol initialization works
  - [ ] Request routing implemented
- **Technical Notes:** Use MCP Java SDK or implement protocol from spec. Handle server startup, shutdown, and request processing. Support both stdio and SSE transports.

### T2: Implement stdio transport handler
- **Description:** Implement stdio transport handler for MCP protocol. Read JSON-RPC requests from stdin, process requests, and write responses to stdout. Handle protocol handshake and message framing.
- **Estimated Hours:** 5 hours
- **Assignee:** TBD
- **Status:** Not Started
- **Dependencies:** T1 (needs MCPServer)
- **Acceptance Criteria:**
  - [ ] stdio transport implemented
  - [ ] JSON-RPC requests read from stdin
  - [ ] Responses written to stdout
  - [ ] Protocol handshake works
- **Technical Notes:** Use JSON-RPC library for request/response handling. Read from System.in, write to System.out. Handle JSON parsing and formatting. Implement protocol handshake.

### T3: Implement SSE transport handler
- **Description:** Implement Server-Sent Events (SSE) transport handler for MCP protocol. Handle HTTP SSE connections, send events, and receive requests. Support multiple concurrent SSE connections.
- **Estimated Hours:** 6 hours
- **Assignee:** TBD
- **Status:** Not Started
- **Dependencies:** T1 (needs MCPServer)
- **Acceptance Criteria:**
  - [ ] SSE transport implemented
  - [ ] HTTP SSE connections handled
  - [ ] Events sent correctly
  - [ ] Multiple connections supported
- **Technical Notes:** Use Quarkus RESTEasy Reactive for SSE. Handle HTTP SSE endpoint. Send events via SSE stream. Receive requests via HTTP POST. Support concurrent connections.

### T4: Create MCPToolRegistry
- **Description:** Create `MCPToolRegistry` class that manages MCP tools registration and discovery. Registry should register tools, provide tool metadata, and route tool calls to handlers. Support tool discovery via tool/list endpoint.
- **Estimated Hours:** 5 hours
- **Assignee:** TBD
- **Status:** Not Started
- **Dependencies:** T1 (needs MCPServer)
- **Acceptance Criteria:**
  - [ ] MCPToolRegistry created
  - [ ] Tool registration works
  - [ ] Tool metadata provided
  - [ ] Tool routing implemented
- **Technical Notes:** Create registry with tool registration. Store tool metadata (name, description, parameters). Route tool calls to handlers. Support tool discovery.

### T5: Implement initialize handshake
- **Description:** Implement MCP protocol initialize handshake. Handle initialize request, send initialize response with server capabilities, and complete handshake. Advertise server capabilities (tools, resources, prompts).
- **Estimated Hours:** 4 hours
- **Assignee:** TBD
- **Status:** Not Started
- **Dependencies:** T1, T4 (needs MCPServer and registry)
- **Acceptance Criteria:**
  - [ ] Initialize handshake implemented
  - [ ] Initialize request handled
  - [ ] Capabilities advertised
  - [ ] Handshake completes successfully
- **Technical Notes:** Handle initialize JSON-RPC request. Send initialize response with server info and capabilities. Advertise available tools, resources, and prompts. Complete handshake.

### T6: Implement tool/list endpoint
- **Description:** Implement tool/list endpoint that returns list of all available MCP tools. Return tool metadata including name, description, and parameter schemas. Support tool discovery by clients.
- **Estimated Hours:** 3 hours
- **Assignee:** TBD
- **Status:** Not Started
- **Dependencies:** T4 (needs MCPToolRegistry)
- **Acceptance Criteria:**
  - [ ] tool/list endpoint implemented
  - [ ] Returns all available tools
  - [ ] Tool metadata included
  - [ ] Parameter schemas included
- **Technical Notes:** Implement tool/list JSON-RPC method. Query MCPToolRegistry for all tools. Return tool list with metadata. Include JSON Schema for parameters.

### T7: Generate mcp.json file
- **Description:** Implement mcp.json discovery file generation. Generate mcp.json file with server configuration, command to start server, and transport information. File should be discoverable by MCP clients (Cursor, Claude Desktop).
- **Estimated Hours:** 3 hours
- **Assignee:** TBD
- **Status:** Not Started
- **Dependencies:** T1 (needs MCPServer)
- **Acceptance Criteria:**
  - [ ] mcp.json file generated
  - [ ] Server configuration included
  - [ ] Command to start server included
  - [ ] File format is correct
- **Technical Notes:** Generate mcp.json in standard location (~/.config/mcp/servers/). Include server name, command, args, env. Support stdio transport configuration.

### T8: Write protocol compliance tests
- **Description:** Create comprehensive tests for MCP protocol compliance. Test initialize handshake, tool discovery, tool calls, error handling, and both transports. Use MCP protocol test suite if available.
- **Estimated Hours:** 5 hours
- **Assignee:** TBD
- **Status:** Not Started
- **Dependencies:** T1-T7 (needs complete implementation)
- **Acceptance Criteria:**
  - [ ] Protocol compliance tests created
  - [ ] Tests cover handshake
  - [ ] Tests cover tool discovery
  - [ ] Tests cover both transports
  - [ ] Test coverage >80%
- **Technical Notes:** Use MCP protocol test suite or create custom tests. Test initialize, tool/list, tool calls. Test stdio and SSE transports. Verify protocol compliance.

---

## Summary
- **Total Tasks:** 8
- **Total Estimated Hours:** 37 hours
- **Story Points:** 8 (1 SP ≈ 4.6 hours, aligns with estimate)

