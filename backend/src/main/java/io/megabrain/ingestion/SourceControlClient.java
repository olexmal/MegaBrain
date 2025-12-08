/*
 * Copyright (c) 2025 MegaBrain Contributors
 * Licensed under the MIT License - see LICENSE file for details.
 */

package io.megabrain.ingestion;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;

import java.nio.file.Path;
import java.util.List;

/**
 * Interface for source control system clients.
 * Provides unified access to different SCM providers (GitHub, GitLab, Bitbucket, etc.).
 */
public interface SourceControlClient {

    /**
     * Checks if this client can handle the given repository URL.
     *
     * @param repositoryUrl the repository URL to check
     * @return true if this client can handle the URL, false otherwise
     */
    boolean canHandle(String repositoryUrl);

    /**
     * Fetches repository metadata (name, owner, branch, commit SHA).
     *
     * @param repositoryUrl the repository URL
     * @return a Uni that emits the repository metadata
     */
    Uni<RepositoryMetadata> fetchMetadata(String repositoryUrl);

    /**
     * Clones the repository to a temporary directory.
     *
     * @param repositoryUrl the repository URL
     * @param branch the branch to clone (null for default branch)
     * @return a Multi that emits progress events and completes with the path to the cloned repository
     */
    Multi<ProgressEvent> cloneRepository(String repositoryUrl, String branch);

    /**
     * Extracts source files from a cloned repository.
     *
     * @param repositoryPath the path to the cloned repository
     * @return a Multi that emits progress events and completes with a list of extracted files
     */
    Multi<ProgressEvent> extractFiles(Path repositoryPath);

    /**
     * Gets the cloned repository path after cloning is complete.
     * This should be called after cloneRepository completes.
     *
     * @return the path to the cloned repository, or null if not yet cloned
     */
    Path getClonedRepositoryPath();
}

