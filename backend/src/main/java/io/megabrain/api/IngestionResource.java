/*
 * Copyright (c) 2025 MegaBrain Contributors
 * Licensed under the MIT License - see LICENSE file for details.
 */

package io.megabrain.api;

import io.megabrain.ingestion.IngestionService;
import io.megabrain.ingestion.ProgressEvent;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * REST endpoint for repository ingestion operations.
 */
@ApplicationScoped
@Path("/api/v1/ingest")
public class IngestionResource {

    private final IngestionService ingestionService;

    @Inject
    public IngestionResource(IngestionService ingestionService) {
        this.ingestionService = ingestionService;
    }

    public enum SourceType {
        GITHUB, GITLAB, BITBUCKET, LOCAL;

        public static SourceType fromString(String source) {
            if (source == null) return null;
            try {
                return valueOf(source.toUpperCase());
            } catch (IllegalArgumentException e) {
                return null;
            }
        }
    }

    @POST
    @Path("/{source}")
    @Produces(MediaType.SERVER_SENT_EVENTS)
    public Multi<String> ingest(@PathParam("source") String source, @Valid @NotNull IngestionRequest request) {
        SourceType sourceType = SourceType.fromString(source);
        if (sourceType == null) {
            throw new jakarta.ws.rs.WebApplicationException(
                    Response.status(Response.Status.BAD_REQUEST)
                            .entity("Invalid source: " + source + ". Supported sources: github, gitlab, bitbucket, local")
                            .build()
            );
        }

        try {
            Multi<ProgressEvent> progressStream;
            if (Boolean.TRUE.equals(request.incremental())) {
                progressStream = ingestionService.ingestRepositoryIncrementally(request.repository());
            } else {
                progressStream = ingestionService.ingestRepository(request.repository());
            }

            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            mapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());

            return progressStream.map(pe -> {
                io.megabrain.ingestion.StreamEvent.Stage stage = io.megabrain.ingestion.StreamEvent.Stage.INDEXING;
                String msg = pe.message() != null ? pe.message().toLowerCase() : "";
                if (msg.contains("clone") || msg.contains("cloning")) {
                    stage = io.megabrain.ingestion.StreamEvent.Stage.CLONING;
                } else if (msg.contains("pars")) {
                    stage = io.megabrain.ingestion.StreamEvent.Stage.PARSING;
                } else if (msg.contains("success") || msg.contains("completed")) {
                    stage = io.megabrain.ingestion.StreamEvent.Stage.COMPLETE;
                } else if (msg.contains("fail") || msg.contains("error")) {
                    stage = io.megabrain.ingestion.StreamEvent.Stage.FAILED;
                }
                
                io.megabrain.ingestion.StreamEvent event = io.megabrain.ingestion.StreamEvent.of(stage, pe.message(), (int) pe.progress());
                try {
                    return "event: progress\ndata: " + mapper.writeValueAsString(event) + "\n\n";
                } catch (Exception e) {
                    return "event: progress\ndata: {\"stage\":\"FAILED\",\"message\":\"Serialization error\",\"percentage\":0}\n\n";
                }
            }).onFailure().recoverWithItem(error -> {
                io.megabrain.ingestion.StreamEvent event = io.megabrain.ingestion.StreamEvent.of(
                    io.megabrain.ingestion.StreamEvent.Stage.FAILED, 
                    "Ingestion failed: " + (error != null ? error.getMessage() : "Unknown error"), 
                    0
                );
                try {
                    return "event: progress\ndata: " + mapper.writeValueAsString(event) + "\n\n";
                } catch (Exception e) {
                    return "event: progress\ndata: {\"stage\":\"FAILED\",\"message\":\"Ingestion failed\",\"percentage\":0}\n\n";
                }
            });

        } catch (IllegalArgumentException e) {
            throw new jakarta.ws.rs.WebApplicationException(
                    Response.status(Response.Status.BAD_REQUEST)
                            .entity("Invalid request: " + e.getMessage())
                            .build()
            );
        } catch (Exception e) {
            throw new jakarta.ws.rs.WebApplicationException(
                    Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                            .entity("Failed to start ingestion: " + e.getMessage())
                            .build()
            );
        }
    }
}
