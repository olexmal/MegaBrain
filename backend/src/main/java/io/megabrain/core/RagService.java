/*
 * Copyright (c) 2025 MegaBrain Contributors
 * Licensed under the MIT License - see LICENSE file for details.
 */

package io.megabrain.core;

import io.megabrain.api.CancelledEvent;
import io.megabrain.api.SseStreamEvent;
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
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Service for RAG answer generation and token streaming (US-03-04).
 * Builds SSE-compatible streams of token events for streaming LLM responses.
 * T2: Integrates LangChain4j streaming callback; tokens are converted to SSE events.
 * Supports OpenAI and Ollama (and Anthropic) streaming formats via {@link StreamingChatModelProvider}.
 * T3: Handles stream cancellation (connection close or explicit cancel); cleans up and emits cancelled event.
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
     * When the client cancels (e.g. connection close), resources are cleaned up and a cancelled event is emitted.
     *
     * @param question the user question
     * @return Multi of stream events (token or cancelled), formatted for SSE
     */
    public Multi<SseStreamEvent> streamTokens(String question) {
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

        AtomicBoolean cancelled = new AtomicBoolean(false);
        return Multi.createFrom().emitter(emitter -> {
            emitter.onTermination(() -> {
                cancelled.set(true);
                LOG.debug("RAG stream cancelled; cleaning up");
            });
            executor.execute(() -> {
                try {
                    model.chat(question.trim(), new StreamingChatResponseHandler() {
                        @Override
                        public void onPartialResponse(String partialResponse) {
                            if (cancelled.get()) {
                                emitter.emit(new CancelledEvent());
                                emitter.complete();
                                return;
                            }
                            if (partialResponse != null && !partialResponse.isEmpty()) {
                                emitter.emit(new TokenStreamEvent(partialResponse));
                            }
                        }

                        @Override
                        public void onCompleteResponse(ChatResponse completeResponse) {
                            if (cancelled.get()) {
                                emitter.emit(new CancelledEvent());
                            }
                            emitter.complete();
                        }

                        @Override
                        public void onError(Throwable error) {
                            if (!cancelled.get()) {
                                emitter.fail(error);
                            } else {
                                emitter.complete();
                            }
                        }
                    });
                } catch (Throwable t) {
                    if (!cancelled.get()) {
                        emitter.fail(t);
                    } else {
                        emitter.complete();
                    }
                }
            });
        });
    }
}
