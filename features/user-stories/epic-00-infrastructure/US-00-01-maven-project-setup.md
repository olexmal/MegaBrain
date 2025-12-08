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

- [ ] **AC1:** Maven project structure created (pom.xml, src/main/java, src/test/java, src/main/resources, src/test/resources)
- [ ] **AC2:** Quarkus 3.15+ configured with BOM and required extensions (RESTEasy Reactive, LangChain4j, Picocli, Quartz, SSE)
- [ ] **AC3:** All core dependencies added and resolved (JavaParser, java-tree-sitter, Apache Lucene, Neo4j Driver, JGit, PostgreSQL JDBC)
- [ ] **AC4:** Base package structure created (io.megabrain.core, io.megabrain.ingestion, io.megabrain.search, io.megabrain.rag, io.megabrain.api, io.megabrain.cli, io.megabrain.config)
- [ ] **AC5:** Application configuration file (application.properties) created with basic Quarkus settings
- [ ] **AC6:** Build and test infrastructure working (Maven compiler plugin, surefire plugin, Quarkus dev mode)
- [ ] **AC7:** Project compiles, runs, and health endpoint responds at `/q/health`

---

## Demo Script

### Setup
1. Ensure Java 21+ is installed and configured
2. Ensure Maven 3.8+ is installed
3. Have a terminal ready for Maven commands

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

### Expected Outcome
- Project structure visible and organized
- All dependencies resolve without conflicts
- Project compiles successfully
- Test infrastructure works
- Application starts in dev mode
- Health endpoint returns 200 OK
- Demo completes in <2 minutes

---

## Technical Tasks

- [ ] **T1:** Create Maven project structure (pom.xml, directories)
- [ ] **T2:** Configure Quarkus BOM and core extensions
- [ ] **T3:** Add JavaParser dependency
- [ ] **T4:** Add Tree-sitter (java-tree-sitter) dependency
- [ ] **T5:** Add Apache Lucene dependency
- [ ] **T6:** Add Neo4j Java Driver dependency
- [ ] **T7:** Add LangChain4j and LLM provider dependencies
- [ ] **T8:** Add JGit, Picocli, Quartz dependencies
- [ ] **T9:** Add PostgreSQL driver and testing dependencies
- [ ] **T10:** Create base package structure (io.megabrain.*)
- [ ] **T11:** Create application.properties with basic config
- [ ] **T12:** Create simple health check endpoint
- [ ] **T13:** Verify build and test infrastructure

---

## Test Scenarios

| Scenario | Given | When | Then |
|:---------|:------|:-----|:-----|
| Project compilation | Maven project with pom.xml | Run `mvn clean compile` | Compilation succeeds without errors |
| Dependency resolution | pom.xml with all dependencies | Run `mvn dependency:tree` | All dependencies resolve without conflicts |
| Test execution | Empty test directory | Run `mvn test` | Test phase completes successfully |
| Application startup | Quarkus application | Run `mvn quarkus:dev` | Application starts and health endpoint responds |
| Health check | Running application | GET `/q/health` | Returns 200 OK with health status |

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

