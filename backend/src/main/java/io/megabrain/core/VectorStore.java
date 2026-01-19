/*
 * Copyright (c) 2025 MegaBrain Contributors
 * Licensed under the MIT License - see LICENSE file for details.
 */

package io.megabrain.core;

import io.smallrye.mutiny.Uni;

import java.util.List;

/**
 * Backend-agnostic interface for vector storage and similarity search operations.
 * This interface abstracts vector storage operations to allow switching between
 * different backends like pgvector, Milvus, Pinecone, etc.
 */
public interface VectorStore {

    /**
     * Stores a single vector with associated metadata.
     *
     * @param id unique identifier for the vector
     * @param vector the embedding vector
     * @param metadata additional metadata associated with the vector
     * @return a Uni that completes when the vector is stored
     */
    Uni<Void> store(String id, float[] vector, VectorMetadata metadata);

    /**
     * Stores multiple vectors in batch for efficiency.
     *
     * @param vectors list of vectors to store with their metadata
     * @return a Uni that completes when all vectors are stored
     */
    Uni<Void> storeBatch(List<VectorEntry> vectors);

    /**
     * Finds the k most similar vectors to the given query vector.
     *
     * @param queryVector the query embedding vector
     * @param k maximum number of similar vectors to return
     * @return a Uni that emits the search results ordered by similarity (highest first)
     */
    Uni<List<SearchResult>> search(float[] queryVector, int k);

    /**
     * Finds similar vectors within a similarity threshold.
     *
     * @param queryVector the query embedding vector
     * @param k maximum number of similar vectors to return
     * @param similarityThreshold minimum similarity score (0.0 to 1.0)
     * @return a Uni that emits the search results ordered by similarity (highest first)
     */
    Uni<List<SearchResult>> search(float[] queryVector, int k, double similarityThreshold);

    /**
     * Deletes a vector by its ID.
     *
     * @param id the vector ID to delete
     * @return a Uni that emits true if the vector was deleted, false if it didn't exist
     */
    Uni<Boolean> delete(String id);

    /**
     * Deletes multiple vectors by their IDs.
     *
     * @param ids the vector IDs to delete
     * @return a Uni that emits the number of vectors deleted
     */
    Uni<Integer> deleteBatch(List<String> ids);

    /**
     * Gets the expected dimension of vectors in this store.
     * All vectors stored must have this exact dimension.
     *
     * @return the vector dimension
     */
    int getVectorDimension();

    /**
     * Checks if the vector store is healthy and ready for operations.
     *
     * @return a Uni that emits true if healthy, false otherwise
     */
    Uni<Boolean> healthCheck();

    /**
     * Gets statistics about the vector store.
     *
     * @return a Uni that emits store statistics
     */
    Uni<VectorStoreStats> getStats();

    /**
     * Metadata associated with a stored vector.
     */
    record VectorMetadata(
            String content,
            String language,
            String entityType,
            String entityName,
            String sourceFile,
            int startLine,
            int endLine,
            int startByte,
            int endByte
    ) {}

    /**
     * A vector entry for batch operations.
     */
    record VectorEntry(
            String id,
            float[] vector,
            VectorMetadata metadata
    ) {}

    /**
     * Result of a similarity search operation.
     */
    record SearchResult(
            String id,
            float[] vector,
            VectorMetadata metadata,
            double similarity
    ) {}

    /**
     * Statistics about the vector store.
     */
    record VectorStoreStats(
            long totalVectors,
            int vectorDimension,
            String backendType,
            long indexSizeBytes
    ) {}
}