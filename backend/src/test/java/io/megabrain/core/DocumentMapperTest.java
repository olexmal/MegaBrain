/*
 * Copyright (c) 2025 MegaBrain Contributors
 * Licensed under the MIT License - see LICENSE file for details.
 */

package io.megabrain.core;

import io.megabrain.ingestion.parser.TextChunk;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexableField;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for DocumentMapper utility class.
 *
 * Tests document creation, field mapping, and ID generation.
 */
class DocumentMapperTest {

    private static final String TEST_CONTENT = "public class TestClass { }";
    private static final String TEST_LANGUAGE = "java";
    private static final String TEST_ENTITY_TYPE = "class";
    private static final String TEST_ENTITY_NAME = "TestClass";
    private static final String TEST_FILE_PATH = "/src/main/java/TestClass.java";
    private static final int TEST_START_LINE = 1;
    private static final int TEST_END_LINE = 5;
    private static final int TEST_START_BYTE = 0;
    private static final int TEST_END_BYTE = 30;

    @Test
    void toDocument_shouldCreateDocumentWithAllFields() {
        // Given
        TextChunk chunk = createTestChunk();

        // When
        Document doc = DocumentMapper.toDocument(chunk);

        // Then
        assertNotNull(doc);
        assertEquals(TEST_CONTENT, doc.get(LuceneSchema.FIELD_CONTENT));
        assertEquals(TEST_ENTITY_NAME, doc.get(LuceneSchema.FIELD_ENTITY_NAME));
        assertEquals(TEST_ENTITY_NAME, doc.get(LuceneSchema.FIELD_ENTITY_NAME_KEYWORD));
        assertEquals(TEST_LANGUAGE, doc.get(LuceneSchema.FIELD_LANGUAGE));
        assertEquals(TEST_ENTITY_TYPE, doc.get(LuceneSchema.FIELD_ENTITY_TYPE));
        assertEquals(TEST_FILE_PATH, doc.get(LuceneSchema.FIELD_FILE_PATH));
        assertEquals("src", doc.get(LuceneSchema.FIELD_REPOSITORY)); // extracted from path
        assertEquals("1", doc.get(LuceneSchema.FIELD_START_LINE));
        assertEquals("5", doc.get(LuceneSchema.FIELD_END_LINE));
        assertEquals("0", doc.get(LuceneSchema.FIELD_START_BYTE));
        assertEquals("30", doc.get(LuceneSchema.FIELD_END_BYTE));
    }

    @Test
    void toDocument_shouldHandleAttributes() {
        // Given
        Map<String, String> attributes = Map.of(
                "visibility", "public",
                "doc_summary", "A test class",
                "custom_attr", "custom_value"
        );

        TextChunk chunk = new TextChunk(
                TEST_CONTENT, TEST_LANGUAGE, TEST_ENTITY_TYPE, TEST_ENTITY_NAME,
                TEST_FILE_PATH, TEST_START_LINE, TEST_END_LINE, TEST_START_BYTE, TEST_END_BYTE,
                attributes
        );

        // When
        Document doc = DocumentMapper.toDocument(chunk);

        // Then
        assertEquals("A test class", doc.get(LuceneSchema.FIELD_DOC_SUMMARY));
        assertEquals("public", doc.get("meta_visibility"));
        assertEquals("custom_value", doc.get("meta_custom_attr"));
    }

    @Test
    void toDocumentWithId_shouldIncludeDocumentIdField() {
        // Given
        TextChunk chunk = createTestChunk();

        // When
        Document doc = DocumentMapper.toDocumentWithId(chunk);

        // Then
        String expectedId = TEST_FILE_PATH + ":" + TEST_ENTITY_NAME + ":" + TEST_START_LINE + ":" + TEST_END_LINE;
        assertEquals(expectedId, doc.get(LuceneSchema.FIELD_DOCUMENT_ID));
        // Should also have all regular fields
        assertEquals(TEST_CONTENT, doc.get(LuceneSchema.FIELD_CONTENT));
    }

