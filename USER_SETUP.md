# MegaBrain Deployment & User Setup Guide

Audience: operators running MegaBrain backend for ingestion/search. This focuses on configuration and runtime verification (no code changes required).

## Prerequisites
- Java 22+, Maven 3.8+ (backend)
- Optional: Node 18+/Angular CLI 20+ (frontend UI)
- Network access to Git providers (GitHub, Bitbucket Cloud/Server)
- PostgreSQL (for persistence/vector) and Neo4j (graph) if you enable those features

## Services & Ports
- Backend (Quarkus): `:8080`
- Frontend dev server (optional): `:4200` (proxies API to `:8080`)
- Health endpoint: `http://localhost:8080/q/health`

## Required Configuration (env vars or `backend/src/main/resources/application.properties`)
- GitHub:
  - `megabrain.github.token` (or env `GITHUB_TOKEN`) — optional for public repos, required for private.
- Bitbucket Cloud:
  - `megabrain.bitbucket.cloud.username` / `megabrain.bitbucket.cloud.app-password`
- Bitbucket Server/Data Center:
  - `bitbucket-server-api/mp-rest/url` — must be the server root (e.g., `https://bitbucket.myco.com`), not a `/rest` path.
  - `megabrain.bitbucket.server.username` / `megabrain.bitbucket.server.token` (PAT). Uses Basic auth (`username:PAT`).
- LLM providers (pick one):
  - Ollama (local): `megabrain.llm.provider=ollama`, `megabrain.llm.ollama.base-url`, `megabrain.llm.ollama.model`
  - OpenAI/Anthropic: set provider plus API key (`OPENAI_API_KEY` / `ANTHROPIC_API_KEY`)
- Storage (configure if enabling):
  - PostgreSQL: datasource URL/username/password
  - Neo4j: `neo4j.uri`, `neo4j.username`, `neo4j.password`
  - Lucene index dir: `megabrain.index.directory`
  - Grammar cache: `megabrain.grammar.cache.directory`

## Source Control Authentication Notes
- Bitbucket Server clone URLs are derived as `<bitbucket-server-api/mp-rest/url>/scm/<project>/<repo>.git`.
- Bitbucket Server URLs supported: REST (`/projects/<proj>/repos/<repo>`), SCM (`/scm/<proj>/<repo>.git`), SSH (`git@host:<proj>/<repo>.git`).
- GitHub tokens: if not prefixed with `Bearer`/`token`, headers add `token <value>` automatically.

## Run the Backend
```bash
cd backend
mvn clean test        # optional but recommended
mvn quarkus:dev       # starts on http://localhost:8080
```

## Quick Verification
```bash
curl http://localhost:8080/q/health
```
- Expect `UP` status. If using protected repos, verify tokens are present in environment or properties.

## Troubleshooting
- Bitbucket Server clone fails: ensure `bitbucket-server-api/mp-rest/url` points to server root (no `/rest` suffix) and PAT is valid.
- Unauthorized on Bitbucket Server REST: PAT must be sent as Basic (`username:PAT`); ensure `megabrain.bitbucket.server.username/token` are set.
- Missing files in extraction: hidden/binary files are intentionally filtered; `.gitignore` support is pending.
- LLM connection errors: confirm provider base URL/API key and that the service is reachable from the backend host.

