

# Test Specification: MegaBrain RAG Pipeline

## Reviewers

- Reviewer 1: Product Owner
- Reviewer 2: Lead Developer
- Reviewer 3: QA Lead

## Introduction

This specification covers comprehensive testing of the MegaBrain RAG Pipeline — a scalable, self-hosted code knowledge platform that indexes multi-language source code from various repositories and provides semantic search, natural language Q&A, dependency graph analysis, documentation intelligence, and LLM tool integration via the Model Context Protocol (MCP).

**Scope:** All functional requirements (FR-ING, FR-SRH, FR-RAG, FR-IFC, FR-DEP, FR-DOC, FR-MCP) and non-functional requirements (performance, scalability, privacy, security) as defined in the Feature Specification v1.0.

**Objectives:**

- Validate that code ingestion works across all supported source control systems and languages.
- Ensure hybrid search returns accurate, contextually relevant results.
- Verify RAG answers are grounded in actual code with proper source attribution.
- Confirm all system interfaces (REST API, CLI, Web Dashboard, MCP Server) function correctly.
- Validate dependency graph extraction, storage, and querying.
- Verify documentation intelligence extraction, indexing, and quality metrics.
- Ensure MCP server tools, resources, and transports work as specified.
- Verify non-functional requirements (performance, scalability, privacy, security).

**Assumptions:**

- A dedicated test repository with known code structures, dependencies, and documentation is available.
- Ollama is running locally with at least one model (e.g., Codellama) for LLM tests.
- PostgreSQL with pgvector extension is available for vector storage tests.
- Neo4j or JanusGraph is available for graph database tests.
- Test environment has network access for GitHub/Bitbucket/GitLab API tests (integration tests only).

## References

- Epic: [MegaBrain RAG Pipeline Feature Specification](../../features/feature_specification.md)
- Business Case: [MegaBrain README](../../README.md)
- Documentation: [MegaBrain Documentation](../../DOCUMENTATION.md)

---

## Test Cases

### Section 1: Code Ingestion & Indexing (FR-ING)

Grouped inputs for this section: source type (GitHub, Bitbucket, GitLab, Local Git), repository identifier, branch, credentials, file types (.java, .py, .c, .cpp, .js, .ts, .go, .rs, .kt, .rb, .scala, .swift, .php, .cs), indexing mode (full / incremental).

---

### Test Case 1: Successful full ingestion from GitHub with multiple languages


|                      |                                                                                                                                                                                                                                                                                                                                                              |
| -------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| **ID**               | TC001                                                                                                                                                                                                                                                                                                                                                        |
| **Description**      | Verify that a full ingestion from a GitHub repository correctly clones, parses multi-language files, and indexes all code chunks with metadata.                                                                                                                                                                                                              |
| **Preconditions**    | GitHub test repository exists with Java, Python, TypeScript, and Go files. Valid GitHub PAT is configured. No prior index exists.                                                                                                                                                                                                                            |
| **Test Data**        | Source: `github` Repository: `test-org/polyglot-repo` Branch: `main` Credentials: valid PAT via env var                                                                                                                                                                                                                                                      |
| **Steps**            | 1. Trigger ingestion via `POST /api/v1/ingest/github` with repository and branch. 2. Observe SSE stream for progress events. 3. Wait for "Complete" stage. 4. Query the index for known entities from each language.                                                                                                                                         |
| **Expected Results** | SSE stream emits stages: "Cloning" → "Parsing" → "Embedding" → "Storing" → "Complete" with percentage updates. Index contains chunks for Java classes (via JavaParser), Python functions, TypeScript modules, and Go functions (via Tree-sitter). Each chunk has correct `language`, `entity_type`, `entity_name`, `source_file`, and `byte_range` metadata. |


### Test Case 2: Multi-source ingestion across all SCM providers


|                      |                                                                                                                                                                                                                                                   |
| -------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **ID**               | TC002                                                                                                                                                                                                                                             |
| **Description**      | Verify that the `SourceControlClientFactory` correctly provides clients for all supported SCM providers and that ingestion succeeds for each.                                                                                                     |
| **Preconditions**    | Test repositories exist on GitHub, Bitbucket, and GitLab. A local Git repository is also available. Valid credentials are configured for each.                                                                                                    |
| **Test Data**        | Sources: `github`, `bitbucket`, `gitlab`, `local-git` Repositories: one per source with known Java files Branch: `main` for all                                                                                                                   |
| **Steps**            | 1. Trigger ingestion for GitHub repository. 2. Trigger ingestion for Bitbucket repository. 3. Trigger ingestion for GitLab repository. 4. Trigger ingestion for local Git repository. 5. Verify each completes and indexes the expected entities. |
| **Expected Results** | All four ingestion jobs complete successfully. Each source's entities are indexed with correct `repository` metadata. The `Multi<Document>` stream from each source yields the expected file count.                                               |


### Test Case 3: Incremental indexing processes only changed files


|                      |                                                                                                                                                                                                                                                              |
| -------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| **ID**               | TC003                                                                                                                                                                                                                                                        |
| **Description**      | Verify that after a full index, a subsequent incremental indexing run only processes files changed since the last run (based on `git diff`).                                                                                                                 |
| **Preconditions**    | Repository has been fully indexed once. A known commit has modified 2 files and added 1 new file.                                                                                                                                                            |
| **Test Data**        | Repository: `test-org/polyglot-repo` Branch: `main` Changed files: `src/Main.java` (modified), `src/utils.py` (modified), `src/NewService.ts` (added)                                                                                                        |
| **Steps**            | 1. Trigger incremental ingestion. 2. Monitor SSE stream for files processed. 3. Verify only the 3 changed/new files are parsed and re-indexed. 4. Verify unchanged files retain their original index entries.                                                |
| **Expected Results** | Incremental job processes exactly 3 files. Updated chunks replace old entries for `Main.java` and `utils.py`. New chunks are added for `NewService.ts`. All other index entries remain unchanged. Ingestion time is significantly less than a full re-index. |


### Test Case 4: Real-time SSE progress streaming during ingestion


|                      |                                                                                                                                                                                                                        |
| -------------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **ID**               | TC004                                                                                                                                                                                                                  |
| **Description**      | Verify that SSE progress events are emitted in the correct order with valid stage names, messages, and percentage values during ingestion.                                                                             |
| **Preconditions**    | A valid repository is configured for ingestion. SSE client is connected.                                                                                                                                               |
| **Test Data**        | Repository: `test-org/small-repo` (< 100 files for fast execution)                                                                                                                                                     |
| **Steps**            | 1. Connect an SSE client to the ingestion endpoint. 2. Trigger ingestion. 3. Collect all `StreamEvent` objects from the SSE stream. 4. Verify stage ordering, percentage progression, and message content.             |
| **Expected Results** | Events arrive in order: "Cloning" (0-20%) → "Parsing" (20-60%) → "Embedding" (60-80%) → "Storing" (80-95%) → "Complete" (100%). Each event contains a non-empty `message`. Percentage is monotonically non-decreasing. |


