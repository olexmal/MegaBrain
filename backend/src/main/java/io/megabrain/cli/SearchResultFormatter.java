/*
 * Copyright (c) 2025 MegaBrain Contributors
 * Licensed under the MIT License - see LICENSE file for details.
 */

package io.megabrain.cli;

import io.megabrain.api.SearchResponse;

/**
 * Formats search results for terminal or other text output.
 * Implementations produce human-readable or minimal (quiet) layout.
 */
public interface SearchResultFormatter {

    /**
     * Formats the full search response (default layout).
     *
     * @param response the search response to format
     * @return formatted string for terminal output
     */
    String format(SearchResponse response);

    /**
     * Formats the search response, optionally in quiet (minimal) mode.
     *
     * @param response the search response to format
     * @param quiet when true, output is minimal (e.g. one line per result: path + entity)
     * @return formatted string for terminal output
     */
    default String format(SearchResponse response, boolean quiet) {
        if (quiet) {
            return formatQuiet(response);
        }
        return format(response);
    }

    /**
     * Minimal format: one line per result (path + entity), pipe-friendly.
     *
     * @param response the search response
     * @return minimal formatted string
     */
    String formatQuiet(SearchResponse response);
}
