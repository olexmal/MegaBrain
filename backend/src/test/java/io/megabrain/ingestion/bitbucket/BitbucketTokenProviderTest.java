/*
 * Copyright (c) 2025 MegaBrain Contributors
 * Licensed under the MIT License - see LICENSE file for details.
 */

package io.megabrain.ingestion.bitbucket;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
class BitbucketTokenProviderTest {

    @Test
    void getCredentials_shouldReturnCloudCredentials_whenUsernameAndAppPasswordPresent() {
        // given
        BitbucketTokenProvider provider = new BitbucketTokenProvider();
        provider.cloudUsername = Optional.of("alice");
        provider.cloudAppPassword = Optional.of("app-pass");

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
        BitbucketTokenProvider provider = new BitbucketTokenProvider();
        provider.serverUsername = Optional.of("bob");
        provider.serverToken = Optional.of("pat-123");

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
        BitbucketTokenProvider provider = new BitbucketTokenProvider();
        provider.cloudUsername = Optional.empty();
        provider.cloudAppPassword = Optional.of("");
        provider.serverUsername = Optional.of(" ");
        provider.serverToken = Optional.empty();

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
