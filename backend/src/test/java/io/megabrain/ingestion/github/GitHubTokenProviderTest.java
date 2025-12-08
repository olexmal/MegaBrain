/*
 * Copyright (c) 2025 MegaBrain Contributors
 * Licensed under the MIT License - see LICENSE file for details.
 */

package io.megabrain.ingestion.github;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
class GitHubTokenProviderTest {

    @Test
    void getToken_shouldReturnValue_whenConfigured() {
        // given
        GitHubTokenProvider provider = new GitHubTokenProvider();
        provider.token = Optional.of("ghp-token");

        // when
        String token = provider.getToken();

        // then
        assertThat(token).isEqualTo("ghp-token");
        assertThat(provider.hasToken()).isTrue();
    }

    @Test
    void getToken_shouldReturnNull_whenMissingOrBlank() {
        // given
        GitHubTokenProvider provider = new GitHubTokenProvider();
        provider.token = Optional.of(" ");

        // when
        String token = provider.getToken();

        // then
        assertThat(token).isNull();
        assertThat(provider.hasToken()).isFalse();
    }
}
