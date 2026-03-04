/*
 * Copyright (c) 2025 MegaBrain Contributors
 * Licensed under the MIT License - see LICENSE file for details.
 */

package io.megabrain.core;

import io.megabrain.api.TokenStreamEvent;
import io.smallrye.mutiny.Multi;
import org.jboss.logging.Logger;

import jakarta.enterprise.context.ApplicationScoped;

/**
 * Service for RAG answer generation and token streaming (US-03-04).
 * Builds SSE-compatible streams of token events for streaming LLM responses.
 */
@ApplicationScoped
public class RagService {

    private static final Logger LOG = Logger.getLogger(RagService.class);

    /**
     * Returns a reactive stream of token events for the given question.
     * T1: SSE response builder; actual LLM streaming is integrated in T2.
     *
     * @param question the user question
     * @return Multi of token events, formatted for SSE (event: token, data: {"token": "..."})
     */
    public Multi<TokenStreamEvent> streamTokens(String question) {
        if (question == null || question.isBlank()) {
            return Multi.createFrom().empty();
        }
        LOG.debugf("Building token stream for question: %s", question);
        // Placeholder stream until T2 integrates LangChain4j streaming callback
        return Multi.createFrom().items("Streaming", " ", "endpoint", " ", "ready", ".")
                .map(TokenStreamEvent::new);
    }
}
