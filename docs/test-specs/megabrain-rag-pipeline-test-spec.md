# Test Specification: MegaBrain RAG Pipeline

## Reviewers
- Reviewer 1: Product Owner
- Reviewer 2: Lead Developer

## Introduction
This specification covers end-to-end testing of the MegaBrain RAG Pipeline as described in the feature specification. The system provides intelligent code ingestion, hybrid search, RAG-based Q&A, dependency graph analysis, documentation intelligence, and MCP server capabilities. Testing scope includes REST API, CLI, Web Dashboard modules, ingestion, search, RAG, dependency graph, documentation features, MCP tools/resources, and non-functional requirements (performance, security, concurrency). Objectives: verify functional correctness, API contracts, performance thresholds, security (no credential leakage, path traversal prevention), and multi-provider LLM behavior. Assumptions: PostgreSQL with pgvector, optional Neo4j/JanusGraph and Milvus available for integration tests; test repositories and fixtures are available; Ollama/OpenAI/Anthropic are configurable for RAG tests.

## References
- Epic: [Link to Epic]
- Business Case: [Link to Business Case]

## Test case count

| Execution type | Count | Description |
| -------------- | ----- | ----------- |
| Automatic      | 36    | Runnable by automation (API, CLI, headless UI tests). |
| Manual         | 16    | Requires human execution (visual checks, exploratory, UX). |
| **Total**      | 52    | |

## Test Cases

### Test Case 1: Ingest from GitHub with valid repo and branch
| Field | Value |
| ----- | ----- |
| **ID**         | TC001                                                        |
| **Description**| Verify ingestion can be initiated via REST API for a valid GitHub repository and branch. |
| **Preconditions**| GitHub client configured; valid repo and branch; index not locked. |
| **Test Data**  | source=github, repo=owner/repo, branch=main. |
| **Steps**      | 1. POST /api/v1/ingest/github with repo and branch.<br>2. Consume SSE stream until "Complete" or error.<br>3. Verify 200 and stream events contain Cloning, Parsing, Storing, Complete. |
| **Expected Results** | Ingestion starts; SSE events received; job completes without error. |
| **Category**   | Positive |
| **Execution**  | Automatic |

### Test Case 2: Ingest with invalid repository identifier
| Field | Value |
| ----- | ----- |
| **ID**         | TC002                                                        |
| **Description**| Verify API rejects invalid repository identifier (e.g. malformed, path traversal). |
| **Preconditions**| API available; credentials configured. |
| **Test Data**  | repo="../../../etc/passwd" or repo="". |
| **Steps**      | 1. POST /api/v1/ingest/github with invalid repo.<br>2. Observe response status and body. |
| **Expected Results** | 400 Bad Request or 422; no file system access; no sensitive data in response. |
| **Category**   | Negative, Security |
| **Execution**  | Automatic |

### Test Case 3: Search with valid query and filters
| Field | Value |
| ----- | ----- |
| **ID**         | TC003                                                        |
| **Description**| Verify hybrid search returns results for valid query with language and repository filters. |
| **Preconditions**| Index populated with known code; search API available. |
| **Test Data**  | q="parse JSON", language=java, repository=my-repo. |
| **Steps**      | 1. GET /api/v1/search?q=parse+JSON&language=java&repository=my-repo.<br>2. Parse JSON response. |
| **Expected Results** | 200; results array; each result has source_file, entity_type, language; results filtered by filters. |
| **Category**   | Positive |
| **Execution**  | Automatic |

### Test Case 4: Search with empty query
| Field | Value |
| ----- | ----- |
| **ID**         | TC004                                                        |
| **Description**| Verify search API handles empty or missing query parameter. |
| **Preconditions**| Search API available. |
| **Test Data**  | q="" or omit q. |
| **Steps**      | 1. GET /api/v1/search?q= or GET /api/v1/search. |
| **Expected Results** | 400 or 422 with clear error; no server crash. |
| **Category**   | Negative |
| **Execution**  | Automatic |

