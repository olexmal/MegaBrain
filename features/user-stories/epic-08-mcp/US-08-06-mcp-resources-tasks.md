# Tasks for US-08-06: MCP Resources Provider

## Story Reference
- **Epic:** EPIC-08 (MCP Tool Server)
- **Story:** US-08-06
- **Story Points:** 5
- **Sprint Target:** Sprint 7

## Task List

### T1: Create MCPResourceProvider class
- **Description:** Create `MCPResourceProvider` class that implements MCP resource provider interface. Class should handle resource URIs, resource reading, and resource subscriptions. Support megabrain:// URI scheme.
- **Estimated Hours:** 5 hours
- **Assignee:** TBD
- **Status:** Not Started
- **Dependencies:** US-08-01 (needs MCP server)
- **Acceptance Criteria:**
  - [ ] MCPResourceProvider created
  - [ ] Resource URI handling
  - [ ] Resource reading implemented
  - [ ] Resource subscriptions supported
- **Technical Notes:** Implement MCP resource provider interface. Handle megabrain:// URI scheme. Support resource reading and subscriptions. Manage resource lifecycle.

### T2: Implement repo resource handler
- **Description:** Implement repository resource handler for `megabrain://repo/{name}` URIs. Handler should return repository metadata (name, language, last indexed, file count). Support resource reading and subscriptions.
- **Estimated Hours:** 4 hours
- **Assignee:** TBD
- **Status:** Not Started
- **Dependencies:** T1 (needs MCPResourceProvider)
- **Acceptance Criteria:**
  - [ ] repo resource handler implemented
  - [ ] Returns repository metadata
  - [ ] Resource reading works
  - [ ] Subscriptions supported
- **Technical Notes:** Parse megabrain://repo/{name} URI. Query repository metadata from database. Return repository information. Support resource subscriptions for updates.

### T3: Implement file resource handler
- **Description:** Implement file resource handler for `megabrain://file/{path}` URIs. Handler should return file content, metadata, and entities. Support resource reading and subscriptions.
- **Estimated Hours:** 4 hours
- **Assignee:** TBD
- **Status:** Not Started
- **Dependencies:** T1 (needs MCPResourceProvider)
- **Acceptance Criteria:**
  - [ ] file resource handler implemented
  - [ ] Returns file content
  - [ ] Includes metadata
  - [ ] Subscriptions supported
- **Technical Notes:** Parse megabrain://file/{path} URI. Fetch file content from storage. Return file content with metadata (language, entities). Support subscriptions for file changes.

### T4: Implement entity resource handler
- **Description:** Implement entity resource handler for `megabrain://entity/{id}` URIs. Handler should return entity details (name, type, file, documentation, relationships). Support resource reading and subscriptions.
- **Estimated Hours:** 5 hours
- **Assignee:** TBD
- **Status:** Not Started
- **Dependencies:** T1 (needs MCPResourceProvider)
- **Acceptance Criteria:**
  - [ ] entity resource handler implemented
  - [ ] Returns entity details
  - [ ] Includes relationships
  - [ ] Subscriptions supported
- **Technical Notes:** Parse megabrain://entity/{id} URI. Query entity from database. Return entity details (name, type, file, documentation). Include relationships if available. Support subscriptions.

### T5: Implement subscription manager
- **Description:** Implement subscription manager that handles resource subscriptions. Manager should track active subscriptions, notify subscribers of updates, and support 100+ concurrent subscriptions. Handle subscription lifecycle.
- **Estimated Hours:** 6 hours
- **Assignee:** TBD
- **Status:** Not Started
- **Dependencies:** T2, T3, T4 (needs resource handlers)
- **Acceptance Criteria:**
  - [ ] Subscription manager implemented
  - [ ] Tracks active subscriptions
  - [ ] Notifies subscribers
  - [ ] Supports 100+ subscriptions
- **Technical Notes:** Create subscription manager. Track subscriptions by resource URI. Notify subscribers on resource updates. Use reactive streams for notifications. Handle subscription cleanup.

### T6: Add update notifications
- **Description:** Implement update notification system that notifies subscribers when resources change. Monitor resource changes (repository updates, file changes, entity modifications). Send notifications to subscribers.
- **Estimated Hours:** 5 hours
- **Assignee:** TBD
- **Status:** Not Started
- **Dependencies:** T5 (needs subscription manager)
- **Acceptance Criteria:**
  - [ ] Update notifications implemented
  - [ ] Resource changes detected
  - [ ] Subscribers notified
  - [ ] Notifications are timely
- **Technical Notes:** Monitor resource changes via ingestion events or database triggers. Detect changes to repositories, files, entities. Send notifications to subscribed clients. Use event-driven architecture.

### T7: Write resource tests
- **Description:** Create comprehensive tests for MCP resources. Test resource reading, subscriptions, update notifications, and concurrent subscriptions. Use MCP protocol test framework.
- **Estimated Hours:** 4 hours
- **Assignee:** TBD
- **Status:** Not Started
- **Dependencies:** T1-T6 (needs complete implementation)
- **Acceptance Criteria:**
  - [ ] Tests for resource reading
  - [ ] Tests for subscriptions
  - [ ] Tests for update notifications
  - [ ] Test coverage >80%
- **Technical Notes:** Use MCP test framework or create custom tests. Test resource URI parsing. Test resource reading. Test subscriptions and notifications. Verify concurrent subscription handling.

---

## Summary
- **Total Tasks:** 7
- **Total Estimated Hours:** 33 hours
- **Story Points:** 5 (1 SP ≈ 6.6 hours, aligns with estimate)

