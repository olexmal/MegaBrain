# MegaBrain Product Backlog

This directory contains sprint-ready user stories organized by epic. Each story is designed to be demonstrable at the end of a sprint.

## Progress Summary

| Metric | Value |
|:-------|:------|
| Stories completed | 11 of 52 (21%) |
| Stories partially complete | 1 |
| Story points completed | 98 of 211 (46%) |
| Epics with all stories done | EPIC-02 (Search) |

## Backlog Overview

| Epic | Stories | Done | Total Points | Points Done | Sprints |
|:-----|:--------|:-----|:-------------|:------------|:--------|
| [EPIC-00: Infrastructure](epic-00-infrastructure/) | 1 | 1 | 3 | 3 | Pre-Sprint 1 |
| [EPIC-01: Ingestion](epic-01-ingestion/) | 8 | 4 | 37 | 23 | 1-3 |
| [EPIC-02: Search](epic-02-search/) | 6 | 6 | 28 | 28 | 2-5 |
| [EPIC-03: RAG](epic-03-rag/) | 5 | 0* | 19 | 0* | 4-5 |
| [EPIC-04: API & CLI](epic-04-api-cli/) | 6 | 0 | 16 | 0 | 2-5 |
| [EPIC-05: Dashboard](epic-05-dashboard/) | 6 | 0 | 23 | 0 | 4-6 |
| [EPIC-06: Dependency](epic-06-dependency/) | 7 | 0 | 32 | 0 | 3-6 |
| [EPIC-07: Documentation](epic-07-documentation/) | 6 | 0 | 22 | 0 | 3-6 |
| [EPIC-08: MCP](epic-08-mcp/) | 7 | 0 | 31 | 0 | 5-7 |
| **Total** | **52** | **11** | **211** | **54** | **7 sprints** |

*US-03-01 is partially complete (2 of 6 tasks done).

## Sprint Planning

### Pre-Sprint 1: Project Setup
**Velocity Target:** 3 points | **Demo Theme:** "Project Infrastructure Ready"

| Story | Points | Epic | Status |
|:------|:-------|:-----|:-------|
| US-00-01: Maven Project Setup with Dependencies | 3 | Infrastructure | Done |

**Sprint Demo:**
- Show Maven project structure
- Verify all dependencies resolve
- Demonstrate project compiles and runs
- Health endpoint responds

---

### Sprint 1: Foundation
**Velocity Target:** 13 points | **Demo Theme:** "First Code Indexed"

| Story | Points | Epic | Status |
|:------|:-------|:-----|:-------|
| US-01-01: GitHub Repository Ingestion | 5 | Ingestion | Done |
| US-01-02: GitLab Repository Ingestion | 3 | Ingestion | Pending |
| US-01-04: Java Parsing with JavaParser | 5 | Ingestion | Done |

**Sprint Demo:**
- Ingest a Java repository from GitHub
- Show parsed classes and methods in database
- Display ingestion progress

---

### Sprint 2: Multi-Source & Search
**Velocity Target:** 19 points | **Demo Theme:** "Search Your Code"

| Story | Points | Epic | Status |
|:------|:-------|:-----|:-------|
| US-01-03: Bitbucket Repository Ingestion | 3 | Ingestion | Pending |
| US-01-05: Tree-sitter Multi-Language Parsing | 8 | Ingestion | Done |
| US-01-07: Real-Time Progress Streaming | 3 | Ingestion | Pending |
| US-02-01: Lucene Keyword Search | 5 | Search | Done |

**Sprint Demo:**
- Ingest Python/JS repository via Tree-sitter
- Show real-time progress in terminal
- Search for class names and get results

---

### Sprint 3: Advanced Search & Graph
**Velocity Target:** 31 points | **Demo Theme:** "Smart Search"

| Story | Points | Epic | Status |
|:------|:-------|:-----|:-------|
| US-01-06: Incremental Git-Diff Indexing | 5 | Ingestion | Pending |
| US-01-08: Dynamic Grammar Management | 5 | Ingestion | Done |
| US-02-02: Vector Similarity Search | 8 | Search | Done |
| US-02-03: Hybrid Ranking Algorithm | 5 | Search | Done |
| US-02-04: Metadata Facet Filtering | 3 | Search | Done |
| US-06-01: Entity Relationship Extraction | 8 | Dependency | Pending |
| US-06-02: Neo4j Graph Storage | 5 | Dependency | Pending |
| US-07-01: Javadoc/JSDoc Extraction | 5 | Documentation | Pending |

**Sprint Demo:**
- Semantic search finds conceptually similar code
- Filter by language and repository
- Show dependency graph in Neo4j browser

---

### Sprint 4: RAG & UI Basics
**Velocity Target:** 30 points | **Demo Theme:** "Ask Your Code"

