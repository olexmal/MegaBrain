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
import org.jboss.logging.Logger;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Hybrid index service that combines Lucene keyword search with vector similarity search.
 *
 * This service provides both traditional keyword-based search through Lucene and
 * semantic similarity search through vector embeddings stored in pgvector.
 */
@Alternative
@IndexType(IndexType.Type.HYBRID)
@ApplicationScoped
public class HybridIndexService implements IndexService {

    private static final Logger LOG = Logger.getLogger(HybridIndexService.class);

    @Inject
    @IndexType(IndexType.Type.LUCENE)
    IndexService luceneIndexService;

    @Inject
    EmbeddingService embeddingService;

    @Inject
    PgVectorStore vectorStore;

    @PostConstruct
    void init() {
        LOG.info("Initializing HybridIndexService");
        LOG.info("Hybrid index service initialized successfully");
    }

    @PreDestroy
    void destroy() {
        LOG.info("Shutting down HybridIndexService");
    }

    @Override
    public Uni<Void> addChunks(List<TextChunk> chunks) {
        LOG.debugf("Adding %d chunks to hybrid index", chunks.size());

        // Index in Lucene first (for keyword search)
        Uni<Void> luceneIndexing = luceneIndexService.addChunks(chunks);

        // Generate embeddings and store in vector database
        Uni<Void> vectorIndexing = embeddingService.generateEmbeddings(chunks)
                .map(results -> {
                    // Filter successful embeddings
                    List<EmbeddingService.EmbeddingResult> successfulResults = results.stream()
                            .filter(EmbeddingService.EmbeddingResult::isSuccess)
                            .collect(Collectors.toList());

                    if (successfulResults.isEmpty()) {
                        LOG.warn("No successful embeddings generated, skipping vector storage");
                        return Uni.createFrom().voidItem();
                    }

                    // Convert to vector entries
                    List<VectorStore.VectorEntry> vectorEntries = successfulResults.stream()
                            .map(result -> {
                                TextChunk chunk = result.getChunk();
                                return new VectorStore.VectorEntry(
                                    generateVectorId(chunk),
                                    result.getEmbedding(),
                                    new VectorStore.VectorMetadata(
                                        chunk.content(),
                                        chunk.language(),
                                        chunk.entityType(),
                                        chunk.entityName(),
                                        chunk.sourceFile(),
                                        chunk.startLine(),
                                        chunk.endLine(),
                                        chunk.startByte(),
                                        chunk.endByte()
                                    )
                                );
                            })
                            .collect(Collectors.toList());

                    // Store in vector database
                    return vectorStore.storeBatch(vectorEntries);
                })
                .flatMap(uni -> uni);

        // Return combined result - both operations should complete
        return Uni.combine().all().unis(luceneIndexing, vectorIndexing)
                .discardItems()
                .onItem().transformToUni(x -> Uni.createFrom().voidItem());
    }

    @Override
    public Uni<Integer> removeChunksForFile(String filePath) {
        LOG.debugf("Removing chunks for file: %s from hybrid index", filePath);

        // Remove from Lucene
        Uni<Integer> luceneRemoval = luceneIndexService.removeChunksForFile(filePath);

        // Remove from vector store (need to find vector IDs for this file)
        Uni<Integer> vectorRemoval = vectorStore.search(new float[384], Integer.MAX_VALUE) // Get all vectors
                .map(results -> {
                    List<String> idsToRemove = results.stream()
                            .filter(result -> filePath.equals(result.metadata().sourceFile()))
                            .map(VectorStore.SearchResult::id)
                            .collect(Collectors.toList());

                    if (idsToRemove.isEmpty()) {
                        return Uni.createFrom().item(0);
                    }

                    return vectorStore.deleteBatch(idsToRemove);
                })
                .flatMap(uni -> uni);

        // Return the Lucene removal count (vector removal is cleanup)
        return luceneRemoval;
    }

    @Override
    public Uni<Void> updateChunksForFile(String filePath, List<TextChunk> newChunks) {
        LOG.debugf("Updating chunks for file: %s in hybrid index", filePath);

        // Remove old chunks first
        Uni<Integer> removal = removeChunksForFile(filePath);

        // Add new chunks
        Uni<Void> addition = addChunks(newChunks);

        // Combine operations
        return Uni.combine().all().unis(removal, addition)
                .discardItems()
                .onItem().transformToUni(x -> Uni.createFrom().voidItem());
    }

    /**
     * Generates a unique ID for a vector based on the chunk's properties.
     * This ensures consistency between Lucene and vector storage.
     */
    private String generateVectorId(TextChunk chunk) {
        // Create a unique ID based on file path and location
        return String.format("%s:%d:%d:%d",
            chunk.sourceFile(),
            chunk.startLine(),
            chunk.startByte(),
            chunk.endByte()
        );
    }

    /**
     * Performs hybrid search combining keyword and semantic similarity.
     * This is an extension method beyond the basic IndexService interface.
     */
    public Uni<HybridSearchResult> hybridSearch(String query, int limit) {
        LOG.debugf("Performing hybrid search for query: %s", query);

        // For now, implement semantic search using the query as text to embed
        return embeddingService.generateEmbedding(createQueryChunk(query))
                .map(embeddingResult -> {
                    if (!embeddingResult.isSuccess()) {
                        LOG.warn("Failed to generate embedding for query, returning empty results");
                        return new HybridSearchResult(List.of(), List.of());
                    }

                    // Search vectors
                    List<VectorStore.SearchResult> vectorResults = vectorStore.search(
                        embeddingResult.getEmbedding(), limit)
                        .await().indefinitely();

                    // TODO: Also search Lucene for keyword matches
                    // For now, return only vector results
                    return new HybridSearchResult(vectorResults, List.of());
                });
    }

    private TextChunk createQueryChunk(String query) {
        return new TextChunk(
            query,
            "text", // Generic language for queries
            "query",
            "search",
            "query",
            1, 1, 0, query.length(),
            Map.of()
        );
    }

    /**
     * Result of a hybrid search operation.
     */
    public static class HybridSearchResult {
        private final List<VectorStore.SearchResult> vectorResults;
        private final List<?> keywordResults; // TODO: Define keyword result type

        public HybridSearchResult(List<VectorStore.SearchResult> vectorResults, List<?> keywordResults) {
            this.vectorResults = vectorResults;
            this.keywordResults = keywordResults;
        }

        public List<VectorStore.SearchResult> getVectorResults() {
            return vectorResults;
        }

        public List<?> getKeywordResults() {
            return keywordResults;
        }
    }
}