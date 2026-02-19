# Implemented Features

This document describes all features that have been implemented, organized by epic. Each section covers the key classes, configuration, and testing status.

**Status:** 11 of 52 user stories fully completed, 1 partially completed (98 of 211 story points).

---

## EPIC-00: Project Infrastructure Setup

### US-00-01: Maven Project Setup (Done)

The project foundation with all core dependencies and build configuration.

- **Backend:** Single-module Maven project with Quarkus 3.30.2, Java 22
- **Frontend:** Angular 20.3.0 with standalone components, Jest for testing
- **Dependencies:** Quarkus BOM, LangChain4j BOM (1.9.1), Lucene 10.3.2, Neo4j 6.0.2, JGit 7.4.0, JavaParser 3.27.1, java-tree-sitter 0.25.6
- **Build Profiles:** Standard JAR, native (GraalVM), JMH benchmarks
- **CI/CD:** JaCoCo for coverage, Codecov integration
- **Tasks completed:** 16 of 16

---

## EPIC-01: Code Ingestion & Indexing

### US-01-01: GitHub Repository Ingestion (Done)

Fetches and indexes source code from GitHub repositories.

**Key Classes:**
- `GitHubSourceControlClient` - Implements `SourceControlClient` interface for GitHub
- `GitHubApiClient` - REST client for GitHub API v3 (metadata, rate limiting)
- `GitHubTokenProvider` - Secure token management for PAT and GitHub App tokens
- `CompositeSourceControlClient` - Routes to correct provider based on URL

**Capabilities:**
- Clones repositories via JGit with branch specification
- Supports public and private repositories via token auth
- Extracts files with metadata (path, size, modification time)
- Filters binary files and respects `.gitignore` patterns
- Emits progress events via Mutiny `Multi` at key milestones
- Handles rate limiting with exponential backoff

**Configuration:**
```properties
github-api/mp-rest/url=https://api.github.com
megabrain.github.token=${GITHUB_TOKEN}
```

**Tests:** 10 unit tests + 1 integration test (real GitHub API), >80% coverage

---

### US-01-04: Java Parsing with JavaParser (Done)

Parses Java source files into structured code entities using AST analysis.

**Key Classes:**
- `JavaParserService` - Implements `CodeParser` interface using JavaParser 3.27.1
- `JavaAstVisitor` - Custom AST visitor extracting classes, methods, and fields

**Capabilities:**
- Extracts classes with fully qualified names, modifiers, and package declarations
- Extracts methods with signatures, parameters (with types), and return types
- Extracts fields with types, modifiers, and initialization expressions
- Handles inner classes, static nested classes, and anonymous classes with parent-child relationships
- Creates `TextChunk` objects with metadata: language, entity_type, entity_name, source_file, line_range
- Robust error handling for malformed Java files with partial parsing recovery
- Performance: >10,000 LOC per minute

**Tests:** Unit tests covering all entity types, inner classes, interfaces, enums, edge cases. >80% coverage. JMH benchmark.

---

### US-01-05: Tree-sitter Multi-Language Parsing (Done)

Provides code parsing for 14+ programming languages via Tree-sitter.

**Key Classes:**
- `TreeSitterParser` - Abstract base class implementing `CodeParser`
- Language-specific parsers for: C, C++, Python, JavaScript, TypeScript, Go, Rust, Kotlin, Ruby, Scala, Swift, PHP, C#, Java

**Capabilities:**
- Extracts functions, classes, methods from each language's AST
- Handles language-specific constructs (decorators, async functions, type hints, generics)
- File extension routing via `ParserRegistry`
- Dynamic grammar loading from cached binaries
- Consistent `TextChunk` metadata across all languages
- Performance benchmarked per language

**Supported Languages (16 total):**

| Language | Extensions | Key Constructs |
|:---------|:-----------|:---------------|
| Java | `.java` | classes, methods, fields, interfaces, enums |
| Python | `.py` | functions, classes, async functions, decorators |
| JavaScript | `.js`, `.jsx` | functions, classes, arrow functions, exports |
| TypeScript | `.ts`, `.tsx` | interfaces, type aliases, decorators, generics |
| C | `.c`, `.h` | functions, structs, typedefs, macros |
| C++ | `.cpp`, `.hpp`, `.cc` | classes, templates, namespaces |
| Go | `.go` | functions, structs, interfaces, methods |
| Rust | `.rs` | functions, structs, traits, impl blocks |
| Kotlin | `.kt` | classes, functions, data classes, objects |
| Ruby | `.rb` | classes, modules, methods |
| Scala | `.scala` | classes, traits, objects, case classes |
| Swift | `.swift` | classes, structs, protocols, functions |
| PHP | `.php` | classes, functions, interfaces, traits |
| C# | `.cs` | classes, interfaces, structs, methods |

**Tests:** Unit tests per language, performance benchmark. >80% coverage.

---

### US-01-08: Dynamic Grammar Management (Done)

Manages Tree-sitter grammar lifecycle: downloading, caching, versioning, and health checks.

