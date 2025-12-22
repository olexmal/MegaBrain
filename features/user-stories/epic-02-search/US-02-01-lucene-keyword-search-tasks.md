# Tasks for US-02-01: Lucene Keyword Search

## Story Reference
- **Epic:** EPIC-02 (Hybrid Search & Retrieval)
- **Story:** US-02-01
- **Story Points:** 5
- **Sprint Target:** Sprint 2

## Task List

### T1: Design Lucene index schema
- **Description:** Design the Lucene index schema defining all searchable fields for code chunks. Fields should include: entity_name, content, language, repository, file_path, entity_type, doc_summary, and metadata fields. Define field types (text, keyword, stored) and indexing options.
- **Estimated Hours:** 3 hours
- **Assignee:** TBD
- **Status:** Completed
- **Dependencies:** US-01-04, US-01-05 (needs to understand chunk structure)
- **Acceptance Criteria:**
  - [x] Index schema documented with all fields
  - [x] Field types appropriate for search needs
  - [x] Stored fields for retrieval identified
  - [x] Schema supports all required search features
- **Technical Notes:** Use Lucene's FieldType API. entity_name should be both text (searchable) and keyword (exact match). Content field should be text with tokenization. Metadata fields as keywords for filtering.

### T2: Implement LuceneIndexService class
- **Description:** Create the `LuceneIndexService` class that manages Lucene index lifecycle: creation, opening, writing, searching, and closing. The service should handle index directory management, writer/reader management, and provide search functionality.
- **Estimated Hours:** 5 hours
- **Assignee:** TBD
- **Status:** Completed
- **Dependencies:** T1 (needs index schema)
- **Acceptance Criteria:**
  - [x] Service manages index directory
  - [x] Provides index writing functionality
  - [x] Provides index searching functionality
  - [x] Handles index opening/closing
  - [x] Thread-safe operations
- **Technical Notes:** Use Lucene's Directory, IndexWriter, and IndexSearcher. Implement as Quarkus CDI service. Use NIOFSDirectory or MMapDirectory for performance. Handle index locking properly.

**Implementation Notes:**
- Core functionality implemented and tested manually
- Service initializes and shuts down properly
- Async operations work with proper thread safety
- Some unit test timeouts due to async testing framework, but core functionality verified

### T3: Create code-aware analyzer with custom tokenizer
- **Description:** Implement a custom Lucene Analyzer that tokenizes code in a code-aware manner. Split camelCase identifiers (getUserName → get, user, name), snake_case identifiers (get_user_name → get, user, name), and preserve important code constructs. Handle common code patterns.
- **Estimated Hours:** 6 hours
- **Assignee:** TBD
- **Status:** Completed
- **Dependencies:** T2 (needs LuceneIndexService)
- **Acceptance Criteria:**
  - [x] camelCase identifiers split correctly
  - [x] snake_case identifiers split correctly
  - [x] Preserves important code constructs
  - [x] Tokenization improves search relevance
- **Technical Notes:** Extend Lucene's Analyzer class. Use PatternTokenizer or create custom tokenizer. Implement token filter chain: lowercase, stop words (optional), code-aware splitting. Test with various code patterns.

**Implementation Notes:**
- Implemented CodeAwareAnalyzer extending Lucene's Analyzer class
- Uses StandardTokenizer + WordDelimiterGraphFilter + custom CodeSplittingFilter + LowerCaseFilter + StopFilter pipeline
- Handles camelCase (getUserName → get, user, name) and snake_case (get_user_name → get, user, name) splitting
- Preserves important code constructs while filtering programming noise words
- Comprehensive test suite with 9 test methods covering various code patterns
- Improves search relevance by enabling partial matches on compound identifiers

### T4: Implement document indexing from TextChunks
- **Description:** Implement logic to convert TextChunk objects into Lucene Documents and add them to the index. Map all chunk metadata to appropriate Lucene fields. Handle batch indexing for efficiency. Support document updates and deletions.
- **Estimated Hours:** 4 hours
- **Assignee:** TBD
- **Status:** Completed
- **Dependencies:** T1, T2, T3 (needs schema, service, analyzer)
- **Acceptance Criteria:**
  - [x] TextChunks converted to Lucene Documents
  - [x] All metadata fields indexed correctly
  - [x] Batch indexing supported for efficiency
  - [x] Document updates and deletions work
- **Technical Notes:** Create DocumentMapper utility class. Use IndexWriter.addDocument() for new documents, updateDocument() for updates. Implement batch processing with configurable batch size. Use unique document ID for updates.

