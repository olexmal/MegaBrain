# EPIC-08: MCP Tool Server

## Epic Overview

| Attribute | Value |
|:----------|:------|
| **Epic ID** | EPIC-08 |
| **Priority** | High |
| **Estimated Scope** | L |
| **Dependencies** | EPIC-02 (Search), EPIC-03 (RAG), EPIC-06 (Dependency Graph), EPIC-07 (Documentation) |
| **Spec Reference** | Section 4.7 (FR-MCP), Section 4.4 (FR-IFC-04) |
| **Status** | Planned |

## Business Value

This epic enables native LLM integration via the Model Context Protocol (MCP):

- **Cursor integration:** MegaBrain tools available directly in Cursor IDE
- **Claude Desktop:** Use MegaBrain as a tool for Claude
- **Any MCP client:** Standard protocol ensures broad compatibility

LLMs can directly search code, analyze dependencies, find documentation, and ask questions—all without manual copy-paste of context.

---

## User Stories

### US-08-01: MCP Server Implementation

**As a** system, **I want** to implement the MCP protocol, **so that** LLM clients can connect and use MegaBrain tools.

**Acceptance Criteria:**
- [ ] MCP server implements full protocol specification
- [ ] stdio transport for local IDE integration (Cursor)
- [ ] SSE transport for remote/web clients
- [ ] Server discovery via `mcp.json` configuration
- [ ] Session management for stateful operations
- [ ] Protocol version negotiation
- [ ] Graceful error handling

**Spec Reference:** FR-MCP-01

---

### US-08-02: Code Search Tools

**As an** LLM, **I want** tools to search code, **so that** I can find relevant code for user questions.

**Acceptance Criteria:**
- [ ] Tool: `search_code` - semantic and keyword search
  - Parameters: `query`, `language?`, `repository?`, `limit?`
  - Returns: list of matching code chunks with metadata
- [ ] Tool: `search_by_entity` - find specific entities
  - Parameters: `entity_name`, `entity_type?`, `exact_match?`
  - Returns: matching entities with file locations
- [ ] Tool: `get_file_content` - retrieve file content
  - Parameters: `file_path`, `start_line?`, `end_line?`
  - Returns: file content with line numbers
- [ ] Tool: `list_repositories` - list indexed repos
  - Returns: repository names and metadata
- [ ] Tool: `list_entities` - list entities in file/module
  - Parameters: `path`, `entity_type?`
  - Returns: list of entities

**Spec Reference:** FR-MCP-02

---

### US-08-03: Dependency Analysis Tools

**As an** LLM, **I want** tools to analyze code dependencies, **so that** I can answer structural questions.

**Acceptance Criteria:**
- [ ] Tool: `find_implementations` - find interface implementations
  - Parameters: `interface_name`, `repository?`
  - Returns: all implementations (transitive)
- [ ] Tool: `find_usages` - find usages of entity
  - Parameters: `entity_name`, `transitive?`
  - Returns: all usage locations
- [ ] Tool: `find_callers` - find function callers
  - Parameters: `function_name`, `depth?`
  - Returns: caller functions with paths
- [ ] Tool: `find_dependencies` - find entity dependencies
  - Parameters: `entity_name`, `depth?`
  - Returns: dependency entities
- [ ] Tool: `get_inheritance_tree` - get class hierarchy
  - Parameters: `class_name`, `direction?`
  - Returns: inheritance tree structure

**Spec Reference:** FR-MCP-03

---

### US-08-04: Documentation Tools

**As an** LLM, **I want** tools to access documentation, **so that** I can provide accurate API information.

**Acceptance Criteria:**
- [ ] Tool: `get_documentation` - get entity documentation
  - Parameters: `entity_name`
  - Returns: doc comment content, params, returns
- [ ] Tool: `find_examples` - find code examples
  - Parameters: `entity_name`
  - Returns: example code from documentation
- [ ] Tool: `get_doc_coverage` - get coverage metrics
  - Parameters: `path`
  - Returns: coverage score and undocumented APIs

**Spec Reference:** FR-MCP-04

---

### US-08-05: RAG Query Tool

**As an** LLM, **I want** a tool for natural language Q&A, **so that** I can get synthesized answers about the codebase.

**Acceptance Criteria:**
- [ ] Tool: `ask_codebase` - natural language question
  - Parameters: `question`, `context_limit?`
  - Returns: answer with source citations
- [ ] Internally uses hybrid search and LLM generation
- [ ] Sources included in response
- [ ] Appropriate for complex "how does X work" questions

**Spec Reference:** FR-MCP-05

---

### US-08-06: MCP Resources

**As an** LLM client, **I want** to access indexed data as resources, **so that** I can explore the codebase.

**Acceptance Criteria:**
- [ ] Resource: `megabrain://repo/{repo_name}` - repository info
- [ ] Resource: `megabrain://file/{path}` - file content
- [ ] Resource: `megabrain://entity/{id}` - entity details
- [ ] Resource subscription for update notifications
- [ ] Resource listing (list available resources)
- [ ] Efficient resource fetching

**Spec Reference:** FR-MCP-06

---

### US-08-07: Tool Schema Discovery

**As an** LLM client, **I want** to discover available tools with their schemas, **so that** I know what tools to use.

**Acceptance Criteria:**
- [ ] Tool list endpoint returns all available tools
- [ ] Each tool includes JSON Schema for parameters
- [ ] Tool descriptions explain purpose and usage
- [ ] Schema served in <100ms
- [ ] Schema includes examples

