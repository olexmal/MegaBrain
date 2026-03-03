/*
 * Copyright (c) 2025 MegaBrain Contributors
 * Licensed under the MIT License - see LICENSE file for details.
 */

package io.megabrain.core.prompt;

import io.megabrain.api.LineRange;
import io.megabrain.api.SearchResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChunkSelectorImplTest {

    @Mock
    private TokenCounter tokenCounter;

    @Mock
    private ContextFormatter contextFormatter;

    private ChunkSelectorImpl chunkSelector;

    @BeforeEach
    void setUp() {
        chunkSelector = new ChunkSelectorImpl(tokenCounter, contextFormatter);
    }

    private SearchResult createChunk(String file, float score) {
        return SearchResult.create(
                "content for " + file,
                "Entity",
                "method",
                file,
                "java",
                "repo",
                score,
                new LineRange(1, 10)
        );
    }

    @Test
    void shouldReturnEmptyListWhenInputIsEmpty() {
        List<SearchResult> result = chunkSelector.selectChunks(null, "system", "question", "gpt-4");
        assertThat(result).isEmpty();

        result = chunkSelector.selectChunks(new ArrayList<>(), "system", "question", "gpt-4");
        assertThat(result).isEmpty();
    }

    @Test
    void shouldSelectChunksWithinBudget() {
        // Setup budget: max=1000, reserved=200, system=100, question=100 -> available=600
        when(tokenCounter.getMaxContextWindow("gpt-4")).thenReturn(1000);
        when(tokenCounter.estimateTokens("system")).thenReturn(100);
        when(tokenCounter.estimateTokens("question")).thenReturn(100);

        // Formatted chunks cost 250 tokens each
        when(contextFormatter.format(any(SearchResult.class))).thenReturn("formatted_chunk");
        when(tokenCounter.estimateTokens("formatted_chunk")).thenReturn(250);

        List<SearchResult> chunks = List.of(
                createChunk("file1.java", 0.9f),
                createChunk("file2.java", 0.8f),
                createChunk("file3.java", 0.7f)
        );

        List<SearchResult> selected = chunkSelector.selectChunks(chunks, "system", "question", "gpt-4");

        // Can fit 2 chunks (250 * 2 = 500 <= 600)
        assertThat(selected).hasSize(2);
        assertThat(selected.get(0).getSourceFile()).isEqualTo("file1.java");
        assertThat(selected.get(1).getSourceFile()).isEqualTo("file2.java");
    }

    @Test
    void shouldPrioritizeDiversityOverScore() {
        when(tokenCounter.getMaxContextWindow("gpt-4")).thenReturn(1000);
        when(tokenCounter.estimateTokens("system")).thenReturn(100);
        when(tokenCounter.estimateTokens("question")).thenReturn(100);
        
        when(contextFormatter.format(any(SearchResult.class))).thenReturn("formatted_chunk");
        when(tokenCounter.estimateTokens("formatted_chunk")).thenReturn(250);

        List<SearchResult> chunks = List.of(
                createChunk("file1.java", 0.9f),
                createChunk("file1.java", 0.85f), // duplicate file, high score
                createChunk("file2.java", 0.8f)   // unique file, lower score
        );

        List<SearchResult> selected = chunkSelector.selectChunks(chunks, "system", "question", "gpt-4");

        // Can fit 2 chunks. Should pick file1 (0.9) and file2 (0.8) due to diversity logic
        assertThat(selected).hasSize(2);
        assertThat(selected.get(0).getSourceFile()).isEqualTo("file1.java");
        assertThat(selected.get(0).getScore()).isEqualTo(0.9f);
        assertThat(selected.get(1).getSourceFile()).isEqualTo("file2.java");
    }

    @Test
    void shouldIncludeDuplicatesIfBudgetAllows() {
        when(tokenCounter.getMaxContextWindow("gpt-4")).thenReturn(1000);
        when(tokenCounter.estimateTokens("system")).thenReturn(50);
        when(tokenCounter.estimateTokens("question")).thenReturn(50);
        // available = 1000 - 200 - 50 - 50 = 700

        when(contextFormatter.format(any(SearchResult.class))).thenReturn("formatted_chunk");
        when(tokenCounter.estimateTokens("formatted_chunk")).thenReturn(200);

        List<SearchResult> chunks = List.of(
                createChunk("file1.java", 0.9f),
                createChunk("file1.java", 0.85f),
                createChunk("file2.java", 0.8f)
        );

        List<SearchResult> selected = chunkSelector.selectChunks(chunks, "system", "question", "gpt-4");

        // Can fit 3 chunks (3 * 200 = 600 <= 700)
        assertThat(selected).hasSize(3);
        // Order: file1 (0.9), file2 (0.8), then duplicate file1 (0.85)
        assertThat(selected.get(0).getSourceFile()).isEqualTo("file1.java");
        assertThat(selected.get(0).getScore()).isEqualTo(0.9f);
        assertThat(selected.get(1).getSourceFile()).isEqualTo("file2.java");
        assertThat(selected.get(2).getSourceFile()).isEqualTo("file1.java");
        assertThat(selected.get(2).getScore()).isEqualTo(0.85f);
    }

    @Test
    void shouldReturnEmptyIfNoTokensAvailable() {
        when(tokenCounter.getMaxContextWindow("gpt-4")).thenReturn(100);
        when(tokenCounter.estimateTokens("system")).thenReturn(50);
        when(tokenCounter.estimateTokens("question")).thenReturn(50);
        // available = 100 - 20 - 50 - 50 = -20 (<=0)

        List<SearchResult> chunks = List.of(createChunk("file1.java", 0.9f));
        List<SearchResult> selected = chunkSelector.selectChunks(chunks, "system", "question", "gpt-4");

        assertThat(selected).isEmpty();
    }
}