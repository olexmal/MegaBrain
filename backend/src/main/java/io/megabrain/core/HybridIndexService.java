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
@Priority(1)
@Alternative
@IndexType(IndexType.Type.HYBRID)
@ApplicationScoped
public class HybridIndexService implements IndexService {

    private static final Logger LOG = Logger.getLogger(HybridIndexService.class);

    @Inject
    @IndexType(IndexType.Type.LUCENE)
    IndexService luceneIndexService;

    @Inject
    @IndexType(IndexType.Type.LUCENE)
    LuceneIndexService luceneService;

    @Inject
    EmbeddingService embeddingService;

    @Inject
    PgVectorStore vectorStore;

    @Inject
    ResultMerger resultMerger;

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
     * Performs search with configurable mode (hybrid/keyword/vector) for hybrid ranking (US-02-03, T6).
     * No metadata filters are applied. Use {@link #search(String, int, SearchMode, SearchFilters)} for filtered search.
     *
     * @param query the search query string
     * @param limit maximum number of results to return
     * @param mode search mode (HYBRID, KEYWORD, or VECTOR)
     * @return merged search results sorted by combined score
     */
    public Uni<List<ResultMerger.MergedResult>> search(String query, int limit, SearchMode mode) {
        return search(query, limit, mode, null);
    }

    /**
     * Performs search with configurable mode and optional metadata filters (US-02-04, T2).
     * <p>
     * Supports three modes:
     * <ul>
     *   <li>{@link SearchMode#HYBRID}: Executes both Lucene and vector searches, merges results</li>
     *   <li>{@link SearchMode#KEYWORD}: Executes only Lucene search, skips vector search</li>
     *   <li>{@link SearchMode#VECTOR}: Executes only vector search, skips Lucene search</li>
     * </ul>
     * <p>
     * When filters are present, Lucene search applies them (language, repository, file_path, entity_type)
     * before scoring. Vector search does not apply filters.
     * <p>
     * Results are normalized and merged (for HYBRID mode) using {@link ResultMerger}.
     *
     * @param query the search query string
     * @param limit maximum number of results to return
     * @param mode search mode (HYBRID, KEYWORD, or VECTOR)
     * @param filters optional metadata filters; null or empty to skip
     * @return merged search results sorted by combined score
     */
    public Uni<List<ResultMerger.MergedResult>> search(String query, int limit, SearchMode mode, SearchFilters filters) {
        LOG.debugf("Performing %s search for query: %s%s", mode, query,
                filters != null && filters.hasFilters() ? " (with filters)" : "");

        if (mode == null) {
            mode = SearchMode.HYBRID; // Default to hybrid
        }

        // Handle zero or negative limit
        if (limit <= 0) {
            return Uni.createFrom().item(List.<ResultMerger.MergedResult>of());
        }

        boolean performLucene = (mode == SearchMode.HYBRID || mode == SearchMode.KEYWORD);
        boolean performVector = (mode == SearchMode.HYBRID || mode == SearchMode.VECTOR);

        // Execute Lucene search if needed (with filters when present)
        Uni<List<LuceneIndexService.LuceneScoredResult>> luceneUni;
        if (performLucene) {
            luceneUni = luceneService.searchWithScores(query, limit, filters)
                    .map(LuceneIndexService::normalizeScores);
        } else {
            luceneUni = Uni.createFrom().item(List.<LuceneIndexService.LuceneScoredResult>of());
        }

        // Execute vector search if needed
        Uni<List<VectorStore.SearchResult>> vectorUni;
        if (performVector) {
            vectorUni = embeddingService.generateEmbedding(createQueryChunk(query))
                    .flatMap(embeddingResult -> {
                        if (!embeddingResult.isSuccess()) {
                            LOG.warn("Failed to generate embedding for query, returning empty vector results");
                            return Uni.createFrom().item(List.<VectorStore.SearchResult>of());
                        }
                        return vectorStore.search(embeddingResult.getEmbedding(), limit)
                                .map(VectorScoreNormalizer::normalizeScores);
                    });
        } else {
            vectorUni = Uni.createFrom().item(List.<VectorStore.SearchResult>of());
        }

        // Combine both searches and merge results
        return Uni.combine().all().unis(luceneUni, vectorUni)
                .asTuple()
                .map(tuple -> {
                    List<LuceneIndexService.LuceneScoredResult> luceneResults = tuple.getItem1();
                    List<VectorStore.SearchResult> vectorResults = tuple.getItem2();
                    return resultMerger.merge(luceneResults, vectorResults);
                });
    }

    /**
     * Performs hybrid search combining keyword and semantic similarity.
     * This is an extension method beyond the basic IndexService interface.
     * <p>
     * This method is deprecated in favor of {@link #search(String, int, SearchMode)}.
     * It defaults to {@link SearchMode#HYBRID} mode.
     *
     * @deprecated Use {@link #search(String, int, SearchMode)} instead
     */
    @Deprecated
    public Uni<HybridSearchResult> hybridSearch(String query, int limit) {
        LOG.debugf("Performing hybrid search for query: %s", query);

        // For now, implement semantic search using the query as text to embed
        return embeddingService.generateEmbedding(createQueryChunk(query))
                .flatMap(embeddingResult -> {
                    if (!embeddingResult.isSuccess()) {
                        LOG.warn("Failed to generate embedding for query, returning empty results");
                        return Uni.createFrom().item(new HybridSearchResult(List.of(), List.of()));
                    }

                    // Search vectors
                    return vectorStore.search(embeddingResult.getEmbedding(), limit)
                            .map(vectorResults -> {
                                // TODO: Also search Lucene for keyword matches
                                // For now, return only vector results
                                return new HybridSearchResult(vectorResults, List.of());
                            });
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