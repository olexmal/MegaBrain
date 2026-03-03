/*
 * Copyright (c) 2025 MegaBrain Contributors
 * Licensed under the MIT License - see LICENSE file for details.
 */

package io.megabrain.core.prompt;

import io.megabrain.api.SearchResult;
import java.util.List;

/**
 * Formats code search results into readable context chunks for LLM prompts.
 */
public interface ContextFormatter {

    /**
     * Formats a single search result into a readable context chunk.
     * Includes source file path, entity name, line numbers, and code content.
     *
     * @param result the search result to format
     * @return a formatted string representation of the chunk
     */
    String format(SearchResult result);

    /**
     * Formats multiple search results into a combined readable context.
     *
     * @param results the list of search results to format
     * @return a formatted string representation of all chunks, separated by newlines
     */
    String formatAll(List<SearchResult> results);
}
