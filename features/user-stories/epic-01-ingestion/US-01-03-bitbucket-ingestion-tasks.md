# Tasks for US-01-03: Bitbucket Repository Ingestion

## Story Reference
- **Epic:** EPIC-01 (Code Ingestion & Indexing)
- **Story:** US-01-03
- **Story Points:** 3
- **Sprint Target:** Sprint 2

## Task List

### T1: Implement BitbucketSourceControlClient class
- **Description:** Create the `BitbucketSourceControlClient` class that implements the `SourceControlClient` interface. This class will handle Bitbucket-specific operations for both Cloud and Server/Data Center editions. Support repository cloning, metadata fetching, and authentication.
- **Estimated Hours:** 5 hours
- **Assignee:** TBD
- **Status:** Not Started
- **Dependencies:** US-01-01 (reuses SourceControlClient interface pattern)
- **Acceptance Criteria:**
  - [ ] Class implements `SourceControlClient` interface
  - [ ] Class handles Bitbucket Cloud URLs (bitbucket.org/workspace/repo)
  - [ ] Class handles Bitbucket Server URLs (custom domain)
  - [ ] Error handling for invalid URLs and network failures
- **Technical Notes:** Use JGit for cloning operations. Abstract Cloud vs Server differences behind common interface.

### T2: Create Bitbucket Cloud API integration
- **Description:** Implement Bitbucket Cloud API v2 client integration to fetch repository metadata such as name, workspace, default branch, and latest commit SHA. Handle Bitbucket Cloud-specific authentication and rate limiting.
- **Estimated Hours:** 4 hours
- **Assignee:** TBD
- **Status:** Not Started
- **Dependencies:** T1 (needs BitbucketSourceControlClient structure)
- **Acceptance Criteria:**
  - [ ] Fetches repository name, workspace, branch information
  - [ ] Retrieves latest commit SHA
  - [ ] Handles Bitbucket Cloud API rate limiting
  - [ ] Works with app password authentication
- **Technical Notes:** Use Bitbucket REST API v2. Support app password authentication (username:app-password format). Handle rate limiting with appropriate backoff.

### T3: Create Bitbucket Server API integration
- **Description:** Implement Bitbucket Server/Data Center API integration to fetch repository metadata. Support both REST API v1 and v2 endpoints. Handle Server-specific authentication using personal access tokens.
- **Estimated Hours:** 4 hours
- **Assignee:** TBD
- **Status:** Not Started
- **Dependencies:** T1 (needs BitbucketSourceControlClient structure)
- **Acceptance Criteria:**
  - [ ] Fetches repository name, project, branch information
  - [ ] Retrieves latest commit SHA
  - [ ] Works with personal access token authentication
  - [ ] Supports custom Server base URL
- **Technical Notes:** Use Bitbucket Server REST API. Support both API v1 and v2. Handle authentication via Bearer token or basic auth with PAT.

### T4: Implement authentication for both platforms
- **Description:** Implement unified authentication handling for both Bitbucket Cloud (app passwords) and Bitbucket Server (personal access tokens). Abstract authentication differences behind common interface while supporting platform-specific requirements.
- **Estimated Hours:** 3 hours
- **Assignee:** TBD
- **Status:** Not Started
- **Dependencies:** T1, T2, T3 (needs client structure and API integrations)
- **Acceptance Criteria:**
  - [ ] Supports app password authentication (Cloud)
  - [ ] Supports personal access token authentication (Server)
  - [ ] Tokens stored securely (environment variables, vault)
  - [ ] Tokens never logged or exposed in error messages
  - [ ] Clear error messages for invalid/expired tokens
- **Technical Notes:** Use different authentication headers for Cloud vs Server. Cloud uses Basic Auth with app password, Server uses Bearer token or Basic Auth with PAT.

### T5: Write unit tests for BitbucketSourceControlClient
- **Description:** Create comprehensive unit tests for `BitbucketSourceControlClient` class. Test all major operations including cloning, metadata fetching, authentication for both Cloud and Server editions. Use mocking for external dependencies.
- **Estimated Hours:** 3 hours
- **Assignee:** TBD
- **Status:** Not Started
- **Dependencies:** T1-T4 (needs implementation to test)
- **Acceptance Criteria:**
  - [ ] Unit tests cover all public methods
  - [ ] Tests use mocks for external dependencies
  - [ ] Test coverage >80%
  - [ ] Tests include error scenarios
  - [ ] Tests cover both Cloud and Server scenarios
- **Technical Notes:** Use JUnit 5 and Mockito for testing. Test both success and failure paths. Include tests for Cloud vs Server API differences.

---

## Summary
- **Total Tasks:** 5
- **Total Estimated Hours:** 19 hours
- **Story Points:** 3 (1 SP ≈ 6.3 hours, aligns with estimate)

