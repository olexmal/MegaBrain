/*
 * Copyright (c) 2025 MegaBrain Contributors
 * Licensed under the MIT License - see LICENSE file for details.
 */

package io.megabrain.ingestion.github;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
class GitHubApiHeadersFactoryTest {

    private static final String TOKEN_PROPERTY = "megabrain.github.token";

    @AfterEach
    void clearToken() {
        System.clearProperty(TOKEN_PROPERTY);
    }

    @Test
    void update_shouldAddApiVersionAndTokenPrefix_whenTokenConfiguredWithoutPrefix() {
        // given
        System.setProperty(TOKEN_PROPERTY, "abc123");
        GitHubApiHeadersFactory factory = new GitHubApiHeadersFactory();

        // when
        MultivaluedMap<String, String> headers = factory.update(new MultivaluedHashMap<>(), new MultivaluedHashMap<>());

        // then
        assertThat(headers.getFirst("X-GitHub-Api-Version")).isEqualTo("2022-11-28");
        assertThat(headers.getFirst("Authorization")).isEqualTo("token abc123");
    }

    @Test
    void update_shouldPreservePrefix_whenTokenAlreadyPrefixed() {
        // given
        System.setProperty(TOKEN_PROPERTY, "Bearer secret");
        GitHubApiHeadersFactory factory = new GitHubApiHeadersFactory();

        // when
        MultivaluedMap<String, String> headers = factory.update(new MultivaluedHashMap<>(), new MultivaluedHashMap<>());

        // then
        assertThat(headers.getFirst("Authorization")).isEqualTo("Bearer secret");
        assertThat(headers.getFirst("X-GitHub-Api-Version")).isEqualTo("2022-11-28");
    }

    @Test
    void update_shouldOnlyAddVersionHeader_whenTokenMissing() {
        // given
        GitHubApiHeadersFactory factory = new GitHubApiHeadersFactory();

        // when
        MultivaluedMap<String, String> headers = factory.update(new MultivaluedHashMap<>(), new MultivaluedHashMap<>());

        // then
        assertThat(headers.containsKey("Authorization")).isFalse();
        assertThat(headers.getFirst("X-GitHub-Api-Version")).isEqualTo("2022-11-28");
    }
}
