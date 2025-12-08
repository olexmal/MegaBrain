/*
 * Copyright (c) 2025 MegaBrain Contributors
 * Licensed under the MIT License - see LICENSE file for details.
 */

package io.megabrain.ingestion.github;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * GitHub commit information DTO.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record GitHubCommitInfo(
        String sha,
        Commit commit
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Commit(
            String message,
            Author author
    ) {
        @JsonIgnoreProperties(ignoreUnknown = true)
        public record Author(
                String name,
                String email,
                String date
        ) {
        }
    }
}

