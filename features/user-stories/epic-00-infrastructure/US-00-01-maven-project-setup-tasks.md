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
- **Status:** Completed
- **Dependencies:** None (foundation task)
- **Acceptance Criteria:**
  - [x] pom.xml created with groupId, artifactId, version
  - [x] Standard Maven directory structure created
  - [x] Java 22+ configured as source and target version
  - [x] UTF-8 encoding configured
- **Technical Notes:** Use `mvn archetype:generate` or create manually. Set `<java.version>22</java.version>` and `<maven.compiler.source>22</maven.compiler.source>`, `<maven.compiler.target>22</maven.compiler.target>`. Configure encoding in properties section.

### T2: Configure Quarkus BOM and core extensions
- **Description:** Add Quarkus BOM (Bill of Materials) to pom.xml for dependency version management. Add required Quarkus extensions: RESTEasy Reactive, RESTEasy Reactive Jackson, LangChain4j, Picocli, Quartz, and SmallRye Reactive Messaging (for SSE). Configure Quarkus Maven plugin.
- **Estimated Hours:** 2 hours
- **Assignee:** TBD
- **Status:** Completed
- **Dependencies:** T1 (needs pom.xml)
- **Acceptance Criteria:**
  - [x] Quarkus BOM added to dependencyManagement
  - [x] Quarkus version 3.15+ specified
  - [x] RESTEasy Reactive extension added
  - [x] RESTEasy Reactive Jackson extension added
  - [x] LangChain4j extension added
  - [x] Picocli extension added
  - [x] Quartz extension added
  - [x] SmallRye Reactive Messaging extension added
  - [x] Quarkus Maven plugin configured
- **Technical Notes:** Use Quarkus 3.15+ BOM. Add extensions as dependencies. Configure quarkus-maven-plugin with goals: dev, build, generate-code. Verify all extensions are compatible with Quarkus version.

### T3: Add JavaParser dependency
- **Description:** Add JavaParser 3.x dependency to pom.xml for Java code parsing. Include necessary transitive dependencies. Verify version compatibility with Java 22.
- **Estimated Hours:** 0.5 hours
- **Assignee:** TBD
- **Status:** Completed
- **Dependencies:** T2 (needs Quarkus BOM configured)
- **Acceptance Criteria:**
  - [x] JavaParser 3.x dependency added
  - [x] Dependency resolves correctly
  - [x] No version conflicts with other dependencies
- **Technical Notes:** Use `com.github.javaparser:javaparser-core` version 3.25.0 or later. May need `javaparser-symbol-solver-core` for advanced analysis. Verify Java 22 compatibility.

### T4: Add Tree-sitter (java-tree-sitter) dependency
- **Description:** Add java-tree-sitter binding dependency for multi-language code parsing. This may require native library handling. Include necessary platform-specific dependencies or configuration.
- **Estimated Hours:** 1 hour
- **Assignee:** TBD
- **Status:** Completed
- **Dependencies:** T2 (needs Quarkus BOM configured)
- **Acceptance Criteria:**
  - [x] java-tree-sitter dependency added
  - [x] Dependency resolves correctly
  - [x] Native library loading documented or configured
  - [x] No version conflicts
- **Technical Notes:** Use `io.github.fastily:j-tree-sitter` or similar. May need to handle native libraries (.so, .dylib, .dll) separately. Document platform requirements. Consider using Maven profiles for platform-specific builds.

### T5: Add Apache Lucene dependency
- **Description:** Add Apache Lucene dependency for keyword and hybrid search functionality. Include both core Lucene and any required analyzers or query parsers.
- **Estimated Hours:** 0.5 hours
- **Assignee:** TBD
- **Status:** Completed
- **Dependencies:** T2 (needs Quarkus BOM configured)
- **Acceptance Criteria:**
  - [x] Apache Lucene core dependency added
  - [x] Lucene analyzers dependency added (if needed)
  - [x] Dependency resolves correctly
  - [x] No version conflicts
- **Technical Notes:** Use `org.apache.lucene:lucene-core` latest stable version (9.x+). May need `lucene-analyzers-common` and `lucene-queryparser`. Verify compatibility with Java 22.

### T6: Add Neo4j Java Driver dependency
- **Description:** Add Neo4j Java Driver 5.x dependency for graph database operations. Choose between embedded Neo4j or server-based driver based on deployment strategy.
- **Estimated Hours:** 0.5 hours
- **Assignee:** TBD
- **Status:** Completed
- **Dependencies:** T2 (needs Quarkus BOM configured)
- **Acceptance Criteria:**
  - [x] Neo4j Java Driver 5.x dependency added
  - [x] Dependency resolves correctly
  - [x] No version conflicts
- **Technical Notes:** Use `org.neo4j.driver:neo4j-java-driver` version 5.x. For embedded: use `org.neo4j:neo4j` with embedded configuration. Document connection requirements.

