# Configuration Reference

All configuration is managed via `backend/src/main/resources/application.properties`. Sensitive values should use environment variable substitution.

---

## Application Settings

| Property | Default | Description |
|:---------|:--------|:------------|
| `quarkus.application.name` | `megabrain` | Application name |
| `quarkus.application.version` | `1.0.0-SNAPSHOT` | Application version |
| `quarkus.http.port` | `8080` | HTTP server port |
| `quarkus.http.cors` | `true` | Enable CORS |
| `quarkus.log.level` | `INFO` | Root log level |
| `quarkus.log.category."io.megabrain".level` | `DEBUG` | MegaBrain log level |

---

## Search Configuration

### Field Boosts

Control relevance ranking by boosting matches in specific fields.

| Property | Default | Description |
|:---------|:--------|:------------|
| `megabrain.search.boost.entity-name` | `3.0` | Boost for entity name matches (class/method names) |
| `megabrain.search.boost.doc-summary` | `2.0` | Boost for documentation summary matches |
| `megabrain.search.boost.content` | `1.0` | Boost for code content matches |

### Hybrid Ranking Weights

Control the balance between keyword and vector search. Weights must sum to 1.0.

| Property | Default | Description |
|:---------|:--------|:------------|
| `megabrain.search.hybrid.keyword-weight` | `0.6` | Weight for Lucene keyword search scores |
| `megabrain.search.hybrid.vector-weight` | `0.4` | Weight for vector similarity scores |

### Transitive Search

Configure depth limits for graph-based transitive searches.

| Property | Default | Description |
|:---------|:--------|:------------|
| `megabrain.search.transitive.default-depth` | `5` | Default traversal depth when `depth` param not specified |
| `megabrain.search.transitive.max-depth` | `10` | Maximum allowed traversal depth (enforced on API) |

---

## Index Configuration

| Property | Default | Description |
|:---------|:--------|:------------|
| `megabrain.index.directory` | `./data/index` | Directory for Lucene index files |
| `megabrain.index.batch.size` | `1000` | Batch size for bulk indexing operations |

---

## Vector Search Configuration

| Property | Default | Description |
|:---------|:--------|:------------|
| `megabrain.vector.ef-search` | `40` | HNSW ef_search parameter (higher = more accurate, slower) |

---

## Grammar Configuration

| Property | Default | Description |
|:---------|:--------|:------------|
| `megabrain.grammar.cache.directory` | `~/.megabrain/grammars` | Local cache directory for Tree-sitter grammar binaries |

---

## LLM Provider Configuration

### Provider Selection

| Property | Default | Description |
|:---------|:--------|:------------|
| `megabrain.llm.provider` | `ollama` | Active LLM provider: `ollama`, `openai`, or `anthropic` |

### Ollama (Free/Local)

| Property | Default | Description |
|:---------|:--------|:------------|
| `megabrain.llm.ollama.base-url` | `http://localhost:11434` | Ollama API endpoint |
| `megabrain.llm.ollama.model` | `codellama` | Model name (codellama, mistral, llama2, phi, etc.) |
| `megabrain.llm.ollama.timeout-seconds` | `60` | Request timeout in seconds |

### OpenAI (Paid Cloud)

| Property | Default | Description |
|:---------|:--------|:------------|
| `megabrain.llm.openai.api-key` | - | OpenAI API key. Use `${OPENAI_API_KEY}` for env var. |
| `megabrain.llm.openai.chat-model.model-name` | `gpt-3.5-turbo` | Model: gpt-3.5-turbo, gpt-4, gpt-4-turbo |

### Anthropic Claude (Paid Cloud)

| Property | Default | Description |
|:---------|:--------|:------------|
| `megabrain.llm.anthropic.api-key` | - | Anthropic API key. Use `${ANTHROPIC_API_KEY}` for env var. |
| `megabrain.llm.anthropic.chat-model.model-name` | `claude-3-sonnet-20240229` | Model: claude-3-sonnet, claude-3-opus |

---

## Database Configuration

### PostgreSQL (for vector search)