### Test Case 5: RAG ask with default LLM provider
| Field | Value |
| ----- | ----- |
| **ID**         | TC005                                                        |
| **Description**| Verify RAG endpoint returns streamed answer using default LLM provider. |
| **Preconditions**| Index populated; default LLM (e.g. Ollama) configured and reachable. |
| **Test Data**  | question="How is authentication implemented?" |
| **Steps**      | 1. POST /api/v1/rag with JSON body { "question": "How is authentication implemented?" }.<br>2. Consume SSE stream of tokens.<br>3. Collect full response and check for source citations. |
| **Expected Results** | 200; SSE stream; first token within <2s; answer references source files/entities. |
| **Category**   | Positive |
| **Execution**  | Automatic |

### Test Case 6: RAG ask with per-request model override
| Field | Value |
| ----- | ----- |
| **ID**         | TC006                                                        |
| **Description**| Verify per-request model selection overrides default provider. |
| **Preconditions**| Multiple providers configured (e.g. Ollama and OpenAI). |
| **Test Data**  | question="What does X do?", model=codellama. |
| **Steps**      | 1. POST /api/v1/rag?model=codellama with question.<br>2. Verify response is generated (and optionally that correct model was used via logs/metrics). |
| **Expected Results** | 200; response streamed; no error from wrong provider. |
| **Category**   | Positive |
| **Execution**  | Automatic |

### Test Case 7: RAG with invalid or missing question
| Field | Value |
| ----- | ----- |
| **ID**         | TC007                                                        |
| **Description**| Verify RAG rejects missing or invalid question body. |
| **Preconditions**| RAG API available. |
| **Test Data**  | Body: {} or { "question": "" }. |
| **Steps**      | 1. POST /api/v1/rag with empty or missing question.<br>2. Check status and response. |
| **Expected Results** | 400 or 422; clear validation error; no LLM call. |
| **Category**   | Negative |
| **Execution**  | Automatic |

### Test Case 8: CLI ingest command success
| Field | Value |
| ----- | ----- |
| **ID**         | TC008                                                        |
| **Description**| Verify CLI ingest command runs and exits successfully for valid args. |
| **Preconditions**| CLI built; source and repo configured. |
| **Test Data**  | megabrain ingest --source github --repo owner/repo. |
| **Steps**      | 1. Run megabrain ingest --source github --repo owner/repo.<br>2. Capture exit code and stdout/stderr. |
| **Expected Results** | Exit code 0; progress or completion message. |
| **Category**   | Positive |
| **Execution**  | Automatic |

### Test Case 9: CLI search command returns results
| Field | Value |
| ----- | ----- |
| **ID**         | TC009                                                        |
| **Description**| Verify CLI search command returns search results for a query. |
| **Preconditions**| Index populated; CLI and backend available. |
| **Test Data**  | megabrain search "how to parse JSON". |
| **Steps**      | 1. Run megabrain search "how to parse JSON".<br>2. Parse output for result lines or JSON. |
| **Expected Results** | Exit code 0; output contains search hits or structured result. |
| **Category**   | Positive |
| **Execution**  | Automatic |

### Test Case 10: CLI ask command streams response
| Field | Value |
| ----- | ----- |
| **ID**         | TC010                                                        |
| **Description**| Verify CLI ask command streams RAG response. |
| **Preconditions**| Index and LLM configured. |
| **Test Data**  | megabrain ask "How is authentication implemented?" |
| **Steps**      | 1. Run megabrain ask "How is authentication implemented?".<br>2. Verify incremental output and exit 0. |
| **Expected Results** | Exit code 0; text streamed to stdout; answer and citations present. |
| **Category**   | Positive |
| **Execution**  | Automatic |

### Test Case 11: Search 95th percentile latency under 500ms
| Field | Value |
| ----- | ----- |
| **ID**         | TC011                                                        |
| **Description**| Verify search latency meets NFR (95th percentile <500ms). |
| **Preconditions**| Index with representative size; search endpoint available. |
| **Test Data**  | 20+ varied queries. |
| **Steps**      | 1. Execute 20+ search requests with varied queries.<br>2. Measure response time for each.<br>3. Compute 95th percentile. |
| **Expected Results** | 95th percentile latency <500ms. |
| **Category**   | Performance |
| **Execution**  | Automatic |

