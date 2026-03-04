/*
 * Copyright (c) 2025 MegaBrain Contributors
 * Licensed under the MIT License - see LICENSE file for details.
 */

package io.megabrain.api;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.InjectMock;
import io.megabrain.ingestion.IngestionService;
import io.megabrain.ingestion.ProgressEvent;
import io.smallrye.mutiny.Multi;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import jakarta.inject.Inject;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

@QuarkusTest
class IngestionResourceTest {

    @InjectMock
    IngestionService ingestionService;

    @Inject
    IngestionResource ingestionResource;

    private IngestionRequest validRequest;

    @BeforeEach
    void setUp() {
        validRequest = new IngestionRequest("test-repo", "main", "token", false);
        when(ingestionService.ingestRepository(anyString())).thenReturn(Multi.createFrom().empty());
        when(ingestionService.ingestRepositoryIncrementally(anyString())).thenReturn(Multi.createFrom().empty());
    }

    @Test
    void testResourceInjected() {
        assertNotNull(ingestionResource, "IngestionResource should be injected");
        assertNotNull(ingestionService, "IngestionService mock should be injected");
    }

    @ParameterizedTest
    @ValueSource(strings = {"github", "gitlab", "bitbucket", "local", "GITHUB", "GitLab"})
    void ingest_withValidSource_returnsAccepted(String source) {
        // When
        Response response = ingestionResource.ingest(source, validRequest).await().indefinitely();

        // Then
        assertThat(response.getStatus()).isEqualTo(Response.Status.ACCEPTED.getStatusCode());
        verify(ingestionService).ingestRepository("test-repo");
    }

    @Test
    void ingest_withIncrementalTrue_callsIncrementalIngestion() {
        // Given
        IngestionRequest incrementalRequest = new IngestionRequest("test-repo", "main", "token", true);

        // When
        Response response = ingestionResource.ingest("github", incrementalRequest).await().indefinitely();

        // Then
        assertThat(response.getStatus()).isEqualTo(Response.Status.ACCEPTED.getStatusCode());
        verify(ingestionService).ingestRepositoryIncrementally("test-repo");
    }

    @Test
    void ingest_withInvalidSource_returnsBadRequest() {
        // When
        Response response = ingestionResource.ingest("invalid-source", validRequest).await().indefinitely();

        // Then
        assertThat(response.getStatus()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
        assertThat(response.getEntity().toString()).contains("invalid-source");
    }

    @Test
    void ingest_withNullSource_returnsBadRequest() {
        // When
        Response response = ingestionResource.ingest(null, validRequest).await().indefinitely();

        // Then
        assertThat(response.getStatus()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
    }
    
    @Test
    void ingest_serviceThrowsIllegalArgumentException_returnsBadRequest() {
        // Given
        when(ingestionService.ingestRepository(anyString())).thenThrow(new IllegalArgumentException("Invalid repo"));
        
        // When
        Response response = ingestionResource.ingest("github", validRequest).await().indefinitely();
        
        // Then
        assertThat(response.getStatus()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
        assertThat(response.getEntity().toString()).contains("Invalid repo");
    }
    
    @Test
    void ingest_serviceThrowsException_returnsInternalServerError() {
        // Given
        when(ingestionService.ingestRepository(anyString())).thenThrow(new RuntimeException("Server error"));
        
        // When
        Response response = ingestionResource.ingest("github", validRequest).await().indefinitely();
        
        // Then
        assertThat(response.getStatus()).isEqualTo(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
        assertThat(response.getEntity().toString()).contains("Server error");
    }
}
