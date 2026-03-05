/*
 * Copyright (c) 2025 MegaBrain Contributors
 * Licensed under the MIT License - see LICENSE file for details.
 */

package io.megabrain.api;

import io.megabrain.core.FacetValue;
import io.megabrain.core.ResultMerger;
import io.megabrain.core.SearchOrchestrator;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.smallrye.mutiny.Uni;
import org.apache.lucene.document.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

/**
 * Integration tests for the search REST endpoint.
 * Hits GET /api/v1/search with REST Assured and verifies response status, JSON structure,
 * query parameters, pagination, and error handling.
 */
@QuarkusTest
class SearchResourceIntegrationTest {

    @InjectMock
    SearchOrchestrator searchOrchestrator;

    @BeforeEach
    void setUp() {
        when(searchOrchestrator.orchestrate(any(SearchRequest.class), any(), anyInt(), anyInt()))
                .thenReturn(Uni.createFrom().item(new SearchOrchestrator.OrchestratorResult(
                        List.of(), Map.of())));
    }

    @Test
    @DisplayName("GET with valid q returns 200 and JSON structure with results, total, page, size, facets")
    void get_search_withValidQuery_returns200AndJsonStructure() {
        given()
                .queryParam("q", "authentication")
                .when()
                .get("/api/v1/search")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("results", notNullValue())
                .body("total", notNullValue())
                .body("page", notNullValue())
                .body("size", notNullValue())
                .body("query", equalTo("authentication"))
                .body("took_ms", notNullValue())
                .body("facets", notNullValue());
    }

    @Test
    @DisplayName("GET with all query parameters accepts filters and pagination")
    void get_search_withAllQueryParams_returns200() {
        given()
                .queryParam("q", "service")
                .queryParam("language", "java")
                .queryParam("repository", "backend")
                .queryParam("entity_type", "class")
                .queryParam("limit", 5)
                .queryParam("offset", 0)
                .queryParam("mode", "hybrid")
                .when()
                .get("/api/v1/search")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("query", equalTo("service"))
                .body("size", equalTo(5))
                .body("page", equalTo(0));
    }

    @Test
    @DisplayName("GET with multiple language values accepts all")
    void get_search_withMultipleLanguageParams_returns200() {
        given()
                .queryParam("q", "test")
                .queryParam("language", "java")
                .queryParam("language", "python")
                .when()
                .get("/api/v1/search")
                .then()
                .statusCode(200);
    }

    @Test
    @DisplayName("GET with file_path filter accepts parameter")
    void get_search_withFilePathParam_returns200() {
        given()
                .queryParam("q", "handler")
                .queryParam("file_path", "src/main")
                .when()
                .get("/api/v1/search")
                .then()
                .statusCode(200);
    }

    @Test
    @DisplayName("Pagination: offset and limit reflected in response page, size, total")
    void get_search_withPagination_returnsCorrectPageAndSize() {
        List<ResultMerger.MergedResult> mockResults = createMockMergedResults(10);
        when(searchOrchestrator.orchestrate(any(SearchRequest.class), any(), anyInt(), anyInt()))
                .thenReturn(Uni.createFrom().item(new SearchOrchestrator.OrchestratorResult(mockResults, Map.of())));

        given()
                .queryParam("q", "pagination")
                .queryParam("limit", 3)
                .queryParam("offset", 2)
                .when()
                .get("/api/v1/search")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("results.size()", equalTo(3))
                .body("total", equalTo(10))
                .body("page", equalTo(0))
                .body("size", equalTo(3));
    }

    @Test
    @DisplayName("Missing q returns 400 Bad Request")
    void get_search_missingQ_returns400() {
        given()
                .when()
                .get("/api/v1/search")
                .then()
                .statusCode(400)
                .body("error", containsString("required"));
    }

    @Test
    @DisplayName("Blank q returns 400 Bad Request")
    void get_search_blankQ_returns400() {
        given()
                .queryParam("q", "   ")
                .when()
                .get("/api/v1/search")
                .then()
                .statusCode(400)
                .body("error", notNullValue());
    }