### Test Case 12: RAG first token within 2 seconds
| Field | Value |
| ----- | ----- |
| **ID**         | TC012                                                        |
| **Description**| Verify RAG first token is streamed within 2s (NFR). |
| **Preconditions**| RAG and LLM configured; index populated. |
| **Test Data**  | question="What is the main entry point?" |
| **Steps**      | 1. POST /api/v1/rag with question; record time to first token.<br>2. Repeat 5 times. |
| **Expected Results** | First token received in <2s for all runs (or with acceptable variance). |
| **Category**   | Performance |
| **Execution**  | Automatic |

### Test Case 13: Health check returns 200 and component status
| Field | Value |
| ----- | ----- |
| **ID**         | TC013                                                        |
| **Description**| Verify /q/health returns 200 and includes grammar/component status. |
| **Preconditions**| Application running. |
| **Test Data**  | GET /q/health. |
| **Steps**      | 1. GET /q/health.<br>2. Parse JSON; verify status and optional grammar bundle check. |
| **Expected Results** | 200; JSON with status up/down; no credentials in response. |
| **Category**   | Positive |
| **Execution**  | Automatic |

### Test Case 14: API does not log or return credentials
| Field | Value |
| ----- | ----- |
| **ID**         | TC014                                                        |
| **Description**| Verify error responses and logs never contain API keys or tokens. |
| **Preconditions**| API configured with invalid/missing API key for a provider. |
| **Test Data**  | Trigger RAG or ingest that uses provider with invalid key. |
| **Steps**      | 1. Trigger request that fails due to auth.<br>2. Inspect response body and server logs. |
| **Expected Results** | No API key, token, or password in response or logs. |
| **Category**   | Security |
| **Execution**  | Automatic |

### Test Case 15: Path traversal in file_path or repo parameter rejected
| Field | Value |
| ----- | ----- |
| **ID**         | TC015                                                        |
| **Description**| Verify parameters like file_path or repo reject path traversal sequences. |
| **Preconditions**| Search or MCP API available. |
| **Test Data**  | file_path="../../../etc/passwd" or similar. |
| **Steps**      | 1. Call search or get_file_content with path containing .. or ~.<br>2. Verify 400/422 and no file system access outside index. |
| **Expected Results** | 400 or 422; no reading of /etc/passwd or other sensitive paths. |
| **Category**   | Security |
| **Execution**  | Automatic |

### Test Case 16: Grammar loading cold start under 500ms
| Field | Value |
| ----- | ----- |
| **ID**         | TC016                                                        |
| **Description**| Verify grammar loading for a language meets <500ms cold start (NFR). |
| **Preconditions**| Grammar manager and parser registry available; grammar not yet loaded. |
| **Test Data**  | Language: python or go. |
| **Steps**      | 1. Trigger parsing of a file of target language (cold).<br>2. Measure time until first chunk produced or grammar loaded.<br>3. Assert <500ms. |
| **Expected Results** | Grammar load or first parse completion <500ms. |
| **Category**   | Performance |
| **Execution**  | Automatic |

### Test Case 17: Concurrent search requests handled correctly
| Field | Value |
| ----- | ----- |
| **ID**         | TC017                                                        |
| **Description**| Verify multiple simultaneous search requests complete without errors or cross-talk. |
| **Preconditions**| Search API and index available. |
| **Test Data**  | 10 concurrent GET /api/v1/search with different q values. |
| **Steps**      | 1. Send 10 search requests in parallel.<br>2. Collect all responses and status codes. |
| **Expected Results** | All 200; each response matches its query; no mixed or corrupted results. |
| **Category**   | Concurrency |
| **Execution**  | Automatic |

### Test Case 18: Incremental indexing only processes changed files
| Field | Value |
| ----- | ----- |
| **ID**         | TC018                                                        |
| **Description**| Verify incremental indexing uses git diff and only indexes changed files. |
| **Preconditions**| Repo already ingested once; test can modify a known file and re-ingest. |
| **Test Data**  | Same repo, branch; one file modified. |
| **Steps**      | 1. Run full ingest; record count of indexed chunks.<br>2. Change one file in repo (or mock diff).<br>3. Trigger incremental ingest.<br>4. Verify only changed file (or minimal set) processed. |
| **Expected Results** | Incremental run processes only changed files; index updated correctly. |
| **Category**   | Positive |
| **Execution**  | Automatic |

