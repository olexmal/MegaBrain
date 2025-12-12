/*
 * Copyright (c) 2025 MegaBrain Contributors
 * Licensed under the MIT License - see LICENSE file for details.
 */

package io.megabrain.ingestion.bitbucket;

import io.megabrain.ingestion.IngestionException;
import io.megabrain.ingestion.ProgressEvent;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.enterprise.inject.Vetoed;
import jakarta.ws.rs.WebApplicationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowable;

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
        assertThat(events.getLast().message()).contains("extraction");
        assertThat(events.getLast().message()).contains("completed");
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
    void canHandle_shouldAcceptServerScmAndSshUrls() {
        BitbucketSourceControlClient client = new BitbucketSourceControlClient();

        assertThat(client.canHandle("https://bitbucket.company.com/scm/PROJ/repo.git")).isTrue();
        assertThat(client.canHandle("git@bitbucket.company.com:PROJ/repo.git")).isTrue();
    }

    @Test
    void buildCloneUrl_shouldUseConfiguredBaseUrlForServer() throws Exception {
        BitbucketSourceControlClient client = new BitbucketSourceControlClient();

        // Inject server base URL
        Field baseField = BitbucketSourceControlClient.class.getDeclaredField("serverBaseUrl");
        baseField.setAccessible(true);
        baseField.set(client, Optional.of("https://bitbucket.company.com/rest/api/1.0"));

        // Parse server URL via reflection
        Method parse = BitbucketSourceControlClient.class.getDeclaredMethod("parseRepositoryUrl", String.class);
        parse.setAccessible(true);
        Object urlParts = parse.invoke(client, "https://bitbucket.company.com/projects/PROJ/repos/repo");

        // Build clone URL via reflection
        Method build = BitbucketSourceControlClient.class.getDeclaredMethod("buildCloneUrl", urlParts.getClass());
        build.setAccessible(true);
        String cloneUrl = (String) build.invoke(client, urlParts);

        assertThat(cloneUrl).isEqualTo("https://bitbucket.company.com/scm/PROJ/repo.git");
    }

    @Test
    void buildCloneUrl_shouldFailWhenServerBaseUrlMissing() throws Exception {
        BitbucketSourceControlClient client = new BitbucketSourceControlClient();

        // Ensure Optional is present but empty instead of null to avoid NPE
        Field baseField = BitbucketSourceControlClient.class.getDeclaredField("serverBaseUrl");
        baseField.setAccessible(true);
        baseField.set(client, Optional.empty());

        Method parse = BitbucketSourceControlClient.class.getDeclaredMethod("parseRepositoryUrl", String.class);
        parse.setAccessible(true);
        Object urlParts = parse.invoke(client, "https://bitbucket.company.com/projects/PROJ/repos/repo");

        Method build = BitbucketSourceControlClient.class.getDeclaredMethod("buildCloneUrl", urlParts.getClass());
        build.setAccessible(true);

        Throwable thrown = catchThrowable(() -> build.invoke(client, urlParts));
        assertThat(thrown).hasCauseInstanceOf(IllegalStateException.class);
        assertThat(thrown.getCause().getMessage()).contains("bitbucket-server-api/mp-rest/url");
    }

    @Test
    void fetchMetadata_shouldMapServerAuthFailure() throws Exception {
        BitbucketSourceControlClient client = new BitbucketSourceControlClient();
        injectServerClient(client, new ThrowingServerApiClient(401));

        assertThatThrownBy(() -> client.fetchMetadata("https://company.bitbucket.com/projects/PROJ/repos/repo")
                .await().indefinitely())
                .isInstanceOf(IngestionException.class)
                .hasMessageContaining("authentication failed");
    }

    @Test
    void fetchMetadata_shouldMapServerRateLimit() throws Exception {
        BitbucketSourceControlClient client = new BitbucketSourceControlClient();
        injectServerClient(client, new ThrowingServerApiClient(429));

        assertThatThrownBy(() -> client.fetchMetadata("https://company.bitbucket.com/projects/PROJ/repos/repo")
                .await().indefinitely())
                .isInstanceOf(IngestionException.class)
                .hasMessageContaining("rate limited");
    }

    @Test
    void fetchMetadata_shouldMapCloudAuthFailure() throws Exception {
        BitbucketSourceControlClient client = new BitbucketSourceControlClient();
        injectCloudClient(client, new ThrowingCloudApiClient(401));

        assertThatThrownBy(() -> client.fetchMetadata("https://bitbucket.org/workspace/repo")
                .await().indefinitely())
                .isInstanceOf(IngestionException.class)
                .hasMessageContaining("authentication failed");
    }

    private void injectServerClient(BitbucketSourceControlClient client, BitbucketServerApiClient api) throws Exception {
        Field serverField = BitbucketSourceControlClient.class.getDeclaredField("bitbucketServerApiClient");
        serverField.setAccessible(true);
        serverField.set(client, api);
    }

    private void injectCloudClient(BitbucketSourceControlClient client, BitbucketCloudApiClient api) throws Exception {
        Field cloudField = BitbucketSourceControlClient.class.getDeclaredField("bitbucketCloudApiClient");
        cloudField.setAccessible(true);
        cloudField.set(client, api);
    }

    @Vetoed
    private static class ThrowingServerApiClient implements BitbucketServerApiClient {
        private final int status;
        ThrowingServerApiClient(int status) { this.status = status; }
        @Override
        public BitbucketServerRepositoryInfo getRepository(String project, String repo) {
            throw new WebApplicationException(status);
        }
        @Override
        public BitbucketServerCommitInfo getCommit(String project, String repo, String branch) {
            throw new WebApplicationException(status);
        }
    }

    @Vetoed
    private static class ThrowingCloudApiClient implements BitbucketCloudApiClient {
        private final int status;
        ThrowingCloudApiClient(int status) { this.status = status; }
        @Override
        public BitbucketCloudRepositoryInfo getRepository(String workspace, String repo) {
            throw new WebApplicationException(status);
        }
        @Override
        public BitbucketCloudCommitInfo getCommit(String workspace, String repo, String branch) {
            throw new WebApplicationException(status);
        }
    }

    @Test
    void mapBitbucketException_shouldProvideAuthMessage() throws Exception {
        BitbucketSourceControlClient client = new BitbucketSourceControlClient();
        Method mapper = BitbucketSourceControlClient.class.getDeclaredMethod("mapBitbucketException", Throwable.class);
        mapper.setAccessible(true);

        IngestionException result = (IngestionException) mapper.invoke(client, new WebApplicationException(401));
        assertThat(result).isNotNull();
        assertThat(result.getMessage()).contains("authentication failed");
    }

    @Test
    void mapBitbucketException_shouldProvideRateLimitMessage() throws Exception {
        BitbucketSourceControlClient client = new BitbucketSourceControlClient();
        Method mapper = BitbucketSourceControlClient.class.getDeclaredMethod("mapBitbucketException", Throwable.class);
        mapper.setAccessible(true);

        IngestionException result = (IngestionException) mapper.invoke(client, new WebApplicationException(429));
        assertThat(result).isNotNull();
        assertThat(result.getMessage()).contains("rate limited");
    }

    @Test
    void isRateLimit_shouldDetect429() throws Exception {
        BitbucketSourceControlClient client = new BitbucketSourceControlClient();
        Method rate = BitbucketSourceControlClient.class.getDeclaredMethod("isRateLimit", Throwable.class);
        rate.setAccessible(true);

        boolean is429 = (boolean) rate.invoke(client, new WebApplicationException(429));
        boolean not429 = (boolean) rate.invoke(client, new WebApplicationException(500));

        assertThat(is429).isTrue();
        assertThat(not429).isFalse();
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

    @Test
    void shouldIncludeFile_shouldRespectRootGitignore(@TempDir Path tempDir) throws IOException {
        BitbucketSourceControlClient client = new BitbucketSourceControlClient();
        Files.writeString(tempDir.resolve(".gitignore"), "*.md");

        Path readme = tempDir.resolve("README.md");
        Files.writeString(readme, "# ignore me");
        Path javaFile = tempDir.resolve("Main.java");
        Files.writeString(javaFile, "public class Main {}");

        var events = client.extractFiles(tempDir).collect().asList().await().indefinitely();

        assertThat(events).anyMatch(e -> e.message().contains("Main.java"))
                .noneMatch(e -> e.message().contains("README.md"));
    }
}