| Property | Default | Description |
|:---------|:--------|:------------|
| `quarkus.datasource.db-kind` | `postgresql` | Database type |
| `quarkus.datasource.username` | - | Database username |
| `quarkus.datasource.password` | - | Database password. Use `${POSTGRES_PASSWORD}`. |
| `quarkus.datasource.jdbc.url` | - | JDBC connection URL |
| `quarkus.datasource.jdbc` | `false` | Set to `true` when database is configured |

### Flyway Migrations

| Property | Default | Description |
|:---------|:--------|:------------|
| `quarkus.flyway.migrate-at-start` | `false` | Run migrations on startup |
| `quarkus.flyway.baseline-on-migrate` | `false` | Baseline existing database |

### Neo4j (for graph database)

| Property | Default | Description |
|:---------|:--------|:------------|
| `megabrain.neo4j.uri` | - | Neo4j Bolt URI (e.g., `bolt://localhost:7687`). When unset, graph queries return empty. |
| `megabrain.neo4j.username` | `neo4j` | Neo4j username |
| `megabrain.neo4j.password` | - | Neo4j password. Use `${NEO4J_PASSWORD}`. |

---

## Source Control Configuration

### GitHub

| Property | Default | Description |
|:---------|:--------|:------------|
| `github-api/mp-rest/url` | `https://api.github.com` | GitHub API base URL |
| `megabrain.github.token` | - | GitHub PAT or App token. Use `${GITHUB_TOKEN}`. |

### GitLab

| Property | Default | Description |
|:---------|:--------|:------------|
| `megabrain.gitlab.api-url` | `https://gitlab.com` | GitLab API base URL (supports self-hosted) |
| `megabrain.gitlab.token` | - | GitLab access token. Use `${GITLAB_TOKEN}`. |
| `megabrain.gitlab.ssl.trust-store` | - | Path to custom trust store (for self-hosted SSL) |
| `megabrain.gitlab.ssl.verify-ssl` | `true` | Enable SSL verification |

### Bitbucket

| Property | Default | Description |
|:---------|:--------|:------------|
| `bitbucket-cloud-api/mp-rest/url` | `https://api.bitbucket.org` | Bitbucket Cloud API URL |
| `bitbucket-server-api/mp-rest/url` | `http://localhost:7990` | Bitbucket Server/DC base URL (no `/rest` suffix) |
| `megabrain.bitbucket.cloud.username` | - | Bitbucket Cloud username |
| `megabrain.bitbucket.cloud.app-password` | - | Bitbucket Cloud app password |
| `megabrain.bitbucket.server.username` | - | Bitbucket Server username |
| `megabrain.bitbucket.server.token` | - | Bitbucket Server PAT |

---

## Frontend Configuration

### Development Environment

Edit `frontend/src/environments/environment.ts`:

```typescript
export const environment = {
  production: false,
  apiUrl: 'http://localhost:8080/api/v1'
};
```

### Production Environment

Edit `frontend/src/environments/environment.prod.ts`:

```typescript
export const environment = {
  production: true,
  apiUrl: '/api/v1'
};
```

### API Proxy (Development)

The `frontend/proxy.conf.json` proxies API requests to the backend:

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

---

## Environment Variables

For sensitive values, use environment variables:

```bash
export GITHUB_TOKEN=ghp_...
export GITLAB_TOKEN=glpat_...
export OPENAI_API_KEY=sk-...
export ANTHROPIC_API_KEY=sk-...
export POSTGRES_PASSWORD=your_password
export NEO4J_PASSWORD=your_password
export BITBUCKET_CLOUD_USERNAME=your_username
export BITBUCKET_CLOUD_APP_PASSWORD=your_app_password
export BITBUCKET_SERVER_URL=https://bitbucket.mycompany.com
export BITBUCKET_SERVER_TOKEN=your_pat
```

Reference in `application.properties`:
```properties
megabrain.github.token=${GITHUB_TOKEN}
megabrain.llm.openai.api-key=${OPENAI_API_KEY}
```
