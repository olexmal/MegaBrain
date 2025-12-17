/*
 * Copyright (c) 2025 MegaBrain Contributors
 * Licensed under the MIT License - see LICENSE file for details.
 */

package io.megabrain.ingestion.github;

import io.megabrain.ingestion.IngestionException;
import io.megabrain.ingestion.ProgressEvent;
import io.megabrain.ingestion.RepositoryMetadata;
import io.megabrain.ingestion.SourceControlClient;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.ProgressMonitor;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

/**
 * GitHub source control client implementation.
 * Handles repository cloning, metadata fetching, and file extraction for GitHub repositories.
 */
@ApplicationScoped
public class GitHubSourceControlClient implements SourceControlClient {

    private static final Logger LOG = Logger.getLogger(GitHubSourceControlClient.class);
    private static final Pattern GITHUB_URL_PATTERN = Pattern.compile(
            "^(https?://)?(www\\.)?github\\.com/([^/]+)/([^/]+)(\\.git)?(/.*)?$",
            Pattern.CASE_INSENSITIVE
    );
    private static final int CLONE_TIMEOUT_SECONDS = 300; // 5 minutes

    private final AtomicReference<Path> clonedRepositoryPath = new AtomicReference<>();

    private final GitHubApiClient githubApiClient;
    private final GitHubTokenProvider tokenProvider;

    @Inject
    public GitHubSourceControlClient(@RestClient GitHubApiClient githubApiClient,
                                     GitHubTokenProvider tokenProvider) {
        this.githubApiClient = githubApiClient;
        this.tokenProvider = tokenProvider;
    }

    @Override
    public boolean canHandle(String repositoryUrl) {
        if (repositoryUrl == null || repositoryUrl.isBlank()) {
            return false;
        }
        return GITHUB_URL_PATTERN.matcher(repositoryUrl).matches();
    }

    @Override
    public Uni<RepositoryMetadata> fetchMetadata(String repositoryUrl) {
        return Uni.createFrom().item(() -> {
            try {
                RepositoryUrlParts urlParts = parseRepositoryUrl(repositoryUrl);
                
                // Fetch repository info
                GitHubRepositoryInfo repository = githubApiClient.getRepository(
                        urlParts.owner(),
                        urlParts.repo()
                );

                // Fetch latest commit SHA
                String branch = repository.defaultBranch();
                GitHubCommitInfo commitInfo = githubApiClient.getCommit(
                        repository.owner().login(),
                        repository.name(),
                        branch
                );

                return new RepositoryMetadata(
                        repository.name(),
                        repository.owner().login(),
                        branch,
                        commitInfo.sha(),
                        repository.cloneUrl()
                );
            } catch (Exception e) {
                LOG.errorf(e, "Failed to fetch metadata for repository: %s", repositoryUrl);
                throw new IngestionException("Failed to fetch repository metadata: " + e.getMessage(), e);
            }
        })
        .onFailure().retry().withBackOff(Duration.ofSeconds(1), Duration.ofSeconds(5))
        .atMost(3);
    }

    @Override
    public Multi<ProgressEvent> cloneRepository(String repositoryUrl, String branch) {
        return Multi.createFrom().emitter(emitter -> {
            try {
                emitter.emit(ProgressEvent.of("Starting repository clone", 0.0));

                RepositoryUrlParts urlParts = parseRepositoryUrl(repositoryUrl);
                String cloneUrl = buildCloneUrl(urlParts);

                Path tempDir = Files.createTempDirectory("megabrain-git-");
                Path clonePath = tempDir.resolve(urlParts.repo());

                emitter.emit(ProgressEvent.of("Preparing clone destination", 10.0));

                CloneCommand cloneCommand = configureCloneCommand(cloneUrl, clonePath, branch, emitter::emit);
                emitter.emit(ProgressEvent.of("Cloning repository", 40.0));

                try (Git ignored = cloneCommand.call()) {
                    clonedRepositoryPath.set(clonePath);
                    emitter.emit(ProgressEvent.of("Repository cloned successfully", 100.0));
                    emitter.complete();
                }

            } catch (GitAPIException e) {
                LOG.errorf(e, "Failed to clone repository: %s", repositoryUrl);
                emitter.fail(new IngestionException("Failed to clone repository: " + e.getMessage(), e));
            } catch (IOException e) {
                LOG.errorf(e, "Failed to create temporary directory for clone");
                emitter.fail(new IngestionException("Failed to create temporary directory: " + e.getMessage(), e));
            } catch (Exception e) {
                LOG.errorf(e, "Unexpected error during clone: %s", repositoryUrl);
                emitter.fail(new IngestionException("Unexpected error during clone: " + e.getMessage(), e));
            }
        });
    }

