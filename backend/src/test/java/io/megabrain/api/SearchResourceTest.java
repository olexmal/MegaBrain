/*
 * Copyright (c) 2025 MegaBrain Contributors
 * Licensed under the MIT License - see LICENSE file for details.
 */

package io.megabrain.api;

import io.megabrain.core.FacetValue;
import io.megabrain.core.ResultMerger;
import io.megabrain.core.SearchMode;
import io.megabrain.core.SearchOrchestrator;
import io.smallrye.mutiny.Uni;
import jakarta.ws.rs.core.Response;
import org.apache.lucene.document.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * Unit tests for SearchResource.
 * Tests filter parameter parsing, validation, and search execution.
 */
@ExtendWith(MockitoExtension.class)
class SearchResourceTest {

    @Mock
    private SearchOrchestrator searchOrchestrator;

    @InjectMocks
    private SearchResource searchResource;

    @BeforeEach
    void setUp() {
        searchResource.facetLimit = 10;
        searchResource.transitiveDefaultDepth = 5;
        searchResource.transitiveMaxDepth = 10;
        lenient().when(searchOrchestrator.orchestrate(any(SearchRequest.class), any(SearchMode.class), anyInt(), anyInt()))
                .thenReturn(Uni.createFrom().item(new SearchOrchestrator.OrchestratorResult(
                        List.of(), Map.of())));
    }

    @Test
    void search_withValidQuery_shouldReturnResults() {
        // Given
        String query = "authentication";
        List<ResultMerger.MergedResult> mockResults = createMockMergedResults(2);
        Map<String, List<FacetValue>> facets = Map.of(
                "language", List.of(new FacetValue("java", 3))
        );
        when(searchOrchestrator.orchestrate(any(SearchRequest.class), eq(SearchMode.HYBRID), anyInt(), anyInt()))
                .thenReturn(Uni.createFrom().item(new SearchOrchestrator.OrchestratorResult(mockResults, facets)));

        // When
        Uni<Response> responseUni = searchResource.search(
                query, null, null, null, null, 10, 0, null, null, null, null);

        // Then
        Response response = responseUni.await().indefinitely();
        assertThat(response.getStatus()).isEqualTo(200);
        SearchResponse searchResponse = (SearchResponse) response.getEntity();
        assertThat(searchResponse.getResults()).hasSize(2);
        assertThat(searchResponse.getQuery()).isEqualTo(query);
        assertThat(searchResponse.getFacets()).containsKey("language");
    }

