# US-02-01: Lucene Keyword Search

## Story
**As a** developer  
**I want** to search for exact class names, method names, and identifiers  
**So that** I can quickly find specific code entities

## Story Points: 5
## Priority: Critical
## Sprint Target: Sprint 2

---

## Acceptance Criteria

- [x] **AC1:** Lucene index stores all code chunks with searchable fields
- [x] **AC2:** Exact matches on `entity_name` field rank highest
- [x] **AC3:** Code-aware tokenization (camelCase, snake_case splitting)
- [x] **AC4:** Query syntax supports AND/OR/NOT operators
- [x] **AC5:** Phrase queries for multi-word searches
- [x] **AC6:** Query latency <500ms for 95th percentile
- [x] **AC7:** Results include: chunk content, metadata, relevance score

---

## Demo Script

### Setup
1. Ensure repository is ingested with parsed chunks
2. Index should contain varied entity names

### Demo Steps
1. **Exact Name Search:** Search for a specific class name
   ```bash
   curl "http://localhost:8080/api/v1/search?q=AuthenticationService"
   ```
2. **Show Results:** Display matching chunks with scores
3. **CamelCase Search:** Search for partial name (e.g., "Authentication")
4. **Boolean Query:** Search with AND operator
   ```bash
   curl "http://localhost:8080/api/v1/search?q=user+AND+login"
   ```
5. **Phrase Query:** Search for exact phrase
6. **Performance:** Show query latency in response headers

### Expected Outcome
- Exact matches appear first
- Boolean queries work correctly
- Latency under 500ms

---

## Technical Tasks

- [x] **T1:** Design Lucene index schema (backend)
- [x] **T2:** Implement `LuceneIndexService` class (backend)
- [x] **T3:** Create code-aware analyzer with custom tokenizer (backend)
- [x] **T4:** Implement document indexing from TextChunks (backend)
- [x] **T5:** Implement search query parsing (backend)
- [x] **T6:** Add relevance scoring with field boosts (backend)
- [x] **T7:** Create search result DTO (backend)
- [x] **T8:** Write unit tests for indexing and search (test)
- [x] **T9:** Performance test with 100K chunks (test)

---

## Test Scenarios

| Scenario | Given | When | Then |
|:---------|:------|:-----|:-----|
| Exact match | Chunk with "AuthService" | Search "AuthService" | Chunk returned, high score |
| CamelCase split | Chunk with "getUserName" | Search "user" | Chunk returned |
| snake_case split | Chunk with "get_user_name" | Search "user" | Chunk returned |
| Boolean AND | Multiple chunks | Search "user AND auth" | Only matching both returned |
| Boolean OR | Multiple chunks | Search "user OR admin" | Matching either returned |
| No results | No matching chunks | Search "xyznonexistent" | Empty result set |
| Large result set | Many matches | Search common term | Paginated results |

---

## Dependencies

- **Blocked by:** US-01-04 or US-01-05 (needs chunks to index)
- **Enables:** US-02-03 (hybrid ranking), US-04-02 (search API)

---

## Risks

| Risk | Impact | Likelihood | Mitigation |
|:-----|:-------|:-----------|:-----------|
| Index corruption | Data loss | Low | Backup strategy; transaction logs |
| Large index size | Disk usage | Medium | Optimize schema; compression |
| Query injection | Security issue | Low | Sanitize queries; use QueryParser |

---

## Definition of Ready

- [x] Acceptance criteria clear
- [x] Dependencies identified
- [x] Tech tasks estimated
- [x] Test scenarios defined
- [x] Demo script approved
- [x] No blockers

---

## Implementation Status: ✅ COMPLETED

**US-02-01: Lucene Keyword Search** has been fully implemented and tested.

### Key Deliverables
- Complete Lucene index service with thread-safe operations
- Code-aware analyzer supporting camelCase/snake_case splitting
- Advanced query parser with boolean operators, phrases, and wildcards
- Relevance scoring with configurable field boosts
- REST API DTOs with proper JSON serialization
- Comprehensive unit test suite (373 tests passing)
- JMH performance benchmarks validating <500ms latency target
- Full documentation and task completion tracking

### Performance Results
- Query latency: <500ms for 95th percentile ✅
- Test coverage: >80% ✅
- All acceptance criteria met ✅

### Files Modified
- `backend/src/main/java/io/megabrain/core/LuceneIndexService.java`
- `backend/src/main/java/io/megabrain/core/QueryParserService.java`
- `backend/src/main/java/io/megabrain/api/SearchResult.java`
- `backend/src/main/java/io/megabrain/api/SearchResponse.java`
- `backend/src/main/java/io/megabrain/api/LineRange.java`
- `backend/src/benchmark/java/io/megabrain/core/LuceneIndexServiceBenchmark.java`
- Plus comprehensive test coverage and documentation updates
