/*
 * Copyright (c) 2025 MegaBrain Contributors
 * Licensed under the MIT License - see LICENSE file for details.
 */

package io.megabrain.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class IngestionRequestTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void deserialize_withAllFields_createsCompleteRecord() throws Exception {
        // Given
        String json = """
                {
                    "repository": "olexmal/MegaBrain",
                    "branch": "main",
                    "token": "secret-token",
                    "incremental": true
                }
                """;

        // When
        IngestionRequest request = mapper.readValue(json, IngestionRequest.class);

        // Then
        assertThat(request.repository()).isEqualTo("olexmal/MegaBrain");
        assertThat(request.branch()).isEqualTo("main");
        assertThat(request.token()).isEqualTo("secret-token");
        assertThat(request.incremental()).isTrue();
    }

    @Test
    void deserialize_withRequiredFieldsOnly_createsRecord() throws Exception {
        // Given
        String json = """
                {
                    "repository": "olexmal/MegaBrain"
                }
                """;

        // When
        IngestionRequest request = mapper.readValue(json, IngestionRequest.class);

        // Then
        assertThat(request.repository()).isEqualTo("olexmal/MegaBrain");
        assertThat(request.branch()).isNull();
        assertThat(request.token()).isNull();
        assertThat(request.incremental()).isNull();
    }
}
