/*
 * Copyright (c) 2025 MegaBrain Contributors
 * Licensed under the MIT License - see LICENSE file for details.
 */

package io.megabrain.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for SearchResult DTO.
 */
class SearchResultTest {

    private static final String TEST_CONTENT = "public class TestClass {\n    private String field;\n}";
    private static final String TEST_ENTITY_NAME = "TestClass";
    private static final String TEST_ENTITY_TYPE = "class";
    private static final String TEST_SOURCE_FILE = "src/main/java/TestClass.java";
    private static final String TEST_LANGUAGE = "java";
    private static final String TEST_REPOSITORY = "test-repo";
    private static final float TEST_SCORE = 0.95f;
    private static final String TEST_DOC_SUMMARY = "A test class for demonstration";

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("creates result with all fields set")
    void createSearchResult_withAllFields_setsAllFields() {
        // Given
        LineRange lineRange = new LineRange(10, 15);

        // When
        SearchResult actual = new SearchResult(
            TEST_CONTENT, TEST_ENTITY_NAME, TEST_ENTITY_TYPE, TEST_SOURCE_FILE,
            TEST_LANGUAGE, TEST_REPOSITORY, TEST_SCORE, lineRange, TEST_DOC_SUMMARY, null, false, null
        );

        // Then
        assertThat(actual.getContent()).isEqualTo(TEST_CONTENT);
        assertThat(actual.getEntityName()).isEqualTo(TEST_ENTITY_NAME);
        assertThat(actual.getEntityType()).isEqualTo(TEST_ENTITY_TYPE);
        assertThat(actual.getSourceFile()).isEqualTo(TEST_SOURCE_FILE);
        assertThat(actual.getLanguage()).isEqualTo(TEST_LANGUAGE);
        assertThat(actual.getRepository()).isEqualTo(TEST_REPOSITORY);
        assertThat(actual.getScore()).isEqualTo(TEST_SCORE);
        assertThat(actual.getLineRange()).isEqualTo(lineRange);
        assertThat(actual.getDocSummary()).isEqualTo(TEST_DOC_SUMMARY);
    }

    @Test
    @DisplayName("creates result with null doc summary")
    void createSearchResult_withNullDocSummary_setsDocSummaryNull() {
        // Given
        LineRange lineRange = new LineRange(5, 8);

        // When
        SearchResult actual = new SearchResult(
            "function test() {}", "test", "function",
            "src/test.js", "javascript", TEST_REPOSITORY,
            0.8f, lineRange, null, null, false, null
        );

        // Then
        assertThat(actual.getDocSummary()).isNull();
    }

    @Test
    @DisplayName("serializes to JSON with expected keys")
    void writeValueAsString_serializesToJson() throws Exception {
        // Given
        LineRange lineRange = new LineRange(20, 25);
        SearchResult result = new SearchResult(
            "public void method() {}", "method", "method",
            "src/Main.java", "java", "example-repo",
            1.2f, lineRange, "Sample method", null, false, null
        );

        // Use the factory method for a simpler case
        SearchResult simpleResult = SearchResult.create(
            "simple content", TEST_ENTITY_NAME, TEST_ENTITY_TYPE,
            TEST_SOURCE_FILE, TEST_LANGUAGE, TEST_REPOSITORY,
            TEST_SCORE, lineRange
        );

        // When
        String actual = objectMapper.writeValueAsString(result);

        // Then
        assertThat(actual).contains("\"content\":\"public void method() {}\"");
        assertThat(actual).contains("\"entity_name\":\"method\"");
        assertThat(actual).contains("\"entity_type\":\"method\"");
        assertThat(actual).contains("\"source_file\":\"src/Main.java\"");
        assertThat(actual).contains("\"language\":\"java\"");
        assertThat(actual).contains("\"repository\":\"example-repo\"");
        assertThat(actual).contains("\"score\":1.2");
        assertThat(actual).contains("\"line_range\":{\"start\":20,\"end\":25}");
        assertThat(actual).contains("\"doc_summary\":\"Sample method\"");
    }

