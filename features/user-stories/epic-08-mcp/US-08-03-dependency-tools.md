# US-08-03: Dependency Analysis MCP Tools

## Story
**As an** LLM (Cursor/Claude)  
**I want** dependency analysis tools via MCP  
**So that** I can answer structural questions about code

## Story Points: 5
## Priority: High
## Sprint Target: Sprint 6

---

## Acceptance Criteria

- [ ] **AC1:** `find_implementations` tool: interface → implementers (transitive)
- [ ] **AC2:** `find_usages` tool: entity → callers/references
- [ ] **AC3:** `find_callers` tool: method → call sites
- [ ] **AC4:** `find_dependencies` tool: entity → dependencies
- [ ] **AC5:** `get_inheritance_tree` tool: class hierarchy
- [ ] **AC6:** Tool latency <3s for traversals

---

## Demo Script

### Demo Steps
1. **Find Implementations:**
   ```json
   {"tool": "find_implementations", "arguments": {"interface": "IRepository", "transitive": true}}
   ```
2. **Show Results:** All implementing classes including transitive
3. **Get Callers:** Use `find_callers` tool
4. **Show Hierarchy:** Use `get_inheritance_tree` tool

### Expected Outcome
- Transitive queries work correctly
- Complete results returned

---

## Technical Tasks

- [ ] **T1:** Implement `find_implementations` tool (backend)
- [ ] **T2:** Implement `find_usages` tool (backend)
- [ ] **T3:** Implement `find_callers` tool (backend)
- [ ] **T4:** Implement `find_dependencies` tool (backend)
- [ ] **T5:** Implement `get_inheritance_tree` tool (backend)
- [ ] **T6:** Register tools with MCPToolRegistry (backend)
- [ ] **T7:** Write tool tests (test)

---

## Dependencies

- **Blocked by:** US-08-01 (MCP server), US-06-03-05 (graph queries)
- **Enables:** LLM structural code analysis

---

## Definition of Ready

- [x] All criteria met