### Test Case 5: Parser routing — Java files to JavaParser, others to Tree-sitter


|                      |                                                                                                                                                                                                                                                                                                                                     |
| -------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **ID**               | TC005                                                                                                                                                                                                                                                                                                                               |
| **Description**      | Verify that `.java` files are routed to JavaParser and all other supported extensions are routed to Tree-sitter with the correct grammar.                                                                                                                                                                                           |
| **Preconditions**    | Test repository contains files: `App.java`, `main.py`, `index.ts`, `server.go`, `lib.rs`, `Service.kt`, `app.rb`, `Main.scala`, `ViewController.swift`, `index.php`, `Program.cs`, `module.c`, `module.cpp`, `script.js`.                                                                                                           |
| **Test Data**        | All 14 files with known class/function structures.                                                                                                                                                                                                                                                                                  |
| **Steps**            | 1. Trigger full ingestion. 2. For each file, verify the parser that processed it (via logs or metadata). 3. Verify that chunks contain correct `language` metadata and proper entity extraction for each language.                                                                                                                  |
| **Expected Results** | `App.java` is parsed by JavaParser with deep Java analysis (annotations, generics, etc.). All other files are parsed by Tree-sitter with language-specific grammars. Each chunk has the correct `language` value matching its file extension. Entity types (class, function, method, module) are correctly identified per language. |


### Test Case 6: Extended language grammar management — download, cache, update, rollback


|                      |                                                                                                                                                                                                                                                                                                                                 |
| -------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **ID**               | TC006                                                                                                                                                                                                                                                                                                                           |
| **Description**      | Verify that the Grammar Manager can download grammars on demand, cache them locally, update to new versions, and rollback on failure.                                                                                                                                                                                           |
| **Preconditions**    | Grammar cache directory is empty. Network access to grammar repositories is available.                                                                                                                                                                                                                                          |
| **Test Data**        | Languages: Go, Rust, Kotlin Version pin: `v0.20.0` for Go grammar Simulated bad grammar for rollback test                                                                                                                                                                                                                       |
| **Steps**            | 1. Request parsing of a `.go` file — grammar should be downloaded on demand. 2. Verify grammar is cached locally. 3. Request the same language again — verify it loads from cache (no download). 4. Trigger a grammar update for Go. 5. Simulate a corrupt grammar download and verify rollback to the previous cached version. |
| **Expected Results** | First load: grammar downloaded in <500ms (cold start requirement). Second load: grammar loaded from cache (near-instant). Update: new version replaces cached version. Rollback: corrupt grammar is rejected, previous version restored, and parsing continues to work. Version pinning is respected.                           |


### Test Case 7: Ingestion with invalid credentials


|                      |                                                                                                                                                                                                    |
| -------------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **ID**               | TC007                                                                                                                                                                                              |
| **Description**      | Verify that ingestion fails gracefully with a clear error message when invalid credentials are provided, without exposing credential values.                                                       |
| **Preconditions**    | GitHub repository requires authentication.                                                                                                                                                         |
| **Test Data**        | Source: `github` Repository: `test-org/private-repo` Credentials: invalid/expired PAT                                                                                                              |
| **Steps**            | 1. Configure an invalid PAT. 2. Trigger ingestion for the private repository. 3. Observe the error response.                                                                                       |
| **Expected Results** | HTTP 401 or appropriate error status returned. Error message indicates authentication failure without exposing the token value. SSE stream emits an error event. No partial index data is created. |


### Test Case 8: Ingestion of a repository with unsupported file types


|                      |                                                                                                                                                                                                          |
| -------------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **ID**               | TC008                                                                                                                                                                                                    |
| **Description**      | Verify that files with unsupported extensions are skipped gracefully during ingestion while supported files are processed normally.                                                                      |
| **Preconditions**    | Test repository contains a mix of supported (.java, .py) and unsupported (.pdf, .png, .zip) files.                                                                                                       |
| **Test Data**        | Repository with: `App.java`, `main.py`, `diagram.pdf`, `logo.png`, `archive.zip`                                                                                                                         |
| **Steps**            | 1. Trigger full ingestion. 2. Verify that only supported files produce index chunks. 3. Verify unsupported files are logged as skipped (not errored).                                                    |
| **Expected Results** | `App.java` and `main.py` are parsed and indexed. `diagram.pdf`, `logo.png`, and `archive.zip` are silently skipped. No errors in the SSE stream for unsupported files. Ingestion completes successfully. |


---

### Section 2: Hybrid Search & Retrieval (FR-SRH)

Grouped inputs for this section: query string, language filter, repository filter, file_path filter, entity_type filter (class/method/function), search type (keyword / vector / hybrid).

---

### Test Case 9: Hybrid search combines keyword and vector results


|                      |                                                                                                                                                                                                                                                                                                               |
| -------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **ID**               | TC009                                                                                                                                                                                                                                                                                                         |
| **Description**      | Verify that hybrid search returns results combining keyword matches (exact class/method names) and vector similarity matches (conceptual queries) with a weighted ranking score.                                                                                                                              |
| **Preconditions**    | Index contains code from a test repository with known classes, methods, and documentation. Both Lucene and pgvector indexes are populated.                                                                                                                                                                    |
| **Test Data**        | Query 1 (keyword): `"UserAuthenticationService"` Query 2 (conceptual): `"how is user login handled"`                                                                                                                                                                                                          |
| **Steps**            | 1. Execute keyword query via `GET /api/v1/search?q=UserAuthenticationService`. 2. Verify exact match is ranked first. 3. Execute conceptual query via `GET /api/v1/search?q=how is user login handled`. 4. Verify results include the authentication service even though the query wording differs from code. |
| **Expected Results** | Query 1: `UserAuthenticationService` class chunk is the top result (keyword match). Query 2: Authentication-related code chunks appear in top results via vector similarity, even though "login" may not appear verbatim in the code. Both queries return results with a combined relevance score.            |


### Test Case 10: Context-aware filtering by language, repository, entity type, and file path


|                      |                                                                                                                                                                                                                                                                                                               |
| -------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **ID**               | TC010                                                                                                                                                                                                                                                                                                         |
| **Description**      | Verify that search results can be filtered by multiple metadata facets simultaneously and that combining filters narrows results correctly.                                                                                                                                                                   |
| **Preconditions**    | Index contains code from multiple repositories in Java, Python, and TypeScript with classes, methods, and functions.                                                                                                                                                                                          |
| **Test Data**        | Query: `"parse"` Filters: `language=java`, `repository=test-org/backend`, `entity_type=method`                                                                                                                                                                                                                |
| **Steps**            | 1. Search with no filters — note total result count. 2. Add `language=java` filter — verify only Java results returned. 3. Add `repository=test-org/backend` — verify results limited to that repo. 4. Add `entity_type=method` — verify only method entities returned. 5. Combine all three filters at once. |
| **Expected Results** | Each added filter progressively narrows results. Combined filters return only Java methods from `test-org/backend` that match "parse". No results from other languages, repositories, or entity types leak through.                                                                                           |


