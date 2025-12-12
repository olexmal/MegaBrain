/*
 * Copyright (c) 2025 MegaBrain Contributors
 * Licensed under the MIT License - see LICENSE file for details.
 */

package io.megabrain.ingestion;

import io.smallrye.mutiny.Uni;

import java.nio.file.Path;
import java.util.List;
import java.util.function.Consumer;

/**
 * Service for performing incremental indexing based on git changes.
 * Supports adding, modifying, deleting, and renaming files in the index.
 */
public interface IncrementalIndexingService {

    /**
     * Performs incremental indexing for a repository based on changes since the last indexed commit.
     *
     * @param repositoryPath path to the local git repository
     * @param repositoryUrl repository URL for tracking indexing state
     * @return a Uni that emits the number of files processed
     */
    Uni<Integer> indexChangesSinceLastCommit(Path repositoryPath, String repositoryUrl);

    /**
     * Processes a list of file changes for incremental indexing.
     *
     * @param repositoryPath path to the local git repository
     * @param repositoryUrl repository URL for tracking indexing state
     * @param changes list of file changes to process
     * @return a Uni that emits the number of files processed
     */
    Uni<Integer> processFileChanges(Path repositoryPath, String repositoryUrl, List<FileChange> changes);

    /**
     * Processes a list of file changes for incremental indexing with progress reporting.
     *
     * @param repositoryPath path to the local git repository
     * @param repositoryUrl repository URL for tracking indexing state
     * @param changes list of file changes to process
     * @param progressCallback callback for progress messages (may be null)
     * @return a Uni that emits the number of files processed
     */
    Uni<Integer> processFileChangesWithProgress(Path repositoryPath, String repositoryUrl,
                                               List<FileChange> changes, Consumer<String> progressCallback);
}
