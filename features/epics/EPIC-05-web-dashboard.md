# EPIC-05: Web Dashboard

## Epic Overview

| Attribute | Value |
|:----------|:------|
| **Epic ID** | EPIC-05 |
| **Priority** | High |
| **Estimated Scope** | L |
| **Dependencies** | EPIC-04 (REST API & CLI) |
| **Spec Reference** | Section 4.4 (FR-IFC-03) |
| **Status** | Planned |

## Business Value

The Web Dashboard provides a rich, interactive interface for users who prefer visual interaction over CLI/API:

- **Ingestion Dashboard:** Monitor indexing jobs in real-time
- **Search Interface:** Explore code with faceted filtering and result highlighting
- **RAG Chat Interface:** Interactive Q&A with streaming responses

This makes MegaBrain accessible to users who aren't comfortable with command-line tools.

---

## User Stories

### US-05-01: Ingestion Dashboard

**As a** system administrator, **I want** to monitor ingestion jobs in real-time, **so that** I can track progress and identify issues.

**Acceptance Criteria:**
- [ ] Dashboard shows list of active and recent ingestion jobs
- [ ] Each job displays: repository, status, progress percentage, duration
- [ ] Real-time progress updates via SSE
- [ ] Visual progress bar for each job
- [ ] Error details displayed for failed jobs
- [ ] Ability to cancel in-progress jobs
- [ ] Job history with filtering by status/date

**Spec Reference:** FR-IFC-03 (Ingestion Dashboard)

---

### US-05-02: Trigger Ingestion from UI

**As a** user, **I want** to start a new ingestion job from the dashboard, **so that** I don't need to use CLI or API.

**Acceptance Criteria:**
- [ ] "New Ingestion" button opens configuration form
- [ ] Form fields: source type, repository URL, branch, credentials
- [ ] Source type dropdown: GitHub, GitLab, Bitbucket, Local
- [ ] Validation of inputs before submission
- [ ] Job starts and appears in dashboard immediately
- [ ] Helpful error messages for invalid configurations

**Spec Reference:** FR-IFC-03 (Ingestion Dashboard)

---

### US-05-03: Search Interface

**As a** developer, **I want** a visual search interface with faceted filtering, **so that** I can explore code interactively.

**Acceptance Criteria:**
- [ ] Search box with type-ahead suggestions
- [ ] Facet filters: language, repository, entity type
- [ ] Results displayed with syntax-highlighted snippets
- [ ] Click result to expand full context
- [ ] Pagination for large result sets
- [ ] Sort options: relevance, file path, date
- [ ] Clear filters button
- [ ] Search history (recent searches)

**Spec Reference:** FR-IFC-03 (Search Interface)

---

### US-05-04: Code Preview

**As a** developer, **I want** to preview code in context from search results, **so that** I can understand the code without leaving the dashboard.

**Acceptance Criteria:**
- [ ] Click search result opens code preview panel
- [ ] Full file content displayed with matched section highlighted
- [ ] Syntax highlighting for all supported languages
- [ ] Line numbers displayed
- [ ] Copy code button
- [ ] Link to source repository (if available)
- [ ] Navigate to related entities (calls, implements, etc.)

**Spec Reference:** FR-IFC-03 (Search Interface)

---

### US-05-05: RAG Chat Interface

**As a** developer, **I want** an interactive chat interface for Q&A, **so that** I can ask questions conversationally.

**Acceptance Criteria:**
- [ ] Chat input box at bottom of screen
- [ ] Messages displayed in conversation format
- [ ] Streaming responses appear token-by-token
- [ ] Source citations displayed with links
- [ ] Click citation to view code in preview
- [ ] Conversation history preserved in session
- [ ] Clear conversation button
- [ ] Loading indicator during generation

**Spec Reference:** FR-IFC-03 (RAG Chat Interface)

---

### US-05-06: Responsive Design

**As a** user, **I want** the dashboard to work on different screen sizes, **so that** I can use it on my laptop or monitor.

