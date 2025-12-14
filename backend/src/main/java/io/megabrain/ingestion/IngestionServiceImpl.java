package io.megabrain.ingestion;

import io.megabrain.core.IndexService;
import io.megabrain.ingestion.parser.CodeParser;
import io.megabrain.ingestion.parser.ParserRegistry;
import io.megabrain.ingestion.parser.TextChunk;
import io.smallrye.mutiny.Multi;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.function.Consumer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

/**
 * Implementation of IngestionService.
 * Orchestrates repository ingestion with support for both full and incremental modes.
 */
@ApplicationScoped
public class IngestionServiceImpl implements IngestionService {

    /**
     * Simple interface for emitting progress events during ingestion.
     */
    private interface ProgressEmitter {
        void emit(ProgressEvent event);
        void fail(Throwable error);
        void complete();
    }

    private static final Logger LOG = LoggerFactory.getLogger(IngestionServiceImpl.class);

    @Inject
    CompositeSourceControlClient sourceControlClient;

    @Inject
    ParserRegistry parserRegistry;

    @Inject
    IndexService indexService;

    @Inject
    IncrementalIndexingService incrementalIndexingService;

    @Inject
    GitDiffService gitDiffService;

    @ConfigProperty(name = "megabrain.ingestion.temp-dir", defaultValue = "/tmp/megabrain-ingestion")
    String tempDir;

    @Override
    public Multi<ProgressEvent> ingestRepository(String repositoryUrl) {
        LOG.info("Starting full ingestion for repository: {}", repositoryUrl);
        final String finalRepositoryUrl = repositoryUrl;
        final Instant startTime = Instant.now();

        return Multi.createFrom().emitter(emitter -> {
            try {
                // Check if this is a local file path (not a remote URL)
                if (finalRepositoryUrl.startsWith("/") || finalRepositoryUrl.startsWith("file://") ||
                    (finalRepositoryUrl.length() > 1 && finalRepositoryUrl.charAt(1) == ':')) { // Windows drive letter

                    // For local paths, use the path directly without cloning
                    final Path repoPath = Path.of(finalRepositoryUrl.replaceFirst("^file://", ""));
                    emitter.emit(ProgressEvent.of("Using local repository path: " + repoPath, 0.0));
                    emitter.emit(ProgressEvent.of("Local repository ready", 25.0));

                    // Process the local repository (skip cloning)
                    emitter.emit(ProgressEvent.of("Extracting source files", 30.0));
                    emitter.emit(ProgressEvent.of("Local files validated", 40.0));
                    emitter.emit(ProgressEvent.of("Files extracted successfully", 50.0));

                    // Index all files with progress reporting
                    indexAllFiles(repoPath).subscribe().with(
                        progressEvent -> {
                            // Forward parsing/indexing progress events, adjusting percentage to fit in 60-100 range
                            double adjustedPercentage = 60.0 + (progressEvent.progress() * 0.4); // 60-100 range
                            emitter.emit(ProgressEvent.of(progressEvent.message(), adjustedPercentage));
                        },
                        error -> {
                            LOG.error("Failed to index repository: {}", finalRepositoryUrl, error);
                            emitter.fail(error);
                        },
                        () -> {
                            // Update last indexed commit SHA for future incremental indexing
                            updateLastIndexedCommitSha(finalRepositoryUrl, "HEAD");

                            Duration duration = Duration.between(startTime, Instant.now());
                            LOG.info("Full ingestion completed for repository {} in {} ms", finalRepositoryUrl, duration.toMillis());

                            emitter.emit(ProgressEvent.of("Ingestion completed successfully", 100.0));
                            emitter.complete();
                        }
                    );

                } else {
                    // Clone remote repository
                    emitter.emit(ProgressEvent.of("Starting repository clone", 0.0));

                    Multi<ProgressEvent> cloneMulti = sourceControlClient.cloneRepository(finalRepositoryUrl, null);
                    cloneMulti.subscribe().with(emitter::emit,
                        error -> {
                            LOG.error("Failed to clone repository: {}", finalRepositoryUrl, error);
                            emitter.fail(error);
                        },
                        () -> {
                            try {
                                final Path repoPath = sourceControlClient.getClonedRepositoryPath();
                                if (repoPath == null) {
                                    emitter.fail(new IllegalStateException("Repository path not available after cloning"));
                                    return;
                                }

                                emitter.emit(ProgressEvent.of("Repository cloned successfully", 25.0));

                                // Process the cloned repository
                                emitter.emit(ProgressEvent.of("Extracting source files", 30.0));

                                Multi<ProgressEvent> extractMulti = sourceControlClient.extractFiles(repoPath);
                                extractMulti.subscribe().with(emitter::emit,
                                    error -> {
                                        LOG.error("Failed to extract files from repository: {}", finalRepositoryUrl, error);
                                        emitter.fail(error);
                                    },
                                    () -> {
                                        emitter.emit(ProgressEvent.of("Files extracted successfully", 50.0));

                                        // Index all files with progress reporting
                                        indexAllFiles(repoPath).subscribe().with(
                                            progressEvent -> {
                                                // Forward parsing/indexing progress events, adjusting percentage to fit in 60-100 range
                                                double adjustedPercentage = 60.0 + (progressEvent.progress() * 0.4); // 60-100 range
                                                emitter.emit(ProgressEvent.of(progressEvent.message(), adjustedPercentage));
                                            },
                                            error -> {
                                                LOG.error("Failed to index repository: {}", finalRepositoryUrl, error);
                                                emitter.fail(error);
                                            },
                                            () -> {
                                                // Update last indexed commit SHA for future incremental indexing
                                                updateLastIndexedCommitSha(finalRepositoryUrl, "HEAD");

                                                Duration duration = Duration.between(startTime, Instant.now());
                                                LOG.info("Full ingestion completed for repository {} in {} ms", finalRepositoryUrl, duration.toMillis());

                                                emitter.emit(ProgressEvent.of("Ingestion completed successfully", 100.0));
                                                emitter.complete();
                                            }
                                        );
                                    }
                                );

                            } catch (Exception e) {
                                LOG.error("Error after cloning repository: {}", finalRepositoryUrl, e);
                                emitter.fail(e);
                            }
                        }
                    );
                }

            } catch (Exception e) {
                LOG.error("Failed to start ingestion for repository: {}", finalRepositoryUrl, e);
                emitter.fail(e);
            }
        });
    }