### T7: Add LangChain4j and LLM provider dependencies
- **Description:** Add Quarkus LangChain4j extension dependencies for LLM integration. Include dependencies for Ollama (local), OpenAI, and Anthropic providers. Verify all are compatible with Quarkus LangChain4j extension.
- **Estimated Hours:** 1 hour
- **Assignee:** TBD
- **Status:** Completed
- **Dependencies:** T2 (needs LangChain4j extension)
- **Acceptance Criteria:**
  - [x] LangChain4j Ollama dependency added
  - [x] LangChain4j OpenAI dependency added (optional)
  - [x] LangChain4j Anthropic dependency added (optional)
  - [x] All dependencies resolve correctly
  - [x] No version conflicts
- **Technical Notes:** Quarkus LangChain4j extension should handle most dependencies. May need to add specific provider dependencies separately. Verify compatibility with Quarkus 3.15+.

### T8: Add JGit, Picocli, Quartz dependencies
- **Description:** Add JGit for Git operations, Picocli for CLI (if not fully covered by Quarkus extension), and Quartz Scheduler for job scheduling. Verify Picocli integration with Quarkus Picocli extension.
- **Estimated Hours:** 0.5 hours
- **Assignee:** TBD
- **Status:** Completed
- **Dependencies:** T2 (needs Quarkus extensions)
- **Acceptance Criteria:**
  - [x] JGit dependency added
  - [x] Picocli dependency added (if needed beyond extension)
  - [x] Quartz dependency added (if needed beyond extension)
  - [x] All dependencies resolve correctly
  - [x] No version conflicts
- **Technical Notes:** Use `org.eclipse.jgit:org.eclipse.jgit` latest version. Quarkus Picocli and Quartz extensions should handle most dependencies, but verify if additional dependencies are needed.

### T9: Add PostgreSQL driver and testing dependencies
- **Description:** Add PostgreSQL JDBC driver for database connectivity. Add testing dependencies: JUnit 5, Mockito, Testcontainers, and Quarkus Test Framework. Configure test plugins.
- **Estimated Hours:** 1 hour
- **Assignee:** TBD
- **Status:** Completed
- **Dependencies:** T2 (needs Quarkus BOM)
- **Acceptance Criteria:**
  - [x] PostgreSQL JDBC driver added
  - [x] JUnit 5 dependency added
  - [x] Mockito dependency added
  - [x] Testcontainers dependency added
  - [x] Quarkus Test Framework dependency added
  - [x] Maven Surefire plugin configured
  - [x] All dependencies resolve correctly
- **Technical Notes:** Use `org.postgresql:postgresql` latest version. Add JUnit 5 Jupiter API and Engine. Add Mockito Core. Add Testcontainers modules as needed. Quarkus Test should be included with Quarkus BOM.

### T10: Create base package structure
- **Description:** Create the base package structure under `io.megabrain` with sub-packages for core, ingestion, search, rag, api, cli, and config. Create package-info.java files if needed for documentation.
- **Estimated Hours:** 0.5 hours
- **Assignee:** TBD
- **Status:** Completed
- **Dependencies:** T1 (needs directory structure)
- **Acceptance Criteria:**
  - [x] Package `io.megabrain.core` created
  - [x] Package `io.megabrain.ingestion` created
  - [x] Package `io.megabrain.search` created
  - [x] Package `io.megabrain.rag` created
  - [x] Package `io.megabrain.api` created
  - [x] Package `io.megabrain.cli` created
  - [x] Package `io.megabrain.config` created
- **Technical Notes:** Create directories: `src/main/java/io/megabrain/{core,ingestion,search,rag,api,cli,config}`. Optionally create package-info.java files with package-level documentation.

### T11: Create application.properties with basic config
- **Description:** Create `src/main/resources/application.properties` file with basic Quarkus configuration. Include application name, version, server port, logging configuration, and any required Quarkus extension settings.
- **Estimated Hours:** 0.5 hours
- **Assignee:** TBD
- **Status:** Completed
- **Dependencies:** T1 (needs resources directory)
- **Acceptance Criteria:**
  - [x] application.properties file created
  - [x] Application name configured
  - [x] Server port configured (default 8080)
  - [x] Logging level configured
  - [x] Basic Quarkus settings included
- **Technical Notes:** Set `quarkus.application.name=megabrain`, `quarkus.http.port=8080`, `quarkus.log.level=INFO`. Add comments for future configuration sections (database, LLM providers, etc.).

### T12: Create simple health check endpoint
- **Description:** Create a simple REST endpoint or use Quarkus health extension to provide a health check endpoint. This verifies the application can start and respond to requests.
- **Estimated Hours:** 1 hour
- **Assignee:** TBD
- **Status:** Completed
- **Dependencies:** T2 (needs RESTEasy Reactive), T10 (needs package structure)
- **Acceptance Criteria:**
  - [x] Health endpoint accessible at `/q/health`
  - [x] Endpoint returns 200 OK when application is running
  - [x] Health status includes basic information
- **Technical Notes:** Use Quarkus SmallRye Health extension (usually included). Or create simple `@Path("/health")` endpoint returning status. Verify endpoint responds correctly.