### Test Case 11: Relevance tuning — entity_name matches boosted over code body


|                      |                                                                                                                                                              |
| -------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| **ID**               | TC011                                                                                                                                                        |
| **Description**      | Verify that a match in `entity_name` (class/method name) is ranked higher than a match found only in general code comments or body.                          |
| **Preconditions**    | Index contains: (A) a method named `parseJsonResponse` and (B) a method named `processData` that has a comment "parse JSON response here".                   |
| **Test Data**        | Query: `"parseJsonResponse"`                                                                                                                                 |
| **Steps**            | 1. Execute search. 2. Compare ranking positions of result A and result B.                                                                                    |
| **Expected Results** | Method `parseJsonResponse` (entity_name match) is ranked higher than `processData` (comment match). The relevance score for A is demonstrably higher than B. |


### Test Case 12: Search with empty query and edge-case inputs


|                      |                                                                                                                                                                                                                                                                                                                           |
| -------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **ID**               | TC012                                                                                                                                                                                                                                                                                                                     |
| **Description**      | Verify proper handling of empty queries, very long queries, special characters, and SQL/NoSQL injection attempts in search input.                                                                                                                                                                                         |
| **Preconditions**    | Index is populated with test data.                                                                                                                                                                                                                                                                                        |
| **Test Data**        | Query 1: `""` (empty) Query 2: `"a" × 10000` (very long) Query 3: `"class<T extends List<? super Map<K,V>>>"` Query 4: `"'; DROP TABLE chunks; --"`                                                                                                                                                                       |
| **Steps**            | 1. Execute each query via the search API. 2. Observe responses for each.                                                                                                                                                                                                                                                  |
| **Expected Results** | Empty query: returns 400 Bad Request or empty results (not a server error). Very long query: handled gracefully (truncated or rejected with clear message). Special characters: properly escaped, returns relevant results or empty set. Injection attempt: no SQL/NoSQL injection, query treated as literal search text. |


---

### Section 3: RAG — Augmented Generation (FR-RAG)

Grouped inputs for this section: natural language question, model/provider selection (Ollama/OpenAI/Anthropic + specific model), context_limit (top-k).

---

### Test Case 13: RAG answer grounded in code with source attribution


|                      |                                                                                                                                                                                                                                               |
| -------------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **ID**               | TC013                                                                                                                                                                                                                                         |
| **Description**      | Verify that a RAG query generates an answer based on retrieved code chunks, with explicit source file and entity citations.                                                                                                                   |
| **Preconditions**    | Index contains a known authentication module. Ollama is running with Codellama model.                                                                                                                                                         |
| **Test Data**        | Question: `"How is user authentication implemented?"` Provider: `ollama` Model: `codellama`                                                                                                                                                   |
| **Steps**            | 1. Submit `POST /api/v1/rag` with the question. 2. Collect the streamed response. 3. Verify the answer references specific source files and entities.                                                                                         |
| **Expected Results** | Answer describes the authentication flow. Source citations include file paths (e.g., `src/auth/AuthService.java`) and entity names (e.g., `AuthService.authenticate()`). Answer content is grounded in actual indexed code, not hallucinated. |


### Test Case 14: Streaming token response via SSE


|                      |                                                                                                                                                                                              |
| -------------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **ID**               | TC014                                                                                                                                                                                        |
| **Description**      | Verify that the RAG response is streamed token-by-token via SSE and that the first token arrives within the performance requirement.                                                         |
| **Preconditions**    | Index is populated. LLM provider is configured and reachable.                                                                                                                                |
| **Test Data**        | Question: `"What does the SearchService class do?"` Provider: `ollama` Model: `codellama`                                                                                                    |
| **Steps**            | 1. Open SSE connection to `POST /api/v1/rag`. 2. Record the timestamp of the first token received. 3. Collect all tokens until the stream closes. 4. Reassemble the full answer from tokens. |
| **Expected Results** | First token arrives in <2 seconds (NFR requirement). Tokens arrive incrementally (not all at once). Reassembled answer is coherent and complete. SSE stream terminates cleanly.              |


### Test Case 15: LLM provider selection — per-request model override


|                      |                                                                                                                                                                                                                                                                                                                                       |
| -------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **ID**               | TC015                                                                                                                                                                                                                                                                                                                                 |
| **Description**      | Verify that the global default LLM provider can be overridden per-request via API parameter, and that runtime switching works without restart.                                                                                                                                                                                        |
| **Preconditions**    | Default provider is set to `ollama`. OpenAI API key is configured. Both providers are reachable.                                                                                                                                                                                                                                      |
| **Test Data**        | Default: `ollama/codellama` Override: `?model=gpt-4` Question: `"Explain the SearchService class"`                                                                                                                                                                                                                                    |
| **Steps**            | 1. Submit RAG query without model parameter — verify Ollama is used (check logs/response metadata). 2. Submit same query with `?model=gpt-4` — verify OpenAI GPT-4 is used. 3. Submit with `?model=codellama` — verify back to Ollama. 4. Change global config to `openai` at runtime — verify subsequent default queries use OpenAI. |
| **Expected Results** | Per-request override correctly routes to the specified provider. Default fallback works when no override is specified. Runtime switching of global default works without service restart. Response metadata or headers indicate which provider was used.                                                                              |


### Test Case 16: RAG with unavailable LLM provider — fallback to Ollama


|                      |                                                                                                                                                                                                                     |
| -------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **ID**               | TC016                                                                                                                                                                                                               |
| **Description**      | Verify that when a cloud LLM provider (OpenAI/Anthropic) is unavailable, the system automatically falls back to local Ollama.                                                                                       |
| **Preconditions**    | Default provider is `openai`. OpenAI endpoint is unreachable (simulated). Ollama is running locally.                                                                                                                |
| **Test Data**        | Question: `"What does the IngestionService do?"` Provider: `openai` (unreachable)                                                                                                                                   |
| **Steps**            | 1. Configure OpenAI as default provider. 2. Simulate OpenAI being unreachable (e.g., invalid endpoint or network block). 3. Submit a RAG query. 4. Verify fallback behavior.                                        |
| **Expected Results** | System detects OpenAI is unavailable. Automatic fallback to local Ollama occurs. Answer is generated successfully via Ollama. A warning is logged about the fallback. No credentials are exposed in error messages. |


### Test Case 17: RAG query with no relevant code chunks found


