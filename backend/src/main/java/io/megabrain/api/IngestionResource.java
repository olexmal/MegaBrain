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
    public Uni<Response> ingest(@PathParam("source") String source, @Valid @NotNull IngestionRequest request) {
        SourceType sourceType = SourceType.fromString(source);
        if (sourceType == null) {
            return Uni.createFrom().item(Response.status(Response.Status.BAD_REQUEST)
                    .entity("Invalid source: " + source + ". Supported sources: github, gitlab, bitbucket, local")
                    .build());
        }

        try {
            Multi<ProgressEvent> progressStream;
            if (Boolean.TRUE.equals(request.incremental())) {
                progressStream = ingestionService.ingestRepositoryIncrementally(request.repository());
            } else {
                progressStream = ingestionService.ingestRepository(request.repository());
            }
            
            // Start the ingestion process in the background for now (T5 will implement SSE streaming)
            progressStream.subscribe().with(
                event -> {},
                failure -> {}
            );

            return Uni.createFrom().item(Response.accepted().build());
        } catch (IllegalArgumentException e) {
            return Uni.createFrom().item(Response.status(Response.Status.BAD_REQUEST)
                    .entity("Invalid request: " + e.getMessage())
                    .build());
        } catch (Exception e) {
            return Uni.createFrom().item(Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Failed to start ingestion: " + e.getMessage())
                    .build());
        }
    }
}
