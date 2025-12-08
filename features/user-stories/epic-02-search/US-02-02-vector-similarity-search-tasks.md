# Tasks for US-02-02: Vector Similarity Search

## Story Reference
- **Epic:** EPIC-02 (Hybrid Search & Retrieval)
- **Story:** US-02-02
- **Story Points:** 8
- **Sprint Target:** Sprint 3

## Task List

### T1: Set up pgvector extension in PostgreSQL
- **Description:** Set up pgvector extension in PostgreSQL database. Create database migration script to enable the extension, verify installation, and configure extension version. Ensure extension is available in all environments (dev, test, prod).
- **Estimated Hours:** 2 hours
- **Assignee:** TBD
- **Status:** Not Started
- **Dependencies:** None (infrastructure setup)
- **Acceptance Criteria:**
  - [ ] pgvector extension enabled in PostgreSQL
  - [ ] Extension version verified
  - [ ] Migration script created
  - [ ] Works in all environments
- **Technical Notes:** Use Flyway or Liquibase for migrations. Extension version: 1.5+. Verify with `SELECT * FROM pg_extension WHERE extname = 'vector';`. Document installation requirements.

### T2: Select and integrate embedding model
- **Description:** Select an appropriate embedding model for code (e.g., all-MiniLM-L6-v2 or code-specific model). Integrate the model using a Java library (e.g., ONNX Runtime, sentence-transformers Java binding, or REST API). Create model loading and inference service.
- **Estimated Hours:** 6 hours
- **Assignee:** TBD
- **Status:** Not Started
- **Dependencies:** None
- **Acceptance Criteria:**
  - [ ] Embedding model selected and integrated
  - [ ] Model loads successfully
  - [ ] Generates embeddings for code chunks
  - [ ] Embedding dimension matches pgvector requirements
  - [ ] Model configurable (can switch models)
- **Technical Notes:** Consider ONNX Runtime for Java, or use sentence-transformers via Python service, or use Hugging Face Inference API. Model should generate 384 or 768-dimensional vectors. Cache model instance for performance.

### T3: Implement VectorStore interface
- **Description:** Create a `VectorStore` interface that abstracts vector storage operations. Define methods for storing embeddings, searching by similarity, batch operations, and vector dimension management. This interface will allow switching between pgvector and other backends (e.g., Milvus).
- **Estimated Hours:** 3 hours
- **Assignee:** TBD
- **Status:** Not Started
- **Dependencies:** T1 (needs pgvector setup)
- **Acceptance Criteria:**
  - [ ] VectorStore interface defined
  - [ ] Methods for storing vectors
  - [ ] Methods for similarity search
  - [ ] Methods for batch operations
  - [ ] Interface is backend-agnostic
- **Technical Notes:** Define interface with methods: store(embedding, metadata), search(queryVector, limit), batchStore(embeddings), delete(id). Use generic types for flexibility.

### T4: Implement PgVectorStore class
- **Description:** Implement `PgVectorStore` class that implements the `VectorStore` interface using pgvector. Create database schema for storing vectors (table with vector column), implement CRUD operations, and implement cosine similarity search using pgvector operators.
- **Estimated Hours:** 6 hours
- **Assignee:** TBD
- **Status:** Not Started
- **Dependencies:** T1, T3 (needs pgvector and interface)
- **Acceptance Criteria:**
  - [ ] PgVectorStore implements VectorStore interface
  - [ ] Vectors stored in PostgreSQL with pgvector type
  - [ ] Cosine similarity search implemented
  - [ ] Batch operations supported
  - [ ] Vector dimension validated
- **Technical Notes:** Use pgvector's `vector` type. Create table: `CREATE TABLE chunks_vectors (id BIGINT, embedding vector(384), ...)`. Use `<=>` operator for cosine similarity. Use JDBC or R2DBC for database access.

### T5: Create embedding generation service
- **Description:** Create `EmbeddingService` that generates embeddings for code chunks using the selected model. The service should handle text preprocessing, model inference, and error handling. Support both single and batch embedding generation.
- **Estimated Hours:** 5 hours
- **Assignee:** TBD
- **Status:** Not Started
- **Dependencies:** T2 (needs embedding model integrated)
- **Acceptance Criteria:**
  - [ ] Generates embeddings for code chunks
  - [ ] Supports single and batch generation
  - [ ] Handles model errors gracefully
  - [ ] Embeddings are consistent and normalized
- **Technical Notes:** Preprocess code text (normalize, truncate if needed). Handle model input/output formats. Cache embeddings to avoid recomputation. Support async/batch processing for performance.