|                      |                                                                                                                                                                                                                                                       |
| -------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **ID**               | TC017                                                                                                                                                                                                                                                 |
| **Description**      | Verify that a RAG query about a topic with no matching code in the index returns a clear "no relevant context found" response rather than hallucinating.                                                                                              |
| **Preconditions**    | Index contains only Java backend code. No machine learning code is indexed.                                                                                                                                                                           |
| **Test Data**        | Question: `"How does the neural network training loop work?"`                                                                                                                                                                                         |
| **Steps**            | 1. Submit RAG query. 2. Observe the response.                                                                                                                                                                                                         |
| **Expected Results** | Response clearly indicates that no relevant code was found in the indexed codebase. No hallucinated code or fabricated answers are returned. The response is informative (e.g., "No matching code found in the indexed repositories for this query"). |


---

### Section 4: REST API (FR-IFC-01)

Grouped inputs for this section: HTTP method, endpoint path, request body / query parameters, content type, authentication headers.

---

### Test Case 18: REST API endpoint validation — correct methods and paths


|                      |                                                                                                                                                                                                                                                                                                                                                                |
| -------------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **ID**               | TC018                                                                                                                                                                                                                                                                                                                                                          |
| **Description**      | Verify that all documented REST API endpoints accept the correct HTTP methods and reject unsupported methods with appropriate status codes.                                                                                                                                                                                                                    |
| **Preconditions**    | Application is running.                                                                                                                                                                                                                                                                                                                                        |
| **Test Data**        | Endpoints: `POST /api/v1/ingest/{source}` `GET /api/v1/search?q=...` `POST /api/v1/rag`                                                                                                                                                                                                                                                                        |
| **Steps**            | 1. Send POST to `/api/v1/ingest/github` with valid body — expect 200/202. 2. Send GET to `/api/v1/ingest/github` — expect 405 Method Not Allowed. 3. Send GET to `/api/v1/search?q=test` — expect 200. 4. Send DELETE to `/api/v1/search` — expect 405. 5. Send POST to `/api/v1/rag` with valid body — expect 200. 6. Send GET to `/api/v1/rag` — expect 405. |
| **Expected Results** | Correct methods return success (200/202) with expected response format. Incorrect methods return 405 with `Allow` header indicating supported methods. All responses use reactive Mutiny types (non-blocking).                                                                                                                                                 |


### Test Case 19: REST API non-blocking reactive behavior


|                      |                                                                                                                                                                                                |
| -------------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **ID**               | TC019                                                                                                                                                                                          |
| **Description**      | Verify that REST API endpoints are truly non-blocking by handling multiple concurrent requests without thread starvation.                                                                      |
| **Preconditions**    | Application is running. Index is populated.                                                                                                                                                    |
| **Test Data**        | 50 concurrent search requests with different queries.                                                                                                                                          |
| **Steps**            | 1. Fire 50 concurrent GET requests to `/api/v1/search` with varying queries. 2. Measure response times for all requests. 3. Verify no requests time out or fail due to thread pool exhaustion. |
| **Expected Results** | All 50 requests complete successfully. No request experiences significantly degraded latency due to blocking. Response times remain within the <500ms 95th percentile requirement.             |


---

### Section 5: Command-Line Interface (FR-IFC-02)

Grouped inputs for this section: CLI command (ingest/search/ask), flags (--source, --repo, query string), output format.

---

### Test Case 20: CLI commands — ingest, search, ask


|                      |                                                                                                                                                                                                                                                                                                                   |
| -------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **ID**               | TC020                                                                                                                                                                                                                                                                                                             |
| **Description**      | Verify that all three CLI commands execute correctly with valid arguments and produce expected output.                                                                                                                                                                                                            |
| **Preconditions**    | Application is built and CLI binary is available. Test repository is accessible. Ollama is running.                                                                                                                                                                                                               |
| **Test Data**        | Commands: `megabrain ingest --source github --repo test-org/repo` `megabrain search "how to parse JSON"` `megabrain ask "How is authentication implemented?"`                                                                                                                                                     |
| **Steps**            | 1. Run ingest command — verify it starts ingestion and shows progress. 2. Run search command — verify results are displayed with file paths and relevance scores. 3. Run ask command — verify a streamed RAG answer is displayed with source citations.                                                           |
| **Expected Results** | Ingest: Shows progress stages, completes successfully, returns exit code 0. Search: Displays ranked results with entity names, file paths, and relevance scores. Ask: Streams a coherent answer with source attribution. All commands handle errors gracefully with non-zero exit codes and descriptive messages. |


### Test Case 21: CLI with invalid flags and missing required arguments


|                      |                                                                                                                                                                                                                                                                                                         |
| -------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **ID**               | TC021                                                                                                                                                                                                                                                                                                   |
| **Description**      | Verify that the CLI provides helpful error messages and usage hints when given invalid flags or missing required arguments.                                                                                                                                                                             |
| **Preconditions**    | CLI binary is available.                                                                                                                                                                                                                                                                                |
| **Test Data**        | Commands: `megabrain ingest` (missing --source and --repo) `megabrain ingest --source invalid_source` `megabrain search` (missing query)                                                                                                                                                                |
| **Steps**            | 1. Run each invalid command. 2. Observe error messages and exit codes.                                                                                                                                                                                                                                  |
| **Expected Results** | Missing arguments: descriptive error with usage hint (e.g., "Missing required option: --source"). Invalid source: error listing supported sources (github, bitbucket, gitlab, local-git). Missing query: error indicating query is required. All return non-zero exit codes. No stack traces in output. |


---

### Section 6: Web Dashboard (FR-IFC-03)

Grouped inputs for this section: UI interactions (form fields, buttons, filters, chat input), SSE display, faceted search controls.

---

### Test Case 22: Ingestion Dashboard — real-time progress display


|                      |                                                                                                                                                                                                                                                                            |
| -------------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **ID**               | TC022                                                                                                                                                                                                                                                                      |
| **Description**      | Verify that the Ingestion Dashboard displays real-time progress of indexing jobs with visual feedback for each stage.                                                                                                                                                      |
| **Preconditions**    | Web dashboard is loaded. User is authenticated (if required).                                                                                                                                                                                                              |
| **Test Data**        | Source: GitHub Repository: `test-org/small-repo`                                                                                                                                                                                                                           |
| **Steps**            | 1. Navigate to the Ingestion Dashboard. 2. Enter repository details and click "Start Ingestion". 3. Observe the progress display during each stage. 4. Wait for completion.                                                                                                |
| **Expected Results** | Progress bar or stage indicator updates in real-time. Each stage (Cloning, Parsing, Embedding, Storing, Complete) is visually represented. Percentage updates smoothly. Completion shows a success message or summary. No UI freezing or disconnection during the process. |


### Test Case 23: Search Interface — faceted search and result exploration


