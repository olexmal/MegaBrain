/*
 * Copyright (c) 2025 MegaBrain Contributors
 * Licensed under the MIT License - see LICENSE file for details.
 */

package io.megabrain.ingestion.bitbucket;

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
import org.eclipse.jgit.ignore.IgnoreNode;
import org.eclipse.jgit.lib.ProgressMonitor;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.jboss.logging.Logger;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import jakarta.ws.rs.WebApplicationException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

/**
 * Bitbucket source control client implementation.
 * Handles repository cloning, metadata fetching, and file extraction for Bitbucket repositories
 * (both Cloud and Server/Data Center editions).
 */
@ApplicationScoped
public class BitbucketSourceControlClient implements SourceControlClient {

    private static final Logger LOG = Logger.getLogger(BitbucketSourceControlClient.class);

    // Bitbucket Cloud: https://bitbucket.org/workspace/repo
    private static final Pattern BITBUCKET_CLOUD_URL_PATTERN = Pattern.compile(
            "^(https?://)?(www\\.)?bitbucket\\.org/([^/]+)/([^/]+)(\\.git)?(/.*)?$",
            Pattern.CASE_INSENSITIVE
    );

    // Bitbucket Server: https://custom.domain.com/projects/PROJ/repos/repo
    private static final Pattern BITBUCKET_SERVER_URL_PATTERN = Pattern.compile(
            "^(https?://)?([^/]+)/(projects|rest/api(?:/.*)?)/([^/]+)/repos/([^/]+)",
            Pattern.CASE_INSENSITIVE
    );

    // Bitbucket Server SCM clone URLs: https://host/scm/PROJ/repo.git
    private static final Pattern BITBUCKET_SERVER_SCM_URL_PATTERN = Pattern.compile(
            "^(https?://)?([^/]+)/scm/([^/]+)/([^/]+)(\\.git)?$",
            Pattern.CASE_INSENSITIVE
    );

    // Bitbucket Server SSH URLs: git@host:PROJ/repo.git
    private static final Pattern BITBUCKET_SERVER_SSH_URL_PATTERN = Pattern.compile(
            "^git@([^:]+):([^/]+)/([^/]+)(\\.git)?$",
            Pattern.CASE_INSENSITIVE
    );

    private static final int CLONE_TIMEOUT_SECONDS = 300; // 5 minutes

    private final AtomicReference<Path> clonedRepositoryPath = new AtomicReference<>();

    private final BitbucketCloudApiClient bitbucketCloudApiClient;
    private final BitbucketServerApiClient bitbucketServerApiClient;
    private final BitbucketTokenProvider tokenProvider;
    private final Optional<String> serverBaseUrl;

    @Inject
    public BitbucketSourceControlClient(@RestClient BitbucketCloudApiClient bitbucketCloudApiClient,
                                        @RestClient BitbucketServerApiClient bitbucketServerApiClient,
                                        BitbucketTokenProvider tokenProvider,
                                        @ConfigProperty(name = "bitbucket-server-api/mp-rest/url") Optional<String> serverBaseUrl) {
        this.bitbucketCloudApiClient = bitbucketCloudApiClient;
        this.bitbucketServerApiClient = bitbucketServerApiClient;
        this.tokenProvider = tokenProvider;
        this.serverBaseUrl = serverBaseUrl;
    }

    @Override
    public boolean canHandle(String repositoryUrl) {
        if (repositoryUrl == null || repositoryUrl.isBlank()) {
            return false;
        }
        return BITBUCKET_CLOUD_URL_PATTERN.matcher(repositoryUrl).matches() ||
               BITBUCKET_SERVER_URL_PATTERN.matcher(repositoryUrl).matches() ||
               BITBUCKET_SERVER_SCM_URL_PATTERN.matcher(repositoryUrl).matches() ||
               BITBUCKET_SERVER_SSH_URL_PATTERN.matcher(repositoryUrl).matches();
    }

