/*
 * Copyright (c) 2025 MegaBrain Contributors
 * Licensed under the MIT License - see LICENSE file for details.
 */

package io.megabrain.core;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import io.smallrye.mutiny.Uni;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;

import java.time.Duration;

/**
 * LLM client that wraps LangChain4j's Ollama integration.
 * Connects to an Ollama endpoint and loads configuration from application properties.
 */
@ApplicationScoped
public class OllamaLLMClient implements LLMClient {

    private static final Logger LOG = Logger.getLogger(OllamaLLMClient.class);

    private final OllamaConfiguration config;
    private volatile ChatModel chatModel;
    private volatile boolean available;

    public OllamaLLMClient(OllamaConfiguration config) {
        this.config = config;
    }

    @PostConstruct
    void init() {
        long startTime = System.nanoTime();
        try {
            String baseUrl = config.baseUrl();
            String model = config.model();
            int timeoutSeconds = config.timeoutSeconds();

            LOG.infof("Initializing Ollama LLM client: baseUrl=%s, model=%s, timeout=%ds", baseUrl, model, timeoutSeconds);

            this.chatModel = OllamaChatModel.builder()
                    .baseUrl(baseUrl)
                    .modelName(model)
                    .timeout(Duration.ofSeconds(timeoutSeconds))
                    .build();

            this.available = true;
            long durationMs = (System.nanoTime() - startTime) / 1_000_000;
            LOG.infof("Ollama LLM client initialized in %d ms", durationMs);
        } catch (Exception e) {
            long durationMs = (System.nanoTime() - startTime) / 1_000_000;
            LOG.errorf(e, "Failed to initialize Ollama LLM client after %d ms", durationMs);
            this.available = false;
            throw e;
        }
    }

    @PreDestroy
    void destroy() {
        LOG.info("Shutting down Ollama LLM client");
        this.chatModel = null;
        this.available = false;
    }

    @Override
    public Uni<String> generate(String userMessage) {
        if (userMessage == null || userMessage.isBlank()) {
            return Uni.createFrom().failure(new IllegalArgumentException("userMessage must not be blank"));
        }
        if (!available || chatModel == null) {
            return Uni.createFrom().failure(new IllegalStateException("Ollama LLM client is not available"));
        }

        return Uni.createFrom().item(() -> {
            long startTime = System.nanoTime();
            try {
                String response = chatModel.chat(userMessage);
                long durationMs = (System.nanoTime() - startTime) / 1_000_000;
                LOG.debugf("Ollama generation completed in %d ms", durationMs);
                return response;
            } catch (Exception e) {
                long durationMs = (System.nanoTime() - startTime) / 1_000_000;
                LOG.errorf(e, "Ollama generation failed after %d ms", durationMs);
                throw e;
            }
        });
    }

    @Override
    public boolean isAvailable() {
        return available && chatModel != null;
    }
}
