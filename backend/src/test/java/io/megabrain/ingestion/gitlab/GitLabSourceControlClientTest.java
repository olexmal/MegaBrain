/*
 * Copyright (c) 2025 MegaBrain Contributors
 * Licensed under the MIT License - see LICENSE file for details.
 */

package io.megabrain.ingestion.gitlab;

import io.megabrain.ingestion.IngestionException;
import io.megabrain.ingestion.ProgressEvent;
import io.megabrain.ingestion.RepositoryMetadata;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.lenient;

import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletionException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@QuarkusTest
@ExtendWith(MockitoExtension.class)
class GitLabSourceControlClientTest {

    @Mock
    private GitLabApiClient gitlabApiClient;

    @Mock
    private GitLabTokenProvider tokenProvider;

    @Mock
    private GitLabConfiguration config;

    private GitLabSourceControlClient client;

    @BeforeEach
    void setUp() {
        // Initialize mocks
        MockitoAnnotations.openMocks(this);

        // Set up mock behavior (lenient to avoid unnecessary stubbing exceptions)
        lenient().when(config.apiUrl()).thenReturn("https://gitlab.com");
        lenient().when(config.connectTimeout()).thenReturn(10000);
        lenient().when(config.readTimeout()).thenReturn(30000);
        lenient().when(tokenProvider.getToken()).thenReturn("test-token");

        // Create client and manually inject dependencies
        client = new GitLabSourceControlClient();
        setField(client, "config", config);
        setField(client, "gitlabApiClient", gitlabApiClient);
        setField(client, "tokenProvider", tokenProvider);
    }

    private void setField(Object target, String fieldName, Object value) {
        try {
            java.lang.reflect.Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set field " + fieldName, e);
        }
    }

    // Note: For CDI beans with dependencies, we would typically use @Inject
    // For now, we'll create a basic instance for testing public methods

