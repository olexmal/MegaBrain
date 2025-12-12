/*
 * Copyright (c) 2025 MegaBrain Contributors
 * Licensed under the MIT License - see LICENSE file for details.
 */

package io.megabrain.repository;

import io.megabrain.ingestion.RepositoryIndexState;
import io.smallrye.mutiny.Uni;

import java.util.Optional;

/**
 * Repository interface for managing repository indexing state.
 * Provides methods to store and retrieve the last indexed commit SHA for repositories.
 */
public interface RepositoryIndexStateRepository {

    /**
     * Finds the indexing state for a repository by its URL.
     *
     * @param repositoryUrl the repository URL
     * @return a Uni that emits the repository index state if found, or empty if not found
     */
    Uni<Optional<RepositoryIndexState>> findByRepositoryUrl(String repositoryUrl);

    /**
     * Saves or updates the indexing state for a repository.
     * If the repository already exists, it will be updated with the new state.
     *
     * @param state the repository index state to save
     * @return a Uni that emits the saved state
     */
    Uni<RepositoryIndexState> save(RepositoryIndexState state);

    /**
     * Deletes the indexing state for a repository.
     *
     * @param repositoryUrl the repository URL
     * @return a Uni that emits true if the state was deleted, false if it didn't exist
     */
    Uni<Boolean> deleteByRepositoryUrl(String repositoryUrl);

    /**
     * Checks if indexing state exists for a repository.
     *
     * @param repositoryUrl the repository URL
     * @return a Uni that emits true if state exists, false otherwise
     */
    Uni<Boolean> existsByRepositoryUrl(String repositoryUrl);
}