**Key Classes:**
- `ParserRegistry` - Central registry mapping file extensions to parser instances
- `GrammarManager` - Grammar download, caching, and version management
- `GrammarConfig` - Grammar configuration (cache directory, version pins)
- `GrammarSpec` - Grammar specification (name, version, platform binary)
- `GrammarHealthCheck` - Quarkus readiness probe for grammar status

**Capabilities:**
- Downloads grammar binaries from GitHub releases on demand
- Caches grammars locally with version tracking
- Supports version pinning via configuration
- Rollback/downgrade to previous grammar versions
- Health check verifies all configured grammars are loaded
- Fast parser lookup (<10ms) via extension-to-parser mapping
- Supports dynamic registration of new parsers at runtime
- Cold start <500ms requirement met

**Configuration:**
```properties
megabrain.grammar.cache.directory=~/.megabrain/grammars
```

**Tests:** 240+ tests across all grammar management components, >80% coverage.

---

## EPIC-02: Hybrid Search & Retrieval (ALL 6 stories complete)

### US-02-01: Lucene Keyword Search (Done)

Full-text code search powered by Apache Lucene 10.3.2.

**Key Classes:**
- `LuceneIndexService` - Index management (create, write, search, close)
- `CodeAwareAnalyzer` - Custom analyzer: StandardTokenizer + WordDelimiterGraphFilter + CodeSplittingFilter + LowerCaseFilter + StopFilter
- `QueryParserService` - Multi-field query parser with full Lucene syntax
- `DocumentMapper` - Converts `TextChunk` to Lucene `Document` with facet fields
- `LuceneSchema` - Field definitions (entity_name, content, doc_summary, language, repository, entity_type, file_path)

**Capabilities:**
- Code-aware tokenization: splits `getUserName` into [get, user, name] and `get_user_name` into [get, user, name]
- Full Lucene query syntax: AND/OR/NOT, "phrase queries", wildcards (*, ?), field:value
- Batch indexing with configurable batch size (default 1000)
- Document updates via `updateDocument()` and deletions by ID
- Graceful error handling for malformed queries with multiple fallback strategies
- Configurable field boosts applied at query time

**Configuration:**
```properties
megabrain.index.directory=./data/index
megabrain.index.batch.size=1000
```

**Performance:** <500ms search latency at 95th percentile with 100K chunks (verified via JMH benchmark).

**Tests:** 25+ unit tests, JMH benchmark. >80% coverage.

---

### US-02-02: Vector Similarity Search (Done)

Semantic search using vector embeddings stored in PostgreSQL with pgvector.

**Key Classes:**
- `VectorStore` - Backend-agnostic vector storage interface
- `PgVectorStore` - pgvector implementation with cosine similarity
- `EmbeddingService` - Embedding generation for code chunks
- `EmbeddingModelService` - Model loading and management

**Capabilities:**
- pgvector extension setup via Flyway migrations (V1-V3)
- Embedding generation for code chunks (single and batch)
- Cosine similarity search using pgvector `<=>` operator
- HNSW index for fast approximate nearest neighbor search (M=16, ef_construction=64)
- Batch embedding during indexing with graceful degradation
- Vector dimension validation

**Database Migrations:**
- `V1.0.0__enable_pgvector_extension.sql`
- `V2.0.0__create_vector_storage_schema.sql`
- `V3.0.0__add_vector_indexes.sql`

**Configuration:**
```properties
quarkus.datasource.db-kind=postgresql
quarkus.datasource.jdbc.url=jdbc:postgresql://localhost:5432/megabrain_db
megabrain.vector.ef-search=40
```

**Performance:** <500ms search latency at 95th percentile with 100K vectors.

**Tests:** Unit tests for embedding, storage, search. JMH benchmark. >80% coverage.

---

### US-02-03: Hybrid Ranking Algorithm (Done)

Combines keyword and vector search results with configurable weights.

**Key Classes:**
- `HybridScorer` - Weighted score combination (default: 0.6 keyword, 0.4 vector)
- `VectorScoreNormalizer` - Min-max normalization to 0-1 range
- `ResultMerger` - Merges and deduplicates results from both search modes
- `ABTestHarness` - Framework for comparing ranking approaches (precision@k, recall)
- `SearchMode` - Enum: HYBRID, KEYWORD, VECTOR
- `HybridIndexService` - Coordinates Lucene and vector search execution

**Capabilities:**
- Min-max score normalization for fair cross-system comparison
- Weighted combination: `final_score = (kw_weight * lucene) + (vec_weight * vector)`
- Deduplication by chunk ID (document_id or file_path + entity_name)
- Per-request weight override supported
- Three search modes: HYBRID (both), KEYWORD (Lucene only), VECTOR (pgvector only)
- A/B test harness computes precision@5, precision@10, recall across modes

**Configuration:**
```properties
megabrain.search.hybrid.keyword-weight=0.6
megabrain.search.hybrid.vector-weight=0.4
```

**Tests:** 23+ tests for HybridScorer, 14 for ResultMerger, 12 for VectorScoreNormalizer, 22 for ABTestHarness, 11 for search modes. >80% coverage.

