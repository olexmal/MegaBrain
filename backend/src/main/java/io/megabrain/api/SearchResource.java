/*
 * Copyright (c) 2025 MegaBrain Contributors
 * Licensed under the MIT License - see LICENSE file for details.
 */

package io.megabrain.api;

import io.megabrain.core.FacetValue;
import io.megabrain.core.ResultMerger;
import io.megabrain.core.SearchFilters;
import io.megabrain.core.SearchMode;
import io.megabrain.core.SearchOrchestrator;
import io.smallrye.mutiny.Uni;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import jakarta.inject.Inject;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;

import org.apache.lucene.document.Document;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * REST API resource for code search operations.
 * <p>
 * Provides endpoints for searching indexed code with support for filtering by
 * language, repository, file path, and entity type.
 * <p>
 * Supports multiple filter values per filter type (e.g., language=java&language=python).
 * Filters are combined with AND logic (all filters must match).
 * Multiple values within a filter use OR logic (any value matches).
 */
@Path("/search")
@Produces(MediaType.APPLICATION_JSON)
public class SearchResource {

    private static final Logger LOG = Logger.getLogger(SearchResource.class);

    private final SearchOrchestrator searchOrchestrator;

    @ConfigProperty(name = "megabrain.search.facets.limit", defaultValue = "10")
    int facetLimit;

    @ConfigProperty(name = "megabrain.search.transitive.default-depth", defaultValue = "5")
    int transitiveDefaultDepth;

    @ConfigProperty(name = "megabrain.search.transitive.max-depth", defaultValue = "10")
    int transitiveMaxDepth;

    @Inject
    public SearchResource(SearchOrchestrator searchOrchestrator) {
        this.searchOrchestrator = searchOrchestrator;
    }