### Test Case 19: Search with entity_type and file_path filters
| Field | Value |
| ----- | ----- |
| **ID**         | TC019                                                        |
| **Description**| Verify context-aware filtering by entity_type and file_path. |
| **Preconditions**| Index with mixed entity types and paths. |
| **Test Data**  | q="auth", entity_type=class, file_path=src/main. |
| **Steps**      | 1. GET /api/v1/search with entity_type and file_path.<br>2. Verify all results match filters. |
| **Expected Results** | 200; results restricted to specified entity_type and path prefix. |
| **Category**   | Positive |
| **Execution**  | Automatic |

### Test Case 20: Search with limit boundary (0, 1, max)
| Field | Value |
| ----- | ----- |
| **ID**         | TC020                                                        |
| **Description**| Verify search limit parameter at boundary values. |
| **Preconditions**| Search API; index with many results. |
| **Test Data**  | limit=0, limit=1, limit=1000 (or configured max). |
| **Steps**      | 1. Search with limit=0; expect empty or defined behavior.<br>2. Search with limit=1; exactly 1 result.<br>3. Search with limit=max; no more than max results. |
| **Expected Results** | No crash; result count respects limit; overflow handled. |
| **Category**   | Boundary |
| **Execution**  | Automatic |

### Test Case 21: MCP tool search_code returns results
| Field | Value |
| ----- | ----- |
| **ID**         | TC021                                                        |
| **Description**| Verify MCP tool search_code returns code search results. |
| **Preconditions**| MCP server running; index populated. |
| **Test Data**  | query="parse JSON", limit=5. |
| **Steps**      | 1. Invoke MCP tool search_code with query and limit.<br>2. Parse tool result. |
| **Expected Results** | Result contains search hits; structure matches tool schema. |
| **Category**   | Positive |
| **Execution**  | Automatic |

### Test Case 22: MCP tool get_file_content with line range
| Field | Value |
| ----- | ----- |
| **ID**         | TC022                                                        |
| **Description**| Verify MCP get_file_content returns content for path and optional line range. |
| **Preconditions**| File indexed; MCP server available. |
| **Test Data**  | file_path=src/main/App.java, start_line=1, end_line=20. |
| **Steps**      | 1. Invoke get_file_content with path and line range.<br>2. Verify content and line bounds. |
| **Expected Results** | Content returned for specified range; no path traversal. |
| **Category**   | Positive |
| **Execution**  | Automatic |

### Test Case 23: MCP tool ask_codebase returns answer with citations
| Field | Value |
| ----- | ----- |
| **ID**         | TC023                                                        |
| **Description**| Verify MCP ask_codebase returns synthesized answer with source citations. |
| **Preconditions**| MCP and RAG configured; index populated. |
| **Test Data**  | question="Where is the main method?" |
| **Steps**      | 1. Invoke ask_codebase with question.<br>2. Check response for answer text and file/entity references. |
| **Expected Results** | Answer text and at least one source citation. |
| **Category**   | Positive |
| **Execution**  | Automatic |

### Test Case 24: MCP find_implementations returns transitive implementations
| Field | Value |
| ----- | ----- |
| **ID**         | TC024                                                        |
| **Description**| Verify MCP find_implementations returns all implementations of an interface (transitive). |
| **Preconditions**| Graph populated with interface/implementations. |
| **Test Data**  | interface_name=MyService. |
| **Steps**      | 1. Invoke find_implementations for known interface.<br>2. Verify list includes expected classes. |
| **Expected Results** | List of implementing classes; transitive where applicable. |
| **Category**   | Positive |
| **Execution**  | Automatic |

### Test Case 25: Graph query latency under 200ms for depth 3
| Field | Value |
| ----- | ----- |
| **ID**         | TC025                                                        |
| **Description**| Verify graph dependency traversal (e.g. find_dependencies depth 3) meets <200ms (NFR). |
| **Preconditions**| Graph DB populated; MCP or graph API available. |
| **Test Data**  | entity_name=KnownClass, depth=3. |
| **Steps**      | 1. Invoke find_dependencies or equivalent with depth=3.<br>2. Measure response time.<br>3. Repeat 5 times. |
| **Expected Results** | Response time <200ms (p95). |
| **Category**   | Performance |
| **Execution**  | Automatic |