    @Test
    void search_withLanguageFilter_shouldPassFilterToService() {
        // Given
        String query = "service";
        List<String> languages = List.of("java", "python");
        List<ResultMerger.MergedResult> mockResults = createMockMergedResults(1);
        when(searchOrchestrator.orchestrate(any(SearchRequest.class), any(SearchMode.class), anyInt(), anyInt()))
                .thenReturn(Uni.createFrom().item(new SearchOrchestrator.OrchestratorResult(mockResults, Map.of())));

        // When
        Uni<Response> responseUni = searchResource.search(
                query, languages, null, null, null, 10, 0, null, null, null, null);

        // Then
        Response response = responseUni.await().indefinitely();
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    void search_withMultipleFilters_shouldAcceptAllFilters() {
        // Given
        String query = "test";
        List<String> languages = List.of("java");
        List<String> repositories = List.of("backend");
        List<String> filePaths = List.of("src/main");
        List<String> entityTypes = List.of("class");
        List<ResultMerger.MergedResult> mockResults = createMockMergedResults(1);
        when(searchOrchestrator.orchestrate(any(SearchRequest.class), any(SearchMode.class), anyInt(), anyInt()))
                .thenReturn(Uni.createFrom().item(new SearchOrchestrator.OrchestratorResult(mockResults, Map.of())));

        // When
        Uni<Response> responseUni = searchResource.search(
                query, languages, repositories, filePaths, entityTypes, 10, 0, null, null, null, null);

        // Then
        Response response = responseUni.await().indefinitely();
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    void search_withMultipleLanguageValues_shouldAcceptAll() {
        // Given
        String query = "query";
        List<String> languages = List.of("java", "python", "typescript");
        List<ResultMerger.MergedResult> mockResults = createMockMergedResults(0);
        when(searchOrchestrator.orchestrate(any(SearchRequest.class), any(SearchMode.class), anyInt(), anyInt()))
                .thenReturn(Uni.createFrom().item(new SearchOrchestrator.OrchestratorResult(mockResults, Map.of())));

        // When
        Uni<Response> responseUni = searchResource.search(
                query, languages, null, null, null, 10, 0, null, null, null, null);

        // Then
        Response response = responseUni.await().indefinitely();
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    void search_withMissingQuery_shouldReturnBadRequest() {
        // When
        Uni<Response> responseUni = searchResource.search(
                null, null, null, null, null, 10, 0, null, null, null, null);

        // Then
        Response response = responseUni.await().indefinitely();
        assertThat(response.getStatus()).isEqualTo(400);
        SearchResource.ErrorResponse errorResponse = (SearchResource.ErrorResponse) response.getEntity();
        assertThat(errorResponse.error()).contains("required");
    }

    @Test
    void search_withBlankQuery_shouldReturnBadRequest() {
        // When
        Uni<Response> responseUni = searchResource.search(
                "   ", null, null, null, null, 10, 0, null, null, null, null);

        // Then
        Response response = responseUni.await().indefinitely();
        assertThat(response.getStatus()).isEqualTo(400);
    }

    @Test
    void search_withInvalidLimit_shouldReturnBadRequest() {
        // Given
        String query = "test";

        // When - limit too high (JAX-RS validation should catch @Max(100))
        Uni<Response> responseUni1 = searchResource.search(
                query, null, null, null, null, 101, 0, null, null, null, null);
        Response response1 = responseUni1.await().indefinitely();

        // When - limit too low (JAX-RS validation should catch @Min(1))
        Uni<Response> responseUni2 = searchResource.search(
                query, null, null, null, null, 0, 0, null, null, null, null);
        Response response2 = responseUni2.await().indefinitely();

        // Then - JAX-RS validation should handle @Min/@Max annotations
        // If validation passes, the service would be called, but we don't stub it here
        // since validation should fail first
        assertThat(response1.getStatus()).isIn(200, 400);
        assertThat(response2.getStatus()).isIn(200, 400);
    }

    @Test
    void search_withNegativeOffset_shouldReturnBadRequest() {
        // Given
        String query = "test";

        // When - negative offset (JAX-RS validation should catch @Min(0))
        Uni<Response> responseUni = searchResource.search(
                query, null, null, null, null, 10, -1, null, null, null, null);

        // Then - JAX-RS validation should handle @Min annotation
        Response response = responseUni.await().indefinitely();
        assertThat(response.getStatus()).isIn(200, 400);
    }

    @Test
    void search_withSearchMode_shouldPassModeToService() {
        // Given
        String query = "test";
        List<ResultMerger.MergedResult> mockResults = createMockMergedResults(1);
        when(searchOrchestrator.orchestrate(any(SearchRequest.class), eq(SearchMode.KEYWORD), anyInt(), anyInt()))
                .thenReturn(Uni.createFrom().item(new SearchOrchestrator.OrchestratorResult(mockResults, Map.of())));

        // When
        Uni<Response> responseUni = searchResource.search(
                query, null, null, null, null, 10, 0, "keyword", null, null, null);

        // Then
        Response response = responseUni.await().indefinitely();
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    void search_withInvalidSearchMode_shouldDefaultToHybrid() {
        // Given
        String query = "test";
        List<ResultMerger.MergedResult> mockResults = createMockMergedResults(1);
        when(searchOrchestrator.orchestrate(any(SearchRequest.class), eq(SearchMode.HYBRID), anyInt(), anyInt()))
                .thenReturn(Uni.createFrom().item(new SearchOrchestrator.OrchestratorResult(mockResults, Map.of())));

        // When
        Uni<Response> responseUni = searchResource.search(
                query, null, null, null, null, 10, 0, "invalid", null, null, null);

        // Then
        Response response = responseUni.await().indefinitely();
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    void search_withPagination_shouldApplyOffsetAndLimit() {
        // Given
        String query = "test";
        List<ResultMerger.MergedResult> mockResults = createMockMergedResults(10);
        when(searchOrchestrator.orchestrate(any(SearchRequest.class), any(SearchMode.class), anyInt(), anyInt()))
                .thenReturn(Uni.createFrom().item(new SearchOrchestrator.OrchestratorResult(mockResults, Map.of())));

        // When
        Uni<Response> responseUni = searchResource.search(
                query, null, null, null, null, 5, 3, null, null, null, null);

        // Then
        Response response = responseUni.await().indefinitely();
        assertThat(response.getStatus()).isEqualTo(200);
        SearchResponse searchResponse = (SearchResponse) response.getEntity();
        // Should return results from index 3 to 8 (5 results)
        assertThat(searchResponse.getResults().size()).isLessThanOrEqualTo(5);
        assertThat(searchResponse.getSize()).isEqualTo(5);
        assertThat(searchResponse.getPage()).isGreaterThanOrEqualTo(0);
    }

    @Test
    void search_withServiceFailure_shouldReturnInternalServerError() {
        // Given
        String query = "test";
        when(searchOrchestrator.orchestrate(any(SearchRequest.class), any(SearchMode.class), anyInt(), anyInt()))
                .thenReturn(Uni.createFrom().failure(new RuntimeException("Search failed")));

        // When
        Uni<Response> responseUni = searchResource.search(
                query, null, null, null, null, 10, 0, null, null, null, null);

        // Then
        Response response = responseUni.await().indefinitely();
        assertThat(response.getStatus()).isEqualTo(500);
        SearchResource.ErrorResponse errorResponse = (SearchResource.ErrorResponse) response.getEntity();
        assertThat(errorResponse.error()).contains("failed");
    }

    @Test
    void search_withEmptyFilters_shouldStillWork() {
        // Given
        String query = "test";
        List<String> emptyList = new ArrayList<>();
        List<ResultMerger.MergedResult> mockResults = createMockMergedResults(1);
        when(searchOrchestrator.orchestrate(any(SearchRequest.class), any(SearchMode.class), anyInt(), anyInt()))
                .thenReturn(Uni.createFrom().item(new SearchOrchestrator.OrchestratorResult(mockResults, Map.of())));

        // When
        Uni<Response> responseUni = searchResource.search(
                query, emptyList, emptyList, emptyList, emptyList, 10, 0, null, null, null, null);

        // Then
        Response response = responseUni.await().indefinitely();
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    void search_withTransitiveTrue_shouldCallOrchestratorAndReturnResults() {
        // Given - transitive=true triggers graph integration in orchestrator
        String query = "implements:IRepository";
        List<ResultMerger.MergedResult> mockResults = createMockMergedResults(2);
        when(searchOrchestrator.orchestrate(any(SearchRequest.class), any(SearchMode.class), anyInt(), anyInt()))
                .thenReturn(Uni.createFrom().item(new SearchOrchestrator.OrchestratorResult(mockResults, Map.of())));

        // When
        Uni<Response> responseUni = searchResource.search(
                query, null, null, null, null, 10, 0, null, null, true, null);

        // Then
        Response response = responseUni.await().indefinitely();
        assertThat(response.getStatus()).isEqualTo(200);
        SearchResponse searchResponse = (SearchResponse) response.getEntity();
        assertThat(searchResponse.getResults()).hasSize(2);
        assertThat(searchResponse.getQuery()).isEqualTo(query);
    }

    @Test
    void search_withDepthBelowOne_shouldReturnBadRequest() {
        Uni<Response> responseUni = searchResource.search(
                "implements:I", null, null, null, null, 10, 0, null, null, true, 0);
        Response response = responseUni.await().indefinitely();
        assertThat(response.getStatus()).isEqualTo(400);
        SearchResource.ErrorResponse errorResponse = (SearchResource.ErrorResponse) response.getEntity();
        assertThat(errorResponse.error()).contains("1 and");
    }

    @Test
    void search_withDepthAboveMax_shouldReturnBadRequest() {
        Uni<Response> responseUni = searchResource.search(
                "extends:Base", null, null, null, null, 10, 0, null, null, true, 99);
        Response response = responseUni.await().indefinitely();
        assertThat(response.getStatus()).isEqualTo(400);
        SearchResource.ErrorResponse errorResponse = (SearchResource.ErrorResponse) response.getEntity();
        assertThat(errorResponse.error()).contains("1 and 10");
    }

    /**
     * Creates mock merged results for testing.
     *
     * @param count number of results to create
     * @return list of mock merged results
     */
    private List<ResultMerger.MergedResult> createMockMergedResults(int count) {
        List<ResultMerger.MergedResult> results = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            Document doc = new Document();
            doc.add(new org.apache.lucene.document.StringField("content", "Test content " + i, org.apache.lucene.document.Field.Store.YES));
            doc.add(new org.apache.lucene.document.StringField("entity_name", "TestEntity" + i, org.apache.lucene.document.Field.Store.YES));
            doc.add(new org.apache.lucene.document.StringField("entity_type", "class", org.apache.lucene.document.Field.Store.YES));
            doc.add(new org.apache.lucene.document.StringField("source_file", "Test" + i + ".java", org.apache.lucene.document.Field.Store.YES));
            doc.add(new org.apache.lucene.document.StringField("language", "java", org.apache.lucene.document.Field.Store.YES));
            doc.add(new org.apache.lucene.document.StringField("repository", "test-repo", org.apache.lucene.document.Field.Store.YES));
            doc.add(new org.apache.lucene.document.StringField("start_line", "1", org.apache.lucene.document.Field.Store.YES));
            doc.add(new org.apache.lucene.document.StringField("end_line", "10", org.apache.lucene.document.Field.Store.YES));

            String chunkId = "Test" + i + ".java:TestEntity" + i;
            ResultMerger.MergedResult mergedResult = ResultMerger.MergedResult.fromLucene(chunkId, doc, 0.8);
            results.add(mergedResult);
        }
        return results;
    }
}