    @Test
    @DisplayName("transitive=true with depth=0 returns 400")
    void get_search_transitiveWithDepthZero_returns400() {
        given()
                .queryParam("q", "implements:I")
                .queryParam("transitive", true)
                .queryParam("depth", 0)
                .when()
                .get("/api/v1/search")
                .then()
                .statusCode(400)
                .body("error", containsString("1 and"));
    }

    @Test
    @DisplayName("transitive=true with depth above max returns 400")
    void get_search_transitiveWithDepthAboveMax_returns400() {
        given()
                .queryParam("q", "extends:Base")
                .queryParam("transitive", true)
                .queryParam("depth", 99)
                .when()
                .get("/api/v1/search")
                .then()
                .statusCode(400)
                .body("error", containsString("1 and"));
    }

    @Test
    @DisplayName("Search mode keyword returns 200")
    void get_search_modeKeyword_returns200() {
        given()
                .queryParam("q", "test")
                .queryParam("mode", "keyword")
                .when()
                .get("/api/v1/search")
                .then()
                .statusCode(200);
    }

    @Test
    @DisplayName("Search mode vector returns 200")
    void get_search_modeVector_returns200() {
        given()
                .queryParam("q", "test")
                .queryParam("mode", "vector")
                .when()
                .get("/api/v1/search")
                .then()
                .statusCode(200);
    }

    @Test
    @DisplayName("Service failure returns 500 and error body")
    void get_search_serviceFailure_returns500() {
        when(searchOrchestrator.orchestrate(any(SearchRequest.class), any(), anyInt(), anyInt()))
                .thenReturn(Uni.createFrom().failure(new RuntimeException("Search failed")));

        given()
                .queryParam("q", "fail")
                .when()
                .get("/api/v1/search")
                .then()
                .statusCode(500)
                .body("error", containsString("failed"));
    }

    @Test
    @DisplayName("Response with facets includes facet structure")
    void get_search_withFacets_returnsFacetsInResponse() {
        Map<String, List<FacetValue>> facets = Map.of(
                "language", List.of(new FacetValue("java", 5), new FacetValue("python", 2))
        );
        when(searchOrchestrator.orchestrate(any(SearchRequest.class), any(), anyInt(), anyInt()))
                .thenReturn(Uni.createFrom().item(new SearchOrchestrator.OrchestratorResult(List.of(), facets)));

        given()
                .queryParam("q", "service")
                .when()
                .get("/api/v1/search")
                .then()
                .statusCode(200)
                .body("facets", notNullValue())
                .body("facets.language", notNullValue())
                .body("facets.language.size()", greaterThanOrEqualTo(1));
    }

    @Test
    @DisplayName("Default limit and offset when omitted")
    void get_search_noLimitOrOffset_usesDefaults() {
        given()
                .queryParam("q", "defaults")
                .when()
                .get("/api/v1/search")
                .then()
                .statusCode(200)
                .body("size", equalTo(10))
                .body("page", equalTo(0));
    }

    private static List<ResultMerger.MergedResult> createMockMergedResults(int count) {
        List<ResultMerger.MergedResult> results = new java.util.ArrayList<>();
        for (int i = 0; i < count; i++) {
            Document doc = new Document();
            doc.add(new org.apache.lucene.document.StringField("content", "Content " + i, org.apache.lucene.document.Field.Store.YES));
            doc.add(new org.apache.lucene.document.StringField("entity_name", "Entity" + i, org.apache.lucene.document.Field.Store.YES));
            doc.add(new org.apache.lucene.document.StringField("entity_type", "class", org.apache.lucene.document.Field.Store.YES));
            doc.add(new org.apache.lucene.document.StringField("source_file", "File" + i + ".java", org.apache.lucene.document.Field.Store.YES));
            doc.add(new org.apache.lucene.document.StringField("language", "java", org.apache.lucene.document.Field.Store.YES));
            doc.add(new org.apache.lucene.document.StringField("repository", "test-repo", org.apache.lucene.document.Field.Store.YES));
            doc.add(new org.apache.lucene.document.StringField("start_line", "1", org.apache.lucene.document.Field.Store.YES));
            doc.add(new org.apache.lucene.document.StringField("end_line", "10", org.apache.lucene.document.Field.Store.YES));
            String chunkId = "File" + i + ".java:Entity" + i;
            results.add(ResultMerger.MergedResult.fromLucene(chunkId, doc, 0.8));
        }
        return results;
    }
}
