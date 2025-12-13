/*
 * Copyright (c) 2025 MegaBrain Contributors
 * Licensed under the MIT License - see LICENSE file for details.
 */

package io.megabrain.ingestion.gitlab;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * GitLab repository information DTO.
 * Maps to GitLab API v4 project response.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record GitLabRepositoryInfo(
        int id,
        String name,
        @JsonProperty("path_with_namespace") String pathWithNamespace,
        @JsonProperty("default_branch") String defaultBranch,
        @JsonProperty("http_url_to_repo") String httpUrlToRepo,
        @JsonProperty("ssh_url_to_repo") String sshUrlToRepo,
        @JsonProperty("web_url") String webUrl,
        Namespace namespace
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Namespace(
            int id,
            String name,
            @JsonProperty("path") String path,
            @JsonProperty("kind") String kind,
            @JsonProperty("full_path") String fullPath
    ) {
    }
}
