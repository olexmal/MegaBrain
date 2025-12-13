# Tasks for US-01-02: GitLab Repository Ingestion

## Story Reference
- **Epic:** EPIC-01 (Code Ingestion & Indexing)
- **Story:** US-01-02
- **Story Points:** 3
- **Sprint Target:** Sprint 1

## Task List

### T1: Implement GitLabSourceControlClient class
- **Description:** Create the `GitLabSourceControlClient` class that implements the `SourceControlClient` interface, following the same pattern as GitHub client. This class will handle GitLab-specific operations including repository cloning, metadata fetching, and authentication for both gitlab.com and self-hosted instances.
- **Estimated Hours:** 5 hours
- **Assignee:** TBD
- **Status:** Not Started
- **Dependencies:** US-01-01 (reuses SourceControlClient interface pattern)
- **Acceptance Criteria:**
  - [x] Class implements `SourceControlClient` interface
  - [x] Class handles GitLab repository URLs (gitlab.com/namespace/repo format)
  - [x] Class supports both public and private repositories
  - [x] Error handling for invalid URLs and network failures
- **Technical Notes:** Reuse JGit for cloning operations. Follow same architectural pattern as GitHub client for consistency.

### T2: Create GitLab API integration for metadata
- **Description:** Implement GitLab API client integration to fetch repository metadata such as name, namespace, default branch, and latest commit SHA. Support both GitLab.com (API v4) and self-hosted instances. Handle rate limiting and authentication properly.
- **Estimated Hours:** 4 hours
- **Assignee:** TBD
- **Status:** Not Started
- **Dependencies:** T1 (needs GitLabSourceControlClient structure)
- **Acceptance Criteria:**
  - [x] Fetches repository name, namespace, branch information
  - [x] Retrieves latest commit SHA
  - [x] Handles GitLab API rate limiting with backoff
  - [x] Works with both authenticated and unauthenticated requests
- **Technical Notes:** Use GitLab REST API v4. Support custom base URL for self-hosted instances. Implement exponential backoff for rate limit handling.

### T3: Add self-hosted GitLab URL configuration
- **Description:** Implement configuration support for self-hosted GitLab instances. Allow users to specify custom base URL for GitLab API endpoints. Support custom SSL certificate validation for enterprise deployments.
- **Estimated Hours:** 3 hours
- **Assignee:** TBD
- **Status:** Not Started
- **Dependencies:** T1, T2 (needs client structure and API integration)
- **Acceptance Criteria:**
  - [x] Supports custom GitLab base URL configuration
  - [x] Handles self-hosted instance authentication
  - [x] Supports custom CA certificates for SSL validation
  - [x] Clear error messages for connection failures
- **Technical Notes:** Use Quarkus configuration for base URL. Support environment variables and application.properties. Consider SSL context customization for custom certificates.

### T4: Implement token-based authentication
- **Description:** Implement GitLab token-based authentication for accessing private repositories. Support both personal access tokens and project access tokens. Handle token validation and error responses.
- **Estimated Hours:** 2 hours
- **Assignee:** TBD
- **Status:** Not Started
- **Dependencies:** T1, T2 (needs client structure and API integration)
- **Acceptance Criteria:**
  - [x] Supports personal access tokens
  - [x] Supports project access tokens
  - [x] Tokens stored securely (environment variables, vault)
  - [x] Tokens never logged or exposed in error messages
  - [x] Clear error messages for invalid/expired tokens
- **Technical Notes:** Use `PRIVATE-TOKEN` header for GitLab API authentication. Support both read and read_repository scopes.

### T5: Write unit tests for GitLabSourceControlClient
- **Description:** Create comprehensive unit tests for `GitLabSourceControlClient` class. Test all major operations including cloning, metadata fetching, authentication, and error handling. Use mocking for external dependencies (GitLab API, JGit).
- **Estimated Hours:** 3 hours
- **Assignee:** TBD
- **Status:** Not Started
- **Dependencies:** T1-T4 (needs implementation to test)
- **Acceptance Criteria:**
  - [ ] Unit tests cover all public methods
  - [ ] Tests use mocks for external dependencies
  - [ ] Test coverage >80%
  - [ ] Tests include error scenarios
  - [ ] Tests cover both gitlab.com and self-hosted scenarios
- **Technical Notes:** Use JUnit 5 and Mockito for testing. Test both success and failure paths. Include tests for self-hosted URL handling.

---

## Summary
- **Total Tasks:** 5
- **Total Estimated Hours:** 17 hours
- **Story Points:** 3 (1 SP ≈ 5.7 hours, aligns with estimate)

