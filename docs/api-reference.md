# API Reference

MegaBrain exposes a REST API for search, ingestion, and health checking.

---

## Base URL

- **Development:** `http://localhost:8080`
- **Production:** `https://your-domain.com`

## Authentication

Currently, authentication is not enforced. Future versions will support API keys, OAuth 2.0, and JWT tokens. Source control tokens (GitHub, GitLab, Bitbucket) are configured server-side via `application.properties` or environment variables.

---

## Health Check

```http
GET /q/health
```

**Response (200 OK):**
```json
{
  "status": "UP",
  "message": "MegaBrain is running"
}
```

---

## Search

### Search Code

```http
GET /api/v1/search
```

**Query Parameters:**

| Parameter | Type | Required | Default | Description |
|:----------|:-----|:---------|:--------|:------------|
| `query` | string | Yes | - | Search query string. Supports Lucene syntax: AND/OR/NOT, "phrase queries", wildcards (*, ?), field:value. Also supports structural queries: `implements:InterfaceName`, `extends:ClassName`, `usages:TypeName`. |
| `limit` | integer | No | 10 | Maximum number of results (1-100) |
| `mode` | string | No | `HYBRID` | Search mode: `HYBRID`, `KEYWORD`, or `VECTOR` |
| `transitive` | boolean | No | `false` | Enable transitive relationship traversal for structural queries |
| `depth` | integer | No | 5 | Maximum traversal depth for transitive queries (1-10) |
| `language` | string | No | - | Filter by programming language (e.g., `java`, `python`). Multiple values supported. |
| `repository` | string | No | - | Filter by repository name. Multiple values supported. |
| `file_path` | string | No | - | Filter by file path prefix |
| `entity_type` | string | No | - | Filter by entity type: `class`, `method`, `field`, `function` |
| `include_field_match` | boolean | No | `false` | Include per-field match scores in results (uses Lucene Explanation API) |

**Example Request:**
```bash
curl "http://localhost:8080/api/v1/search?query=getUserName&limit=5&language=java&mode=HYBRID"
```

**Response (200 OK):**
```json
{
  "results": [
    {
      "content": "public String getUserName() { return this.userName; }",
      "entity_name": "UserService.getUserName",
      "entity_type": "method",
      "source_file": "src/main/java/com/example/UserService.java",
      "language": "java",
      "repository": "my-app",
      "score": 0.95,
      "line_range": {
        "start": 42,
        "end": 44
      },
      "doc_summary": "Returns the user name for the current session",
      "is_transitive": false,
      "relationship_path": null,
      "field_match": null
    }
  ],
  "total": 1,
  "page": 1,
  "size": 5,
  "facets": {
    "language": [
      { "value": "java", "count": 150 },
      { "value": "typescript", "count": 42 }
    ],
    "repository": [
      { "value": "my-app", "count": 120 }
    ],
    "entity_type": [
      { "value": "method", "count": 85 },
      { "value": "class", "count": 45 }
    ]
  }
}
```

### Search Response Format

**SearchResponse:**

| Field | Type | Description |
|:------|:-----|:------------|
| `results` | SearchResult[] | Array of search results |
| `total` | integer | Total number of matching results |
| `page` | integer | Current page number |
| `size` | integer | Page size |
| `facets` | Map<string, FacetValue[]> | Available filter values with counts |

**SearchResult:**

| Field | Type | Description |
|:------|:-----|:------------|
| `content` | string | Code content snippet |
| `entity_name` | string | Fully qualified entity name |
| `entity_type` | string | Entity type (class, method, field, function) |
| `source_file` | string | Source file path |
| `language` | string | Programming language |
| `repository` | string | Repository name |
| `score` | float | Relevance score (0.0 - 1.0) |
| `line_range` | LineRange | Start and end line numbers |
| `doc_summary` | string | Documentation summary (if available) |
| `is_transitive` | boolean | Whether found via transitive traversal |
| `relationship_path` | string[] | Traversal path for transitive results (e.g., `["Interface", "AbstractClass", "ConcreteClass"]`) |
| `field_match` | FieldMatchInfo | Per-field match details (when `include_field_match=true`) |

**FieldMatchInfo:**

| Field | Type | Description |
|:------|:-----|:------------|
| `matched_fields` | string[] | Fields that matched the query |
| `scores` | Map<string, float> | Per-field relevance scores |

### Search Modes

| Mode | Description |
|:-----|:------------|
| `HYBRID` | Combines keyword search (Lucene) and vector search (pgvector) with weighted scoring. Default mode. |
| `KEYWORD` | Uses only Lucene keyword search. Faster, no vector database required. |
| `VECTOR` | Uses only vector similarity search. Requires pgvector configured. |

### Structural Queries

Structural queries leverage the dependency graph for relationship-aware search:

| Syntax | Description | Example |
|:-------|:------------|:--------|
| `implements:InterfaceName` | Find all classes implementing an interface | `implements:Repository` |
| `extends:ClassName` | Find all subclasses of a class | `extends:AbstractService` |
| `usages:TypeName` | Find all usages including polymorphic call sites | `usages:UserService` |

When `transitive=true`, these queries traverse the full inheritance hierarchy up to the configured depth.

---

## Ingestion

### Trigger Ingestion

```http
POST /api/v1/ingestion
Content-Type: application/json
```

**Request Body:**
```json
{
  "url": "https://github.com/user/repo",
  "branch": "main",
  "provider": "github"
}
```

| Field | Type | Required | Description |
|:------|:-----|:---------|:------------|
| `url` | string | Yes | Repository URL |
| `branch` | string | No | Branch to ingest (defaults to default branch) |
| `provider` | string | Yes | Source control provider: `github`, `gitlab`, `bitbucket` |

**Response (200 OK):**
Streams progress events via Server-Sent Events (SSE) with stage, message, and percentage fields.

---

## CLI

Run via Quarkus CLI or packaged JAR:

```bash
# Ingest a repository
megabrain ingest --url https://github.com/user/repo --branch main

# Search code
megabrain search --query "dependency graph builder" --limit 5

# Get help
megabrain --help
```

---

## MCP Server

MegaBrain supports the Model Context Protocol (MCP) for LLM tool integration.

- **Transport:** stdio (primary), SSE (secondary)
- **Purpose:** Expose search, ingestion, and dependency tools to LLM clients
- See EPIC-08 documentation for detailed protocol and tool schemas

---

## Error Responses

All errors follow this format:

```json
{
  "error": "Error Type",
  "message": "Human-readable error message",
  "timestamp": "2026-02-19T10:30:00Z",
  "path": "/api/v1/endpoint"
}
```

**HTTP Status Codes:**

| Code | Meaning |
|:-----|:--------|
| `200 OK` | Success |
| `201 Created` | Resource created |
| `400 Bad Request` | Invalid request (e.g., missing query, depth out of range) |
| `404 Not Found` | Resource not found |
| `429 Too Many Requests` | Rate limit exceeded |
| `500 Internal Server Error` | Server error |
