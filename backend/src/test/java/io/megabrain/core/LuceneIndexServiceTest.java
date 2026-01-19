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
        assertEquals(1, stats.maxDoc()); // Documents are fully removed, not marked as deleted
        assertEquals(0, stats.numDeletedDocs());
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

    /**
     * Test-specific subclass to allow injection of test directory.
     */
    private static class TestLuceneIndexService extends LuceneIndexService {
        // Extends LuceneIndexService to access protected fields for testing
    }
}