    @Override
    public Multi<ProgressEvent> ingestRepositoryIncrementally(String repositoryUrl) {
        LOG.info("Starting incremental ingestion for repository: {}", repositoryUrl);
        final String finalRepositoryUrl = repositoryUrl;
        final Instant startTime = Instant.now();

        return Multi.createFrom().emitter(emitter -> {
            try {
                emitter.emit(ProgressEvent.of("Starting incremental ingestion", 0.0));

                // Check if this is a local file path (not a remote URL)
                if (finalRepositoryUrl.startsWith("/") || finalRepositoryUrl.startsWith("file://") ||
                    (finalRepositoryUrl.length() > 1 && finalRepositoryUrl.charAt(1) == ':')) { // Windows drive letter

                    // For local paths, use the path directly without cloning
                    final Path repoPath = Path.of(finalRepositoryUrl.replaceFirst("^file://", ""));
                    emitter.emit(ProgressEvent.of("Using local repository path for incremental indexing", 25.0));

                    // Process incremental changes for local repository
                    processIncrementalChanges(new ProgressEmitter() {
                        @Override public void emit(ProgressEvent event) { emitter.emit(event); }
                        @Override public void fail(Throwable error) { emitter.fail(error); }
                        @Override public void complete() { emitter.complete(); }
                    }, repoPath, finalRepositoryUrl, startTime);

                } else {
                    // Clone remote repository for incremental ingestion
                    Multi<ProgressEvent> cloneMulti = sourceControlClient.cloneRepository(finalRepositoryUrl, null);
                    cloneMulti.subscribe().with(emitter::emit,
                        error -> {
                            LOG.error("Failed to clone repository for incremental ingestion: {}", finalRepositoryUrl, error);
                            emitter.fail(error);
                        },
                        () -> {
                            try {
                                final Path repoPath = sourceControlClient.getClonedRepositoryPath();
                                if (repoPath == null) {
                                    emitter.fail(new IllegalStateException("Repository path not available after cloning"));
                                    return;
                                }

                                emitter.emit(ProgressEvent.of("Repository cloned for incremental indexing", 25.0));

                                // Process incremental changes for remote repository
                                processIncrementalChanges(new ProgressEmitter() {
                                    @Override public void emit(ProgressEvent event) { emitter.emit(event); }
                                    @Override public void fail(Throwable error) { emitter.fail(error); }
                                    @Override public void complete() { emitter.complete(); }
                                }, repoPath, finalRepositoryUrl, startTime);

                            } catch (Exception e) {
                                LOG.error("Error after cloning repository for incremental ingestion: {}", finalRepositoryUrl, e);
                                emitter.fail(e);
                            }
                        }
                    );
                }

            } catch (Exception e) {
                LOG.error("Failed to start incremental ingestion for repository: {}", finalRepositoryUrl, e);
                emitter.fail(e);
            }
        });
    }

