/*
 * Copyright (c) 2025 MegaBrain Contributors
 * Licensed under the MIT License - see LICENSE file for details.
 */

package io.megabrain.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for RagRequest DTO (US-04-03 T3).
 * Covers optional fields (context_limit, model), validation annotations, and JSON serialization.
 */
class RagRequestTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    @DisplayName("constructor with question only leaves optional fields null")
    void constructor_questionOnly_setsQuestionOnly() {
        RagRequest request = new RagRequest("How does auth work?");
        assertThat(request.getQuestion()).isEqualTo("How does auth work?");
        assertThat(request.getContextLimit()).isNull();
        assertThat(request.getModel()).isNull();
    }

    @Test
    @DisplayName("constructor with all args sets question, context_limit, model")
    void constructor_allArgs_setsAllFields() {
        RagRequest request = new RagRequest("Explain caching", 10, "llama3");
        assertThat(request.getQuestion()).isEqualTo("Explain caching");
        assertThat(request.getContextLimit()).isEqualTo(10);
        assertThat(request.getModel()).isEqualTo("llama3");
    }

    @Test
    @DisplayName("setters round-trip optional fields")
    void setters_roundTripOptionalFields() {
        RagRequest request = new RagRequest("q");
        request.setContextLimit(5);
        request.setModel("codellama");
        assertThat(request.getContextLimit()).isEqualTo(5);
        assertThat(request.getModel()).isEqualTo("codellama");
    }

    @Test
    @DisplayName("deserialize JSON with question only")
    void deserialize_questionOnly_createsRequest() throws Exception {
        String json = "{\"question\": \"What is RAG?\"}";
        RagRequest request = mapper.readValue(json, RagRequest.class);
        assertThat(request.getQuestion()).isEqualTo("What is RAG?");
        assertThat(request.getContextLimit()).isNull();
        assertThat(request.getModel()).isNull();
    }

    @Test
    @DisplayName("deserialize JSON with context_limit and model")
    void deserialize_withContextLimitAndModel_createsRequest() throws Exception {
        String json = """
                {"question": "Explain it", "context_limit": 20, "model": "mistral"}
                """;
        RagRequest request = mapper.readValue(json, RagRequest.class);
        assertThat(request.getQuestion()).isEqualTo("Explain it");
        assertThat(request.getContextLimit()).isEqualTo(20);
        assertThat(request.getModel()).isEqualTo("mistral");
    }

    @Test
    @DisplayName("serialize to JSON includes context_limit key")
    void serialize_withContextLimit_includesContextLimitKey() throws Exception {
        RagRequest request = new RagRequest("Hi", 15, "phi");
        String json = mapper.writeValueAsString(request);
        assertThat(json).contains("\"question\":\"Hi\"");
        assertThat(json).contains("\"context_limit\":15");
        assertThat(json).contains("\"model\":\"phi\"");
    }

    @Test
    @DisplayName("round-trip JSON preserves optional fields")
    void roundTrip_preservesOptionalFields() throws Exception {
        RagRequest original = new RagRequest("Round trip", 8, "codellama");
        String json = mapper.writeValueAsString(original);
        RagRequest deserialized = mapper.readValue(json, RagRequest.class);
        assertThat(deserialized.getQuestion()).isEqualTo(original.getQuestion());
        assertThat(deserialized.getContextLimit()).isEqualTo(original.getContextLimit());
        assertThat(deserialized.getModel()).isEqualTo(original.getModel());
    }
}