    /**
     * Searches the code index with optional filters.
     * <p>
     * Query parameters:
     * <ul>
     *   <li>{@code q} (required): Search query string</li>
     *   <li>{@code language} (optional, repeatable): Filter by programming language (e.g., java, python)</li>
     *   <li>{@code repository} (optional, repeatable): Filter by repository name</li>
     *   <li>{@code file_path} (optional, repeatable): Filter by file path prefix</li>
     *   <li>{@code entity_type} (optional, repeatable): Filter by entity type (class, method, function, etc.)</li>
     *   <li>{@code limit} (optional, default: 10): Maximum number of results (1-100)</li>
     *   <li>{@code offset} (optional, default: 0): Pagination offset</li>
     *   <li>{@code mode} (optional, default: hybrid): Search mode (hybrid, keyword, vector)</li>
     *   <li>{@code include_field_match} (optional, default: false): Include which fields matched and per-field scores</li>
     *   <li>{@code transitive} (optional, default: false): Enable transitive relationship traversal for structural queries (implements, extends)</li>
     *   <li>{@code depth} (optional): Maximum traversal depth for transitive queries (1 to configured max, default from config). Only used when transitive=true.</li>
     * </ul>
     * <p>
     * Example:
     * <pre>
     * GET /api/v1/search?q=authentication&language=java&language=python&entity_type=class&limit=20
     * GET /api/v1/search?q=implements:IRepository&transitive=true
     * </pre>
     *
     * @param query the search query string (required)
     * @param languages list of language filters (optional, repeatable)
     * @param repositories list of repository filters (optional, repeatable)
     * @param filePaths list of file path prefix filters (optional, repeatable)
     * @param entityTypes list of entity type filters (optional, repeatable)
     * @param limit maximum number of results (default: 10, max: 100)
     * @param offset pagination offset (default: 0)
     * @param mode search mode: hybrid, keyword, or vector (default: hybrid)
     * @param includeFieldMatch include field match info in results (default: false, optional for performance)
     * @param transitive enable transitive relationship traversal (default: false for backward compatibility)
     * @param depth maximum traversal depth for transitive queries (1 to configured max; optional, uses server default when omitted)
     * @return search response with results and pagination metadata
     */
    @GET
    public Uni<Response> search(
            @QueryParam("q") String query,
            @QueryParam("language") List<String> languages,
            @QueryParam("repository") List<String> repositories,
            @QueryParam("file_path") List<String> filePaths,
            @QueryParam("entity_type") List<String> entityTypes,
            @QueryParam("limit") @Min(1) @Max(100) Integer limit,
            @QueryParam("offset") @Min(0) Integer offset,
            @QueryParam("mode") String mode,
            @QueryParam("include_field_match") Boolean includeFieldMatch,
            @QueryParam("transitive") Boolean transitive,
            @QueryParam("depth") Integer depth) {

        long startTime = System.currentTimeMillis();

        try {
            // Validate required query parameter
            if (query == null || query.isBlank()) {
                return Uni.createFrom().item(Response.status(Response.Status.BAD_REQUEST)
                        .entity(new ErrorResponse("Query parameter 'q' is required and cannot be blank"))
                        .build());
            }

            // Build SearchRequest from query parameters
            SearchRequest searchRequest = new SearchRequest(query.trim());

            // Add filter parameters (supporting multiple values)
            if (languages != null) {
                languages.forEach(searchRequest::addLanguage);
            }
            if (repositories != null) {
                repositories.forEach(searchRequest::addRepository);
            }
            if (filePaths != null) {
                filePaths.forEach(searchRequest::addFilePath);
            }
            if (entityTypes != null) {
                entityTypes.forEach(searchRequest::addEntityType);
            }

            // Set pagination parameters with defaults
            searchRequest.setLimit(limit != null ? limit : 10);
            searchRequest.setOffset(offset != null ? offset : 0);
            searchRequest.setIncludeFieldMatch(Boolean.TRUE.equals(includeFieldMatch));
            searchRequest.setTransitive(Boolean.TRUE.equals(transitive));
            searchRequest.setDepth(depth);

            // Validate the request
            try {
                searchRequest.validate();
            } catch (IllegalArgumentException e) {
                LOG.warnf("Invalid search request: %s", e.getMessage());
                return Uni.createFrom().item(Response.status(Response.Status.BAD_REQUEST)
                        .entity(new ErrorResponse(e.getMessage()))
                        .build());
            }

            // Validate depth when supplied (must be 1 to max)
            if (searchRequest.getDepth() != null && (searchRequest.getDepth() < 1 || searchRequest.getDepth() > transitiveMaxDepth)) {
                String msg = String.format("Depth must be between 1 and %d (configured max)", transitiveMaxDepth);
                LOG.warnf("Invalid search request: %s", msg);
                return Uni.createFrom().item(Response.status(Response.Status.BAD_REQUEST)
                        .entity(new ErrorResponse(msg))
                        .build());
            }

            // Resolve effective transitive depth (used only when transitive=true)
            int effectiveDepth = searchRequest.getDepth() != null
                    ? Math.max(1, Math.min(searchRequest.getDepth(), transitiveMaxDepth))
                    : transitiveDefaultDepth;

            // Parse search mode
            SearchMode searchMode = parseSearchMode(mode);

            LOG.debugf("Search request: query='%s', filters=%s, limit=%d, offset=%d, mode=%s",
                    searchRequest.getQuery(), searchRequest.hasFilters() ? "present" : "none",
                    searchRequest.getLimit(), searchRequest.getOffset(), searchMode);

            return searchOrchestrator.orchestrate(searchRequest, searchMode, facetLimit, effectiveDepth)
                    .map(orcResult -> {
                        long tookMs = System.currentTimeMillis() - startTime;
                        List<ResultMerger.MergedResult> mergedResults = orcResult.mergedResults();
                        Map<String, List<FacetValue>> facets = orcResult.facets();

                        // Convert merged results to SearchResult DTOs
                        List<SearchResult> results = mergedResults.stream()
                                .map(this::convertToSearchResult)
                                .collect(Collectors.toList());

                        // Calculate pagination
                        int page = searchRequest.getOffset() / searchRequest.getLimit();
                        long total = results.size();

                        // Apply pagination (offset/limit)
                        int fromIndex = searchRequest.getOffset();
                        int toIndex = Math.min(fromIndex + searchRequest.getLimit(), results.size());
                        List<SearchResult> paginatedResults = fromIndex < results.size() ?
                                results.subList(fromIndex, toIndex) : new ArrayList<>();

                        SearchResponse response = new SearchResponse(
                                paginatedResults,
                                total,
                                page,
                                searchRequest.getLimit(),
                                searchRequest.getQuery(),
                                tookMs,
                                facets
                        );

                        LOG.infof("Search completed: query='%s', results=%d, total=%d, took=%d ms%s",
                                searchRequest.getQuery(), paginatedResults.size(), total, tookMs,
                                searchRequest.isTransitive() ? " (transitive)" : "");

                        return Response.ok(response).build();
                    })
                    .onFailure().recoverWithItem(throwable -> {
                        long tookMs = System.currentTimeMillis() - startTime;
                        LOG.errorf(throwable, "Search failed after %d ms: query='%s'", tookMs, query);
                        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                                .entity(new ErrorResponse("Search operation failed: " + throwable.getMessage()))
                                .build();
                    });

        } catch (Exception e) {
            long tookMs = System.currentTimeMillis() - startTime;
            LOG.errorf(e, "Unexpected error during search after %d ms", tookMs);
            return Uni.createFrom().item(Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorResponse("Unexpected error: " + e.getMessage()))
                    .build());
        }
    }

    /**
     * Converts a MergedResult to a SearchResult DTO.
     *
     * @param mergedResult the merged result from hybrid search
     * @return SearchResult DTO
     */
    private SearchResult convertToSearchResult(ResultMerger.MergedResult mergedResult) {
        // Determine which result source to use (prefer Lucene if available)
        String content;
        String entityName;
        String entityType;
        String sourceFile;
        String language;
        String repository;
        float score;
        LineRange lineRange;

        if (mergedResult.luceneDocument() != null) {
            var luceneDoc = mergedResult.luceneDocument();
            content = luceneDoc.get("content");
            entityName = luceneDoc.get("entity_name");
            entityType = luceneDoc.get("entity_type");
            sourceFile = luceneDoc.get("source_file");
            language = luceneDoc.get("language");
            repository = luceneDoc.get("repository");
            score = (float) mergedResult.combinedScore();
            int startLine = getIntField(luceneDoc, "start_line", 1);
            int endLine = getIntField(luceneDoc, "end_line", 1);
            lineRange = new LineRange(startLine, endLine);
        } else if (mergedResult.vectorResult() != null) {
            var vectorMeta = mergedResult.vectorResult().metadata();
            content = vectorMeta.content();
            entityName = vectorMeta.entityName();
            entityType = vectorMeta.entityType();
            sourceFile = vectorMeta.sourceFile();
            language = vectorMeta.language();
            repository = ""; // Vector results don't have repository yet
            score = (float) mergedResult.combinedScore();
            lineRange = new LineRange(vectorMeta.startLine(), vectorMeta.endLine());
        } else {
            // Fallback (should not happen)
            content = "";
            entityName = "";
            entityType = "";
            sourceFile = "";
            language = "";
            repository = "";
            score = 0.0f;
            lineRange = new LineRange(1, 1);
        }

        FieldMatchInfo apiFieldMatch = mergedResult.fieldMatch() != null
                ? new FieldMatchInfo(mergedResult.fieldMatch().matchedFields(), mergedResult.fieldMatch().scores())
                : null;
        return new SearchResult(content, entityName, entityType, sourceFile,
                language, repository, score, lineRange, null, apiFieldMatch);
    }

    /**
     * Gets an integer field from a Lucene document, with a default value.
     *
     * @param doc the Lucene document
     * @param fieldName the field name
     * @param defaultValue the default value if field is missing or invalid
     * @return the integer value
     */
    private int getIntField(Document doc, String fieldName, int defaultValue) {
        String value = doc.get(fieldName);
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * Parses the search mode string to SearchMode enum.
     *
     * @param mode the mode string (hybrid, keyword, vector)
     * @return the SearchMode enum, defaulting to HYBRID if invalid
     */
    private SearchMode parseSearchMode(String mode) {
        if (mode == null || mode.isBlank()) {
            return SearchMode.HYBRID;
        }
        try {
            return SearchMode.valueOf(mode.toUpperCase());
        } catch (IllegalArgumentException e) {
            LOG.warnf("Invalid search mode '%s', defaulting to HYBRID", mode);
            return SearchMode.HYBRID;
        }
    }

    /**
     * Error response DTO.
     */
    public record ErrorResponse(String error) {
    }
}
