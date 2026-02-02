/*
 * Copyright (c) 2025 MegaBrain Contributors
 * Licensed under the MIT License - see LICENSE file for details.
 */

package io.megabrain.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;

import java.util.ArrayList;
import java.util.List;

/**
 * Request DTO for search operations with filter support.
 * <p>
 * Supports filtering by language, repository, file path, and entity type.
 * Multiple values can be specified for each filter (OR logic within filter, AND logic between filters).
 * <p>
 * This DTO is used internally to pass filter parameters from the REST API to the search service.
 * For GET requests, filters are parsed from query parameters.
 */
public class SearchRequest {

    @NotBlank(message = "Query string cannot be blank")
    private String query;

    private List<String> languages;
    private List<String> repositories;
    private List<String> filePaths;
    private List<String> entityTypes;

    @Min(value = 1, message = "Limit must be at least 1")
    @Max(value = 100, message = "Limit cannot exceed 100")
    private int limit = 10;

    @Min(value = 0, message = "Offset cannot be negative")
    private int offset = 0;

    /** When true, include field match info (matched fields and per-field scores) in results (US-02-05, T4). Optional for performance. */
    private boolean includeFieldMatch = false;

    /** When true, enable transitive relationship traversal for structural queries (implements, extends). Default false for backward compatibility (US-02-06, T1). */
    private boolean transitive = false;

    /**
     * Default constructor.
     */
    public SearchRequest() {
        this.languages = new ArrayList<>();
        this.repositories = new ArrayList<>();
        this.filePaths = new ArrayList<>();
        this.entityTypes = new ArrayList<>();
    }

    /**
     * Constructor with query string.
     *
     * @param query the search query string
     */
    public SearchRequest(String query) {
        this();
        this.query = query;
    }

    /**
     * Gets the search query string.
     *
     * @return the query string
     */
    public String getQuery() {
        return query;
    }

    /**
     * Sets the search query string.
     *
     * @param query the query string
     */
    public void setQuery(String query) {
        this.query = query;
    }

    /**
     * Gets the list of language filters.
     * Multiple languages can be specified (OR logic).
     *
     * @return list of language filters
     */
    public List<String> getLanguages() {
        return languages;
    }

    /**
     * Sets the list of language filters.
     *
     * @param languages list of language filters
     */
    public void setLanguages(List<String> languages) {
        this.languages = languages != null ? new ArrayList<>(languages) : new ArrayList<>();
    }

    /**
     * Adds a language filter.
     *
     * @param language the language to filter by
     */
    public void addLanguage(String language) {
        if (language != null && !language.isBlank()) {
            this.languages.add(language.trim());
        }
    }

    /**
     * Gets the list of repository filters.
     * Multiple repositories can be specified (OR logic).
     *
     * @return list of repository filters
     */
    public List<String> getRepositories() {
        return repositories;
    }

    /**
     * Sets the list of repository filters.
     *
     * @param repositories list of repository filters
     */
    public void setRepositories(List<String> repositories) {
        this.repositories = repositories != null ? new ArrayList<>(repositories) : new ArrayList<>();
    }

    /**
     * Adds a repository filter.
     *
     * @param repository the repository to filter by
     */
    public void addRepository(String repository) {
        if (repository != null && !repository.isBlank()) {
            this.repositories.add(repository.trim());
        }
    }

    /**
     * Gets the list of file path filters (prefix match).
     * Multiple paths can be specified (OR logic).
     *
     * @return list of file path filters
     */
    public List<String> getFilePaths() {
        return filePaths;
    }

    /**
     * Sets the list of file path filters.
     *
     * @param filePaths list of file path filters
     */
    public void setFilePaths(List<String> filePaths) {
        this.filePaths = filePaths != null ? new ArrayList<>(filePaths) : new ArrayList<>();
    }

    /**
     * Adds a file path filter.
     *
     * @param filePath the file path prefix to filter by
     */
    public void addFilePath(String filePath) {
        if (filePath != null && !filePath.isBlank()) {
            this.filePaths.add(filePath.trim());
        }
    }

    /**
     * Gets the list of entity type filters.
     * Multiple entity types can be specified (OR logic).
     *
     * @return list of entity type filters
     */
    public List<String> getEntityTypes() {
        return entityTypes;
    }

    /**
     * Sets the list of entity type filters.
     *
     * @param entityTypes list of entity type filters
     */
    public void setEntityTypes(List<String> entityTypes) {
        this.entityTypes = entityTypes != null ? new ArrayList<>(entityTypes) : new ArrayList<>();
    }

    /**
     * Adds an entity type filter.
     *
     * @param entityType the entity type to filter by
     */
    public void addEntityType(String entityType) {
        if (entityType != null && !entityType.isBlank()) {
            this.entityTypes.add(entityType.trim());
        }
    }

    /**
     * Gets the maximum number of results to return.
     *
     * @return the limit
     */
    public int getLimit() {
        return limit;
    }

    /**
     * Sets the maximum number of results to return.
     *
     * @param limit the limit (1-100)
     */
    public void setLimit(int limit) {
        this.limit = limit;
    }

    /**
     * Gets the offset for pagination.
     *
     * @return the offset
     */
    public int getOffset() {
        return offset;
    }

    /**
     * Sets the offset for pagination.
     *
     * @param offset the offset (>= 0)
     */
    public void setOffset(int offset) {
        this.offset = offset;
    }

    /**
     * Whether to include field match information in search results (which fields matched and per-field scores).
     *
     * @return true if field match should be included (adds explain cost per hit)
     */
    public boolean isIncludeFieldMatch() {
        return includeFieldMatch;
    }

    /**
     * Sets whether to include field match information in results.
     *
     * @param includeFieldMatch true to include field match (optional for performance)
     */
    public void setIncludeFieldMatch(boolean includeFieldMatch) {
        this.includeFieldMatch = includeFieldMatch;
    }

    /**
     * Whether to enable transitive relationship traversal for structural queries (implements, extends).
     *
     * @return true if transitive traversal is enabled
     */
    public boolean isTransitive() {
        return transitive;
    }

    /**
     * Sets whether to enable transitive relationship traversal.
     *
     * @param transitive true to enable transitive traversal (default: false for backward compatibility)
     */
    public void setTransitive(boolean transitive) {
        this.transitive = transitive;
    }

    /**
     * Checks if any filters are specified.
     *
     * @return true if at least one filter is set
     */
    public boolean hasFilters() {
        return !languages.isEmpty() || !repositories.isEmpty() ||
               !filePaths.isEmpty() || !entityTypes.isEmpty();
    }

    /**
     * Validates the request and throws IllegalArgumentException if invalid.
     * This is called before passing to the search service.
     *
     * @throws IllegalArgumentException if the request is invalid
     */
    public void validate() {
        if (query == null || query.isBlank()) {
            throw new IllegalArgumentException("Query string cannot be blank");
        }
        if (limit < 1 || limit > 100) {
            throw new IllegalArgumentException("Limit must be between 1 and 100");
        }
        if (offset < 0) {
            throw new IllegalArgumentException("Offset cannot be negative");
        }
    }

    @Override
    public String toString() {
        return "SearchRequest{" +
                "query='" + query + '\'' +
                ", languages=" + languages +
                ", repositories=" + repositories +
                ", filePaths=" + filePaths +
                ", entityTypes=" + entityTypes +
                ", limit=" + limit +
                ", offset=" + offset +
                ", includeFieldMatch=" + includeFieldMatch +
                ", transitive=" + transitive +
                '}';
    }
}
