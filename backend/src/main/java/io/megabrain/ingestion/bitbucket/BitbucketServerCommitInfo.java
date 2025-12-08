/*
 * Copyright (c) 2025 MegaBrain Contributors
 * Licensed under the MIT License - see LICENSE file for details.
 */

package io.megabrain.ingestion.bitbucket;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * Bitbucket Server commit information DTO.
 * Note: Server API returns commits in a paginated format.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record BitbucketServerCommitInfo(
        List<Commit> values
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Commit(
            String id,
            String message,
            Author author
    ) {
        @JsonIgnoreProperties(ignoreUnknown = true)
        public record Author(
                String name,
                String emailAddress
        ) {
        }
    }

    /**
     * Gets the first (latest) commit from the list.
     */
    public Commit getLatestCommit() {
        return values != null && !values.isEmpty() ? values.get(0) : null;
    }
}