    private CloneCommand configureCloneCommand(String cloneUrl, Path clonePath, String branch,
                                               java.util.function.Consumer<ProgressEvent> progressEmitter) {
        CloneCommand cloneCommand = Git.cloneRepository()
                .setURI(cloneUrl)
                .setDirectory(clonePath.toFile())
                .setProgressMonitor(new CloneProgressMonitor(progressEmitter))
                .setTimeout(CLONE_TIMEOUT_SECONDS)
                .setCloneSubmodules(false);

        if (branch != null && !branch.isBlank()) {
            cloneCommand.setBranch(branch);
        }

        String token = tokenProvider.getToken();
        if (token != null && !token.isBlank()) {
            String cleanToken = token.replaceFirst("^(token|Bearer)\\s+", "");
            cloneCommand.setCredentialsProvider(new UsernamePasswordCredentialsProvider(cleanToken, ""));
        }

        return cloneCommand;
    }

    /**
     * Progress monitor for git clone operations.
     */
    private static class CloneProgressMonitor implements ProgressMonitor {
        private final java.util.function.Consumer<ProgressEvent> emitter;
        private int currentTask = 0;
        private int totalTasks = 0;
        private int lastReportedProgress = 20;

        CloneProgressMonitor(java.util.function.Consumer<ProgressEvent> emitter) {
            this.emitter = emitter;
        }

        @Override
        public void start(int totalTasks) {
            this.totalTasks = totalTasks;
            this.currentTask = 0;
            emitter.accept(ProgressEvent.of("Clone started", 20.0));
        }

        @Override
        public void beginTask(String title, int totalWork) {
            currentTask++;
            int baseProgress = 20 + (currentTask * 50 / Math.max(totalTasks, 1));
            emitter.accept(ProgressEvent.of("Cloning: " + title, Math.min(baseProgress, 70.0)));
        }

        @Override
        public void update(int completed) {
            int currentProgress = 20 + (currentTask * 50 / Math.max(totalTasks, 1));
            if (currentProgress > lastReportedProgress + 5) {
                emitter.accept(ProgressEvent.of("Cloning repository", Math.min(currentProgress, 70.0)));
                lastReportedProgress = currentProgress;
            }
        }

        @Override
        public void endTask() {
            int progress = 20 + (currentTask * 50 / Math.max(totalTasks, 1));
            emitter.accept(ProgressEvent.of("Task completed", Math.min(progress, 70.0)));
        }

        @Override
        public boolean isCancelled() {
            return false;
        }

        @Override
        public void showDuration(boolean enabled) {
            // Duration display control - not needed for our use case
        }
    }

    @Override
    public Multi<ProgressEvent> extractFiles(Path repositoryPath) {
        return Multi.createFrom().emitter(emitter -> {
            try {
                emitter.emit(ProgressEvent.of("Starting file extraction", 0.0));

                if (!Files.exists(repositoryPath) || !Files.isDirectory(repositoryPath)) {
                    emitter.fail(new IllegalArgumentException("Repository path does not exist or is not a directory: " + repositoryPath));
                    return;
                }

                // Walk through repository and extract files
                final int[] totalFiles = {0};
                final AtomicInteger processedFiles = new AtomicInteger(0);

                // First pass: count files
                try (var paths = Files.walk(repositoryPath)) {
                    totalFiles[0] = (int) paths
                            .filter(Files::isRegularFile)
                            .filter(this::shouldIncludeFile)
                            .count();
                }

                emitter.emit(ProgressEvent.of("Found " + totalFiles[0] + " files to extract", 10.0));

                // Second pass: process files
                try (var paths = Files.walk(repositoryPath)) {
                    paths.filter(Files::isRegularFile)
                            .filter(this::shouldIncludeFile)
                            .forEach(file -> {
                                int currentProcessed = processedFiles.incrementAndGet();
                                int percentage = totalFiles[0] > 0 ? (currentProcessed * 90 / totalFiles[0]) + 10 : 50;
                                emitter.emit(ProgressEvent.of(
                                        "Extracted: " + file.getFileName(),
                                        percentage
                                ));
                            });
                }

                emitter.emit(ProgressEvent.of("File extraction completed", 100.0));
                emitter.complete();

            } catch (IOException e) {
                LOG.errorf(e, "Failed to extract files from repository: %s", repositoryPath);
                emitter.fail(new IngestionException("Failed to extract files: " + e.getMessage(), e));
            } catch (Exception e) {
                LOG.errorf(e, "Unexpected error during file extraction: %s", repositoryPath);
                emitter.fail(new IngestionException("Unexpected error during file extraction: " + e.getMessage(), e));
            }
        });
    }

