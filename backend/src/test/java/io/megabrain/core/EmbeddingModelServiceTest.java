/*
 * Copyright (c) 2025 MegaBrain Contributors
 * Licensed under the MIT License - see LICENSE file for details.
 */

package io.megabrain.core;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test for the EmbeddingModelService.
 */
@QuarkusTest
public class EmbeddingModelServiceTest {

    @Inject
    EmbeddingModelService embeddingService;

    @Test
    public void testEmbeddingDimension() {
        assertThat(embeddingService.getEmbeddingDimension()).isEqualTo(384);
    }

    @Test
    public void testSingleEmbedding() {
        String text = "public class HelloWorld { }";
        float[] embedding = embeddingService.embed(text);

        assertThat(embedding).isNotNull();
        assertThat(embedding.length).isEqualTo(384);

        // Check that embedding is not all zeros (should have meaningful values)
        boolean hasNonZero = false;
        for (float value : embedding) {
            if (value != 0.0f) {
                hasNonZero = true;
                break;
            }
        }
        assertThat(hasNonZero).isTrue();
    }

    @Test
    public void testBatchEmbeddings() {
        List<String> texts = List.of(
            "public class Test { }",
            "private void method() { }",
            "import java.util.List;"
        );

        List<float[]> embeddings = embeddingService.embed(texts);

        assertThat(embeddings).hasSize(3);
        for (float[] embedding : embeddings) {
            assertThat(embedding).hasSize(384);
        }

        // Check that embeddings are different (different texts should produce different vectors)
        float[] emb1 = embeddings.get(0);
        float[] emb2 = embeddings.get(1);
        boolean different = false;
        for (int i = 0; i < emb1.length; i++) {
            if (Math.abs(emb1[i] - emb2[i]) > 0.001f) {
                different = true;
                break;
            }
        }
        assertThat(different).isTrue();
    }

    @Test
    public void testEmptyText() {
        float[] embedding = embeddingService.embed("");
        assertThat(embedding).hasSize(384);

        // Empty text should produce zero vector
        for (float value : embedding) {
            assertThat(value).isEqualTo(0.0f);
        }
    }

    @Test
    public void testNullText() {
        float[] embedding = embeddingService.embed((String) null);
        assertThat(embedding).hasSize(384);

        // Null text should produce zero vector
        for (float value : embedding) {
            assertThat(value).isEqualTo(0.0f);
        }
    }

    @Test
    public void testEmptyList() {
        List<float[]> embeddings = embeddingService.embed(List.of());
        assertThat(embeddings).isEmpty();
    }
}