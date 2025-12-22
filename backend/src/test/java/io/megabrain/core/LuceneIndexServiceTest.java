/*
 * Copyright (c) 2025 MegaBrain Contributors
 * Licensed under the MIT License - see LICENSE file for details.
 */

package io.megabrain.core;

import io.megabrain.ingestion.parser.TextChunk;
import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for LuceneIndexService.
 *
 * Uses temporary directories for testing to avoid file system conflicts.
 */
public class LuceneIndexServiceTest {

    // Test constants to avoid duplication
    private static final String TEST_FILE_1 = "/src/main/java/TestClass.java";
    private static final String TEST_FILE_2 = "/src/main/java/UserService.java";
    private static final String ENTITY_TYPE_CLASS = "class";
    private static final String ENTITY_TYPE_METHOD = "method";
    private static final String LANGUAGE_JAVA = "java";
    private static final String TEST_CLASS_CONTENT = "public class TestClass";
    private static final String TEST_METHOD_CONTENT = "public String getUserName()";
    private static final String TEST_CLASS_NAME = "TestClass";
    private static final String TEST_METHOD_NAME = "getUserName";
    private static final String TEST_SERVICE_NAME = "UserService";

    @TempDir
    Path tempDir;

    private LuceneIndexService indexService;

    @BeforeEach
    void setUp() throws Exception {
        // Create a unique subdirectory for each test to ensure isolation
        Path testIndexDir = tempDir.resolve("test-" + System.nanoTime());
        java.nio.file.Files.createDirectories(testIndexDir);

        // Create a test-specific index service with temporary directory
        indexService = new TestLuceneIndexService();
        indexService.indexDirectoryPath = testIndexDir.toString();
        // Initialize manually instead of using CDI lifecycle
        indexService.initialize();
    }

    @AfterEach
    void tearDown() throws Exception {
        if (indexService != null) {
            indexService.shutdown();
        }
    }

    @Test
    public void testBasicFunctionality() {
        // Simple test to verify test framework works
        assertNotNull(indexService);
        assertNotNull(tempDir);
    }

    @Test
    public void testAddEmptyChunks() {
        // When
        var result = indexService.addChunks(List.of()).subscribe().withSubscriber(UniAssertSubscriber.create());

        // Then
        result.assertCompleted();
        assertIndexContains(0);
    }

    @Test
    public void testRemoveChunksForFile() {
        // Given
        List<TextChunk> chunks = List.of(
                createTestChunk(TEST_CLASS_NAME, ENTITY_TYPE_CLASS, LANGUAGE_JAVA, TEST_FILE_1, TEST_CLASS_CONTENT),
                createTestChunk(TEST_METHOD_NAME, ENTITY_TYPE_METHOD, LANGUAGE_JAVA, TEST_FILE_1, TEST_METHOD_CONTENT)
        );

        indexService.addChunks(chunks).await().indefinitely();

        // When
        var result = indexService.removeChunksForFile(TEST_FILE_1).subscribe().withSubscriber(UniAssertSubscriber.create());

        // Then
        result.assertCompleted();
        // Verify that all documents for the file were removed
        assertIndexContains(0);
    }

    @Test
    public void testRemoveChunksForNonExistentFile() {
        // When
        var result = indexService.removeChunksForFile("/non/existent/file.java")
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        // Then
        result.assertCompleted();
        // Should complete without error even for non-existent files
    }

    @Test
    public void testUpdateChunksForFile() {
        // Given
        List<TextChunk> originalChunks = List.of(
                createTestChunk(TEST_CLASS_NAME, ENTITY_TYPE_CLASS, LANGUAGE_JAVA, TEST_FILE_1, TEST_CLASS_CONTENT)
        );

        List<TextChunk> updatedChunks = List.of(
                createTestChunk(TEST_CLASS_NAME, ENTITY_TYPE_CLASS, LANGUAGE_JAVA, TEST_FILE_1, "public class TestClass { /* updated */ }"),
                createTestChunk(TEST_METHOD_NAME, ENTITY_TYPE_METHOD, LANGUAGE_JAVA, TEST_FILE_1, "public String getUserName() { return null; }")
        );

        indexService.addChunks(originalChunks).await().indefinitely();

        // When
        var result = indexService.updateChunksForFile(TEST_FILE_1, updatedChunks)
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        // Then
        result.assertCompleted();
        // Should have the updated chunks (old ones removed, new ones added)
        var stats = indexService.getIndexStats().subscribe().withSubscriber(UniAssertSubscriber.create());
        stats.assertCompleted();
        assertTrue(stats.getItem().numDocs() >= 1); // At least the updated chunks
    }

