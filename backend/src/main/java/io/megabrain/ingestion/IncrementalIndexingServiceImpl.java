/*
 * Copyright (c) 2025 MegaBrain Contributors
 * Licensed under the MIT License - see LICENSE file for details.
 */

package io.megabrain.ingestion;

import io.megabrain.core.IndexService;
import io.megabrain.ingestion.parser.CodeParser;
import io.megabrain.ingestion.parser.ParserRegistry;
import io.megabrain.ingestion.parser.TextChunk;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.util.function.Consumer;

import java.time.Duration;
import java.time.Instant;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Implementation of IncrementalIndexingService that handles git-based incremental indexing.
 * Currently focused on handling added files (T3).
 */
@ApplicationScoped
public class IncrementalIndexingServiceImpl implements IncrementalIndexingService {

    private static final Logger LOG = Logger.getLogger(IncrementalIndexingServiceImpl.class);

    @Inject
    GitDiffService gitDiffService;

    @Inject
    RepositoryIndexStateService indexStateService;

    @Inject
    ParserRegistry parserRegistry;

    @Inject
    IndexService indexService;

    @Override
    public Uni<Integer> indexChangesSinceLastCommit(Path repositoryPath, String repositoryUrl) {
        return gitDiffService.detectChangesSinceLastIndex(repositoryPath, repositoryUrl)
                .flatMap(changes -> processFileChanges(repositoryPath, repositoryUrl, changes));
    }

    @Override
    public Uni<Integer> processFileChanges(Path repositoryPath, String repositoryUrl, List<FileChange> changes) {
        return processFileChangesWithProgress(repositoryPath, repositoryUrl, changes, null);
    }

    /**
     * Process file changes with optional progress reporting.
     */
    public Uni<Integer> processFileChangesWithProgress(Path repositoryPath, String repositoryUrl,
                                                      List<FileChange> changes, Consumer<String> progressCallback) {
        return Uni.createFrom().item(() -> {
            Instant startTime = Instant.now();

            if (changes == null || changes.isEmpty()) {
                LOG.debugf("No changes to process for repository: %s", repositoryUrl);
                return 0;
            }

            int processedFiles = 0;
            int totalFiles = changes.size();

            if (progressCallback != null) {
                progressCallback.accept("Starting to process " + totalFiles + " changed files");
            }

            // Group changes by type for processing
            List<FileChange> addedFiles = changes.stream()
                    .filter(change -> change.changeType() == ChangeType.ADDED)
                    .toList();

            List<FileChange> modifiedFiles = changes.stream()
                    .filter(change -> change.changeType() == ChangeType.MODIFIED)
                    .toList();

            List<FileChange> deletedFiles = changes.stream()
                    .filter(change -> change.changeType() == ChangeType.DELETED)
                    .toList();

            List<FileChange> renamedFiles = changes.stream()
                    .filter(change -> change.changeType() == ChangeType.RENAMED)
                    .toList();

            // Process added files (T3)
            processedFiles += processAddedFilesWithProgress(repositoryPath, addedFiles, progressCallback);
            if (progressCallback != null) {
                progressCallback.accept("Processed " + processedFiles + "/" + totalFiles + " files (added)");
            }

            // Process modified files (T4)
            processedFiles += processModifiedFilesWithProgress(repositoryPath, modifiedFiles, progressCallback);
            if (progressCallback != null) {
                progressCallback.accept("Processed " + processedFiles + "/" + totalFiles + " files (modified)");
            }

            // Process deleted files (T5)
            processedFiles += processDeletedFiles(repositoryPath, deletedFiles);
            if (progressCallback != null) {
                progressCallback.accept("Processed " + processedFiles + "/" + totalFiles + " files (deleted)");
            }

            // Process renamed files (T6)
            processedFiles += processRenamedFilesWithProgress(repositoryPath, renamedFiles, progressCallback);
            if (progressCallback != null) {
                progressCallback.accept("Processed " + processedFiles + "/" + totalFiles + " files (renamed)");
            }

            // Update the last indexed commit SHA after successful processing
            if (processedFiles > 0) {
                // For now, assume we're processing up to HEAD
                // In a real implementation, we'd get the actual commit SHA that was processed
                updateLastIndexedCommitSha(repositoryUrl, "HEAD");
            }

                    Duration duration = Duration.between(startTime, Instant.now());
                    LOG.infof("Processed %d files for repository %s in %d ms", processedFiles, repositoryUrl, duration.toMillis());
                    return processedFiles;
        });
    }

    /**
     * Processes renamed files by removing old chunks and adding new chunks at the new location.
     */
    private int processRenamedFiles(Path repositoryPath, List<FileChange> renamedFiles) {
        return processRenamedFilesWithProgress(repositoryPath, renamedFiles, null);
    }

