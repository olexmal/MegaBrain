/*
 * Copyright (c) 2025 MegaBrain Contributors
 * Licensed under the MIT License - see LICENSE file for details.
 */

package io.megabrain.api;

import io.megabrain.core.RagService;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import jakarta.ws.rs.core.MediaType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Integration tests for RAG REST endpoint (US-04-03 T6).
 * Hits POST /api/v1/rag with REST Assured; RagService is mocked so no LLM is required.
 * Covers streaming (SSE), non-streaming (JSON), error handling, and source attribution.
 */
@QuarkusTest
class RagResourceIT {

    private static final String RAG_PATH = "/api/v1/rag";
    private static final String RAG_STREAM_PATH = "/api/v1/rag/stream";

    @InjectMock
    RagService ragService;

    @BeforeEach
    void setUp() {
        // Default: empty stream so streaming tests can override
        when(ragService.streamTokens(anyString())).thenReturn(Multi.createFrom().empty());
        when(ragService.ask(anyString())).thenReturn(Uni.createFrom().item(RagResponse.of("")));
    }

    @Test
    @DisplayName("POST with valid body and stream=true (default) returns 200 and SSE token events")
    void post_rag_defaultStream_returns200AndSseEvents() {
        when(ragService.streamTokens(anyString())).thenReturn(Multi.createFrom().items(
                new TokenStreamEvent("Hello"),
                new TokenStreamEvent(" world")
        ));

        String body = given()
                .contentType(ContentType.JSON)
                .accept(MediaType.SERVER_SENT_EVENTS)
                .body("{\"question\":\"Say hello\"}")
                .when()
                .post(RAG_PATH)
                .then()
                .statusCode(200)
                .contentType(MediaType.SERVER_SENT_EVENTS)
                .extract().body().asString();

        org.assertj.core.api.Assertions.assertThat(body)
                .contains("event: token")
                .contains("data:")
                .contains("Hello")
                .contains("world");
    }

    @Test
    @DisplayName("POST /rag/stream with valid body returns 200 and SSE token events")
    void post_rag_streamPath_returns200AndSseEvents() {
        when(ragService.streamTokens(anyString())).thenReturn(Multi.createFrom().items(
                new TokenStreamEvent("Hello"),
                new TokenStreamEvent(" world")
        ));

        String body = given()
                .contentType(ContentType.JSON)
                .accept(MediaType.SERVER_SENT_EVENTS)
                .body("{\"question\":\"Say hello\"}")
                .when()
                .post(RAG_STREAM_PATH)
                .then()
                .statusCode(200)
                .contentType(MediaType.SERVER_SENT_EVENTS)
                .extract().body().asString();

        org.assertj.core.api.Assertions.assertThat(body)
                .contains("event: token")
                .contains("data:")
                .contains("Hello")
                .contains("world");
    }

    @Test
    @DisplayName("POST with stream=false returns 200 and complete JSON RagResponse (main /rag endpoint)")
    void post_rag_nonStreaming_returns200AndJsonResponse() {
        when(ragService.ask(anyString())).thenReturn(Uni.createFrom().item(
                RagResponse.of("The answer is 42.", List.of("src/Answer.java"), "ollama/llama2")
        ));

        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body("{\"question\":\"What is the answer?\"}")
                .queryParam("stream", false)
                .when()
                .post(RAG_PATH)
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("answer", equalTo("The answer is 42."))
                .body("sources", notNullValue())
                .body("sources.size()", equalTo(1))
                .body("sources[0]", equalTo("src/Answer.java"))
                .body("model_used", equalTo("ollama/llama2"));
    }