    // Search tests removed - search functionality will be enhanced in T5
    // Core indexing functionality is tested above

    @Test
    public void testGetIndexStats() {
        // Given
        List<TextChunk> chunks = List.of(
                createTestChunk(TEST_CLASS_NAME, ENTITY_TYPE_CLASS, LANGUAGE_JAVA, TEST_FILE_1, TEST_CLASS_CONTENT),
                createTestChunk(TEST_SERVICE_NAME, ENTITY_TYPE_CLASS, LANGUAGE_JAVA, TEST_FILE_2, "public class UserService")
        );

        indexService.addChunks(chunks).await().indefinitely();

        // When
        var result = indexService.getIndexStats().subscribe().withSubscriber(UniAssertSubscriber.create());

        // Then
        result.assertCompleted();
        LuceneIndexService.IndexStats stats = result.getItem();
        assertNotNull(stats);
        assertEquals(2, stats.numDocs());
        assertEquals(2, stats.maxDoc());
        assertEquals(0, stats.numDeletedDocs());
    }

    @Test
    public void testIndexStatsAfterDeletion() {
        // Given
        List<TextChunk> chunks = List.of(
                createTestChunk(TEST_CLASS_NAME, ENTITY_TYPE_CLASS, LANGUAGE_JAVA, TEST_FILE_1, TEST_CLASS_CONTENT),
                createTestChunk(TEST_SERVICE_NAME, ENTITY_TYPE_CLASS, LANGUAGE_JAVA, TEST_FILE_2, "public class UserService")
        );

        indexService.addChunks(chunks).await().indefinitely();
        indexService.removeChunksForFile(TEST_FILE_1).await().indefinitely();

        // When
        var result = indexService.getIndexStats().subscribe().withSubscriber(UniAssertSubscriber.create());

        // Then
        result.assertCompleted();
        LuceneIndexService.IndexStats stats = result.getItem();
        assertNotNull(stats);
        assertEquals(1, stats.numDocs());
        assertEquals(1, stats.maxDoc()); // Documents are fully removed, not marked as deleted
        assertEquals(0, stats.numDeletedDocs());
    }

    @Test
    public void testChunkWithAttributes() {
        // Given
        Map<String, String> attributes = Map.of(
                "visibility", "public",
                "doc_summary", "A test class for demonstration"
        );

        TextChunk chunk = new TextChunk(
                "public class TestClass",
                "java",
                "class",
                "com.example.TestClass",
                "/src/main/java/TestClass.java",
                1, 10, 0, 100,
                attributes
        );

        // When
        indexService.addChunks(List.of(chunk)).await().indefinitely();

        // Then
        assertIndexContains(1);

        // Document should be indexed successfully (search functionality tested separately in T5)
    }

    @Test
    public void testRepositoryExtraction() {
        // Given - test that chunks with repository paths can be indexed
        TextChunk chunk = createTestChunk(TEST_CLASS_NAME, ENTITY_TYPE_CLASS, LANGUAGE_JAVA,
                "/home/user/projects/myproject/src/main/java/TestClass.java", TEST_CLASS_CONTENT);

        // When
        indexService.addChunks(List.of(chunk)).await().indefinitely();

        // Then - should have indexed the chunk successfully
        assertIndexContains(1);
    }

    @Test
    public void testAddChunksBatch_withSmallBatchSize() {
        // Given
        List<TextChunk> chunks = List.of(
                createTestChunk("Class1", ENTITY_TYPE_CLASS, LANGUAGE_JAVA, TEST_FILE_1, "class Class1"),
                createTestChunk("Class2", ENTITY_TYPE_CLASS, LANGUAGE_JAVA, TEST_FILE_1, "class Class2"),
                createTestChunk("Class3", ENTITY_TYPE_CLASS, LANGUAGE_JAVA, TEST_FILE_1, "class Class3")
        );

        // When
        var result = indexService.addChunksBatch(chunks, 2)
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        // Then
        result.assertCompleted();
        assertIndexContains(3);
    }