    @Override
    public Uni<RepositoryMetadata> fetchMetadata(String repositoryUrl) {
        return Uni.createFrom().item(() -> {
            try {
                RepositoryUrlParts urlParts = parseRepositoryUrl(repositoryUrl);

                if (urlParts.isCloud()) {
                    // Handle Bitbucket Cloud
                    BitbucketCloudRepositoryInfo repository = bitbucketCloudApiClient.getRepository(
                            urlParts.workspace(),
                            urlParts.repo()
                    );

                    // Fetch latest commit SHA for default branch
                    String branch = repository.mainbranch().name();
                    BitbucketCloudCommitInfo commitInfo = bitbucketCloudApiClient.getCommit(
                            urlParts.workspace(),
                            urlParts.repo(),
                            branch
                    );

                    return new RepositoryMetadata(
                            repository.name(),
                            urlParts.workspace(),
                            branch,
                            commitInfo.hash(),
                            repository.links().cloneLinks().stream()
                                    .filter(link -> "https".equals(link.name()))
                                    .findFirst()
                                    .map(BitbucketCloudRepositoryInfo.Links.CloneLink::href)
                                    .orElse(repositoryUrl)
                    );
                } else {
                    // Handle Bitbucket Server
                    BitbucketServerRepositoryInfo repository = bitbucketServerApiClient.getRepository(
                            urlParts.project(),
                            urlParts.repo()
                    );

                    // Fetch latest commit SHA for default branch
                    String branch = repository.defaultBranch();
                    BitbucketServerCommitInfo commitInfo = bitbucketServerApiClient.getCommit(
                            urlParts.project(),
                            urlParts.repo(),
                            branch
                    );

                    return new RepositoryMetadata(
                            repository.name(),
                            urlParts.project(),
                            branch,
                            commitInfo.getLatestCommit() != null ? commitInfo.getLatestCommit().id() : "unknown",
                            repository.links().cloneLinks().stream()
                                    .filter(link -> "http".equals(link.name()) || "https".equals(link.name()))
                                    .findFirst()
                                    .map(BitbucketServerRepositoryInfo.Links.CloneLink::href)
                                    .orElse(repositoryUrl)
                    );
                }

            } catch (Exception e) {
                LOG.errorf(e, "Failed to fetch metadata for repository: %s", repositoryUrl);
                throw mapBitbucketException(e);
            }
        })
        .onFailure(this::isRateLimit)
            .retry().withBackOff(Duration.ofSeconds(1), Duration.ofSeconds(5)).atMost(3)
        .onFailure(WebApplicationException.class)
            .transform(this::mapBitbucketException);
    }

