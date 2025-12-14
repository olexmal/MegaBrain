/*
 * Copyright (c) 2025 MegaBrain Contributors
 * Licensed under the MIT License - see LICENSE file for details.
 */

package io.megabrain.ingestion.gitlab;

import io.megabrain.ingestion.IngestionException;
import io.megabrain.ingestion.ProgressEvent;
import io.megabrain.ingestion.RepositoryMetadata;
import io.megabrain.ingestion.SourceControlClient;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.ProgressMonitor;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

import java.io.IOException;
import java.net.ConnectException;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import java.util.regex.Pattern;

import javax.net.ssl.SSLHandshakeException;

/**
 * GitLab source control client implementation.
 * Handles repository cloning, metadata fetching, and file extraction for GitLab repositories.
 */
@ApplicationScoped
public class GitLabSourceControlClient implements SourceControlClient {

    private static final Logger LOG = Logger.getLogger(GitLabSourceControlClient.class);
    private static final Pattern GITLAB_URL_PATTERN = Pattern.compile(
            "^(https?://)?(www\\.)?gitlab\\.com/([^/]+)/([^/]+)(\\.git)?(/.*)?$",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern GITLAB_SELF_HOSTED_URL_PATTERN = Pattern.compile(
            "^(https?://)?([^/]+)/([^/]+)/([^/]+)(\\.git)?(/.*)?$",
            Pattern.CASE_INSENSITIVE
    );
    private static final int CLONE_TIMEOUT_SECONDS = 300; // 5 minutes

    private final AtomicReference<Path> clonedRepositoryPath = new AtomicReference<>();

    @Inject
    @RestClient
    GitLabApiClient gitlabApiClient;

    @Inject
    GitLabTokenProvider tokenProvider;

    @Inject
    GitLabConfiguration config;

    @Override
    public boolean canHandle(String repositoryUrl) {
        if (repositoryUrl == null || repositoryUrl.isBlank()) {
            return false;
        }
        // Check both gitlab.com and self-hosted GitLab patterns
        return GITLAB_URL_PATTERN.matcher(repositoryUrl).matches() ||
               isSelfHostedGitLabUrl(repositoryUrl);
    }

    @Override
    public Uni<RepositoryMetadata> fetchMetadata(String repositoryUrl) {
        return Uni.createFrom().item(() -> {
            try {
                RepositoryUrlParts urlParts = parseRepositoryUrl(repositoryUrl);

                // URL encode the project path for GitLab API
                String encodedProjectPath = URLEncoder.encode(urlParts.namespace() + "/" + urlParts.project(), StandardCharsets.UTF_8);

                // Fetch project info with rate limiting handling
                GitLabRepositoryInfo project = fetchWithRateLimitHandling(() ->
                    gitlabApiClient.getProject(encodedProjectPath)
                );

                // Fetch latest commit for default branch
                String defaultBranch = project.defaultBranch();
                GitLabCommitInfo[] commits = fetchWithRateLimitHandling(() ->
                    gitlabApiClient.getCommits(encodedProjectPath, defaultBranch, 1)
                );

                if (commits.length == 0) {
                    throw new IngestionException("No commits found for project: " + urlParts.namespace() + "/" + urlParts.project());
                }

                GitLabCommitInfo latestCommit = commits[0];

                return new RepositoryMetadata(
                        project.name(),
                        project.namespace().fullPath(),
                        defaultBranch,
                        latestCommit.id(),
                        project.httpUrlToRepo()
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

                // Create temporary directory for clone
                Path tempDir = Files.createTempDirectory("megabrain-gitlab-");
                Path clonePath = tempDir.resolve(urlParts.project());
                clonedRepositoryPath.set(clonePath);

                emitter.emit(ProgressEvent.of("Preparing clone destination", 10.0));

                // Configure clone command
                CloneCommand cloneCommand = Git.cloneRepository()
                        .setURI(cloneUrl)
                        .setDirectory(clonePath.toFile())
                        .setProgressMonitor(new ProgressMonitor() {
                            private int currentTask = 0;
                            private int totalTasks = 0;
                            private int lastReportedProgress = 20;

                            @Override
                            public void start(int totalTasks) {
                                this.totalTasks = totalTasks;
                                this.currentTask = 0;
                                emitter.emit(ProgressEvent.of("Clone started", 20.0));
                            }

                            @Override
                            public void beginTask(String title, int totalWork) {
                                currentTask++;
                                int baseProgress = 20 + (currentTask * 50 / (totalTasks > 0 ? totalTasks : 1));
                                emitter.emit(ProgressEvent.of("Cloning: " + title, Math.min(baseProgress, 70.0)));
                            }

                            @Override
                            public void update(int completed) {
                                // Calculate progress within current task
                                int currentProgress = 20 + (currentTask * 50 / (totalTasks > 0 ? totalTasks : 1));
                                if (currentProgress > lastReportedProgress + 5) { // Report every 5% increase
                                    emitter.emit(ProgressEvent.of("Cloning repository", Math.min(currentProgress, 70.0)));
                                    lastReportedProgress = currentProgress;
                                }
                            }

                            @Override
                            public void endTask() {
                                int progress = 20 + (currentTask * 50 / (totalTasks > 0 ? totalTasks : 1));
                                emitter.emit(ProgressEvent.of("Task completed", Math.min(progress, 70.0)));
                            }

                            @Override
                            public boolean isCancelled() {
                                return false;
                            }

                            @Override
                            public void showDuration(boolean enabled) {
                                // Duration display control - not needed for our use case
                            }
                        })
                        .setTimeout(CLONE_TIMEOUT_SECONDS)
                        .setCloneSubmodules(false);

                // Set branch if specified
                if (branch != null && !branch.isBlank()) {
                    cloneCommand.setBranch(branch);
                }

                // Add authentication if token is available
                String token = tokenProvider.getToken();
                if (token != null && !token.isBlank()) {
                    // GitLab uses OAuth token for Git operations (same as GitHub)
                    cloneCommand.setCredentialsProvider(
                            new UsernamePasswordCredentialsProvider("oauth2", token)
                    );
                }

                emitter.emit(ProgressEvent.of("Cloning repository", 40.0));

                // Perform clone
                try (Git git = cloneCommand.call()) {
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
    private record RepositoryUrlParts(String host, String namespace, String project) {
    }

    /**
     * Parses a GitLab repository URL and extracts host, namespace, and project name.
     */
    private RepositoryUrlParts parseRepositoryUrl(String repositoryUrl) {
        // Try gitlab.com pattern first
        var matcher = GITLAB_URL_PATTERN.matcher(repositoryUrl);
        if (matcher.matches()) {
            String namespace = matcher.group(3);
            String project = matcher.group(4);
            // Remove .git suffix if present
            if (project.endsWith(".git")) {
                project = project.substring(0, project.length() - 4);
            }
            return new RepositoryUrlParts("gitlab.com", namespace, project);
        }

        // Try self-hosted pattern
        matcher = GITLAB_SELF_HOSTED_URL_PATTERN.matcher(repositoryUrl);
        if (matcher.matches()) {
            String host = matcher.group(2);
            String namespace = matcher.group(3);
            String project = matcher.group(4);
            // Remove .git suffix if present
            if (project.endsWith(".git")) {
                project = project.substring(0, project.length() - 4);
            }
            return new RepositoryUrlParts(host, namespace, project);
        }

        throw new IllegalArgumentException("Invalid GitLab repository URL: " + repositoryUrl);
    }

    /**
     * Builds the clone URL for the repository.
     */
    private String buildCloneUrl(RepositoryUrlParts urlParts) {
        // Always use HTTPS for cloning
        return "https://" + urlParts.host() + "/" + urlParts.namespace() + "/" + urlParts.project() + ".git";
    }

    /**
     * Checks if a URL appears to be a self-hosted GitLab instance.
     */
    private boolean isSelfHostedGitLabUrl(String repositoryUrl) {
        // Only consider URLs that contain "gitlab" in the hostname as self-hosted instances
        // This avoids false positives for generic company domains
        return repositoryUrl.contains("gitlab") &&
               !repositoryUrl.contains("gitlab.com") &&
               repositoryUrl.startsWith("https://") &&
               GITLAB_SELF_HOSTED_URL_PATTERN.matcher(repositoryUrl).matches();
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
                        throw new RuntimeException(e);
                    }
                });
            }
        }
        Files.delete(path);
    }

    /**
     * Validates GitLab instance connectivity and configuration.
     * Provides clear error messages for common connection issues.
     */
    public void validateGitLabConnection() {
        try {
            LOG.infof("Validating GitLab connection to: %s", config.apiUrl());

            // Try to access a simple endpoint to test connectivity
            // We'll use a simple API call that doesn't require authentication
            fetchWithRateLimitHandling(() -> {
                try {
                    // Try to get version info or a simple endpoint
                    // For now, we'll just validate that we can make any API call
                    // This is a placeholder - in a real implementation, we might call
                    // a version endpoint if available
                    gitlabApiClient.getProject("gitlab-org%2Fgitlab"); // Test with a known public project
                    return true;
                } catch (Exception e) {
                    if (e.getMessage().contains("404") || e.getMessage().contains("403")) {
                        // This is expected for some endpoints, connection is working
                        return true;
                    }
                    throw e;
                }
            });

            LOG.infof("GitLab connection validated successfully for: %s", config.apiUrl());

        } catch (WebApplicationException e) {
            Response response = e.getResponse();
            int statusCode = response.getStatus();

            switch (statusCode) {
                case 401:
                    throw new IngestionException(
                        "GitLab authentication failed. Please check your GITLAB_TOKEN configuration.", e);
                case 403:
                    throw new IngestionException(
                        "GitLab access forbidden. Your token may not have the required permissions.", e);
                case 404:
                    throw new IngestionException(
                        "GitLab instance not found. Please check your GITLAB_API_URL configuration.", e);
                default:
                    throw new IngestionException(
                        String.format("GitLab connection failed with HTTP %d: %s. Please check your configuration.",
                            statusCode, response.getStatusInfo().getReasonPhrase()), e);
            }
        } catch (Exception e) {
            if (e.getCause() instanceof UnknownHostException) {
                throw new IngestionException(
                    String.format("Cannot resolve GitLab hostname: %s. Please check your GITLAB_API_URL configuration.",
                        config.apiUrl()), e);
            } else if (e.getCause() instanceof ConnectException) {
                throw new IngestionException(
                    String.format("Cannot connect to GitLab instance: %s. Please check network connectivity and URL.",
                        config.apiUrl()), e);
            } else if (e.getCause() instanceof SSLHandshakeException) {
                throw new IngestionException(
                    "SSL certificate validation failed. For self-hosted GitLab with custom certificates, " +
                    "configure megabrain.gitlab.ssl.trust-store and related properties.", e);
            } else {
                throw new IngestionException(
                    String.format("Failed to connect to GitLab instance: %s. Error: %s",
                        config.apiUrl(), e.getMessage()), e);
            }
        }
    }

    /**
     * Executes an API call with GitLab rate limiting handling.
     * Implements exponential backoff for 429 responses.
     *
     * @param apiCall the API call to execute
     * @return the API response
     * @param <T> the response type
     */
    private <T> T fetchWithRateLimitHandling(Supplier<T> apiCall) {
        int attempt = 0;
        int maxAttempts = 5;
        long backoffMs = 1000; // Start with 1 second

        while (attempt < maxAttempts) {
            try {
                return apiCall.get();
            } catch (WebApplicationException e) {
                Response response = e.getResponse();
                int statusCode = response.getStatus();

                switch (statusCode) {
                    case 429 -> { // Too Many Requests
                        attempt++;

                        if (attempt >= maxAttempts) {
                            LOG.warnf("GitLab API rate limit exceeded, giving up after %d attempts", maxAttempts);
                            throw new IngestionException("GitLab API rate limit exceeded", e);
                        }

                        // Check for Retry-After header (GitLab may provide this)
                        String retryAfter = response.getHeaderString("Retry-After");
                        long waitMs = backoffMs;

                        if (retryAfter != null) {
                            try {
                                waitMs = Long.parseLong(retryAfter) * 1000; // Convert seconds to milliseconds
                                LOG.infof("GitLab API rate limited, waiting %d seconds as requested", waitMs / 1000);
                            } catch (NumberFormatException nfe) {
                                LOG.warnf("Invalid Retry-After header: %s, using exponential backoff", retryAfter);
                            }
                        } else {
                            LOG.infof("GitLab API rate limited, attempt %d/%d, waiting %d ms", attempt, maxAttempts, waitMs);
                        }

                        try {
                            Thread.sleep(waitMs);
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            throw new IngestionException("Interrupted while waiting for rate limit", ie);
                        }

                        // Exponential backoff for next attempt
                        backoffMs = Math.min(backoffMs * 2, 30000); // Max 30 seconds

                    }
                    case 401 -> {
                        throw new IngestionException("GitLab authentication failed. Please check your token.", e);
                    }
                    case 403 -> {
                        throw new IngestionException("GitLab access forbidden. You may not have permission to access this repository.", e);
                    }
                    case 404 -> {
                        throw new IngestionException("GitLab repository not found. Please check the URL.", e);
                    }
                    default -> {
                        // For other errors, don't retry
                        throw e;
                    }
                }
            } catch (Exception e) {
                // For non-HTTP exceptions, don't retry
                throw new IngestionException("GitLab API call failed: " + e.getMessage(), e);
            }
        }

        throw new IngestionException("GitLab API call failed after maximum retry attempts");
    }
}
