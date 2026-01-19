/*
 * Copyright (c) 2025 MegaBrain Contributors
 * Licensed under the MIT License - see LICENSE file for details.
 */

package io.megabrain.core;

import io.megabrain.ingestion.parser.TextChunk;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

/**
 * Service for generating embeddings from code chunks.
 * Handles text preprocessing, batch processing, and error handling.
 */
@ApplicationScoped
public class EmbeddingService {

    private static final Logger LOG = Logger.getLogger(EmbeddingService.class);

    @ConfigProperty(name = "megabrain.embedding.max-tokens", defaultValue = "512")
    int maxTokens;

    @ConfigProperty(name = "megabrain.embedding.batch-size", defaultValue = "32")
    int batchSize;

    @Inject
    EmbeddingModelService embeddingModel;

    private Executor executor;

    @PostConstruct
    void init() {
        // Use Quarkus's default worker executor
        this.executor = Infrastructure.getDefaultExecutor();
        LOG.infof("Initialized EmbeddingService with maxTokens=%d, batchSize=%d", maxTokens, batchSize);
    }

    @PreDestroy
    void destroy() {
        LOG.info("Shutting down EmbeddingService");
    }

    /**
     * Generates embeddings for a list of text chunks.
     * Handles preprocessing, batching, and error recovery.
     *
     * @param chunks the text chunks to embed
     * @return Uni that emits the list of embedding results
     */
    public Uni<List<EmbeddingResult>> generateEmbeddings(List<TextChunk> chunks) {
        return Uni.createFrom().completionStage(
            CompletableFuture.supplyAsync(() -> {
                if (chunks == null || chunks.isEmpty()) {
                    return List.<EmbeddingResult>of();
                }

                LOG.debugf("Generating embeddings for %d chunks", chunks.size());

                // Process in batches for efficiency
                List<List<TextChunk>> batches = partitionIntoBatches(chunks, batchSize);
                List<EmbeddingResult> allResults = new java.util.ArrayList<>();

                for (List<TextChunk> batch : batches) {
                    try {
                        List<EmbeddingResult> batchResults = processBatch(batch);
                        allResults.addAll(batchResults);
                    } catch (Exception e) {
                        LOG.errorf(e, "Failed to process batch of %d chunks", batch.size());
                        // Add error results for failed batch
                        for (TextChunk chunk : batch) {
                            allResults.add(EmbeddingResult.error(chunk, e));
                        }
                    }
                }

                LOG.debugf("Generated embeddings for %d chunks (%d successful, %d failed)",
                    chunks.size(),
                    allResults.stream().filter(EmbeddingResult::isSuccess).count(),
                    allResults.stream().filter(r -> !r.isSuccess()).count());

                return allResults;
            }, executor)
        );
    }

    /**
     * Generates embeddings for a single text chunk.
     *
     * @param chunk the text chunk to embed
     * @return Uni that emits the embedding result
     */
    public Uni<EmbeddingResult> generateEmbedding(TextChunk chunk) {
        return generateEmbeddings(List.of(chunk))
                .map(results -> results.isEmpty() ? EmbeddingResult.error(chunk, new IllegalStateException("No result")) : results.get(0));
    }

    /**
     * Processes a batch of text chunks and generates embeddings.
     */
    private List<EmbeddingResult> processBatch(List<TextChunk> batch) {
        List<String> preprocessedTexts = batch.stream()
                .map(this::preprocessText)
                .collect(Collectors.toList());

        try {
            List<float[]> embeddings = embeddingModel.embed(preprocessedTexts);

            List<EmbeddingResult> results = new java.util.ArrayList<>();
            for (int i = 0; i < batch.size(); i++) {
                TextChunk chunk = batch.get(i);
                if (i < embeddings.size()) {
                    results.add(EmbeddingResult.success(chunk, embeddings.get(i)));
                } else {
                    results.add(EmbeddingResult.error(chunk, new IllegalStateException("Missing embedding for chunk")));
                }
            }

            return results;

        } catch (Exception e) {
            LOG.errorf(e, "Failed to generate embeddings for batch");
            return batch.stream()
                    .map(chunk -> EmbeddingResult.error(chunk, e))
                    .collect(Collectors.toList());
        }
    }

    /**
     * Preprocesses text from a code chunk for embedding generation.
     * Handles token limits, normalization, and code-specific cleaning.
     */
    private String preprocessText(TextChunk chunk) {
        String text = chunk.content();

        if (text == null || text.trim().isEmpty()) {
            return "";
        }

        // Basic preprocessing for code
        text = text.trim();

        // Remove excessive whitespace but preserve structure
        text = text.replaceAll("\\s+", " ");

        // Truncate if too long (rough approximation: ~4 chars per token)
        int maxChars = maxTokens * 4;
        if (text.length() > maxChars) {
            text = text.substring(0, maxChars);
            // Try to cut at word boundary
            int lastSpace = text.lastIndexOf(' ');
            if (lastSpace > maxChars * 0.8) {
                text = text.substring(0, lastSpace);
            }
        }

        // Add language context if helpful
        String language = chunk.language();
        if (language != null && !language.isEmpty()) {
            text = language + ": " + text;
        }

        return text;
    }

    /**
     * Partitions a list into batches of the specified size.
     */
    private <T> List<List<T>> partitionIntoBatches(List<T> list, int batchSize) {
        List<List<T>> batches = new java.util.ArrayList<>();
        for (int i = 0; i < list.size(); i += batchSize) {
            int endIndex = Math.min(i + batchSize, list.size());
            batches.add(list.subList(i, endIndex));
        }
        return batches;
    }

    /**
     * Result of an embedding generation operation.
     */
    public static class EmbeddingResult {
        private final TextChunk chunk;
        private final float[] embedding;
        private final Exception error;
        private final boolean success;

        private EmbeddingResult(TextChunk chunk, float[] embedding, Exception error, boolean success) {
            this.chunk = chunk;
            this.embedding = embedding;
            this.error = error;
            this.success = success;
        }

        public static EmbeddingResult success(TextChunk chunk, float[] embedding) {
            return new EmbeddingResult(chunk, embedding, null, true);
        }

        public static EmbeddingResult error(TextChunk chunk, Exception error) {
            return new EmbeddingResult(chunk, null, error, false);
        }

        public TextChunk getChunk() {
            return chunk;
        }

        public float[] getEmbedding() {
            return embedding;
        }

        public Exception getError() {
            return error;
        }

        public boolean isSuccess() {
            return success;
        }

        public String getErrorMessage() {
            return error != null ? error.getMessage() : null;
        }
    }
}