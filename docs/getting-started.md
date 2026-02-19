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

---

## Next Steps

- [Configure the application](configuration-reference.md) for your environment
- [Read the API Reference](api-reference.md) to start making requests
- [Explore the architecture](architecture.md) to understand the system
