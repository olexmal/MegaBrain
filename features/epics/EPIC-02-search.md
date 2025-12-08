# EPIC-02: Hybrid Search & Retrieval

## Epic Overview

| Attribute | Value |
|:----------|:------|
| **Epic ID** | EPIC-02 |
| **Priority** | Critical |
| **Estimated Scope** | L |
| **Dependencies** | EPIC-01 (Code Ingestion & Indexing) |
| **Spec Reference** | Section 4.2 (FR-SRH) |
| **Status** | Planned |

## Business Value

This epic delivers the core search capability that makes indexed code discoverable. By combining keyword search (Lucene) with optional vector similarity search (pgvector), developers can find code using:

- Exact identifiers (class names, method names, error codes)
- Natural language queries ("how to parse JSON")
- Conceptual searches where wording differs from actual code

This hybrid approach ensures high precision for exact matches while capturing semantic similarity for conceptual queries.

---

## User Stories

### US-02-01: Keyword Search via Lucene

**As a** developer, **I want** to search for exact class names, method names, and identifiers, **so that** I can quickly find specific code entities.

**Acceptance Criteria:**
- [ ] Lucene index stores all code chunks with full-text search capability
- [ ] Exact matches on `entity_name` field rank highest
- [ ] Support for code-aware tokenization (camelCase, snake_case splitting)
- [ ] Query syntax supports AND/OR/NOT operators
- [ ] Phrase queries for multi-word searches
- [ ] Query latency <500ms for 95th percentile

**Spec Reference:** FR-SRH-01 (Keyword Search)

---

### US-02-02: Vector Similarity Search

**As a** developer, **I want** to find conceptually similar code even when my query wording differs from the actual code, **so that** I can discover relevant implementations without knowing exact names.

**Acceptance Criteria:**
- [ ] Code chunks embedded using sentence transformer model
- [ ] Embeddings stored in pgvector (PostgreSQL) or Milvus
- [ ] Cosine similarity search returns top-k most similar chunks
- [ ] Vector search configurable as primary or fallback
- [ ] Embedding model configurable (default: code-optimized model)
- [ ] Vector search latency <500ms for 95th percentile

**Spec Reference:** FR-SRH-01 (Vector Search)

---

### US-02-03: Hybrid Ranking

**As a** developer, **I want** search results to combine keyword and vector relevance, **so that** I get the best of both approaches.

**Acceptance Criteria:**
- [ ] Final score = weighted combination of Lucene score and vector similarity
- [ ] Weights configurable (default: 0.6 keyword, 0.4 vector)
- [ ] Results deduplicated when same chunk appears in both result sets
- [ ] Hybrid ranking improves relevance over either approach alone
- [ ] Option to use keyword-only or vector-only search

**Spec Reference:** FR-SRH-01 (Ranking)

---

### US-02-04: Metadata Filtering

**As a** developer, **I want** to filter search results by language, repository, file path, and entity type, **so that** I can narrow down results to relevant code.

**Acceptance Criteria:**
- [ ] Filter by `language` (Java, Python, etc.)
- [ ] Filter by `repository` name
- [ ] Filter by `file_path` (prefix match)
- [ ] Filter by `entity_type` (class, method, function, etc.)
- [ ] Filters combinable with AND logic
- [ ] Filters applied before ranking for efficiency
- [ ] Filter options populated from indexed data (facets)

**Spec Reference:** FR-SRH-02

---

### US-02-05: Relevance Tuning

**As a** search administrator, **I want** to boost certain fields in ranking, **so that** matches in important fields (entity names) rank higher than matches in comments.

**Acceptance Criteria:**
- [ ] Configurable boost factors per field
- [ ] Default boosts: `entity_name` (3.0), `doc_summary` (2.0), `content` (1.0)
- [ ] Boost configuration via application properties
- [ ] A/B testing support for tuning experiments

**Spec Reference:** FR-SRH-03

---

### US-02-06: Transitive Search (Graph-Enhanced)

**As a** developer, **I want** to find all implementations of an interface including transitive subclasses, **so that** inheritance hierarchies don't hide relevant results.

**Acceptance Criteria:**
- [ ] Search API supports `transitive=true` parameter
- [ ] "Find implementations of X" includes classes extending abstract implementers
- [ ] "Find usages of X" includes polymorphic call sites
- [ ] Graph traversal integrated into search pipeline
- [ ] Transitive results clearly marked in response

**Spec Reference:** FR-DEP-04 (Graph-Enhanced Search Integration)

---

## Technical Notes

### Key Components
- **`LuceneIndexService`:** Manages embedded Lucene index for keyword search
- **`VectorStore`:** Interface for vector similarity search (pgvector implementation)
- **`MegaBrainOrchestrator`:** Coordinates hybrid search combining both approaches

### Technology Stack
| Component | Technology |
|:----------|:-----------|
| Keyword Index | Apache Lucene (Embedded) |
| Vector Storage | pgvector (PostgreSQL extension) or Milvus |
| Embedding Model | Sentence Transformers (e.g., all-MiniLM-L6-v2) |
| Query Parsing | Lucene QueryParser |

### Index Schema (Lucene)

```
Field: id (StringField, stored)
Field: content (TextField, indexed)
Field: entity_name (TextField, indexed, boosted)
Field: entity_type (StringField, indexed)
Field: language (StringField, indexed)
Field: repository (StringField, indexed)
Field: file_path (StringField, indexed)
Field: doc_summary (TextField, indexed, boosted)
```

### Embedding Strategy
- Chunk content + entity name concatenated for embedding
- Max token length: 512 tokens
- Long chunks truncated with overlap strategy
- Batch embedding for efficiency during indexing

### Search Pipeline
```
Query → Query Parser → [Keyword Search] → Results
                    → [Vector Search]  → Results
                                        → Hybrid Merger → Ranked Results → Filters → Response
```

---

## Risks & Mitigations

| Risk | Impact | Likelihood | Mitigation |
|:-----|:-------|:-----------|:-----------|
| Vector search latency too high | Poor UX | Medium | Index optimization; approximate NN (HNSW); caching |
| Embedding model too large | Memory constraints | Medium | Use quantized models; lazy loading |
| Hybrid ranking degrades results | Lower relevance | Low | A/B testing; configurable weights; fallback to keyword-only |
| Lucene index corruption | Data loss | Low | Regular backups; transaction logging; recovery procedures |
| pgvector not available | Vector search disabled | Low | Graceful degradation to keyword-only; clear documentation |

---

## Non-Functional Requirements

| NFR | Target | Validation |
|:----|:-------|:-----------|
| Query latency (95th percentile) | <500ms | Load testing with representative queries |
| Index size | <2x raw code size | Measure on 1M LOC benchmark |
| Concurrent queries | 100+ simultaneous | Load testing |
| Relevance (MRR@10) | >0.6 | Evaluation on curated query set |

---

## Definition of Done

- [ ] All user stories complete and accepted
- [ ] Lucene index operational with all fields
- [ ] Vector search working with pgvector
- [ ] Hybrid ranking implemented and tunable
- [ ] All metadata filters functional
- [ ] Relevance tuning configurable
- [ ] Query latency NFRs met
- [ ] Unit tests for search components (>80% coverage)
- [ ] Integration tests with real indexed data
- [ ] Documentation updated

---

## Open Questions

1. Should we support saved searches / search history?
2. What embedding model should be the default for code?
3. Should we expose raw Lucene query syntax to advanced users?
4. How do we handle search across multiple indexes (multi-tenant)?

---

**Epic Owner:** TBD  
**Created:** December 2024  
**Last Updated:** December 2024

