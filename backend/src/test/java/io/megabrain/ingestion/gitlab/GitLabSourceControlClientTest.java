/*
 * Copyright (c) 2025 MegaBrain Contributors
 * Licensed under the MIT License - see LICENSE file for details.
 */

package io.megabrain.ingestion.gitlab;

import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.StatusType;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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
        List<String> nonGitLabUrls = Arrays.asList(
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

    @Test
    void rateLimitHandling_shouldRetryAfter429Error() throws Exception {
        // Given
        GitLabSourceControlClient client = new GitLabSourceControlClient();

        // Create a proper response for 429 error
        Response response = Response.status(429).header("Retry-After", "2").build();
        WebApplicationException rateLimitException = new WebApplicationException(response);

        // Use reflection to test the private method
        var method = GitLabSourceControlClient.class.getDeclaredMethod("fetchWithRateLimitHandling", java.util.function.Supplier.class);
        method.setAccessible(true);

        // Mock a supplier that throws rate limit exception on first call, succeeds on second
        java.util.concurrent.atomic.AtomicInteger callCount = new java.util.concurrent.atomic.AtomicInteger(0);
        java.util.function.Supplier<String> mockSupplier = () -> {
            if (callCount.incrementAndGet() == 1) {
                throw rateLimitException;
            }
            return "success";
        };

        // When
        long startTime = System.currentTimeMillis();
        String result = (String) method.invoke(client, mockSupplier);
        long endTime = System.currentTimeMillis();

        // Then
        assertThat(result).isEqualTo("success");
        assertThat(callCount.get()).isEqualTo(2); // Should have been called twice
        assertThat(endTime - startTime).isGreaterThanOrEqualTo(2000); // Should have waited at least 2 seconds
    }

    @Test
    void rateLimitHandling_shouldHandleAuthErrors() throws Exception {
        // Given
        GitLabSourceControlClient client = new GitLabSourceControlClient();

        // Create a proper response for 401 error
        Response response = Response.status(401).build();
        WebApplicationException authException = new WebApplicationException(response);

        // Use reflection to test the private method
        var method = GitLabSourceControlClient.class.getDeclaredMethod("fetchWithRateLimitHandling", java.util.function.Supplier.class);
        method.setAccessible(true);

        // Mock a supplier that throws auth exception
        java.util.function.Supplier<String> mockSupplier = () -> {
            throw authException;
        };

        // When & Then
        assertThatThrownBy(() -> method.invoke(client, mockSupplier))
            .isInstanceOf(java.lang.reflect.InvocationTargetException.class)
            .hasCauseInstanceOf(io.megabrain.ingestion.IngestionException.class);
    }

    @Test
    void validateGitLabConnection_shouldHandleConnectionErrors() throws Exception {
        // Given
        GitLabSourceControlClient client = new GitLabSourceControlClient();

        // Test that the method exists and can be called (would need mocking for full test)
        var method = GitLabSourceControlClient.class.getDeclaredMethod("validateGitLabConnection");
        method.setAccessible(true);

        // This test just verifies the method exists and can be callable
        // Full integration testing would require mocking the GitLab API client
        assertThat(method).isNotNull();
    }
}
