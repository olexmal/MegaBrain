# US-08-02: Code Search MCP Tools

## Story
**As an** LLM (Cursor/Claude)  
**I want** search tools available via MCP  
**So that** I can find relevant code for the user

## Story Points: 5
## Priority: Critical
## Sprint Target: Sprint 6

---

## Acceptance Criteria

- [ ] **AC1:** `search_code` tool: query, language, repository filters
- [ ] **AC2:** `search_by_entity` tool: entity name, type filter
- [ ] **AC3:** `get_file_content` tool: full file by path
- [ ] **AC4:** `list_repositories` tool: indexed repositories
- [ ] **AC5:** `list_entities` tool: entities in a file
- [ ] **AC6:** Tool latency <1s for search

---

## Demo Script

### Demo Steps
1. **Call search_code:**
   ```json
   {"tool": "search_code", "arguments": {"query": "authentication", "language": "java"}}
   ```
2. **Show Results:** Display returned code chunks
3. **Get File:** Use `get_file_content` tool
4. **List Repos:** Use `list_repositories` tool

### Expected Outcome
- All search tools functional
- Fast response times
- Useful for LLM context

---

## Technical Tasks

- [ ] **T1:** Implement `search_code` tool handler (backend)
- [ ] **T2:** Implement `search_by_entity` tool handler (backend)
- [ ] **T3:** Implement `get_file_content` tool handler (backend)
- [ ] **T4:** Implement `list_repositories` tool handler (backend)
- [ ] **T5:** Implement `list_entities` tool handler (backend)
- [ ] **T6:** Register tools with MCPToolRegistry (backend)
- [ ] **T7:** Write tool tests (test)

---

## Dependencies

- **Blocked by:** US-08-01 (MCP server)
- **Enables:** LLM code search capability

---

## Definition of Ready

- [x] All criteria met

