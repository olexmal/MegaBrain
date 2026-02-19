# Technology Stack

---

## Backend Technologies


| Component                  | Technology                       | Version       | Purpose                                                              |
| -------------------------- | -------------------------------- | ------------- | -------------------------------------------------------------------- |
| **Framework**              | Quarkus                          | 3.30.2        | Ultrafast startup, low memory, reactive, GraalVM native support      |
| **Language**               | Java                             | 22+           | Modern Java features, records, pattern matching                      |
| **Build Tool**             | Maven                            | 3.8+          | Dependency management and build automation                           |
| **Reactive Model**         | Mutiny                           | (via Quarkus) | `Multi`/`Uni` types for non-blocking streams                         |
| **REST API**               | RESTEasy Reactive                | (via Quarkus) | JAX-RS reactive endpoints                                            |
| **Dependency Injection**   | CDI                              | (via Quarkus) | Contexts and Dependency Injection                                    |
| **Primary Search**         | Apache Lucene                    | 10.3.2        | Fast, embeddable keyword and hybrid search                           |
| **Graph Database**         | Neo4j Java Driver                | 6.0.2         | Dependency graph storage and queries                                 |
| **Vector Search**          | pgvector                         | (PostgreSQL)  | Optional semantic similarity search                                  |
| **Database**               | PostgreSQL                       | (via JDBC)    | Persistent storage with pgvector extension                           |
| **Java Parsing**           | JavaParser                       | 3.27.1        | Superior Java-specific code analysis                                 |
| **Multi-Language Parsing** | java-tree-sitter                 | 0.25.6        | C, C++, Python, JS/TS, Go, Rust, Kotlin, Ruby, Scala, Swift, PHP, C# |
| **LLM Integration**        | LangChain4j                      | 1.9.1         | Unified LLM client for multiple providers                            |
| **LLM Providers**          | Ollama, OpenAI, Anthropic        | -             | Free/local and paid cloud options                                    |
| **Git Operations**         | JGit                             | 7.4.0         | Git repository access and operations                                 |
| **CLI Framework**          | Picocli                          | (via Quarkus) | Feature-rich CLI with CDI integration                                |
| **Scheduling**             | Quartz                           | (via Quarkus) | Job scheduling for ingestion                                         |
| **Streaming**              | Server-Sent Events               | (via Quarkus) | Real-time progress and token streaming                               |
| **Testing**                | JUnit 5, Mockito, Testcontainers | Latest        | Unit and integration testing                                         |
| **Coverage**               | JaCoCo                           | (via Maven)   | Code coverage reports                                                |


## Frontend Technologies


| Component               | Technology            | Version | Purpose                            |
| ----------------------- | --------------------- | ------- | ---------------------------------- |
| **Framework**           | Angular               | 20.3.0  | Full-featured TypeScript framework |
| **Architecture**        | Standalone Components | -       | Modern Angular without NgModules   |
| **UI Library**          | Angular Material      | 20.0    | Material Design components         |
| **State Management**    | RxJS                  | 7.8.1   | Reactive state management          |
| **Syntax Highlighting** | Prism.js              | 1.29.0  | Code syntax highlighting           |
| **Build Tool**          | Angular CLI           | 20.0    | Development and build tooling      |
| **Language**            | TypeScript            | 5.6.0   | Type-safe JavaScript               |
| **Testing**             | Jest                  | Latest  | Unit testing framework             |


## Development Tools

- **IDE Support:** VS Code, IntelliJ IDEA, Eclipse
- **Linting:** ESLint (frontend), Checkstyle (backend)
- **Formatting:** Prettier (frontend), Google Java Format (backend)
- **Version Control:** Git
- **Containerization:** Docker (for deployment)

