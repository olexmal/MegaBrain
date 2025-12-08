# Tasks for US-00-01: Maven Project Setup with Dependencies

## Story Reference
- **Epic:** EPIC-00 (Project Infrastructure Setup)
- **Story:** US-00-01
- **Story Points:** 3
- **Sprint Target:** Pre-Sprint 1 (Foundation Setup)

## Task List

### T1: Create Maven project structure
- **Description:** Create the basic Maven project structure including pom.xml file and standard Maven directory layout (src/main/java, src/main/resources, src/test/java, src/test/resources). Initialize pom.xml with basic Maven coordinates, Java version, and encoding settings.
- **Estimated Hours:** 1 hour
- **Assignee:** TBD
- **Status:** Not Started
- **Dependencies:** None (foundation task)
- **Acceptance Criteria:**
  - [ ] pom.xml created with groupId, artifactId, version
  - [ ] Standard Maven directory structure created
  - [ ] Java 21+ configured as source and target version
  - [ ] UTF-8 encoding configured
- **Technical Notes:** Use `mvn archetype:generate` or create manually. Set `<java.version>21</java.version>` and `<maven.compiler.source>21</maven.compiler.source>`, `<maven.compiler.target>21</maven.compiler.target>`. Configure encoding in properties section.

### T2: Configure Quarkus BOM and core extensions
- **Description:** Add Quarkus BOM (Bill of Materials) to pom.xml for dependency version management. Add required Quarkus extensions: RESTEasy Reactive, RESTEasy Reactive Jackson, LangChain4j, Picocli, Quartz, and SmallRye Reactive Messaging (for SSE). Configure Quarkus Maven plugin.
- **Estimated Hours:** 2 hours
- **Assignee:** TBD
- **Status:** Not Started
- **Dependencies:** T1 (needs pom.xml)
- **Acceptance Criteria:**
  - [ ] Quarkus BOM added to dependencyManagement
  - [ ] Quarkus version 3.15+ specified
  - [ ] RESTEasy Reactive extension added
  - [ ] RESTEasy Reactive Jackson extension added
  - [ ] LangChain4j extension added
  - [ ] Picocli extension added
  - [ ] Quartz extension added
  - [ ] SmallRye Reactive Messaging extension added
  - [ ] Quarkus Maven plugin configured
- **Technical Notes:** Use Quarkus 3.15+ BOM. Add extensions as dependencies. Configure quarkus-maven-plugin with goals: dev, build, generate-code. Verify all extensions are compatible with Quarkus version.

### T3: Add JavaParser dependency
- **Description:** Add JavaParser 3.x dependency to pom.xml for Java code parsing. Include necessary transitive dependencies. Verify version compatibility with Java 21.
- **Estimated Hours:** 0.5 hours
- **Assignee:** TBD
- **Status:** Not Started
- **Dependencies:** T2 (needs Quarkus BOM configured)
- **Acceptance Criteria:**
  - [ ] JavaParser 3.x dependency added
  - [ ] Dependency resolves correctly
  - [ ] No version conflicts with other dependencies
- **Technical Notes:** Use `com.github.javaparser:javaparser-core` version 3.25.0 or later. May need `javaparser-symbol-solver-core` for advanced analysis. Verify Java 21 compatibility.

### T4: Add Tree-sitter (java-tree-sitter) dependency
- **Description:** Add java-tree-sitter binding dependency for multi-language code parsing. This may require native library handling. Include necessary platform-specific dependencies or configuration.
- **Estimated Hours:** 1 hour
- **Assignee:** TBD
- **Status:** Not Started
- **Dependencies:** T2 (needs Quarkus BOM configured)
- **Acceptance Criteria:**
  - [ ] java-tree-sitter dependency added
  - [ ] Dependency resolves correctly
  - [ ] Native library loading documented or configured
  - [ ] No version conflicts
- **Technical Notes:** Use `io.github.fastily:j-tree-sitter` or similar. May need to handle native libraries (.so, .dylib, .dll) separately. Document platform requirements. Consider using Maven profiles for platform-specific builds.

