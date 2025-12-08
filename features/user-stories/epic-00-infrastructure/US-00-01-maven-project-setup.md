# US-00-01: Maven Project Setup with Dependencies

## Story
**As a** developer  
**I want** a properly configured Maven project with all required dependencies and project structure  
**So that** I can start implementing features without infrastructure blockers

## Story Points: 3
## Priority: Critical
## Sprint Target: Pre-Sprint 1 (Foundation Setup)

---

## Acceptance Criteria

- [x] **AC1:** Maven project structure created (pom.xml, src/main/java, src/test/java, src/main/resources, src/test/resources)
- [x] **AC2:** Quarkus 3.15+ configured with BOM and required extensions (RESTEasy Reactive, LangChain4j, Picocli, Quartz, SSE)
- [x] **AC3:** All core dependencies added and resolved (JavaParser, java-tree-sitter, Apache Lucene, Neo4j Driver, JGit, PostgreSQL JDBC)
- [x] **AC4:** Base package structure created (io.megabrain.core, io.megabrain.ingestion, io.megabrain.search, io.megabrain.rag, io.megabrain.api, io.megabrain.cli, io.megabrain.config)
- [x] **AC5:** Application configuration file (application.properties) created with basic Quarkus settings
- [x] **AC6:** Build and test infrastructure working (Maven compiler plugin, surefire plugin, Quarkus dev mode)
- [x] **AC7:** Project compiles, runs, and health endpoint responds at `/q/health`
- [x] **AC8:** Angular 20 project initialized with standalone components architecture
- [x] **AC9:** Angular dependencies configured (Angular Material/PrimeNG, RxJS, syntax highlighting)
- [x] **AC10:** Angular build and development server configured with API proxy

---

## Demo Script

### Setup
1. Ensure Java 21+ is installed and configured
2. Ensure Maven 3.8+ is installed
3. Ensure Node.js 18+ and npm are installed (for Angular)
4. Ensure Angular CLI is installed globally (`npm install -g @angular/cli@20`)
5. Have a terminal ready for Maven and npm commands

### Demo Steps
1. **Show Project Structure:** Display the Maven project directory structure
   ```bash
   tree -L 3 -I 'target|.git'
   ```

2. **Verify Dependencies:** Show all dependencies are resolved
   ```bash
   mvn dependency:tree | head -50
   ```

3. **Compile Project:** Demonstrate successful compilation
   ```bash
   mvn clean compile
   ```

4. **Run Tests:** Show test infrastructure works (even if no tests yet)
   ```bash
   mvn test
   ```

5. **Start Application:** Start Quarkus dev mode
   ```bash
   mvn quarkus:dev
   ```

6. **Verify Health Endpoint:** Check health endpoint responds
   ```bash
   curl http://localhost:8080/q/health
   ```

7. **Show Angular Project:** Display Angular frontend structure
   ```bash
   ls -la frontend/
   tree -L 2 frontend/src
   ```

8. **Verify Angular Setup:** Check Angular dependencies and configuration
   ```bash
   cd frontend && npm list --depth=0
   ```

9. **Start Angular Dev Server (Optional):** Demonstrate Angular dev server
   ```bash
   cd frontend && ng serve
   ```

### Expected Outcome
- Project structure visible and organized (backend and frontend)
- All dependencies resolve without conflicts (Maven and npm)
- Project compiles successfully
- Test infrastructure works
- Application starts in dev mode
- Health endpoint returns 200 OK
- Angular project structure created
- Angular dependencies installed
- Demo completes in <3 minutes

---

## Technical Tasks

- [x] **T1:** Create Maven project structure (pom.xml, directories)
- [x] **T2:** Configure Quarkus BOM and core extensions
- [x] **T3:** Add JavaParser dependency
- [x] **T4:** Add Tree-sitter (java-tree-sitter) dependency
- [x] **T5:** Add Apache Lucene dependency
- [x] **T6:** Add Neo4j Java Driver dependency
- [x] **T7:** Add LangChain4j and LLM provider dependencies
- [x] **T8:** Add JGit, Picocli, Quartz dependencies
- [x] **T9:** Add PostgreSQL driver and testing dependencies
- [x] **T10:** Create base package structure (io.megabrain.*)
- [x] **T11:** Create application.properties with basic config
- [x] **T12:** Create simple health check endpoint
- [x] **T13:** Verify build and test infrastructure
- [x] **T14:** Create Angular project structure
- [x] **T15:** Configure Angular dependencies
- [x] **T16:** Configure Angular build and development

---

## Test Scenarios

| Scenario | Given | When | Then |
|:---------|:------|:-----|:-----|
| Project compilation | Maven project with pom.xml | Run `mvn clean compile` | Compilation succeeds without errors |
| Dependency resolution | pom.xml with all dependencies | Run `mvn dependency:tree` | All dependencies resolve without conflicts |
| Test execution | Empty test directory | Run `mvn test` | Test phase completes successfully |
| Application startup | Quarkus application | Run `mvn quarkus:dev` | Application starts and health endpoint responds |
| Health check | Running application | GET `/q/health` | Returns 200 OK with health status |
| Angular project setup | Angular CLI installed | Run `ng new` or create manually | Angular 20 project created with standalone components |
| Angular dependencies | Angular project created | Run `npm install` | All Angular dependencies resolve correctly |
| Angular dev server | Angular project configured | Run `ng serve` | Development server starts and serves application |

---

## Dependencies

- **Blocked by:** None (foundation story)
- **Enables:** All other user stories (US-01-01, US-02-01, etc.)

---

## Risks

| Risk | Impact | Likelihood | Mitigation |
|:-----|:-------|:-----------|:-----------|
| Dependency version conflicts | Build failures | Medium | Use Quarkus BOM for version management; test early |
| Missing dependencies | Development blockers | High | Comprehensive dependency audit; verify against feature spec |
| Incorrect Maven structure | Development inefficiency | Low | Follow Quarkus conventions; validate structure |
| Native library issues (Tree-sitter) | Platform-specific failures | Medium | Document platform requirements; test on target platforms |

---

## Definition of Ready

- [x] Acceptance criteria clear
- [x] Dependencies identified
- [x] Tech tasks estimated
- [x] Test scenarios defined
- [x] Demo script approved
- [x] No blockers

---

## Notes

- This story must be completed before Sprint 1 begins
- All subsequent stories depend on this foundation
- Focus on getting dependencies right - easier to add than remove later
- Consider creating a dependency matrix document for reference
- Verify compatibility between Quarkus version and all dependencies
- Angular frontend can be developed in parallel with backend, but infrastructure setup should be done first
- Angular build output should be configured to integrate with Quarkus static resources for production deployment

