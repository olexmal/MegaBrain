# Tasks for US-01-07: Real-Time Progress Streaming

## Story Reference
- **Epic:** EPIC-01 (Code Ingestion & Indexing)
- **Story:** US-01-07
- **Story Points:** 3
- **Sprint Target:** Sprint 2

## Task List

### T1: Define StreamEvent record/class
- **Description:** Create a `StreamEvent` data class/record to represent progress events during ingestion. Include fields: stage (enum), message (String), percentage (int), timestamp (Instant), and optional metadata. Make it serializable for SSE transmission.
- **Estimated Hours:** 2 hours
- **Assignee:** TBD
- **Status:** Not Started
- **Dependencies:** None
- **Acceptance Criteria:**
  - [ ] StreamEvent class/record defined
  - [ ] Includes stage, message, percentage, timestamp fields
  - [ ] Serializable to JSON for SSE
  - [ ] Immutable (if using record)
- **Technical Notes:** Use Java record for immutability. Include stage enum: CLONING, PARSING, INDEXING, COMPLETE, FAILED. Use Jackson for JSON serialization.

### T2: Implement SSE endpoint using Mutiny Multi
- **Description:** Create Server-Sent Events (SSE) endpoint using Quarkus and Mutiny's `Multi` reactive stream. Endpoint should accept ingestion request and return SSE stream of progress events. Handle client disconnections gracefully.
- **Estimated Hours:** 4 hours
- **Assignee:** TBD
- **Status:** Not Started
- **Dependencies:** T1 (needs StreamEvent), US-01-01 (needs ingestion service)
- **Acceptance Criteria:**
  - [ ] SSE endpoint returns `Multi<StreamEvent>`
  - [ ] Events streamed in real-time
  - [ ] Client disconnections handled gracefully
  - [ ] Proper SSE headers set (Content-Type: text/event-stream)
- **Technical Notes:** Use Quarkus RESTEasy Reactive with `@Produces(MediaType.SERVER_SENT_EVENTS)`. Use Mutiny's `Multi` for reactive streams. Handle backpressure appropriately.

### T3: Emit progress events during cloning
- **Description:** Integrate progress event emission into repository cloning process. Emit events at key milestones: cloning started, cloning in progress (with percentage), cloning completed. Calculate percentage based on clone progress if available.
- **Estimated Hours:** 3 hours
- **Assignee:** TBD
- **Status:** Not Started
- **Dependencies:** T1, T2 (needs StreamEvent and SSE endpoint), US-01-01 (needs cloning)
- **Acceptance Criteria:**
  - [ ] Event emitted when cloning starts
  - [ ] Progress events emitted during cloning (if progress available)
  - [ ] Event emitted when cloning completes
  - [ ] Percentage reflects cloning progress
- **Technical Notes:** Integrate with JGit's progress monitor if available. Emit events at 0%, 25%, 50%, 75%, 100% milestones. Use Mutiny's `Multi.createFrom().emitter()` for event emission.

### T4: Emit progress events during parsing
- **Description:** Integrate progress event emission into file parsing process. Emit events showing file-level progress (e.g., "Parsing file 50/200") and overall parsing percentage. Update events as each file is processed.
- **Estimated Hours:** 4 hours
- **Assignee:** TBD
- **Status:** Not Started
- **Dependencies:** T1, T2 (needs StreamEvent and SSE endpoint), US-01-04, US-01-05 (needs parsers)
- **Acceptance Criteria:**
  - [ ] Event emitted when parsing starts
  - [ ] Progress events show "Parsing file X/Y" format
  - [ ] Percentage calculated based on files processed
  - [ ] Event emitted when parsing completes
- **Technical Notes:** Track total file count and processed file count. Emit events every N files (e.g., every 10 files) or every 5 seconds, whichever comes first. Calculate percentage: (processed / total) * 100.

### T5: Emit progress events during indexing
- **Description:** Integrate progress event emission into indexing process. Emit events showing indexing progress (chunks indexed, percentage complete). Update events as chunks are added to the search index.
- **Estimated Hours:** 3 hours
- **Assignee:** TBD
- **Status:** Not Started
- **Dependencies:** T1, T2 (needs StreamEvent and SSE endpoint), US-02-01 (needs indexing)
- **Acceptance Criteria:**
  - [ ] Event emitted when indexing starts
  - [ ] Progress events show indexing progress
  - [ ] Percentage calculated based on chunks indexed
  - [ ] Event emitted when indexing completes
- **Technical Notes:** Track total chunk count and indexed chunk count. Emit events periodically (every N chunks or time interval). Calculate percentage based on indexing progress.

### T6: Emit completion/error events
- **Description:** Implement completion and error event emission. Emit COMPLETE event when ingestion finishes successfully with final statistics. Emit FAILED event with error details when ingestion fails at any stage.
- **Estimated Hours:** 3 hours
- **Assignee:** TBD
- **Status:** Not Started
- **Dependencies:** T1, T2 (needs StreamEvent and SSE endpoint)
- **Acceptance Criteria:**
  - [ ] COMPLETE event emitted on success
  - [ ] COMPLETE event includes final statistics (files processed, chunks created)
  - [ ] FAILED event emitted on error
  - [ ] FAILED event includes error message and stage where failure occurred
- **Technical Notes:** Include summary statistics in COMPLETE event (total files, total chunks, duration). Include error details in FAILED event (error message, exception type if applicable). Always close the stream after completion or failure.

### T7: Write integration test for SSE streaming
- **Description:** Create integration test that verifies SSE streaming works end-to-end. Test that events are received in correct order, progress percentages are accurate, and completion/error events are properly emitted. Test client reconnection scenarios.
- **Estimated Hours:** 3 hours
- **Assignee:** TBD
- **Status:** Not Started
- **Dependencies:** T1-T6 (needs complete implementation)
- **Acceptance Criteria:**
  - [ ] Integration test receives SSE events
  - [ ] Events received in correct order
  - [ ] Progress percentages verified
  - [ ] Completion event received on success
  - [ ] Error event received on failure
- **Technical Notes:** Use HTTP client that supports SSE (e.g., Java 11+ HttpClient or OkHttp). Parse SSE events and verify content. Test with real ingestion to verify end-to-end flow.

---

## Summary
- **Total Tasks:** 7
- **Total Estimated Hours:** 22 hours
- **Story Points:** 3 (1 SP ≈ 7.3 hours, aligns with estimate)

