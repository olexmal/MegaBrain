# US-08-04: Documentation MCP Tools

## Story
**As an** LLM (Cursor/Claude)  
**I want** documentation tools via MCP  
**So that** I can provide documentation context to users

## Story Points: 3
## Priority: High
## Sprint Target: Sprint 6

---

## Acceptance Criteria

- [ ] **AC1:** `get_documentation` tool: docs for entity
- [ ] **AC2:** `find_examples` tool: code examples for entity
- [ ] **AC3:** `get_doc_coverage` tool: coverage report for repo
- [ ] **AC4:** Tools return structured documentation data
- [ ] **AC5:** Handle undocumented entities gracefully

---

## Demo Script

### Demo Steps
1. **Get Docs:**
   ```json
   {"tool": "get_documentation", "arguments": {"entity": "UserService.authenticate"}}
   ```
2. **Show Docs:** Display Javadoc/description
3. **Find Examples:** Use `find_examples` tool
4. **Coverage:** Use `get_doc_coverage` tool

### Expected Outcome
- Documentation accessible via MCP
- Useful for LLM context enrichment

---

## Technical Tasks

- [ ] **T1:** Implement `get_documentation` tool (backend)
- [ ] **T2:** Implement `find_examples` tool (backend)
- [ ] **T3:** Implement `get_doc_coverage` tool (backend)
- [ ] **T4:** Register tools with MCPToolRegistry (backend)
- [ ] **T5:** Write tool tests (test)

---

## Dependencies

- **Blocked by:** US-08-01 (MCP server), US-07-03 (doc indexing)
- **Enables:** LLM documentation access

---

## Definition of Ready

- [x] All criteria met

