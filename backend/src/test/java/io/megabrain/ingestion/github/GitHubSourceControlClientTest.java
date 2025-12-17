/*
 * Copyright (c) 2025 MegaBrain Contributors
 * Licensed under the MIT License - see LICENSE file for details.
 */

package io.megabrain.ingestion.github;

import io.megabrain.ingestion.ProgressEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class GitHubSourceControlClientTest {

    @Mock
    private GitHubApiClient githubApiClient;

    @Mock
    private GitHubTokenProvider tokenProvider;

    private GitHubSourceControlClient client;

    @BeforeEach
    void setUp() {
        client = new GitHubSourceControlClient(githubApiClient, tokenProvider);
    }

    @Test
    void canHandle_shouldReturnTrue_forValidGitHubUrls() {
        List<String> validUrls = List.of(
            "https://github.com/owner/repo",
            "https://github.com/owner/repo.git",
            "http://github.com/owner/repo"
        );

        for (String url : validUrls) {
            assertThat(client.canHandle(url)).isTrue();
        }
    }

    @Test
    void canHandle_shouldReturnFalse_forInvalidUrls() {
        List<String> invalidUrls = List.of(
            "",
            "   ",
            "https://gitlab.com/owner/repo",
            "https://bitbucket.org/owner/repo",
            "not-a-url"
        );

        for (String url : invalidUrls) {
            assertThat(client.canHandle(url)).isFalse();
        }
    }

    @Test
    void canHandle_shouldReturnFalse_forNullUrl() {
        assertThat(client.canHandle(null)).isFalse();
    }

    @Test
    void extractFiles_shouldEmitProgressEvents_forValidRepository(@TempDir Path tempDir) throws IOException {
        Path repoPath = tempDir.resolve("repo");
        Files.createDirectories(repoPath);

        // Create some test files
        Files.writeString(repoPath.resolve("README.md"), "# Test Repository");
        Files.createDirectories(repoPath.resolve("src"));
        Files.writeString(repoPath.resolve("src/Main.java"), "public class Main {}");
        Files.createDirectories(repoPath.resolve(".git"));
        Files.writeString(repoPath.resolve(".git/config"), "[core]");

        var result = client.extractFiles(repoPath);

        List<ProgressEvent> events = result.collect().asList().await().indefinitely();
        assertThat(events).isNotEmpty();
        assertThat(events.getLast().message()).contains("extraction").contains("completed");
    }

    @Test
    void extractFiles_shouldHandleInvalidRepositoryPath(@TempDir Path tempDir) {
        Path invalidPath = tempDir.resolve("nonexistent");

        assertThat(client.extractFiles(invalidPath)).isNotNull();
    }

    @Test
    void getClonedRepositoryPath_shouldReturnNull_whenNotCloned() {
        Path result = client.getClonedRepositoryPath();

        assertThat(result).isNull();
    }
}
