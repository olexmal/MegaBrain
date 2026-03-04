/*
 * Copyright (c) 2025 MegaBrain Contributors
 * Licensed under the MIT License - see LICENSE file for details.
 */

package io.megabrain.api;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.InjectMock;
import io.megabrain.ingestion.IngestionService;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import jakarta.inject.Inject;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
class IngestionResourceTest {

    @InjectMock
    IngestionService ingestionService;

    @Inject
    IngestionResource ingestionResource;

    @Test
    void testResourceInjected() {
        assertNotNull(ingestionResource, "IngestionResource should be injected");
        assertNotNull(ingestionService, "IngestionService mock should be injected");
    }

    @ParameterizedTest
    @ValueSource(strings = {"github", "gitlab", "bitbucket", "local", "GITHUB", "GitLab"})
    void ingest_withValidSource_returnsAccepted(String source) {
        // When
        Response response = ingestionResource.ingest(source).await().indefinitely();

        // Then
        assertThat(response.getStatus()).isEqualTo(Response.Status.ACCEPTED.getStatusCode());
    }

    @Test
    void ingest_withInvalidSource_returnsBadRequest() {
        // When
        Response response = ingestionResource.ingest("invalid-source").await().indefinitely();

        // Then
        assertThat(response.getStatus()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
        assertThat(response.getEntity().toString()).contains("invalid-source");
    }

    @Test
    void ingest_withNullSource_returnsBadRequest() {
        // When
        Response response = ingestionResource.ingest(null).await().indefinitely();

        // Then
        assertThat(response.getStatus()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
    }
}