### Test Case 26: Documentation coverage report generated within 30s
| Field | Value |
| ----- | ----- |
| **ID**         | TC026                                                        |
| **Description**| Verify documentation coverage report for repo <1M LOC generated within 30s (NFR). |
| **Preconditions**| Repo indexed with doc extraction; report endpoint or tool available. |
| **Test Data**  | path=repo/module <1M LOC. |
| **Steps**      | 1. Request doc coverage report for path.<br>2. Measure time to completion. |
| **Expected Results** | Report returned in <30s; contains coverage/completeness metrics. |
| **Category**   | Performance |
| **Execution**  | Automatic |

### Test Case 27: Rate limit returns 429
| Field | Value |
| ----- | ----- |
| **ID**         | TC027                                                        |
| **Description**| Verify API returns 429 when rate limit exceeded (if rate limiting implemented). |
| **Preconditions**| Rate limit configured to low value for test. |
| **Test Data**  | Burst of search or RAG requests above limit. |
| **Steps**      | 1. Send requests until rate limit hit.<br>2. Verify next request returns 429. |
| **Expected Results** | 429 Too Many Requests; no credential in response. |
| **Category**   | Negative, Security |
| **Execution**  | Automatic |

### Test Case 28: Java file parsed with JavaParser
| Field | Value |
| ----- | ----- |
| **ID**         | TC028                                                        |
| **Description**| Verify .java files are parsed by JavaParser and produce structured chunks. |
| **Preconditions**| Ingestion with Java file; JavaParser configured. |
| **Test Data**  | Repo containing .java file with class and methods. |
| **Steps**      | 1. Ingest repo containing known Java file.<br>2. Search for entity_name or class name.<br>3. Verify chunk has entity_type, language=java. |
| **Expected Results** | Chunks have language=java, entity_type (e.g. class, method); correct entity_name. |
| **Category**   | Positive |
| **Execution**  | Automatic |

### Test Case 29: Tree-sitter language file parsed correctly
| Field | Value |
| ----- | ----- |
| **ID**         | TC029                                                        |
| **Description**| Verify a Tree-sitter language (e.g. Python) produces correct chunks. |
| **Preconditions**| Grammar for language available; repo with .py file. |
| **Test Data**  | Repo with .py file containing function and class. |
| **Steps**      | 1. Ingest repo; search for function/class name.<br>2. Verify chunks have language=python, entity_type. |
| **Expected Results** | Chunks with language=python; correct entity names and types. |
| **Category**   | Positive |
| **Execution**  | Automatic |

### Test Case 30: RAG response includes source attribution
| Field | Value |
| ----- | ----- |
| **ID**         | TC030                                                        |
| **Description**| Verify every RAG answer explicitly references source files/entities used. |
| **Preconditions**| RAG and index available. |
| **Test Data**  | question that should match known indexed code. |
| **Steps**      | 1. POST /api/v1/rag with question.<br>2. Parse full response (body or SSE).<br>3. Assert presence of file path or entity references. |
| **Expected Results** | Response contains at least one clear source reference (file or entity). |
| **Category**   | Positive |
| **Execution**  | Automatic |

### Test Case 31: Fallback to Ollama when cloud provider unavailable
| Field | Value |
| ----- | ----- |
| **ID**         | TC031                                                        |
| **Description**| Verify fallback to local Ollama when configured cloud provider is unavailable (if implemented). |
| **Preconditions**| Default=OpenAI; OpenAI unreachable; Ollama configured and running. |
| **Test Data**  | POST /api/v1/rag with question. |
| **Steps**      | 1. Make OpenAI unreachable (e.g. invalid endpoint).<br>2. POST /api/v1/rag.<br>3. Verify response is still streamed (from Ollama). |
| **Expected Results** | Request succeeds using Ollama; or defined error if fallback not configured. |
| **Category**   | Edge |
| **Execution**  | Automatic |

### Test Case 32: Tool discovery latency under 100ms
| Field | Value |
| ----- | ----- |
| **ID**         | TC032                                                        |
| **Description**| Verify MCP tool schema/discovery is served in <100ms (NFR). |
| **Preconditions**| MCP server running. |
| **Test Data**  | Tool list or schema request. |
| **Steps**      | 1. Request tool list or schema from MCP server.<br>2. Measure response time. |
| **Expected Results** | Response in <100ms. |
| **Category**   | Performance |
| **Execution**  | Automatic |

