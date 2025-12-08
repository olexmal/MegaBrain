# Tasks for US-01-01: GitHub Repository Ingestion

## Story Reference
- **Epic:** EPIC-01 (Code Ingestion & Indexing)
- **Story:** US-01-01
- **Story Points:** 5
- **Sprint Target:** Sprint 1

## Task List

### T1: Implement GitHubSourceControlClient class
- **Description:** Create the `GitHubSourceControlClient` class that implements the `SourceControlClient` interface. This class will handle all GitHub-specific operations including repository cloning, metadata fetching, and authentication. The class should use JGit for Git operations and GitHub REST API for metadata retrieval.
- **Estimated Hours:** 6 hours
- **Assignee:** TBD
- **Status:** Not Started
- **Dependencies:** None (foundation task)
- **Acceptance Criteria:**
  - [ ] Class implements `SourceControlClient` interface
  - [ ] Class handles GitHub repository URLs (github.com/owner/repo format)
  - [ ] Class supports both public and private repositories
  - [ ] Error handling for invalid URLs and network failures
- **Technical Notes:** Use Quarkus CDI for dependency injection. Consider using GitHub API v4 (GraphQL) for more efficient metadata queries. JGit should be used for cloning operations.

### T2: Create GitHub API integration for metadata fetch
- **Description:** Implement GitHub API client integration to fetch repository metadata such as name, owner, default branch, and latest commit SHA. This should use GitHub REST API v3 or GraphQL API v4. Handle rate limiting and authentication properly.
- **Estimated Hours:** 4 hours
- **Assignee:** TBD
- **Status:** Not Started
- **Dependencies:** T1 (needs GitHubSourceControlClient structure)
- **Acceptance Criteria:**
  - [ ] Fetches repository name, owner, branch information
  - [ ] Retrieves latest commit SHA
  - [ ] Handles GitHub API rate limiting with backoff
  - [ ] Works with both authenticated and unauthenticated requests
- **Technical Notes:** Use Quarkus REST Client for API calls. Implement exponential backoff for rate limit handling. Cache metadata to reduce API calls.

### T3: Implement repository cloning via JGit
- **Description:** Implement repository cloning functionality using JGit library. Clone repositories to temporary storage, support branch specification, and handle large repositories efficiently. Clean up temporary directories after processing.
- **Estimated Hours:** 5 hours
- **Assignee:** TBD
- **Status:** Not Started
- **Dependencies:** T1 (needs GitHubSourceControlClient structure)
- **Acceptance Criteria:**
  - [ ] Clones repository to temporary directory
  - [ ] Supports branch specification (default: main/master)
  - [ ] Handles large repositories (>10K files) efficiently
  - [ ] Cleans up temporary directories after processing
  - [ ] Handles clone failures gracefully
- **Technical Notes:** Use JGit's `CloneCommand` with shallow clone option for faster cloning. Set appropriate timeout for large repositories. Use Mutiny for non-blocking operations.

### T4: Add file extraction from cloned repository
- **Description:** Implement file extraction logic that walks through the cloned repository directory and extracts all source files. Filter out binary files, ignore patterns (.gitignore), and extract file metadata (path, size, last modified).
- **Estimated Hours:** 4 hours
- **Assignee:** TBD
- **Status:** Not Started
- **Dependencies:** T3 (needs cloned repository)
- **Acceptance Criteria:**
  - [ ] Extracts all source files from repository
  - [ ] Filters out binary files and ignored patterns
  - [ ] Extracts file metadata (path, size, modification time)
  - [ ] Handles nested directory structures
- **Technical Notes:** Use Java NIO for efficient file walking. Respect .gitignore patterns. Consider using a library like `jgit-ignore` for proper ignore handling.

### T5: Implement token-based authentication
- **Description:** Implement GitHub token-based authentication for accessing private repositories. Support both personal access tokens (PAT) and GitHub App tokens. Securely store and manage tokens without logging them.
- **Estimated Hours:** 3 hours
- **Assignee:** TBD
- **Status:** Not Started
- **Dependencies:** T1, T2 (needs client structure and API integration)
- **Acceptance Criteria:**
  - [ ] Supports personal access tokens (PAT)
  - [ ] Supports GitHub App tokens
  - [ ] Tokens stored securely (environment variables, vault)
  - [ ] Tokens never logged or exposed in error messages
  - [ ] Clear error messages for invalid/expired tokens
- **Technical Notes:** Use Quarkus configuration for token management. Support both `Authorization: token` and `Authorization: Bearer` headers. Integrate with vault systems if available.

### T6: Add progress event emission
- **Description:** Implement progress event emission during repository cloning and file extraction. Emit events at key stages (cloning started, cloning progress, files extracted, etc.) using Mutiny's reactive streams. Events should include stage, message, and percentage.
- **Estimated Hours:** 3 hours
- **Assignee:** TBD
- **Status:** Not Started
- **Dependencies:** T3, T4 (needs cloning and extraction operations)
- **Acceptance Criteria:**
  - [ ] Emits progress events during cloning
  - [ ] Emits progress events during file extraction
  - [ ] Events include stage, message, and percentage
  - [ ] Events emitted at reasonable intervals (not too frequent)
- **Technical Notes:** Use Mutiny `Multi` for reactive event streams. Emit events at major milestones (10%, 25%, 50%, 75%, 100%). Consider throttling to avoid overwhelming clients.

### T7: Write unit tests for GitHubSourceControlClient
- **Description:** Create comprehensive unit tests for `GitHubSourceControlClient` class. Test all major operations including cloning, metadata fetching, authentication, and error handling. Use mocking for external dependencies (GitHub API, JGit).
- **Estimated Hours:** 4 hours
- **Assignee:** TBD
- **Status:** Not Started
- **Dependencies:** T1-T6 (needs implementation to test)
- **Acceptance Criteria:**
  - [ ] Unit tests cover all public methods
  - [ ] Tests use mocks for external dependencies
  - [ ] Test coverage >80%
  - [ ] Tests include error scenarios
- **Technical Notes:** Use JUnit 5 and Mockito for testing. Test both success and failure paths. Use Testcontainers if needed for integration-like tests.

### T8: Write integration test with real GitHub API
- **Description:** Create integration tests that interact with real GitHub API (using a test repository). Test end-to-end flow including cloning, metadata fetching, and file extraction. Use a dedicated test repository to avoid rate limiting issues.
- **Estimated Hours:** 3 hours
- **Assignee:** TBD
- **Status:** Not Started
- **Dependencies:** T1-T6 (needs complete implementation)
- **Acceptance Criteria:**
  - [ ] Integration test uses real GitHub repository
  - [ ] Test verifies complete ingestion flow
  - [ ] Test handles rate limiting gracefully
  - [ ] Test cleans up after execution
- **Technical Notes:** Use a dedicated test repository (e.g., `octocat/Hello-World`). Mark tests with `@IntegrationTest` annotation. Consider using GitHub Actions secrets for test tokens.

---

## Summary
- **Total Tasks:** 8
- **Total Estimated Hours:** 32 hours
- **Story Points:** 5 (1 SP ≈ 6.4 hours, aligns with estimate)