**Spec Reference:** FR-MCP-01 (Protocol)

---

### US-08-08: Cursor Integration

**As a** developer using Cursor, **I want** MegaBrain tools available in my IDE, **so that** I can search my org's code from Cursor.

**Acceptance Criteria:**
- [ ] stdio transport works with Cursor's MCP client
- [ ] `mcp.json` configuration documented
- [ ] Tools appear in Cursor's tool list
- [ ] Tool calls complete successfully
- [ ] Error messages helpful for debugging
- [ ] Setup guide for Cursor users

**Spec Reference:** FR-IFC-04 (Clients: Cursor)

---

### US-08-09: Concurrent Sessions

**As a** system, **I want** to support multiple concurrent MCP sessions, **so that** multiple users/clients can connect.

**Acceptance Criteria:**
- [ ] Support 10+ concurrent MCP client connections
- [ ] Session isolation (one client's state doesn't affect others)
- [ ] Session timeout and cleanup
- [ ] Resource fair sharing
- [ ] Connection monitoring and logging

**Spec Reference:** NFR - MCP Server

---

## Technical Notes

### Key Components
- **`MCPServer`:** Core protocol handler, manages transports and sessions
- **`MCPToolRegistry`:** Registers and dispatches tool calls
- **`MCPResourceProvider`:** Exposes repositories and files as resources

### Technology Stack
| Component | Technology |
|:----------|:-----------|
| MCP Implementation | Custom Java or mcp-java-sdk |
| stdio Transport | Java Process I/O |
| SSE Transport | Quarkus SSE support |
| Schema Definition | JSON Schema |

### MCP Tool Definition Format

```json
{
  "name": "search_code",
  "description": "Search for code using semantic and keyword matching",
  "inputSchema": {
    "type": "object",
    "properties": {
      "query": {
        "type": "string",
        "description": "Search query"
      },
      "language": {
        "type": "string",
        "description": "Filter by programming language"
      },
      "repository": {
        "type": "string",
        "description": "Filter by repository name"
      },
      "limit": {
        "type": "integer",
        "description": "Maximum results to return",
        "default": 10
      }
    },
    "required": ["query"]
  }
}
```

### MCP Server Architecture

```
┌─────────────────────────────────────────┐
│              MCP Server                  │
├─────────────────────────────────────────┤
│  ┌─────────┐  ┌─────────┐  ┌─────────┐ │
│  │  stdio  │  │   SSE   │  │  HTTP   │ │
│  │Transport│  │Transport│  │Transport│ │
│  └────┬────┘  └────┬────┘  └────┬────┘ │
│       └────────────┼────────────┘       │
│                    ▼                     │
│           ┌──────────────┐              │
│           │Protocol Layer│              │
│           │ (MCP Spec)   │              │
│           └──────┬───────┘              │
│                  ▼                       │
│    ┌─────────────────────────────┐      │
│    │       Tool Registry         │      │
│    │  search_code, find_usages,  │      │
│    │  ask_codebase, etc.         │      │
│    └─────────────┬───────────────┘      │
│                  ▼                       │
│    ┌─────────────────────────────┐      │
│    │    Resource Provider        │      │
│    │  repos, files, entities     │      │
│    └─────────────────────────────┘      │
└─────────────────────────────────────────┘
                   │
                   ▼
        ┌─────────────────────┐
        │  MegaBrain Services │
        │  (Search, RAG,      │
        │   Graph, Docs)      │
        └─────────────────────┘
```

### mcp.json Configuration Example

```json
{
  "mcpServers": {
    "megabrain": {
      "command": "megabrain",
      "args": ["mcp-server"],
      "env": {
        "MEGABRAIN_URL": "http://localhost:8080"
      }
    }
  }
}
```

---

## Risks & Mitigations

| Risk | Impact | Likelihood | Mitigation |
|:-----|:-------|:-----------|:-----------|
| MCP protocol changes | Breaking changes | Medium | Pin protocol version; abstraction layer |
| stdio buffering issues | Dropped messages | Medium | Proper stream handling; testing |
| Tool response too large | LLM context overflow | Medium | Response size limits; pagination |
| Session state leaks | Memory growth | Medium | Session cleanup; monitoring |
| Client compatibility | Tools don't work in client | Medium | Test with multiple clients; documentation |

---

## Non-Functional Requirements

| NFR | Target | Validation |
|:----|:-------|:-----------|
| Tool response latency (search) | <1s | End-to-end timing |
| Tool response latency (graph) | <3s | End-to-end timing |
| Concurrent sessions | 10+ | Load testing |
| Tool discovery latency | <100ms | Timing tests |
| Resource subscriptions | 100+ per instance | Scale testing |

---

## Definition of Done

- [ ] All user stories complete and accepted
- [ ] MCP server running with stdio transport
- [ ] All tools implemented and tested
- [ ] Resource provider working
- [ ] Cursor integration verified
- [ ] Claude Desktop integration verified
- [ ] Concurrent session support
- [ ] Unit tests (>80% coverage)
- [ ] Integration tests with real MCP clients
- [ ] Setup documentation for users
- [ ] NFRs validated

---

## Open Questions

1. Should we support MCP "prompts" feature for guided interactions?
2. How do we version MCP tools for backwards compatibility?
3. Should resource subscriptions use polling or push?
4. Do we need authentication for MCP connections?

---

**Epic Owner:** TBD  
**Created:** December 2024  
**Last Updated:** December 2024