|                      |                                                                                                                                                                                                                                                                                          |
| -------------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **ID**               | TC023                                                                                                                                                                                                                                                                                    |
| **Description**      | Verify that the Search Interface allows users to enter queries, apply metadata facet filters, and explore results with code highlighting.                                                                                                                                                |
| **Preconditions**    | Web dashboard is loaded. Index is populated with multi-language code.                                                                                                                                                                                                                    |
| **Test Data**        | Query: `"authentication"` Filters: language=Java, entity_type=class                                                                                                                                                                                                                      |
| **Steps**            | 1. Navigate to the Search Interface. 2. Enter the query and submit. 3. Verify results are displayed with syntax highlighting. 4. Apply language filter (Java). 5. Apply entity type filter (class). 6. Verify results update to reflect applied filters.                                 |
| **Expected Results** | Results display code snippets with proper syntax highlighting (via Prism.js/Highlight.js). Facet filters are available for language, repository, entity type. Applying filters dynamically narrows results. Each result shows entity name, file path, relevance score, and code preview. |


### Test Case 24: RAG Chat Interface — interactive Q&A with streaming


|                      |                                                                                                                                                                                                                |
| -------------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **ID**               | TC024                                                                                                                                                                                                          |
| **Description**      | Verify that the RAG Chat Interface allows users to ask natural language questions and displays streaming responses with source references.                                                                     |
| **Preconditions**    | Web dashboard is loaded. Index is populated. LLM provider is configured.                                                                                                                                       |
| **Test Data**        | Question: `"How does the search ranking algorithm work?"`                                                                                                                                                      |
| **Steps**            | 1. Navigate to the RAG Chat Interface. 2. Type the question and submit. 3. Observe the streaming response appearing token by token. 4. Verify source citations are displayed with links.                       |
| **Expected Results** | Response streams in real-time (tokens appear progressively). Source citations link to specific files and entities. Chat history is maintained for follow-up questions. UI remains responsive during streaming. |


---

### Section 7: Dependency Graph Analysis (FR-DEP)

Grouped inputs for this section: entity name, relationship type (imports/extends/implements/calls/instantiates/references), traversal depth, transitive flag, direction (incoming/outgoing).

---

### Test Case 25: Entity relationship extraction during ingestion


|                      |                                                                                                                                                                                                                                                                                                                                                                   |
| -------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **ID**               | TC025                                                                                                                                                                                                                                                                                                                                                             |
| **Description**      | Verify that the `DependencyExtractor` correctly extracts all 6 relationship types during parsing and stores them as directed graph edges.                                                                                                                                                                                                                         |
| **Preconditions**    | Test repository contains known class hierarchies, method calls, imports, and object instantiations. Graph database is empty.                                                                                                                                                                                                                                      |
| **Test Data**        | Files with known relationships: `ServiceImpl extends BaseService implements IService` (extends + implements) `ServiceImpl` calls `repository.save()` (calls) `ServiceImpl` instantiates `new DataObject()` (instantiates) `ServiceImpl` imports `java.util.List` (imports) `Controller` references `ServiceImpl.TIMEOUT` constant (references)                    |
| **Steps**            | 1. Trigger full ingestion of the test repository. 2. Query the graph database for all edges. 3. Verify each relationship type is correctly extracted.                                                                                                                                                                                                             |
| **Expected Results** | Graph contains directed edges: `ServiceImpl → extends → BaseService` `ServiceImpl → implements → IService` `ServiceImpl → calls → Repository.save` `ServiceImpl → instantiates → DataObject` `ServiceImpl → imports → java.util.List` `Controller → references → ServiceImpl.TIMEOUT` All edges have correct source entity, target entity, and relationship type. |


### Test Case 26: Graph queries — call graph, dependency traversal, inheritance, cycle detection


|                      |                                                                                                                                                                                                                                                                              |
| -------------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **ID**               | TC026                                                                                                                                                                                                                                                                        |
| **Description**      | Verify that all graph query types return correct results for known dependency structures.                                                                                                                                                                                    |
| **Preconditions**    | Graph database is populated with known relationships including an intentional circular dependency.                                                                                                                                                                           |
| **Test Data**        | Known structure: `A extends B`, `B extends C`, `A calls D.method()`, `D calls A.other()` (circular call), `E implements F`                                                                                                                                                   |
| **Steps**            | 1. Query "What calls D.method()?" — verify incoming call graph. 2. Query "What does A depend on?" — verify outgoing dependencies. 3. Query "Show inheritance hierarchy for A" — verify A → B → C chain. 4. Query "Find circular dependencies" — verify A ↔ D cycle detected. |
| **Expected Results** | Incoming call graph for `D.method()` includes A. Outgoing dependencies of A include B (extends), D (calls). Inheritance chain shows A → B → C. Circular dependency between A and D is detected and reported. All queries complete in <200ms (NFR requirement for depth ≤ 3). |


### Test Case 27: Impact analysis — blast radius and dead code detection


|                      |                                                                                                                                                                                                                                                |
| -------------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **ID**               | TC027                                                                                                                                                                                                                                          |
| **Description**      | Verify that impact analysis correctly computes the transitive blast radius for proposed changes and detects dead (unreferenced) code.                                                                                                          |
| **Preconditions**    | Graph database contains a known dependency tree. Some entities are intentionally unreferenced.                                                                                                                                                 |
| **Test Data**        | Modified entity: `BaseService` (used by 3 services transitively) Dead code: `LegacyHelper` class (no incoming edges)                                                                                                                           |
| **Steps**            | 1. Request impact analysis for `BaseService`. 2. Verify all transitively affected entities are listed. 3. Request dead code detection. 4. Verify `LegacyHelper` is identified as unreferenced.                                                 |
| **Expected Results** | Impact report lists all services that directly or transitively depend on `BaseService`. Dead code detection identifies `LegacyHelper` and any other unreferenced entities. Cross-repository usage is considered if multiple repos are indexed. |


### Test Case 28: Graph-enhanced search — transitive queries and LLM-optimized output


|                      |                                                                                                                                                                                                                                                                         |
| -------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **ID**               | TC028                                                                                                                                                                                                                                                                   |
| **Description**      | Verify that the search API supports `transitive=true` for inheritance-aware queries and that results are formatted for LLM consumption.                                                                                                                                 |
| **Preconditions**    | Graph contains: `IService` ← `BaseService` ← `UserService`, `OrderService`. Index is populated.                                                                                                                                                                         |
| **Test Data**        | Query: `"find all implementations of IService"` Parameter: `transitive=true`                                                                                                                                                                                            |
| **Steps**            | 1. Execute search with `transitive=true`. 2. Verify results include both direct (`BaseService`) and transitive (`UserService`, `OrderService`) implementations. 3. Verify output format includes entity relationships in a structured, LLM-consumable format.           |
| **Expected Results** | Results include `BaseService` (direct), `UserService` (transitive), and `OrderService` (transitive). Output includes structured relationship metadata. Depth-limited traversal prevents unbounded results. Response size is bounded for LLM context window constraints. |


---

