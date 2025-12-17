/*
 * Copyright (c) 2025 MegaBrain Contributors
 * Licensed under the MIT License - see LICENSE file for details.
 */

package io.megabrain.ingestion.bitbucket;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class BitbucketTokenProviderTest {

    @Test
    void getCredentials_shouldReturnCloudCredentials_whenUsernameAndAppPasswordPresent() {
        // given
        BitbucketTokenProvider provider = new BitbucketTokenProvider(
                Optional.of("alice"),
                Optional.of("app-pass"),
                Optional.empty(),
                Optional.empty()
        );

        // when
        BitbucketTokenProvider.BitbucketCredentials credentials = provider.getCredentials(true);

        // then
        assertThat(credentials).isNotNull();
        assertThat(credentials.username()).isEqualTo("alice");
        assertThat(credentials.password()).isEqualTo("app-pass");
        assertThat(provider.hasCredentials(true)).isTrue();
    }

    @Test
    void getCredentials_shouldReturnServerCredentials_whenUsernameAndTokenPresent() {
        // given
        BitbucketTokenProvider provider = new BitbucketTokenProvider(
                Optional.empty(),
                Optional.empty(),
                Optional.of("bob"),
                Optional.of("pat-123")
        );

        // when
        BitbucketTokenProvider.BitbucketCredentials credentials = provider.getCredentials(false);

        // then
        assertThat(credentials).isNotNull();
        assertThat(credentials.username()).isEqualTo("bob");
        assertThat(credentials.password()).isEqualTo("pat-123");
        assertThat(provider.hasCredentials(false)).isTrue();
    }

    @Test
    void getCredentials_shouldReturnNull_whenConfigMissingOrBlank() {
        // given
        BitbucketTokenProvider provider = new BitbucketTokenProvider(
                Optional.empty(),
                Optional.of(""),
                Optional.of(" "),
                Optional.empty()
        );

        // when
        BitbucketTokenProvider.BitbucketCredentials cloudCreds = provider.getCredentials(true);
        BitbucketTokenProvider.BitbucketCredentials serverCreds = provider.getCredentials(false);

        // then
        assertThat(cloudCreds).isNull();
        assertThat(serverCreds).isNull();
        assertThat(provider.hasCredentials(true)).isFalse();
        assertThat(provider.hasCredentials(false)).isFalse();
    }
}