    @Test
    @DisplayName("Non-streaming response includes source_metadata when provided by service")
    void post_rag_nonStreaming_withSourceMetadata_returnsSourceAttribution() {
        List<SourceDTO> sourceMetadata = List.of(
                new SourceDTO("src/auth/AuthService.java", "AuthService", 10, 25, 0.95f, "my-repo", "java", "chunk-0")
        );
        RagResponse response = new RagResponse(
                "Auth is handled in AuthService.",
                List.of("src/auth/AuthService.java"),
                sourceMetadata,
                "ollama/codellama"
        );
        when(ragService.ask(anyString())).thenReturn(Uni.createFrom().item(response));

        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body("{\"question\":\"Where is auth implemented?\"}")
                .queryParam("stream", false)
                .when()
                .post(RAG_PATH)
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("answer", equalTo("Auth is handled in AuthService."))
                .body("sources", notNullValue())
                .body("source_metadata", notNullValue())
                .body("source_metadata.size()", equalTo(1))
                .body("source_metadata[0].file_path", equalTo("src/auth/AuthService.java"))
                .body("source_metadata[0].entity_name", equalTo("AuthService"))
                .body("source_metadata[0].line_start", equalTo(10))
                .body("source_metadata[0].line_end", equalTo(25))
                .body("source_metadata[0].relevance_score", equalTo(0.95f))
                .body("source_metadata[0].repository", equalTo("my-repo"))
                .body("source_metadata[0].language", equalTo("java"))
                .body("source_metadata[0].chunk_id", equalTo("chunk-0"));
    }

    @Test
    @DisplayName("Empty question returns 400 Bad Request")
    void post_rag_emptyQuestion_returns400() {
        given()
                .contentType(ContentType.JSON)
                .body("{\"question\":\"\"}")
                .when()
                .post(RAG_PATH)
                .then()
                .statusCode(400);
    }

    @Test
    @DisplayName("Missing question returns 400 Bad Request")
    void post_rag_missingQuestion_returns400() {
        given()
                .contentType(ContentType.JSON)
                .body("{}")
                .when()
                .post(RAG_PATH)
                .then()
                .statusCode(400)
                .body(containsString("question"));
    }

    @Test
    @DisplayName("Streaming when service fails returns 200 with error SSE event")
    void post_rag_streaming_serviceFails_returnsErrorSseEvent() {
        when(ragService.streamTokens(anyString())).thenReturn(
                Multi.createFrom().failure(new RuntimeException("LLM unavailable"))
        );

        String body = given()
                .contentType(ContentType.JSON)
                .accept(MediaType.SERVER_SENT_EVENTS)
                .body("{\"question\":\"Fail\"}")
                .when()
                .post(RAG_STREAM_PATH)
                .then()
                .statusCode(200)
                .contentType(MediaType.SERVER_SENT_EVENTS)
                .extract().body().asString();

        org.assertj.core.api.Assertions.assertThat(body)
                .contains("event: error")
                .contains("data:")
                .contains("LLM unavailable")
                .contains("STREAM_ERROR");
    }

    @Test
    @DisplayName("Non-streaming when service fails returns 503 with error body")
    void post_rag_nonStreaming_serviceFails_returns503() {
        when(ragService.ask(anyString())).thenReturn(
                Uni.createFrom().failure(new RuntimeException("RAG request failed"))
        );

        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body("{\"question\":\"Fail\"}")
                .queryParam("stream", false)
                .when()
                .post(RAG_PATH)
                .then()
                .statusCode(503)
                .contentType(ContentType.JSON)
                .body("error", containsString("RAG request failed"));
    }

    @Test
    @DisplayName("Streaming with ErrorStreamEvent returns event: error in SSE")
    void post_rag_streaming_errorEvent_returnsErrorSse() {
        when(ragService.streamTokens(anyString())).thenReturn(Multi.createFrom().items(
                new ErrorStreamEvent("Model overloaded", "LLM_ERROR")
        ));

        String body = given()
                .contentType(ContentType.JSON)
                .accept(MediaType.SERVER_SENT_EVENTS)
                .body("{\"question\":\"Hi\"}")
                .when()
                .post(RAG_STREAM_PATH)
                .then()
                .statusCode(200)
                .contentType(MediaType.SERVER_SENT_EVENTS)
                .extract().body().asString();

        org.assertj.core.api.Assertions.assertThat(body)
                .contains("event: error")
                .contains("Model overloaded")
                .contains("LLM_ERROR");
    }
}
