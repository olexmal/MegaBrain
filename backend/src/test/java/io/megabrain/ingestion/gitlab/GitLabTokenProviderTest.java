/*
 * Copyright (c) 2025 MegaBrain Contributors
 * Licensed under the MIT License - see LICENSE file for details.
 */

package io.megabrain.ingestion.gitlab;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GitLabTokenProviderTest {

    @Mock
    private GitLabConfiguration config;

    @Test
    void tokenProvider_shouldReturnToken_whenConfigured() {
        when(config.token()).thenReturn(Optional.of("test-token"));
        GitLabTokenProvider provider = new GitLabTokenProvider(config);

        assertThat(provider.getToken()).isEqualTo("test-token");
        assertThat(provider.hasToken()).isTrue();
    }

    @Test
    void tokenProvider_shouldReturnNull_whenNotConfigured() {
        when(config.token()).thenReturn(Optional.empty());
        GitLabTokenProvider provider = new GitLabTokenProvider(config);

        assertThat(provider.getToken()).isNull();
        assertThat(provider.hasToken()).isFalse();
    }

    @Test
    void tokenProvider_shouldReturnNull_whenBlank() {
        when(config.token()).thenReturn(Optional.of("   "));
        GitLabTokenProvider provider = new GitLabTokenProvider(config);

        assertThat(provider.getToken()).isNull();
        assertThat(provider.hasToken()).isFalse();
    }
}
