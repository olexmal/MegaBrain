/*
 * Copyright (c) 2025 MegaBrain Contributors
 * Licensed under the MIT License - see LICENSE file for details.
 */

package io.megabrain.core.prompt;

import io.megabrain.api.SearchResult;
import java.util.List;

/**
 * Service for selecting code chunks based on relevance, token budget, and diversity.
 */
public interface ChunkSelector {

    /**
     * Selects chunks based on relevance score, respecting the token budget, and ensuring diversity.
     * 
     * @param chunks The available chunks to select from
     * @param systemPrompt The system prompt to reserve tokens for
     * @param userQuestion The user question to reserve tokens for
     * @param modelName The model name to determine the max context window
     * @return The selected chunks that fit within the remaining token budget
     */
    List<SearchResult> selectChunks(List<SearchResult> chunks, String systemPrompt, String userQuestion, String modelName);
}