    @Test
    @DisplayName("deserializes from JSON")
    void readValue_deserializesFromJson() throws Exception {
        // Given
        String json = """
            {
                "content": "class Test {}",
                "entity_name": "Test",
                "entity_type": "class",
                "source_file": "src/Test.java",
                "language": "java",
                "repository": "test-repo",
                "score": 0.75,
                "line_range": {"start": 1, "end": 5},
                "doc_summary": "Test class"
            }
            """;

        // When
        SearchResult actual = objectMapper.readValue(json, SearchResult.class);

        // Then
        assertThat(actual.getContent()).isEqualTo("class Test {}");
        assertThat(actual.getEntityName()).isEqualTo("Test");
        assertThat(actual.getEntityType()).isEqualTo("class");
        assertThat(actual.getSourceFile()).isEqualTo("src/Test.java");
        assertThat(actual.getLanguage()).isEqualTo("java");
        assertThat(actual.getRepository()).isEqualTo("test-repo");
        assertThat(actual.getScore()).isEqualTo(0.75f);
        assertThat(actual.getLineRange().getStartLine()).isEqualTo(1);
        assertThat(actual.getLineRange().getEndLine()).isEqualTo(5);
        assertThat(actual.getDocSummary()).isEqualTo("Test class");
    }

    @Test
    @DisplayName("creates result with field match info")
    void createSearchResult_withFieldMatch_setsFieldMatch() {
        // Given (US-02-05, T4)
        LineRange lineRange = new LineRange(1, 5);
        FieldMatchInfo fieldMatch = new FieldMatchInfo(
                List.of("entity_name", "content"),
                Map.of("entity_name", 2.1f, "content", 0.5f)
        );

        // When
        SearchResult actual = new SearchResult(
                TEST_CONTENT, TEST_ENTITY_NAME, TEST_ENTITY_TYPE, TEST_SOURCE_FILE,
                TEST_LANGUAGE, TEST_REPOSITORY, TEST_SCORE, lineRange, TEST_DOC_SUMMARY, fieldMatch, false, null
        );

        // Then
        assertThat(actual.getFieldMatch()).isNotNull();
        assertThat(actual.getFieldMatch().getMatchedFields()).containsExactly("entity_name", "content");
        assertThat(actual.getFieldMatch().getScores()).containsEntry("entity_name", 2.1f);
        assertThat(actual.getFieldMatch().getScores()).containsEntry("content", 0.5f);
    }

    @Test
    @DisplayName("creates result with null field match")
    void createSearchResult_withNullFieldMatch_setsFieldMatchNull() {
        // Given
        LineRange lineRange = new LineRange(1, 1);

        // When
        SearchResult actual = new SearchResult(
                "content", "Entity", "class", "path.java", "java", "repo",
                1.0f, lineRange, null, null, false, null
        );

        // Then
        assertThat(actual.getFieldMatch()).isNull();
    }

    @Test
    @DisplayName("creates transitive result with relationship path (US-02-06, T6)")
    void createSearchResult_transitiveWithPath_setsIsTransitiveAndPath() {
        // Given
        LineRange lineRange = new LineRange(1, 10);
        List<String> path = List.of("IRepository", "BaseRepo", "ConcreteRepo");

        // When
        SearchResult actual = new SearchResult(
                TEST_CONTENT, "ConcreteRepo", "class", TEST_SOURCE_FILE,
                TEST_LANGUAGE, TEST_REPOSITORY, TEST_SCORE, lineRange, null, null, true, path
        );

        // Then
        assertThat(actual.isTransitive()).isTrue();
        assertThat(actual.getRelationshipPath()).containsExactly("IRepository", "BaseRepo", "ConcreteRepo");
    }

    @Test
    @DisplayName("creates non-transitive result has no path")
    void createSearchResult_nonTransitive_hasFalseAndNullPath() {
        // When
        SearchResult actual = SearchResult.create(
                "content", "Entity", "class", "file.java", "java", "repo", 1.0f, new LineRange(1, 1)
        );

        // Then
        assertThat(actual.isTransitive()).isFalse();
        assertThat(actual.getRelationshipPath()).isNull();
    }

    @Test
    @DisplayName("toString includes key fields")
    void toString_includesKeyFields() {
        // Given
        LineRange lineRange = new LineRange(100, 105);
        SearchResult result = new SearchResult(
            "This is a very long content that should be truncated in toString",
            "LongEntityName", "class", "path/to/file.java",
            "java", "repo", 2.5f, lineRange, "Long summary text", null, false, null
        );

        // When
        String actual = result.toString();

        // Then
        assertThat(actual)
            .contains("SearchResult{")
            .contains("content='This is a very long content that should be...")
            .contains("entityName='LongEntityName'")
            .contains("entityType='class'")
            .contains("sourceFile='path/to/file.java'")
            .contains("language='java'")
            .contains("repository='repo'")
            .contains("score=2.5")
            .contains("lineRange=100-105")
            .contains("docSummary='Long summary text...'");
    }
}