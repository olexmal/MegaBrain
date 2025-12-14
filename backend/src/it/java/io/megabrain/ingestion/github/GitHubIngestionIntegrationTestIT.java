/*
 * Copyright (c) 2025 MegaBrain Contributors
 * Licensed under the MIT License - see LICENSE file for details.
 */

package io.megabrain.ingestion.github;

import io.megabrain.ingestion.IngestionException;
import io.megabrain.ingestion.ProgressEvent;
import io.megabrain.ingestion.RepositoryMetadata;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Integration tests for GitHub repository ingestion using real GitHub API.
 * Uses octocat/Hello-World as a test repository.
 */
@QuarkusTest
class GitHubIngestionIntegrationTestIT {

    private static final String TEST_REPO_URL = "https://github.com/octocat/Hello-World";
    private static final Duration TIMEOUT = Duration.ofMinutes(5);

    @Inject
    GitHubSourceControlClient client;

    private Path tempDir;

    @BeforeEach
    void setUp() throws IOException {
        tempDir = Files.createTempDirectory("megabrain-integration-test-");
    }

    @AfterEach
    void tearDown() throws IOException {
        if (tempDir != null && Files.exists(tempDir)) {
            deleteRecursively(tempDir);
        }
    }

    @Test
    void shouldHandleValidGitHubRepositoryUrl() {
        assertThat(client.canHandle(TEST_REPO_URL)).isTrue();
    }

    @Test
    void shouldFetchRepositoryMetadataSuccessfully() {
        // When
        Uni<RepositoryMetadata> metadataUni = client.fetchMetadata(TEST_REPO_URL);

        // Then
        RepositoryMetadata metadata = metadataUni.await().atMost(TIMEOUT);
        assertThat(metadata).isNotNull();
        assertThat(metadata.name()).isEqualTo("Hello-World");
        assertThat(metadata.owner()).isEqualTo("octocat");
        assertThat(metadata.branch()).isNotBlank();
        assertThat(metadata.commitSha()).isNotBlank();
        assertThat(metadata.cloneUrl()).isEqualTo(TEST_REPO_URL);
    }

    @Test
    void shouldCloneRepositorySuccessfully() {
        // When
        Multi<ProgressEvent> progress = client.cloneRepository(TEST_REPO_URL, null);

        // Then
        List<ProgressEvent> events = progress.collect().asList().await().atMost(TIMEOUT);
        assertThat(events).isNotEmpty();
        assertThat(events.get(0).stage()).isEqualTo(CLONING);
        assertThat(events.get(0).message()).isEqualTo("Starting repository clone");

        ProgressEvent lastEvent = events.get(events.size() - 1);
        assertThat(lastEvent.stage()).isEqualTo(CLONING);
        assertThat(lastEvent.message()).isEqualTo("Repository cloned successfully");
        assertThat(lastEvent.percentage()).isEqualTo(100);

        // Verify repository was cloned
        Path clonedPath = client.getClonedRepositoryPath();
        assertThat(clonedPath).isNotNull();
        assertThat(Files.exists(clonedPath)).isTrue();
        assertThat(Files.isDirectory(clonedPath)).isTrue();
    }

    @Test
    void shouldExtractFilesFromClonedRepository() {
        // Given - repository is cloned
        Multi<ProgressEvent> cloneProgress = client.cloneRepository(TEST_REPO_URL, null);
        cloneProgress.collect().asList().await().atMost(TIMEOUT);

        Path clonedPath = client.getClonedRepositoryPath();
        assertThat(clonedPath).isNotNull();

        // When
        Multi<ProgressEvent> extractProgress = client.extractFiles(clonedPath);

        // Then
        List<ProgressEvent> events = extractProgress.collect().asList().await().atMost(TIMEOUT);
        assertThat(events).isNotEmpty();
        assertThat(events.get(0).stage()).isEqualTo("EXTRACTING");
        assertThat(events.get(0).message()).isEqualTo("Starting file extraction");

        ProgressEvent lastEvent = events.get(events.size() - 1);
        assertThat(lastEvent.stage()).isEqualTo("EXTRACTING");
        assertThat(lastEvent.message()).isEqualTo("File extraction completed");
        assertThat(lastEvent.percentage()).isEqualTo(100);
    }

    @Test
    void shouldHandleInvalidRepositoryUrl() {
        String invalidUrl = "https://github.com/nonexistent/repo12345";

        // When & Then
        assertThatThrownBy(() -> client.fetchMetadata(invalidUrl).await().atMost(TIMEOUT))
            .isInstanceOf(IngestionException.class)
            .hasMessageContaining("Failed to fetch repository metadata");
    }

    @Test
    void shouldHandleNonGitHubUrls() {
        String gitlabUrl = "https://gitlab.com/owner/repo";

        assertThat(client.canHandle(gitlabUrl)).isFalse();
    }

    @Test
    void shouldCloneSpecificBranchSuccessfully() {
        // When - clone main branch explicitly
        Multi<ProgressEvent> progress = client.cloneRepository(TEST_REPO_URL, "main");

        // Then
        List<ProgressEvent> events = progress.collect().asList().await().atMost(TIMEOUT);
        assertThat(events).isNotEmpty();

        ProgressEvent lastEvent = events.get(events.size() - 1);
        assertThat(lastEvent.stage()).isEqualTo("CLONING");
        assertThat(lastEvent.message()).isEqualTo("Repository cloned successfully");

        // Verify repository was cloned
        Path clonedPath = client.getClonedRepositoryPath();
        assertThat(clonedPath).isNotNull();
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
}
