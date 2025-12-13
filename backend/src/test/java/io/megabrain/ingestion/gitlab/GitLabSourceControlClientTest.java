/*
 * Copyright (c) 2025 MegaBrain Contributors
 * Licensed under the MIT License - see LICENSE file for details.
 */

package io.megabrain.ingestion.gitlab;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
class GitLabSourceControlClientTest {

    // Note: For CDI beans with dependencies, we would typically use @Inject
    // For now, we'll create a basic instance for testing public methods

    @Test
    void canHandle_shouldReturnTrue_forValidGitLabUrls() {
        // Given
        GitLabSourceControlClient client = new GitLabSourceControlClient();
        List<String> validUrls = List.of(
            "https://gitlab.com/namespace/project",
            "https://gitlab.com/namespace/project.git",
            "http://gitlab.com/namespace/project",
            "https://gitlab.example.com/group/project",
            "https://gitlab.example.com/group/subgroup/project.git"
        );

        // When & Then
        for (String url : validUrls) {
            assertThat(client.canHandle(url)).isTrue();
        }
    }

    @Test
    void canHandle_shouldReturnFalse_forInvalidUrls() {
        // Given
        GitLabSourceControlClient client = new GitLabSourceControlClient();
        List<String> invalidUrls = Arrays.asList(
            "",
            "   ",
            null,
            "https://github.com/owner/repo",
            "https://bitbucket.org/owner/repo",
            "not-a-url",
            "ftp://example.com/repo"
        );

        // When & Then
        for (String url : invalidUrls) {
            assertThat(client.canHandle(url)).isFalse();
        }
    }

    @Test
    void canHandle_shouldReturnFalse_forNonGitLabUrls() {
        // Given
        GitLabSourceControlClient client = new GitLabSourceControlClient();
        List<String> nonGitLabUrls = List.of(
            "https://github.com/owner/repo",
            "https://bitbucket.org/owner/repo",
            "https://example.com/repo",
            "https://mycompany.com/repo"
        );

        // When & Then
        for (String url : nonGitLabUrls) {
            assertThat(client.canHandle(url)).isFalse();
        }
    }
}
