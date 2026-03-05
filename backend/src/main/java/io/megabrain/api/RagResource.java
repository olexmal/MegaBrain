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
import jakarta.ws.rs.core.Response;

import java.util.Map;

import org.jboss.logging.Logger;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.megabrain.api.CancelledEvent;
import io.megabrain.api.ErrorStreamEvent;
import io.megabrain.api.SseStreamEvent;
import io.megabrain.api.TokenStreamEvent;
import io.megabrain.core.RagService;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;

/**
 * REST API resource for RAG question-answering with optional SSE token streaming (US-03-04).
 * Use {@code stream=true} (default) for Server-Sent Events; {@code stream=false} for a single JSON response.
 * Content negotiation: send Accept: text/event-stream for streaming, Accept: application/json for non-streaming.
 * Clients should use query param {@code stream=false} with Accept: application/json for non-streaming (demo script).
 * T4: Integrates with RagService (ask for non-streaming, streamTokens for streaming). Request fields
 * {@code context_limit} and {@code model} are accepted in RagRequest but not yet passed to RagService (deferred
 * until search/RAG pipeline provides context chunks and model selection).
 */
@Path("/rag")
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
     * Non-streaming RAG: returns full answer as JSON. Use Accept: application/json and optionally {@code stream=false}.
     */
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<Response> ragJson(@Valid RagRequest request) {
        String question = request != null && request.getQuestion() != null ? request.getQuestion().trim() : "";
        LOG.infof("RAG request: stream=false (JSON), question length=%d", question.length());
        return ragService.ask(question)
                .map(r -> Response.ok(r).type(MediaType.APPLICATION_JSON).build())
                .onFailure().recoverWithItem(throwable -> {
                    LOG.error("RAG non-streaming request failed", throwable);
                    return Response.status(Response.Status.SERVICE_UNAVAILABLE)
                            .type(MediaType.APPLICATION_JSON)
                            .entity(Map.of("error", "RAG request failed"))
                            .build();
                });
    }

    /**
     * Streaming RAG: returns SSE. Use Accept: text/event-stream or call POST /api/v1/rag/stream.
     */
    @POST
    @Produces(MediaType.SERVER_SENT_EVENTS)
    public Multi<String> ragStream(@Valid RagRequest request) {
        String question = request != null && request.getQuestion() != null ? request.getQuestion().trim() : "";
        LOG.infof("RAG request: stream=true (SSE), question length=%d", question.length());
        return streamRag(question);
    }

    /**
     * SSE streaming endpoint for RAG. Returns Multi so the framework streams the response.
     * Use POST /api/v1/rag/stream with same body as /rag for streaming; Accept: text/event-stream.
     */
    @POST
    @Path("stream")
    @Produces(MediaType.SERVER_SENT_EVENTS)
    public Multi<String> streamRag(@Valid RagRequest request) {
        String question = request != null && request.getQuestion() != null ? request.getQuestion().trim() : "";
        return streamRag(question);
    }

    private Multi<String> streamRag(String question) {
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
