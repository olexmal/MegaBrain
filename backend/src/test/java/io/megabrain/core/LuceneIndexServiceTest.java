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
        indexService.initialize();
    }

    @AfterEach
    void tearDown() throws Exception {
        if (indexService != null) {
            indexService.shutdown();
        }
    }

    @Test
    public void testAddChunks() {
        // Given
        List<TextChunk> chunks = List.of(
                createTestChunk("com.example.TestClass", ENTITY_TYPE_CLASS, LANGUAGE_JAVA, TEST_FILE_1, TEST_CLASS_CONTENT),
                createTestChunk(TEST_METHOD_NAME, ENTITY_TYPE_METHOD, LANGUAGE_JAVA, TEST_FILE_1, TEST_METHOD_CONTENT)
        );

        // When
        var result = indexService.addChunks(chunks).subscribe().withSubscriber(UniAssertSubscriber.create());

        // Then
        result.assertCompleted();
        assertIndexContains(chunks.size());
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

    @Test
    public void testSearch() {
        // Given
        List<TextChunk> chunks = List.of(
                createTestChunk(TEST_CLASS_NAME, ENTITY_TYPE_CLASS, LANGUAGE_JAVA, TEST_FILE_1,
                        "public class TestClass implements Serializable"),
                createTestChunk(TEST_SERVICE_NAME, ENTITY_TYPE_CLASS, LANGUAGE_JAVA, TEST_FILE_2,
                        "public class UserService { private String name; }")
        );

        indexService.addChunks(chunks).await().indefinitely();

        // When - search for "class" (should match both)
        var result = indexService.search("class", 10).subscribe().withSubscriber(UniAssertSubscriber.create());

        // Then
        result.assertCompleted();
        List<?> searchResults = result.getItem();
        assertNotNull(searchResults);
        assertEquals(2, searchResults.size());
    }

    @Test
    public void testSearchNoResults() {
        // Given
        List<TextChunk> chunks = List.of(
                createTestChunk(TEST_CLASS_NAME, ENTITY_TYPE_CLASS, LANGUAGE_JAVA, TEST_FILE_1, TEST_CLASS_CONTENT)
        );

        indexService.addChunks(chunks).await().indefinitely();

        // When - search for non-existent term
        var result = indexService.search("nonexistent", 10).subscribe().withSubscriber(UniAssertSubscriber.create());

        // Then
        result.assertCompleted();
        List<?> searchResults = result.getItem();
        assertNotNull(searchResults);
        assertTrue(searchResults.isEmpty());
    }

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

        // Search for the doc summary content
        var result = indexService.search("demonstration", 10).subscribe().withSubscriber(UniAssertSubscriber.create());
        result.assertCompleted();
        List<?> searchResults = result.getItem();
        assertEquals(1, searchResults.size());
    }

    @Test
    public void testRepositoryExtraction() {
        // Given
        TextChunk chunk = createTestChunk(TEST_CLASS_NAME, ENTITY_TYPE_CLASS, LANGUAGE_JAVA,
                "/home/user/projects/myproject/src/main/java/TestClass.java", TEST_CLASS_CONTENT);

        // When
        indexService.addChunks(List.of(chunk)).await().indefinitely();

        // Then - should extract "myproject" as repository name
        var searchResult = indexService.search("myproject", 10).subscribe().withSubscriber(UniAssertSubscriber.create());
        searchResult.assertCompleted();
        List<?> results = searchResult.getItem();
        assertEquals(1, results.size());
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