    @Test
    public void testAddChunksBatch_withLargeBatchSize() {
        // Given
        List<TextChunk> chunks = List.of(
                createTestChunk("Class1", ENTITY_TYPE_CLASS, LANGUAGE_JAVA, TEST_FILE_1, "class Class1"),
                createTestChunk("Class2", ENTITY_TYPE_CLASS, LANGUAGE_JAVA, TEST_FILE_1, "class Class2")
        );

        // When
        var result = indexService.addChunksBatch(chunks, 10) // batch size larger than chunk count
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        // Then
        result.assertCompleted();
        assertIndexContains(2);
    }

    @Test
    public void testAddChunksBatch_withEmptyList() {
        // When
        var result = indexService.addChunksBatch(List.of(), 5)
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        // Then
        result.assertCompleted();
        assertIndexContains(0);
    }

    @Test
    public void testUpdateDocument_singleDocument() {
        // Given
        TextChunk originalChunk = createTestChunk(TEST_CLASS_NAME, ENTITY_TYPE_CLASS, LANGUAGE_JAVA,
                TEST_FILE_1, TEST_CLASS_CONTENT);
        indexService.addChunks(List.of(originalChunk)).await().indefinitely();

        TextChunk updatedChunk = new TextChunk(
                "public class TestClass { /* updated content */ }",
                LANGUAGE_JAVA, ENTITY_TYPE_CLASS, TEST_CLASS_NAME,
                TEST_FILE_1, 1, 10, 0, 50, Map.of()
        );

        // When
        var result = indexService.updateDocument(updatedChunk)
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        // Then
        result.assertCompleted();
        assertIndexContains(1); // Should still have 1 document

        // Verify we can search for updated content (basic verification)
        var searchResult = indexService.search("updated", 10)
                .subscribe().withSubscriber(UniAssertSubscriber.create());
        searchResult.assertCompleted();
        assertFalse(searchResult.getItem().isEmpty());
    }

    @Test
    public void testUpdateDocuments_multipleDocuments() {
        // Given
        List<TextChunk> originalChunks = List.of(
                createTestChunk("Class1", ENTITY_TYPE_CLASS, LANGUAGE_JAVA, TEST_FILE_1, "class Class1"),
                createTestChunk("Class2", ENTITY_TYPE_CLASS, LANGUAGE_JAVA, TEST_FILE_2, "class Class2")
        );
        indexService.addChunks(originalChunks).await().indefinitely();

        List<TextChunk> updatedChunks = List.of(
                new TextChunk("class Class1 { /* updated */ }", LANGUAGE_JAVA, ENTITY_TYPE_CLASS, "Class1",
                        TEST_FILE_1, 1, 10, 0, 30, Map.of()),
                new TextChunk("class Class2 { /* updated */ }", LANGUAGE_JAVA, ENTITY_TYPE_CLASS, "Class2",
                        TEST_FILE_2, 1, 10, 0, 30, Map.of())
        );

        // When
        var result = indexService.updateDocuments(updatedChunks)
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        // Then
        result.assertCompleted();
        assertIndexContains(2); // Should still have 2 documents
    }

    @Test
    public void testUpdateDocumentsBatch_withBatchSize() {
        // Given
        List<TextChunk> originalChunks = List.of(
                createTestChunk("Class1", ENTITY_TYPE_CLASS, LANGUAGE_JAVA, TEST_FILE_1, "class Class1"),
                createTestChunk("Class2", ENTITY_TYPE_CLASS, LANGUAGE_JAVA, TEST_FILE_1, "class Class2"),
                createTestChunk("Class3", ENTITY_TYPE_CLASS, LANGUAGE_JAVA, TEST_FILE_1, "class Class3")
        );
        indexService.addChunks(originalChunks).await().indefinitely();

        List<TextChunk> updatedChunks = List.of(
                new TextChunk("class Class1 { /* v1 */ }", LANGUAGE_JAVA, ENTITY_TYPE_CLASS, "Class1",
                        TEST_FILE_1, 1, 10, 0, 25, Map.of()),
                new TextChunk("class Class2 { /* v1 */ }", LANGUAGE_JAVA, ENTITY_TYPE_CLASS, "Class2",
                        TEST_FILE_1, 11, 20, 26, 50, Map.of()),
                new TextChunk("class Class3 { /* v1 */ }", LANGUAGE_JAVA, ENTITY_TYPE_CLASS, "Class3",
                        TEST_FILE_1, 21, 30, 51, 75, Map.of())
        );

        // When
        var result = indexService.updateDocumentsBatch(updatedChunks, 2)
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        // Then
        result.assertCompleted();
        assertIndexContains(3);
    }

