/*
 * Copyright (c) 2025 MegaBrain Contributors
 * Licensed under the MIT License - see LICENSE file for details.
 */

package io.megabrain.ingestion.bitbucket;

import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import org.junit.jupiter.api.Test;

import java.util.Base64;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class BitbucketServerApiHeadersFactoryTest {

    @Test
    void update_shouldAddBearerAuthAndDefaultHeaders_whenServerCredentialsPresent() {
        // given
        BitbucketTokenProvider tokenProvider = new BitbucketTokenProvider(
                Optional.empty(),
                Optional.empty(),
                Optional.of("alice"),
                Optional.of("pat-123")
        );

        BitbucketServerApiHeadersFactory factory = new BitbucketServerApiHeadersFactory(tokenProvider);

        MultivaluedMap<String, String> headers = new MultivaluedHashMap<>();

        // when
        MultivaluedMap<String, String> result = factory.update(new MultivaluedHashMap<>(), headers);

        // then
        String expectedBasic = "Basic " + Base64.getEncoder().encodeToString("alice:pat-123".getBytes());
        assertThat(result.getFirst("Authorization")).isEqualTo(expectedBasic);
        assertThat(result.getFirst("User-Agent")).isEqualTo("MegaBrain/1.0");
        assertThat(result.getFirst("Accept")).isEqualTo("application/json");
    }

    @Test
    void update_shouldSkipAuthorization_whenCredentialsMissing() {
        // given
        BitbucketTokenProvider tokenProvider = new BitbucketTokenProvider(
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty()
        );

        BitbucketServerApiHeadersFactory factory = new BitbucketServerApiHeadersFactory(tokenProvider);

        MultivaluedMap<String, String> result = factory.update(new MultivaluedHashMap<>(), new MultivaluedHashMap<>());

        // then
        assertThat(result).doesNotContainKey("Authorization");
        assertThat(result.getFirst("User-Agent")).isEqualTo("MegaBrain/1.0");
        assertThat(result.getFirst("Accept")).isEqualTo("application/json");
    }
}
