/*
 * Copyright (c) 2025 MegaBrain Contributors
 * Licensed under the MIT License - see LICENSE file for details.
 */

package io.megabrain.ingestion;

import java.time.Instant;

/**
 * Tracks the indexing state for a repository, specifically the last commit SHA that was successfully indexed.
 * This enables incremental indexing by comparing the current HEAD with the last indexed commit.
 *
 * @param repositoryUrl the repository URL (unique identifier)
 * @param lastIndexedCommitSha the SHA of the last commit that was successfully indexed
 * @param lastIndexedAt timestamp when the repository was last indexed (ISO-8601 format)
 */
public record RepositoryIndexState(
        String repositoryUrl,
        String lastIndexedCommitSha,
        String lastIndexedAt
) {
    public RepositoryIndexState {
        if (repositoryUrl == null || repositoryUrl.isBlank()) {
            throw new IllegalArgumentException("repositoryUrl must not be null or blank");
        }
        if (lastIndexedCommitSha == null || lastIndexedCommitSha.isBlank()) {
            throw new IllegalArgumentException("lastIndexedCommitSha must not be null or blank");
        }
        if (lastIndexedAt == null || lastIndexedAt.isBlank()) {
            throw new IllegalArgumentException("lastIndexedAt must not be null or blank");
        }
    }

    /**
     * Creates a new RepositoryIndexState with the current timestamp.
     */
    public static RepositoryIndexState create(String repositoryUrl, String lastIndexedCommitSha) {
        return new RepositoryIndexState(
                repositoryUrl,
                lastIndexedCommitSha,
                Instant.now().toString()
        );
    }
}
