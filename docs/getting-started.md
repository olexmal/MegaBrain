# Getting Started

This guide covers prerequisites, installation, and verification for MegaBrain.

---

## Prerequisites

### Backend Prerequisites

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
  For **offline operation**: run Ollama locally and pull models before disconnecting (e.g. `ollama pull codellama`). Inference uses only the configured endpoint; no internet is required at runtime.

### Frontend Prerequisites

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

---

## Installation Steps

### 1. Clone the Repository

```bash
git clone <repository-url>
cd MegaBrain
```

### 2. Backend Setup

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

### 3. Frontend Setup

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

### 4. Verify Installation

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

### 5. CLI (optional)

When the backend is built for CLI mode, you can run the MegaBrain CLI. The **ingest** and **search** commands are available; use `megabrain ingest --help` or `megabrain search --help` to see usage and options. The search command supports filter options (`--language`, `--repo`, `--type`, `--limit`) and output options (`--json`, `--quiet`, `--no-color`); see [CLI Reference](cli-reference.md#megabrain-search) for details. Use `--json` for scripting (e.g. `megabrain search "query" --json` or `--json --quiet` for the results array only). When you run an ingest (e.g. `megabrain ingest --source github --repo owner/repo`), progress is streamed in the terminal. Use `--verbose` for detailed progress and stack traces on errors.

```bash
cd backend
mvn package
java -jar target/quarkus-app/quarkus-run.jar ingest --help
```

### 6. Local LLM (Ollama) – offline operation

To use the local LLM **without internet connectivity** (AC3):

1. Install and start Ollama on the same machine or a reachable host.
2. Pull the model you will use while online: `ollama pull codellama` (or `mistral`, `llama2`, etc.).
3. Configure MegaBrain to use that endpoint (default is `http://localhost:11434`) and the same model name in `application.properties` (see [Configuration Reference](configuration-reference.md#ollama-freelocal)).
4. At runtime, all LLM requests go only to the configured Ollama endpoint; no external API calls are made. You can disconnect the network and continue using the local LLM.

---

## Next Steps

- [Configure the application](configuration-reference.md) for your environment
- [Read the API Reference](api-reference.md) to start making requests
- [Explore the architecture](architecture.md) to understand the system
