# MegaBrain RAG Pipeline

Scalable, self-hosted, intelligent code knowledge platform that indexes multi-language source code from various repositories and provides precise semantic search and natural language Q&A through a modern, reactive architecture.

## Documentation

- [Documentation Hub](docs/README.md) - Complete documentation index
- [Getting Started](docs/getting-started.md) - Prerequisites and installation
- [API Reference](docs/api-reference.md) - REST API endpoints and parameters
- [Configuration Reference](docs/configuration-reference.md) - All configuration properties
- [Implemented Features](docs/implemented-features.md) - Detailed feature documentation
- [Architecture](docs/architecture.md) - Component architecture and data flow
- [Development Guide](docs/development-guide.md) - Coding standards and contributing
- [Deployment & Operations](docs/deployment.md) - Production build and troubleshooting
- [User Setup Guide](USER_SETUP.md) - Deployment and user configuration

## Technology Stack

- **Backend Framework:** Quarkus 3.30.2 (RESTEasy Reactive)
- **Language:** Java 22+
- **Build Tool:** Maven 3.8+
- **Code Parsing:** JavaParser 3.27.1, Tree-sitter (java-tree-sitter 0.25.6)
- **Search:** Apache Lucene 10.3.2
- **Graph Database:** Neo4j Java Driver 6.0.2
- **LLM Integration:** LangChain4j 1.9.1 (Ollama, OpenAI, Anthropic)
- **Git Operations:** JGit 7.4.0
- **CLI:** Picocli with Quarkus Integration
- **Scheduling:** Quartz Scheduler
- **Database:** PostgreSQL (with pgvector for vector search)

## Project Structure

```
megabrain/
├── backend/                      # Java/Quarkus backend
│   ├── pom.xml                  # Maven project configuration
│   └── src/
│       ├── main/
│       │   ├── java/
│       │   │   └── io/megabrain/
│       │   │       ├── api/          # REST endpoints
│       │   │       ├── core/         # Core services (search, vector, graph, LLM)
│       │   │       ├── ingestion/    # Code ingestion & parsing
│       │   │       └── repository/   # Repository state persistence
│       │   └── resources/
│       │       └── application.properties
│       └── test/
│           ├── java/
│           └── resources/
├── frontend/                     # Angular 20 frontend
│   ├── package.json             # npm dependencies
│   ├── angular.json             # Angular CLI configuration
│   ├── tsconfig.json            # TypeScript configuration
│   └── src/
│       ├── app/                  # Angular components
│       ├── assets/               # Static assets
│       └── environments/         # Environment configuration
├── docs/                         # Documentation
│   ├── README.md                # Documentation hub
│   ├── getting-started.md       # Installation & setup
│   ├── architecture.md          # Component architecture
│   ├── implemented-features.md  # Completed feature details
│   ├── api-reference.md         # REST API reference
│   └── configuration-reference.md # Configuration properties
├── features/                     # Feature specifications
│   ├── epics/                    # Epic definitions
│   └── user-stories/            # User stories and tasks
└── README.md
```

## Getting Started

### Prerequisites

**Backend:**
- Java 22 or higher
- Maven 3.8 or higher

**Frontend:**
- Node.js 18+ and npm
- Angular CLI 20 (install globally: `npm install -g @angular/cli@20`)

### Build and Run Backend

```bash
cd backend

# Compile the project
mvn clean compile

# Run tests
mvn test

# Start in development mode
mvn quarkus:dev

# Build native executable (requires GraalVM)
mvn clean package -Pnative
```

### Build and Run Frontend

```bash
cd frontend

# Install dependencies
npm install

# Start development server (with API proxy)
npm start
# or
ng serve

# Build for production
npm run build
```

The frontend development server runs on `http://localhost:4200` and proxies API requests to `http://localhost:8080`.

### Health Check

Once the application is running, check the health endpoint:

```bash
curl http://localhost:8080/q/health
```

## Configuration

### Backend Configuration

Edit `backend/src/main/resources/application.properties` to configure:
- Database connections (PostgreSQL, Neo4j)
- LLM provider settings (Ollama, OpenAI, Anthropic)
- Lucene index directory
- Grammar cache directory

### Frontend Configuration

Edit `frontend/src/environments/environment.ts` for development or `frontend/src/environments/environment.prod.ts` for production to configure:
- API base URL
- Feature flags
- Other environment-specific settings

## Development

This project follows the MegaBrain feature specification organized by epics:
- EPIC-00: Project Infrastructure Setup (this setup)
- EPIC-01: Code Ingestion & Indexing
- EPIC-02: Hybrid Search & Retrieval
- EPIC-03: RAG Answer Generation
- EPIC-04: REST API & CLI
- EPIC-05: Web Dashboard
- EPIC-06: Dependency Graph Analysis
- EPIC-07: Documentation Intelligence
- EPIC-08: MCP Tool Server

See `features/` directory for detailed user stories and task breakdowns.

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

