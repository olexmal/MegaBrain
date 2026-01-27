/*
 * Copyright (c) 2025 MegaBrain Contributors
 * Licensed under the MIT License - see LICENSE file for details.
 */

package io.megabrain.core;

import io.megabrain.ingestion.parser.TextChunk;
import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;
import org.apache.lucene.document.Document;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.assertj.core.api.Assertions.assertThat;
import org.assertj.core.data.Offset;

/**
 * Unit tests for LuceneIndexService.
 * <p>
 * Uses temporary directories for testing to avoid file system conflicts.
 */
class LuceneIndexServiceTest {

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
        Files.createDirectories(testIndexDir);

        // Create a test-specific index service with temporary directory
        indexService = new TestLuceneIndexService();
        indexService.indexDirectoryPath = testIndexDir.toString();

        // Manually inject dependencies for testing
        QueryParserService queryParser = new QueryParserService();
        queryParser.analyzer = new CodeAwareAnalyzer(); // Inject the analyzer
        queryParser.initialize();
        indexService.queryParser = queryParser;

        // Initialize manually instead of using CDI lifecycle
        indexService.initialize();
    }

    @AfterEach
    void tearDown() {
        if (indexService != null) {
            indexService.shutdown();
        }
    }

    @Test
    void testBasicFunctionality() {
        // Simple test to verify test framework works
        assertNotNull(indexService);
        assertNotNull(tempDir);
    }

    @Test
    void testAddEmptyChunks() {
        // When
        indexService.addChunks(List.of()).await().indefinitely();

        // Then
        assertIndexContains(0);
    }

    @Test
    void testRemoveChunksForFile() {
        // Given
        List<TextChunk> chunks = List.of(
                createTestChunk(TEST_CLASS_NAME, ENTITY_TYPE_CLASS, LANGUAGE_JAVA, TEST_FILE_1, TEST_CLASS_CONTENT),
                createTestChunk(TEST_METHOD_NAME, ENTITY_TYPE_METHOD, LANGUAGE_JAVA, TEST_FILE_1, TEST_METHOD_CONTENT)
        );

        indexService.addChunks(chunks).await().indefinitely();

        // When
        indexService.removeChunksForFile(TEST_FILE_1).await().indefinitely();

        // Then
        // Verify that all documents for the file were removed
        assertIndexContains(0);
    }

    @Test
    void testRemoveChunksForNonExistentFile() {
        // When
        indexService.removeChunksForFile("/non/existent/file.java").await().indefinitely();

        // Then
        // Should complete without error even for non-existent files
    }

    @Test
    void testUpdateChunksForFile() {
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
        indexService.updateChunksForFile(TEST_FILE_1, updatedChunks).await().indefinitely();

        // Then
        // Should have the updated chunks (old ones removed, new ones added)
        LuceneIndexService.IndexStats stats = indexService.getIndexStats().await().indefinitely();
        assertTrue(stats.numDocs() >= 1); // At least the updated chunks
    }

    @Test
    void testSearchWithQueryParser() {
        // Given
        List<TextChunk> chunks = List.of(
                createTestChunk(TEST_CLASS_NAME, ENTITY_TYPE_CLASS, LANGUAGE_JAVA, TEST_FILE_1, TEST_CLASS_CONTENT),
                createTestChunk(TEST_METHOD_NAME, ENTITY_TYPE_METHOD, LANGUAGE_JAVA, TEST_FILE_1, TEST_METHOD_CONTENT)
        );

        indexService.addChunks(chunks).await().indefinitely();

        // When - search for "TestClass" (entity name) which should match the first chunk
        List<Document> documents = indexService.search("TestClass", 10).await().indefinitely();

        // Then
        assertThat(documents).isNotEmpty();
        assertThat(documents.getFirst().get(LuceneSchema.FIELD_ENTITY_NAME)).isEqualTo(TEST_CLASS_NAME);
    }

    @Test
    void testSearchFieldSpecific() {
        // Given
        List<TextChunk> chunks = List.of(
                createTestChunk(TEST_CLASS_NAME, ENTITY_TYPE_CLASS, LANGUAGE_JAVA, TEST_FILE_1, TEST_CLASS_CONTENT),
                createTestChunk(TEST_METHOD_NAME, ENTITY_TYPE_METHOD, LANGUAGE_JAVA, TEST_FILE_1, TEST_METHOD_CONTENT)
        );

        indexService.addChunks(chunks).await().indefinitely();

        // When - search for "java" in language field
        List<Document> documents = indexService.searchField(LuceneSchema.FIELD_LANGUAGE, "java", 10).await().indefinitely();

        // Then
        assertThat(documents).hasSize(2); // Both chunks have language=java
    }

    @Test
    void testSearchEmptyIndex() {
        // When
        List<Document> documents = indexService.search("anything", 10).await().indefinitely();

        // Then
        assertThat(documents).isEmpty();
    }

    @Test
    void testGetIndexStats() {
        // Given
        List<TextChunk> chunks = List.of(
                createTestChunk(TEST_CLASS_NAME, ENTITY_TYPE_CLASS, LANGUAGE_JAVA, TEST_FILE_1, TEST_CLASS_CONTENT),
                createTestChunk(TEST_SERVICE_NAME, ENTITY_TYPE_CLASS, LANGUAGE_JAVA, TEST_FILE_2, "public class UserService")
        );

        indexService.addChunks(chunks).await().indefinitely();

        // When
        LuceneIndexService.IndexStats stats = indexService.getIndexStats().await().indefinitely();

        // Then
        assertNotNull(stats);
        assertEquals(2, stats.numDocs());
        assertEquals(2, stats.maxDoc());
        assertEquals(0, stats.numDeletedDocs());
    }

    @Test
    void testIndexStatsAfterDeletion() {
        // Given
        List<TextChunk> chunks = List.of(
                createTestChunk(TEST_CLASS_NAME, ENTITY_TYPE_CLASS, LANGUAGE_JAVA, TEST_FILE_1, TEST_CLASS_CONTENT),
                createTestChunk(TEST_SERVICE_NAME, ENTITY_TYPE_CLASS, LANGUAGE_JAVA, TEST_FILE_2, "public class UserService")
        );

        indexService.addChunks(chunks).await().indefinitely();
        indexService.removeChunksForFile(TEST_FILE_1).await().indefinitely();

        // When
        LuceneIndexService.IndexStats stats = indexService.getIndexStats().await().indefinitely();

        // Then
        assertNotNull(stats);
        assertEquals(1, stats.numDocs());
        assertTrue(stats.maxDoc() >= stats.numDocs());
        assertEquals(stats.maxDoc() - stats.numDocs(), stats.numDeletedDocs());
    }

    @Test
    void testChunkWithAttributes() {
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
    void testRepositoryExtraction() {
        // Given - test that chunks with repository paths can be indexed
        TextChunk chunk = createTestChunk(TEST_CLASS_NAME, ENTITY_TYPE_CLASS, LANGUAGE_JAVA,
                "/home/user/projects/myproject/src/main/java/TestClass.java", TEST_CLASS_CONTENT);

        // When
        indexService.addChunks(List.of(chunk)).await().indefinitely();

        // Then - should have indexed the chunk successfully
        assertIndexContains(1);
    }

    @Test
    void testAddChunksBatch_withSmallBatchSize() {
        // Given
        List<TextChunk> chunks = List.of(
                createTestChunk("Class1", ENTITY_TYPE_CLASS, LANGUAGE_JAVA, TEST_FILE_1, "class Class1"),
                createTestChunk("Class2", ENTITY_TYPE_CLASS, LANGUAGE_JAVA, TEST_FILE_1, "class Class2"),
                createTestChunk("Class3", ENTITY_TYPE_CLASS, LANGUAGE_JAVA, TEST_FILE_1, "class Class3")
        );

        // When
        indexService.addChunksBatch(chunks, 2).await().indefinitely();

        // Then
        assertIndexContains(3);
    }

    @Test
    void testAddChunksBatch_withLargeBatchSize() {
        // Given
        List<TextChunk> chunks = List.of(
                createTestChunk("Class1", ENTITY_TYPE_CLASS, LANGUAGE_JAVA, TEST_FILE_1, "class Class1"),
                createTestChunk("Class2", ENTITY_TYPE_CLASS, LANGUAGE_JAVA, TEST_FILE_1, "class Class2")
        );

        // When
        indexService.addChunksBatch(chunks, 10).await().indefinitely(); // batch size larger than chunk count

        // Then
        assertIndexContains(2);
    }

    @Test
    void testAddChunksBatch_withEmptyList() {
        // When
        indexService.addChunksBatch(List.of(), 5).await().indefinitely();

        // Then
        assertIndexContains(0);
    }

    @Test
    void testUpdateDocument_singleDocument() {
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
    void testUpdateDocuments_multipleDocuments() {
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
        indexService.updateDocuments(updatedChunks).await().indefinitely();

        // Then
        assertIndexContains(2); // Should still have 2 documents
    }

    @Test
    void testUpdateDocumentsBatch_withBatchSize() {
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
        indexService.updateDocumentsBatch(updatedChunks, 2).await().indefinitely();

        // Then
        assertIndexContains(5);
    }

    @Test
    void testRemoveDocument_byDocumentId() {
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
        result.assertItem(4);
        assertIndexContains(0);
    }

    @Test
    void testRemoveDocument_nonExistentId() {
        // When
        var result = indexService.removeDocument("non:existent:id")
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        // Then
        result.assertCompleted();
        result.assertItem(1);
    }

    @Test
    void testRemoveDocuments_multipleIds() {
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
        result.assertItem(5);
        assertIndexContains(0);
    }

    @Test
    void testRemoveDocuments_emptyList() {
        // When
        var result = indexService.removeDocuments(List.of())
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        // Then
        result.assertCompleted();
        result.assertItem(0);
    }

    @Test
    void testUpdateChunksForFileBatch_withBatchSize() {
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
        indexService.updateChunksForFileBatch(TEST_FILE_1, updatedChunks, 1).await().indefinitely();

        // Then
        assertIndexContains(2); // Should have new chunks after removing old ones
    }

    // ===== COMPREHENSIVE SEARCH TESTS =====

    @Test
    void testSearchBooleanAndQuery() {
        // Given
        List<TextChunk> chunks = List.of(
                createTestChunk("UserService", ENTITY_TYPE_CLASS, LANGUAGE_JAVA, TEST_FILE_2,
                        "public class UserService implements Service"),
                createTestChunk("DataService", ENTITY_TYPE_CLASS, LANGUAGE_JAVA, TEST_FILE_1,
                        "public class DataService implements Service")
        );
        indexService.addChunks(chunks).await().indefinitely();

        // When - search for "Service AND Data" (should match only DataService)
        List<Document> documents = indexService.search("Service AND Data", 10).await().indefinitely();

        // Then
        assertThat(documents).hasSize(1);
        assertThat(documents.getFirst().get(LuceneSchema.FIELD_ENTITY_NAME)).isEqualTo("DataService");
    }

    @Test
    void testSearchBooleanOrQuery() {
        // Given
        List<TextChunk> chunks = List.of(
                createTestChunk("UserService", ENTITY_TYPE_CLASS, LANGUAGE_JAVA, TEST_FILE_2,
                        "public class UserService implements Service"),
                createTestChunk("DataService", ENTITY_TYPE_CLASS, LANGUAGE_JAVA, TEST_FILE_1,
                        "public class DataService implements Repository")
        );
        indexService.addChunks(chunks).await().indefinitely();

        // When - search for "Service OR Repository" (should match both)
        List<Document> documents = indexService.search("Service OR Repository", 10).await().indefinitely();

        // Then
        assertThat(documents).hasSize(2);
    }

    @Test
    void testSearchBooleanNotQuery() {
        // Given
        List<TextChunk> chunks = List.of(
                createTestChunk("UserService", ENTITY_TYPE_CLASS, LANGUAGE_JAVA, TEST_FILE_2,
                        "public class UserService implements Service"),
                createTestChunk("DataService", ENTITY_TYPE_CLASS, LANGUAGE_JAVA, TEST_FILE_1,
                        "public class DataService implements Repository")
        );
        indexService.addChunks(chunks).await().indefinitely();

        // When - search for "Service NOT Data" (should match only UserService)
        List<Document> documents = indexService.search("Service NOT Data", 10).await().indefinitely();

        // Then
        assertThat(documents).hasSize(1);
        assertThat(documents.getFirst().get(LuceneSchema.FIELD_ENTITY_NAME)).isEqualTo("UserService");
    }

    @Test
    void testSearchPhraseQuery() {
        // Given
        List<TextChunk> chunks = List.of(
                createTestChunk("UserService", ENTITY_TYPE_CLASS, LANGUAGE_JAVA, TEST_FILE_2,
                        "user service handles data processing"),
                createTestChunk("DataService", ENTITY_TYPE_CLASS, LANGUAGE_JAVA, TEST_FILE_1,
                        "data service manages database operations")
        );
        indexService.addChunks(chunks).await().indefinitely();

        // When - search for exact phrase "user service"
        List<Document> documents = indexService.search("\"user service\"", 10).await().indefinitely();

        // Then - should only match the document containing the exact phrase
        assertThat(documents).hasSize(1);
        assertThat(documents.getFirst().get(LuceneSchema.FIELD_ENTITY_NAME)).isEqualTo("UserService");
        assertThat(documents.getFirst().get(LuceneSchema.FIELD_CONTENT)).contains("user service");
    }

    @Test
    void testSearchWildcardQuery() {
        // Given
        List<TextChunk> chunks = List.of(
                createTestChunk("UserService", ENTITY_TYPE_CLASS, LANGUAGE_JAVA, TEST_FILE_2,
                        "public class UserService implements Service"),
                createTestChunk("DataService", ENTITY_TYPE_CLASS, LANGUAGE_JAVA, TEST_FILE_1,
                        "public class DataService implements Repository"),
                createTestChunk("TestService", ENTITY_TYPE_CLASS, LANGUAGE_JAVA, TEST_FILE_1,
                        "public class TestService implements Service")
        );
        indexService.addChunks(chunks).await().indefinitely();

        // When - search for "*Service" (should match all three)
        List<Document> documents = indexService.search("*Service", 10).await().indefinitely();

        // Then
        assertThat(documents).hasSize(3);
    }

    @Test
    void testSearchFieldSpecificQuery() {
        // Given
        List<TextChunk> chunks = List.of(
                createTestChunk("UserService", ENTITY_TYPE_CLASS, LANGUAGE_JAVA, TEST_FILE_2,
                        "public class UserService implements Service"),
                createTestChunk("DataService", ENTITY_TYPE_CLASS, "python", TEST_FILE_1,
                        "class DataService:")
        );
        indexService.addChunks(chunks).await().indefinitely();

        // When - search for "java" in language field
        List<Document> documents = indexService.search("language:java", 10).await().indefinitely();

        // Then
        assertThat(documents).hasSize(1);
        assertThat(documents.getFirst().get(LuceneSchema.FIELD_ENTITY_NAME)).isEqualTo("UserService");
    }

    @Test
    void testSearchEntityNameBoosting() {
        // Given - create chunks where "test" appears in both entity name and content
        List<TextChunk> chunks = List.of(
                createTestChunk("TestClass", ENTITY_TYPE_CLASS, LANGUAGE_JAVA, TEST_FILE_1,
                        "public class TestClass implements Runnable"), // "test" in entity name (boosted)
                createTestChunk("UserService", ENTITY_TYPE_CLASS, LANGUAGE_JAVA, TEST_FILE_2,
                        "public class UserService implements TestInterface") // "test" in content (not boosted)
        );
        indexService.addChunks(chunks).await().indefinitely();

        // When - search for "test"
        List<Document> documents = indexService.search("test", 10).await().indefinitely();

        // Then - TestClass should rank higher due to entity name boosting
        assertThat(documents).hasSize(2);
        assertThat(documents.getFirst().get(LuceneSchema.FIELD_ENTITY_NAME)).isEqualTo("TestClass");
    }

    @Test
    void testSearchDocSummaryBoosting() {
        // Given
        Map<String, String> attributesWithSummary = Map.of("doc_summary", "This is a test summary");
        TextChunk chunkWithSummary = new TextChunk(
                "public class SummaryClass {}", LANGUAGE_JAVA, ENTITY_TYPE_CLASS, "SummaryClass",
                TEST_FILE_1, 1, 10, 0, 50, attributesWithSummary
        );

        TextChunk chunkWithoutSummary = createTestChunk("RegularClass", ENTITY_TYPE_CLASS,
                LANGUAGE_JAVA, TEST_FILE_2, "public class RegularClass {}");

        indexService.addChunks(List.of(chunkWithSummary, chunkWithoutSummary)).await().indefinitely();

        // When - search for "test"
        List<Document> documents = indexService.search("test", 10).await().indefinitely();

        // Then - chunk with summary should rank higher due to doc_summary boosting
        assertThat(documents).hasSize(1); // Only the summary chunk should match
        assertThat(documents.getFirst().get(LuceneSchema.FIELD_ENTITY_NAME)).isEqualTo("SummaryClass");
    }

    @Test
    void testSearchCamelCaseSplitting() {
        // Given
        List<TextChunk> chunks = List.of(
                createTestChunk("UserService", ENTITY_TYPE_CLASS, LANGUAGE_JAVA, TEST_FILE_2,
                        "public class UserService implements Service"),
                createTestChunk("getUserName", ENTITY_TYPE_METHOD, LANGUAGE_JAVA, TEST_FILE_1,
                        "public String getUserName() { return name; }")
        );
        indexService.addChunks(chunks).await().indefinitely();

        // When - search for "user" (should match both via camelCase splitting)
        List<Document> documents = indexService.search("user", 10).await().indefinitely();

        // Then
        assertThat(documents).hasSize(2);
    }

    @Test
    void testSearchSnakeCaseSplitting() {
        // Given
        List<TextChunk> chunks = List.of(
                createTestChunk("UserService", ENTITY_TYPE_CLASS, LANGUAGE_JAVA, TEST_FILE_2,
                        "public class UserService implements Service"),
                createTestChunk("get_user_name", ENTITY_TYPE_METHOD, LANGUAGE_JAVA, TEST_FILE_1,
                        "public String get_user_name() { return name; }")
        );
        indexService.addChunks(chunks).await().indefinitely();

        // When - search for "user" (should match both via snake_case splitting)
        List<Document> documents = indexService.search("user", 10).await().indefinitely();

        // Then
        assertThat(documents).hasSize(2);
    }

    @Test
    void testSearchEmptyQuery() {
        // Given
        List<TextChunk> chunks = List.of(
                createTestChunk(TEST_CLASS_NAME, ENTITY_TYPE_CLASS, LANGUAGE_JAVA, TEST_FILE_1, TEST_CLASS_CONTENT)
        );
        indexService.addChunks(chunks).await().indefinitely();

        // When - search with empty query (should return match-all query)
        List<Document> documents = indexService.search("", 10).await().indefinitely();

        // Then - should return all indexed documents for empty query
        assertThat(documents).hasSize(1);
        assertThat(documents.getFirst().get(LuceneSchema.FIELD_ENTITY_NAME)).isEqualTo(TEST_CLASS_NAME);
    }

    @Test
    void testSearchNullQuery() {
        // Given
        List<TextChunk> chunks = List.of(
                createTestChunk(TEST_CLASS_NAME, ENTITY_TYPE_CLASS, LANGUAGE_JAVA, TEST_FILE_1, TEST_CLASS_CONTENT)
        );
        indexService.addChunks(chunks).await().indefinitely();

        // When - search with null query (should return match-all query)
        List<Document> documents = indexService.search(null, 10).await().indefinitely();

        // Then - should return all indexed documents for null query
        assertThat(documents).hasSize(1);
        assertThat(documents.getFirst().get(LuceneSchema.FIELD_ENTITY_NAME)).isEqualTo(TEST_CLASS_NAME);
    }

    @Test
    void testSearchMalformedQuery() {
        // Given
        List<TextChunk> chunks = List.of(
                createTestChunk(TEST_CLASS_NAME, ENTITY_TYPE_CLASS, LANGUAGE_JAVA, TEST_FILE_1, TEST_CLASS_CONTENT)
        );
        indexService.addChunks(chunks).await().indefinitely();

        // When - search with malformed query (unclosed quote)
        List<Document> documents = indexService.search("\"unclosed quote", 10).await().indefinitely();

        // Then - should handle gracefully and return some results (fallback parsing)
        // The exact behavior depends on QueryParserService error handling
        assertThat(documents).isNotNull(); // Should not crash
    }

    @Test
    void testSearchWithLimit() {
        // Given - create many chunks
        List<TextChunk> chunks = List.of(
                createTestChunk("Class1", ENTITY_TYPE_CLASS, LANGUAGE_JAVA, TEST_FILE_1, "public class Class1"),
                createTestChunk("Class2", ENTITY_TYPE_CLASS, LANGUAGE_JAVA, TEST_FILE_1, "public class Class2"),
                createTestChunk("Class3", ENTITY_TYPE_CLASS, LANGUAGE_JAVA, TEST_FILE_1, "public class Class3"),
                createTestChunk("Class4", ENTITY_TYPE_CLASS, LANGUAGE_JAVA, TEST_FILE_1, "public class Class4"),
                createTestChunk("Class5", ENTITY_TYPE_CLASS, LANGUAGE_JAVA, TEST_FILE_1, "public class Class5")
        );
        indexService.addChunks(chunks).await().indefinitely();

        // When - search with limit of 3
        List<Document> documents = indexService.search("class", 3).await().indefinitely();

        // Then - should return at most 3 results
        assertThat(documents).hasSizeLessThanOrEqualTo(3);
    }

    @Test
    void testSearchFieldWithNonExistentField() {
        // Given
        List<TextChunk> chunks = List.of(
                createTestChunk(TEST_CLASS_NAME, ENTITY_TYPE_CLASS, LANGUAGE_JAVA, TEST_FILE_1, TEST_CLASS_CONTENT)
        );
        indexService.addChunks(chunks).await().indefinitely();

        // When - search in non-existent field
        List<Document> documents = indexService.searchField("nonexistent", "java", 10).await().indefinitely();

        // Then - should handle gracefully
        assertThat(documents).isNotNull(); // Should not crash
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

    // ===== SCORE NORMALIZATION TESTS =====

    @Test
    void testNormalizeScores_emptyList() {
        // When
        List<LuceneIndexService.LuceneScoredResult> normalized = LuceneIndexService.normalizeScores(List.of());

        // Then
        assertThat(normalized).isEmpty();
    }

    @Test
    void testNormalizeScores_nullList() {
        // When
        List<LuceneIndexService.LuceneScoredResult> normalized = LuceneIndexService.normalizeScores(null);

        // Then
        assertThat(normalized).isEmpty();
    }

    @Test
    void testNormalizeScores_singleResult() {
        // Given
        Document doc = new Document();
        doc.add(new org.apache.lucene.document.Field("test", "test", org.apache.lucene.document.TextField.TYPE_STORED));
        LuceneIndexService.LuceneScoredResult result = new LuceneIndexService.LuceneScoredResult(doc, 0.8f);

        // When
        List<LuceneIndexService.LuceneScoredResult> normalized = LuceneIndexService.normalizeScores(List.of(result));

        // Then
        assertThat(normalized).hasSize(1);
        assertThat(normalized.get(0).score()).isEqualTo(1.0f);
        assertThat(normalized.get(0).document()).isSameAs(doc);
    }

    @Test
    void testNormalizeScores_multipleResults() {
        // Given - create test documents with different scores
        Document doc1 = new Document();
        doc1.add(new org.apache.lucene.document.Field("content", "doc1", org.apache.lucene.document.TextField.TYPE_STORED));

        Document doc2 = new Document();
        doc2.add(new org.apache.lucene.document.Field("content", "doc2", org.apache.lucene.document.TextField.TYPE_STORED));

        Document doc3 = new Document();
        doc3.add(new org.apache.lucene.document.Field("content", "doc3", org.apache.lucene.document.TextField.TYPE_STORED));

        List<LuceneIndexService.LuceneScoredResult> results = List.of(
            new LuceneIndexService.LuceneScoredResult(doc1, 0.5f),  // min score
            new LuceneIndexService.LuceneScoredResult(doc2, 0.8f),  // middle score
            new LuceneIndexService.LuceneScoredResult(doc3, 1.0f)   // max score
        );

        // When
        List<LuceneIndexService.LuceneScoredResult> normalized = LuceneIndexService.normalizeScores(results);

        // Then
        assertThat(normalized).hasSize(3);

        // Check normalized scores: (score - min) / (max - min)
        // doc1: (0.5 - 0.5) / (1.0 - 0.5) = 0.0 / 0.5 = 0.0
        assertThat(normalized.get(0).score()).isEqualTo(0.0f);

        // doc2: (0.8 - 0.5) / (1.0 - 0.5) = 0.3 / 0.5 = 0.6
        assertThat(normalized.get(1).score()).isEqualTo(0.6f);

        // doc3: (1.0 - 0.5) / (1.0 - 0.5) = 0.5 / 0.5 = 1.0
        assertThat(normalized.get(2).score()).isEqualTo(1.0f);
    }

    @Test
    void testNormalizeScores_equalScores() {
        // Given - all results have the same score
        Document doc1 = new Document();
        Document doc2 = new Document();
        Document doc3 = new Document();

        List<LuceneIndexService.LuceneScoredResult> results = List.of(
            new LuceneIndexService.LuceneScoredResult(doc1, 0.7f),
            new LuceneIndexService.LuceneScoredResult(doc2, 0.7f),
            new LuceneIndexService.LuceneScoredResult(doc3, 0.7f)
        );

        // When
        List<LuceneIndexService.LuceneScoredResult> normalized = LuceneIndexService.normalizeScores(results);

        // Then - all should get score 1.0 since they're equally relevant
        assertThat(normalized).hasSize(3);
        assertThat(normalized.get(0).score()).isEqualTo(1.0f);
        assertThat(normalized.get(1).score()).isEqualTo(1.0f);
        assertThat(normalized.get(2).score()).isEqualTo(1.0f);
    }

    @Test
    void testNormalizeScores_zeroScores() {
        // Given - results with zero scores
        Document doc1 = new Document();
        Document doc2 = new Document();

        List<LuceneIndexService.LuceneScoredResult> results = List.of(
            new LuceneIndexService.LuceneScoredResult(doc1, 0.0f),
            new LuceneIndexService.LuceneScoredResult(doc2, 0.2f)
        );

        // When
        List<LuceneIndexService.LuceneScoredResult> normalized = LuceneIndexService.normalizeScores(results);

        // Then - normalization should handle zero scores correctly
        assertThat(normalized).hasSize(2);
        assertThat(normalized.get(0).score()).isEqualTo(0.0f);  // (0.0 - 0.0) / (0.2 - 0.0) = 0.0
        assertThat(normalized.get(1).score()).isEqualTo(1.0f);  // (0.2 - 0.0) / (0.2 - 0.0) = 1.0
    }

    @Test
    void testNormalizeScores_negativeScores() {
        // Given - results with negative scores (edge case, though Lucene typically doesn't produce negative scores)
        Document doc1 = new Document();
        Document doc2 = new Document();

        List<LuceneIndexService.LuceneScoredResult> results = List.of(
            new LuceneIndexService.LuceneScoredResult(doc1, -0.1f),  // negative score
            new LuceneIndexService.LuceneScoredResult(doc2, 0.9f)    // positive score
        );

        // When
        List<LuceneIndexService.LuceneScoredResult> normalized = LuceneIndexService.normalizeScores(results);

        // Then - should normalize correctly even with negative scores
        assertThat(normalized).hasSize(2);
        assertThat(normalized.get(0).score()).isEqualTo(0.0f);  // (-0.1 - (-0.1)) / (0.9 - (-0.1)) = 0.0
        assertThat(normalized.get(1).score()).isEqualTo(1.0f);  // (0.9 - (-0.1)) / (0.9 - (-0.1)) = 1.0
    }

    @Test
    void testNormalizeScores_preservesOrder() {
        // Given - results in specific order
        Document doc1 = new Document();
        doc1.add(new org.apache.lucene.document.Field("id", "1", org.apache.lucene.document.TextField.TYPE_STORED));

        Document doc2 = new Document();
        doc2.add(new org.apache.lucene.document.Field("id", "2", org.apache.lucene.document.TextField.TYPE_STORED));

        Document doc3 = new Document();
        doc3.add(new org.apache.lucene.document.Field("id", "3", org.apache.lucene.document.TextField.TYPE_STORED));

        List<LuceneIndexService.LuceneScoredResult> results = List.of(
            new LuceneIndexService.LuceneScoredResult(doc1, 0.3f),
            new LuceneIndexService.LuceneScoredResult(doc2, 0.6f),
            new LuceneIndexService.LuceneScoredResult(doc3, 0.9f)
        );

        // When
        List<LuceneIndexService.LuceneScoredResult> normalized = LuceneIndexService.normalizeScores(results);

        // Then - order should be preserved, scores normalized
        assertThat(normalized).hasSize(3);
        assertThat(normalized.get(0).document().get("id")).isEqualTo("1");
        assertThat(normalized.get(0).score()).isEqualTo(0.0f);  // (0.3 - 0.3) / (0.9 - 0.3) = 0.0

        assertThat(normalized.get(1).document().get("id")).isEqualTo("2");
        assertThat(normalized.get(1).score()).isCloseTo(0.5f, Offset.offset(0.001f));  // (0.6 - 0.3) / (0.9 - 0.3) = 0.5

        assertThat(normalized.get(2).document().get("id")).isEqualTo("3");
        assertThat(normalized.get(2).score()).isEqualTo(1.0f);  // (0.9 - 0.3) / (0.9 - 0.3) = 1.0
    }

    @Test
    void testSearchWithScores_returnsScoredResults() {
        // Given
        List<TextChunk> chunks = List.of(
                createTestChunk(TEST_CLASS_NAME, ENTITY_TYPE_CLASS, LANGUAGE_JAVA, TEST_FILE_1, TEST_CLASS_CONTENT),
                createTestChunk(TEST_METHOD_NAME, ENTITY_TYPE_METHOD, LANGUAGE_JAVA, TEST_FILE_1, TEST_METHOD_CONTENT)
        );

        indexService.addChunks(chunks).await().indefinitely();

        // When - search with scores (use field-specific query to ensure match)
        List<LuceneIndexService.LuceneScoredResult> scoredResults =
                indexService.searchWithScores("entity_name:TestClass", 10).await().indefinitely();

        // Then
        assertThat(scoredResults).isNotEmpty();
        // Score should be positive (Lucene gives scores > 0 for matches)
        assertThat(scoredResults.get(0).score()).isGreaterThanOrEqualTo(0.0f);
        assertThat(scoredResults.get(0).document().get(LuceneSchema.FIELD_ENTITY_NAME)).isEqualTo(TEST_CLASS_NAME);
    }

    @Test
    void testSearchWithScores_emptyIndex() {
        // When
        List<LuceneIndexService.LuceneScoredResult> scoredResults =
                indexService.searchWithScores("anything", 10).await().indefinitely();

        // Then
        assertThat(scoredResults).isEmpty();
    }

    // ===== FILTER INTEGRATION TESTS (US-02-04, T2) =====

    @Test
    void testSearchWithScores_languageFilter() {
        List<TextChunk> chunks = List.of(
                createTestChunk("JavaClass", ENTITY_TYPE_CLASS, "java", TEST_FILE_1, "public class JavaClass"),
                createTestChunk("PythonClass", ENTITY_TYPE_CLASS, "python", TEST_FILE_2, "class PythonClass:")
        );
        indexService.addChunks(chunks).await().indefinitely();

        SearchFilters filters = new SearchFilters(List.of("java"), List.of(), List.of(), List.of());
        List<LuceneIndexService.LuceneScoredResult> results =
                indexService.searchWithScores("entity_name:JavaClass", 10, filters).await().indefinitely();

        assertThat(results).hasSize(1);
        assertThat(results.get(0).document().get(LuceneSchema.FIELD_LANGUAGE)).isEqualTo("java");
    }

    @Test
    void testSearchWithScores_entityTypeFilter() {
        List<TextChunk> chunks = List.of(
                createTestChunk(TEST_CLASS_NAME, ENTITY_TYPE_CLASS, LANGUAGE_JAVA, TEST_FILE_1, TEST_CLASS_CONTENT),
                createTestChunk(TEST_METHOD_NAME, ENTITY_TYPE_METHOD, LANGUAGE_JAVA, TEST_FILE_1, TEST_METHOD_CONTENT)
        );
        indexService.addChunks(chunks).await().indefinitely();

        SearchFilters filters = new SearchFilters(List.of(), List.of(), List.of(), List.of("class"));
        List<LuceneIndexService.LuceneScoredResult> results =
                indexService.searchWithScores("entity_name:TestClass", 10, filters).await().indefinitely();

        assertThat(results).hasSize(1);
        assertThat(results.get(0).document().get(LuceneSchema.FIELD_ENTITY_TYPE)).isEqualTo("class");
    }

    @Test
    void testSearchWithScores_filePathPrefixFilter() {
        String path = "src/main/java/Foo.java";
        List<TextChunk> chunks = List.of(
                createTestChunk("Foo", ENTITY_TYPE_CLASS, LANGUAGE_JAVA, path, "public class Foo"),
                createTestChunk("Bar", ENTITY_TYPE_CLASS, LANGUAGE_JAVA, "test/java/Bar.java", "public class Bar")
        );
        indexService.addChunks(chunks).await().indefinitely();

        SearchFilters filters = new SearchFilters(List.of(), List.of(), List.of("src/main"), List.of());
        List<LuceneIndexService.LuceneScoredResult> results =
                indexService.searchWithScores("entity_name:Foo", 10, filters).await().indefinitely();

        assertThat(results).hasSize(1);
        assertThat(results.get(0).document().get(LuceneSchema.FIELD_FILE_PATH)).isEqualTo(path);
    }

    @Test
    void testSearchWithScores_repositoryFilter() {
        String pathWithRepo = "/home/user/projects/myproject/src/main/java/RepoClass.java";
        List<TextChunk> chunks = List.of(
                createTestChunk("RepoClass", ENTITY_TYPE_CLASS, LANGUAGE_JAVA, pathWithRepo, "public class RepoClass"),
                createTestChunk("OtherClass", ENTITY_TYPE_CLASS, LANGUAGE_JAVA, TEST_FILE_1, "public class OtherClass")
        );
        indexService.addChunks(chunks).await().indefinitely();

        SearchFilters filters = new SearchFilters(List.of(), List.of("myproject"), List.of(), List.of());
        List<LuceneIndexService.LuceneScoredResult> results =
                indexService.searchWithScores("entity_name:RepoClass", 10, filters).await().indefinitely();

        assertThat(results).hasSize(1);
        assertThat(results.get(0).document().get(LuceneSchema.FIELD_REPOSITORY)).isEqualTo("myproject");
    }

    @Test
    void testSearchWithScores_combinedFilters() {
        List<TextChunk> chunks = List.of(
                createTestChunk("JavaClass", ENTITY_TYPE_CLASS, "java", TEST_FILE_1, "public class JavaClass"),
                createTestChunk("JavaMethod", ENTITY_TYPE_METHOD, "java", TEST_FILE_1, "void run()"),
                createTestChunk("PythonClass", ENTITY_TYPE_CLASS, "python", TEST_FILE_2, "class PythonClass:")
        );
        indexService.addChunks(chunks).await().indefinitely();

        SearchFilters filters = new SearchFilters(
                List.of("java"), List.of(), List.of(), List.of("class"));
        List<LuceneIndexService.LuceneScoredResult> results =
                indexService.searchWithScores("entity_name:JavaClass", 10, filters).await().indefinitely();

        assertThat(results).hasSize(1);
        assertThat(results.get(0).document().get(LuceneSchema.FIELD_LANGUAGE)).isEqualTo("java");
        assertThat(results.get(0).document().get(LuceneSchema.FIELD_ENTITY_TYPE)).isEqualTo("class");
    }

    @Test
    void testSearchWithScores_filterExcludesAll() {
        List<TextChunk> chunks = List.of(
                createTestChunk("JavaClass", ENTITY_TYPE_CLASS, "java", TEST_FILE_1, "public class JavaClass")
        );
        indexService.addChunks(chunks).await().indefinitely();

        SearchFilters filters = new SearchFilters(List.of("python"), List.of(), List.of(), List.of());
        List<LuceneIndexService.LuceneScoredResult> results =
                indexService.searchWithScores("class", 10, filters).await().indefinitely();

        assertThat(results).isEmpty();
    }

    @Test
    void testSearchWithScores_nullFiltersSameAsNoFilters() {
        List<TextChunk> chunks = List.of(
                createTestChunk(TEST_CLASS_NAME, ENTITY_TYPE_CLASS, LANGUAGE_JAVA, TEST_FILE_1, TEST_CLASS_CONTENT)
        );
        indexService.addChunks(chunks).await().indefinitely();

        List<LuceneIndexService.LuceneScoredResult> withNull =
                indexService.searchWithScores("entity_name:TestClass", 10, null).await().indefinitely();
        List<LuceneIndexService.LuceneScoredResult> noFilters =
                indexService.searchWithScores("entity_name:TestClass", 10).await().indefinitely();

        assertThat(withNull).hasSize(1);
        assertThat(noFilters).hasSize(1);
        assertThat(withNull.get(0).document().get(LuceneSchema.FIELD_ENTITY_NAME))
                .isEqualTo(noFilters.get(0).document().get(LuceneSchema.FIELD_ENTITY_NAME));
    }

    @Test
    void testComputeFacets_returnsCounts() {
        String repoPath1 = "/home/user/projects/myproject/src/main/java/Foo.java";
        String repoPath2 = "/home/user/projects/otherproject/src/main/python/Bar.py";

        List<TextChunk> chunks = List.of(
                createTestChunk("Foo", ENTITY_TYPE_CLASS, "java", repoPath1, "public class Foo"),
                createTestChunk("FooHelper", ENTITY_TYPE_METHOD, "java", repoPath1, "public void helper()"),
                createTestChunk("Bar", ENTITY_TYPE_CLASS, "python", repoPath2, "class Bar:")
        );
        indexService.addChunks(chunks).await().indefinitely();

        // First verify documents are searchable (use "Foo" which is not a stop word)
        List<Document> searchResults = indexService.search("Foo", 10).await().indefinitely();
        assertThat(searchResults).isNotEmpty(); // Ensure documents are indexed and searchable

        // Test facets with MatchAllDocsQuery (empty query) to get all facets
        Map<String, List<FacetValue>> facets = indexService.computeFacets("", null, 10)
                .await().indefinitely();

        assertThat(facets).containsKeys(
                LuceneSchema.FIELD_LANGUAGE,
                LuceneSchema.FIELD_REPOSITORY,
                LuceneSchema.FIELD_ENTITY_TYPE
        );
        
        // Verify language facets
        List<FacetValue> languageFacets = facets.get(LuceneSchema.FIELD_LANGUAGE);
        assertThat(languageFacets).isNotEmpty();
        assertThat(languageFacets)
                .extracting(FacetValue::value)
                .contains("java", "python");
        
        // Verify entity type facets
        List<FacetValue> entityTypeFacets = facets.get(LuceneSchema.FIELD_ENTITY_TYPE);
        assertThat(entityTypeFacets).isNotEmpty();
        assertThat(entityTypeFacets)
                .extracting(FacetValue::value)
                .contains("class", "method");
        
        // Verify repository facets
        List<FacetValue> repositoryFacets = facets.get(LuceneSchema.FIELD_REPOSITORY);
        assertThat(repositoryFacets).isNotEmpty();
        assertThat(repositoryFacets)
                .extracting(FacetValue::value)
                .contains("myproject", "otherproject");
        
        // Verify counts are correct
        assertThat(languageFacets.stream()
                .filter(f -> "java".equals(f.value()))
                .findFirst()
                .orElseThrow()
                .count()).isEqualTo(2); // Two java documents
        assertThat(languageFacets.stream()
                .filter(f -> "python".equals(f.value()))
                .findFirst()
                .orElseThrow()
                .count()).isEqualTo(1); // One python document
    }

    @Test
    void testComputeFacets_withFilters() {
        String repoPath1 = "/home/user/projects/myproject/src/main/java/Foo.java";
        String repoPath2 = "/home/user/projects/otherproject/src/main/python/Bar.py";

        List<TextChunk> chunks = List.of(
                createTestChunk("Foo", ENTITY_TYPE_CLASS, "java", repoPath1, "public class Foo"),
                createTestChunk("FooHelper", ENTITY_TYPE_METHOD, "java", repoPath1, "public void helper()"),
                createTestChunk("Bar", ENTITY_TYPE_CLASS, "python", repoPath2, "class Bar:")
        );
        indexService.addChunks(chunks).await().indefinitely();

        // Test facets with language filter (should only show java facets)
        SearchFilters filters = new SearchFilters(
                List.of("java"), // language filter
                null, null, null
        );
        Map<String, List<FacetValue>> facets = indexService.computeFacets("", filters, 10)
                .await().indefinitely();

        // Language facets should only show java (filtered)
        List<FacetValue> languageFacets = facets.get(LuceneSchema.FIELD_LANGUAGE);
        assertThat(languageFacets)
                .extracting(FacetValue::value)
                .containsExactly("java"); // Only java, python filtered out
        assertThat(languageFacets.get(0).count()).isEqualTo(2); // Two java documents
    }

    @Test
    void testComputeFacets_emptyIndex() {
        // Test facets on empty index (before adding any documents)
        Map<String, List<FacetValue>> facets = indexService.computeFacets("", null, 10)
                .await().indefinitely();

        // Should return map with all facet keys, even if empty
        assertThat(facets).containsKeys(
                LuceneSchema.FIELD_LANGUAGE,
                LuceneSchema.FIELD_REPOSITORY,
                LuceneSchema.FIELD_ENTITY_TYPE
        );
        // All facets should be empty lists
        assertThat(facets.get(LuceneSchema.FIELD_LANGUAGE)).isEmpty();
        assertThat(facets.get(LuceneSchema.FIELD_REPOSITORY)).isEmpty();
        assertThat(facets.get(LuceneSchema.FIELD_ENTITY_TYPE)).isEmpty();
    }

    @Test
    void testComputeFacets_maxFacetValuesLimit() {
        String repoPath = "/home/user/projects/myproject/src/main/java/Foo.java";

        // Create chunks with multiple languages
        List<TextChunk> chunks = List.of(
                createTestChunk("Class1", ENTITY_TYPE_CLASS, "java", repoPath, "class Class1"),
                createTestChunk("Class2", ENTITY_TYPE_CLASS, "java", repoPath, "class Class2"),
                createTestChunk("Class3", ENTITY_TYPE_CLASS, "python", repoPath, "class Class3"),
                createTestChunk("Class4", ENTITY_TYPE_CLASS, "python", repoPath, "class Class4"),
                createTestChunk("Class5", ENTITY_TYPE_CLASS, "go", repoPath, "class Class5")
        );
        indexService.addChunks(chunks).await().indefinitely();

        // Request only top 2 facet values
        Map<String, List<FacetValue>> facets = indexService.computeFacets("", null, 2)
                .await().indefinitely();

        List<FacetValue> languageFacets = facets.get(LuceneSchema.FIELD_LANGUAGE);
        // Should return at most 2 values
        assertThat(languageFacets.size()).isLessThanOrEqualTo(2);
    }

    @Test
    void testComputeFacets_withQuery() {
        String repoPath1 = "/home/user/projects/myproject/src/main/java/Foo.java";
        String repoPath2 = "/home/user/projects/otherproject/src/main/python/Bar.py";

        List<TextChunk> chunks = List.of(
                createTestChunk("Foo", ENTITY_TYPE_CLASS, "java", repoPath1, "public class Foo"),
                createTestChunk("FooHelper", ENTITY_TYPE_METHOD, "java", repoPath1, "public void helper()"),
                createTestChunk("Bar", ENTITY_TYPE_CLASS, "python", repoPath2, "class Bar:")
        );
        indexService.addChunks(chunks).await().indefinitely();

        // Test facets with a query that matches only some documents
        Map<String, List<FacetValue>> facets = indexService.computeFacets("Foo", null, 10)
                .await().indefinitely();

        // Facets should only reflect documents matching the query
        List<FacetValue> languageFacets = facets.get(LuceneSchema.FIELD_LANGUAGE);
        assertThat(languageFacets)
                .extracting(FacetValue::value)
                .contains("java"); // Only java documents match "Foo"
        // Python document doesn't match "Foo", so it shouldn't appear in facets
        assertThat(languageFacets)
                .extracting(FacetValue::value)
                .doesNotContain("python");
    }

    @Test
    void testComputeFacets_zeroMaxFacetValues() {
        String repoPath = "/home/user/projects/myproject/src/main/java/Foo.java";
        List<TextChunk> chunks = List.of(
                createTestChunk("Foo", ENTITY_TYPE_CLASS, "java", repoPath, "public class Foo")
        );
        indexService.addChunks(chunks).await().indefinitely();

        // Test with maxFacetValues = 0 (should return empty map)
        Map<String, List<FacetValue>> facets = indexService.computeFacets("", null, 0)
                .await().indefinitely();

        assertThat(facets).isEmpty();
    }

    /**
     * Test-specific subclass to allow injection of test directory.
     */
    private static class TestLuceneIndexService extends LuceneIndexService {
        // Extends LuceneIndexService to access protected fields for testing
    }
}
