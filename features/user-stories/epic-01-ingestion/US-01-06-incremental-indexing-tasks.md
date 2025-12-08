# Tasks for US-01-06: Incremental Git-Diff Indexing

## Story Reference
- **Epic:** EPIC-01 (Code Ingestion & Indexing)
- **Story:** US-01-06
- **Story Points:** 5
- **Sprint Target:** Sprint 3

## Task List

### T1: Store last indexed commit SHA per repository
- **Description:** Implement storage mechanism to track the last indexed commit SHA for each repository. Store this information in the database (PostgreSQL) with repository identifier as key. This enables incremental indexing by comparing current HEAD with last indexed commit.
- **Estimated Hours:** 3 hours
- **Assignee:** TBD
- **Status:** Not Started
- **Dependencies:** US-01-01 (needs repository ingestion working)
- **Acceptance Criteria:**
  - [ ] Last commit SHA stored per repository
  - [ ] SHA retrieved efficiently for comparison
  - [ ] SHA updated after successful indexing
  - [ ] Handles repository re-indexing (new SHA overwrites old)
- **Technical Notes:** Create database table or use existing repository metadata table. Store SHA as string. Include timestamp for tracking when last indexed.

### T2: Implement git diff detection for changed files
- **Description:** Implement git diff functionality using JGit to detect changed files between last indexed commit and current HEAD. Support diff operations: added, modified, deleted, renamed. Handle edge cases like branch switches and force pushes.
- **Estimated Hours:** 5 hours
- **Assignee:** TBD
- **Status:** Not Started
- **Dependencies:** T1 (needs commit SHA tracking)
- **Acceptance Criteria:**
  - [ ] Detects added files
  - [ ] Detects modified files
  - [ ] Detects deleted files
  - [ ] Detects renamed files (with similarity threshold)
  - [ ] Handles branch switches gracefully
- **Technical Notes:** Use JGit's `DiffCommand` and `RenameDetector`. Set similarity threshold for rename detection (default 50%). Handle large diffs efficiently.

### T3: Handle file additions in incremental mode
- **Description:** Implement logic to process newly added files during incremental indexing. Parse and index only the new files, skipping unchanged files. Update index and commit SHA after processing.
- **Estimated Hours:** 3 hours
- **Assignee:** TBD
- **Status:** Not Started
- **Dependencies:** T2 (needs diff detection), US-01-04, US-01-05 (needs parsers)
- **Acceptance Criteria:**
  - [ ] New files parsed and indexed
  - [ ] Only new files processed (others skipped)
  - [ ] Index updated with new chunks
  - [ ] Commit SHA updated after processing
- **Technical Notes:** Reuse existing parsing and indexing logic. Track which files were added for progress reporting. Ensure atomic updates to index.

### T4: Handle file modifications in incremental mode
- **Description:** Implement logic to process modified files during incremental indexing. Remove old chunks for the file, then re-parse and re-index with new content. Handle both content changes and metadata changes.
- **Estimated Hours:** 4 hours
- **Assignee:** TBD
- **Status:** Not Started
- **Dependencies:** T2 (needs diff detection), US-01-04, US-01-05 (needs parsers)
- **Acceptance Criteria:**
  - [ ] Old chunks for modified file removed from index
  - [ ] Modified file re-parsed and re-indexed
  - [ ] Index remains consistent after update
  - [ ] Commit SHA updated after processing
- **Technical Notes:** Use file path as key to identify chunks to remove. Perform removal and addition in transaction to maintain consistency. Handle cases where file structure changes significantly.

### T5: Handle file deletions in incremental mode
- **Description:** Implement logic to remove deleted files from the index during incremental indexing. Find all chunks associated with deleted file paths and remove them from both search index and database.
- **Estimated Hours:** 3 hours
- **Assignee:** TBD
- **Status:** Not Started
- **Dependencies:** T2 (needs diff detection)
- **Acceptance Criteria:**
  - [ ] Deleted files identified from git diff
  - [ ] All chunks for deleted files removed from index
  - [ ] Database cleaned up (chunks removed)
  - [ ] Commit SHA updated after processing
- **Technical Notes:** Query index and database for chunks matching deleted file paths. Perform batch deletion for efficiency. Ensure referential integrity (e.g., if using foreign keys).

### T6: Handle file renames in incremental mode
- **Description:** Implement logic to handle file renames during incremental indexing. Detect renames from git diff, remove old file chunks, and add new file chunks. Optionally preserve entity relationships if entity names unchanged.
- **Estimated Hours:** 4 hours
- **Assignee:** TBD
- **Status:** Not Started
- **Dependencies:** T2 (needs diff detection with rename detection), US-01-04, US-01-05 (needs parsers)
- **Acceptance Criteria:**
  - [ ] File renames detected from git diff
  - [ ] Old file chunks removed
  - [ ] New file chunks added
  - [ ] Entity relationships preserved if applicable
- **Technical Notes:** Use JGit's RenameDetector with similarity threshold. Handle both exact renames and content-similar renames. Consider preserving entity IDs if content unchanged.

### T7: Add incremental flag to ingestion API
- **Description:** Add `incremental` boolean parameter to ingestion API endpoints. When true, perform incremental indexing; when false or omitted, perform full indexing. Update API documentation and request DTOs.
- **Estimated Hours:** 2 hours
- **Assignee:** TBD
- **Status:** Not Started
- **Dependencies:** T1-T6 (needs incremental logic working)
- **Acceptance Criteria:**
  - [ ] `incremental` parameter added to API
  - [ ] Parameter defaults to false (full index)
  - [ ] API documentation updated
  - [ ] Request validation handles parameter
- **Technical Notes:** Update `IngestionRequest` DTO. Add parameter to REST endpoint. Update OpenAPI/Swagger documentation.

### T8: Write tests for each change type
- **Description:** Create comprehensive tests for incremental indexing covering all change types: file additions, modifications, deletions, and renames. Test edge cases like no changes, large number of changes, and mixed change types.
- **Estimated Hours:** 4 hours
- **Assignee:** TBD
- **Status:** Not Started
- **Dependencies:** T1-T7 (needs complete implementation)
- **Acceptance Criteria:**
  - [ ] Tests for file additions
  - [ ] Tests for file modifications
  - [ ] Tests for file deletions
  - [ ] Tests for file renames
  - [ ] Tests for no changes scenario
  - [ ] Tests for mixed change types
  - [ ] Test coverage >80%
- **Technical Notes:** Use test Git repositories with known commit history. Create commits with specific changes for testing. Verify index state before and after incremental runs.

---

## Summary
- **Total Tasks:** 8
- **Total Estimated Hours:** 28 hours
- **Story Points:** 5 (1 SP ≈ 5.6 hours, aligns with estimate)

