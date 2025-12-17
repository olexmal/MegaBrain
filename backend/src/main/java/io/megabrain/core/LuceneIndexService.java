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
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
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
@ApplicationScoped
public class LuceneIndexService implements IndexService {

    private static final Logger LOG = Logger.getLogger(LuceneIndexService.class);

    @ConfigProperty(name = "megabrain.index.directory", defaultValue = "./data/index")
    protected String indexDirectoryPath;

    private Directory directory;
    private Analyzer analyzer;
    private IndexWriter indexWriter;
    private final ReadWriteLock lock = new ReentrantReadWriteLock();

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
            this.analyzer = new StandardAnalyzer(); // TODO: Replace with custom code-aware analyzer in T3

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
        return Uni.createFrom().item(() -> {
            lock.writeLock().lock();
            try {
                if (chunks == null || chunks.isEmpty()) {
                    return null;
                }

                LOG.debugf("Adding %d chunks to index", chunks.size());

                for (TextChunk chunk : chunks) {
                    Document doc = createDocument(chunk);
                    String documentId = generateDocumentId(chunk);
                    doc.add(new Field(LuceneSchema.FIELD_DOCUMENT_ID, documentId,
                            LuceneSchema.KEYWORD_FIELD_TYPE));

                    indexWriter.addDocument(doc);
                    LOG.tracef("Added chunk: %s", documentId);
                }

                indexWriter.commit();
                LOG.debugf("Successfully added %d chunks to index", chunks.size());

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

    @Override
    public Uni<Void> updateChunksForFile(String filePath, List<TextChunk> newChunks) {
        return removeChunksForFile(filePath)
                .flatMap(removedCount -> addChunks(newChunks));
    }

    /**
     * Searches the index for documents matching a query.
     * This is a basic implementation - will be enhanced with proper query parsing in T5.
     *
     * @param queryString the search query (currently supports exact field matches)
     * @param maxResults maximum number of results to return
     * @return list of matching documents
     */
    public Uni<List<Document>> search(String queryString, int maxResults) {
        return Uni.createFrom().item(() -> {
            lock.readLock().lock();
            try (IndexReader reader = DirectoryReader.open(directory)) {
                IndexSearcher searcher = new IndexSearcher(reader);

                // Search across multiple fields - basic implementation for T2
                org.apache.lucene.search.BooleanQuery.Builder booleanQuery = new org.apache.lucene.search.BooleanQuery.Builder();
                booleanQuery.add(new TermQuery(new Term(LuceneSchema.FIELD_CONTENT, queryString)), org.apache.lucene.search.BooleanClause.Occur.SHOULD);
                booleanQuery.add(new TermQuery(new Term(LuceneSchema.FIELD_DOC_SUMMARY, queryString)), org.apache.lucene.search.BooleanClause.Occur.SHOULD);
                booleanQuery.add(new TermQuery(new Term(LuceneSchema.FIELD_ENTITY_NAME, queryString)), org.apache.lucene.search.BooleanClause.Occur.SHOULD);
                booleanQuery.add(new TermQuery(new Term(LuceneSchema.FIELD_REPOSITORY, queryString)), org.apache.lucene.search.BooleanClause.Occur.SHOULD);

                TopDocs topDocs = searcher.search(booleanQuery.build(), maxResults);

                List<Document> results = new java.util.ArrayList<>();
                for (var scoreDoc : topDocs.scoreDocs) {
                    Document doc = searcher.storedFields().document(scoreDoc.doc);
                    results.add(doc);
                }

                LOG.debugf("Found %d results for query: %s", results.size(), queryString);
                return results;
            } catch (IOException e) {
                LOG.error("Error searching index", e);
                throw new RuntimeException("Failed to search index", e);
            } finally {
                lock.readLock().unlock();
            }
        });
    }

    /**
     * Creates a Lucene Document from a TextChunk.
     */
    private Document createDocument(TextChunk chunk) {
        Document doc = new Document();

        // Core content and metadata fields
        doc.add(new Field(LuceneSchema.FIELD_CONTENT, chunk.content(), LuceneSchema.CONTENT_FIELD_TYPE));
        doc.add(new Field(LuceneSchema.FIELD_ENTITY_NAME, chunk.entityName(), LuceneSchema.ENTITY_NAME_FIELD_TYPE));
        doc.add(new Field(LuceneSchema.FIELD_ENTITY_NAME_KEYWORD, chunk.entityName(), LuceneSchema.KEYWORD_FIELD_TYPE));
        doc.add(new Field(LuceneSchema.FIELD_LANGUAGE, chunk.language(), LuceneSchema.KEYWORD_FIELD_TYPE));
        doc.add(new Field(LuceneSchema.FIELD_ENTITY_TYPE, chunk.entityType(), LuceneSchema.KEYWORD_FIELD_TYPE));
        doc.add(new Field(LuceneSchema.FIELD_FILE_PATH, chunk.sourceFile(), LuceneSchema.KEYWORD_FIELD_TYPE));

        // Repository extraction
        String repository = LuceneSchema.extractRepositoryFromPath(chunk.sourceFile());
        doc.add(new Field(LuceneSchema.FIELD_REPOSITORY, repository, LuceneSchema.KEYWORD_FIELD_TYPE));

        // Line and byte information (stored only)
        doc.add(new Field(LuceneSchema.FIELD_START_LINE, String.valueOf(chunk.startLine()), LuceneSchema.STORED_ONLY_FIELD_TYPE));
        doc.add(new Field(LuceneSchema.FIELD_END_LINE, String.valueOf(chunk.endLine()), LuceneSchema.STORED_ONLY_FIELD_TYPE));
        doc.add(new Field(LuceneSchema.FIELD_START_BYTE, String.valueOf(chunk.startByte()), LuceneSchema.STORED_ONLY_FIELD_TYPE));
        doc.add(new Field(LuceneSchema.FIELD_END_BYTE, String.valueOf(chunk.endByte()), LuceneSchema.STORED_ONLY_FIELD_TYPE));

        // Dynamic metadata fields from attributes
        if (chunk.attributes() != null) {
            for (var entry : chunk.attributes().entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();

                if (LuceneSchema.ATTR_DOC_SUMMARY.equals(key)) {
                    // Special handling for doc summary - make it searchable
                    doc.add(new Field(LuceneSchema.FIELD_DOC_SUMMARY, value, LuceneSchema.CONTENT_FIELD_TYPE));
                } else {
                    // Regular metadata fields as keywords
                    String fieldName = LuceneSchema.createMetadataFieldName(key);
                    doc.add(new Field(fieldName, value, LuceneSchema.KEYWORD_FIELD_TYPE));
                }
            }
        }

        return doc;
    }

    /**
     * Generates a unique document ID for the chunk.
     */
    private String generateDocumentId(TextChunk chunk) {
        return String.format("%s:%s:%d:%d",
                chunk.sourceFile(),
                chunk.entityName(),
                (int) chunk.startLine(),
                (int) chunk.endLine());
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
