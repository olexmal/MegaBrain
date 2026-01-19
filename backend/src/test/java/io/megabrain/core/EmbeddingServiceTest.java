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

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test for the EmbeddingService.
 */
@QuarkusTest
public class EmbeddingServiceTest {

    @Inject
    EmbeddingService embeddingService;

    @Test
    public void testGenerateEmbeddingForSingleChunk() {
        TextChunk chunk = createTestChunk("public class Test { }");

        EmbeddingService.EmbeddingResult result = embeddingService.generateEmbedding(chunk)
                .subscribe().withSubscriber(UniAssertSubscriber.create())
                .awaitItem()
                .getItem();

        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getEmbedding()).isNotNull();
        assertThat(result.getEmbedding().length).isEqualTo(384);
        assertThat(result.getChunk()).isEqualTo(chunk);
        assertThat(result.getError()).isNull();
    }

    @Test
    public void testGenerateEmbeddingsForMultipleChunks() {
        List<TextChunk> chunks = List.of(
            createTestChunk("public class A { }"),
            createTestChunk("private void method() { }"),
            createTestChunk("import java.util.List;")
        );

        List<EmbeddingService.EmbeddingResult> results = embeddingService.generateEmbeddings(chunks)
                .subscribe().withSubscriber(UniAssertSubscriber.create())
                .awaitItem()
                .getItem();

        assertThat(results).hasSize(3);
        for (EmbeddingService.EmbeddingResult result : results) {
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getEmbedding()).hasSize(384);
            assertThat(result.getError()).isNull();
        }
    }

    @Test
    public void testBatchProcessing() {
        // Create more chunks than the batch size to test batching
        List<TextChunk> chunks = new java.util.ArrayList<>();
        for (int i = 0; i < 100; i++) {
            chunks.add(createTestChunk("class Test" + i + " { }"));
        }

        List<EmbeddingService.EmbeddingResult> results = embeddingService.generateEmbeddings(chunks)
                .subscribe().withSubscriber(UniAssertSubscriber.create())
                .awaitItem()
                .getItem();

        assertThat(results).hasSize(100);
        assertThat(results.stream().allMatch(EmbeddingService.EmbeddingResult::isSuccess)).isTrue();
        assertThat(results.stream().allMatch(r -> r.getEmbedding().length == 384)).isTrue();
    }

    @Test
    public void testEmptyInput() {
        List<EmbeddingService.EmbeddingResult> results = embeddingService.generateEmbeddings(List.of())
                .subscribe().withSubscriber(UniAssertSubscriber.create())
                .awaitItem()
                .getItem();

        assertThat(results).isEmpty();
    }

    @Test
    public void testNullInput() {
        List<EmbeddingService.EmbeddingResult> results = embeddingService.generateEmbeddings(null)
                .subscribe().withSubscriber(UniAssertSubscriber.create())
                .awaitItem()
                .getItem();

        assertThat(results).isEmpty();
    }

    @Test
    public void testTextPreprocessing() {
        // Test that preprocessing works (we can't easily test the internal preprocessing
        // but we can verify that embeddings are generated successfully for various inputs)

        List<TextChunk> chunks = List.of(
            createTestChunk("minimal"), // Minimal content
            createTestChunk("public class Test {\n    private int field;\n    public void method() {}\n}"), // Multi-line
            createTestChunk("very long text ".repeat(100)) // Long text
        );

        List<EmbeddingService.EmbeddingResult> results = embeddingService.generateEmbeddings(chunks)
                .subscribe().withSubscriber(UniAssertSubscriber.create())
                .awaitItem()
                .getItem();

        assertThat(results).hasSize(3);
        // All should succeed
        assertThat(results.stream().allMatch(EmbeddingService.EmbeddingResult::isSuccess)).isTrue();
        assertThat(results.stream().allMatch(r -> r.getEmbedding().length == 384)).isTrue();
    }

    @Test
    public void testLanguageContextAddition() {
        TextChunk javaChunk = createTestChunk("public class Test {}", "java");
        TextChunk pythonChunk = createTestChunk("def test():", "python");

        List<EmbeddingService.EmbeddingResult> results = embeddingService.generateEmbeddings(List.of(javaChunk, pythonChunk))
                .subscribe().withSubscriber(UniAssertSubscriber.create())
                .awaitItem()
                .getItem();

        assertThat(results).hasSize(2);
        assertThat(results.stream().allMatch(EmbeddingService.EmbeddingResult::isSuccess)).isTrue();
    }

    private TextChunk createTestChunk(String content) {
        return createTestChunk(content, "java");
    }

    private TextChunk createTestChunk(String content, String language) {
        return new TextChunk(
            content,
            language,
            "class",
            "Test",
            "/path/Test.java",
            1,
            1,
            0,
            content.length(),
            Map.of()
        );
    }
}