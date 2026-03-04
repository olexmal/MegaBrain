/*
 * Copyright (c) 2025 MegaBrain Contributors
 * Licensed under the MIT License - see LICENSE file for details.
 */

package io.megabrain.api;

import io.megabrain.ingestion.IngestionService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Path;

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
}
