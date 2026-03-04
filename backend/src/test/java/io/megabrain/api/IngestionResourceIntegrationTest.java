/*
 * Copyright (c) 2025 MegaBrain Contributors
 * Licensed under the MIT License - see LICENSE file for details.
 */

package io.megabrain.api;

import io.megabrain.ingestion.IngestionService;
import io.megabrain.ingestion.ProgressEvent;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.smallrye.mutiny.Multi;
import jakarta.ws.rs.core.MediaType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@QuarkusTest
class IngestionResourceIntegrationTest {

    @InjectMock
    IngestionService ingestionService;

    @BeforeEach
    void setUp() {
        Multi<ProgressEvent> progressStream = Multi.createFrom().items(
                new ProgressEvent("Cloning", 10.0),
                new ProgressEvent("Parsing", 50.0),
                new ProgressEvent("Completed", 100.0)
        );

        when(ingestionService.ingestRepository(anyString())).thenReturn(progressStream);
        when(ingestionService.ingestRepositoryIncrementally(anyString())).thenReturn(progressStream);
    }

    @Test
    @DisplayName("Returns SSE stream for valid POST request")
    void ingest_validRequest_returnsSseStream() {
        String requestBody = """
                {
                    "repository": "test-repo",
                    "branch": "main",
                    "incremental": false
                }
                """;

        given()
                .contentType(ContentType.JSON)
                .body(requestBody)
                .when()
                .post("/api/v1/ingest/github")
                .then()
                .statusCode(200)
                .contentType(MediaType.SERVER_SENT_EVENTS)
                .body(containsString("event: progress"))
                .body(containsString("\"stage\":\"CLONING\""))
                .body(containsString("\"stage\":\"PARSING\""))
                .body(containsString("\"stage\":\"COMPLETE\""));
    }

    @Test
    @DisplayName("Returns 400 Bad Request for invalid source type")
    void ingest_invalidSource_returnsBadRequest() {
        String requestBody = """
                {
                    "repository": "test-repo"
                }
                """;

        given()
                .contentType(ContentType.JSON)
                .body(requestBody)
                .when()
                .post("/api/v1/ingest/invalid-source")
                .then()
                .statusCode(400)
                .body(containsString("Invalid source"));
    }

    @Test
    @DisplayName("Returns 400 Bad Request for missing repository")
    void ingest_missingRepository_returnsBadRequest() {
        String requestBody = """
                {
                    "branch": "main"
                }
                """;

        given()
                .contentType(ContentType.JSON)
                .body(requestBody)
                .when()
                .post("/api/v1/ingest/github")
                .then()
                .statusCode(400); // Bean validation @NotNull should trigger 400
    }

    @Test
    @DisplayName("Returns SSE stream with error for service exception")
    void ingest_serviceException_returnsErrorInSse() {
        when(ingestionService.ingestRepository(anyString()))
                .thenReturn(Multi.createFrom().failure(new RuntimeException("Service failure")));

        String requestBody = """
                {
                    "repository": "test-repo"
                }
                """;

        given()
                .contentType(ContentType.JSON)
                .body(requestBody)
                .when()
                .post("/api/v1/ingest/github")
                .then()
                .statusCode(200)
                .contentType(MediaType.SERVER_SENT_EVENTS)
                .body(containsString("\"stage\":\"FAILED\""))
                .body(containsString("Service failure"));
    }

    @Test
    @DisplayName("Handles concurrent requests correctly")
    void ingest_concurrentRequests_handledCorrectly() throws InterruptedException {
        int threadCount = 5;
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);

        String requestBody = """
                {
                    "repository": "test-repo-concurrent"
                }
                """;

        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    given()
                            .contentType(ContentType.JSON)
                            .body(requestBody)
                            .when()
                            .post("/api/v1/ingest/gitlab")
                            .then()
                            .statusCode(200)
                            .contentType(MediaType.SERVER_SENT_EVENTS)
                            .body(containsString("event: progress"));
                    successCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        boolean completed = latch.await(10, TimeUnit.SECONDS);
        assertThat(completed).isTrue();
        assertThat(successCount.get()).isEqualTo(threadCount);
        
        executorService.shutdown();
    }
}