### Test Case 33: Ingestion dashboard shows real-time progress stages
| Field | Value |
| ----- | ----- |
| **ID**         | TC033                                                        |
| **Description**| Verify Web Ingestion Dashboard displays Cloning, Parsing, Embedding, Storing, Complete. |
| **Preconditions**| Dashboard deployed; user can start an ingestion job. |
| **Test Data**  | Start ingestion for a small repo. |
| **Steps**      | 1. Open Ingestion Dashboard.<br>2. Start ingestion.<br>3. Visually confirm stage labels and percentage updates. |
| **Expected Results** | Stages and progress percentage visible and update in real time. |
| **Category**   | Positive |
| **Execution**  | Manual |

### Test Case 34: Search interface faceted filters work correctly
| Field | Value |
| ----- | ----- |
| **ID**         | TC034                                                        |
| **Description**| Verify Search Interface facet filters (language, repository, entity_type) narrow results. |
| **Preconditions**| Dashboard and index with multiple languages/repos. |
| **Test Data**  | Apply language=Java, then repository=X. |
| **Steps**      | 1. Open Search; run query.<br>2. Apply language facet; verify result set.<br>3. Apply repository facet; verify again. |
| **Expected Results** | Results update correctly for each facet; counts consistent. |
| **Category**   | Positive |
| **Execution**  | Manual |

### Test Case 35: RAG chat streams tokens visibly
| Field | Value |
| ----- | ----- |
| **ID**         | TC035                                                        |
| **Description**| Verify RAG Chat Interface shows tokens streaming in real time. |
| **Preconditions**| RAG Chat UI and backend available. |
| **Test Data**  | Type "How is authentication implemented?" and submit. |
| **Steps**      | 1. Open RAG Chat.<br>2. Enter question and submit.<br>3. Observe response area. |
| **Expected Results** | Text appears incrementally (streaming); no long blank wait then full dump. |
| **Category**   | Positive |
| **Execution**  | Manual |

### Test Case 36: Documentation in search results renders correctly
| Field | Value |
| ----- | ----- |
| **ID**         | TC036                                                        |
| **Description**| Verify documentation snippets in search results render (e.g. markdown). |
| **Preconditions**| Index has docs; search returns doc content. |
| **Test Data**  | Query that returns documented entities. |
| **Steps**      | 1. Run search that returns results with doc_summary or doc content.<br>2. Check rendering of docs in UI. |
| **Expected Results** | Doc content displayed; formatting correct (no raw markdown where HTML expected). |
| **Category**   | Positive |
| **Execution**  | Manual |

### Test Case 37: Error message on failed ingestion is clear
| Field | Value |
| ----- | ----- |
| **ID**         | TC037                                                        |
| **Description**| Verify failed ingestion shows user-friendly error without stack trace or paths. |
| **Preconditions**| Ability to trigger failed ingest (e.g. invalid repo). |
| **Test Data**  | Invalid repo or unreachable source. |
| **Steps**      | 1. Trigger ingestion that will fail.<br>2. Read error message in UI or SSE. |
| **Expected Results** | Clear message; no stack trace, no server paths, no credentials. |
| **Category**   | Negative |
| **Execution**  | Manual |

### Test Case 38: Empty search results display appropriately
| Field | Value |
| ----- | ----- |
| **ID**         | TC038                                                        |
| **Description**| Verify UI shows appropriate empty state when search returns no results. |
| **Preconditions**| Search UI available. |
| **Test Data**  | Query that returns 0 results (e.g. random string). |
| **Steps**      | 1. Enter query with no matches.<br>2. Observe result area. |
| **Expected Results** | Clear "no results" message; no broken layout or generic error. |
| **Category**   | Edge |
| **Execution**  | Manual |

