/*
 * Copyright (c) 2025 MegaBrain Contributors
 * Licensed under the MIT License - see LICENSE file for details.
 */

package io.megabrain.core;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.anthropic.AnthropicChatModel;
import io.smallrye.mutiny.Uni;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;

import java.time.Instant;
import java.time.Duration;

/**
 * LLM client that wraps LangChain4j's Anthropic integration (US-03-02 T3).
 * Uses API key authentication and configuration from application properties.
 * Supports Claude 3.5 Sonnet and Claude 3 Opus. When API key is not set, client is disabled (isAvailable() false).
 */
@ApplicationScoped
public class AnthropicLLMClient implements LLMClient {

    private static final Logger LOG = Logger.getLogger(AnthropicLLMClient.class);

    private final AnthropicConfiguration config;
    private final LLMRetryHelper retryHelper;
    private final LLMUsageRecorder usageRecorder;
    private volatile ChatModel chatModel;
    private volatile boolean available;

    public AnthropicLLMClient(AnthropicConfiguration config, LLMRetryHelper retryHelper, LLMUsageRecorder usageRecorder) {
        this.config = config;
        this.retryHelper = retryHelper;
        this.usageRecorder = usageRecorder != null ? usageRecorder : new NoOpLLMUsageRecorder();
    }

    @PostConstruct
    void init() {
        long startTime = System.nanoTime();
        try {
            String apiKey = config.apiKey();
            if (apiKey == null || apiKey.isBlank()) {
                LOG.info("Anthropic LLM client disabled: no API key configured (set megabrain.llm.anthropic.api-key or ANTHROPIC_API_KEY)");
                this.available = false;
                this.chatModel = null;
                return;
            }
            String trimmed = apiKey.trim();
            if (!LLMApiKeyValidator.isValidAnthropicKey(trimmed)) {
                LOG.warnf("Anthropic API key has invalid format (expected to start with %s). Key not logged. Set megabrain.llm.anthropic.api-key or ANTHROPIC_API_KEY.",
                        LLMApiKeyValidator.maskAnthropicKey(trimmed));
                throw new IllegalStateException(
                        "Anthropic API key has invalid format (expected to start with sk-ant-). Set megabrain.llm.anthropic.api-key or ANTHROPIC_API_KEY.");
            }
            String model = config.model();
            LOG.infof("Initializing Anthropic LLM client: model=%s", model);
            this.chatModel = buildChatModel(trimmed, model, config.timeoutSeconds(), config.baseUrl());
            this.available = true;
            long durationMs = (System.nanoTime() - startTime) / 1_000_000;
            LOG.infof("Anthropic LLM client initialized in %d ms", durationMs);
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            long durationMs = (System.nanoTime() - startTime) / 1_000_000;
            LOG.errorf("Failed to initialize Anthropic LLM client after %d ms: check API key and configuration (key not logged)", durationMs);
            this.available = false;
            this.chatModel = null;
            throw new IllegalStateException("Anthropic LLM client failed to initialize: check API key and configuration.", e);
        }
    }

    @PreDestroy
    void destroy() {
        LOG.info("Shutting down Anthropic LLM client");
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
            return Uni.createFrom().failure(new IllegalStateException("Anthropic LLM client is not available"));
        }
        String effectiveModel = (modelOverride != null && !modelOverride.isBlank())
                ? modelOverride.trim()
                : config.model();
        return performGeneration(userMessage, effectiveModel);
    }

    private Uni<String> performGeneration(String userMessage, String model) {
        ChatModel modelToUse = model.equals(config.model())
                ? chatModel
                : buildChatModel(config.apiKey().trim(), model, config.timeoutSeconds(), config.baseUrl());

        return Uni.createFrom().item(() -> {
            long startTime = System.nanoTime();
            try {
                String response = retryHelper.executeWithRetry(
                        () -> modelToUse.chat(userMessage),
                        "Anthropic",
                        config.maxRetries(),
                        config.baseDelayMs());
                long durationMs = (System.nanoTime() - startTime) / 1_000_000;
                int inputTokens = LLMCostEstimator.estimateTokens(userMessage);
                int outputTokens = LLMCostEstimator.estimateTokens(response);
                double costEstimate = LLMCostEstimator.estimateCost("anthropic", model, inputTokens, outputTokens);
                LLMUsageRecord usage = new LLMUsageRecord("anthropic", model, inputTokens, outputTokens, costEstimate, Instant.now());
                usageRecorder.record(usage);
                LOG.infof("Anthropic usage: model=%s inputTokens=%d outputTokens=%d costEstimate=$%.6f durationMs=%d",
                        model, inputTokens, outputTokens, costEstimate, durationMs);
                LOG.debugf("Anthropic generation (model=%s) completed in %d ms", model, durationMs);
                return response;
            } catch (IllegalStateException e) {
                throw e;
            } catch (Exception e) {
                long durationMs = (System.nanoTime() - startTime) / 1_000_000;
                LOG.warnf(e, "Anthropic generation (model=%s) failed after %d ms", model, durationMs);
                throw e instanceof RuntimeException re ? re : new RuntimeException(e);
            }
        });
    }

    private static ChatModel buildChatModel(String apiKey, String modelName, int timeoutSeconds, String baseUrl) {
        var builder = AnthropicChatModel.builder()
                .apiKey(apiKey)
                .modelName(modelName)
                .timeout(Duration.ofSeconds(timeoutSeconds));
        if (baseUrl != null && !baseUrl.isBlank()) {
            builder.baseUrl(baseUrl.trim());
        }
        return builder.build();
    }

    @Override
    public boolean isAvailable() {
        return available && chatModel != null;
    }
}