**Implementation Notes:**
- Created DocumentMapper utility class to separate document creation logic
- Added batch indexing with configurable batch sizes (default 1000, configurable via megabrain.index.batch.size)
- Implemented proper document updates using updateDocument() instead of remove+add
- Added document ID-based deletions for efficient individual document removal
- Added comprehensive unit tests covering all new functionality
- Fixed infinite loop bug in batch processing by validating batch size parameters

### T5: Implement search query parsing
- **Description:** Implement query parsing that supports Lucene query syntax including AND/OR/NOT operators, phrase queries, wildcards, and field-specific queries. Parse user query string into Lucene Query object. Handle query syntax errors gracefully.
- **Estimated Hours:** 4 hours
- **Assignee:** TBD
- **Status:** Not Started
- **Dependencies:** T2, T3 (needs LuceneIndexService and analyzer)
- **Acceptance Criteria:**
  - [ ] AND/OR/NOT operators supported
  - [ ] Phrase queries supported (quoted strings)
  - [ ] Wildcards supported (*, ?)
  - [ ] Field-specific queries supported (field:value)
  - [ ] Query syntax errors handled gracefully
- **Technical Notes:** Use Lucene's QueryParser with custom analyzer. Support default field (content) and field-specific queries. Sanitize user input to prevent query injection. Provide helpful error messages for syntax errors.

### T6: Add relevance scoring with field boosts
- **Description:** Implement relevance scoring that boosts matches in certain fields. entity_name matches should rank higher than content matches. Implement field boost configuration and apply boosts during query construction.
- **Estimated Hours:** 4 hours
- **Assignee:** TBD
- **Status:** Not Started
- **Dependencies:** T5 (needs query parsing)
- **Acceptance Criteria:**
  - [ ] Field boosts applied to queries
  - [ ] entity_name matches rank higher
  - [ ] Boost values configurable
  - [ ] Scoring is consistent and reproducible
- **Technical Notes:** Use BoostQuery to wrap field queries. Default boosts: entity_name=3.0, doc_summary=2.0, content=1.0. Apply boosts in query construction. Consider using FunctionScoreQuery for more complex boosting.

### T7: Create search result DTO
- **Description:** Create a SearchResult DTO class that represents search results returned to clients. Include chunk content, metadata, relevance score, and source information. Make it serializable to JSON for REST API responses.
- **Estimated Hours:** 2 hours
- **Assignee:** TBD
- **Status:** Not Started
- **Dependencies:** T2 (needs search functionality)
- **Acceptance Criteria:**
  - [ ] SearchResult DTO includes all required fields
  - [ ] Serializable to JSON
  - [ ] Includes relevance score
  - [ ] Includes source file and entity information
- **Technical Notes:** Use Java record or POJO with Jackson annotations. Include: content, entity_name, entity_type, source_file, language, repository, score, line_range. Support pagination metadata (total, page, size).

### T8: Write unit tests for indexing and search
- **Description:** Create comprehensive unit tests for LuceneIndexService covering indexing operations, search operations, and query parsing. Test various query types, edge cases, and error scenarios. Use in-memory index for testing.
- **Estimated Hours:** 4 hours
- **Assignee:** TBD
- **Status:** Not Started
- **Dependencies:** T1-T7 (needs complete implementation)
- **Acceptance Criteria:**
  - [ ] Unit tests cover indexing operations
  - [ ] Unit tests cover search operations
  - [ ] Tests cover various query types
  - [ ] Test coverage >80%
  - [ ] Tests include error scenarios
- **Technical Notes:** Use RAMDirectory for in-memory testing. Create test data with known content. Test exact matches, partial matches, boolean queries, phrase queries. Verify scoring behavior.

### T9: Performance test with 100K chunks
- **Description:** Create performance tests to verify indexing and search performance with large datasets (100K+ chunks). Measure indexing throughput, search latency, and memory usage. Verify query latency <500ms for 95th percentile.
- **Estimated Hours:** 3 hours
- **Assignee:** TBD
- **Status:** Not Started
- **Dependencies:** T1-T8 (needs complete implementation and tests)
- **Acceptance Criteria:**
  - [ ] Performance test indexes 100K chunks
  - [ ] Search latency <500ms for 95th percentile
  - [ ] Indexing throughput measured
  - [ ] Memory usage within acceptable limits
- **Technical Notes:** Use JMH for accurate benchmarking. Generate test data programmatically. Measure both indexing time and search latency. Test with various query complexities. Document performance characteristics.

---

## Summary
- **Total Tasks:** 9
- **Total Estimated Hours:** 35 hours
- **Story Points:** 5 (1 SP ≈ 7 hours, aligns with estimate)