### Section 8: Documentation Intelligence (FR-DOC)

Grouped inputs for this section: source files with doc comments (Javadoc, JSDoc, docstrings, etc.), entity names, module paths, documentation format.

---

### Test Case 29: Multi-format documentation extraction and AST correlation


|                      |                                                                                                                                                                                                                                                                                                                            |
| -------------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **ID**               | TC029                                                                                                                                                                                                                                                                                                                      |
| **Description**      | Verify that documentation comments in all 6 supported formats are correctly extracted and correlated with their corresponding AST code entities.                                                                                                                                                                           |
| **Preconditions**    | Test repository contains files with known documentation in each format.                                                                                                                                                                                                                                                    |
| **Test Data**        | Files: `UserService.java` with Javadoc (`/** @param ... */`) `app.js` with JSDoc (`/** @param ... */`) `utils.py` with docstrings (`"""..."""`) `lib.rs` with `///` doc comments `server.go` with `//` doc comments `matrix.cpp` with Doxygen (`/** ... */`)                                                               |
| **Steps**            | 1. Trigger ingestion of all test files. 2. For each file, query the indexed documentation for known entities. 3. Verify extracted fields: summary, params, returns, examples, see_also.                                                                                                                                    |
| **Expected Results** | Each format is correctly parsed. Documentation is linked to the correct code entity (class/method/function) via AST correlation. Extracted fields (`doc_summary`, `doc_params`, `doc_returns`, `doc_examples`, `doc_see_also`) are populated correctly. Documentation extraction adds <10% overhead to parsing time (NFR). |


### Test Case 30: Documentation search — boosted relevance and "docs only" filter


|                      |                                                                                                                                                                                                                    |
| -------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| **ID**               | TC030                                                                                                                                                                                                              |
| **Description**      | Verify that documentation content receives higher boost weights in search ranking and that a "docs only" filter restricts results to documented APIs.                                                              |
| **Preconditions**    | Index contains both documented and undocumented entities.                                                                                                                                                          |
| **Test Data**        | Query: `"authentication"` Entity A: `AuthService` with Javadoc describing authentication Entity B: `AuthHelper` with "authentication" in code body but no docs                                                     |
| **Steps**            | 1. Search for "authentication" without filter. 2. Verify `AuthService` (doc match) ranks higher than `AuthHelper` (code match). 3. Apply "docs only" filter. 4. Verify only documented entities appear in results. |
| **Expected Results** | Documentation matches are boosted above code body matches. "Docs only" filter excludes undocumented entities entirely. Markdown rendering support works for rich documentation display.                            |


### Test Case 31: Documentation quality metrics — coverage, completeness, staleness


|                      |                                                                                                                                                                                                                                                                                               |
| -------------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **ID**               | TC031                                                                                                                                                                                                                                                                                         |
| **Description**      | Verify that documentation quality metrics are correctly computed per repository and per module, including coverage score, completeness, and staleness detection.                                                                                                                              |
| **Preconditions**    | Test repository has: 10 public APIs, 7 documented (2 with incomplete docs), 1 referencing a renamed method (stale).                                                                                                                                                                           |
| **Test Data**        | Repository: `test-org/metrics-repo` Expected coverage: 7/10 = 0.70 Expected stale docs: 1                                                                                                                                                                                                     |
| **Steps**            | 1. Trigger ingestion and documentation quality analysis. 2. Request documentation coverage report for the repository. 3. Verify coverage score, completeness details, and staleness indicators.                                                                                               |
| **Expected Results** | Coverage score: 0.70 (7 of 10 public APIs documented). Completeness score reflects 2 APIs with missing param/return docs. Staleness indicator flags the doc referencing a renamed method. Report generated within 30s (NFR for <1M LOC). Undocumented public APIs are listed for improvement. |


### Test Case 32: Code example extraction — @example tags, fenced blocks, doctests


