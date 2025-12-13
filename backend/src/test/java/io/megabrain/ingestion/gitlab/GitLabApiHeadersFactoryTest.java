/*
 * Copyright (c) 2025 MegaBrain Contributors
 * Licensed under the MIT License - see LICENSE file for details.
 */

package io.megabrain.ingestion.gitlab;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
class GitLabApiHeadersFactoryTest {

    private static final String TOKEN_PROPERTY = "megabrain.gitlab.token";

    @AfterEach
    void clearToken() {
        System.clearProperty(TOKEN_PROPERTY);
    }

    @Test
    void update_shouldAddPrivateTokenHeader_whenTokenConfiguredWithoutPrefix() {
        // given
        System.setProperty(TOKEN_PROPERTY, "glpat-abc123");
        GitLabApiHeadersFactory factory = new GitLabApiHeadersFactory();

        // when
        MultivaluedMap<String, String> headers = factory.update(new MultivaluedHashMap<>(), new MultivaluedHashMap<>());

        // then
        assertThat(headers.getFirst("PRIVATE-TOKEN")).isEqualTo("glpat-abc123");
    }

    @Test
    void update_shouldPreserveToken_whenTokenAlreadyHasPrefix() {
        // given
        System.setProperty(TOKEN_PROPERTY, "PRIVATE-TOKEN glpat-secret");
        GitLabApiHeadersFactory factory = new GitLabApiHeadersFactory();

        // when
        MultivaluedMap<String, String> headers = factory.update(new MultivaluedHashMap<>(), new MultivaluedHashMap<>());

        // then
        assertThat(headers.getFirst("PRIVATE-TOKEN")).isEqualTo("glpat-secret");
    }

    @Test
    void update_shouldNotAddTokenHeader_whenTokenMissing() {
        // given
        GitLabApiHeadersFactory factory = new GitLabApiHeadersFactory();

        // when
        MultivaluedMap<String, String> headers = factory.update(new MultivaluedHashMap<>(), new MultivaluedHashMap<>());

        // then
        assertThat(headers).doesNotContainKey("PRIVATE-TOKEN");
    }

    @Test
    void update_shouldHandlePersonalAccessTokens() {
        // given - Personal access tokens from gitlab.com
        System.setProperty(TOKEN_PROPERTY, "glpat-abc123personal");
        GitLabApiHeadersFactory factory = new GitLabApiHeadersFactory();

        // when
        MultivaluedMap<String, String> headers = factory.update(new MultivaluedHashMap<>(), new MultivaluedHashMap<>());

        // then
        assertThat(headers.getFirst("PRIVATE-TOKEN")).isEqualTo("glpat-abc123personal");
    }

    @Test
    void update_shouldHandleProjectAccessTokens() {
        // given - Project access tokens (same format as personal tokens)
        System.setProperty(TOKEN_PROPERTY, "glpat-xyz789project");
        GitLabApiHeadersFactory factory = new GitLabApiHeadersFactory();

        // when
        MultivaluedMap<String, String> headers = factory.update(new MultivaluedHashMap<>(), new MultivaluedHashMap<>());

        // then
        assertThat(headers.getFirst("PRIVATE-TOKEN")).isEqualTo("glpat-xyz789project");
    }

    @Test
    void update_shouldHandleLegacyTokenFormats() {
        // given - Some users might still use old token formats
        System.setProperty(TOKEN_PROPERTY, "legacy-token-123");
        GitLabApiHeadersFactory factory = new GitLabApiHeadersFactory();

        // when
        MultivaluedMap<String, String> headers = factory.update(new MultivaluedHashMap<>(), new MultivaluedHashMap<>());

        // then
        assertThat(headers.getFirst("PRIVATE-TOKEN")).isEqualTo("legacy-token-123");
    }
}
