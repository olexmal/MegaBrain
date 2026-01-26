/*
 * Copyright (c) 2025 MegaBrain Contributors
 * Licensed under the MIT License - see LICENSE file for details.
 */

package io.megabrain.core;

import java.util.List;

/**
 * Immutable filter criteria for search operations (US-02-04, T2).
 * <p>
 * Supports filtering by language, repository, file path prefix, and entity type.
 * Multiple values per dimension use OR logic; dimensions are combined with AND.
 * Used by Lucene filter queries and passed from the search API to the index service.
 */
public record SearchFilters(
        List<String> languages,
        List<String> repositories,
        List<String> filePaths,
        List<String> entityTypes
) {

    public SearchFilters {
        languages = languages != null ? List.copyOf(languages) : List.of();
        repositories = repositories != null ? List.copyOf(repositories) : List.of();
        filePaths = filePaths != null ? List.copyOf(filePaths) : List.of();
        entityTypes = entityTypes != null ? List.copyOf(entityTypes) : List.of();
    }

    /**
     * Returns empty filters (no filtering).
     *
     * @return SearchFilters with no filter values
     */
    public static SearchFilters empty() {
        return new SearchFilters(List.of(), List.of(), List.of(), List.of());
    }

    /**
     * Checks whether any filter dimension has values.
     *
     * @return true if at least one filter is set
     */
    public boolean hasFilters() {
        return !languages.isEmpty() || !repositories.isEmpty()
                || !filePaths.isEmpty() || !entityTypes.isEmpty();
    }
}