**Acceptance Criteria:**
- [ ] Layout adapts to screen width (responsive breakpoints)
- [ ] Usable on 13" laptop screens
- [ ] Optimal experience on 24"+ monitors
- [ ] Mobile view functional (read-only acceptable)
- [ ] Touch-friendly controls where applicable

**Spec Reference:** Implicit UX requirement

---

### US-05-07: Dark/Light Theme

**As a** developer, **I want** to choose between dark and light themes, **so that** the UI matches my preference.

**Acceptance Criteria:**
- [ ] Theme toggle in settings/header
- [ ] Dark theme with appropriate contrast
- [ ] Light theme with appropriate contrast
- [ ] Theme preference persisted in local storage
- [ ] System preference detection as default

**Spec Reference:** Implicit UX requirement

---

## Technical Notes

### Key Components
- **Angular 20 Application:** Standalone components architecture
- **Modules:** Ingestion, Search, Chat
- **Services:** API client services for each backend endpoint

### Technology Stack
| Component | Technology |
|:----------|:-----------|
| Framework | Angular 20 (Standalone Components) |
| UI Components | Angular Material or PrimeNG |
| State Management | Angular Signals / RxJS |
| Syntax Highlighting | Prism.js or Highlight.js |
| SSE Client | EventSource API |
| Build | Angular CLI, esbuild |

### Application Structure
```
src/
├── app/
│   ├── core/
│   │   ├── services/
│   │   │   ├── api.service.ts
│   │   │   ├── ingestion.service.ts
│   │   │   ├── search.service.ts
│   │   │   └── rag.service.ts
│   │   └── interceptors/
│   ├── features/
│   │   ├── ingestion/
│   │   │   ├── ingestion-dashboard.component.ts
│   │   │   └── ingestion-form.component.ts
│   │   ├── search/
│   │   │   ├── search-page.component.ts
│   │   │   ├── search-results.component.ts
│   │   │   └── code-preview.component.ts
│   │   └── chat/
│   │       ├── chat-page.component.ts
│   │       └── message.component.ts
│   └── shared/
│       └── components/
├── assets/
└── styles/
```

### Deployment
- Built as static SPA
- Served from Quarkus static resources (`META-INF/resources`)
- Single deployment artifact with backend
- Optional: CDN deployment for assets

---

## Risks & Mitigations

| Risk | Impact | Likelihood | Mitigation |
|:-----|:-------|:-----------|:-----------|
| SSE connection instability | Lost updates | Medium | Reconnection logic; polling fallback |
| Large search results slow rendering | Poor UX | Medium | Virtual scrolling; pagination |
| Syntax highlighting performance | Lag on large files | Medium | Lazy highlighting; web workers |
| Cross-browser compatibility | Feature gaps | Low | Modern browsers only; polyfills |

---

## Non-Functional Requirements

| NFR | Target | Validation |
|:----|:-------|:-----------|
| Initial load time | <3s on 3G | Lighthouse audit |
| Bundle size | <500KB gzipped | Build analysis |
| Accessibility | WCAG 2.1 AA | Accessibility audit |
| Browser support | Chrome, Firefox, Safari, Edge (latest 2 versions) | Manual testing |

---

## Definition of Done

- [ ] All user stories complete and accepted
- [ ] Ingestion dashboard with real-time updates
- [ ] Search interface with faceted filtering
- [ ] Chat interface with streaming responses
- [ ] Responsive design validated
- [ ] Dark/light theme implemented
- [ ] Accessibility audit passed
- [ ] Unit tests for services (>80% coverage)
- [ ] E2E tests for critical flows
- [ ] Performance NFRs met
- [ ] Documentation for deployment

---

## Open Questions

1. Should we support keyboard shortcuts for power users?
2. Do we need offline support (PWA)?
3. Should search results be shareable via URL?
4. Do we need user authentication in the UI?

---

**Epic Owner:** TBD  
**Created:** December 2025  
**Last Updated:** December 2025

