/*
 * Copyright (c) 2025 MegaBrain Contributors
 * Licensed under the MIT License - see LICENSE file for details.
 */

package io.megabrain.core;

import io.megabrain.ingestion.parser.TextChunk;
import io.smallrye.mutiny.Uni;
import jakarta.annotation.Priority;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Alternative;
import jakarta.inject.Inject;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.facet.FacetResult;
import org.apache.lucene.facet.Facets;
import org.apache.lucene.facet.FacetsCollector;
import org.apache.lucene.facet.FacetsConfig;
import org.apache.lucene.facet.LabelAndValue;
import org.apache.lucene.facet.sortedset.DefaultSortedSetDocValuesReaderState;
import org.apache.lucene.facet.sortedset.SortedSetDocValuesFacetCounts;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexNotFoundException;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.BoostQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.PhraseQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.Explanation;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.WildcardQuery;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.NIOFSDirectory;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Lucene-based implementation of IndexService for high-performance code search.
 * <p>
 * This service manages a Lucene index for storing and searching TextChunks.
 * It provides thread-safe operations for indexing, searching, and managing
 * code chunks with proper lifecycle management.
 */
@Priority(1)
@Alternative
@IndexType(IndexType.Type.LUCENE)
@ApplicationScoped
public class LuceneIndexService implements IndexService {

    private static final Logger LOG = Logger.getLogger(LuceneIndexService.class);

    @ConfigProperty(name = "megabrain.index.directory", defaultValue = "./data/index")
    protected String indexDirectoryPath;

    @ConfigProperty(name = "megabrain.index.batch.size", defaultValue = "1000")
    protected int batchSize;

    @ConfigProperty(name = "megabrain.index.commit.on.batch", defaultValue = "false")
    protected boolean commitOnBatch;

    private Directory directory;
    private Analyzer analyzer;
    private IndexWriter indexWriter;
    private FacetsConfig facetsConfig;
    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    
    // Cache for filter queries to avoid rebuilding them for repeated filter combinations (US-02-04, T4)
    private final Map<SearchFilters, Query> filterQueryCache = new ConcurrentHashMap<>();

    @Inject
    QueryParserService queryParser;

    @Inject
    BoostConfiguration boostConfiguration;

