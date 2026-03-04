/*
 * Copyright (c) 2025 MegaBrain Contributors
 * Licensed under the MIT License - see LICENSE file for details.
 */

package io.megabrain.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;

/**
 * DTO for repository ingestion request.
 */
public record IngestionRequest(
    @NotNull(message = "Repository URL must not be null")
    @JsonProperty("repository")
    String repository,

    @JsonProperty("branch")
    String branch,

    @JsonProperty("token")
    String token,

    @JsonProperty("incremental")
    Boolean incremental
) {
}
