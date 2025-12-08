# Tasks for US-05-01: Ingestion Dashboard View

## Story Reference
- **Epic:** EPIC-05 (Web Dashboard)
- **Story:** US-05-01
- **Story Points:** 5
- **Sprint Target:** Sprint 4

## Task List

### T1: Create ingestion dashboard component
- **Description:** Create main ingestion dashboard Angular component that displays ingestion jobs. Component should show list of active and recent jobs with their status, progress, and metadata. Use Angular Material or similar UI library for layout.
- **Estimated Hours:** 4 hours
- **Assignee:** TBD
- **Status:** Not Started
- **Dependencies:** US-04-01 (needs ingestion API)
- **Acceptance Criteria:**
  - [ ] Dashboard component created
  - [ ] Displays list of ingestion jobs
  - [ ] Shows job status and progress
  - [ ] Responsive layout
- **Technical Notes:** Use Angular component with Material table or list. Fetch jobs from API endpoint. Display in card or table format.

### T2: Create job list component
- **Description:** Create reusable job list component that displays individual ingestion jobs. Each job item should show repository name, status, progress percentage, duration, and action buttons. Support filtering and sorting.
- **Estimated Hours:** 4 hours
- **Assignee:** TBD
- **Status:** Not Started
- **Dependencies:** T1 (needs dashboard component)
- **Acceptance Criteria:**
  - [ ] Job list component created
  - [ ] Displays job metadata
  - [ ] Supports filtering and sorting
  - [ ] Clickable for details
- **Technical Notes:** Create Angular component with Material list or table. Include status badges, progress indicators, and action buttons.

### T3: Implement SSE service for progress
- **Description:** Create Angular service that connects to Server-Sent Events (SSE) endpoint for real-time progress updates. Service should handle connection, event parsing, and error recovery. Emit progress events to components via RxJS observables.
- **Estimated Hours:** 5 hours
- **Assignee:** TBD
- **Status:** Not Started
- **Dependencies:** T1 (needs dashboard), US-04-01 (needs SSE endpoint)
- **Acceptance Criteria:**
  - [ ] SSE service created
  - [ ] Connects to progress endpoint
  - [ ] Parses progress events
  - [ ] Emits events via observables
- **Technical Notes:** Use EventSource API or Angular HTTP client with SSE support. Handle reconnection on disconnect. Use RxJS Subject or BehaviorSubject for event emission.

### T4: Create progress bar component
- **Description:** Create reusable progress bar component that displays ingestion progress visually. Component should show percentage, stage name, and animated progress bar. Support different states (active, completed, failed).
- **Estimated Hours:** 3 hours
- **Assignee:** TBD
- **Status:** Not Started
- **Dependencies:** T2 (needs job list)
- **Acceptance Criteria:**
  - [ ] Progress bar component created
  - [ ] Shows percentage and stage
  - [ ] Animated progress updates
  - [ ] Different states supported
- **Technical Notes:** Use Angular Material progress bar or custom component. Update progress in real-time from SSE events. Use CSS animations for smooth updates.

### T5: Add job history view with filters
- **Description:** Implement job history view that shows completed and failed ingestion jobs. Add filters for status (completed, failed, cancelled) and date range. Support pagination for large history lists.
- **Estimated Hours:** 5 hours
- **Assignee:** TBD
- **Status:** Not Started
- **Dependencies:** T1, T2 (needs dashboard and job list)
- **Acceptance Criteria:**
  - [ ] Job history view implemented
  - [ ] Filters for status and date
  - [ ] Pagination supported
  - [ ] Error details accessible
- **Technical Notes:** Add filter controls (dropdowns, date pickers). Query API with filter parameters. Display history in table or list format. Show error details in modal or expandable section.

### T6: Write component tests
- **Description:** Create unit tests for ingestion dashboard components. Test component rendering, SSE service integration, progress updates, filtering, and error handling. Use Angular testing utilities.
- **Estimated Hours:** 4 hours
- **Assignee:** TBD
- **Status:** Not Started
- **Dependencies:** T1-T5 (needs complete implementation)
- **Acceptance Criteria:**
  - [ ] Unit tests for all components
  - [ ] Tests cover SSE integration
  - [ ] Tests cover filtering
  - [ ] Test coverage >80%
- **Technical Notes:** Use Angular TestBed and Jasmine/Karma. Mock SSE service and API calls. Test component interactions and state changes.

---

## Summary
- **Total Tasks:** 6
- **Total Estimated Hours:** 25 hours
- **Story Points:** 5 (1 SP ≈ 5 hours, aligns with estimate)

