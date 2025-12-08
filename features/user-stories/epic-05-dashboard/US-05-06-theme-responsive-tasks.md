# Tasks for US-05-06: Theme and Responsive Design

## Story Reference
- **Epic:** EPIC-05 (Web Dashboard)
- **Story:** US-05-06
- **Story Points:** 2
- **Sprint Target:** Sprint 6

## Task List

### T1: Create theme service
- **Description:** Create Angular service for theme management. Service should handle theme switching (dark/light), theme persistence, and system preference detection. Provide observable for theme changes.
- **Estimated Hours:** 3 hours
- **Assignee:** TBD
- **Status:** Not Started
- **Dependencies:** None
- **Acceptance Criteria:**
  - [ ] Theme service created
  - [ ] Theme switching works
  - [ ] System preference detected
  - [ ] Observable for theme changes
- **Technical Notes:** Use Angular service with BehaviorSubject. Detect system preference via `prefers-color-scheme` media query. Store theme in local storage.

### T2: Define dark/light CSS variables
- **Description:** Define CSS custom properties (variables) for dark and light themes. Include colors for background, text, primary, secondary, and accent colors. Ensure good contrast ratios for accessibility.
- **Estimated Hours:** 3 hours
- **Assignee:** TBD
- **Status:** Not Started
- **Dependencies:** T1 (needs theme service)
- **Acceptance Criteria:**
  - [ ] CSS variables defined
  - [ ] Dark theme variables
  - [ ] Light theme variables
  - [ ] Good contrast ratios
- **Technical Notes:** Define CSS variables in global styles. Use `:root` for light theme, `[data-theme="dark"]` for dark theme. Include all color tokens.

### T3: Add theme toggle component
- **Description:** Create theme toggle component (button or switch) for switching between dark and light themes. Component should update theme service and show current theme. Place in header or navigation.
- **Estimated Hours:** 2 hours
- **Assignee:** TBD
- **Status:** Not Started
- **Dependencies:** T1, T2 (needs theme service and variables)
- **Acceptance Criteria:**
  - [ ] Theme toggle component created
  - [ ] Toggle switches theme
  - [ ] Current theme indicated
  - [ ] Accessible (keyboard, screen reader)
- **Technical Notes:** Use Angular Material toggle or icon button. Subscribe to theme service. Update theme on click. Show icon or label for current theme.

### T4: Implement local storage persistence
- **Description:** Implement local storage persistence for theme preference. Save selected theme to local storage. Load theme preference on app initialization. Handle storage errors gracefully.
- **Estimated Hours:** 2 hours
- **Assignee:** TBD
- **Status:** Not Started
- **Dependencies:** T1 (needs theme service)
- **Acceptance Criteria:**
  - [ ] Theme persisted to local storage
  - [ ] Theme loaded on startup
  - [ ] Storage errors handled
  - [ ] Preference maintained across sessions
- **Technical Notes:** Use Angular localStorage service or native localStorage API. Save theme on change. Load theme in app initialization.

### T5: Add responsive breakpoints
- **Description:** Implement responsive design with CSS breakpoints for different screen sizes. Ensure layout works on laptop (1024px+) and desktop (1920px+). Adjust component layouts for smaller screens.
- **Estimated Hours:** 4 hours
- **Assignee:** TBD
- **Status:** Not Started
- **Dependencies:** Base dashboard components
- **Acceptance Criteria:**
  - [ ] Responsive breakpoints defined
  - [ ] Layout adapts to screen size
  - [ ] Works on laptop and desktop
  - [ ] Components responsive
- **Technical Notes:** Use CSS media queries. Define breakpoints (mobile: 768px, tablet: 1024px, desktop: 1920px). Adjust grid, flexbox, and component layouts.

### T6: Write visual tests
- **Description:** Create visual regression tests or manual test checklist for theme and responsive design. Test theme switching, persistence, and responsive layouts on different screen sizes. Document test scenarios.
- **Estimated Hours:** 2 hours
- **Assignee:** TBD
- **Status:** Not Started
- **Dependencies:** T1-T5 (needs complete implementation)
- **Acceptance Criteria:**
  - [ ] Visual tests created
  - [ ] Theme switching tested
  - [ ] Responsive layouts tested
  - [ ] Test scenarios documented
- **Technical Notes:** Use visual regression testing tools (Percy, Chromatic) or manual testing checklist. Test on multiple screen sizes and browsers.

---

## Summary
- **Total Tasks:** 6
- **Total Estimated Hours:** 16 hours
- **Story Points:** 2 (1 SP ≈ 8 hours, aligns with estimate)

