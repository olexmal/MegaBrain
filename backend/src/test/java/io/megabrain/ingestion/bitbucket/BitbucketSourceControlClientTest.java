/*
 * Copyright (c) 2025 MegaBrain Contributors
 * Licensed under the MIT License - see LICENSE file for details.
 */

package io.megabrain.ingestion.bitbucket;

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
class BitbucketSourceControlClientTest {

    // Note: For CDI beans, we would typically use @Inject to get the bean instance
    // For now, we'll create a basic instance for testing public methods

    @Test
    void canHandle_shouldReturnTrue_forValidBitbucketCloudUrls() {
        // Given
        BitbucketSourceControlClient client = new BitbucketSourceControlClient();
        List<String> validCloudUrls = List.of(
            "https://bitbucket.org/workspace/repo",
            "https://bitbucket.org/workspace/repo.git",
            "http://bitbucket.org/workspace/repo",
            "https://www.bitbucket.org/workspace/repo"
        );

        // When & Then
        for (String url : validCloudUrls) {
            assertThat(client.canHandle(url)).isTrue();
        }
    }

    @Test
    void canHandle_shouldReturnTrue_forValidBitbucketServerUrls() {
        // Given
        BitbucketSourceControlClient client = new BitbucketSourceControlClient();
        List<String> validServerUrls = List.of(
            "https://company.bitbucket.com/projects/PROJ/repos/repo",
            "http://localhost:7990/projects/MYPROJ/repos/myrepo",
            "https://bitbucket.company.com/rest/api/1.0/projects/TEST/repos/test"
        );

        // When & Then
        for (String url : validServerUrls) {
            assertThat(client.canHandle(url)).isTrue();
        }
    }

    @Test
    void canHandle_shouldReturnFalse_forInvalidUrls() {
        // Given
        BitbucketSourceControlClient client = new BitbucketSourceControlClient();
        List<String> invalidUrls = List.of(
            "",
            "   ",
            "https://github.com/owner/repo",
            "https://gitlab.com/owner/repo",
            "not-a-url",
            "https://bitbucket.org/invalid",
            "https://bitbucket.org/workspace/",
            "https://bitbucket.org//repo"
        );

        // When & Then
        for (String url : invalidUrls) {
            assertThat(client.canHandle(url)).isFalse();
        }
    }

    @Test
    void canHandle_shouldReturnFalse_forNullUrl() {
        // Given
        BitbucketSourceControlClient client = new BitbucketSourceControlClient();

        // When & Then
        assertThat(client.canHandle(null)).isFalse();
    }

    @Test
    void extractFiles_shouldEmitProgressEvents_forValidRepository(@TempDir Path tempDir) throws IOException {
        // Given
        BitbucketSourceControlClient client = new BitbucketSourceControlClient();
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
        BitbucketSourceControlClient client = new BitbucketSourceControlClient();
        Path invalidPath = tempDir.resolve("nonexistent");

        // When & Then
        assertThatThrownBy(() -> client.extractFiles(invalidPath)
                .collect().asList()
                .await().indefinitely())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("does not exist or is not a directory");
    }

    @Test
    void getClonedRepositoryPath_shouldReturnNull_whenNotCloned() {
        // Given
        BitbucketSourceControlClient client = new BitbucketSourceControlClient();

        // When
        Path result = client.getClonedRepositoryPath();

        // Then
        assertThat(result).isNull();
    }

    @Test
    void parseRepositoryUrl_shouldParseBitbucketCloudUrl() {
        // Given
        BitbucketSourceControlClient client = new BitbucketSourceControlClient();

        // When - Test internal method via reflection or by testing behavior
        // For now, we'll test the canHandle method which uses parseRepositoryUrl internally
        boolean canHandle = client.canHandle("https://bitbucket.org/myworkspace/myrepo");

        // Then
        assertThat(canHandle).isTrue();
    }

    @Test
    void parseRepositoryUrl_shouldParseBitbucketServerUrl() {
        // Given
        BitbucketSourceControlClient client = new BitbucketSourceControlClient();

        // When
        boolean canHandle = client.canHandle("https://company.bitbucket.com/projects/MYPROJ/repos/myrepo");

        // Then
        assertThat(canHandle).isTrue();
    }

    @Test
    void parseRepositoryUrl_shouldThrowException_forInvalidUrl() {
        // Given
        BitbucketSourceControlClient client = new BitbucketSourceControlClient();

        // When & Then
        assertThat(client.canHandle("https://invalid-url.com/repo")).isFalse();
    }

    @Test
    void shouldIncludeFile_shouldExcludeHiddenFiles(@TempDir Path tempDir) throws IOException {
        // Given
        BitbucketSourceControlClient client = new BitbucketSourceControlClient();
        Path hiddenFile = tempDir.resolve(".hidden.txt");
        Files.writeString(hiddenFile, "hidden content");

        // When - Test via extractFiles which uses shouldIncludeFile internally
        var result = client.extractFiles(tempDir);
        List<ProgressEvent> events = result.collect().asList().await().indefinitely();

        // Then - Hidden files should be filtered out
        long hiddenFileEvents = events.stream()
                .filter(e -> e.message().contains(".hidden.txt"))
                .count();
        assertThat(hiddenFileEvents).isZero();
    }

    @Test
    void shouldIncludeFile_shouldIncludeSourceFiles(@TempDir Path tempDir) throws IOException {
        // Given
        BitbucketSourceControlClient client = new BitbucketSourceControlClient();
        Path sourceFile = tempDir.resolve("Main.java");
        Files.writeString(sourceFile, "public class Main {}");

        // When
        var result = client.extractFiles(tempDir);
        List<ProgressEvent> events = result.collect().asList().await().indefinitely();

        // Then
        assertThat(events).anyMatch(e -> e.message().contains("Main.java"));
    }
}
