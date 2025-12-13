/*
 * Copyright (c) 2025 MegaBrain Contributors
 * Licensed under the MIT License - see LICENSE file for details.
 */

package io.megabrain.ingestion.gitlab;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
class GitLabTokenProviderTest {

    @Test
    void getToken_shouldReturnValue_whenConfigured() {
        // given
        GitLabTokenProvider provider = new GitLabTokenProvider();
        provider.token = Optional.of("glpat-token");

        // when
        String token = provider.getToken();

        // then
        assertThat(token).isEqualTo("glpat-token");
        assertThat(provider.hasToken()).isTrue();
    }

    @Test
    void getToken_shouldReturnNull_whenMissingOrBlank() {
        // given
        GitLabTokenProvider provider = new GitLabTokenProvider();
        provider.token = Optional.of(" ");

        // when
        String token = provider.getToken();

        // then
        assertThat(token).isNull();
        assertThat(provider.hasToken()).isFalse();
    }
}
