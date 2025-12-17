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

    private final GitDiffService gitDiffService;
    private final RepositoryIndexStateService indexStateService;
    private final ParserRegistry parserRegistry;
    private final IndexService indexService;

    @Inject
    public IncrementalIndexingServiceImpl(GitDiffService gitDiffService,
                                         RepositoryIndexStateService indexStateService,
                                         ParserRegistry parserRegistry,
                                         IndexService indexService) {
        this.gitDiffService = gitDiffService;
        this.indexStateService = indexStateService;
        this.parserRegistry = parserRegistry;
        this.indexService = indexService;
    }

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
            processedCount++;
            processRenamedFile(repositoryPath, change, processedCount, totalFiles, progressCallback);
        }

        return processedCount;
    }

    private void processRenamedFile(Path repositoryPath, FileChange change, int processedCount, int totalFiles, Consumer<String> progressCallback) {
        try {
            Path oldFilePath = repositoryPath.resolve(change.oldPath());
            Path newFilePath = repositoryPath.resolve(change.filePath());

            Integer removedCount = indexService.removeChunksForFile(oldFilePath.toString()).await().indefinitely();
            LOG.debugf("Removed %d chunks for renamed file from old path: %s", removedCount, change.oldPath());

            if (!Files.exists(newFilePath)) {
                LOG.warnf("Renamed file does not exist at new location: %s", newFilePath);
                reportProgress(progressCallback, processedCount, totalFiles, change.filePath(), "file missing");
                return;
            }

            Optional<CodeParser> parser = parserRegistry.findParser(newFilePath);
            if (parser.isEmpty()) {
                LOG.debugf("No parser found for renamed file: %s", newFilePath);
                reportProgress(progressCallback, processedCount, totalFiles, change.filePath(), "no parser");
                return;
            }

            indexParsedChunks(parser.get(), newFilePath, change.filePath(), progressCallback);
            reportProgress(progressCallback, processedCount, totalFiles, change.filePath(), null);

        } catch (Exception e) {
            LOG.errorf(e, "Failed to process renamed file: %s -> %s", change.oldPath(), change.filePath());
        }
    }

    private void indexParsedChunks(CodeParser parser, Path filePath, String displayPath, Consumer<String> progressCallback) {
        List<TextChunk> chunks = parser.parse(filePath);
        if (!chunks.isEmpty()) {
            if (progressCallback != null) {
                progressCallback.accept(String.format("Indexing %d chunks for file: %s", chunks.size(), displayPath));
            }
            indexService.addChunks(chunks).await().indefinitely();
            LOG.debugf("Indexed %d chunks for file: %s", chunks.size(), displayPath);
        } else {
            LOG.debugf("Parsed 0 chunks from file: %s", displayPath);
        }
    }

    private void reportProgress(Consumer<String> callback, int processed, int total, String filePath, String suffix) {
        if (callback != null && processed % Math.max(1, total / 5) == 0) {
            String message = suffix != null
                    ? String.format("Processed file %d/%d: %s (%s)", processed, total, filePath, suffix)
                    : String.format("Processed file %d/%d: %s", processed, total, filePath);
            callback.accept(message);
        }
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
            processedCount++;
            processModifiedFile(repositoryPath, change, processedCount, totalFiles, progressCallback);
        }

        return processedCount;
    }

    private void processModifiedFile(Path repositoryPath, FileChange change, int processedCount, int totalFiles, Consumer<String> progressCallback) {
        Path filePath = repositoryPath.resolve(change.filePath());

        if (!Files.exists(filePath)) {
            LOG.warnf("Modified file does not exist: %s", filePath);
            return;
        }

        try {
            Integer removedCount = indexService.removeChunksForFile(filePath.toString()).await().indefinitely();
            LOG.debugf("Removed %d old chunks for modified file: %s", removedCount, change.filePath());

            Optional<CodeParser> parser = parserRegistry.findParser(filePath);
            if (parser.isEmpty()) {
                LOG.debugf("No parser found for modified file: %s", filePath);
                reportProgress(progressCallback, processedCount, totalFiles, change.filePath(), "no parser");
                return;
            }

            indexParsedChunks(parser.get(), filePath, change.filePath(), progressCallback);
            reportProgress(progressCallback, processedCount, totalFiles, change.filePath(), null);

        } catch (Exception e) {
            LOG.errorf(e, "Failed to process modified file: %s", filePath);
        }
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

        for (FileChange change : addedFiles) {
            processedCount++;
            List<TextChunk> chunks = parseAddedFile(repositoryPath, change, processedCount, totalFiles, progressCallback);
            allChunks.addAll(chunks);
        }

        indexCollectedChunks(allChunks, processedCount, progressCallback);
        return processedCount;
    }

    private List<TextChunk> parseAddedFile(Path repositoryPath, FileChange change, int processedCount, int totalFiles, Consumer<String> progressCallback) {
        Path filePath = repositoryPath.resolve(change.filePath());

        if (!Files.exists(filePath)) {
            LOG.warnf("Added file does not exist: %s", filePath);
            return List.of();
        }

        Optional<CodeParser> parser = parserRegistry.findParser(filePath);
        if (parser.isEmpty()) {
            LOG.debugf("No parser found for file: %s", filePath);
            reportProgress(progressCallback, processedCount, totalFiles, change.filePath(), "no parser");
            return List.of();
        }

        try {
            List<TextChunk> chunks = parser.get().parse(filePath);
            if (!chunks.isEmpty()) {
                LOG.debugf("Parsed %d chunks from added file: %s", chunks.size(), change.filePath());
            } else {
                LOG.debugf("Parsed 0 chunks from added file: %s", change.filePath());
            }
            reportProgress(progressCallback, processedCount, totalFiles, change.filePath(), null);
            return chunks;
        } catch (Exception e) {
            LOG.errorf(e, "Failed to parse added file: %s", filePath);
            return List.of();
        }
    }

    private void indexCollectedChunks(List<TextChunk> allChunks, int processedCount, Consumer<String> progressCallback) {
        if (allChunks.isEmpty()) {
            return;
        }

        if (progressCallback != null) {
            progressCallback.accept("Indexing " + allChunks.size() + " chunks from added files");
        }
        indexService.addChunks(allChunks).await().indefinitely();
        LOG.debugf("Indexed %d chunks from %d added files", allChunks.size(), processedCount);
        if (progressCallback != null) {
            progressCallback.accept("Completed indexing " + allChunks.size() + " chunks");
        }
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
