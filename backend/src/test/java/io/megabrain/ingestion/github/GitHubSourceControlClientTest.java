/*
 * Copyright (c) 2025 MegaBrain Contributors
 * Licensed under the MIT License - see LICENSE file for details.
 */

package io.megabrain.ingestion.github;

import io.megabrain.ingestion.ProgressEvent;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@QuarkusTest
class GitHubSourceControlClientTest {

    // Note: For CDI beans, we would typically use @Inject to get the bean instance
    // For now, we'll create a basic instance for testing public methods

    @Test
    void canHandle_shouldReturnTrue_forValidGitHubUrls() {
        // Given
        GitHubSourceControlClient client = new GitHubSourceControlClient();
        List<String> validUrls = List.of(
            "https://github.com/owner/repo",
            "https://github.com/owner/repo.git",
            "http://github.com/owner/repo"
        );

        // When & Then
        for (String url : validUrls) {
            assertThat(client.canHandle(url)).isTrue();
        }
    }

    @Test
    void canHandle_shouldReturnFalse_forInvalidUrls() {
        // Given
        GitHubSourceControlClient client = new GitHubSourceControlClient();
        List<String> invalidUrls = List.of(
            "",
            "   ",
            "https://gitlab.com/owner/repo",
            "https://bitbucket.org/owner/repo",
            "not-a-url"
        );

        // When & Then
        for (String url : invalidUrls) {
            assertThat(client.canHandle(url)).isFalse();
        }
    }

    @Test
    void canHandle_shouldReturnFalse_forNullUrl() {
        // Given
        GitHubSourceControlClient client = new GitHubSourceControlClient();

        // When & Then
        assertThat(client.canHandle(null)).isFalse();
    }

    @Test
    void extractFiles_shouldEmitProgressEvents_forValidRepository(@TempDir Path tempDir) throws IOException {
        // Given
        GitHubSourceControlClient client = new GitHubSourceControlClient();
        Path repoPath = tempDir.resolve("repo");
        Files.createDirectories(repoPath);

        // Create some test files
        Files.writeString(repoPath.resolve("README.md"), "# Test Repository");
        Files.createDirectories(repoPath.resolve("src"));
        Files.writeString(repoPath.resolve("src/Main.java"), "public class Main {}");
        Files.createDirectories(repoPath.resolve(".git"));
        Files.writeString(repoPath.resolve(".git/config"), "[core]");

        // When
        var result = client.extractFiles(repoPath);

        // Then
        List<ProgressEvent> events = result.collect().asList().await().indefinitely();
        assertThat(events).isNotEmpty();
        assertThat(events.get(events.size() - 1).stage()).isEqualTo("EXTRACTING");
        assertThat(events.get(events.size() - 1).message()).contains("completed");
    }

    @Test
    void extractFiles_shouldHandleInvalidRepositoryPath(@TempDir Path tempDir) {
        // Given
        GitHubSourceControlClient client = new GitHubSourceControlClient();
        Path invalidPath = tempDir.resolve("nonexistent");

        // When & Then - This test verifies the method can be called
        // In a full implementation, we'd test error handling more thoroughly
        assertThat(client.extractFiles(invalidPath)).isNotNull();
    }

    @Test
    void getClonedRepositoryPath_shouldReturnNull_whenNotCloned() {
        // Given
        GitHubSourceControlClient client = new GitHubSourceControlClient();

        // When
        Path result = client.getClonedRepositoryPath();

        // Then
        assertThat(result).isNull();
    }
}
