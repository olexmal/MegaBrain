/*
 * Copyright (c) 2025 MegaBrain Contributors
 * Licensed under the MIT License - see LICENSE file for details.
 */

package io.megabrain.core;

import io.megabrain.ingestion.parser.TextChunk;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Qualifier;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.List;

/**
 * CDI qualifier for IndexService implementations.
 */
@Qualifier
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.METHOD, ElementType.TYPE})
@interface IndexType {
    Type value();

    enum Type {
        MEMORY, LUCENE
    }
}

/**
 * Service for indexing and managing TextChunks in the search index.
 */
public interface IndexService {

    /**
     * Adds chunks to the index.
     *
     * @param chunks the text chunks to add
     * @return a Uni that completes when indexing is done
     */
    Uni<Void> addChunks(List<TextChunk> chunks);

    /**
     * Removes chunks for a specific file from the index.
     *
     * @param filePath the file path whose chunks should be removed
     * @return a Uni that emits the number of chunks removed
     */
    Uni<Integer> removeChunksForFile(String filePath);

    /**
     * Updates chunks for a specific file (removes old, adds new).
     *
     * @param filePath the file path
     * @param newChunks the new chunks for this file
     * @return a Uni that completes when update is done
     */
    Uni<Void> updateChunksForFile(String filePath, List<TextChunk> newChunks);
}
