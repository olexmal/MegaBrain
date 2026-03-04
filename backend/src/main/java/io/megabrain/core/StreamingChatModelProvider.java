/*
 * Copyright (c) 2025 MegaBrain Contributors
 * Licensed under the MIT License - see LICENSE file for details.
 */

package io.megabrain.core;

import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.ollama.OllamaStreamingChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import dev.langchain4j.model.anthropic.AnthropicStreamingChatModel;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.time.Duration;
import java.util.Optional;

/**
 * Provides the active {@link StreamingChatModel} for RAG token streaming (US-03-04 T2).
 * Supports Ollama, OpenAI, and Anthropic streaming formats. Uses configured provider
 * preference (megabrain.llm.provider) or first available in order: ollama, openai, anthropic.
 */
@ApplicationScoped
public class StreamingChatModelProvider {

    private static final Logger LOG = Logger.getLogger(StreamingChatModelProvider.class);

    @ConfigProperty(name = "megabrain.llm.provider", defaultValue = "ollama")
    String preferredProvider;

    private final OllamaConfiguration ollamaConfig;
    private final OpenAIConfiguration openAIConfig;
    private final AnthropicConfiguration anthropicConfig;

    public StreamingChatModelProvider(OllamaConfiguration ollamaConfig,
                                     OpenAIConfiguration openAIConfig,
                                     AnthropicConfiguration anthropicConfig) {
        this.ollamaConfig = ollamaConfig;
        this.openAIConfig = openAIConfig;
        this.anthropicConfig = anthropicConfig;
    }

    /**
     * Returns the first available streaming chat model for the preferred provider, or first available.
     *
     * @return Optional containing the streaming model, or empty if none is configured/available
     */
    public Optional<StreamingChatModel> getStreamingModel() {
        String provider = preferredProvider == null ? "ollama" : preferredProvider.trim().toLowerCase();
        switch (provider) {
            case "openai":
                return tryOpenAI().or(this::tryOllama).or(this::tryAnthropic);
            case "anthropic":
                return tryAnthropic().or(this::tryOllama).or(this::tryOpenAI);
            case "ollama":
            default:
                return tryOllama().or(this::tryOpenAI).or(this::tryAnthropic);
        }
    }

    private Optional<StreamingChatModel> tryOllama() {
        try {
            String baseUrl = ollamaConfig.baseUrl();
            if (baseUrl == null || baseUrl.isBlank()) {
                return Optional.empty();
            }
            StreamingChatModel model = OllamaStreamingChatModel.builder()
                    .baseUrl(baseUrl.trim())
                    .modelName(ollamaConfig.model())
                    .timeout(Duration.ofSeconds(ollamaConfig.timeoutSeconds()))
                    .build();
            LOG.debugf("Ollama streaming model available: %s", ollamaConfig.model());
            return Optional.of(model);
        } catch (Exception e) {
            LOG.debugf(e, "Ollama streaming model not available");
            return Optional.empty();
        }
    }

    private Optional<StreamingChatModel> tryOpenAI() {
        try {
            String apiKey = openAIConfig.apiKey();
            if (apiKey == null || apiKey.isBlank()) {
                return Optional.empty();
            }
            String trimmed = apiKey.trim();
            if (!LLMApiKeyValidator.isValidOpenAIKey(trimmed)) {
                return Optional.empty();
            }
            var builder = OpenAiStreamingChatModel.builder()
                    .apiKey(trimmed)
                    .modelName(openAIConfig.model())
                    .timeout(Duration.ofSeconds(openAIConfig.timeoutSeconds()));
            if (openAIConfig.baseUrl() != null && !openAIConfig.baseUrl().isBlank()) {
                builder.baseUrl(openAIConfig.baseUrl().trim());
            }
            StreamingChatModel model = builder.build();
            LOG.debugf("OpenAI streaming model available: %s", openAIConfig.model());
            return Optional.of(model);
        } catch (Exception e) {
            LOG.debugf(e, "OpenAI streaming model not available");
            return Optional.empty();
        }
    }

    private Optional<StreamingChatModel> tryAnthropic() {
        try {
            String apiKey = anthropicConfig.apiKey();
            if (apiKey == null || apiKey.isBlank()) {
                return Optional.empty();
            }
            String trimmed = apiKey.trim();
            if (!LLMApiKeyValidator.isValidAnthropicKey(trimmed)) {
                return Optional.empty();
            }
            var builder = AnthropicStreamingChatModel.builder()
                    .apiKey(trimmed)
                    .modelName(anthropicConfig.model())
                    .timeout(Duration.ofSeconds(anthropicConfig.timeoutSeconds()));
            if (anthropicConfig.baseUrl() != null && !anthropicConfig.baseUrl().isBlank()) {
                builder.baseUrl(anthropicConfig.baseUrl().trim());
            }
            StreamingChatModel model = builder.build();
            LOG.debugf("Anthropic streaming model available: %s", anthropicConfig.model());
            return Optional.of(model);
        } catch (Exception e) {
            LOG.debugf(e, "Anthropic streaming model not available");
            return Optional.empty();
        }
    }
}