### Test Case 39: Long query string does not break search UI
| Field | Value |
| ----- | ----- |
| **ID**         | TC039                                                        |
| **Description**| Verify very long search query does not break layout or submission. |
| **Preconditions**| Search UI available. |
| **Test Data**  | Query string length 2000+ characters. |
| **Steps**      | 1. Paste or type very long query.<br>2. Submit and observe. |
| **Expected Results** | UI remains usable; request either sent or truncated with validation; no crash. |
| **Category**   | Boundary |
| **Execution**  | Manual |

### Test Case 40: Dependency graph visualization (if present)
| Field | Value |
| ----- | ----- |
| **ID**         | TC040                                                        |
| **Description**| If dependency graph is shown in UI, verify nodes and edges display correctly. |
| **Preconditions**| Graph data available; UI has graph view. |
| **Test Data**  | Select entity with known dependents/dependencies. |
| **Steps**      | 1. Open graph view for an entity.<br>2. Verify nodes and relationships visible and readable. |
| **Expected Results** | Graph renders; labels readable; navigation works. |
| **Category**   | Positive |
| **Execution**  | Manual |

### Test Case 41: MCP resource subscription (megabrain://repo/...)
| Field | Value |
| ----- | ----- |
| **ID**         | TC041                                                        |
| **Description**| Verify client can subscribe to megabrain://repo/{name} and receive repository metadata. |
| **Preconditions**| MCP server with resource provider; client that supports subscriptions. |
| **Test Data**  | megabrain://repo/my-repo. |
| **Steps**      | 1. Subscribe to resource URI for a repo.<br>2. Fetch or receive initial content.<br>3. Optionally trigger index update and check notification. |
| **Expected Results** | Resource returns repo metadata; subscription works per MCP spec. |
| **Category**   | Positive |
| **Execution**  | Manual |

### Test Case 42: Cross-browser compatibility of dashboard
| Field | Value |
| ----- | ----- |
| **ID**         | TC042                                                        |
| **Description**| Verify dashboard works in at least two browsers (e.g. Chrome and Firefox). |
| **Preconditions**| Dashboard deployed. |
| **Test Data**  | Chrome, Firefox (or Safari). |
| **Steps**      | 1. Open ingestion, search, and RAG in Browser A.<br>2. Repeat in Browser B.<br>3. Compare behavior. |
| **Expected Results** | No critical differences; search, RAG, and ingestion work in both. |
| **Category**   | Edge |
| **Execution**  | Manual |

### Test Case 43: Keyboard navigation in search and RAG
| Field | Value |
| ----- | ----- |
| **ID**         | TC043                                                        |
| **Description**| Verify keyboard users can submit search and RAG without mouse. |
| **Preconditions**| Search and RAG UI available. |
| **Test Data**  | Tab order and Enter to submit. |
| **Steps**      | 1. Tab to search input; enter query; Tab to submit (or Enter).<br>2. Repeat for RAG input. |
| **Expected Results** | Focus order logical; Enter submits; no focus trap. |
| **Category**   | Positive |
| **Execution**  | Manual |

### Test Case 44: Doc coverage report readability
| Field | Value |
| ----- | ----- |
| **ID**         | TC044                                                        |
| **Description**| Verify documentation quality report is readable and metrics clear. |
| **Preconditions**| Doc coverage report available (UI or export). |
| **Test Data**  | Module with mixed doc coverage. |
| **Steps**      | 1. Generate or open doc coverage report.<br>2. Verify coverage score, completeness, and undocumented API list are clear. |
| **Expected Results** | Report readable; metrics and alerts understandable. |
| **Category**   | Positive |
| **Execution**  | Manual |

### Test Case 45: Concurrent MCP sessions (10+)
| Field | Value |
| ----- | ----- |
| **ID**         | TC045                                                        |
| **Description**| Verify 10+ simultaneous MCP client connections are supported (NFR). |
| **Preconditions**| MCP server; 10+ client simulators or real clients. |
| **Test Data**  | 10 concurrent sessions; each invokes a tool. |
| **Steps**      | 1. Open 10+ MCP sessions.<br>2. From each, call search_code or ask_codebase.<br>3. Verify all complete successfully. |
| **Expected Results** | All sessions receive correct responses; no connection refused or timeout. |
| **Category**   | Concurrency |
| **Execution**  | Manual |

