# Deployment & Operations

This guide covers production builds, system requirements, deployment, and troubleshooting.

---

## Production Build

### Backend

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

### Frontend

```bash
cd frontend

# Build for production
npm run build
```

**Output:** `backend/src/main/resources/META-INF/resources/`

The frontend is served by Quarkus in production (no separate web server needed).

---

## System Requirements

**Minimum:**
- CPU: 2 cores
- RAM: 4 GB
- Disk: 10 GB

**Recommended:**
- CPU: 4+ cores
- RAM: 8+ GB
- Disk: 50+ GB (for indexes and graph database)

---

## Services & Ports

| Service | Default Port | Required |
|:--------|:-------------|:---------|
| MegaBrain Backend | 8080 | Yes |
| Angular Dev Server | 4200 | Development only |
| PostgreSQL | 5432 | Optional (for vector search) |
| Neo4j | 7687 (Bolt) | Optional (for graph queries) |
| Ollama | 11434 | Optional (for local LLM) |

---

## Docker Deployment

*Docker configuration will be added in future versions.*

---

## Production Configuration Checklist

1. Set environment variables for all sensitive data (tokens, API keys, passwords)
2. Configure database connections (PostgreSQL for vector search, Neo4j for graph)
3. Set up SSL/TLS certificates and enforce HTTPS
4. Configure logging levels (set `quarkus.log.level=WARN` for production)
5. Set up monitoring and alerts
6. Limit exposure of `/q/*` management endpoints
7. Never log tokens -- verify with `quarkus.log.category."io.megabrain".level=INFO`

### Operational Notes

- **Backend port** is configurable via `quarkus.http.port`
- **Health check:** `GET /q/health` returns service status
- **Bitbucket Server:** ensure `bitbucket-server-api/mp-rest/url` points to the server root (no `/rest` suffix); clone URL is derived as `<base>/scm/<project>/<repo>.git`
- **LLM providers:** prefer Ollama for on-premises deployments; cloud providers require API keys
- **Datastores:** configure PostgreSQL, Neo4j, Lucene index path, and optional vector store before production use

---

## Troubleshooting

### Backend Won't Start

**Issue:** `Port 8080 already in use`

**Solution:**
```bash
lsof -i :8080
# Kill the process or change port:
# quarkus.http.port=8081
```

**Issue:** `Dependency resolution failed`

**Solution:**
```bash
cd backend
mvn clean install -U
```

### Frontend Won't Start

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

### LLM Provider Issues

**Issue:** `Ollama connection failed`

**Solution:**
1. Verify Ollama is running: `ollama serve`
2. Check base URL in `application.properties` (`megabrain.llm.ollama.base-url`)
3. Verify model is available: `ollama list`

**Issue:** `OpenAI/Anthropic API key invalid`

**Solution:**
1. Verify API key in environment variables
2. Check API key has correct permissions
3. Verify billing is active (for paid providers)

### Database Issues

**Issue:** `PostgreSQL connection refused`

**Solution:**
1. Verify PostgreSQL is running: `pg_isready`
2. Check connection URL in `application.properties`
3. Ensure `quarkus.datasource.jdbc=true` (disabled by default)

**Issue:** `pgvector extension not found`

**Solution:**
```sql
CREATE EXTENSION IF NOT EXISTS vector;
```

### Debugging

**Enable Debug Logging:**

```properties
# Backend
quarkus.log.level=DEBUG
quarkus.log.category."io.megabrain".level=TRACE
```

**Frontend:**
- Open browser DevTools (F12)
- Check Console and Network tabs

**Common Log Locations:**
- Backend logs: Console output (Quarkus dev mode)
- Frontend logs: Browser console
