/*
 * Copyright (c) 2025 MegaBrain Contributors
 * Licensed under the MIT License - see LICENSE file for details.
 */

package io.megabrain.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.megabrain.core.FacetValue;

import java.util.List;
import java.util.Map;

/**
 * Represents a paginated search response containing multiple search results.
 *
 * This DTO wraps search results with pagination metadata, providing information
 * about the total number of matches, current page, and result set size.
 */
public class SearchResponse {

    @JsonProperty("results")
    private final List<SearchResult> results;

    @JsonProperty("total")
    private final long total;

    @JsonProperty("page")
    private final int page;

    @JsonProperty("size")
    private final int size;

    @JsonProperty("query")
    private final String query;

    @JsonProperty("took_ms")
    private final long tookMs;

    @JsonProperty("facets")
    private final Map<String, List<FacetValue>> facets;

    // Default constructor for Jackson deserialization
    public SearchResponse() {
        this.results = List.of();
        this.total = 0;
        this.page = 0;
        this.size = 10;
        this.query = "";
        this.tookMs = 0;
        this.facets = Map.of();
    }

    /**
     * Creates a new SearchResponse.
     *
     * @param results the list of search results for this page
     * @param total the total number of matching results across all pages
     * @param page the current page number (0-based)
     * @param size the number of results per page
     * @param query the original search query
     * @param tookMs the time taken for the search in milliseconds
     */
    public SearchResponse(List<SearchResult> results, long total, int page, int size,
                         String query, long tookMs) {
        this.results = results != null ? List.copyOf(results) : List.of();
        this.total = total;
        this.page = page;
        this.size = size;
        this.query = query;
        this.tookMs = tookMs;
        this.facets = Map.of();
    }

    /**
     * Creates a new SearchResponse with facet aggregation.
     *
     * @param results the list of search results for this page
     * @param total the total number of matching results across all pages
     * @param page the current page number (0-based)
     * @param size the number of results per page
     * @param query the original search query
     * @param tookMs the time taken for the search in milliseconds
     * @param facets facet counts by field name
     */
    public SearchResponse(List<SearchResult> results, long total, int page, int size,
                          String query, long tookMs, Map<String, List<FacetValue>> facets) {
        this.results = results != null ? List.copyOf(results) : List.of();
        this.total = total;
        this.page = page;
        this.size = size;
        this.query = query;
        this.tookMs = tookMs;
        this.facets = facets != null ? Map.copyOf(facets) : Map.of();
    }

    public List<SearchResult> getResults() {
        return results;
    }

    public long getTotal() {
        return total;
    }

    public int getPage() {
        return page;
    }

    public int getSize() {
        return size;
    }

    public String getQuery() {
        return query;
    }

    public long getTookMs() {
        return tookMs;
    }

    public Map<String, List<FacetValue>> getFacets() {
        return facets;
    }

    public boolean hasNextPage() {
        return size > 0 && (page + 1) * size < total;
    }

    public boolean hasPreviousPage() {
        return page > 0;
    }

    public int getTotalPages() {
        return size > 0 ? (int) Math.ceil((double) total / size) : 0;
    }

    @Override
    public String toString() {
        return "SearchResponse{" +
                "results.size=" + (results != null ? results.size() : 0) +
                ", total=" + total +
                ", page=" + page +
                ", size=" + size +
                ", query='" + query + '\'' +
                ", tookMs=" + tookMs +
                ", facets=" + (facets != null ? facets.keySet() : "none") +
                ", totalPages=" + getTotalPages() +
                '}';
    }
}