/*
 * Copyright (c) 2025 MegaBrain Contributors
 * Licensed under the MIT License - see LICENSE file for details.
 */

package io.megabrain.core;

import io.megabrain.ingestion.parser.TextChunk;
import io.smallrye.mutiny.Uni;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Alternative;
import jakarta.inject.Inject;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.IndexSearcher;
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
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Lucene-based implementation of IndexService for high-performance code search.
 *
 * This service manages a Lucene index for storing and searching TextChunks.
 * It provides thread-safe operations for indexing, searching, and managing
 * code chunks with proper lifecycle management.
 */
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
                        indexWriter.addDocument(doc);
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
                indexWriter.updateDocument(term, doc);
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

                        indexWriter.updateDocument(term, doc);
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
     * Index statistics record.
     */
    public record IndexStats(int numDocs, int maxDoc, int numDeletedDocs) {}
}
