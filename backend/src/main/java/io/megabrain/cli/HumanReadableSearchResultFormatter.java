/*
 * Copyright (c) 2025 MegaBrain Contributors
 * Licensed under the MIT License - see LICENSE file for details.
 */

package io.megabrain.cli;

import io.megabrain.api.SearchResponse;
import io.megabrain.api.SearchResult;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;

/**
 * Human-readable formatter for CLI search output.
 * Format per result: File, Entity, Score, snippet, separator. Truncates long snippets.
 * Does not reuse ContextFormatter (that is for LLM prompts).
 */
@ApplicationScoped
public class HumanReadableSearchResultFormatter implements SearchResultFormatter {

    /** Maximum number of lines to show in a snippet. */
    public static final int MAX_SNIPPET_LINES = 15;
    /** Maximum characters per line before truncation. */
    public static final int MAX_LINE_LENGTH = 120;
    /** Separator between results. */
    public static final String RESULT_SEPARATOR = "---";

    private static final String PLACEHOLDER_PATH = "(no path)";
    private static final String PLACEHOLDER_ENTITY = "(no entity)";
    private static final String NO_RESULTS = "No results.";

    @Override
    public String format(SearchResponse response) {
        if (response == null || response.getResults() == null || response.getResults().isEmpty()) {
            return NO_RESULTS;
        }
        StringBuilder sb = new StringBuilder();
        appendHeader(response, sb);
        List<SearchResult> results = response.getResults();
        for (int i = 0; i < results.size(); i++) {
            appendResult(results.get(i), sb);
            sb.append(RESULT_SEPARATOR);
            if (i < results.size() - 1) {
                sb.append('\n');
            }
        }
        return sb.toString();
    }

    @Override
    public String formatQuiet(SearchResponse response) {
        if (response == null || response.getResults() == null || response.getResults().isEmpty()) {
            return NO_RESULTS;
        }
        StringBuilder sb = new StringBuilder();
        for (SearchResult r : response.getResults()) {
            String path = nullToEmpty(r.getSourceFile(), PLACEHOLDER_PATH);
            String entity = nullToEmpty(r.getEntityName(), PLACEHOLDER_ENTITY);
            sb.append(path).append('\t').append(entity).append('\n');
        }
        return sb.toString();
    }

    private void appendHeader(SearchResponse response, StringBuilder sb) {
        String query = response.getQuery();
        if (query != null && !query.isBlank()) {
            sb.append("Query: ").append(query.trim()).append('\n');
            sb.append("Total: ").append(response.getTotal()).append(" | Took: ").append(response.getTookMs()).append(" ms\n\n");
        }
    }

    private void appendResult(SearchResult r, StringBuilder sb) {
        String path = nullToEmpty(r.getSourceFile(), PLACEHOLDER_PATH);
        String entity = nullToEmpty(r.getEntityName(), PLACEHOLDER_ENTITY);
        float score = r.getScore();
        String snippet = truncateSnippet(nullToEmpty(r.getContent(), ""));

        sb.append("File: ").append(path).append('\n');
        sb.append("Entity: ").append(entity).append('\n');
        sb.append("Score: ").append(score).append('\n');
        sb.append('\n');
        if (!snippet.isEmpty()) {
            sb.append(snippet).append('\n');
        }
        sb.append('\n');
    }

    private String truncateSnippet(String content) {
        if (content == null || content.isBlank()) {
            return "";
        }
        String[] lines = content.split("\n", -1);
        StringBuilder sb = new StringBuilder();
        int lineCount = 0;
        for (String line : lines) {
            if (lineCount >= MAX_SNIPPET_LINES) {
                sb.append("... (truncated)\n");
                break;
            }
            String trimmed = line.length() > MAX_LINE_LENGTH
                    ? line.substring(0, MAX_LINE_LENGTH) + "..."
                    : line;
            sb.append(trimmed).append('\n');
            lineCount++;
        }
        return sb.toString().trim();
    }

    private static String nullToEmpty(String value, String placeholder) {
        if (value == null || value.isBlank()) {
            return placeholder != null ? placeholder : "";
        }
        return value.trim();
    }
}