---

### US-02-04: Metadata Facet Filtering (Done)

Filter search results by language, repository, file path, and entity type with facet aggregation.

**Key Classes:**
- `SearchFilters` - Filter data record (language, repository, file_path, entity_type)
- `LuceneFilterQueryBuilder` - Builds Lucene filter queries (TermQuery for exact, PrefixQuery for paths; OR within dimension, AND across)
- `FacetValue` - DTO for facet value and count
- `SortedSetDocValuesFacetCounts` - Lucene facet aggregation

**Capabilities:**
- Four filter dimensions: language, repository, file_path (prefix match), entity_type
- Multiple values per filter (OR logic within dimension)
- Combined filters (AND logic across dimensions)
- Filters applied as `BooleanClause.Occur.FILTER` (before scoring for efficiency)
- Filter query caching via `ConcurrentHashMap` (<1ms for cached filters vs ~150ms first build)
- Facet aggregation returns available values with counts for matching documents
- Always returns consistent map structure with all facet keys

**Performance:** <50ms filter overhead (verified via unit tests).

**Tests:** 17 tests for filter building, multiple integration tests, API-level tests. >80% coverage.

---

### US-02-05: Relevance Tuning Configuration (Done)

Configurable field boosts and field match explanation.

**Key Classes:**
- `BoostConfiguration` - Holds boost values loaded from `application.properties`
- `FieldMatchInfo` - Per-field match scores from Lucene Explanation API

**Capabilities:**
- Configurable boost values: entity_name (3.0x), doc_summary (2.0x), content (1.0x)
- Boosts applied at query time via `BoostQuery` wrapping (no reindexing needed)
- Recursive boost application to BooleanQuery, TermQuery, PhraseQuery, WildcardQuery
- Optional field match explanation: shows which fields matched with per-field scores
- Enabled via `include_field_match=true` query parameter

**Configuration:**
```properties
megabrain.search.boost.entity-name=3.0
megabrain.search.boost.doc-summary=2.0
megabrain.search.boost.content=1.0
```

**Tests:** Configuration loading, boost application, ranking verification with inverted boosts. >80% coverage.

---

### US-02-06: Transitive Search Integration (Done)

Graph-powered transitive search for inheritance and usage relationships.

**Key Classes:**
- `SearchOrchestrator` - Coordinates hybrid search + graph queries in parallel
- `GraphQueryService` - Graph query interface
- `GraphQueryServiceStub` - Implementation delegating to closure queries
- `ImplementsClosureQuery` / `Neo4jImplementsClosureQuery` - Transitive implements traversal via Neo4j Cypher
- `ExtendsClosureQuery` / `Neo4jExtendsClosureQuery` - Transitive extends traversal
- `StructuralQueryParser` - Parses `implements:X`, `extends:X`, `usages:X` syntax

**Capabilities:**
- Structural query syntax: `implements:InterfaceName`, `extends:ClassName`, `usages:TypeName`
- Transitive closure via Neo4j Cypher: `MATCH (i)<-[:IMPLEMENTS|EXTENDS*1..depth]-(c) RETURN DISTINCT c`
- Configurable depth limit (default 5, max 10, per-request override)
- Parallel execution: hybrid search runs alongside graph queries
- Graph results merged with hybrid results (deduplicated, sorted by score)
- Results annotated with `is_transitive` flag and `relationship_path` array
- `usages:TypeName` combines both implements and extends closures for polymorphic call site coverage
- Graceful degradation: returns empty when `megabrain.neo4j.uri` is unset

**Configuration:**
```properties
megabrain.search.transitive.default-depth=5
megabrain.search.transitive.max-depth=10
megabrain.neo4j.uri=bolt://localhost:7687
```

**Tests:** Comprehensive tests for structural parsing, closure queries, orchestration, depth validation, result marking. >80% coverage.

---

## EPIC-03: RAG Answer Generation (partial)

### US-03-01: Ollama Local LLM Integration (Partial - T1-T2 of 6)

Foundation for local LLM integration via Ollama.

**Key Classes:**
- `LLMClient` - Unified interface for LLM providers (generate, isAvailable)
- `OllamaLLMClient` - Ollama implementation wrapping LangChain4j `OllamaChatModel`
- `OllamaConfiguration` - Config mapping for base URL, model, and timeout

**Completed:**
- LangChain4j Ollama dependency integrated (BOM 1.9.1)
- `LLMClient` interface defined for provider abstraction
- `OllamaLLMClient` implementation with configuration loading

**Not Yet Implemented:**
- Model selection with per-request override (T3)
- Endpoint configuration with retry logic (T4)
- Health check for Ollama availability (T5)
- Integration tests with real Ollama (T6)

**Configuration:**
```properties
megabrain.llm.ollama.base-url=http://localhost:11434
megabrain.llm.ollama.model=codellama
megabrain.llm.ollama.timeout-seconds=60
```

**Tests:** Unit tests for OllamaLLMClient, LLMClient interface, OllamaConfiguration.