|                      |                                                                                                                                                                                                                                                                    |
| -------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| **ID**               | TC032                                                                                                                                                                                                                                                              |
| **Description**      | Verify that code examples from documentation comments are extracted, indexed, and linked to their source entities.                                                                                                                                                 |
| **Preconditions**    | Test files contain `@example` tags (JSDoc), fenced code blocks (Rust markdown docs), and Python doctest blocks.                                                                                                                                                    |
| **Test Data**        | `parser.js` with `@example` tag showing usage `lib.rs` with ````rust` fenced example `utils.py` with `>>>` doctest block                                                                                                                                           |
| **Steps**            | 1. Ingest test files. 2. Query "show me examples of using Parser" — verify JS example returned. 3. Query "show me examples of using lib_function" — verify Rust example returned. 4. Query "show me examples of using calculate" — verify Python doctest returned. |
| **Expected Results** | Examples are extracted from all three formats. Each example is linked to its corresponding function/class. Natural language query "show me examples of using X" returns relevant examples. Syntax highlighting is preserved for UI display.                        |


---

### Section 9: MCP Tool Server (FR-MCP)

Grouped inputs for this section: transport type (stdio/SSE), tool name, tool parameters (query, entity_name, depth, etc.), resource URIs.

---

### Test Case 33: MCP server — stdio and SSE transport initialization


|                      |                                                                                                                                                                                                                                                                               |
| -------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **ID**               | TC033                                                                                                                                                                                                                                                                         |
| **Description**      | Verify that the MCP server initializes correctly on both stdio (primary) and SSE (secondary) transports, with proper protocol negotiation and tool discovery.                                                                                                                 |
| **Preconditions**    | MCP server binary/process is available. `mcp.json` configuration exists.                                                                                                                                                                                                      |
| **Test Data**        | Transport 1: stdio (simulated via process pipe) Transport 2: SSE (via HTTP endpoint)                                                                                                                                                                                          |
| **Steps**            | 1. Start MCP server with stdio transport — send initialization request. 2. Verify protocol negotiation completes and tool list is returned. 3. Start MCP server with SSE transport — connect via HTTP. 4. Verify tool discovery returns all registered tools in <100ms (NFR). |
| **Expected Results** | stdio: Initialization handshake succeeds, tool schemas are returned. SSE: Connection established, tool list served over SSE stream. Both transports expose identical tool sets. Tool schema response time is <100ms. `mcp.json` is valid for client auto-discovery.           |


### Test Case 34: MCP code search tools — search_code, search_by_entity, get_file_content


|                      |                                                                                                                                                                                                                                                                                                                                       |
| -------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **ID**               | TC034                                                                                                                                                                                                                                                                                                                                 |
| **Description**      | Verify that MCP code search tools return correct results and respect optional parameters.                                                                                                                                                                                                                                             |
| **Preconditions**    | MCP server is running. Index is populated.                                                                                                                                                                                                                                                                                            |
| **Test Data**        | `search_code`: query=`"authentication"`, language=`"java"`, limit=5 `search_by_entity`: entity_name=`"UserService"`, exact_match=true `get_file_content`: file_path=`"src/Main.java"`, start_line=10, end_line=20 `list_repositories`: no params `list_entities`: path=`"src/"`, entity_type=`"class"`                                |
| **Steps**            | 1. Call `search_code` — verify filtered results. 2. Call `search_by_entity` — verify exact match returned. 3. Call `get_file_content` — verify specific line range returned. 4. Call `list_repositories` — verify all indexed repos listed. 5. Call `list_entities` — verify class entities in `src/` listed.                         |
| **Expected Results** | `search_code`: Returns ≤5 Java results for "authentication". `search_by_entity`: Returns exactly the `UserService` entity. `get_file_content`: Returns lines 10-20 of `Main.java`. `list_repositories`: Returns all indexed repository names. `list_entities`: Returns class entities in `src/`. All responses complete in <1s (NFR). |


### Test Case 35: MCP dependency analysis tools — find_implementations, find_usages, find_callers


|                      |                                                                                                                                                                                                                                                                                                                                                                                                        |
| -------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| **ID**               | TC035                                                                                                                                                                                                                                                                                                                                                                                                  |
| **Description**      | Verify that MCP dependency analysis tools correctly leverage the graph database for relationship queries.                                                                                                                                                                                                                                                                                              |
| **Preconditions**    | MCP server is running. Graph database is populated with known relationships.                                                                                                                                                                                                                                                                                                                           |
| **Test Data**        | `find_implementations`: interface_name=`"IService"` `find_usages`: entity_name=`"UserService"`, transitive=true `find_callers`: function_name=`"authenticate"`, depth=2 `find_dependencies`: entity_name=`"Controller"`, depth=3 `get_inheritance_tree`: class_name=`"BaseService"`, direction=`"down"`                                                                                                |
| **Steps**            | 1. Call each tool with the specified parameters. 2. Verify results against known graph structure.                                                                                                                                                                                                                                                                                                      |
| **Expected Results** | `find_implementations`: Returns all classes implementing `IService` (including transitive). `find_usages`: Returns all usages of `UserService` across repositories. `find_callers`: Returns callers up to depth 2. `find_dependencies`: Returns dependency tree up to depth 3. `get_inheritance_tree`: Returns subclass hierarchy below `BaseService`. Complex graph traversals complete in <3s (NFR). |


### Test Case 36: MCP documentation tools and RAG query tool


|                      |                                                                                                                                                                                                                                                                                                                                                                                |
| -------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| **ID**               | TC036                                                                                                                                                                                                                                                                                                                                                                          |
| **Description**      | Verify that MCP documentation tools and the RAG query tool return accurate information.                                                                                                                                                                                                                                                                                        |
| **Preconditions**    | MCP server is running. Index has documented entities. LLM is available.                                                                                                                                                                                                                                                                                                        |
| **Test Data**        | `get_documentation`: entity_name=`"UserService"` `find_examples`: entity_name=`"parseJson"` `get_doc_coverage`: path=`"src/services/"` `ask_codebase`: question=`"How does caching work?"`, context_limit=5                                                                                                                                                                    |
| **Steps**            | 1. Call `get_documentation` — verify Javadoc content returned. 2. Call `find_examples` — verify code examples returned. 3. Call `get_doc_coverage` — verify coverage metrics returned. 4. Call `ask_codebase` — verify RAG answer with citations returned.                                                                                                                     |
| **Expected Results** | `get_documentation`: Returns full documentation for `UserService` (summary, params, returns). `find_examples`: Returns code examples demonstrating `parseJson`. `get_doc_coverage`: Returns numeric coverage score for `src/services/`. `ask_codebase`: Returns synthesized answer about caching with source citations and respects `context_limit=5` (uses at most 5 chunks). |


### Test Case 37: MCP resources — URIs and subscriptions


|                      |                                                                                                                                                                                                                                                                                                                                                                             |
| -------------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **ID**               | TC037                                                                                                                                                                                                                                                                                                                                                                       |
| **Description**      | Verify that MCP resource URIs return correct data and that subscriptions emit notifications when the index is updated.                                                                                                                                                                                                                                                      |
| **Preconditions**    | MCP server is running. Index contains at least one repository with known entities.                                                                                                                                                                                                                                                                                          |
| **Test Data**        | Resources: `megabrain://repo/test-org/repo` — repository metadata `megabrain://file/src/Main.java` — file with parsed structure `megabrain://entity/UserService` — entity details                                                                                                                                                                                           |
| **Steps**            | 1. Fetch `megabrain://repo/test-org/repo` — verify metadata. 2. Fetch `megabrain://file/src/Main.java` — verify parsed structure. 3. Fetch `megabrain://entity/UserService` — verify relationships included. 4. Subscribe to `megabrain://repo/test-org/repo`. 5. Trigger re-indexing of the repository. 6. Verify subscription notification is received.                   |
| **Expected Results** | Repository resource: Returns repo name, stats (file count, entity count, last indexed). File resource: Returns file content with AST structure overlay. Entity resource: Returns entity details with relationships (extends, implements, calls). Subscription: Notification received after re-indexing with updated metadata. Supports 100+ concurrent subscriptions (NFR). |


### Test Case 38: MCP concurrent sessions


