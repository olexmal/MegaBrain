# MegaBrain RAG Pipeline - Complete Documentation

**Version:** 1.0.0-SNAPSHOT  
**Last Updated:** December 2025

---

## Table of Contents

1. [Introduction](#introduction)
2. [Architecture Overview](#architecture-overview)
3. [Technology Stack](#technology-stack)
4. [Installation & Setup](#installation--setup)
5. [Configuration](#configuration)
6. [Development Guide](#development-guide)
7. [API & CLI Usage](#api--cli-usage)
8. [Frontend Development](#frontend-development)
9. [Deployment & Operations](#deployment--operations)
10. [Troubleshooting](#troubleshooting)
11. [Contributing](#contributing)
12. [References](#references)

---

## Introduction

### What is MegaBrain?

MegaBrain is a **scalable, self-hosted, intelligent code knowledge platform** that indexes multi-language source code from various repositories and provides precise semantic search and natural language Q&A through a modern, reactive architecture.

### Core Value Proposition

MegaBrain solves the problem of **knowledge fragmentation** across large, polyglot, and actively evolving codebases. It moves beyond simple text search by understanding code semantics and structure, providing developers with instant, context-aware answers about their own code.

### Key Features

- **Semantic Code Search** - Find code by meaning, not just keywords
- **Natural Language Q&A** - Ask questions about your codebase in plain English
- **Multi-Language Support** - Java, Python, C/C++, JavaScript/TypeScript, Go, Rust, Kotlin, Ruby, Scala, Swift, PHP, C#
- **Dependency Graph Analysis** - Visualize and analyze code relationships
- **Documentation Intelligence** - Extract and correlate documentation from code
- **Privacy-First** - Fully self-hosted, supports offline operation with Ollama
- **High Performance** - Sub-second query latency, millions of lines indexed daily
- **Multiple Interfaces** - Web UI, CLI, REST API, and MCP Server

### Problem Statement

**The Problem:**
- **Lost Context:** Developers struggle to find specific implementations, API usage patterns, and documentation scattered across repositories
- **Inefficient Search:** Traditional `grep` or IDE search lacks semantic understanding and cannot answer "how-to" questions
- **External Dependency Risk:** Using general-purpose LLMs or external code assistants risks exposing proprietary code
- **Manual Overhead:** Onboarding new team members requires extensive, manual code traversal

**The Solution:**
MegaBrain creates a private, intelligent knowledge base of your codebase that enables semantic search and Q&A while keeping all data in-house.

---

## Architecture Overview

### High-Level Architecture

MegaBrain follows a **modular, event-driven architecture** built on a modern Java stack with reactive programming principles.

```
┌─────────────────────────────────────────────────────────────┐
│                    Source Code Repositories                  │
│         (GitHub, GitLab, Bitbucket, Local Git)              │
└──────────────────────┬──────────────────────────────────────┘
                       │
                       ▼
┌─────────────────────────────────────────────────────────────┐
│              Ingestion Layer (EPIC-01)                       │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐      │
│  │ Source       │  │ Parser       │  │ Dependency  │      │
│  │ Control      │→ │ Registry     │→ │ Extractor   │      │
│  │ Client       │  │ (JavaParser/ │  │             │      │
│  │ Factory      │  │ Tree-sitter) │  │             │      │
│  └──────────────┘  └──────────────┘  └──────────────┘      │
└──────────────────────┬──────────────────────────────────────┘
                       │
                       ▼
┌─────────────────────────────────────────────────────────────┐
│              Storage Layer                                   │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐      │
│  │ Lucene       │  │ Graph DB     │  │ Vector Store │      │
│  │ Index        │  │ (Neo4j)      │  │ (pgvector)   │      │
│  │ (Primary)     │  │              │  │ (Optional)   │      │
│  └──────────────┘  └──────────────┘  └──────────────┘      │
└──────────────────────┬──────────────────────────────────────┘
                       │
                       ▼
┌─────────────────────────────────────────────────────────────┐
│              Search & Retrieval Layer (EPIC-02)              │
│  ┌──────────────────────────────────────────────────────┐  │
│  │         Hybrid Search Orchestrator                    │  │
│  │  (Keyword + Semantic + Graph + Vector)              │  │
│  └──────────────────────────────────────────────────────┘  │
└──────────────────────┬──────────────────────────────────────┘
                       │
                       ▼
┌─────────────────────────────────────────────────────────────┐
│              RAG Layer (EPIC-03)                             │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐      │
│  │ Context      │  │ LLM Provider │  │ Answer       │      │
│  │ Assembler   │→ │ (Ollama/     │→ │ Streamer     │      │
│  │              │  │ OpenAI/      │  │ (SSE)        │      │
│  │              │  │ Anthropic)   │  │              │      │
│  └──────────────┘  └──────────────┘  └──────────────┘      │
└──────────────────────┬──────────────────────────────────────┘
                       │
                       ▼
┌─────────────────────────────────────────────────────────────┐
│              Interface Layer (EPIC-04, EPIC-05, EPIC-08)     │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐    │
│  │ REST API │  │ Web UI   │  │ CLI      │  │ MCP      │    │
│  │          │  │ (Angular)│  │ (Picocli)│  │ Server   │    │
│  └──────────┘  └──────────┘  └──────────┘  └──────────┘    │
└─────────────────────────────────────────────────────────────┘
```

### Component Architecture

#### Backend (Java/Quarkus)

```
io.megabrain/
├── api/              # REST endpoints (JAX-RS)
├── cli/              # CLI commands (Picocli)
├── config/           # Configuration classes
├── core/             # Core services and utilities
├── ingestion/        # Code ingestion services
│   ├── SourceControlClientFactory
│   ├── ParserRegistry
│   ├── JavaParserService
│   ├── TreeSitterParser
│   └── GrammarManager
├── search/           # Search services
│   ├── LuceneIndexService
│   ├── VectorStore
│   └── HybridSearchOrchestrator
├── rag/              # RAG services
│   ├── RagService
│   └── ContextAssembler
├── dependency/       # Dependency analysis (EPIC-06)
│   ├── DependencyExtractor
│   └── GraphQueryService
└── documentation/    # Documentation intelligence (EPIC-07)
    └── DocCommentParser
```

#### Frontend (Angular 20)

```
frontend/src/app/
├── app.component.ts      # Root component
├── app.routes.ts         # Routing configuration
├── app.config.ts         # Application configuration
├── dashboard/            # Ingestion dashboard
├── search/               # Search interface
└── chat/                 # RAG chat interface
```

### Data Flow

1. **Ingestion Flow:**
   - Repository trigger (scheduled or API)
   - Source control client fetches code
   - Language router selects parser (JavaParser or Tree-sitter)
   - Parser creates structured chunks with metadata
   - Dependency extractor builds graph relationships
   - Documentation parser enriches chunks
   - Chunks indexed in Lucene (and optionally vector store)

2. **Query Flow:**
   - User submits query (via UI, CLI, API, or MCP)
   - Hybrid search orchestrator queries:
     - Lucene index (keyword + hybrid search)
     - Graph database (dependency relationships)
     - Vector store (semantic similarity, if enabled)
   - Top-K chunks retrieved and ranked
   - Context assembled for LLM
   - LLM generates answer (streamed via SSE)
   - Response returned to user

---

## Technology Stack

### Backend Technologies

| Component | Technology | Version | Purpose |
|:----------|:-----------|:--------|:--------|
| **Framework** | Quarkus | 3.15+ | Ultrafast startup, low memory, reactive, GraalVM native support |
| **Language** | Java | 21+ | Modern Java features, records, pattern matching |
| **Build Tool** | Maven | 3.8+ | Dependency management and build automation |
| **Reactive Model** | Mutiny | (via Quarkus) | `Multi`/`Uni` types for non-blocking streams |
| **REST API** | RESTEasy Reactive | (via Quarkus) | JAX-RS reactive endpoints |
| **Dependency Injection** | CDI | (via Quarkus) | Contexts and Dependency Injection |
| **Primary Search** | Apache Lucene | 9.11.0 | Fast, embeddable keyword and hybrid search |
| **Graph Database** | Neo4j Java Driver | 5.20.0 | Dependency graph storage and queries |
| **Vector Search** | pgvector | (PostgreSQL) | Optional semantic similarity search |
| **Database** | PostgreSQL | (via JDBC) | Persistent storage with pgvector extension |
| **Java Parsing** | JavaParser | 3.25.9 | Superior Java-specific code analysis |
| **Multi-Language Parsing** | java-tree-sitter | 0.20.2 | C, C++, Python, JS/TS, Go, Rust, Kotlin, Ruby, Scala, Swift, PHP, C# |
| **LLM Integration** | LangChain4j | (via Quarkus) | Unified LLM client for multiple providers |
| **LLM Providers** | Ollama, OpenAI, Anthropic | - | Free/local and paid cloud options |
| **Git Operations** | JGit | 6.9.0 | Git repository access and operations |
| **CLI Framework** | Picocli | (via Quarkus) | Feature-rich CLI with CDI integration |
| **Scheduling** | Quartz | (via Quarkus) | Job scheduling for ingestion |
| **Streaming** | Server-Sent Events | (via Quarkus) | Real-time progress and token streaming |
| **Testing** | JUnit 5, Mockito, Testcontainers | Latest | Unit and integration testing |

### Frontend Technologies

| Component | Technology | Version | Purpose |
|:----------|:-----------|:--------|:--------|
| **Framework** | Angular | 20.0 | Full-featured TypeScript framework |
| **Architecture** | Standalone Components | - | Modern Angular without NgModules |
| **UI Library** | Angular Material | 20.0 | Material Design components |
| **State Management** | RxJS | 7.8.1 | Reactive state management |
| **Syntax Highlighting** | Prism.js | 1.29.0 | Code syntax highlighting |
| **Build Tool** | Angular CLI | 20.0 | Development and build tooling |
| **Language** | TypeScript | 5.6.0 | Type-safe JavaScript |

### Development Tools

- **IDE Support:** VS Code, IntelliJ IDEA, Eclipse
- **Linting:** ESLint (frontend), Checkstyle (backend)
- **Formatting:** Prettier (frontend), Google Java Format (backend)
- **Version Control:** Git
- **Containerization:** Docker (for deployment)

---

## Installation & Setup

### Prerequisites

#### Backend Prerequisites

- **Java 22 or higher**
  ```bash
  java -version  # Should show 22 or higher
  ```

- **Maven 3.8 or higher**
  ```bash
  mvn -version  # Should show 3.8 or higher
  ```

- **PostgreSQL 12+** (optional, for vector search)
  ```bash
  psql --version
  ```

- **Neo4j 5.x** (optional, for graph database)
  ```bash
  neo4j version
  ```

- **Ollama** (optional, for local LLM)
  ```bash
  ollama --version
  # Or install from https://ollama.ai
  ```

#### Frontend Prerequisites

- **Node.js 18+ and npm**
  ```bash
  node --version  # Should show 18 or higher
  npm --version
  ```

- **Angular CLI 20** (install globally)
  ```bash
  npm install -g @angular/cli@20
  ng version
  ```

### Installation Steps

#### 1. Clone the Repository

```bash
git clone <repository-url>
cd MegaBrain
```

#### 2. Backend Setup

```bash
cd backend

# Verify Java and Maven
java -version
mvn -version

# Compile the project
mvn clean compile

# Run tests
mvn test

# Start in development mode
mvn quarkus:dev
```

The backend will start on `http://localhost:8080`

#### 3. Frontend Setup

```bash
cd frontend

# Install dependencies
npm install

# Start development server (with API proxy)
npm start
# or
ng serve
```

The frontend will start on `http://localhost:4200` and proxy API requests to `http://localhost:8080`

#### 4. Verify Installation

**Backend Health Check:**
```bash
curl http://localhost:8080/q/health
```

Expected response:
```json
{
  "status": "UP",
  "message": "MegaBrain is running"
}
```

**Frontend:**
Open `http://localhost:4200` in your browser. You should see the MegaBrain dashboard.

---

## Configuration

### Backend Configuration

Edit `backend/src/main/resources/application.properties`:

#### Basic Application Settings

```properties
# Application Info
quarkus.application.name=megabrain
quarkus.application.version=1.0.0-SNAPSHOT

# HTTP Server
quarkus.http.port=8080
quarkus.http.cors=true

# Logging
quarkus.log.level=INFO
quarkus.log.category."io.megabrain".level=DEBUG
```

#### Database Configuration

**PostgreSQL (for vector search):**
```properties
quarkus.datasource.db-kind=postgresql
quarkus.datasource.username=megabrain_user
quarkus.datasource.password=megabrain_password
quarkus.datasource.jdbc.url=jdbc:postgresql://localhost:5432/megabrain_db
```

**Neo4j (for graph database):**
```properties
neo4j.uri=bolt://localhost:7687
neo4j.username=neo4j
neo4j.password=your_password
```

#### LLM Provider Configuration

**Ollama (Free/Local - Recommended for Privacy):**
```properties
megabrain.llm.provider=ollama
megabrain.llm.ollama.base-url=http://localhost:11434
megabrain.llm.ollama.model=codellama
# Available models: codellama, mistral, llama2, phi, etc.
```

**OpenAI (Paid Cloud):**
```properties
megabrain.llm.provider=openai
megabrain.llm.openai.api-key=${OPENAI_API_KEY}
megabrain.llm.openai.chat-model.model-name=gpt-3.5-turbo
# Or: gpt-4, gpt-4-turbo
```

**Anthropic Claude (Paid Cloud):**
```properties
megabrain.llm.provider=anthropic
megabrain.llm.anthropic.api-key=${ANTHROPIC_API_KEY}
megabrain.llm.anthropic.chat-model.model-name=claude-3-sonnet-20240229
# Or: claude-3-opus-20240229
```

#### Index Configuration

```properties
# Lucene Index Directory
megabrain.index.directory=./data/index

# Grammar Cache Directory (for Tree-sitter)
megabrain.grammar.cache.directory=~/.megabrain/grammars
```

### Frontend Configuration

#### Development Environment

Edit `frontend/src/environments/environment.ts`:

```typescript
export const environment = {
  production: false,
  apiUrl: 'http://localhost:8080/api/v1'
};
```

#### Production Environment

Edit `frontend/src/environments/environment.prod.ts`:

```typescript
export const environment = {
  production: true,
  apiUrl: '/api/v1'  // Relative URL when served from Quarkus
};
```

#### API Proxy Configuration

The `frontend/proxy.conf.json` is already configured for development:

```json
{
  "/api": {
    "target": "http://localhost:8080",
    "secure": false,
    "changeOrigin": true,
    "logLevel": "debug"
  }
}
```

### Environment Variables

For sensitive configuration, use environment variables:

```bash
# Backend
export OPENAI_API_KEY=sk-...
export ANTHROPIC_API_KEY=sk-...
export POSTGRES_PASSWORD=your_password
export NEO4J_PASSWORD=your_password

# Then reference in application.properties:
# megabrain.llm.openai.api-key=${OPENAI_API_KEY}
```

---

## Development Guide

### Project Structure

```
MegaBrain/
├── backend/                    # Java/Quarkus backend
│   ├── pom.xml                # Maven configuration
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/          # Java source code
│   │   │   └── resources/     # Configuration files
│   │   └── test/              # Test code
│   └── target/                # Build output
├── frontend/                   # Angular frontend
│   ├── package.json           # npm dependencies
│   ├── angular.json           # Angular configuration
│   ├── src/
│   │   ├── app/               # Angular components
│   │   ├── assets/            # Static assets
│   │   └── environments/      # Environment configs
│   └── dist/                  # Build output
├── features/                   # Feature specifications
│   ├── epics/                 # Epic definitions
│   ├── user-stories/          # User stories and tasks
│   └── feature_specification.md
└── README.md                   # Project overview
```

### Development Workflow

#### Backend Development

1. **Start Quarkus Dev Mode:**
   ```bash
   cd backend
   mvn quarkus:dev
   ```
   - Hot reload enabled
   - Changes automatically compiled
   - Application restarts on code changes

2. **Run Tests:**
   ```bash
   mvn test
   ```

3. **Build for Production:**
   ```bash
   mvn clean package
   ```

4. **Build Native Executable:**
   ```bash
   mvn clean package -Pnative
   # Requires GraalVM
   ```

#### Frontend Development

1. **Start Development Server:**
   ```bash
   cd frontend
   npm start
   ```
   - Hot reload enabled
   - API proxy configured
   - Available at `http://localhost:4200`

2. **Run Tests:**
   ```bash
   npm test
   ```

3. **Lint Code:**
   ```bash
   npm run lint
   ```

4. **Build for Production:**
   ```bash
   npm run build
   ```
   - Output goes to `backend/src/main/resources/META-INF/resources`
   - Served by Quarkus in production

### Coding Standards

#### Java Backend

- **Naming:**
  - Classes: `PascalCase` (e.g., `GitHubSourceControlClient`)
  - Methods: `camelCase` (e.g., `extractRelationships()`)
  - Constants: `UPPER_SNAKE_CASE`
  - Packages: `lowercase.with.dots`

- **Reactive Programming:**
  - Use `Uni<T>` for single async values
  - Use `Multi<T>` for streams
  - Prefer reactive chains over blocking operations

- **Dependency Injection:**
  - Use constructor injection: `@Inject public MyService(OtherService other)`
  - Annotate services with `@ApplicationScoped` or `@Singleton`
  - Use `@ConfigProperty` for configuration

- **Testing:**
  - Minimum 80% code coverage
  - Use `@QuarkusTest` for integration tests
  - Mock external dependencies with Mockito

#### TypeScript Frontend

- **Architecture:**
  - Standalone components (no NgModules)
  - Use Angular Signals for state management
  - Prefer RxJS for async operations

- **Naming:**
  - Components: `PascalCase` (e.g., `DashboardComponent`)
  - Files: `kebab-case.component.ts`
  - Services: `PascalCase` with `Service` suffix

- **Code Style:**
  - Use TypeScript strict mode
  - Prefer interfaces over types for public APIs
  - Use async/await over promises

### Testing

#### Backend Testing

```java
@QuarkusTest
class SearchServiceTest {
    @InjectMock
    LuceneIndexService indexService;
    
    @Inject
    SearchService searchService;
    
    @Test
    void testSearch() {
        when(indexService.search(anyString()))
            .thenReturn(List.of(result));
        
        Uni<SearchResult> result = searchService.search("query");
        assertThat(result.await().indefinitely()).isNotNull();
    }
}
```

#### Frontend Testing

```typescript
describe('DashboardComponent', () => {
  let component: DashboardComponent;
  let service: IngestionService;
  
  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [DashboardComponent],
      providers: [
        { provide: IngestionService, useValue: mockService }
      ]
    });
    component = TestBed.createComponent(DashboardComponent).componentInstance;
  });
  
  it('should display jobs', () => {
    // Test implementation
  });
});
```

### Git Workflow

1. Create feature branch: `git checkout -b feature/EPIC-01-01-github-ingestion`
2. Make changes and commit: `git commit -m "feat: implement GitHub ingestion"`
3. Push branch: `git push origin feature/EPIC-01-01-github-ingestion`
4. Create pull request
5. Code review and merge

---

## API & CLI Usage

### REST API
- Base URL: `http://localhost:8080`
- Health: `GET /q/health`
- Ingestion (example):
  - `POST /api/v1/ingestion` — trigger ingestion for a repository
  - Body (example):
    ```json
    {
      "url": "https://bitbucket.org/workspace/repo",
      "branch": "main",
      "provider": "bitbucket"
    }
    ```
- Search (example):
  - `POST /api/v1/search`
  - Body (example):
    ```json
    {
      "query": "how is Bitbucket auth handled",
      "limit": 5
    }
    ```
- SSE streaming: responses stream via Server-Sent Events where noted (e.g., RAG answers).
- Auth: configure tokens via `application.properties` / env; do not embed in URLs.

### CLI (Picocli / Quarkus)
- Run via `mvn -pl backend quarkus:dev` then `java -jar target/quarkus-app/quarkus-run.jar <command>` or via packaged CLI if available.
- Common commands (examples, may vary by module):
  - `megabrain ingest --url https://bitbucket.org/workspace/repo --branch main`
  - `megabrain search --query "dependency graph builder" --limit 5`
- Use `--help` on any command for options and flags.

### MCP Server
- Transport: stdio (primary), SSE (optional).
- Purpose: expose tools (search, ingestion triggers) to LLM clients.
- See EPIC-08 docs for detailed protocol and tool schema.

### Base URL

- **Development:** `http://localhost:8080/api/v1`
- **Production:** `https://your-domain.com/api/v1`

### Authentication

Currently, authentication is not implemented. Future versions will support:
- API keys
- OAuth 2.0
- JWT tokens

### Endpoints

#### Health Check

```http
GET /q/health
```

**Response:**
```json
{
  "status": "UP",
  "message": "MegaBrain is running"
}
```

#### Ingestion Endpoints

*Note: These endpoints will be implemented in EPIC-01 and EPIC-04*

**Start Ingestion Job:**
```http
POST /api/v1/ingestion/jobs
Content-Type: application/json

{
  "repositoryUrl": "https://github.com/user/repo",
  "sourceControlType": "GITHUB",
  "branch": "main"
}
```

**Get Ingestion Progress:**
```http
GET /api/v1/ingestion/jobs/{jobId}/progress
Accept: text/event-stream
```

**List Ingestion Jobs:**
```http
GET /api/v1/ingestion/jobs
```

#### Search Endpoints

*Note: These endpoints will be implemented in EPIC-02 and EPIC-04*

**Search Code:**
```http
POST /api/v1/search
Content-Type: application/json

{
  "query": "how to authenticate users",
  "filters": {
    "language": ["java", "typescript"],
    "repository": ["repo1", "repo2"]
  },
  "limit": 10
}
```

**Get Search Result Details:**
```http
GET /api/v1/search/results/{resultId}
```

#### RAG Endpoints

*Note: These endpoints will be implemented in EPIC-03 and EPIC-04*

**Ask Question:**
```http
POST /api/v1/rag/ask
Content-Type: application/json

{
  "question": "How does authentication work in this codebase?",
  "context": {
    "repository": "repo1",
    "language": "java"
  }
}
```

**Stream Answer:**
```http
POST /api/v1/rag/ask/stream
Content-Type: application/json
Accept: text/event-stream

{
  "question": "How does authentication work?",
  "stream": true
}
```

### Error Responses

All errors follow this format:

```json
{
  "error": "Error Type",
  "message": "Human-readable error message",
  "timestamp": "2025-12-08T10:30:00Z",
  "path": "/api/v1/endpoint"
}
```

**HTTP Status Codes:**
- `200 OK` - Success
- `201 Created` - Resource created
- `400 Bad Request` - Invalid request
- `404 Not Found` - Resource not found
- `500 Internal Server Error` - Server error

---

## Frontend Development
- Prereqs: Node 18+, npm, Angular CLI 20 (`npm install -g @angular/cli@20`).
- Install deps: `cd frontend && npm install`
- Dev server: `npm start` or `ng serve` (runs at `http://localhost:4200`, proxies API to `http://localhost:8080`)
- Build: `npm run build` (production bundle)
- Env config: edit `frontend/src/environments/environment.ts` (dev) or `environment.prod.ts` (prod) for API base URL and feature flags.

## Deployment & Operations
- Backend port: `8080` (configurable via `quarkus.http.port`).
- Bitbucket Server: ensure `bitbucket-server-api/mp-rest/url` points to the server root (no `/rest`), PAT set via `megabrain.bitbucket.server.*`; clone URL derived as `<base>/scm/<project>/<repo>.git`.
- Health: `GET /q/health`
- Logging: controlled via `quarkus.log.level` and category configs.
- Datastores: configure PostgreSQL, Neo4j, Lucene path, and optional vector store (pgvector/Milvus) before production use.
- LLM providers: prefer Ollama for on-prem; set base URL and model; cloud providers require API keys.
- Security: never log tokens; ensure HTTPS in production; limit exposure of `/q/*` endpoints.

### Production Build

#### Backend

```bash
cd backend

# Build JAR
mvn clean package

# Build native executable (requires GraalVM)
mvn clean package -Pnative
```

**Output:**
- JAR: `backend/target/megabrain-1.0.0-SNAPSHOT.jar`
- Native: `backend/target/megabrain-1.0.0-SNAPSHOT`

#### Frontend

```bash
cd frontend

# Build for production
npm run build
```

**Output:** `backend/src/main/resources/META-INF/resources/`

### Docker Deployment

*Note: Docker configuration will be added in future versions*

### System Requirements

**Minimum:**
- CPU: 2 cores
- RAM: 4 GB
- Disk: 10 GB

**Recommended:**
- CPU: 4+ cores
- RAM: 8+ GB
- Disk: 50+ GB (for indexes and graph database)

### Production Configuration

1. Set environment variables for sensitive data
2. Configure database connections
3. Set up SSL/TLS certificates
4. Configure logging levels
5. Set up monitoring and alerts

---

## Troubleshooting

### Common Issues

#### Backend Won't Start

**Issue:** `Port 8080 already in use`

**Solution:**
```bash
# Find process using port 8080
lsof -i :8080
# Kill the process or change port in application.properties
quarkus.http.port=8081
```

**Issue:** `Dependency resolution failed`

**Solution:**
```bash
cd backend
mvn clean install -U
```

#### Frontend Won't Start

**Issue:** `Angular CLI not found`

**Solution:**
```bash
npm install -g @angular/cli@20
```

**Issue:** `Port 4200 already in use`

**Solution:**
```bash
ng serve --port 4201
```

#### LLM Provider Issues

**Issue:** `Ollama connection failed`

**Solution:**
1. Verify Ollama is running: `ollama serve`
2. Check base URL in `application.properties`
3. Verify model is available: `ollama list`

**Issue:** `OpenAI/Anthropic API key invalid`

**Solution:**
1. Verify API key in environment variables
2. Check API key has correct permissions
3. Verify billing is active (for paid providers)

### Debugging

#### Enable Debug Logging

**Backend:**
```properties
quarkus.log.level=DEBUG
quarkus.log.category."io.megabrain".level=TRACE
```

**Frontend:**
- Open browser DevTools (F12)
- Check Console and Network tabs

#### Common Log Locations

- Backend logs: Console output (Quarkus dev mode)
- Frontend logs: Browser console

---

## Contributing

### Development Setup

1. Fork the repository
2. Clone your fork
3. Create a feature branch
4. Make your changes
5. Write tests
6. Ensure all tests pass
7. Submit a pull request

### Code Review Process

1. All code must be reviewed
2. Minimum 80% test coverage required
3. All CI checks must pass
4. Documentation must be updated

### Commit Message Format

```
<type>(<scope>): <subject>

<body>

<footer>
```

**Types:**
- `feat`: New feature
- `fix`: Bug fix
- `docs`: Documentation
- `style`: Code style changes
- `refactor`: Code refactoring
- `test`: Test additions/changes
- `chore`: Build/tooling changes

**Example:**
```
feat(ingestion): add GitHub repository ingestion

Implement GitHubSourceControlClient with authentication
support and repository cloning.

Closes #123
```

---

## References

### Project Documentation

- [Feature Specification](features/feature_specification.md) - Complete feature specification
- [Epics Overview](features/epics/README.md) - All epics and their relationships
- [User Stories](features/user-stories/README.md) - User stories backlog
- [Backend README](backend/README.md) - Backend-specific documentation (if exists)
- [Frontend README](frontend/README.md) - Frontend-specific documentation

### External Resources

- [Quarkus Documentation](https://quarkus.io/guides/)
- [Angular Documentation](https://angular.io/docs)
- [Apache Lucene](https://lucene.apache.org/)
- [Neo4j Java Driver](https://neo4j.com/docs/java-manual/current/)
- [LangChain4j](https://github.com/langchain4j/langchain4j)
- [Tree-sitter](https://tree-sitter.github.io/tree-sitter/)

### Epic Documentation

- [EPIC-00: Project Infrastructure Setup](features/epics/EPIC-00-project-infrastructure.md)
- [EPIC-01: Code Ingestion & Indexing](features/epics/EPIC-01-ingestion.md)
- [EPIC-02: Hybrid Search & Retrieval](features/epics/EPIC-02-search.md)
- [EPIC-03: RAG Answer Generation](features/epics/EPIC-03-rag.md)
- [EPIC-04: REST API & CLI](features/epics/EPIC-04-api-cli.md)
- [EPIC-05: Web Dashboard](features/epics/EPIC-05-web-dashboard.md)
- [EPIC-06: Dependency Graph Analysis](features/epics/EPIC-06-dependency-graph.md)
- [EPIC-07: Documentation Intelligence](features/epics/EPIC-07-documentation.md)
- [EPIC-08: MCP Tool Server](features/epics/EPIC-08-mcp-server.md)

---

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

---

## Support

For issues, questions, or contributions:
- Open an issue on GitHub
- Check existing documentation
- Review epic and user story specifications

---

**Last Updated:** December 2025  
**Document Version:** 1.0.0