    @Override
    public Path getClonedRepositoryPath() {
        return clonedRepositoryPath.get();
    }

    /**
     * Record for repository URL parts.
     */
    private record RepositoryUrlParts(String owner, String repo) {
    }

    /**
     * Parses a GitHub repository URL and extracts owner and repository name.
     */
    private RepositoryUrlParts parseRepositoryUrl(String repositoryUrl) {
        var matcher = GITHUB_URL_PATTERN.matcher(repositoryUrl);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Invalid GitHub repository URL: " + repositoryUrl);
        }

        String owner = matcher.group(3);
        String repo = matcher.group(4);
        
        // Remove .git suffix if present
        if (repo.endsWith(".git")) {
            repo = repo.substring(0, repo.length() - 4);
        }

        return new RepositoryUrlParts(owner, repo);
    }

    /**
     * Builds the clone URL for the repository.
     */
    private String buildCloneUrl(RepositoryUrlParts urlParts) {
        // Always use HTTPS for cloning
        return "https://github.com/" + urlParts.owner() + "/" + urlParts.repo() + ".git";
    }

    /**
     * Checks if a file should be included in extraction.
     * Filters out binary files and ignored patterns.
     */
    private boolean shouldIncludeFile(Path file) {
        String fileName = file.getFileName().toString();
        
        // Skip hidden files and directories
        if (fileName.startsWith(".")) {
            return false;
        }

        // Skip common non-source files
        String lowerName = fileName.toLowerCase();
        if (lowerName.endsWith(".class") ||
            lowerName.endsWith(".jar") ||
            lowerName.endsWith(".war") ||
            lowerName.endsWith(".ear") ||
            lowerName.endsWith(".zip") ||
            lowerName.endsWith(".tar") ||
            lowerName.endsWith(".gz") ||
            lowerName.endsWith(".png") ||
            lowerName.endsWith(".jpg") ||
            lowerName.endsWith(".jpeg") ||
            lowerName.endsWith(".gif") ||
            lowerName.endsWith(".ico") ||
            lowerName.endsWith(".pdf")) {
            return false;
        }

        // TODO: Implement .gitignore pattern support (EPIC-01, US-01-02)
        // Currently only filters by file extension. Future enhancement should:
        // - Parse .gitignore files from repository root
        // - Implement glob pattern matching for ignored files
        // - Support nested .gitignore files
        // - Handle negation patterns (!)
        // For now, include all non-binary text files

        return true;
    }

    /**
     * Cleans up the cloned repository directory.
     */
    public void cleanup() {
        Path path = clonedRepositoryPath.getAndSet(null);
        if (path != null && Files.exists(path)) {
            try {
                deleteRecursively(path);
                LOG.infof("Cleaned up cloned repository: %s", path);
            } catch (IOException e) {
                LOG.warnf(e, "Failed to clean up cloned repository: %s", path);
            }
        }
    }

    /**
     * Recursively deletes a directory.
     */
    private void deleteRecursively(Path path) throws IOException {
        if (Files.isDirectory(path)) {
            try (var stream = Files.list(path)) {
                stream.forEach(child -> {
                    try {
                        deleteRecursively(child);
                    } catch (IOException e) {
                        throw new IngestionException("Failed to delete file during cleanup: " + child, e);
                    }
                });
            }
        }
        Files.delete(path);
    }
}