    /**
     * Processes renamed files by removing old chunks and adding new chunks at the new location with progress reporting.
     */
    private int processRenamedFilesWithProgress(Path repositoryPath, List<FileChange> renamedFiles, Consumer<String> progressCallback) {
        if (renamedFiles.isEmpty()) {
            return 0;
        }

        LOG.debugf("Processing %d renamed files", renamedFiles.size());

        int processedCount = 0;
        int totalFiles = renamedFiles.size();

        for (FileChange change : renamedFiles) {
            processedCount++; // Count every file we attempt to process

            try {
                // Construct paths for old and new locations
                Path oldFilePath = repositoryPath.resolve(change.oldPath());
                Path newFilePath = repositoryPath.resolve(change.filePath());

                // Remove chunks from the old location
                Integer removedCount = indexService.removeChunksForFile(oldFilePath.toString()).await().indefinitely();

                LOG.debugf("Removed %d chunks for renamed file from old path: %s", removedCount, change.oldPath());

                // Parse and index the file at the new location
                if (!Files.exists(newFilePath)) {
                    LOG.warnf("Renamed file does not exist at new location: %s", newFilePath);
                    // Report progress even for missing files
                    if (progressCallback != null && processedCount % Math.max(1, totalFiles / 5) == 0) {
                        progressCallback.accept(String.format("Processed renamed file %d/%d: %s (file missing)", processedCount, totalFiles, change.filePath()));
                    }
                    continue;
                }

                // Find appropriate parser for the file
                Optional<CodeParser> parser = parserRegistry.findParser(newFilePath);
                if (parser.isEmpty()) {
                    LOG.debugf("No parser found for renamed file: %s", newFilePath);
                    // Report progress even for unparseable files
                    if (progressCallback != null && processedCount % Math.max(1, totalFiles / 5) == 0) {
                        progressCallback.accept(String.format("Processed renamed file %d/%d: %s (no parser)", processedCount, totalFiles, change.filePath()));
                    }
                    continue;
                }

                // Re-parse the file at the new location
                List<TextChunk> newChunks = parser.get().parse(newFilePath);
                if (!newChunks.isEmpty()) {
                    // Index the new chunks
                    if (progressCallback != null) {
                        progressCallback.accept(String.format("Indexing %d chunks for renamed file: %s", newChunks.size(), change.filePath()));
                    }
                    indexService.addChunks(newChunks).await().indefinitely();
                    LOG.debugf("Re-indexed %d chunks for renamed file at new path: %s", newChunks.size(), change.filePath());
                } else {
                    LOG.debugf("Parsed 0 chunks from renamed file: %s", change.filePath());
                }

                // Report progress for parsing
                if (progressCallback != null && processedCount % Math.max(1, totalFiles / 5) == 0) {
                    progressCallback.accept(String.format("Processed renamed file %d/%d: %s", processedCount, totalFiles, change.filePath()));
                }

            } catch (Exception e) {
                LOG.errorf(e, "Failed to process renamed file: %s -> %s", change.oldPath(), change.filePath());
                // Still count as processed even if processing failed
            }
        }

        return processedCount;
    }

    /**
     * Processes deleted files by removing their chunks from the index.
     */
    private int processDeletedFiles(Path repositoryPath, List<FileChange> deletedFiles) {
        if (deletedFiles.isEmpty()) {
            return 0;
        }

        LOG.debugf("Processing %d deleted files", deletedFiles.size());

        int processedCount = 0;

        for (FileChange change : deletedFiles) {
            try {
                // Construct the absolute path for the deleted file
                // Since the file is deleted, we can't resolve it, but we can construct the path
                Path deletedFilePath = repositoryPath.resolve(change.filePath());

                // Remove all chunks for this deleted file
                Integer removedCount = indexService.removeChunksForFile(deletedFilePath.toString())
                        .await().indefinitely();

                LOG.debugf("Removed %d chunks for deleted file: %s", removedCount, change.filePath());
                processedCount++;

            } catch (Exception e) {
                LOG.errorf(e, "Failed to remove chunks for deleted file: %s", change.filePath());
            }
        }

        return processedCount;
    }

    /**
     * Processes modified files by removing old chunks and re-parsing/re-indexing.
     */
    private int processModifiedFiles(Path repositoryPath, List<FileChange> modifiedFiles) {
        return processModifiedFilesWithProgress(repositoryPath, modifiedFiles, null);
    }