### T6: Implement batch embedding during indexing
- **Description:** Integrate embedding generation into the indexing pipeline. Generate embeddings for chunks in batches during indexing for efficiency. Store embeddings in pgvector alongside chunk metadata. Handle failures gracefully (index without embeddings if generation fails).
- **Estimated Hours:** 4 hours
- **Assignee:** TBD
- **Status:** Not Started
- **Dependencies:** T4, T5 (needs PgVectorStore and EmbeddingService)
- **Acceptance Criteria:**
  - [ ] Embeddings generated during indexing
  - [ ] Batch processing for efficiency
  - [ ] Embeddings stored in database
  - [ ] Indexing continues if embedding generation fails
- **Technical Notes:** Process chunks in batches (e.g., 100 at a time). Use async processing if possible. Store embeddings in same transaction as chunk metadata. Log failures but don't block indexing.

### T7: Implement cosine similarity search
- **Description:** Implement cosine similarity search functionality in PgVectorStore. Given a query embedding, find the top-k most similar chunks using pgvector's cosine distance operator. Return results sorted by similarity score (highest first).
- **Estimated Hours:** 4 hours
- **Assignee:** TBD
- **Status:** Not Started
- **Dependencies:** T4 (needs PgVectorStore)
- **Acceptance Criteria:**
  - [ ] Cosine similarity search implemented
  - [ ] Returns top-k most similar chunks
  - [ ] Results sorted by similarity (highest first)
  - [ ] Similarity scores included in results
  - [ ] Search latency <500ms
- **Technical Notes:** Use pgvector's `<=>` operator for cosine distance. Query: `SELECT *, embedding <=> $1 AS distance FROM chunks_vectors ORDER BY distance LIMIT $2`. Convert distance to similarity score (1 - distance).

### T8: Add vector index (IVFFlat or HNSW) for performance
- **Description:** Create vector indexes (IVFFlat or HNSW) on the embedding column to improve search performance. Configure index parameters (number of lists for IVFFlat, M and ef_construction for HNSW). Create index creation migration.
- **Estimated Hours:** 4 hours
- **Assignee:** TBD
- **Status:** Not Started
- **Dependencies:** T4 (needs PgVectorStore with data)
- **Acceptance Criteria:**
  - [ ] Vector index created on embedding column
  - [ ] Index improves search performance
  - [ ] Index parameters tuned appropriately
  - [ ] Migration script created
- **Technical Notes:** Use HNSW for better performance: `CREATE INDEX ON chunks_vectors USING hnsw (embedding vector_cosine_ops) WITH (m = 16, ef_construction = 64);`. IVFFlat is faster to build but less accurate. Choose based on data size and accuracy requirements.

### T9: Write unit tests for embedding and search
- **Description:** Create comprehensive unit tests for EmbeddingService and PgVectorStore. Test embedding generation, vector storage, similarity search, batch operations, and error handling. Use test database with pgvector.
- **Estimated Hours:** 4 hours
- **Assignee:** TBD
- **Status:** Not Started
- **Dependencies:** T1-T8 (needs complete implementation)
- **Acceptance Criteria:**
  - [ ] Unit tests for embedding generation
  - [ ] Unit tests for vector storage
  - [ ] Unit tests for similarity search
  - [ ] Test coverage >80%
  - [ ] Tests include error scenarios
- **Technical Notes:** Use Testcontainers with PostgreSQL and pgvector. Mock embedding model if needed. Test with known vectors to verify similarity calculations. Test batch operations and edge cases.

### T10: Performance test with 100K vectors
- **Description:** Create performance tests to verify vector search performance with large datasets (100K+ vectors). Measure search latency, index build time, and memory usage. Verify search latency <500ms for 95th percentile.
- **Estimated Hours:** 3 hours
- **Assignee:** TBD
- **Status:** Not Started
- **Dependencies:** T1-T9 (needs complete implementation and tests)
- **Acceptance Criteria:**
  - [ ] Performance test with 100K vectors
  - [ ] Search latency <500ms for 95th percentile
  - [ ] Index build time measured
  - [ ] Memory usage within acceptable limits
- **Technical Notes:** Use JMH for accurate benchmarking. Generate test vectors programmatically. Test with various query vectors. Measure both index build time and search latency. Document performance characteristics.

---

## Summary
- **Total Tasks:** 10
- **Total Estimated Hours:** 41 hours
- **Story Points:** 8 (1 SP ≈ 5.1 hours, aligns with estimate)

