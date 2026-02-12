/*
 * Copyright (c) 2025 MegaBrain Contributors
 * Licensed under the MIT License - see LICENSE file for details.
 */

package io.megabrain.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.megabrain.core.FacetValue;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for SearchResponse DTO.
 */
class SearchResponseTest {

    private static final String TEST_QUERY = "test query";
    private static final int TEST_SIZE = 20;
    private static final long TEST_TOOK_MS = 45;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldCreateSearchResponseWithAllFields() {
        // Given
        LineRange lineRange = new LineRange(10, 15);
        SearchResult result1 = new SearchResult(
            "public class Test {}", "Test", "class",
            "src/Test.java", "java", "test-repo",
            1.0f, lineRange, "Test class", null, false, null
        );
        SearchResult result2 = new SearchResult(
            "function test() {}", "test", "function",
            "src/test.js", "javascript", "test-repo",
            0.8f, new LineRange(5, 8), null, null, false, null
        );
        List<SearchResult> results = List.of(result1, result2);

        Map<String, List<FacetValue>> facets = Map.of(
                "language", List.of(new FacetValue("java", 120)),
                "repository", List.of(new FacetValue("test-repo", 80))
        );

        // When
        SearchResponse response = new SearchResponse(
            results, 150, 0, TEST_SIZE, TEST_QUERY, TEST_TOOK_MS, facets
        );

        // Then
        assertThat(response.getResults()).isEqualTo(results);
        assertThat(response.getTotal()).isEqualTo(150);
        assertThat(response.getPage()).isZero();
        assertThat(response.getSize()).isEqualTo(TEST_SIZE);
        assertThat(response.getQuery()).isEqualTo(TEST_QUERY);
        assertThat(response.getTookMs()).isEqualTo(TEST_TOOK_MS);
        assertThat(response.getFacets()).isEqualTo(facets);
    }

    @Test
    void shouldHandleEmptyResults() {
        // When
        SearchResponse response = new SearchResponse(
            List.of(), 0, 0, 10, "empty query", 12
        );

        // Then
        assertThat(response.getResults()).isEmpty();
        assertThat(response.getTotal()).isZero();
        assertThat(response.getTotalPages()).isZero();
    }

    @Test
    void shouldHandleNullResults() {
        // When
        SearchResponse response = new SearchResponse(
            null, 100, 1, 25, "null results query", 30
        );

        // Then
        assertThat(response.getResults()).isEmpty();
        assertThat(response.getTotal()).isEqualTo(100);
    }

    @Test
    void shouldCalculatePaginationCorrectly() {
        // Given
        SearchResponse response = new SearchResponse(
            List.of(), 175, 2, 25, "pagination test", 50
        );

        // Then
        assertThat(response.getTotalPages()).isEqualTo(7); // ceil(175/25) = 7
        assertThat(response.hasNextPage()).isTrue(); // page 2 < 6 (0-based)
        assertThat(response.hasPreviousPage()).isTrue(); // page 2 > 0
    }

    @Test
    void shouldHandleFirstPage() {
        // Given
        SearchResponse response = new SearchResponse(
            List.of(), 100, 0, 20, "first page", 25
        );

        // Then
        assertThat(response.hasNextPage()).isTrue();
        assertThat(response.hasPreviousPage()).isFalse();
    }

    @Test
    void shouldHandleLastPage() {
        // Given - exactly on last page
        SearchResponse response = new SearchResponse(
            List.of(), 100, 4, 20, "last page", 25
        ); // 100 total, 20 per page, page 4 = last page

        // Then
        assertThat(response.hasNextPage()).isFalse(); // 5 * 20 = 100, no more pages
        assertThat(response.hasPreviousPage()).isTrue();
    }

    @Test
    void shouldHandleZeroSize() {
        // Given
        SearchResponse response = new SearchResponse(
            List.of(), 50, 0, 0, "zero size", 10
        );

        // Then
        assertThat(response.getTotalPages()).isZero();
        assertThat(response.hasNextPage()).isFalse();
        assertThat(response.hasPreviousPage()).isFalse();
    }

    @Test
    void shouldSerializeToJson() throws Exception {
        // Given
        LineRange lineRange = new LineRange(1, 3);
        SearchResult result = new SearchResult("code snippet", "entity", "type", "file.java", "java", "repo", 0.9f, lineRange, "summary", null, false, null);
        Map<String, List<FacetValue>> facets = Map.of("language", List.of(new FacetValue("java", 2)));
        SearchResponse response = new SearchResponse(List.of(result), 42, 1, 10, "test query", 123, facets);

        // When
        String json = objectMapper.writeValueAsString(response);

        // Then
        assertThat(json)
                .contains("\"results\":[{")
                .contains("\"total\":42")
                .contains("\"page\":1")
                .contains("\"size\":10")
                .contains("\"query\":\"test query\"")
                .contains("\"took_ms\":123")
                .contains("\"facets\"");
    }

    @Test
    void shouldDeserializeFromJson() throws Exception {
        // Given
        String json = """
            {
                "results": [{
                    "content": "sample code",
                    "entity_name": "Sample",
                    "entity_type": "class",
                    "source_file": "Sample.java",
                    "language": "java",
                    "repository": "demo",
                    "score": 1.5,
                    "line_range": {"start": 10, "end": 20},
                    "doc_summary": "Demo class"
                }],
                "total": 1,
                "page": 0,
                "size": 20,
                "query": "sample search",
                "took_ms": 67,
                "facets": {
                    "language": [{"value": "java", "count": 1}]
                }
            }
            """;

        // When
        SearchResponse response = objectMapper.readValue(json, SearchResponse.class);

        // Then
        assertThat(response.getResults()).hasSize(1);
        assertThat(response.getResults().getFirst().getContent()).isEqualTo("sample code");
        assertThat(response.getResults().getFirst().getEntityName()).isEqualTo("Sample");
        assertThat(response.getTotal()).isOne();
        assertThat(response.getPage()).isZero();
        assertThat(response.getSize()).isEqualTo(20);
        assertThat(response.getQuery()).isEqualTo("sample search");
        assertThat(response.getTookMs()).isEqualTo(67);
        assertThat(response.getFacets()).containsKey("language");
    }

    @Test
    void toStringShouldIncludeSummaryInfo() {
        // Given
        SearchResult result1 = new SearchResult("code1", "e1", "t1", "f1", "lang", "repo", 1.0f, new LineRange(1, 1), null, null, false, null);
        SearchResult result2 = new SearchResult("code2", "e2", "t2", "f2", "lang", "repo", 2.0f, new LineRange(2, 2), null, null, false, null);
        SearchResponse response = new SearchResponse(
            List.of(result1, result2), 75, 1, 25, "complex query", 89, Map.of()
        );

        // When
        String string = response.toString();

        // Then
        assertThat(string)
            .contains("SearchResponse{")
            .contains("results.size=2")
            .contains("total=75")
            .contains("page=1")
            .contains("size=25")
            .contains("query='complex query'")
            .contains("tookMs=89")
            .contains("totalPages=3");
    }
}
