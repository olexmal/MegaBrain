/*
 * Copyright (c) 2025 MegaBrain Contributors
 * Licensed under the MIT License - see LICENSE file for details.
 */

package io.megabrain.ingestion.gitlab;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * GitLab commit information DTO.
 * Maps to GitLab API v4 commit response.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record GitLabCommitInfo(
        String id,
        String short_id,
        String title,
        String message,
        Author author,
        String web_url
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Author(
            String name,
            String email,
            String date
    ) {
    }
}
