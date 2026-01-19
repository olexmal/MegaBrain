# US-02-02: Vector Similarity Search

## Story
**As a** developer  
**I want** to find conceptually similar code even when wording differs  
**So that** I can discover relevant implementations without knowing exact names

## Story Points: 8
## Priority: High
## Sprint Target: Sprint 3

---

## Acceptance Criteria

- [x] **AC1:** Code chunks embedded using sentence transformer model
- [x] **AC2:** Embeddings stored in pgvector (PostgreSQL extension)
- [x] **AC3:** Cosine similarity search returns top-k most similar chunks
- [x] **AC4:** Embedding model configurable (default: code-optimized)
- [x] **AC5:** Vector search latency <500ms for 95th percentile
- [x] **AC6:** Batch embedding during indexing for efficiency
- [x] **AC7:** Results include similarity score

---

## Demo Script

### Setup
1. Ensure repository is ingested with embeddings generated
2. pgvector extension enabled in PostgreSQL

### Demo Steps
1. **Semantic Query:** Search with natural language
   ```bash
   curl "http://localhost:8080/api/v1/search?q=function+that+validates+email+addresses&mode=vector"
   ```
2. **Show Results:** Display matching chunks (may not contain "email" literally)
3. **Compare Keyword:** Show same query with keyword search (likely worse results)
4. **Similarity Scores:** Display cosine similarity values
5. **Different Wording:** Search "check if email is valid" (should find same code)

### Expected Outcome
- Finds conceptually related code
- Works even when query words don't appear in code
- Meaningful similarity scores

---

## Technical Tasks

- [x] **T1:** Set up pgvector extension in PostgreSQL (backend)
- [x] **T2:** Select and integrate embedding model (backend)
- [x] **T3:** Implement `VectorStore` interface (backend)
- [x] **T4:** Implement `PgVectorStore` class (backend)
- [x] **T5:** Create embedding generation service (backend)
- [x] **T6:** Implement batch embedding during indexing (backend)
- [x] **T7:** Implement cosine similarity search (backend)
- [x] **T8:** Add vector index (IVFFlat or HNSW) for performance (backend)
- [x] **T9:** Write unit tests for embedding and search (test)
- [x] **T10:** Performance test with 100K vectors (test)

---

## Test Scenarios

| Scenario | Given | When | Then |
|:---------|:------|:-----|:-----|
| Semantic match | Code for email validation | Search "check email format" | Code found via similarity |
| Synonym handling | Code uses "authenticate" | Search "login" | Code found |
| No exact words | Code has no query words | Semantic search | Still finds relevant code |
| Top-k limit | Many similar chunks | Search with limit=5 | Only 5 results |
| Similarity threshold | Mixed relevance results | Search | Low similarity filtered |

---

## Dependencies

- **Blocked by:** US-01-04 or US-01-05 (needs chunks to embed)
- **Enables:** US-02-03 (hybrid ranking)

---

## Risks

| Risk | Impact | Likelihood | Mitigation |
|:-----|:-------|:-----------|:-----------|
| Embedding model too large | Memory issues | Medium | Quantized models; batching |
| pgvector not available | Feature disabled | Low | Fallback to keyword-only |
| Poor code embeddings | Low relevance | Medium | Use code-specific model |

---

## Definition of Ready

- [x] Acceptance criteria clear
- [x] Dependencies identified
- [x] Tech tasks estimated
- [x] Test scenarios defined
- [x] Demo script approved
- [x] No blockers

---

## Notes
- Recommended model: `all-MiniLM-L6-v2` or code-specific model
- Consider caching embeddings to avoid recomputation