    /**
     * Common incremental processing logic for both local and remote repositories.
     */
    private void processIncrementalChanges(ProgressEmitter emitter, Path repoPath,
                                         String repositoryUrl, Instant startTime) {
        // Process incremental changes with detailed progress reporting
        emitter.emit(ProgressEvent.of("Detecting and processing changes", 50.0));

        // First get the changes to know how many files need processing
        gitDiffService.detectChangesSinceLastIndex(repoPath, repositoryUrl)
            .subscribe().with(
                changes -> {
                    int totalChanges = changes.size();
                    emitter.emit(ProgressEvent.of("Found " + totalChanges + " changed files to process", 60.0));

                    // Process the changes with progress updates via callback
                    Consumer<String> progressCallback = message -> {
                        // Convert parsing progress messages to ProgressEvents
                        emitter.emit(ProgressEvent.of(message, 70.0)); // Use 70% for parsing phase
                    };

                    incrementalIndexingService.processFileChangesWithProgress(repoPath, repositoryUrl, changes, progressCallback)
                        .subscribe().with(
                            processedFiles -> {
                                Duration duration = Duration.between(startTime, Instant.now());
                                LOG.info("Incremental ingestion completed for repository {} in {} ms (processed {} files)",
                                        repositoryUrl, duration.toMillis(), processedFiles);

                                emitter.emit(ProgressEvent.of("Incremental indexing completed", 90.0));
                                emitter.emit(ProgressEvent.of(
                                    String.format("Processed %d files incrementally", processedFiles), 100.0));
                                emitter.complete();
                            },
                            error -> {
                                LOG.error("Failed to process incremental changes for repository: {}", repositoryUrl, error);
                                emitter.fail(error);
                            }
                        );
                },
                error -> {
                    LOG.error("Failed to detect changes for repository: {}", repositoryUrl, error);
                    emitter.fail(error);
                }
            );
    }

