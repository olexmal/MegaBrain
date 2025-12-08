/*
 * Copyright (c) 2025 MegaBrain Contributors
 * Licensed under the MIT License - see LICENSE file for details.
 */

package io.megabrain.ingestion.github;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * GitHub repository information DTO.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record GitHubRepositoryInfo(
        String name,
        @JsonProperty("full_name") String fullName,
        @JsonProperty("default_branch") String defaultBranch,
        @JsonProperty("clone_url") String cloneUrl,
        @JsonProperty("ssh_url") String sshUrl,
        Owner owner
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Owner(
            String login
    ) {
    }
}

