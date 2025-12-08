# Tasks for US-05-02: Trigger Ingestion Form

## Story Reference
- **Epic:** EPIC-05 (Web Dashboard)
- **Story:** US-05-02
- **Story Points:** 3
- **Sprint Target:** Sprint 4

## Task List

### T1: Create ingestion form component
- **Description:** Create Angular form component for triggering new ingestion jobs. Form should include fields for source type, repository URL, branch, and optional credentials. Use Angular Reactive Forms for form management.
- **Estimated Hours:** 4 hours
- **Assignee:** TBD
- **Status:** Not Started
- **Dependencies:** US-05-01 (needs dashboard)
- **Acceptance Criteria:**
  - [ ] Form component created
  - [ ] All required fields included
  - [ ] Form validation implemented
  - [ ] User-friendly layout
- **Technical Notes:** Use Angular Reactive Forms with FormBuilder. Include source type dropdown, repository URL input, branch input, and optional token field.

### T2: Add form validation
- **Description:** Implement form validation for all fields. Validate repository URL format, ensure source type is selected, validate branch name format. Show validation errors clearly. Disable submit button when form is invalid.
- **Estimated Hours:** 3 hours
- **Assignee:** TBD
- **Status:** Not Started
- **Dependencies:** T1 (needs form component)
- **Acceptance Criteria:**
  - [ ] Validation rules implemented
  - [ ] Error messages displayed
  - [ ] Submit disabled when invalid
  - [ ] Clear error messages
- **Technical Notes:** Use Angular validators (required, pattern, custom). Display errors below fields or in tooltips. Use Material form field error messages.

### T3: Integrate with ingestion API
- **Description:** Integrate form submission with ingestion REST API. Call POST endpoint with form data. Handle success and error responses. Show loading state during submission. Navigate to dashboard on success.
- **Estimated Hours:** 3 hours
- **Assignee:** TBD
- **Status:** Not Started
- **Dependencies:** T1, T2 (needs form and validation), US-04-01 (needs API)
- **Acceptance Criteria:**
  - [ ] API integration implemented
  - [ ] Success handling works
  - [ ] Error handling works
  - [ ] Loading state shown
- **Technical Notes:** Use Angular HTTP client. Show loading spinner during submission. Display success message and navigate to dashboard. Show error message on failure.

### T4: Add source type selector
- **Description:** Implement source type dropdown selector with options: GitHub, GitLab, Bitbucket, Local. Update form fields based on selected source type. Show/hide relevant fields dynamically.
- **Estimated Hours:** 2 hours
- **Assignee:** TBD
- **Status:** Not Started
- **Dependencies:** T1 (needs form component)
- **Acceptance Criteria:**
  - [ ] Source type selector implemented
  - [ ] All source types available
  - [ ] Form updates based on selection
  - [ ] Clear visual indication
- **Technical Notes:** Use Angular Material select or dropdown. Use reactive forms valueChanges to update form fields. Show/hide fields with *ngIf.

### T5: Write form tests
- **Description:** Create unit tests for ingestion form component. Test form rendering, validation, API integration, error handling, and source type selection. Use Angular testing utilities.
- **Estimated Hours:** 3 hours
- **Assignee:** TBD
- **Status:** Not Started
- **Dependencies:** T1-T4 (needs complete implementation)
- **Acceptance Criteria:**
  - [ ] Unit tests for form component
  - [ ] Tests cover validation
  - [ ] Tests cover API integration
  - [ ] Test coverage >80%
- **Technical Notes:** Use Angular TestBed. Mock HTTP client. Test form interactions, validation, and submission.

---

## Summary
- **Total Tasks:** 5
- **Total Estimated Hours:** 15 hours
- **Story Points:** 3 (1 SP ≈ 5 hours, aligns with estimate)