    @Test
    void generateDocumentId_shouldCreateUniqueId() {
        // Given
        TextChunk chunk1 = createTestChunk();
        TextChunk chunk2 = new TextChunk(
                "public void method() {}", "java", "method", "TestClass.method",
                "/src/main/java/TestClass.java", 10, 15, 100, 130, Map.of()
        );

        // When
        String id1 = DocumentMapper.generateDocumentId(chunk1);
        String id2 = DocumentMapper.generateDocumentId(chunk2);

        // Then
        assertEquals("/src/main/java/TestClass.java:TestClass:1:5", id1);
        assertEquals("/src/main/java/TestClass.java:TestClass.method:10:15", id2);
        assertNotEquals(id1, id2);
    }

    @Test
    void toDocument_shouldHandleEmptyAttributes() {
        // Given
        TextChunk chunk = new TextChunk(
                TEST_CONTENT, TEST_LANGUAGE, TEST_ENTITY_TYPE, TEST_ENTITY_NAME,
                TEST_FILE_PATH, TEST_START_LINE, TEST_END_LINE, TEST_START_BYTE, TEST_END_BYTE,
                null // null attributes
        );

        // When
        Document doc = DocumentMapper.toDocument(chunk);

        // Then
        assertNotNull(doc);
        assertEquals(TEST_CONTENT, doc.get(LuceneSchema.FIELD_CONTENT));
        // Should not have any metadata fields
        assertNull(doc.get("meta_visibility"));
    }

    @Test
    void toDocument_shouldHandleRepositoryExtraction() {
        // Test various repository path patterns
        List<String> testPaths = List.of(
                "/home/user/project/src/main/java/Test.java",
                "github.com/owner/repo/src/Test.java",
                "owner/repo/Test.java",
                "/some/path/Test.java"
        );

        List<String> expectedRepos = List.of("project", "repo", "repo", "path");

        for (int i = 0; i < testPaths.size(); i++) {
            TextChunk chunk = new TextChunk(
                    TEST_CONTENT, TEST_LANGUAGE, TEST_ENTITY_TYPE, TEST_ENTITY_NAME,
                    testPaths.get(i), TEST_START_LINE, TEST_END_LINE, TEST_START_BYTE, TEST_END_BYTE,
                    Map.of()
            );

            Document doc = DocumentMapper.toDocument(chunk);
            assertEquals(expectedRepos.get(i), doc.get(LuceneSchema.FIELD_REPOSITORY),
                    "Failed for path: " + testPaths.get(i));
        }
    }

    @Test
    void toDocument_shouldCreateCorrectFieldTypes() {
        // Given
        TextChunk chunk = createTestChunk();

        // When
        Document doc = DocumentMapper.toDocument(chunk);

        // Then - check that fields have correct types by examining their properties
        IndexableField contentField = doc.getField(LuceneSchema.FIELD_CONTENT);
        assertNotNull(contentField);
        assertTrue(contentField.fieldType().stored());
        assertTrue(contentField.fieldType().tokenized());
        assertEquals(org.apache.lucene.index.IndexOptions.DOCS_AND_FREQS_AND_POSITIONS_AND_OFFSETS,
                contentField.fieldType().indexOptions());

        IndexableField languageField = doc.getField(LuceneSchema.FIELD_LANGUAGE);
        assertNotNull(languageField);
        assertTrue(languageField.fieldType().stored());
        assertFalse(languageField.fieldType().tokenized());
        assertEquals(org.apache.lucene.index.IndexOptions.DOCS, languageField.fieldType().indexOptions());
    }

    private TextChunk createTestChunk() {
        return new TextChunk(
                TEST_CONTENT,
                TEST_LANGUAGE,
                TEST_ENTITY_TYPE,
                TEST_ENTITY_NAME,
                TEST_FILE_PATH,
                TEST_START_LINE,
                TEST_END_LINE,
                TEST_START_BYTE,
                TEST_END_BYTE,
                Map.of()
        );
    }
}