    @PostConstruct
    void initialize() {
        LOG.info("Initializing Lucene index service");

        try {
            // Create index directory if it doesn't exist
            Path indexPath = Paths.get(indexDirectoryPath);
            if (!Files.exists(indexPath)) {
                Files.createDirectories(indexPath);
                LOG.infof("Created index directory: %s", indexPath.toAbsolutePath());
            }

            // Initialize Lucene components
            this.directory = new NIOFSDirectory(indexPath);
            this.analyzer = new CodeAwareAnalyzer();
            this.facetsConfig = buildFacetsConfig();

            IndexWriterConfig config = new IndexWriterConfig(analyzer);
            config.setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);
            config.setCommitOnClose(true);

            this.indexWriter = new IndexWriter(directory, config);

            LOG.info("Lucene index service initialized successfully");
        } catch (IOException e) {
            LOG.error("Failed to initialize Lucene index service", e);
            throw new RuntimeException("Failed to initialize Lucene index service", e);
        }
    }

    @PreDestroy
    void shutdown() {
        lock.writeLock().lock();
        try {
            LOG.info("Shutting down Lucene index service");

            if (indexWriter != null) {
                try {
                    indexWriter.commit();
                    indexWriter.close();
                    LOG.debug("IndexWriter closed successfully");
                } catch (IOException e) {
                    LOG.error("Error closing IndexWriter", e);
                }
            }

            if (analyzer != null) {
                analyzer.close();
                LOG.debug("Analyzer closed successfully");
            }

            if (directory != null) {
                try {
                    directory.close();
                    LOG.debug("Directory closed successfully");
                } catch (IOException e) {
                    LOG.error("Error closing directory", e);
                }
            }

            LOG.info("Lucene index service shutdown complete");
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public Uni<Void> addChunks(List<TextChunk> chunks) {
        return addChunksBatch(chunks, batchSize);
    }

    /**
     * Adds chunks to the index using batch processing for efficiency.
     *
     * @param chunks the text chunks to add
     * @param batchSize the size of each batch to process
     * @return a Uni that completes when indexing is done
     */
    public Uni<Void> addChunksBatch(List<TextChunk> chunks, int batchSize) {
        return Uni.createFrom().item(() -> {
            lock.writeLock().lock();
            try {
                if (chunks == null || chunks.isEmpty()) {
                    return null;
                }

                // Validate batch size to prevent infinite loops
                final int validatedBatchSize;
                if (batchSize <= 0) {
                    LOG.warnf("Invalid batch size %d, using chunks.size() as batch size", batchSize);
                    validatedBatchSize = chunks.size();
                } else {
                    validatedBatchSize = batchSize;
                }

                LOG.debugf("Adding %d chunks to index in batches of %d", chunks.size(), validatedBatchSize);

                int totalProcessed = 0;
                for (int i = 0; i < chunks.size(); i += validatedBatchSize) {
                    int endIndex = Math.min(i + validatedBatchSize, chunks.size());
                    List<TextChunk> batch = chunks.subList(i, endIndex);

                    LOG.tracef("Processing batch %d-%d of %d chunks",
                            i + 1, endIndex, chunks.size());

                    for (TextChunk chunk : batch) {
                        Document doc = DocumentMapper.toDocumentWithId(chunk);
                        indexWriter.addDocument(facetsConfig.build(doc));
                        LOG.tracef("Added chunk: %s", DocumentMapper.generateDocumentId(chunk));
                    }

                    totalProcessed += batch.size();

                    // Commit after each batch if configured
                    if (commitOnBatch) {
                        indexWriter.commit();
                        LOG.tracef("Committed batch of %d chunks", batch.size());
                    }
                }

                // Final commit if not committing on each batch
                if (!commitOnBatch) {
                    indexWriter.commit();
                }

                LOG.debugf("Successfully added %d chunks to index", totalProcessed);
                return null;

            } catch (IOException e) {
                LOG.error("Error adding chunks to index", e);
                throw new RuntimeException("Failed to add chunks to index", e);
            } finally {
                lock.writeLock().unlock();
            }
        });
    }

    @Override
    public Uni<Integer> removeChunksForFile(String filePath) {
        return Uni.createFrom().item(() -> {
            lock.writeLock().lock();
            try {
                LOG.debugf("Removing chunks for file: %s", filePath);

                Term term = new Term(LuceneSchema.FIELD_FILE_PATH, filePath);
                long deletedCount = indexWriter.deleteDocuments(term);
                indexWriter.commit();

                LOG.debugf("Removed %d chunks for file: %s", deletedCount, filePath);
                return (int) deletedCount;
            } catch (IOException e) {
                LOG.error("Error removing chunks for file: " + filePath, e);
                throw new RuntimeException("Failed to remove chunks for file: " + filePath, e);
            } finally {
                lock.writeLock().unlock();
            }
        });
    }

    /**
     * Removes a single document by its document ID.
     *
     * @param documentId the document ID to remove
     * @return a Uni that emits 1 if document was found and removed, 0 otherwise
     */
    public Uni<Integer> removeDocument(String documentId) {
        return Uni.createFrom().item(() -> {
            lock.writeLock().lock();
            try {
                LOG.tracef("Removing document: %s", documentId);

                Term term = new Term(LuceneSchema.FIELD_DOCUMENT_ID, documentId);
                long deletedCount = indexWriter.deleteDocuments(term);
                indexWriter.commit();

                LOG.debugf("Removed %d document(s) with ID: %s", deletedCount, documentId);
                return (int) deletedCount;
            } catch (IOException e) {
                LOG.error("Error removing document: " + documentId, e);
                throw new RuntimeException("Failed to remove document: " + documentId, e);
            } finally {
                lock.writeLock().unlock();
            }
        });
    }

    /**
     * Removes multiple documents by their document IDs.
     *
     * @param documentIds the document IDs to remove
     * @return a Uni that emits the total number of documents removed
     */
    public Uni<Integer> removeDocuments(List<String> documentIds) {
        return Uni.createFrom().item(() -> {
            lock.writeLock().lock();
            try {
                if (documentIds == null || documentIds.isEmpty()) {
                    return 0;
                }

                LOG.debugf("Removing %d documents by ID", documentIds.size());

                Term[] terms = documentIds.stream()
                    .map(id -> new Term(LuceneSchema.FIELD_DOCUMENT_ID, id))
                    .toArray(Term[]::new);

                long deletedCount = indexWriter.deleteDocuments(terms);
                indexWriter.commit();

                LOG.debugf("Removed %d documents by ID", deletedCount);
                return (int) deletedCount;
            } catch (IOException e) {
                LOG.error("Error removing documents by ID", e);
                throw new RuntimeException("Failed to remove documents by ID", e);
            } finally {
                lock.writeLock().unlock();
            }
        });
    }

    @Override
    public Uni<Void> updateChunksForFile(String filePath, List<TextChunk> newChunks) {
        return updateChunksForFileBatch(filePath, newChunks, batchSize);
    }

    /**
     * Updates chunks for a specific file using efficient batch operations.
     * First removes existing chunks for the file, then adds the new chunks.
     *
     * @param filePath the file path
     * @param newChunks the new chunks for this file
     * @param batchSize the batch size for adding operations
     * @return a Uni that completes when update is done
     */
    public Uni<Void> updateChunksForFileBatch(String filePath, List<TextChunk> newChunks, int batchSize) {
        return removeChunksForFile(filePath)
                .flatMap(_ -> {
                    LOG.debugf("Updating %d chunks for file: %s", newChunks.size(), filePath);
                    return addChunksBatch(newChunks, batchSize);
                });
    }

    /**
     * Updates a single document in the index using its document ID.
     * This is more efficient than remove+add for individual document updates.
     *
     * @param chunk the TextChunk to update
     * @return a Uni that completes when update is done
     */
    public Uni<Void> updateDocument(TextChunk chunk) {
        return Uni.createFrom().item(() -> {
            lock.writeLock().lock();
            try {
                String documentId = DocumentMapper.generateDocumentId(chunk);
                Document doc = DocumentMapper.toDocumentWithId(chunk);
                Term term = new Term(LuceneSchema.FIELD_DOCUMENT_ID, documentId);

                LOG.tracef("Updating document: %s", documentId);
                indexWriter.updateDocument(term, facetsConfig.build(doc));
                indexWriter.commit();

                LOG.debugf("Successfully updated document: %s", documentId);
                return null;

            } catch (IOException e) {
                LOG.error("Error updating document", e);
                throw new RuntimeException("Failed to update document", e);
            } finally {
                lock.writeLock().unlock();
            }
        });
    }

    /**
     * Updates multiple documents efficiently.
     * Uses batch processing and updateDocument for better performance.
     *
     * @param chunks the TextChunks to update
     * @return a Uni that completes when all updates are done
     */
    public Uni<Void> updateDocuments(List<TextChunk> chunks) {
        return updateDocumentsBatch(chunks, batchSize);
    }

    /**
     * Updates multiple documents with configurable batch size.
     *
     * @param chunks the TextChunks to update
     * @param batchSize the batch size for processing
     * @return a Uni that completes when all updates are done
     */
    public Uni<Void> updateDocumentsBatch(List<TextChunk> chunks, int batchSize) {
        return Uni.createFrom().item(() -> {
            lock.writeLock().lock();
            try {
                if (chunks == null || chunks.isEmpty()) {
                    return null;
                }

                // Validate batch size to prevent infinite loops
                final int validatedBatchSize;
                if (batchSize <= 0) {
                    LOG.warnf("Invalid batch size %d, using chunks.size() as batch size", batchSize);
                    validatedBatchSize = chunks.size();
                } else {
                    validatedBatchSize = batchSize;
                }

                LOG.debugf("Updating %d documents in batches of %d", chunks.size(), validatedBatchSize);

                int totalProcessed = 0;
                for (int i = 0; i < chunks.size(); i += validatedBatchSize) {
                    int endIndex = Math.min(i + validatedBatchSize, chunks.size());
                    List<TextChunk> batch = chunks.subList(i, endIndex);

                    LOG.tracef("Processing update batch %d-%d of %d documents",
                            i + 1, endIndex, chunks.size());

                    for (TextChunk chunk : batch) {
                        String documentId = DocumentMapper.generateDocumentId(chunk);
                        Document doc = DocumentMapper.toDocumentWithId(chunk);
                        Term term = new Term(LuceneSchema.FIELD_DOCUMENT_ID, documentId);

                        indexWriter.updateDocument(term, facetsConfig.build(doc));
                        LOG.tracef("Updated document: %s", documentId);
                    }

                    totalProcessed += batch.size();

                    // Commit after each batch if configured
                    if (commitOnBatch) {
                        indexWriter.commit();
                        LOG.tracef("Committed update batch of %d documents", batch.size());
                    }
                }

                // Final commit if not committing on each batch
                if (!commitOnBatch) {
                    indexWriter.commit();
                }

                LOG.debugf("Successfully updated %d documents", totalProcessed);
                return null;

            } catch (IOException e) {
                LOG.error("Error updating documents", e);
                throw new RuntimeException("Failed to update documents", e);
            } finally {
                lock.writeLock().unlock();
            }
        });
    }

    /**
     * Searches the index for documents matching a query.
     * Supports full Lucene query syntax including boolean operators, phrase queries,
     * wildcards, and field-specific searches.
     *
     * @param queryString the search query with full Lucene syntax support
     * @param maxResults maximum number of results to return
     * @return list of matching documents
     */
    public Uni<List<Document>> search(String queryString, int maxResults) {
        return queryParser.parseQuery(queryString)
                .flatMap(parsedQuery -> Uni.createFrom().item(() -> {
                    lock.readLock().lock();
                    try (IndexReader reader = DirectoryReader.open(directory)) {
                        IndexSearcher searcher = new IndexSearcher(reader);
                        Query searchQuery = applyFieldBoosts(normalizeFacetQuery(parsedQuery));

                        TopDocs topDocs = searcher.search(searchQuery, maxResults);

                        List<Document> results = new java.util.ArrayList<>();
                        for (var scoreDoc : topDocs.scoreDocs) {
                            Document doc = searcher.storedFields().document(scoreDoc.doc);
                            results.add(doc);
                        }

                        LOG.debugf("Found %d results for parsed query: %s -> %s",
                                 results.size(), queryString, searchQuery);
                        return results;
                    } catch (IndexNotFoundException e) {
                        // Index doesn't exist yet or is empty
                        LOG.debugf("Index not found for query: %s, returning empty results", queryString);
                        return java.util.Collections.emptyList();
                    } catch (IOException e) {
                        LOG.error("Error searching index", e);
                        throw new RuntimeException("Failed to search index", e);
                    } finally {
                        lock.readLock().unlock();
                    }
                }));
    }

    /**
     * Searches within a specific field.
     *
     * @param fieldName the field to search in
     * @param queryString the query string
     * @param maxResults maximum number of results to return
     * @return list of matching documents
     */
    public Uni<List<Document>> searchField(String fieldName, String queryString, int maxResults) {
        return queryParser.parseFieldQuery(fieldName, queryString)
                .flatMap(parsedQuery -> Uni.createFrom().item(() -> {
                    lock.readLock().lock();
                    try (IndexReader reader = DirectoryReader.open(directory)) {
                        IndexSearcher searcher = new IndexSearcher(reader);

                        TopDocs topDocs = searcher.search(parsedQuery, maxResults);

                        List<Document> results = new java.util.ArrayList<>();
                        for (var scoreDoc : topDocs.scoreDocs) {
                            Document doc = searcher.storedFields().document(scoreDoc.doc);
                            results.add(doc);
                        }

                        LOG.debugf("Found %d results for field query: %s:%s -> %s",
                                 results.size(), fieldName, queryString, parsedQuery);
                        return results;
                    } catch (IOException e) {
                        LOG.error("Error searching field", e);
                        throw new RuntimeException("Failed to search field: " + fieldName, e);
                    } finally {
                        lock.readLock().unlock();
                    }
                }));
    }


    /**
     * Gets the current index statistics.
     * Useful for monitoring and testing.
     */
    public Uni<IndexStats> getIndexStats() {
        return Uni.createFrom().item(() -> {
            lock.readLock().lock();
            try (IndexReader reader = DirectoryReader.open(directory)) {
                return new IndexStats(
                        reader.numDocs(),
                        reader.maxDoc(),
                        reader.numDeletedDocs()
                );
            } catch (IndexNotFoundException e) {
                // Index doesn't exist yet, return empty stats
                return new IndexStats(0, 0, 0);
            } catch (IOException e) {
                LOG.error("Error getting index stats", e);
                throw new RuntimeException("Failed to get index stats", e);
            } finally {
                lock.readLock().unlock();
            }
        });
    }

    /** Searchable fields used for field-match extraction (US-02-05, T4). */
    private static final Set<String> SEARCHABLE_FIELDS = Set.of(
            LuceneSchema.FIELD_ENTITY_NAME,
            LuceneSchema.FIELD_DOC_SUMMARY,
            LuceneSchema.FIELD_CONTENT
    );

    /**
     * Represents a Lucene search result with its raw score and optional field match info (US-02-05, T4).
     */
    public record LuceneScoredResult(Document document, float score, FieldMatchInfo fieldMatch) {
        /** Constructor without field match (backward compatible). */
        public LuceneScoredResult(Document document, float score) {
            this(document, score, null);
        }
    }

    /**
     * Searches the index and returns results with their raw Lucene scores.
     * This method preserves the original Lucene scoring for normalization.
     *
     * @param queryString the search query with full Lucene syntax support
     * @param maxResults maximum number of results to return
     * @return list of scored search results
     */
    public Uni<List<LuceneScoredResult>> searchWithScores(String queryString, int maxResults) {
        return searchWithScores(queryString, maxResults, null, false);
    }

    /**
     * Searches the index with optional metadata filters and returns results with raw Lucene scores (US-02-04, T2, T4).
     * <p>
     * When filters are present, builds a filter query (TermQuery for language, repository, entity_type;
     * PrefixQuery for file_path) and applies it as a {@link BooleanClause.Occur#FILTER} clause so that
     * filtering runs before scoring and does not affect relevance scores.
     * <p>
     * Filter queries are cached for reuse to optimize performance (US-02-04, T4). Filter application
     * uses efficient bitset operations internally and is profiled to ensure <50ms overhead.
     *
     * @param queryString the search query with full Lucene syntax support
     * @param maxResults maximum number of results to return
     * @param filters optional metadata filters (language, repository, file_path, entity_type); null or empty to skip
     * @return list of scored search results
     */
    public Uni<List<LuceneScoredResult>> searchWithScores(String queryString, int maxResults, SearchFilters filters) {
        return searchWithScores(queryString, maxResults, filters, false);
    }

    /**
     * Searches the index with optional filters and optional field match explanation (US-02-05, T4).
     * When {@code includeFieldMatch} is true, uses Lucene's Explanation API per hit to populate
     * which fields matched and per-field score contributions. Optional for performance.
     *
     * @param queryString the search query string
     * @param maxResults maximum number of results
     * @param filters optional metadata filters; null to skip
     * @param includeFieldMatch true to compute field match info per result (adds explain() cost per doc)
     * @return list of scored search results, with fieldMatch populated when requested
     */
    public Uni<List<LuceneScoredResult>> searchWithScores(String queryString, int maxResults,
                                                          SearchFilters filters, boolean includeFieldMatch) {
        return queryParser.parseQuery(queryString)
                .flatMap(parsedQuery -> Uni.createFrom().item(() -> {
                    long startTime = System.nanoTime();
                    lock.readLock().lock();
                    try (IndexReader reader = DirectoryReader.open(directory)) {
                        IndexSearcher searcher = new IndexSearcher(reader);
                        Query baseQuery = applyFieldBoosts(normalizeFacetQuery(parsedQuery));
                        
                        // Build filtered query with caching and performance profiling (US-02-04, T4)
                        long filterStartTime = System.nanoTime();
                        Query searchQuery = buildFilteredQueryOptimized(baseQuery, filters);
                        long filterBuildTime = (System.nanoTime() - filterStartTime) / 1_000_000;
                        
                        long searchStartTime = System.nanoTime();
                        TopDocs topDocs = searcher.search(searchQuery, maxResults);
                        long searchTime = (System.nanoTime() - searchStartTime) / 1_000_000;

                        List<LuceneScoredResult> results = new java.util.ArrayList<>();
                        for (var scoreDoc : topDocs.scoreDocs) {
                            Document doc = searcher.storedFields().document(scoreDoc.doc);
                            FieldMatchInfo fieldMatch = includeFieldMatch
                                    ? extractFieldMatch(searcher, searchQuery, scoreDoc.doc)
                                    : null;
                            results.add(new LuceneScoredResult(doc, scoreDoc.score, fieldMatch));
                        }

                        long totalTime = (System.nanoTime() - startTime) / 1_000_000;
                        
                        // Log performance metrics (US-02-04, T4)
                        if (filters != null && filters.hasFilters()) {
                            LOG.debugf("Search with filters completed in %d ms (filter build: %d ms, search: %d ms) - query: %s, results: %d",
                                    totalTime, filterBuildTime, searchTime, queryString, results.size());
                            
                            // Warn if filter overhead exceeds threshold
                            if (filterBuildTime > 50) {
                                LOG.warnf("Filter build time (%d ms) exceeds 50ms threshold for query: %s", filterBuildTime, queryString);
                            }
                        } else {
                            LOG.debugf("Found %d scored results for parsed query: %s -> %s (total time: %d ms)",
                                    results.size(), queryString, searchQuery, totalTime);
                        }
                        
                        return results;
                    } catch (IndexNotFoundException e) {
                        // Index doesn't exist yet or is empty
                        LOG.debugf("Index not found for query: %s, returning empty results", queryString);
                        return java.util.Collections.emptyList();
                    } catch (IOException e) {
                        LOG.error("Error searching index with scores", e);
                        throw new RuntimeException("Failed to search index with scores", e);
                    } finally {
                        lock.readLock().unlock();
                    }
                }));
    }

    /**
     * Computes facet counts for metadata fields (US-02-04, T3, T4).
     * <p>
     * Uses optimized filter application with caching for performance (US-02-04, T4).
     *
     * @param queryString the search query string
     * @param filters optional metadata filters (language, repository, file_path, entity_type)
     * @param maxFacetValues maximum number of facet values to return per field
     * @return map of facet field -> list of facet values with counts
     */
    public Uni<Map<String, List<FacetValue>>> computeFacets(String queryString,
                                                            SearchFilters filters,
                                                            int maxFacetValues) {
        if (maxFacetValues <= 0) {
            return Uni.createFrom().item(Map.of());
        }

        return queryParser.parseQuery(queryString)
                .flatMap(parsedQuery -> Uni.createFrom().item(() -> {
                    long startTime = System.nanoTime();
                    lock.readLock().lock();
                    try {
                        // Open a fresh reader to ensure we see the latest committed changes
                        try (IndexReader reader = DirectoryReader.open(directory)) {
                            IndexSearcher searcher = new IndexSearcher(reader);
                            Query baseQuery = applyFieldBoosts(parsedQuery);
                            
                            // Use optimized filter building with caching (US-02-04, T4)
                            long filterStartTime = System.nanoTime();
                            Query searchQuery = buildFilteredQueryOptimized(baseQuery, filters);
                            long filterBuildTime = (System.nanoTime() - filterStartTime) / 1_000_000;

                            // Create a FacetsCollector to collect matching documents
                            long searchStartTime = System.nanoTime();
                            FacetsCollector facetsCollector = new FacetsCollector();
                            searcher.search(searchQuery, facetsCollector);
                            long searchTime = (System.nanoTime() - searchStartTime) / 1_000_000;

                            // Create facet state and compute counts
                            long facetStartTime = System.nanoTime();
                            DefaultSortedSetDocValuesReaderState state = new DefaultSortedSetDocValuesReaderState(reader, facetsConfig);
                            Facets facets = new SortedSetDocValuesFacetCounts(state, facetsCollector);

                            Map<String, List<FacetValue>> result = Map.of(LuceneSchema.FIELD_LANGUAGE, extractFacetValues(facets, LuceneSchema.FIELD_LANGUAGE, maxFacetValues), LuceneSchema.FIELD_REPOSITORY, extractFacetValues(facets, LuceneSchema.FIELD_REPOSITORY, maxFacetValues), LuceneSchema.FIELD_ENTITY_TYPE, extractFacetValues(facets, LuceneSchema.FIELD_ENTITY_TYPE, maxFacetValues));
                            long facetTime = (System.nanoTime() - facetStartTime) / 1_000_000;

                            long totalTime = (System.nanoTime() - startTime) / 1_000_000;
                            
                            LOG.debugf("Computed facets for query '%s': language=%d, repository=%d, entity_type=%d (total: %d ms, filter: %d ms, search: %d ms, facet: %d ms)",
                                    queryString, result.get(LuceneSchema.FIELD_LANGUAGE).size(), 
                                    result.get(LuceneSchema.FIELD_REPOSITORY).size(), 
                                    result.get(LuceneSchema.FIELD_ENTITY_TYPE).size(),
                                    totalTime, filterBuildTime, searchTime, facetTime);

                            return result;
                        }
                    } catch (IndexNotFoundException e) {
                        LOG.debugf("Index not found for facets on query: %s, returning empty facets", queryString);
                        return Map.of(
                                LuceneSchema.FIELD_LANGUAGE, List.of(),
                                LuceneSchema.FIELD_REPOSITORY, List.of(),
                                LuceneSchema.FIELD_ENTITY_TYPE, List.of()
                        );
                    } catch (IOException e) {
                        LOG.error("Error computing facets", e);
                        throw new RuntimeException("Failed to compute facets", e);
                    } finally {
                        lock.readLock().unlock();
                    }
                }));
    }

    private FacetsConfig buildFacetsConfig() {
        FacetsConfig config = new FacetsConfig();
        config.setMultiValued(LuceneSchema.FIELD_LANGUAGE, false);
        config.setMultiValued(LuceneSchema.FIELD_REPOSITORY, false);
        config.setMultiValued(LuceneSchema.FIELD_ENTITY_TYPE, false);
        return config;
    }

    /**
     * Builds a filtered query with optimized filter application (US-02-04, T4).
     * <p>
     * Uses cached filter queries when available to avoid rebuilding identical filters.
     * Filters are applied using BooleanClause.Occur.FILTER which uses efficient bitset
     * operations internally and applies filters before scoring.
     *
     * @param parsedQuery the base search query
     * @param filters optional metadata filters
     * @return the filtered query, or the original query if no filters
     */
    private Query buildFilteredQuery(Query parsedQuery, SearchFilters filters) {
        return buildFilteredQueryOptimized(parsedQuery, filters);
    }

    /**
     * Optimized version that caches filter queries for reuse (US-02-04, T4).
     */
    private Query buildFilteredQueryOptimized(Query parsedQuery, SearchFilters filters) {
        if (filters == null || !filters.hasFilters()) {
            return parsedQuery;
        }

        // Get or build filter query with caching
        Query filterQuery = filterQueryCache.computeIfAbsent(filters, f -> {
            Optional<Query> filterOpt = LuceneFilterQueryBuilder.build(f);
            return filterOpt.orElse(null);
        });

        if (filterQuery == null) {
            return parsedQuery;
        }

        // Combine base query with filter using FILTER clause for efficient bitset-based filtering
        BooleanQuery.Builder builder = new BooleanQuery.Builder();
        builder.add(parsedQuery, BooleanClause.Occur.MUST);
        builder.add(filterQuery, BooleanClause.Occur.FILTER);
        return builder.build();
    }

    private Query normalizeFacetQuery(Query parsedQuery) {
        if (parsedQuery == null) {
            return new MatchAllDocsQuery();
        }
        String queryString = parsedQuery.toString();
        if (queryString == null || queryString.isBlank()) {
            return new MatchAllDocsQuery();
        }
        return parsedQuery;
    }

    /**
     * Applies field boosts from configuration at query time (US-02-05, T3).
     * Recursively wraps field-specific subqueries (TermQuery, PhraseQuery, WildcardQuery)
     * with BoostQuery using BoostConfiguration so that entity_name, doc_summary, and content
     * matches are weighted without reindexing.
     *
     * @param query the parsed query
     * @return query with configuration boosts applied
     */
    private Query applyFieldBoosts(Query query) {
        if (query == null) {
            return null;
        }
        if (query instanceof BooleanQuery bq) {
            BooleanQuery.Builder builder = new BooleanQuery.Builder();
            for (BooleanClause clause : bq.clauses()) {
                builder.add(applyFieldBoosts(clause.query()), clause.occur());
            }
            return builder.build();
        }
        if (query instanceof BoostQuery bq) {
            Query inner = applyFieldBoosts(bq.getQuery());
            return new BoostQuery(inner, bq.getBoost());
        }
        if (query instanceof TermQuery tq) {
            float boost = boostConfiguration.getBoostForField(tq.getTerm().field());
            if (boost != 1.0f) {
                return new BoostQuery(query, boost);
            }
            return query;
        }
        if (query instanceof PhraseQuery pq) {
            Term[] terms = pq.getTerms();
            if (terms != null && terms.length > 0) {
                float boost = boostConfiguration.getBoostForField(terms[0].field());
                if (boost != 1.0f) {
                    return new BoostQuery(query, boost);
                }
            }
            return query;
        }
        if (query instanceof WildcardQuery wq) {
            float boost = boostConfiguration.getBoostForField(wq.getTerm().field());
            if (boost != 1.0f) {
                return new BoostQuery(query, boost);
            }
            return query;
        }
        return query;
    }

    private List<FacetValue> extractFacetValues(Facets facets, String fieldName, int maxFacetValues) throws IOException {
        FacetResult facetResult = facets.getTopChildren(maxFacetValues, fieldName);
        if (facetResult == null || facetResult.labelValues == null) {
            return List.of();
        }
        List<FacetValue> values = new java.util.ArrayList<>();
        for (LabelAndValue labelAndValue : facetResult.labelValues) {
            values.add(new FacetValue(labelAndValue.label, labelAndValue.value.longValue()));
        }
        return values;
    }

    /**
     * Extracts which index fields contributed to the score for a document using Lucene's Explanation API (US-02-05, T4).
     * Traverses the explanation tree and collects per-field score contributions for entity_name, doc_summary, content.
     *
     * @param searcher the index searcher (must be open for the duration of the call)
     * @param query the executed query (same as used for search)
     * @param docId the document id
     * @return field match info with matched_fields and scores; empty if explanation fails or no field contributions
     */
    FieldMatchInfo extractFieldMatch(IndexSearcher searcher, Query query, int docId) {
        try {
            Explanation explanation = searcher.explain(query, docId);
            if (explanation == null || !explanation.isMatch()) {
                return new FieldMatchInfo(List.of(), Map.of());
            }
            Map<String, Float> scoresByField = new LinkedHashMap<>();
            collectFieldScores(explanation, scoresByField);
            List<String> matchedFields = new ArrayList<>(scoresByField.keySet());
            return new FieldMatchInfo(matchedFields, Map.copyOf(scoresByField));
        } catch (IOException e) {
            LOG.debugf(e, "Could not explain doc %d for field match", docId);
            return new FieldMatchInfo(List.of(), Map.of());
        }
    }

    /**
     * Recursively collects score contributions from explanation tree for searchable fields.
     * Attributes any node whose description contains a known field name (e.g. "weight(entity_name:...)").
     */
    private void collectFieldScores(Explanation explanation, Map<String, Float> scoresByField) {
        if (explanation == null) {
            return;
        }
        String desc = explanation.getDescription();
        if (desc != null) {
            for (String field : SEARCHABLE_FIELDS) {
                if (desc.contains(field)) {
                    float value = explanation.getValue() != null
                            ? explanation.getValue().floatValue()
                            : 0f;
                    if (value > 0f) {
                        scoresByField.merge(field, value, Float::sum);
                    }
                    break; // at most one field per node
                }
            }
        }
        Explanation[] details = explanation.getDetails();
        if (details != null) {
            for (Explanation detail : details) {
                collectFieldScores(detail, scoresByField);
            }
        }
    }

    /**
     * Normalizes Lucene scores to a 0.0-1.0 range using min-max normalization.
     * This ensures fair combination with vector similarity scores.
     *
     * @param scoredResults the list of scored search results to normalize
     * @return list of results with normalized scores (0.0-1.0 range)
     */
    public static List<LuceneScoredResult> normalizeScores(List<LuceneScoredResult> scoredResults) {
        if (scoredResults == null || scoredResults.isEmpty()) {
            return scoredResults != null ? scoredResults : java.util.Collections.emptyList();
        }

        // Handle single result case - assign score of 1.0
        if (scoredResults.size() == 1) {
            LuceneScoredResult result = scoredResults.getFirst();
            return List.of(new LuceneScoredResult(result.document(), 1.0f, result.fieldMatch()));
        }

        // Find min and max scores using simple iteration
        float minScore = Float.MAX_VALUE;
        float maxScore = Float.MIN_VALUE;

        for (LuceneScoredResult result : scoredResults) {
            float score = result.score();
            if (score < minScore) minScore = score;
            if (score > maxScore) maxScore = score;
        }

        // Handle case where all scores are equal (would cause division by zero)
        if (maxScore == minScore) {
            // All results get score 1.0 (they are equally relevant)
            return scoredResults.stream()
                    .map(result -> new LuceneScoredResult(result.document(), 1.0f, result.fieldMatch()))
                    .collect(java.util.stream.Collectors.toList());
        }

        // Apply min-max normalization: normalized = (score - min) / (max - min)
        List<LuceneScoredResult> normalizedResults = new java.util.ArrayList<>();
        for (LuceneScoredResult result : scoredResults) {
            float normalizedScore = (result.score() - minScore) / (maxScore - minScore);
            normalizedResults.add(new LuceneScoredResult(result.document(), normalizedScore, result.fieldMatch()));
        }
        return normalizedResults;
    }

    /**
     * Index statistics record.
     */
    public record IndexStats(int numDocs, int maxDoc, int numDeletedDocs) {}
}
