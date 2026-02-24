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
 * Supports model selection via configuration and per-request model override (US-03-01 T3).
 */
@ApplicationScoped
public class OllamaLLMClient implements LLMClient {

    private static final Logger LOG = Logger.getLogger(OllamaLLMClient.class);

    private final OllamaConfiguration config;
    private final OllamaModelAvailabilityService modelAvailabilityService;
    private volatile ChatModel chatModel;
    private volatile boolean available;

    public OllamaLLMClient(OllamaConfiguration config,
                          OllamaModelAvailabilityService modelAvailabilityService) {
        this.config = config;
        this.modelAvailabilityService = modelAvailabilityService;
    }

    @PostConstruct
    void init() {
        long startTime = System.nanoTime();
        try {
            String baseUrl = config.baseUrl();
            String model = config.model();
            int timeoutSeconds = config.timeoutSeconds();

            LOG.infof("Initializing Ollama LLM client: baseUrl=%s, model=%s, timeout=%ds",
                    baseUrl, model, timeoutSeconds);

            this.chatModel = buildChatModel(baseUrl, model, timeoutSeconds);
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
        return generate(userMessage, null);
    }

    @Override
    public Uni<String> generate(String userMessage, String modelOverride) {
        if (userMessage == null || userMessage.isBlank()) {
            return Uni.createFrom().failure(new IllegalArgumentException("userMessage must not be blank"));
        }
        if (!available || chatModel == null) {
            return Uni.createFrom().failure(new IllegalStateException("Ollama LLM client is not available"));
        }

        String effectiveModel = (modelOverride != null && !modelOverride.isBlank())
                ? modelOverride.trim()
                : config.model();

        return isModelAvailable(effectiveModel)
                .flatMap(available -> {
                    if (!available) {
                        return Uni.createFrom().failure(new IllegalArgumentException(
                                "Model '" + effectiveModel + "' is not available on Ollama. " +
                                        "Use 'ollama pull " + effectiveModel + "' to install, or check megabrain.llm.ollama.model"));
                    }
                    return performGeneration(userMessage, effectiveModel);
                });
    }

    /**
     * Checks if the given model is available on the Ollama instance.
     * Uses cached result with configurable TTL via {@link OllamaModelAvailabilityService}.
     *
     * @param model model name (e.g. codellama, mistral)
     * @return Uni that emits true if available, false otherwise
     */
    public Uni<Boolean> isModelAvailable(String model) {
        return modelAvailabilityService.isModelAvailable(config.baseUrl(), model);
    }

    private Uni<String> performGeneration(String userMessage, String model) {
        ChatModel modelToUse = model.equals(config.model())
                ? chatModel
                : buildChatModel(config.baseUrl(), model, config.timeoutSeconds());

        return Uni.createFrom().item(() -> {
            long startTime = System.nanoTime();
            try {
                String response = modelToUse.chat(userMessage);
                long durationMs = (System.nanoTime() - startTime) / 1_000_000;
                LOG.debugf("Ollama generation (model=%s) completed in %d ms", model, durationMs);
                return response;
            } catch (Exception e) {
                long durationMs = (System.nanoTime() - startTime) / 1_000_000;
                LOG.errorf(e, "Ollama generation (model=%s) failed after %d ms", model, durationMs);
                throw e;
            }
        });
    }

    private static ChatModel buildChatModel(String baseUrl, String modelName, int timeoutSeconds) {
        return OllamaChatModel.builder()
                .baseUrl(baseUrl)
                .modelName(modelName)
                .timeout(Duration.ofSeconds(timeoutSeconds))
                .build();
    }

    @Override
    public boolean isAvailable() {
        return available && chatModel != null;
    }
}