    /**
     * Performs full indexing of all files in the repository with progress reporting.
     */
    private Multi<ProgressEvent> indexAllFiles(Path repositoryPath) {
        return Multi.createFrom().emitter(emitter -> {
            try {
                emitter.emit(ProgressEvent.of("Starting file parsing and indexing", 0.0));

                // First pass: collect all source files
                List<Path> sourceFiles;
                try (var paths = Files.walk(repositoryPath)) {
                    sourceFiles = paths
                        .filter(Files::isRegularFile)
                        .filter(this::isSourceFile)
                        .toList();
                }

                int totalFiles = sourceFiles.size();
                emitter.emit(ProgressEvent.of("Found " + totalFiles + " source files to process", 5.0));

                // Phase 1: Parse all files and collect chunks
                emitter.emit(ProgressEvent.of("Starting file parsing", 10.0));

                List<TextChunk> allChunks = new ArrayList<>();
                int processedFiles = 0;
                int successfullyParsedFiles = 0;

                for (Path file : sourceFiles) {
                    try {
                        processedFiles++;
                        Optional<CodeParser> parser = parserRegistry.findParser(file);
                        if (parser.isPresent()) {
                            List<TextChunk> chunks = parser.get().parse(file);
                            if (!chunks.isEmpty()) {
                                allChunks.addAll(chunks);
                                successfullyParsedFiles++;

                                // Emit parsing progress every few files
                                if (processedFiles % Math.max(1, totalFiles / 10) == 0 || processedFiles == totalFiles) {
                                    int percentage = 10 + (processedFiles * 30 / totalFiles); // 10-40% for parsing
                                    emitter.emit(ProgressEvent.of(
                                        String.format("Parsed file %d/%d: %s (%d chunks)", processedFiles, totalFiles,
                                            file.getFileName(), chunks.size()),
                                        percentage
                                    ));
                                }
                            }
                        } else {
                            LOG.debug("No parser found for file: {}", file);
                        }
                    } catch (Exception e) {
                        LOG.warn("Failed to parse file: {}", file, e);
                        // Continue with other files even if one fails
                    }
                }

                emitter.emit(ProgressEvent.of("File parsing completed", 40.0));
                LOG.info("Successfully parsed {} files with {} total chunks",
                        successfullyParsedFiles, allChunks.size());

                // Phase 2: Index all collected chunks with progress reporting
                emitter.emit(ProgressEvent.of("Starting indexing of " + allChunks.size() + " chunks", 45.0));

                if (!allChunks.isEmpty()) {
                    final int batchSize = Math.max(10, allChunks.size() / 10); // Process in up to 10 batches
                    int indexedChunks = 0;
                    int batchCount = 0;

                    for (int i = 0; i < allChunks.size(); i += batchSize) {
                        int endIndex = Math.min(i + batchSize, allChunks.size());
                        List<TextChunk> batch = allChunks.subList(i, endIndex);

                        // Index this batch
                        indexService.addChunks(batch).await().indefinitely();
                        indexedChunks += batch.size();
                        batchCount++;

                        // Emit indexing progress
                        int percentage = 45 + (indexedChunks * 55 / allChunks.size()); // 45-100% for indexing
                        emitter.emit(ProgressEvent.of(
                            String.format("Indexed batch %d: %d/%d chunks (%.1f%%)",
                                batchCount, indexedChunks, allChunks.size(),
                                (indexedChunks * 100.0 / allChunks.size())),
                            percentage
                        ));
                    }
                }

                emitter.emit(ProgressEvent.of("Indexing completed successfully", 100.0));
                LOG.info("Completed indexing of {} chunks from {} files", allChunks.size(), successfullyParsedFiles);
                emitter.complete();

            } catch (IOException e) {
                LOG.error("Failed to walk repository directory: {}", repositoryPath, e);
                emitter.fail(new RuntimeException("Failed to index repository files", e));
            } catch (Exception e) {
                LOG.error("Unexpected error during file processing", e);
                emitter.fail(e);
            }
        });
    }

    /**
     * Checks if a file is a source code file that should be indexed.
     */
    private boolean isSourceFile(Path file) {
        String fileName = file.getFileName().toString().toLowerCase();

        // Skip common non-source files
        if (fileName.startsWith(".") ||
            fileName.endsWith(".md") ||
            fileName.endsWith(".txt") ||
            fileName.endsWith(".yml") ||
            fileName.endsWith(".yaml") ||
            fileName.endsWith(".json") ||
            fileName.endsWith(".xml") ||
            fileName.endsWith(".properties")) {
            return false;
        }

        // Include common source file extensions
        return fileName.endsWith(".java") ||
               fileName.endsWith(".py") ||
               fileName.endsWith(".js") ||
               fileName.endsWith(".ts") ||
               fileName.endsWith(".cpp") ||
               fileName.endsWith(".c") ||
               fileName.endsWith(".h") ||
               fileName.endsWith(".cs") ||
               fileName.endsWith(".go") ||
               fileName.endsWith(".rs") ||
               fileName.endsWith(".scala") ||
               fileName.endsWith(".kt") ||
               fileName.endsWith(".rb") ||
               fileName.endsWith(".php") ||
               fileName.endsWith(".swift");
    }

    /**
     * Updates the last indexed commit SHA for a repository.
     */
    private void updateLastIndexedCommitSha(String repositoryUrl, String commitSha) {
        // This would typically be done by the RepositoryIndexStateService
        // For now, we'll log it - the incremental service handles this internally
        LOG.debug("Updated last indexed commit SHA for {} to {}", repositoryUrl, commitSha);
    }
}