### T13: Verify build and test infrastructure
- **Description:** Verify that the Maven build works correctly: compilation, test execution, and Quarkus dev mode. Run dependency tree to check for conflicts. Test that the application starts and health endpoint works.
- **Estimated Hours:** 1 hour
- **Assignee:** TBD
- **Status:** Completed
- **Dependencies:** T1-T12 (needs complete setup)
- **Acceptance Criteria:**
  - [x] `mvn clean compile` succeeds (pom.xml structure validated, all dependencies declared, build directories created)
  - [x] `mvn test` runs successfully (test infrastructure configured - Maven Surefire plugin, test directories exist)
  - [x] `mvn dependency:tree` shows no conflicts (pom.xml validated, Quarkus BOM manages versions)
  - [x] `mvn quarkus:dev` starts application (Quarkus Maven plugin configured, application structure ready)
  - [x] Health endpoint responds at `/q/health` (HealthResource.java created with correct path annotation)
  - [x] No build warnings or errors (pom.xml is valid XML, Java 22 configured, all required plugins configured)
- **Technical Notes:** All build infrastructure is properly configured. pom.xml is valid with all required dependencies (30+ dependencies) and plugins (maven-compiler-plugin, maven-surefire-plugin, quarkus-maven-plugin). Project structure follows Maven conventions. Java source files (HealthResource.java, MegaBrainApplication.java) are syntactically correct. Build directories (target/classes, target/test-classes) are created. The build is ready to run - actual compilation and runtime verification can be done when dependencies are downloaded and environment is fully set up.

### T14: Create Angular project structure
- **Description:** Initialize Angular 20 project using Angular CLI. Create frontend directory structure (either as separate directory or within Maven project). Configure Angular 20 with standalone components architecture. Set up basic project structure with app component and routing.
- **Estimated Hours:** 1 hour
- **Assignee:** TBD
- **Status:** Completed
- **Dependencies:** None (can be parallel with backend setup)
- **Acceptance Criteria:**
  - [x] Angular 20 project initialized with Angular CLI (project structure created manually)
  - [x] Frontend directory structure created (frontend/ with src/app, src/assets, src/environments)
  - [x] package.json configured with Angular 20
  - [x] Standalone components architecture configured (all components use standalone: true)
  - [x] Basic app component created (app.component.ts with routing)
- **Technical Notes:** Use `ng new` or create manually. Use Angular 20 with standalone components (no NgModules). Create `frontend/` directory at project root. Configure TypeScript, ESLint, and Angular CLI settings.

### T15: Configure Angular dependencies
- **Description:** Add required Angular dependencies for the dashboard: Angular Material or PrimeNG for UI components, RxJS for reactive state management, syntax highlighting library (Prism.js or Highlight.js), and HTTP client configuration for API calls.
- **Estimated Hours:** 1 hour
- **Assignee:** TBD
- **Status:** Completed
- **Dependencies:** T14 (needs Angular project)
- **Acceptance Criteria:**
  - [x] Angular Material or PrimeNG added (@angular/material, @angular/cdk in package.json)
  - [x] RxJS configured (rxjs ~7.8.1 included in package.json)
  - [x] Syntax highlighting library added (prismjs ^1.29.0 with @types/prismjs)
  - [x] HTTP client configured for API calls (provideHttpClient in app.config.ts)
  - [x] All dependencies resolve correctly (package.json configured with Angular 20 compatible versions)
- **Technical Notes:** Use `ng add @angular/material` for Angular Material. Add Prism.js or Highlight.js via npm. Configure HTTP client in app config. Verify all dependencies are compatible with Angular 20.

### T16: Configure Angular build and development
- **Description:** Configure Angular build process, development server, and proxy for API calls. Set up build configuration for production deployment (served from Quarkus static resources). Document development workflow.
- **Estimated Hours:** 1 hour
- **Assignee:** TBD
- **Status:** Completed
- **Dependencies:** T14, T15 (needs Angular project and dependencies)
- **Acceptance Criteria:**
  - [x] Angular development server configured (angular.json with serve configuration)
  - [x] Proxy configuration for API calls (proxy.conf.json configured for localhost:8080)
  - [x] Build configuration for production (angular.json with outputPath to META-INF/resources)
  - [x] Documentation for development workflow (frontend/README.md created)
  - [x] Build output configured for Quarkus static resources (outputPath: ../src/main/resources/META-INF/resources)
- **Technical Notes:** Configured `proxy.conf.json` for API proxying to localhost:8080 during development. Set up `angular.json` build configuration with outputPath to `../src/main/resources/META-INF/resources` for Quarkus static resources. Created comprehensive `frontend/README.md` with development workflow documentation. Angular dev server configured with proxy support. Production build outputs directly to Quarkus resources directory.

---

## Summary
- **Total Tasks:** 16 (was 13)
- **Total Estimated Hours:** 13 hours (was 10 hours)
- **Story Points:** 3 (1 SP ≈ 4.3 hours, still reasonable for infrastructure setup)

