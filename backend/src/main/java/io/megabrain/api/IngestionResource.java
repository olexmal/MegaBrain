/*
 * Copyright (c) 2025 MegaBrain Contributors
 * Licensed under the MIT License - see LICENSE file for details.
 */

package io.megabrain.api;

import io.megabrain.ingestion.IngestionService;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
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
    public Uni<Response> ingest(@PathParam("source") String source) {
        SourceType sourceType = SourceType.fromString(source);
        if (sourceType == null) {
            return Uni.createFrom().item(Response.status(Response.Status.BAD_REQUEST)
                    .entity("Invalid source: " + source + ". Supported sources: github, gitlab, bitbucket, local")
                    .build());
        }

        // Routing to the ingestion service will be fully implemented in T4.
        return Uni.createFrom().item(Response.accepted().build());
    }
}
