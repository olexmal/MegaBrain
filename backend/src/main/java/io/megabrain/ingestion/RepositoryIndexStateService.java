/*
 * Copyright (c) 2025 MegaBrain Contributors
 * Licensed under the MIT License - see LICENSE file for details.
 */

package io.megabrain.ingestion;

import io.megabrain.repository.RepositoryIndexStateRepository;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.util.Optional;

/**
 * Service for managing repository indexing state.
 * Provides high-level operations for tracking and updating the last indexed commit SHA per repository.
 */
@ApplicationScoped
public class RepositoryIndexStateService {

    private static final Logger LOG = Logger.getLogger(RepositoryIndexStateService.class);

    @Inject
    RepositoryIndexStateRepository repository;

    /**
     * Gets the last indexed commit SHA for a repository.
     *
     * @param repositoryUrl the repository URL
     * @return a Uni that emits the last indexed commit SHA, or empty if not found
     */
    public Uni<Optional<String>> getLastIndexedCommitSha(String repositoryUrl) {
        return repository.findByRepositoryUrl(repositoryUrl)
                .map(state -> state.map(RepositoryIndexState::lastIndexedCommitSha));
    }

    /**
     * Updates the last indexed commit SHA for a repository.
     * Creates a new state record if one doesn't exist, or updates the existing one.
     *
     * @param repositoryUrl the repository URL
     * @param commitSha the commit SHA that was successfully indexed
     * @return a Uni that emits the updated state
     */
    public Uni<RepositoryIndexState> updateLastIndexedCommitSha(String repositoryUrl, String commitSha) {
        LOG.debugf("Updating last indexed commit SHA for repository %s to %s", repositoryUrl, commitSha);

        return repository.findByRepositoryUrl(repositoryUrl)
                .flatMap(existingState -> {
                    RepositoryIndexState newState = RepositoryIndexState.create(repositoryUrl, commitSha);
                    return repository.save(newState)
                            .invoke(saved -> LOG.infof("Updated index state for repository %s: %s",
                                    repositoryUrl, commitSha));
                });
    }

    /**
     * Checks if a repository has been indexed before (has an index state).
     *
     * @param repositoryUrl the repository URL
     * @return a Uni that emits true if the repository has been indexed before
     */
    public Uni<Boolean> hasBeenIndexed(String repositoryUrl) {
        return repository.existsByRepositoryUrl(repositoryUrl);
    }

    /**
     * Removes the index state for a repository.
     * This is useful when re-indexing a repository from scratch.
     *
     * @param repositoryUrl the repository URL
     * @return a Uni that emits true if the state was removed
     */
    public Uni<Boolean> clearIndexState(String repositoryUrl) {
        LOG.debugf("Clearing index state for repository: %s", repositoryUrl);
        return repository.deleteByRepositoryUrl(repositoryUrl)
                .invoke(deleted -> {
                    if (deleted) {
                        LOG.infof("Cleared index state for repository: %s", repositoryUrl);
                    }
                });
    }
}
