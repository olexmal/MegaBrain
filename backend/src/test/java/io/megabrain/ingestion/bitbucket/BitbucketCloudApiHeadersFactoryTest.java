/*
 * Copyright (c) 2025 MegaBrain Contributors
 * Licensed under the MIT License - see LICENSE file for details.
 */

package io.megabrain.ingestion.bitbucket;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import org.junit.jupiter.api.Test;

import java.util.Base64;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
class BitbucketCloudApiHeadersFactoryTest {

    @Test
    void update_shouldAddBasicAuthAndDefaultHeaders_whenCloudCredentialsPresent() {
        // given
        BitbucketTokenProvider tokenProvider = new BitbucketTokenProvider();
        tokenProvider.cloudUsername = Optional.of("alice");
        tokenProvider.cloudAppPassword = Optional.of("app-pass");

        BitbucketCloudApiHeadersFactory factory = new BitbucketCloudApiHeadersFactory();
        factory.tokenProvider = tokenProvider;

        MultivaluedMap<String, String> result = factory.update(new MultivaluedHashMap<>(), new MultivaluedHashMap<>());

        // then
        String expectedBasic = "Basic " + Base64.getEncoder().encodeToString("alice:app-pass".getBytes());
        assertThat(result.getFirst("Authorization")).isEqualTo(expectedBasic);
        assertThat(result.getFirst("User-Agent")).isEqualTo("MegaBrain/1.0");
        assertThat(result.getFirst("Accept")).isEqualTo("application/json");
    }

    @Test
    void update_shouldSkipAuthorization_whenCredentialsMissing() {
        // given
        BitbucketTokenProvider tokenProvider = new BitbucketTokenProvider();
        tokenProvider.cloudUsername = Optional.empty();
        tokenProvider.cloudAppPassword = Optional.empty();

        BitbucketCloudApiHeadersFactory factory = new BitbucketCloudApiHeadersFactory();
        factory.tokenProvider = tokenProvider;

        MultivaluedMap<String, String> result = factory.update(new MultivaluedHashMap<>(), new MultivaluedHashMap<>());

        // then
        assertThat(result).doesNotContainKey("Authorization");
        assertThat(result.getFirst("User-Agent")).isEqualTo("MegaBrain/1.0");
        assertThat(result.getFirst("Accept")).isEqualTo("application/json");
    }
}
