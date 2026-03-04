/*
 * Copyright (c) 2025 MegaBrain Contributors
 * Licensed under the MIT License - see LICENSE file for details.
 */

package io.megabrain.core;

import io.megabrain.api.TokenStreamEvent;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import org.jboss.logging.Logger;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.Optional;
import java.util.concurrent.Executor;

/**
 * Service for RAG answer generation and token streaming (US-03-04).
 * Builds SSE-compatible streams of token events for streaming LLM responses.
 * T2: Integrates LangChain4j streaming callback; tokens are converted to SSE events.
 * Supports OpenAI and Ollama (and Anthropic) streaming formats via {@link StreamingChatModelProvider}.
 */
@ApplicationScoped
public class RagService {

    private static final Logger LOG = Logger.getLogger(RagService.class);

    private final StreamingChatModelProvider streamingModelProvider;
    private Executor executor;

    @Inject
    public RagService(StreamingChatModelProvider streamingModelProvider) {
        this(streamingModelProvider, null);
    }

    /**
     * Constructor for testing: allows injecting an executor so tests can run streaming synchronously.
     */
    RagService(StreamingChatModelProvider streamingModelProvider, Executor executor) {
        this.streamingModelProvider = streamingModelProvider;
        this.executor = executor;
    }

    @PostConstruct
    void init() {
        if (this.executor == null) {
            this.executor = Infrastructure.getDefaultExecutor();
        }
    }

    /**
     * Returns a reactive stream of token events for the given question.
     * Uses LangChain4j's StreamingChatModel and StreamingChatResponseHandler to receive
     * tokens as they are generated (OpenAI delta, Ollama response chunks) and emit them as SSE events.
     *
     * @param question the user question
     * @return Multi of token events, formatted for SSE (event: token, data: {"token": "..."})
     */
    public Multi<TokenStreamEvent> streamTokens(String question) {
        if (question == null || question.isBlank()) {
            return Multi.createFrom().empty();
        }
        Optional<StreamingChatModel> modelOpt = streamingModelProvider.getStreamingModel();
        if (modelOpt.isEmpty()) {
            LOG.warn("No streaming LLM available for RAG; returning failure");
            return Multi.createFrom().failure(
                    new IllegalStateException("No LLM available for streaming. Configure Ollama, OpenAI, or Anthropic."));
        }
        StreamingChatModel model = modelOpt.get();
        LOG.debugf("Building token stream for question: %s", question);

        return Multi.createFrom().emitter(emitter -> {
            executor.execute(() -> {
                try {
                    model.chat(question.trim(), new StreamingChatResponseHandler() {
                        @Override
                        public void onPartialResponse(String partialResponse) {
                            if (partialResponse != null && !partialResponse.isEmpty()) {
                                emitter.emit(new TokenStreamEvent(partialResponse));
                            }
                        }

                        @Override
                        public void onCompleteResponse(ChatResponse completeResponse) {
                            emitter.complete();
                        }

                        @Override
                        public void onError(Throwable error) {
                            emitter.fail(error);
                        }
                    });
                } catch (Throwable t) {
                    emitter.fail(t);
                }
            });
        });
    }
}
