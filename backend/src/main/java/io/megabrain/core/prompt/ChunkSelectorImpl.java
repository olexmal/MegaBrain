/*
 * Copyright (c) 2025 MegaBrain Contributors
 * Licensed under the MIT License - see LICENSE file for details.
 */

package io.megabrain.core.prompt;

import io.megabrain.api.SearchResult;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@ApplicationScoped
public class ChunkSelectorImpl implements ChunkSelector {

    private final TokenCounter tokenCounter;
    private final ContextFormatter contextFormatter;

    @Inject
    public ChunkSelectorImpl(TokenCounter tokenCounter, ContextFormatter contextFormatter) {
        this.tokenCounter = tokenCounter;
        this.contextFormatter = contextFormatter;
    }

    @Override
    public List<SearchResult> selectChunks(List<SearchResult> chunks, String systemPrompt, String userQuestion, String modelName) {
        if (chunks == null || chunks.isEmpty()) {
            return new ArrayList<>();
        }

        int maxTokens = tokenCounter.getMaxContextWindow(modelName);
        int reservedForAnswer = (int) (maxTokens * 0.2); // Reserve 20% for the generated answer and overhead
        int systemTokens = tokenCounter.estimateTokens(systemPrompt != null ? systemPrompt : "");
        int questionTokens = tokenCounter.estimateTokens(userQuestion != null ? userQuestion : "");
        
        int availableTokens = maxTokens - reservedForAnswer - systemTokens - questionTokens;
        
        if (availableTokens <= 0) {
            return new ArrayList<>(); // No tokens available for chunks
        }

        // Sort chunks by score descending
        List<SearchResult> sortedChunks = new ArrayList<>(chunks);
        sortedChunks.sort((a, b) -> Float.compare(b.getScore(), a.getScore()));

        List<SearchResult> selected = new ArrayList<>();
        List<SearchResult> duplicates = new ArrayList<>();
        Set<String> seenFiles = new HashSet<>();

        // First pass: select unique files
        for (SearchResult chunk : sortedChunks) {
            String sourceFile = chunk.getSourceFile();
            if (seenFiles.contains(sourceFile)) {
                duplicates.add(chunk);
                continue;
            }

            int chunkTokens = estimateChunkTokens(chunk);
            if (chunkTokens <= availableTokens) {
                selected.add(chunk);
                availableTokens -= chunkTokens;
                seenFiles.add(sourceFile);
            }
        }

        // Second pass: if we still have tokens, include duplicates
        for (SearchResult chunk : duplicates) {
            if (availableTokens <= 0) {
                break;
            }
            int chunkTokens = estimateChunkTokens(chunk);
            if (chunkTokens <= availableTokens) {
                selected.add(chunk);
                availableTokens -= chunkTokens;
            }
        }

        return selected;
    }

    private int estimateChunkTokens(SearchResult chunk) {
        String formattedChunk = contextFormatter.format(chunk);
        // Estimate token cost for the formatted chunk representation
        return tokenCounter.estimateTokens(formattedChunk);
    }
}