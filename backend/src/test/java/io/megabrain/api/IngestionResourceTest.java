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
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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

    @Test
    void ingest_withValidSource_returnsAccepted() {
        // When
        Multi<String> response = ingestionResource.ingest("github", validRequest);
        
        // Subscribe to consume
        response.subscribe().with(item -> {});

        // Then
        verify(ingestionService).ingestRepository("test-repo");
    }

    @Test
    void ingest_withIncrementalTrue_callsIncrementalIngestion() {
        // Given
        IngestionRequest incrementalRequest = new IngestionRequest("test-repo", "main", "token", true);

        // When
        Multi<String> response = ingestionResource.ingest("github", incrementalRequest);

        // Subscribe
        response.subscribe().with(item -> {});

        // Then
        verify(ingestionService).ingestRepositoryIncrementally("test-repo");
    }

    @Test
    void ingest_withInvalidSource_throwsBadRequest() {
        // When / Then
        WebApplicationException ex = org.junit.jupiter.api.Assertions.assertThrows(
            WebApplicationException.class, 
            () -> ingestionResource.ingest("invalid-source", validRequest)
        );
        assertThat(ex.getResponse().getStatus()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
        assertThat(ex.getResponse().getEntity().toString()).contains("invalid-source");
    }

    @Test
    void ingest_withNullSource_throwsBadRequest() {
        // When / Then
        WebApplicationException ex = org.junit.jupiter.api.Assertions.assertThrows(
            WebApplicationException.class, 
            () -> ingestionResource.ingest(null, validRequest)
        );
        assertThat(ex.getResponse().getStatus()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
    }
    
    @Test
    void ingest_serviceThrowsIllegalArgumentException_throwsBadRequest() {
        // Given
        when(ingestionService.ingestRepository(anyString())).thenThrow(new IllegalArgumentException("Invalid repo"));
        
        // When / Then
        WebApplicationException ex = org.junit.jupiter.api.Assertions.assertThrows(
            WebApplicationException.class, 
            () -> ingestionResource.ingest("github", validRequest)
        );
        assertThat(ex.getResponse().getStatus()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
        assertThat(ex.getResponse().getEntity().toString()).contains("Invalid repo");
    }
    
    @Test
    void ingest_serviceThrowsException_throwsInternalServerError() {
        // Given
        when(ingestionService.ingestRepository(anyString())).thenThrow(new RuntimeException("Server error"));
        
        // When / Then
        WebApplicationException ex = org.junit.jupiter.api.Assertions.assertThrows(
            WebApplicationException.class, 
            () -> ingestionResource.ingest("github", validRequest)
        );
        assertThat(ex.getResponse().getStatus()).isEqualTo(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
        assertThat(ex.getResponse().getEntity().toString()).contains("Server error");
    }
}