    /**
     * Processes modified files by removing old chunks and re-parsing/re-indexing with progress reporting.
     */
    private int processModifiedFilesWithProgress(Path repositoryPath, List<FileChange> modifiedFiles, Consumer<String> progressCallback) {
        if (modifiedFiles.isEmpty()) {
            return 0;
        }

        LOG.debugf("Processing %d modified files", modifiedFiles.size());

        int processedCount = 0;
        int totalFiles = modifiedFiles.size();

        for (FileChange change : modifiedFiles) {
            Path filePath = repositoryPath.resolve(change.filePath());

            processedCount++; // Count every file we attempt to process

            if (!Files.exists(filePath)) {
                LOG.warnf("Modified file does not exist: %s", filePath);
                continue;
            }

            try {
                // Remove old chunks for this file
                Integer removedCount = indexService.removeChunksForFile(filePath.toString()).await().indefinitely();

                LOG.debugf("Removed %d old chunks for modified file: %s", removedCount, change.filePath());

                // Find appropriate parser for the file
                Optional<CodeParser> parser = parserRegistry.findParser(filePath);
                if (parser.isEmpty()) {
                    LOG.debugf("No parser found for modified file: %s", filePath);
                    // Report progress even for unparseable files
                    if (progressCallback != null && processedCount % Math.max(1, totalFiles / 5) == 0) {
                        progressCallback.accept(String.format("Processed modified file %d/%d: %s (no parser)", processedCount, totalFiles, change.filePath()));
                    }
                    continue;
                }

                // Re-parse the modified file
                List<TextChunk> newChunks = parser.get().parse(filePath);
                if (!newChunks.isEmpty()) {
                    // Index the new chunks
                    if (progressCallback != null) {
                        progressCallback.accept(String.format("Indexing %d chunks for modified file: %s", newChunks.size(), change.filePath()));
                    }
                    indexService.addChunks(newChunks).await().indefinitely();
                    LOG.debugf("Re-indexed %d chunks for modified file: %s", newChunks.size(), change.filePath());
                } else {
                    LOG.debugf("Parsed 0 chunks from modified file: %s", change.filePath());
                }

                // Report progress for parsing
                if (progressCallback != null && processedCount % Math.max(1, totalFiles / 5) == 0) {
                    progressCallback.accept(String.format("Processed modified file %d/%d: %s", processedCount, totalFiles, change.filePath()));
                }
            } catch (Exception e) {
                LOG.errorf(e, "Failed to process modified file: %s", filePath);
                // Still count as processed even if processing failed
            }
        }

        return processedCount;
    }

    /**
     * Processes added files by parsing and indexing them.
     */
    private int processAddedFiles(Path repositoryPath, List<FileChange> addedFiles) {
        return processAddedFilesWithProgress(repositoryPath, addedFiles, null);
    }

    /**
     * Processes added files by parsing and indexing them with progress reporting.
     */
    private int processAddedFilesWithProgress(Path repositoryPath, List<FileChange> addedFiles, Consumer<String> progressCallback) {
        if (addedFiles.isEmpty()) {
            return 0;
        }

        LOG.debugf("Processing %d added files", addedFiles.size());

        List<TextChunk> allChunks = new ArrayList<>();
        int processedCount = 0;
        int totalFiles = addedFiles.size();

        for (int i = 0; i < addedFiles.size(); i++) {
            FileChange change = addedFiles.get(i);
            Path filePath = repositoryPath.resolve(change.filePath());

            processedCount++; // Count every file we attempt to process

            if (!Files.exists(filePath)) {
                LOG.warnf("Added file does not exist: %s", filePath);
                continue;
            }

            // Find appropriate parser for the file
            Optional<CodeParser> parser = parserRegistry.findParser(filePath);
            if (parser.isEmpty()) {
                LOG.debugf("No parser found for file: %s", filePath);
                // Report progress even for unparseable files
                if (progressCallback != null && (i + 1) % Math.max(1, totalFiles / 5) == 0) {
                    progressCallback.accept(String.format("Processed added file %d/%d: %s (no parser)", processedCount, totalFiles, change.filePath()));
                }
                continue;
            }

            try {
                // Parse the file
                List<TextChunk> chunks = parser.get().parse(filePath);
                if (!chunks.isEmpty()) {
                    allChunks.addAll(chunks);
                    LOG.debugf("Parsed %d chunks from added file: %s", chunks.size(), change.filePath());
                } else {
                    LOG.debugf("Parsed 0 chunks from added file: %s", change.filePath());
                }

                // Report progress for parsing
                if (progressCallback != null && processedCount % Math.max(1, totalFiles / 5) == 0) {
                    progressCallback.accept(String.format("Parsed added file %d/%d: %s", processedCount, totalFiles, change.filePath()));
                }
            } catch (Exception e) {
                LOG.errorf(e, "Failed to parse added file: %s", filePath);
                // Still count as processed even if parsing failed
            }
        }

        // Index all the chunks with progress reporting
        if (!allChunks.isEmpty()) {
            if (progressCallback != null) {
                progressCallback.accept("Indexing " + allChunks.size() + " chunks from added files");
            }
            indexService.addChunks(allChunks).await().indefinitely();
            LOG.debugf("Indexed %d chunks from %d added files", allChunks.size(), processedCount);
            if (progressCallback != null) {
                progressCallback.accept("Completed indexing " + allChunks.size() + " chunks");
            }
        }

        return processedCount;
    }

    /**
     * Updates the last indexed commit SHA for the repository.
     */
    private void updateLastIndexedCommitSha(String repositoryUrl, String commitSha) {
        try {
            indexStateService.updateLastIndexedCommitSha(repositoryUrl, commitSha)
                    .await().indefinitely();
            LOG.debugf("Updated last indexed commit SHA for %s to %s", repositoryUrl, commitSha);
        } catch (Exception e) {
            LOG.errorf(e, "Failed to update last indexed commit SHA for %s", repositoryUrl);
        }
    }
}