### Test Case 46: Exploratory ingestion of large repo
| Field | Value |
| ----- | ----- |
| **ID**         | TC046                                                        |
| **Description**| Exploratory test: run ingestion on a large repo and observe stability and progress. |
| **Preconditions**| Repo with 100k+ LOC available. |
| **Test Data**  | Large repo. |
| **Steps**      | 1. Start ingestion for large repo.<br>2. Monitor progress, memory, and completion.<br>3. Run search after completion. |
| **Expected Results** | Ingestion completes or fails with clear reason; no silent hang; search works on indexed portion. |
| **Category**   | Performance |
| **Execution**  | Manual |

### Test Case 47: Injection in search query
| Field | Value |
| ----- | ----- |
| **ID**         | TC047                                                        |
| **Description**| Verify search query with SQL/NoSQL or script injection does not execute. |
| **Preconditions**| Search API available. |
| **Test Data**  | q="'; DROP TABLE chunks; --" or script tags. |
| **Steps**      | 1. Send search request with malicious string.<br>2. Verify response is safe (escaped or error) and no side effect. |
| **Expected Results** | No injection; 200 with escaped/sanitized handling or 400. |
| **Category**   | Security |
| **Execution**  | Automatic |

### Test Case 48: get_documentation MCP tool returns doc for entity
| Field | Value |
| ----- | ----- |
| **ID**         | TC048                                                        |
| **Description**| Verify MCP get_documentation returns doc comments for a known entity. |
| **Preconditions**| Entity with Javadoc/docstring indexed; MCP available. |
| **Test Data**  | entity_name=KnownClass.methodName. |
| **Steps**      | 1. Invoke get_documentation with entity_name.<br>2. Verify returned doc content. |
| **Expected Results** | Doc content returned; format matches source (e.g. Javadoc). |
| **Category**   | Positive |
| **Execution**  | Automatic |

### Test Case 49: find_examples returns linked examples
| Field | Value |
| ----- | ----- |
| **ID**         | TC049                                                        |
| **Description**| Verify MCP find_examples returns code examples linked to entity. |
| **Preconditions**| Index has entities with @example or doc examples. |
| **Test Data**  | entity_name=ClassName. |
| **Steps**      | 1. Invoke find_examples for entity.<br>2. Verify list of examples and linkage. |
| **Expected Results** | At least one example or empty list; structure correct. |
| **Category**   | Positive |
| **Execution**  | Automatic |

### Test Case 50: List repositories and list entities
| Field | Value |
| ----- | ----- |
| **ID**         | TC050                                                        |
| **Description**| Verify MCP list_repositories and list_entities return expected data. |
| **Preconditions**| Index populated; MCP server running. |
| **Test Data**  | list_repositories; list_entities path=src/main. |
| **Steps**      | 1. Call list_repositories; verify non-empty if index has repos.<br>2. Call list_entities for a path; verify entity list. |
| **Expected Results** | Repo list and entity list match index; no path traversal. |
| **Category**   | Positive |
| **Execution**  | Automatic |

### Test Case 51: Ingestion progress percentage and stage labels legible
| Field | Value |
| ----- | ----- |
| **ID**         | TC051                                                        |
| **Description**| Verify ingestion progress percentage and stage labels are legible and accessible. |
| **Preconditions**| Ingestion Dashboard open; ingestion running. |
| **Test Data**  | Small repo ingestion. |
| **Steps**      | 1. Start ingestion from dashboard.<br>2. Check visibility of percentage and stage text (font size, contrast).<br>3. Check that progress advances over time. |
| **Expected Results** | Text legible; percentage and stage labels update; no overlapping or cut-off labels. |
| **Category**   | Positive |
| **Execution**  | Manual |

### Test Case 52: RAG chat source links open correct file/line
| Field | Value |
| ----- | ----- |
| **ID**         | TC052                                                        |
| **Description**| Verify clicking a source citation in RAG chat opens or navigates to correct file/line. |
| **Preconditions**| RAG Chat with answer that includes source citations. |
| **Test Data**  | Question that returns answer with file references. |
| **Steps**      | 1. Ask a question; wait for answer with citations.<br>2. Click a source link (if implemented).<br>3. Verify target file/line is correct. |
| **Expected Results** | Link navigates to correct file and line or opens in expected viewer. |
| **Category**   | Positive |
| **Execution**  | Manual |