### T5: Add Apache Lucene dependency
- **Description:** Add Apache Lucene dependency for keyword and hybrid search functionality. Include both core Lucene and any required analyzers or query parsers.
- **Estimated Hours:** 0.5 hours
- **Assignee:** TBD
- **Status:** Not Started
- **Dependencies:** T2 (needs Quarkus BOM configured)
- **Acceptance Criteria:**
  - [ ] Apache Lucene core dependency added
  - [ ] Lucene analyzers dependency added (if needed)
  - [ ] Dependency resolves correctly
  - [ ] No version conflicts
- **Technical Notes:** Use `org.apache.lucene:lucene-core` latest stable version (9.x+). May need `lucene-analyzers-common` and `lucene-queryparser`. Verify compatibility with Java 21.

### T6: Add Neo4j Java Driver dependency
- **Description:** Add Neo4j Java Driver 5.x dependency for graph database operations. Choose between embedded Neo4j or server-based driver based on deployment strategy.
- **Estimated Hours:** 0.5 hours
- **Assignee:** TBD
- **Status:** Not Started
- **Dependencies:** T2 (needs Quarkus BOM configured)
- **Acceptance Criteria:**
  - [ ] Neo4j Java Driver 5.x dependency added
  - [ ] Dependency resolves correctly
  - [ ] No version conflicts
- **Technical Notes:** Use `org.neo4j.driver:neo4j-java-driver` version 5.x. For embedded: use `org.neo4j:neo4j` with embedded configuration. Document connection requirements.

### T7: Add LangChain4j and LLM provider dependencies
- **Description:** Add Quarkus LangChain4j extension dependencies for LLM integration. Include dependencies for Ollama (local), OpenAI, and Anthropic providers. Verify all are compatible with Quarkus LangChain4j extension.
- **Estimated Hours:** 1 hour
- **Assignee:** TBD
- **Status:** Not Started
- **Dependencies:** T2 (needs LangChain4j extension)
- **Acceptance Criteria:**
  - [ ] LangChain4j Ollama dependency added
  - [ ] LangChain4j OpenAI dependency added (optional)
  - [ ] LangChain4j Anthropic dependency added (optional)
  - [ ] All dependencies resolve correctly
  - [ ] No version conflicts
- **Technical Notes:** Quarkus LangChain4j extension should handle most dependencies. May need to add specific provider dependencies separately. Verify compatibility with Quarkus 3.15+.

### T8: Add JGit, Picocli, Quartz dependencies
- **Description:** Add JGit for Git operations, Picocli for CLI (if not fully covered by Quarkus extension), and Quartz Scheduler for job scheduling. Verify Picocli integration with Quarkus Picocli extension.
- **Estimated Hours:** 0.5 hours
- **Assignee:** TBD
- **Status:** Not Started
- **Dependencies:** T2 (needs Quarkus extensions)
- **Acceptance Criteria:**
  - [ ] JGit dependency added
  - [ ] Picocli dependency added (if needed beyond extension)
  - [ ] Quartz dependency added (if needed beyond extension)
  - [ ] All dependencies resolve correctly
  - [ ] No version conflicts
- **Technical Notes:** Use `org.eclipse.jgit:org.eclipse.jgit` latest version. Quarkus Picocli and Quartz extensions should handle most dependencies, but verify if additional dependencies are needed.

### T9: Add PostgreSQL driver and testing dependencies
- **Description:** Add PostgreSQL JDBC driver for database connectivity. Add testing dependencies: JUnit 5, Mockito, Testcontainers, and Quarkus Test Framework. Configure test plugins.
- **Estimated Hours:** 1 hour
- **Assignee:** TBD
- **Status:** Not Started
- **Dependencies:** T2 (needs Quarkus BOM)
- **Acceptance Criteria:**
  - [ ] PostgreSQL JDBC driver added
  - [ ] JUnit 5 dependency added
  - [ ] Mockito dependency added
  - [ ] Testcontainers dependency added
  - [ ] Quarkus Test Framework dependency added
  - [ ] Maven Surefire plugin configured
  - [ ] All dependencies resolve correctly