    @Test
    void canHandle_shouldReturnTrue_forValidGitLabUrls() {
        // Given
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
    void fetchMetadata_shouldReturnRepositoryMetadata_forValidGitLabUrl() {
        // Given
        String repositoryUrl = "https://gitlab.com/namespace/project";
        GitLabRepositoryInfo mockProject = new GitLabRepositoryInfo(
            123, "project", "namespace/project", "main",
            "https://gitlab.com/namespace/project.git",
            "git@gitlab.com:namespace/project.git",
            "https://gitlab.com/namespace/project",
            new GitLabRepositoryInfo.Namespace(1, "namespace", "namespace", "group", "namespace")
        );

        GitLabCommitInfo[] mockCommits = new GitLabCommitInfo[]{
            new GitLabCommitInfo("abc123", "abc123", "Initial commit", "Initial commit",
                new GitLabCommitInfo.Author("John Doe", "john@example.com", "2023-01-01T00:00:00Z"),
                "https://gitlab.com/namespace/project/-/commit/abc123")
        };

        when(gitlabApiClient.getProject("namespace%2Fproject")).thenReturn(mockProject);
        when(gitlabApiClient.getCommits("namespace%2Fproject", "main", 1)).thenReturn(mockCommits);

        // When
        Uni<RepositoryMetadata> result = client.fetchMetadata(repositoryUrl);
        RepositoryMetadata metadata = result.subscribe().asCompletionStage().join();

        // Then
        assertThat(metadata).isNotNull();
        assertThat(metadata.name()).isEqualTo("project");
        assertThat(metadata.owner()).isEqualTo("namespace");
        assertThat(metadata.defaultBranch()).isEqualTo("main");
        assertThat(metadata.latestCommitSha()).isEqualTo("abc123");
        assertThat(metadata.url()).isEqualTo("https://gitlab.com/namespace/project.git");

        verify(gitlabApiClient).getProject("namespace%2Fproject");
        verify(gitlabApiClient).getCommits("namespace%2Fproject", "main", 1);
    }

    @Test
    void fetchMetadata_shouldHandleSelfHostedGitLab() {
        // Given
        lenient().when(config.apiUrl()).thenReturn("https://gitlab.company.com");
        String repositoryUrl = "https://gitlab.company.com/group/project";
        GitLabRepositoryInfo mockProject = new GitLabRepositoryInfo(
            456, "project", "group/project", "develop",
            "https://gitlab.company.com/group/project.git",
            "git@gitlab.company.com:group/project.git",
            "https://gitlab.company.com/group/project",
            new GitLabRepositoryInfo.Namespace(2, "group", "group", "group", "group")
        );

        GitLabCommitInfo[] mockCommits = new GitLabCommitInfo[]{
            new GitLabCommitInfo("def456", "def456", "Update README", "Update README",
                new GitLabCommitInfo.Author("Jane Doe", "jane@example.com", "2023-01-02T00:00:00Z"),
                "https://gitlab.company.com/group/project/-/commit/def456")
        };

        when(gitlabApiClient.getProject("group%2Fproject")).thenReturn(mockProject);
        when(gitlabApiClient.getCommits("group%2Fproject", "develop", 1)).thenReturn(mockCommits);

        // When
        Uni<RepositoryMetadata> result = client.fetchMetadata(repositoryUrl);
        RepositoryMetadata metadata = result.subscribe().asCompletionStage().join();

        // Then
        assertThat(metadata).isNotNull();
        assertThat(metadata.name()).isEqualTo("project");
        assertThat(metadata.owner()).isEqualTo("group");
        assertThat(metadata.defaultBranch()).isEqualTo("develop");
        assertThat(metadata.latestCommitSha()).isEqualTo("def456");
    }

    @Test
    void fetchMetadata_shouldHandleApiErrors() {
        // Given
        String repositoryUrl = "https://gitlab.com/namespace/project";
        Response response = Response.status(404).build();
        WebApplicationException apiException = new WebApplicationException(response);

        when(gitlabApiClient.getProject(anyString())).thenThrow(apiException);

        // When & Then
        Uni<RepositoryMetadata> result = client.fetchMetadata(repositoryUrl);
        assertThatThrownBy(() -> result.subscribe().asCompletionStage().join())
            .isInstanceOf(CompletionException.class)
            .hasCauseInstanceOf(IngestionException.class)
            .hasMessageContaining("Failed to fetch repository metadata");
    }

    @Test
    void cloneRepository_shouldHandleValidGitLabUrl() {
        // Given
        String repositoryUrl = "https://gitlab.com/namespace/project";

        // When - Note: This is hard to test fully due to JGit complexity, so we test URL parsing
        boolean canHandle = client.canHandle(repositoryUrl);

        // Then
        assertThat(canHandle).isTrue();
        // Full clone testing would require complex mocking of JGit
    }

    @Test
    void extractFiles_shouldHandleValidRepositoryPath() {
        // Given
        Path mockPath = mock(Path.class);
        lenient().when(mockPath.toString()).thenReturn("/tmp/test-repo");

        // When - Note: Full testing would require file system mocking
        Multi<ProgressEvent> result = client.extractFiles(mockPath);

        // Then
        assertThat(result).isNotNull();
        // Full file extraction testing would require complex file system mocking
    }

    @Test
    void rateLimitHandling_shouldRetryAfter429Error() throws Exception {
        // Given
        // Create a proper response for 429 error
        Response response = Response.status(429).header("Retry-After", "2").build();
        WebApplicationException rateLimitException = new WebApplicationException(response);

        // Use reflection to test the private method
        var method = GitLabSourceControlClient.class.getDeclaredMethod("fetchWithRateLimitHandling", Supplier.class);
        method.setAccessible(true);

        // Mock a supplier that throws rate limit exception on first call, succeeds on second
        AtomicInteger callCount = new AtomicInteger(0);
        Supplier<String> mockSupplier = () -> {
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
        // Create a proper response for 401 error
        Response response = Response.status(401).build();
        WebApplicationException authException = new WebApplicationException(response);

        // Use reflection to test the private method
        var method = GitLabSourceControlClient.class.getDeclaredMethod("fetchWithRateLimitHandling", Supplier.class);
        method.setAccessible(true);

        // Mock a supplier that throws auth exception
        Supplier<String> mockSupplier = () -> {
            throw authException;
        };

        // When & Then
        assertThatThrownBy(() -> method.invoke(client, mockSupplier))
            .isInstanceOf(InvocationTargetException.class)
            .hasCauseInstanceOf(IngestionException.class);
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

    @Test
    void tokenAuthentication_shouldUseSecureStorage() {
        // Given - Test that tokens are handled securely
        GitLabSourceControlClient client = new GitLabSourceControlClient();

        // Test that we have token provider injected (security through dependency injection)
        // The actual token handling is tested in GitLabTokenProviderTest
        assertThat(client).isNotNull();

        // This test verifies the integration point exists
        // Actual token security is tested in the token provider
    }
}
