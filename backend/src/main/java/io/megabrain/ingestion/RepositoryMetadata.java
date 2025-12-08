/*
 * Copyright (c) 2025 MegaBrain Contributors
 * Licensed under the MIT License - see LICENSE file for details.
 */

package io.megabrain.ingestion;

/**
 * Repository metadata information.
 *
 * @param name the repository name
 * @param owner the repository owner/organization
 * @param defaultBranch the default branch name
 * @param latestCommitSha the latest commit SHA
 * @param url the repository URL
 */
public record RepositoryMetadata(
        String name,
        String owner,
        String defaultBranch,
        String latestCommitSha,
        String url
) {
}

