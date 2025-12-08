/*
 * Copyright (c) 2025 MegaBrain Contributors
 * Licensed under the MIT License - see LICENSE file for details.
 */

package io.megabrain.ingestion.bitbucket;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Bitbucket Cloud repository information DTO.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record BitbucketCloudRepositoryInfo(
        String name,
        String slug,
        String description,
        @JsonProperty("mainbranch") MainBranch mainbranch,
        Links links
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record MainBranch(
            String name,
            String type
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Links(
            @JsonProperty("clone") List<CloneLink> cloneLinks
    ) {
        @JsonIgnoreProperties(ignoreUnknown = true)
        public record CloneLink(
                String name,
                String href
        ) {
        }
    }
}
