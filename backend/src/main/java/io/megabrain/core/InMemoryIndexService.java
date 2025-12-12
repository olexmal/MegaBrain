/*
 * Copyright (c) 2025 MegaBrain Contributors
 * Licensed under the MIT License - see LICENSE file for details.
 */

package io.megabrain.core;

import io.megabrain.ingestion.parser.TextChunk;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Simple in-memory implementation of IndexService for development and testing.
 * This will be replaced with a proper Lucene implementation later.
 */
@ApplicationScoped
public class InMemoryIndexService implements IndexService {

    private static final Logger LOG = Logger.getLogger(InMemoryIndexService.class);

    // Map from file path to list of chunks for that file
    private final Map<String, List<TextChunk>> index = new ConcurrentHashMap<>();

    @Override
    public Uni<Void> addChunks(List<TextChunk> chunks) {
        return Uni.createFrom().item(() -> {
            if (chunks == null || chunks.isEmpty()) {
                return null;
            }

            // Group chunks by file
            Map<String, List<TextChunk>> chunksByFile = chunks.stream()
                    .collect(Collectors.groupingBy(TextChunk::sourceFile));

            for (Map.Entry<String, List<TextChunk>> entry : chunksByFile.entrySet()) {
                String filePath = entry.getKey();
                List<TextChunk> fileChunks = entry.getValue();

                // For now, just replace all chunks for the file
                // In a real implementation, we'd merge or handle conflicts
                index.put(filePath, List.copyOf(fileChunks));
                LOG.debugf("Indexed %d chunks for file: %s", fileChunks.size(), filePath);
            }

            return null;
        });
    }

    @Override
    public Uni<Integer> removeChunksForFile(String filePath) {
        return Uni.createFrom().item(() -> {
            List<TextChunk> removed = index.remove(filePath);
            int count = removed != null ? removed.size() : 0;
            if (count > 0) {
                LOG.debugf("Removed %d chunks for file: %s", count, filePath);
            }
            return count;
        });
    }

    @Override
    public Uni<Void> updateChunksForFile(String filePath, List<TextChunk> newChunks) {
        return removeChunksForFile(filePath)
                .flatMap(removedCount -> addChunks(newChunks));
    }

    /**
     * Gets all indexed chunks (for testing purposes).
     */
    public List<TextChunk> getAllChunks() {
        return index.values().stream()
                .flatMap(List::stream)
                .collect(Collectors.toList());
    }

    /**
     * Gets chunks for a specific file (for testing purposes).
     */
    public List<TextChunk> getChunksForFile(String filePath) {
        return index.getOrDefault(filePath, List.of());
    }

    /**
     * Clears the entire index (for testing purposes).
     */
    public void clear() {
        index.clear();
        LOG.debug("Index cleared");
    }
}