|                      |                                                                                                                                                                                                                                                                    |
| -------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| **ID**               | TC038                                                                                                                                                                                                                                                              |
| **Description**      | Verify that the MCP server supports 10+ simultaneous client connections with independent sessions.                                                                                                                                                                 |
| **Preconditions**    | MCP server is running with SSE transport.                                                                                                                                                                                                                          |
| **Test Data**        | 12 concurrent MCP clients, each with different tool calls.                                                                                                                                                                                                         |
| **Steps**            | 1. Connect 12 MCP clients simultaneously. 2. Each client calls a different tool (mix of search, dependency, docs, RAG). 3. Verify all clients receive correct, independent responses. 4. Disconnect clients one by one — verify remaining sessions are unaffected. |
| **Expected Results** | All 12 clients connect successfully. Each receives correct results for their specific tool call. Sessions are independent (one client's query doesn't affect another's). Disconnecting a client doesn't impact others. No session state leaks between clients.     |


---

### Section 10: Non-Functional Requirements (NFR)

Grouped inputs for this section: concurrent load, data volume, configuration settings, credential handling.

---

### Test Case 39: Performance — indexing throughput and query latency


|                      |                                                                                                                                                                                                                                                                                                                                                  |
| -------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| **ID**               | TC039                                                                                                                                                                                                                                                                                                                                            |
| **Description**      | Verify that the system meets all stated performance thresholds: indexing speed, search latency, RAG first-token time, graph query speed, grammar loading, and documentation overhead.                                                                                                                                                            |
| **Preconditions**    | System is deployed in a representative environment. Test dataset is prepared with known LOC count.                                                                                                                                                                                                                                               |
| **Test Data**        | Repository: 100K LOC across multiple languages Concurrent search users: 20 Graph depth: 3 levels                                                                                                                                                                                                                                                 |
| **Steps**            | 1. Measure indexing throughput (LOC/minute). 2. Execute 100 search queries and measure 95th percentile latency. 3. Execute a RAG query and measure time to first token. 4. Execute graph traversal (depth=3) and measure latency. 5. Measure cold-start grammar loading time. 6. Compare parsing time with and without documentation extraction. |
| **Expected Results** | Indexing: >10,000 LOC/minute. Search: <500ms at 95th percentile. RAG: First token in <2s. Graph queries: <200ms for depth ≤ 3. Grammar loading: <500ms cold start. Documentation extraction: <10% overhead on parsing time.                                                                                                                      |


### Test Case 40: Scalability — large repository and graph database limits


|                      |                                                                                                                                                                                                                                                                                                                    |
| -------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| **ID**               | TC040                                                                                                                                                                                                                                                                                                              |
| **Description**      | Verify that the system can handle large-scale data: 10M+ LOC repositories and up to 50M entity relationships in the graph database.                                                                                                                                                                                |
| **Preconditions**    | Large test dataset is available (or generated synthetically). System has adequate resources.                                                                                                                                                                                                                       |
| **Test Data**        | Repository with 10M+ LOC Graph with 50M entity relationships                                                                                                                                                                                                                                                       |
| **Steps**            | 1. Ingest the large repository — verify completion without OOM or timeout. 2. Populate graph database to 50M relationships. 3. Execute search queries against the large index — verify latency stays within thresholds. 4. Execute graph queries against the large graph — verify latency stays within thresholds. |
| **Expected Results** | Ingestion completes successfully for 10M+ LOC (may take hours but should not fail). Index queries remain performant. Graph queries at depth ≤ 3 remain under 200ms even with 50M relationships. Memory usage remains bounded (no OOM).                                                                             |


### Test Case 41: Privacy & security — no code data leaks, credential protection


|                      |                                                                                                                                                                                                                                                                                                                                                                                                                       |
| -------------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **ID**               | TC041                                                                                                                                                                                                                                                                                                                                                                                                                 |
| **Description**      | Verify that all code data remains in-house, no code is sent to external APIs unless explicitly configured, credentials are never logged, and API inputs are sanitized.                                                                                                                                                                                                                                                |
| **Preconditions**    | System configured with Ollama (default) and OpenAI (optional). Network traffic is monitored.                                                                                                                                                                                                                                                                                                                          |
| **Test Data**        | LLM provider: `ollama` (default) GitHub PAT: test token Malicious path: `../../etc/passwd` Injection attempt: `'; DROP TABLE;--`                                                                                                                                                                                                                                                                                      |
| **Steps**            | 1. With Ollama as default, execute RAG query — verify no outbound network calls to external LLM APIs. 2. Trigger an authentication error — verify token value is NOT in logs or error response. 3. Submit path traversal input (`../../etc/passwd`) — verify rejection. 4. Submit SQL injection input — verify it's treated as literal text. 5. Verify API keys are loaded from environment variables, not hardcoded. |
| **Expected Results** | No external API calls when using Ollama. Token values never appear in logs, error messages, or API responses. Path traversal is blocked with "Invalid path" error. Injection attempts are sanitized. Credentials are loaded from environment variables or vault only.                                                                                                                                                 |


### Test Case 42: Health checks and operational readiness


|                      |                                                                                                                                                                                                                                                                                                                                            |
| -------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| **ID**               | TC042                                                                                                                                                                                                                                                                                                                                      |
| **Description**      | Verify that the `/q/health` endpoint reports correct status for all major components including grammar bundle health.                                                                                                                                                                                                                      |
| **Preconditions**    | Application is running. All services are healthy.                                                                                                                                                                                                                                                                                          |
| **Test Data**        | Health endpoint: `GET /q/health`                                                                                                                                                                                                                                                                                                           |
| **Steps**            | 1. Request health check when all services are up — verify "UP" status. 2. Simulate a database connection failure — verify health check reflects "DOWN". 3. Simulate a missing grammar bundle — verify grammar health check reports the issue. 4. Restore services — verify health returns to "UP".                                         |
| **Expected Results** | Healthy state: Overall status is "UP" with individual component statuses. Database down: Status is "DOWN" with details about which component failed. Missing grammar: Grammar health check reports the specific missing grammar. Recovery: Status returns to "UP" after services are restored. Health check response includes timing data. |


### Test Case 43: LLM provider flexibility — simultaneous providers and cost tracking


|                      |                                                                                                                                                                                                                                                                                                     |
| -------------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **ID**               | TC043                                                                                                                                                                                                                                                                                               |
| **Description**      | Verify that multiple LLM providers can be configured simultaneously, per-request selection works, and optional cost tracking captures usage.                                                                                                                                                        |
| **Preconditions**    | Ollama, OpenAI, and Anthropic are all configured. Cost tracking is enabled.                                                                                                                                                                                                                         |
| **Test Data**        | Requests: Request 1: default (Ollama) Request 2: `?model=gpt-4` (OpenAI) Request 3: `?model=claude-sonnet-3-5` (Anthropic) Request 4: default again (Ollama)                                                                                                                                        |
| **Steps**            | 1. Execute 4 RAG queries with the specified model selections. 2. Verify each request is routed to the correct provider. 3. Check cost tracking for requests 2 and 3 (paid providers). 4. Verify Ollama requests (1, 4) have zero cost recorded.                                                     |
| **Expected Results** | All 4 requests succeed with correct provider routing. Cost tracking records token usage and estimated cost for OpenAI and Anthropic calls. Ollama calls show zero cost. Budget alerts would trigger if configured threshold is exceeded. Runtime switching between providers works without restart. |


### Test Case 44: GraalVM Native Image deployment


|                      |                                                                                                                                                                                                                                                                            |
| -------------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **ID**               | TC044                                                                                                                                                                                                                                                                      |
| **Description**      | Verify that the application can be built and deployed as a GraalVM Native executable with all functionality intact.                                                                                                                                                        |
| **Preconditions**    | GraalVM is installed. Tree-sitter native libraries (.so/.dylib) are on the library path.                                                                                                                                                                                   |
| **Test Data**        | Native executable built via `mvn package -Pnative`                                                                                                                                                                                                                         |
| **Steps**            | 1. Build the native image. 2. Start the native executable — measure startup time. 3. Execute ingestion, search, and RAG queries. 4. Verify health checks respond correctly. 5. Verify MCP server initializes on stdio transport.                                           |
| **Expected Results** | Native image builds successfully. Startup time is significantly faster than JVM mode. All core functionality (ingest, search, RAG, dependency graph, docs, MCP) works identically to JVM mode. Native libraries (Tree-sitter grammars) load correctly. Health checks pass. |


