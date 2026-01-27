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
import org.apache.lucene.document.Field;
import org.apache.lucene.facet.FacetResult;
import org.apache.lucene.facet.Facets;
import org.apache.lucene.facet.FacetsCollector;
import org.apache.lucene.facet.FacetsConfig;
import org.apache.lucene.facet.LabelAndValue;
import org.apache.lucene.facet.sortedset.DefaultSortedSetDocValuesReaderState;
import org.apache.lucene.facet.sortedset.SortedSetDocValuesFacetCounts;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.NIOFSDirectory;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Lucene-based implementation of IndexService for high-performance code search.
 *
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

    @Inject
    QueryParserService queryParser;

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
                .flatMap(removedCount -> {
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

                        TopDocs topDocs = searcher.search(parsedQuery, maxResults);

                        List<Document> results = new java.util.ArrayList<>();
                        for (var scoreDoc : topDocs.scoreDocs) {
                            Document doc = searcher.storedFields().document(scoreDoc.doc);
                            results.add(doc);
                        }

                        LOG.debugf("Found %d results for parsed query: %s -> %s",
                                 results.size(), queryString, parsedQuery);
                        return results;
                    } catch (org.apache.lucene.index.IndexNotFoundException e) {
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
            } catch (org.apache.lucene.index.IndexNotFoundException e) {
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

    /**
     * Represents a Lucene search result with its raw score.
     */
    public record LuceneScoredResult(Document document, float score) {}

    /**
     * Searches the index and returns results with their raw Lucene scores.
     * This method preserves the original Lucene scoring for normalization.
     *
     * @param queryString the search query with full Lucene syntax support
     * @param maxResults maximum number of results to return
     * @return list of scored search results
     */
    public Uni<List<LuceneScoredResult>> searchWithScores(String queryString, int maxResults) {
        return searchWithScores(queryString, maxResults, null);
    }

    /**
     * Searches the index with optional metadata filters and returns results with raw Lucene scores (US-02-04, T2).
     * <p>
     * When filters are present, builds a filter query (TermQuery for language, repository, entity_type;
     * PrefixQuery for file_path) and applies it as a {@link BooleanClause.Occur#FILTER} clause so that
     * filtering runs before scoring and does not affect relevance scores.
     *
     * @param queryString the search query with full Lucene syntax support
     * @param maxResults maximum number of results to return
     * @param filters optional metadata filters (language, repository, file_path, entity_type); null or empty to skip
     * @return list of scored search results
     */
    public Uni<List<LuceneScoredResult>> searchWithScores(String queryString, int maxResults, SearchFilters filters) {
        return queryParser.parseQuery(queryString)
                .flatMap(parsedQuery -> Uni.createFrom().item(() -> {
                    lock.readLock().lock();
                    try (IndexReader reader = DirectoryReader.open(directory)) {
                        IndexSearcher searcher = new IndexSearcher(reader);
                        Query baseQuery = normalizeFacetQuery(parsedQuery);
                        Query searchQuery = buildFilteredQuery(baseQuery, filters);

                        TopDocs topDocs = searcher.search(searchQuery, maxResults);

                        List<LuceneScoredResult> results = new java.util.ArrayList<>();
                        for (var scoreDoc : topDocs.scoreDocs) {
                            Document doc = searcher.storedFields().document(scoreDoc.doc);
                            results.add(new LuceneScoredResult(doc, scoreDoc.score));
                        }

                        LOG.debugf("Found %d scored results for parsed query: %s -> %s",
                                 results.size(), queryString, searchQuery);
                        return results;
                    } catch (org.apache.lucene.index.IndexNotFoundException e) {
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
     * Computes facet counts for metadata fields (US-02-04, T3).
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
                    lock.readLock().lock();
                    try {
                        // Open a fresh reader to ensure we see the latest committed changes
                        IndexReader reader = DirectoryReader.open(directory);
                        try {
                            IndexSearcher searcher = new IndexSearcher(reader);
                            Query searchQuery = buildFilteredQuery(parsedQuery, filters);

                            // Create a FacetsCollector to collect matching documents
                            FacetsCollector facetsCollector = new FacetsCollector();
                            searcher.search(searchQuery, facetsCollector);

                            // Create facet state and compute counts
                            DefaultSortedSetDocValuesReaderState state =
                                    new DefaultSortedSetDocValuesReaderState(reader, facetsConfig);
                            Facets facets = new SortedSetDocValuesFacetCounts(state, facetsCollector);
                            
                            Map<String, List<FacetValue>> result = Map.of(
                                    LuceneSchema.FIELD_LANGUAGE, extractFacetValues(facets, LuceneSchema.FIELD_LANGUAGE, maxFacetValues),
                                    LuceneSchema.FIELD_REPOSITORY, extractFacetValues(facets, LuceneSchema.FIELD_REPOSITORY, maxFacetValues),
                                    LuceneSchema.FIELD_ENTITY_TYPE, extractFacetValues(facets, LuceneSchema.FIELD_ENTITY_TYPE, maxFacetValues)
                            );
                            
                            LOG.debugf("Computed facets for query '%s': language=%d, repository=%d, entity_type=%d",
                                    queryString,
                                    result.get(LuceneSchema.FIELD_LANGUAGE).size(),
                                    result.get(LuceneSchema.FIELD_REPOSITORY).size(),
                                    result.get(LuceneSchema.FIELD_ENTITY_TYPE).size());
                            
                            return result;
                        } finally {
                            reader.close();
                        }
                    } catch (org.apache.lucene.index.IndexNotFoundException e) {
                        LOG.debugf("Index not found for facets on query: %s, returning empty facets", queryString);
                        return Map.of(
                                LuceneSchema.FIELD_LANGUAGE, List.<FacetValue>of(),
                                LuceneSchema.FIELD_REPOSITORY, List.<FacetValue>of(),
                                LuceneSchema.FIELD_ENTITY_TYPE, List.<FacetValue>of()
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

    private Query buildFilteredQuery(Query parsedQuery, SearchFilters filters) {
        Query searchQuery = parsedQuery;
        var filterOpt = LuceneFilterQueryBuilder.build(filters);
        if (filterOpt.isPresent()) {
            BooleanQuery.Builder builder = new BooleanQuery.Builder();
            builder.add(parsedQuery, BooleanClause.Occur.MUST);
            builder.add(filterOpt.get(), BooleanClause.Occur.FILTER);
            searchQuery = builder.build();
        }
        return searchQuery;
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
            LuceneScoredResult result = scoredResults.get(0);
            return List.of(new LuceneScoredResult(result.document(), 1.0f));
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
                    .map(result -> new LuceneScoredResult(result.document(), 1.0f))
                    .collect(java.util.stream.Collectors.toList());
        }

        // Apply min-max normalization: normalized = (score - min) / (max - min)
        List<LuceneScoredResult> normalizedResults = new java.util.ArrayList<>();
        for (LuceneScoredResult result : scoredResults) {
            float normalizedScore = (result.score() - minScore) / (maxScore - minScore);
            normalizedResults.add(new LuceneScoredResult(result.document(), normalizedScore));
        }
        return normalizedResults;
    }

    /**
     * Index statistics record.
     */
    public record IndexStats(int numDocs, int maxDoc, int numDeletedDocs) {}
}
