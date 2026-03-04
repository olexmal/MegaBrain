/*
 * Copyright (c) 2025 MegaBrain Contributors
 * Licensed under the MIT License - see LICENSE file for details.
 */

package io.megabrain.api;

import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.jboss.logging.Logger;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.megabrain.api.CancelledEvent;
import io.megabrain.api.ErrorStreamEvent;
import io.megabrain.api.SseStreamEvent;
import io.megabrain.api.TokenStreamEvent;
import io.megabrain.core.RagService;
import io.smallrye.mutiny.Multi;

/**
 * REST API resource for RAG question-answering with SSE token streaming (US-03-04).
 */
@Path("/rag")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class RagResource {

    private static final Logger LOG = Logger.getLogger(RagResource.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final RagService ragService;

    @Inject
    public RagResource(RagService ragService) {
        this.ragService = ragService;
    }

    /**
     * Streams LLM response tokens as Server-Sent Events.
     * Format: {@code event: token} and {@code data: {"token": "..."}} per event.
     *
     * @param request the RAG request containing the question
     * @return reactive stream of SSE-formatted strings (non-blocking)
     */
    @POST
    @Produces(MediaType.SERVER_SENT_EVENTS)
    public Multi<String> stream(@Valid RagRequest request) {
        String question = request != null && request.getQuestion() != null ? request.getQuestion().trim() : "";
        LOG.infof("RAG stream request: question length=%d", question.length());

        return ragService.streamTokens(question)
                .map(event -> toSseLine(event))
                .onFailure().recoverWithItem(throwable -> {
                    LOG.error("RAG stream failed", throwable);
                    ErrorStreamEvent err = new ErrorStreamEvent(
                            throwable != null && throwable.getMessage() != null ? throwable.getMessage() : "An error occurred",
                            "STREAM_ERROR");
                    try {
                        String json = OBJECT_MAPPER.writeValueAsString(err);
                        return "event: error\ndata: " + json + "\n\n";
                    } catch (Exception e) {
                        return "event: error\ndata: {\"message\":\"" + escapeJson(err.message()) + "\",\"code\":\"" + escapeJson(err.code()) + "\"}\n\n";
                    }
                });
    }

    private static String toSseLine(SseStreamEvent event) {
        if (event instanceof TokenStreamEvent tokenEvent) {
            try {
                String json = OBJECT_MAPPER.writeValueAsString(tokenEvent);
                return "event: token\ndata: " + json + "\n\n";
            } catch (Exception e) {
                return "event: token\ndata: {\"token\":\"\"}\n\n";
            }
        }
        if (event instanceof CancelledEvent) {
            return "event: cancelled\ndata: {}\n\n";
        }
        if (event instanceof ErrorStreamEvent errorEvent) {
            try {
                String json = OBJECT_MAPPER.writeValueAsString(errorEvent);
                return "event: error\ndata: " + json + "\n\n";
            } catch (Exception e) {
                return "event: error\ndata: {\"message\":\"" + escapeJson(errorEvent.message()) + "\",\"code\":\"" + escapeJson(errorEvent.code()) + "\"}\n\n";
            }
        }
        return "event: token\ndata: {\"token\":\"\"}\n\n";
    }

    private static String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
    }
}
