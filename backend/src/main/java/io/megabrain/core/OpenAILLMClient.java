/*
 * Copyright (c) 2025 MegaBrain Contributors
 * Licensed under the MIT License - see LICENSE file for details.
 */

package io.megabrain.core;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import io.smallrye.mutiny.Uni;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;

import java.time.Duration;

/**
 * LLM client that wraps LangChain4j's OpenAI integration (US-03-02 T2).
 * Uses API key authentication and configuration from application properties.
 * Supports GPT-4 and GPT-3.5-turbo. When API key is not set, client is disabled (isAvailable() false).
 */
@ApplicationScoped
public class OpenAILLMClient implements LLMClient {

    private static final Logger LOG = Logger.getLogger(OpenAILLMClient.class);

    private final OpenAIConfiguration config;
    private volatile ChatModel chatModel;
    private volatile boolean available;

    public OpenAILLMClient(OpenAIConfiguration config) {
        this.config = config;
    }

    @PostConstruct
    void init() {
        long startTime = System.nanoTime();
        try {
            String apiKey = config.apiKey();
            if (apiKey == null || apiKey.isBlank()) {
                LOG.info("OpenAI LLM client disabled: no API key configured (set megabrain.llm.openai.api-key or OPENAI_API_KEY)");
                this.available = false;
                this.chatModel = null;
                return;
            }
            String model = config.model();
            LOG.infof("Initializing OpenAI LLM client: model=%s", model);
            this.chatModel = buildChatModel(apiKey.trim(), model, config.timeoutSeconds());
            this.available = true;
            long durationMs = (System.nanoTime() - startTime) / 1_000_000;
            LOG.infof("OpenAI LLM client initialized in %d ms", durationMs);
        } catch (Exception e) {
            long durationMs = (System.nanoTime() - startTime) / 1_000_000;
            LOG.errorf(e, "Failed to initialize OpenAI LLM client after %d ms", durationMs);
            this.available = false;
            this.chatModel = null;
            throw e;
        }
    }

    @PreDestroy
    void destroy() {
        LOG.info("Shutting down OpenAI LLM client");
        this.chatModel = null;
        this.available = false;
    }

    @Override
    public Uni<String> generate(String userMessage) {
        return generate(userMessage, null);
    }

    @Override
    public Uni<String> generate(String userMessage, String modelOverride) {
        if (userMessage == null || userMessage.isBlank()) {
            return Uni.createFrom().failure(new IllegalArgumentException("userMessage must not be blank"));
        }
        if (!available || chatModel == null) {
            return Uni.createFrom().failure(new IllegalStateException("OpenAI LLM client is not available"));
        }
        String effectiveModel = (modelOverride != null && !modelOverride.isBlank())
                ? modelOverride.trim()
                : config.model();
        return performGeneration(userMessage, effectiveModel);
    }

    private Uni<String> performGeneration(String userMessage, String model) {
        ChatModel modelToUse = model.equals(config.model())
                ? chatModel
                : buildChatModel(config.apiKey().trim(), model, config.timeoutSeconds());

        return Uni.createFrom().item(() -> {
            long startTime = System.nanoTime();
            try {
                String response = modelToUse.chat(userMessage);
                long durationMs = (System.nanoTime() - startTime) / 1_000_000;
                LOG.debugf("OpenAI generation (model=%s) completed in %d ms", model, durationMs);
                return response;
            } catch (Exception e) {
                long durationMs = (System.nanoTime() - startTime) / 1_000_000;
                LOG.warnf(e, "OpenAI generation (model=%s) failed after %d ms", model, durationMs);
                throw e instanceof RuntimeException re ? re : new RuntimeException(e);
            }
        });
    }

    private static ChatModel buildChatModel(String apiKey, String modelName, int timeoutSeconds) {
        return OpenAiChatModel.builder()
                .apiKey(apiKey)
                .modelName(modelName)
                .timeout(Duration.ofSeconds(timeoutSeconds))
                .build();
    }

    @Override
    public boolean isAvailable() {
        return available && chatModel != null;
    }
}
