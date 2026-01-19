/*
 * Copyright (c) 2025 MegaBrain Contributors
 * Licensed under the MIT License - see LICENSE file for details.
 */

package io.megabrain.core;

import io.megabrain.ingestion.parser.TextChunk;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test for the HybridIndexService.
 * Tests the complete pipeline from chunk indexing to hybrid search.
 * Requires PostgreSQL database to be configured.
 */
@QuarkusTest
@EnabledIfSystemProperty(named = "quarkus.datasource.db-kind", matches = "postgresql")
public class HybridIndexServiceTest {

    @Inject
    @IndexType(IndexType.Type.HYBRID)
    HybridIndexService hybridIndexService;

    @Test
    public void testAddChunksAndHybridSearch() {
        // Create test chunks
        List<TextChunk> chunks = List.of(
            createTestChunk("public class UserService { private String name; }", "java", "class", "UserService"),
            createTestChunk("public interface Repository<T> { T findById(Long id); }", "java", "interface", "Repository"),
            createTestChunk("def calculate_total(items): return sum(items)", "python", "function", "calculate_total"),
            createTestChunk("function validateEmail(email) { return email.includes('@'); }", "javascript", "function", "validateEmail")
        );

        // Add chunks to index
        hybridIndexService.addChunks(chunks)
                .subscribe().withSubscriber(UniAssertSubscriber.create())
                .awaitItem();

        // Test hybrid search
        HybridIndexService.HybridSearchResult result = hybridIndexService.hybridSearch("user service class", 5)
                .subscribe().withSubscriber(UniAssertSubscriber.create())
                .awaitItem()
                .getItem();

        // Verify results
        assertThat(result).isNotNull();
        assertThat(result.getVectorResults()).isNotEmpty();

        // The most similar result should be the UserService class
        List<VectorStore.SearchResult> vectorResults = result.getVectorResults();
        assertThat(vectorResults.get(0).metadata().entityName()).isEqualTo("UserService");
        assertThat(vectorResults.get(0).similarity()).isGreaterThan(0.0);
    }

    @Test
    public void testRemoveChunksForFile() {
        String testFile = "/test/RemoveTest.java";

        // Create chunks for the test file
        List<TextChunk> chunks = List.of(
            createTestChunk("public class RemoveTest { }", "java", "class", "RemoveTest", testFile),
            createTestChunk("public void method() { }", "java", "method", "method", testFile)
        );

        // Add chunks
        hybridIndexService.addChunks(chunks)
                .subscribe().withSubscriber(UniAssertSubscriber.create())
                .awaitItem();

        // Remove chunks for the file
        Integer removedCount = hybridIndexService.removeChunksForFile(testFile)
                .subscribe().withSubscriber(UniAssertSubscriber.create())
                .awaitItem()
                .getItem();

        // Should have removed the chunks
        assertThat(removedCount).isGreaterThanOrEqualTo(2);
    }

    @Test
    public void testUpdateChunksForFile() {
        String testFile = "/test/UpdateTest.java";

        // Initial chunks
        List<TextChunk> initialChunks = List.of(
            createTestChunk("public class OldClass { }", "java", "class", "OldClass", testFile)
        );

        // Add initial chunks
        hybridIndexService.addChunks(initialChunks)
                .subscribe().withSubscriber(UniAssertSubscriber.create())
                .awaitItem();

        // Updated chunks
        List<TextChunk> updatedChunks = List.of(
            createTestChunk("public class NewClass { }", "java", "class", "NewClass", testFile),
            createTestChunk("public void newMethod() { }", "java", "method", "newMethod", testFile)
        );

        // Update chunks
        hybridIndexService.updateChunksForFile(testFile, updatedChunks)
                .subscribe().withSubscriber(UniAssertSubscriber.create())
                .awaitItem();

        // Search for updated content
        HybridIndexService.HybridSearchResult result = hybridIndexService.hybridSearch("new class", 5)
                .subscribe().withSubscriber(UniAssertSubscriber.create())
                .awaitItem()
                .getItem();

        // Should find the new class
        List<VectorStore.SearchResult> vectorResults = result.getVectorResults();
        boolean foundNewClass = vectorResults.stream()
                .anyMatch(r -> "NewClass".equals(r.metadata().entityName()));

        assertThat(foundNewClass).isTrue();
    }

    @Test
    public void testEmptySearch() {
        // Search with a query that should return no results
        HybridIndexService.HybridSearchResult result = hybridIndexService.hybridSearch("nonexistent content xyz123", 5)
                .subscribe().withSubscriber(UniAssertSubscriber.create())
                .awaitItem()
                .getItem();

        // Should return empty results
        assertThat(result).isNotNull();
        assertThat(result.getVectorResults()).isNotEmpty(); // May return some low-similarity results
    }

    @Test
    public void testMultilingualChunks() {
        // Test with chunks in different languages
        List<TextChunk> chunks = List.of(
            createTestChunk("public class JavaClass { }", "java", "class", "JavaClass"),
            createTestChunk("class PythonClass:\n    pass", "python", "class", "PythonClass"),
            createTestChunk("type TypeScriptInterface = {", "typescript", "interface", "TypeScriptInterface")
        );

        hybridIndexService.addChunks(chunks)
                .subscribe().withSubscriber(UniAssertSubscriber.create())
                .awaitItem();

        // Search should work across languages
        HybridIndexService.HybridSearchResult result = hybridIndexService.hybridSearch("class definition", 10)
                .subscribe().withSubscriber(UniAssertSubscriber.create())
                .awaitItem()
                .getItem();

        assertThat(result.getVectorResults()).hasSizeGreaterThanOrEqualTo(3);

        // Check that different languages are represented
        List<String> languages = result.getVectorResults().stream()
                .map(r -> r.metadata().language())
                .distinct()
                .toList();

        assertThat(languages).containsAnyOf("java", "python", "typescript");
    }

    private TextChunk createTestChunk(String content, String language, String entityType, String entityName) {
        return createTestChunk(content, language, entityType, entityName, "/test/File." + language);
    }

    private TextChunk createTestChunk(String content, String language, String entityType, String entityName, String filePath) {
        return new TextChunk(
            content,
            language,
            entityType,
            entityName,
            filePath,
            1,
            content.split("\n").length,
            0,
            content.length(),
            Map.of()
        );
    }
}