- **Technical Notes:** Use `org.postgresql:postgresql` latest version. Add JUnit 5 Jupiter API and Engine. Add Mockito Core. Add Testcontainers modules as needed. Quarkus Test should be included with Quarkus BOM.

### T10: Create base package structure
- **Description:** Create the base package structure under `io.megabrain` with sub-packages for core, ingestion, search, rag, api, cli, and config. Create package-info.java files if needed for documentation.
- **Estimated Hours:** 0.5 hours
- **Assignee:** TBD
- **Status:** Not Started
- **Dependencies:** T1 (needs directory structure)
- **Acceptance Criteria:**
  - [ ] Package `io.megabrain.core` created
  - [ ] Package `io.megabrain.ingestion` created
  - [ ] Package `io.megabrain.search` created
  - [ ] Package `io.megabrain.rag` created
  - [ ] Package `io.megabrain.api` created
  - [ ] Package `io.megabrain.cli` created
  - [ ] Package `io.megabrain.config` created
- **Technical Notes:** Create directories: `src/main/java/io/megabrain/{core,ingestion,search,rag,api,cli,config}`. Optionally create package-info.java files with package-level documentation.

### T11: Create application.properties with basic config
- **Description:** Create `src/main/resources/application.properties` file with basic Quarkus configuration. Include application name, version, server port, logging configuration, and any required Quarkus extension settings.
- **Estimated Hours:** 0.5 hours
- **Assignee:** TBD
- **Status:** Not Started
- **Dependencies:** T1 (needs resources directory)
- **Acceptance Criteria:**
  - [ ] application.properties file created
  - [ ] Application name configured
  - [ ] Server port configured (default 8080)
  - [ ] Logging level configured
  - [ ] Basic Quarkus settings included
- **Technical Notes:** Set `quarkus.application.name=megabrain`, `quarkus.http.port=8080`, `quarkus.log.level=INFO`. Add comments for future configuration sections (database, LLM providers, etc.).

### T12: Create simple health check endpoint
- **Description:** Create a simple REST endpoint or use Quarkus health extension to provide a health check endpoint. This verifies the application can start and respond to requests.
- **Estimated Hours:** 1 hour
- **Assignee:** TBD
- **Status:** Not Started
- **Dependencies:** T2 (needs RESTEasy Reactive), T10 (needs package structure)
- **Acceptance Criteria:**
  - [ ] Health endpoint accessible at `/q/health`
  - [ ] Endpoint returns 200 OK when application is running
  - [ ] Health status includes basic information
- **Technical Notes:** Use Quarkus SmallRye Health extension (usually included). Or create simple `@Path("/health")` endpoint returning status. Verify endpoint responds correctly.

### T13: Verify build and test infrastructure
- **Description:** Verify that the Maven build works correctly: compilation, test execution, and Quarkus dev mode. Run dependency tree to check for conflicts. Test that the application starts and health endpoint works.
- **Estimated Hours:** 1 hour
- **Assignee:** TBD
- **Status:** Not Started
- **Dependencies:** T1-T12 (needs complete setup)
- **Acceptance Criteria:**
  - [ ] `mvn clean compile` succeeds
  - [ ] `mvn test` runs successfully (even with no tests)
  - [ ] `mvn dependency:tree` shows no conflicts
  - [ ] `mvn quarkus:dev` starts application
  - [ ] Health endpoint responds at `/q/health`
  - [ ] No build warnings or errors
- **Technical Notes:** Run all Maven commands and verify output. Check for dependency conflicts in dependency tree. Verify Quarkus dev mode starts without errors. Test health endpoint with curl or browser.

---

## Summary
- **Total Tasks:** 13
- **Total Estimated Hours:** 10 hours
- **Story Points:** 3 (1 SP ≈ 3.3 hours, aligns with estimate)

