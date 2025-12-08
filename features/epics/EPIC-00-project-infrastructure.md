# EPIC-00: Project Infrastructure Setup

## Epic Overview

| Attribute | Value |
|:----------|:------|
| **Epic ID** | EPIC-00 |
| **Priority** | Critical |
| **Estimated Scope** | S |
| **Dependencies** | None (Pre-requisite for all epics) |
| **Spec Reference** | Foundation Setup |
| **Status** | Planned |

## Business Value

This is the **pre-requisite epic** that must be completed before any development work can begin. Without a properly configured project infrastructure, no features can be implemented. This epic establishes:

- Maven project structure with all required dependencies
- Build and test infrastructure
- Basic project organization and package structure
- Development environment setup
- Foundation for all subsequent development work

---

## User Stories

### US-00-01: Maven Project Setup with Dependencies

**As a** developer  
**I want** a properly configured Maven project with all required dependencies and project structure  
**So that** I can start implementing features without infrastructure blockers

**Acceptance Criteria:**
- [ ] Maven project structure created (pom.xml, src/main/java, src/test/java, etc.)
- [ ] Quarkus 3.15+ configured with required extensions
- [ ] All core dependencies added (JavaParser, Tree-sitter, Lucene, Neo4j, LangChain4j, etc.)
- [ ] Basic package structure created (io.megabrain.*)
- [ ] Application configuration files (application.properties) created
- [ ] Build and test infrastructure working
- [ ] Project compiles and runs (hello world/health endpoint)

**Spec Reference:** Foundation Setup

---

## Technical Notes

### Key Components
- **Maven Project Structure:** Standard Maven directory layout with Quarkus conventions
- **Dependency Management:** All required libraries configured in pom.xml
- **Build Configuration:** Maven compiler, surefire, and Quarkus plugins configured
- **Package Structure:** Organized by feature areas (core, ingestion, search, rag, api, cli, config)

### Technology Stack
| Component | Technology |
|:----------|:-----------|
| Build Tool | Maven 3.8+ |
| Java Version | Java 21+ |
| Framework | Quarkus 3.15+ |
| Code Parsing | JavaParser 3.x, java-tree-sitter |
| Search | Apache Lucene |
| Graph Database | Neo4j Java Driver 5.x |
| LLM Integration | Quarkus LangChain4j Extension |
| Git Operations | JGit |
| CLI | Picocli with Quarkus Integration |
| Scheduling | Quartz Scheduler |
| Database | PostgreSQL JDBC Driver |
| Testing | JUnit 5, Mockito, Testcontainers |

### Architecture Considerations
- Follow Quarkus project structure conventions
- Use Quarkus BOM for dependency version management
- Organize packages by feature/epic boundaries
- Configure UTF-8 encoding throughout
- Set up proper test directory structure
- Enable Quarkus dev mode for rapid development

### Project Structure
```
megabrain/
├── pom.xml
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── io/megabrain/
│   │   │       ├── core/
│   │   │       ├── ingestion/
│   │   │       ├── search/
│   │   │       ├── rag/
│   │   │       ├── api/
│   │   │       ├── cli/
│   │   │       └── config/
│   │   └── resources/
│   │       └── application.properties
│   └── test/
│       ├── java/
│       │   └── io/megabrain/
│       └── resources/
└── README.md
```

---

## Risks & Mitigations

| Risk | Impact | Likelihood | Mitigation |
|:-----|:-------|:-----------|:-----------|
| Dependency version conflicts | Build failures | Medium | Use Quarkus BOM; test dependency resolution |
| Missing required dependencies | Development blockers | High | Comprehensive dependency list; verify all are included |
| Incorrect project structure | Development inefficiency | Low | Follow Quarkus conventions; validate structure |
| Build configuration errors | Cannot compile/run | Medium | Test build early; verify all plugins configured |

---

## Non-Functional Requirements

| NFR | Target | Validation |
|:----|:-------|:-----------|
| Build time | <30 seconds for clean compile | Measure `mvn clean compile` |
| Test execution | <10 seconds for empty test suite | Measure `mvn test` |
| Startup time | <2 seconds for Quarkus dev mode | Measure `mvn quarkus:dev` startup |
| Dependency resolution | All dependencies resolve without conflicts | Verify `mvn dependency:tree` |

---

## Definition of Done

- [ ] Maven project structure created and validated
- [ ] All required dependencies added and resolved
- [ ] Project compiles successfully (`mvn clean compile`)
- [ ] Test infrastructure works (`mvn test`)
- [ ] Quarkus dev mode starts successfully (`mvn quarkus:dev`)
- [ ] Health endpoint responds at `/q/health`
- [ ] Package structure created and organized
- [ ] Application.properties configured with basic settings
- [ ] Documentation updated (README, setup instructions)
- [ ] Code reviewed and merged to main branch

---

## Open Questions

1. Should we use a multi-module Maven structure or single module?
2. Do we need separate modules for frontend (Angular) or keep it separate?
3. What's the strategy for managing Tree-sitter native libraries in the build?

---

**Epic Owner:** TBD  
**Created:** December 2025  
**Last Updated:** December 2025

