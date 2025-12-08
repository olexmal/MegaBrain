# Tasks for US-05-04: Code Preview Panel

## Story Reference
- **Epic:** EPIC-05 (Web Dashboard)
- **Story:** US-05-04
- **Story Points:** 3
- **Sprint Target:** Sprint 5

## Task List

### T1: Create code preview component
- **Description:** Create Angular component for code preview panel. Component should display full file content with syntax highlighting. Support opening/closing panel. Show file metadata (path, repository, language).
- **Estimated Hours:** 4 hours
- **Assignee:** TBD
- **Status:** Not Started
- **Dependencies:** US-05-03 (needs search interface)
- **Acceptance Criteria:**
  - [ ] Code preview component created
  - [ ] Displays full file content
  - [ ] Panel open/close works
  - [ ] File metadata shown
- **Technical Notes:** Use Angular Material dialog or side panel. Fetch file content from API. Use syntax highlighting library.

### T2: Integrate syntax highlighting library
- **Description:** Integrate syntax highlighting library (Prism.js, highlight.js, or Monaco Editor) for code display. Support all languages used in codebase. Apply highlighting based on file extension or language metadata.
- **Estimated Hours:** 4 hours
- **Assignee:** TBD
- **Status:** Not Started
- **Dependencies:** T1 (needs preview component)
- **Acceptance Criteria:**
  - [ ] Syntax highlighting library integrated
  - [ ] All languages supported
  - [ ] Highlighting applied correctly
  - [ ] Performance acceptable
- **Technical Notes:** Install and configure highlighting library. Detect language from file extension or metadata. Apply highlighting to code content.

### T3: Add line numbers display
- **Description:** Implement line numbers display alongside code content. Show line numbers for each line. Highlight matched line range. Support scrolling with line numbers.
- **Estimated Hours:** 3 hours
- **Assignee:** TBD
- **Status:** Not Started
- **Dependencies:** T1, T2 (needs preview and highlighting)
- **Acceptance Criteria:**
  - [ ] Line numbers displayed
  - [ ] Matched lines highlighted
  - [ ] Scrolling works correctly
  - [ ] Line numbers aligned
- **Technical Notes:** Add line number column. Use CSS for alignment. Highlight matched line range with background color.

### T4: Implement copy to clipboard
- **Description:** Add copy to clipboard functionality for code content. Add copy button that copies selected code or entire file. Show success feedback after copy. Support keyboard shortcut (Ctrl+C).
- **Estimated Hours:** 2 hours
- **Assignee:** TBD
- **Status:** Not Started
- **Dependencies:** T1 (needs preview component)
- **Acceptance Criteria:**
  - [ ] Copy button implemented
  - [ ] Copies code to clipboard
  - [ ] Success feedback shown
  - [ ] Keyboard shortcut works
- **Technical Notes:** Use Clipboard API. Add copy button with icon. Show toast notification on success.

### T5: Add source repository link
- **Description:** Add link to source repository (GitHub, GitLab, etc.) in preview panel. Link should open repository file in new tab. Display repository name and file path. Support different source control providers.
- **Estimated Hours:** 2 hours
- **Assignee:** TBD
- **Status:** Not Started
- **Dependencies:** T1 (needs preview component)
- **Acceptance Criteria:**
  - [ ] Repository link added
  - [ ] Opens in new tab
  - [ ] Repository name displayed
  - [ ] Link format correct
- **Technical Notes:** Construct repository URL from metadata. Use Angular router or window.open. Display repository icon and name.

### T6: Write component tests
- **Description:** Create unit tests for code preview component. Test panel opening/closing, syntax highlighting, line numbers, copy functionality, and repository link. Use Angular testing utilities.
- **Estimated Hours:** 3 hours
- **Assignee:** TBD
- **Status:** Not Started
- **Dependencies:** T1-T5 (needs complete implementation)
- **Acceptance Criteria:**
  - [ ] Unit tests for preview component
  - [ ] Tests cover all features
  - [ ] Tests cover interactions
  - [ ] Test coverage >80%
- **Technical Notes:** Use Angular TestBed. Mock file content API. Test component interactions and functionality.

---

## Summary
- **Total Tasks:** 6
- **Total Estimated Hours:** 18 hours
- **Story Points:** 3 (1 SP ≈ 6 hours, aligns with estimate)