| Story | Points | Epic | Status |
|:------|:-------|:-----|:-------|
| US-02-05: Relevance Tuning Configuration | 2 | Search | Done |
| US-03-01: Ollama Local LLM Integration | 5 | RAG | Partial |
| US-03-02: OpenAI Cloud LLM Integration | 3 | RAG | Pending |
| US-03-03: Context-Aware Prompt Construction | 5 | RAG | Pending |
| US-03-04: SSE Token Streaming | 3 | RAG | Pending |
| US-04-05: CLI Search Command | 2 | API & CLI | Pending |
| US-05-01: Ingestion Dashboard View | 5 | Dashboard | Pending |
| US-05-02: Trigger Ingestion Form | 3 | Dashboard | Pending |
| US-06-03: Incoming Dependency Queries | 3 | Dependency | Pending |
| US-06-04: Outgoing Dependency Queries | 3 | Dependency | Pending |
| US-06-05: Inheritance Hierarchy Queries | 3 | Dependency | Pending |
| US-07-02: Python Docstring Extraction | 3 | Documentation | Pending |
| US-07-03: Documentation Indexing | 3 | Documentation | Pending |

**Sprint Demo:**
- Ask "How does authentication work?" and get streaming answer
- Dashboard shows active ingestion jobs
- "What calls this function?" returns results

---

### Sprint 5: Full Pipeline
**Velocity Target:** 29 points | **Demo Theme:** "End-to-End RAG"

| Story | Points | Epic | Status |
|:------|:-------|:-----|:-------|
| US-02-06: Transitive Search Integration | 5 | Search | Done |
| US-03-05: Source Attribution in Answers | 3 | RAG | Pending |
| US-04-03: RAG REST Endpoint | 3 | API & CLI | Pending |
| US-04-06: CLI Ask Command | 2 | API & CLI | Pending |
| US-05-03: Search Results Interface | 5 | Dashboard | Pending |
| US-05-04: Code Preview Panel | 3 | Dashboard | Pending |
| US-06-06: Impact Analysis Report | 5 | Dependency | Pending |
| US-07-04: Coverage Metrics Dashboard | 5 | Documentation | Pending |
| US-07-05: Quality Alerts | 3 | Documentation | Pending |
| US-08-01: MCP Server Core Implementation | 8 | MCP | Pending |

**Sprint Demo:**
- Full RAG flow: question → search → LLM → cited answer
- "Find all implementations of interface X" (transitive)
- Impact analysis: "If I change this, what breaks?"

---

### Sprint 6: Enhanced Features
**Velocity Target:** 28 points | **Demo Theme:** "LLM Native Tools"

| Story | Points | Epic | Status |
|:------|:-------|:-----|:-------|
| US-05-05: RAG Chat Interface | 5 | Dashboard | Pending |
| US-05-06: Theme and Responsive Design | 2 | Dashboard | Pending |
| US-06-07: Dead Code Detection | 5 | Dependency | Pending |
| US-07-06: Code Example Extraction | 3 | Documentation | Pending |
| US-08-02: Code Search MCP Tools | 5 | MCP | Pending |
| US-08-03: Dependency Analysis MCP Tools | 5 | MCP | Pending |
| US-08-04: Documentation MCP Tools | 3 | MCP | Pending |
| US-08-05: RAG Query MCP Tool | 3 | MCP | Pending |

**Sprint Demo:**
- Chat interface with streaming responses
- MCP tools working in test client
- Dead code report for repository

---

### Sprint 7: Polish & Integration
**Velocity Target:** 7 points | **Demo Theme:** "Production Ready"

| Story | Points | Epic | Status |
|:------|:-------|:-----|:-------|
| US-08-06: MCP Resources Provider | 5 | MCP | Pending |
| US-08-07: Cursor Integration Guide | 2 | MCP | Pending |

**Sprint Demo:**
- MegaBrain tools working in Cursor IDE
- Complete user documentation
- Production deployment walkthrough

---

## Story States

| State | Description |
|:------|:------------|
| 📋 Backlog | Not yet refined |
| ✅ Ready | Definition of Ready complete |
| 🚧 In Progress | Active development |
| 👀 In Review | Code review / QA |
| ✔️ Done | Accepted in sprint demo |

## Definition of Ready Checklist

A story is ready for sprint when:
- [ ] User story clearly written (As a... I want... So that...)
- [ ] Acceptance criteria defined and testable
- [ ] Story points estimated by team
- [ ] Dependencies identified and unblocked
- [ ] Technical tasks broken down
- [ ] Test scenarios documented
- [ ] Demo script approved by PO
- [ ] No blocking questions

## Definition of Done Checklist

A story is done when:
- [ ] All acceptance criteria met
- [ ] Code reviewed and merged
- [ ] Unit tests passing (>80% coverage)
- [ ] Integration tests passing
- [ ] Demo successfully presented
- [ ] Documentation updated
- [ ] No critical bugs

---

**Last Updated:** February 2026  
**Product Owner:** TBD  
**Scrum Master:** TBD