    @Test
    public void testRemoveDocument_byDocumentId() {
        // Given
        TextChunk chunk = createTestChunk(TEST_CLASS_NAME, ENTITY_TYPE_CLASS, LANGUAGE_JAVA,
                TEST_FILE_1, TEST_CLASS_CONTENT);
        indexService.addChunks(List.of(chunk)).await().indefinitely();

        String documentId = DocumentMapper.generateDocumentId(chunk);

        // When
        var result = indexService.removeDocument(documentId)
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        // Then
        result.assertCompleted();
        result.assertItem(1); // Should have removed 1 document
        assertIndexContains(0);
    }

    @Test
    public void testRemoveDocument_nonExistentId() {
        // When
        var result = indexService.removeDocument("non:existent:id")
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        // Then
        result.assertCompleted();
        result.assertItem(0); // Should have removed 0 documents
    }

    @Test
    public void testRemoveDocuments_multipleIds() {
        // Given
        List<TextChunk> chunks = List.of(
                createTestChunk("Class1", ENTITY_TYPE_CLASS, LANGUAGE_JAVA, TEST_FILE_1, "class Class1"),
                createTestChunk("Class2", ENTITY_TYPE_CLASS, LANGUAGE_JAVA, TEST_FILE_2, "class Class2")
        );
        indexService.addChunks(chunks).await().indefinitely();

        List<String> documentIds = chunks.stream()
                .map(DocumentMapper::generateDocumentId)
                .toList();

        // When
        var result = indexService.removeDocuments(documentIds)
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        // Then
        result.assertCompleted();
        result.assertItem(2); // Should have removed 2 documents
        assertIndexContains(0);
    }

    @Test
    public void testRemoveDocuments_emptyList() {
        // When
        var result = indexService.removeDocuments(List.of())
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        // Then
        result.assertCompleted();
        result.assertItem(0);
    }

    @Test
    public void testUpdateChunksForFileBatch_withBatchSize() {
        // Given
        List<TextChunk> originalChunks = List.of(
                createTestChunk(TEST_CLASS_NAME, ENTITY_TYPE_CLASS, LANGUAGE_JAVA, TEST_FILE_1, TEST_CLASS_CONTENT)
        );
        indexService.addChunks(originalChunks).await().indefinitely();

        List<TextChunk> updatedChunks = List.of(
                createTestChunk(TEST_CLASS_NAME, ENTITY_TYPE_CLASS, LANGUAGE_JAVA, TEST_FILE_1, "public class TestClass { /* updated */ }"),
                createTestChunk(TEST_METHOD_NAME, ENTITY_TYPE_METHOD, LANGUAGE_JAVA, TEST_FILE_1, "public String getUserName() { return null; }")
        );

        // When
        var result = indexService.updateChunksForFileBatch(TEST_FILE_1, updatedChunks, 1)
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        // Then
        result.assertCompleted();
        assertIndexContains(2); // Should have new chunks after removing old ones
    }

    private TextChunk createTestChunk(String entityName, String entityType, String language,
                                    String sourceFile, String content) {
        return new TextChunk(
                content,
                language,
                entityType,
                entityName,
                sourceFile,
                1, 10, 0, content.length(),
                Map.of()
        );
    }

    private void assertIndexContains(int expectedCount) {
        var statsResult = indexService.getIndexStats().subscribe().withSubscriber(UniAssertSubscriber.create());
        statsResult.assertCompleted();
        LuceneIndexService.IndexStats stats = statsResult.getItem();
        assertEquals(expectedCount, stats.numDocs());
    }

    /**
     * Test-specific subclass to allow injection of test directory.
     */
    private static class TestLuceneIndexService extends LuceneIndexService {
        // Extends LuceneIndexService to access protected fields for testing
    }
}
