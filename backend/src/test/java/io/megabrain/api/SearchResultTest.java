/*
 * Copyright (c) 2025 MegaBrain Contributors
 * Licensed under the MIT License - see LICENSE file for details.
 */

package io.megabrain.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

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
    void shouldCreateSearchResultWithAllFields() {
        // Given
        LineRange lineRange = new LineRange(10, 15);

        // When
        SearchResult result = new SearchResult(
            TEST_CONTENT, TEST_ENTITY_NAME, TEST_ENTITY_TYPE, TEST_SOURCE_FILE,
            TEST_LANGUAGE, TEST_REPOSITORY, TEST_SCORE, lineRange, TEST_DOC_SUMMARY
        );

        // Then
        assertThat(result.getContent()).isEqualTo(TEST_CONTENT);
        assertThat(result.getEntityName()).isEqualTo(TEST_ENTITY_NAME);
        assertThat(result.getEntityType()).isEqualTo(TEST_ENTITY_TYPE);
        assertThat(result.getSourceFile()).isEqualTo(TEST_SOURCE_FILE);
        assertThat(result.getLanguage()).isEqualTo(TEST_LANGUAGE);
        assertThat(result.getRepository()).isEqualTo(TEST_REPOSITORY);
        assertThat(result.getScore()).isEqualTo(TEST_SCORE);
        assertThat(result.getLineRange()).isEqualTo(lineRange);
        assertThat(result.getDocSummary()).isEqualTo(TEST_DOC_SUMMARY);
    }

    @Test
    void shouldCreateSearchResultWithNullDocSummary() {
        // Given
        LineRange lineRange = new LineRange(5, 8);

        // When
        SearchResult result = new SearchResult(
            "function test() {}", "test", "function",
            "src/test.js", "javascript", TEST_REPOSITORY,
            0.8f, lineRange, null
        );

        // Then
        assertThat(result.getDocSummary()).isNull();
    }

    @Test
    void shouldSerializeToJson() throws Exception {
        // Given
        LineRange lineRange = new LineRange(20, 25);
        SearchResult result = new SearchResult(
            "public void method() {}", "method", "method",
            "src/Main.java", "java", "example-repo",
            1.2f, lineRange, "Sample method"
        );

        // Use the factory method for a simpler case
        SearchResult simpleResult = SearchResult.create(
            "simple content", TEST_ENTITY_NAME, TEST_ENTITY_TYPE,
            TEST_SOURCE_FILE, TEST_LANGUAGE, TEST_REPOSITORY,
            TEST_SCORE, lineRange
        );

        // When
        String json = objectMapper.writeValueAsString(result);

        // Then
        assertThat(json).contains("\"content\":\"public void method() {}\"");
        assertThat(json).contains("\"entity_name\":\"method\"");
        assertThat(json).contains("\"entity_type\":\"method\"");
        assertThat(json).contains("\"source_file\":\"src/Main.java\"");
        assertThat(json).contains("\"language\":\"java\"");
        assertThat(json).contains("\"repository\":\"example-repo\"");
        assertThat(json).contains("\"score\":1.2");
        assertThat(json).contains("\"line_range\":{\"start\":20,\"end\":25}");
        assertThat(json).contains("\"doc_summary\":\"Sample method\"");
    }

    @Test
    void shouldDeserializeFromJson() throws Exception {
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
        SearchResult result = objectMapper.readValue(json, SearchResult.class);

        // Then
        assertThat(result.getContent()).isEqualTo("class Test {}");
        assertThat(result.getEntityName()).isEqualTo("Test");
        assertThat(result.getEntityType()).isEqualTo("class");
        assertThat(result.getSourceFile()).isEqualTo("src/Test.java");
        assertThat(result.getLanguage()).isEqualTo("java");
        assertThat(result.getRepository()).isEqualTo("test-repo");
        assertThat(result.getScore()).isEqualTo(0.75f);
        assertThat(result.getLineRange().getStartLine()).isEqualTo(1);
        assertThat(result.getLineRange().getEndLine()).isEqualTo(5);
        assertThat(result.getDocSummary()).isEqualTo("Test class");
    }

    @Test
    void toStringShouldIncludeKeyFields() {
        // Given
        LineRange lineRange = new LineRange(100, 105);
        SearchResult result = new SearchResult(
            "This is a very long content that should be truncated in toString",
            "LongEntityName", "class", "path/to/file.java",
            "java", "repo", 2.5f, lineRange, "Long summary text"
        );

        // When
        String string = result.toString();

        // Then
        assertThat(string)
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