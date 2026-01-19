/*
 * Copyright (c) 2025 MegaBrain Contributors
 * Licensed under the MIT License - see LICENSE file for details.
 */

package io.megabrain.core;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.onnx.allminilml6v2.AllMiniLmL6V2EmbeddingModel;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.util.List;

/**
 * Service for managing the embedding model used for generating vector embeddings.
 * Provides a configured embedding model instance for generating embeddings from text.
 */
@ApplicationScoped
public class EmbeddingModelService {

    private static final Logger LOG = Logger.getLogger(EmbeddingModelService.class);

    @ConfigProperty(name = "megabrain.embedding.model", defaultValue = "all-minilm-l6-v2")
    String modelName;

    @ConfigProperty(name = "megabrain.embedding.cache-directory", defaultValue = "./data/embedding-cache")
    String cacheDirectory;

    private EmbeddingModel embeddingModel;

    /**
     * Gets the dimension of the embedding vectors produced by this model.
     * all-MiniLM-L6-v2 produces 384-dimensional vectors.
     */
    public static final int EMBEDDING_DIMENSION = 384;

    @PostConstruct
    void init() {
        LOG.infof("Initializing embedding model: %s", modelName);

        switch (modelName.toLowerCase()) {
            case "all-minilm-l6-v2", "all-minilml6v2" -> {
                this.embeddingModel = new AllMiniLmL6V2EmbeddingModel();
                LOG.info("Initialized all-MiniLM-L6-v2 embedding model");
            }
            default -> {
                LOG.warnf("Unknown embedding model '%s', falling back to all-MiniLM-L6-v2", modelName);
                this.embeddingModel = new AllMiniLmL6V2EmbeddingModel();
            }
        }

        LOG.infof("Embedding model initialized with dimension: %d", EMBEDDING_DIMENSION);
    }

    @PreDestroy
    void destroy() {
        LOG.info("Shutting down embedding model service");
        // LangChain4j models don't require explicit cleanup
    }

    /**
     * Produces the configured embedding model for CDI injection.
     */
    @Produces
    public EmbeddingModel getEmbeddingModel() {
        return embeddingModel;
    }

    /**
     * Generates embeddings for the given texts.
     *
     * @param texts the texts to embed
     * @return list of embedding vectors
     */
    public List<float[]> embed(List<String> texts) {
        if (texts == null || texts.isEmpty()) {
            return List.of();
        }

        LOG.debugf("Generating embeddings for %d texts", texts.size());

        var textSegments = texts.stream()
                .map(TextSegment::from)
                .toList();

        var response = embeddingModel.embedAll(textSegments);
        return response.content().stream()
                .map(embedding -> {
                    // Convert to float array for easier storage
                    float[] vector = new float[embedding.dimension()];
                    for (int i = 0; i < embedding.dimension(); i++) {
                        vector[i] = embedding.vector()[i];
                    }
                    return vector;
                })
                .toList();
    }

    /**
     * Generates a single embedding for the given text.
     *
     * @param text the text to embed
     * @return the embedding vector
     */
    public float[] embed(String text) {
        if (text == null || text.trim().isEmpty()) {
            // Return zero vector for empty text
            return new float[EMBEDDING_DIMENSION];
        }

        LOG.debugf("Generating embedding for single text");

        var textSegment = TextSegment.from(text);
        var response = embeddingModel.embed(textSegment);
        var embedding = response.content();

        // Convert to float array for easier storage
        float[] vector = new float[embedding.dimension()];
        for (int i = 0; i < embedding.dimension(); i++) {
            vector[i] = embedding.vector()[i];
        }
        return vector;
    }

    /**
     * Gets the embedding dimension for this model.
     */
    public int getEmbeddingDimension() {
        return EMBEDDING_DIMENSION;
    }
}