    @Override
    public Multi<ProgressEvent> cloneRepository(String repositoryUrl, String branch) {
        return Multi.createFrom().emitter(emitter -> {
            try {
                emitter.emit(ProgressEvent.of("Starting repository clone", 0.0));

                RepositoryUrlParts urlParts = parseRepositoryUrl(repositoryUrl);
                String cloneUrl = buildCloneUrl(urlParts);

                Path tempDir = Files.createTempDirectory("megabrain-bitbucket-git-");
                Path clonePath = tempDir.resolve(urlParts.repo());

                emitter.emit(ProgressEvent.of("Preparing clone destination", 10.0));

                CloneCommand cloneCommand = configureCloneCommand(cloneUrl, clonePath, branch, urlParts.isCloud(), emitter::emit);
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
                                               boolean isCloud, java.util.function.Consumer<ProgressEvent> progressEmitter) {
        CloneCommand cloneCommand = Git.cloneRepository()
                .setURI(cloneUrl)
                .setDirectory(clonePath.toFile())
                .setProgressMonitor(new CloneProgressMonitor(progressEmitter))
                .setTimeout(CLONE_TIMEOUT_SECONDS)
                .setCloneSubmodules(false);

        if (branch != null && !branch.isBlank()) {
            cloneCommand.setBranch(branch);
        }

        BitbucketTokenProvider.BitbucketCredentials credentials = tokenProvider.getCredentials(isCloud);
        if (credentials != null) {
            cloneCommand.setCredentialsProvider(
                    new UsernamePasswordCredentialsProvider(credentials.username(), credentials.password())
            );
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

                // Load .gitignore rules (root only for now)
                IgnoreNode ignoreNode = loadIgnoreNode(repositoryPath);

                // First pass: count files
                try (var paths = Files.walk(repositoryPath)) {
                    totalFiles[0] = (int) paths
                            .filter(Files::isRegularFile)
                            .filter(path -> shouldIncludeFile(path, repositoryPath, ignoreNode))
                            .count();
                }

                emitter.emit(ProgressEvent.of("Found " + totalFiles[0] + " files to extract", 10.0));

                // Second pass: process files
                try (var paths = Files.walk(repositoryPath)) {
                    paths.filter(Files::isRegularFile)
                            .filter(path -> shouldIncludeFile(path, repositoryPath, ignoreNode))
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
     * Handles both Bitbucket Cloud and Server URL formats.
     */
    private record RepositoryUrlParts(String workspace, String project, String repo, boolean isCloud) {
    }

    /**
     * Parses a Bitbucket repository URL and extracts workspace/project and repository name.
     */
    private RepositoryUrlParts parseRepositoryUrl(String repositoryUrl) {
        // Try Bitbucket Cloud pattern first
        var cloudMatcher = BITBUCKET_CLOUD_URL_PATTERN.matcher(repositoryUrl);
        if (cloudMatcher.matches()) {
            String workspace = cloudMatcher.group(3);
            String repo = cloudMatcher.group(4);

            // Remove .git suffix if present
            if (repo.endsWith(".git")) {
                repo = repo.substring(0, repo.length() - 4);
            }

            return new RepositoryUrlParts(workspace, null, repo, true);
        }

        // Try Bitbucket Server pattern
        var serverMatcher = BITBUCKET_SERVER_URL_PATTERN.matcher(repositoryUrl);
        if (serverMatcher.matches()) {
            String project = serverMatcher.group(4);
            String repo = serverMatcher.group(5);

            return new RepositoryUrlParts(null, project, repo, false);
        }

        // Try Bitbucket Server SCM clone pattern
        var scmMatcher = BITBUCKET_SERVER_SCM_URL_PATTERN.matcher(repositoryUrl);
        if (scmMatcher.matches()) {
            String project = scmMatcher.group(3);
            String repo = scmMatcher.group(4);
            if (repo.endsWith(".git")) {
                repo = repo.substring(0, repo.length() - 4);
            }
            return new RepositoryUrlParts(null, project, repo, false);
        }

        // Try Bitbucket Server SSH pattern
        var sshMatcher = BITBUCKET_SERVER_SSH_URL_PATTERN.matcher(repositoryUrl);
        if (sshMatcher.matches()) {
            String project = sshMatcher.group(2);
            String repo = sshMatcher.group(3);
            if (repo.endsWith(".git")) {
                repo = repo.substring(0, repo.length() - 4);
            }
            return new RepositoryUrlParts(null, project, repo, false);
        }

        throw new IllegalArgumentException("Invalid Bitbucket repository URL: " + repositoryUrl);
    }

    private boolean isRateLimit(Throwable throwable) {
        if (throwable instanceof WebApplicationException webEx && webEx.getResponse() != null) {
            return webEx.getResponse().getStatus() == 429;
        }
        return false;
    }

    private IngestionException mapBitbucketException(Throwable throwable) {
        if (throwable instanceof IngestionException ie) {
            return ie;
        }
        if (throwable instanceof WebApplicationException webEx && webEx.getResponse() != null) {
            int status = webEx.getResponse().getStatus();
            if (status == 401 || status == 403) {
                return new IngestionException("Bitbucket authentication failed: check app password or PAT", webEx);
            }
            if (status == 429) {
                return new IngestionException("Bitbucket API rate limited: retry later", webEx);
            }
        }
        return new IngestionException("Failed to fetch repository metadata: " + throwable.getMessage(), throwable);
    }

    /**
     * Builds the clone URL for the repository.
     */
    private String buildCloneUrl(RepositoryUrlParts urlParts) {
        if (urlParts.isCloud()) {
            // Bitbucket Cloud: always use HTTPS
            return "https://bitbucket.org/" + urlParts.workspace() + "/" + urlParts.repo() + ".git";
        } else {
            String base = serverBaseUrl.orElseThrow(() ->
                    new IllegalStateException("bitbucket-server-api/mp-rest/url must be configured for Bitbucket Server cloning"));

            // Remove any trailing path starting at /rest to derive host base
            int restIndex = base.toLowerCase().indexOf("/rest");
            if (restIndex >= 0) {
                base = base.substring(0, restIndex);
            }

            // Normalize trailing slash
            if (base.endsWith("/")) {
                base = base.substring(0, base.length() - 1);
            }

            return base + "/scm/" + urlParts.project() + "/" + urlParts.repo() + ".git";
        }
    }

    /**
     * Checks if a file should be included in extraction.
     * Filters out binary files and ignored patterns.
     */
    private boolean shouldIncludeFile(Path file, Path repositoryRoot, IgnoreNode ignoreNode) {
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

        // Apply root .gitignore rules if present
        if (ignoreNode != null) {
            String relative = repositoryRoot.relativize(file).toString().replace("\\", "/");
            var result = ignoreNode.isIgnored(relative, false);
            return result != IgnoreNode.MatchResult.IGNORED;
        }

        return true;
    }

    private IgnoreNode loadIgnoreNode(Path repositoryRoot) {
        Path ignoreFile = repositoryRoot.resolve(".gitignore");
        if (Files.exists(ignoreFile)) {
            try {
                IgnoreNode node = new IgnoreNode();
                try (var is = Files.newInputStream(ignoreFile)) {
                    node.parse(is);
                }
                return node;
            } catch (IOException e) {
                LOG.warnf(e, "Failed to read .gitignore at %s, continuing without it", ignoreFile);
            }
        }
        return null;
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
