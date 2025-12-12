/*
 * Copyright (c) 2025 MegaBrain Contributors
 * Licensed under the MIT License - see LICENSE file for details.
 */

package io.megabrain.ingestion;

import io.smallrye.mutiny.Uni;

import java.nio.file.Path;
import java.util.List;

/**
 * Service for detecting file changes between git commits using JGit.
 * Provides functionality to compare two commits and identify added, modified, deleted, and renamed files.
 */
public interface GitDiffService {

    /**
     * Detects file changes between two commits in a git repository.
     *
     * @param repositoryPath path to the local git repository
     * @param oldCommitSha the older commit SHA to compare from
     * @param newCommitSha the newer commit SHA to compare to (can be "HEAD")
     * @return a Uni that emits a list of file changes
     */
    Uni<List<FileChange>> detectChanges(Path repositoryPath, String oldCommitSha, String newCommitSha);

    /**
     * Detects file changes between the last indexed commit and current HEAD.
     * This is a convenience method that integrates with the RepositoryIndexStateService.
     *
     * @param repositoryPath path to the local git repository
     * @param repositoryUrl repository URL to look up last indexed commit
     * @return a Uni that emits a list of file changes since last indexing
     */
    Uni<List<FileChange>> detectChangesSinceLastIndex(Path repositoryPath, String repositoryUrl);
}
