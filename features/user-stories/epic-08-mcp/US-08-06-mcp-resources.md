# US-08-06: MCP Resources Provider

## Story
**As an** LLM client  
**I want** MCP resources exposing indexed data  
**So that** I can browse and subscribe to codebase changes

## Story Points: 5
## Priority: High
## Sprint Target: Sprint 7

---

## Acceptance Criteria

- [ ] **AC1:** `megabrain://repo/{name}` resource: repository metadata
- [ ] **AC2:** `megabrain://file/{path}` resource: file content
- [ ] **AC3:** `megabrain://entity/{id}` resource: entity details
- [ ] **AC4:** Resource subscriptions for updates
- [ ] **AC5:** 100+ concurrent subscriptions supported
- [ ] **AC6:** Resource list endpoint functional

---

## Demo Script

### Demo Steps
1. **List Resources:**
   ```json
   {"method": "resources/list"}
   ```
2. **Read Resource:**
   ```json
   {"method": "resources/read", "params": {"uri": "megabrain://repo/olexmal/MegaBrain"}}
   ```
3. **Subscribe:** Subscribe to repository updates
4. **Update Notification:** Show notification on change

### Expected Outcome
- Resources browsable
- Subscriptions work
- Updates notified

---

## Technical Tasks

- [ ] **T1:** Create `MCPResourceProvider` class (backend)
- [ ] **T2:** Implement repo resource handler (backend)
- [ ] **T3:** Implement file resource handler (backend)
- [ ] **T4:** Implement entity resource handler (backend)
- [ ] **T5:** Implement subscription manager (backend)
- [ ] **T6:** Add update notifications (backend)
- [ ] **T7:** Write resource tests (test)

---

## Dependencies

- **Blocked by:** US-08-01 (MCP server)
- **Enables:** Full MCP integration

---

## Definition of Ready

- [x] All criteria met

