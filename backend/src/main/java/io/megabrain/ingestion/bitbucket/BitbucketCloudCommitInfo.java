/*
 * Copyright (c) 2025 MegaBrain Contributors
 * Licensed under the MIT License - see LICENSE file for details.
 */

package io.megabrain.ingestion.bitbucket;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Bitbucket Cloud commit information DTO.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record BitbucketCloudCommitInfo(
        String hash,
        String message,
        Author author
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Author(
            String raw,
            User user
    ) {
        @JsonIgnoreProperties(ignoreUnknown = true)
        public record User(
                String display_name,
                String nickname
        ) {
        }
    }
}
