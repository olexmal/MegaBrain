/*
 * Copyright (c) 2025 MegaBrain Contributors
 * Licensed under the MIT License - see LICENSE file for details.
 */

package io.megabrain.core;

import io.megabrain.ingestion.parser.TextChunk;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
class InMemoryIndexServiceTest {

    @Inject
    InMemoryIndexService indexService;

    @BeforeEach
    void setUp() {
        indexService.clear();
    }

    @Test
    void addChunks_shouldStoreChunksByFile() {
        // Create test chunks
        TextChunk chunk1 = new TextChunk(
                "public class Test {}", "java", "class", "Test",
                "Test.java", 1, 1, 0, 20, Map.of()
        );
        TextChunk chunk2 = new TextChunk(
                "public void method() {}", "java", "method", "Test.method",
                "Test.java", 2, 2, 21, 42, Map.of()
        );

        List<TextChunk> chunks = List.of(chunk1, chunk2);

        // Add chunks
        indexService.addChunks(chunks).await().indefinitely();

        // Verify
        List<TextChunk> storedChunks = indexService.getChunksForFile("Test.java");
        assertThat(storedChunks).hasSize(2);
        assertThat(storedChunks.stream().map(TextChunk::entityType))
                .containsExactlyInAnyOrder("class", "method");
    }

    @Test
    void addChunks_shouldHandleMultipleFiles() {
        // Create chunks for different files
        TextChunk javaChunk = new TextChunk(
                "class Java {}", "java", "class", "Java",
                "Java.java", 1, 1, 0, 12, Map.of()
        );
        TextChunk pyChunk = new TextChunk(
                "def python(): pass", "python", "function", "python",
                "python.py", 1, 1, 0, 18, Map.of()
        );

        List<TextChunk> chunks = List.of(javaChunk, pyChunk);

        // Add chunks
        indexService.addChunks(chunks).await().indefinitely();

        // Verify
        assertThat(indexService.getChunksForFile("Java.java")).hasSize(1);
        assertThat(indexService.getChunksForFile("python.py")).hasSize(1);
        assertThat(indexService.getAllChunks()).hasSize(2);
    }

    @Test
    void removeChunksForFile_shouldRemoveAllChunksForFile() {
        // Add chunks first
        TextChunk chunk = new TextChunk(
                "class Test {}", "java", "class", "Test",
                "Test.java", 1, 1, 0, 12, Map.of()
        );
        indexService.addChunks(List.of(chunk)).await().indefinitely();

        // Verify chunk exists
        assertThat(indexService.getChunksForFile("Test.java")).hasSize(1);

        // Remove chunks
        Integer removedCount = indexService.removeChunksForFile("Test.java")
                .subscribe().withSubscriber(UniAssertSubscriber.create())
                .awaitItem().getItem();

        // Verify
        assertThat(removedCount).isOne();
        assertThat(indexService.getChunksForFile("Test.java")).isEmpty();
    }

    @Test
    void removeChunksForFile_shouldReturnZeroForNonExistentFile() {
        Integer removedCount = indexService.removeChunksForFile("nonexistent.java")
                .subscribe().withSubscriber(UniAssertSubscriber.create())
                .awaitItem().getItem();

        assertThat(removedCount).isZero();
    }

    @Test
    void updateChunksForFile_shouldReplaceChunksForFile() {
        // Add initial chunks
        TextChunk oldChunk = new TextChunk(
                "class Old {}", "java", "class", "Old",
                "Test.java", 1, 1, 0, 11, Map.of()
        );
        indexService.addChunks(List.of(oldChunk)).await().indefinitely();

        // Update with new chunks
        TextChunk newChunk1 = new TextChunk(
                "class New {}", "java", "class", "New",
                "Test.java", 1, 1, 0, 11, Map.of()
        );
        TextChunk newChunk2 = new TextChunk(
                "void method() {}", "java", "method", "New.method",
                "Test.java", 2, 2, 12, 26, Map.of()
        );

        indexService.updateChunksForFile("Test.java", List.of(newChunk1, newChunk2))
                .await().indefinitely();

        // Verify
        List<TextChunk> storedChunks = indexService.getChunksForFile("Test.java");
        assertThat(storedChunks).hasSize(2);
        assertThat(storedChunks.stream().map(TextChunk::entityName))
                .containsExactlyInAnyOrder("New", "New.method");
    }

    @Test
    void addChunks_shouldHandleNullOrEmptyList() {
        // Should not throw exceptions
        indexService.addChunks(null).await().indefinitely();
        indexService.addChunks(List.of()).await().indefinitely();

        assertThat(indexService.getAllChunks()).isEmpty();
    }
}
