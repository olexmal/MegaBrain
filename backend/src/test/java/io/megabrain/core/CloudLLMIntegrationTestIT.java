/*
 * Copyright (c) 2025 MegaBrain Contributors
 * Licensed under the MIT License - see LICENSE file for details.
 */

package io.megabrain.core;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowable;

/**
 * Integration tests for OpenAI and Anthropic LLM clients with WireMock (US-03-02 T7).
 * Tests success responses, usage logging, rate limiting (429), retry on 5xx, and API key header.
 */
@QuarkusTest
@QuarkusTestResource(WireMockLLMTestResource.class)
class CloudLLMIntegrationTestIT {

    private static final Duration AWAIT = Duration.ofSeconds(30);

    @Inject
    OpenAILLMClient openAIClient;

    @Inject
    AnthropicLLMClient anthropicClient;

    @Inject
    LLMUsageRecorder usageRecorder;

    @BeforeEach
    void configureWireMock() {
        int port = WireMockLLMTestResource.getPort();
        if (port > 0) {
            configureFor("localhost", port);
            reset();
        }
    }

    @Test
    @DisplayName("OpenAI success returns content and records usage")
    void openAI_success_recordsUsage() {
        stubFor(post(urlPathEqualTo("/v1/chat/completions"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"choices\":[{\"message\":{\"content\":\"Mocked OpenAI reply\"}}]}")));

        String response = openAIClient.generate("Hello").await().atMost(AWAIT);

        assertThat(response).isNotBlank();
        assertThat(response).contains("Mocked OpenAI reply");

        List<LLMUsageRecord> recent = usageRecorder.getRecent(10);
        assertThat(recent).isNotEmpty();
        assertThat(recent.stream().anyMatch(r -> "openai".equals(r.provider()) && r.inputTokens() >= 0)).isTrue();
    }

    @Test
    @DisplayName("Anthropic success returns content and records usage")
    void anthropic_success_recordsUsage() {
        stubFor(post(urlPathEqualTo("/v1/messages"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"content\":[{\"type\":\"text\",\"text\":\"Mocked Anthropic reply\"}]}")));

        String response = anthropicClient.generate("Hi").await().atMost(AWAIT);

        assertThat(response).isNotBlank();
        assertThat(response).contains("Mocked Anthropic reply");

        List<LLMUsageRecord> recent = usageRecorder.getRecent(10);
        assertThat(recent).isNotEmpty();
        assertThat(recent.stream().anyMatch(r -> "anthropic".equals(r.provider()) && r.inputTokens() >= 0)).isTrue();
    }

    @Test
    @DisplayName("OpenAI 429 then 200 succeeds after retry")
    void openAI_rateLimit429_thenSuccess() {
        stubFor(post(urlPathEqualTo("/v1/chat/completions"))
                .inScenario("rate-limit")
                .whenScenarioStateIs(STARTED)
                .willReturn(aResponse().withStatus(429).withBody("rate limit"))
                .willSetStateTo("ok"));
        stubFor(post(urlPathEqualTo("/v1/chat/completions"))
                .inScenario("rate-limit")
                .whenScenarioStateIs("ok")
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"choices\":[{\"message\":{\"content\":\"After retry\"}}]}")));

        String response = openAIClient.generate("Hi").await().atMost(AWAIT);

        assertThat(response).contains("After retry");
    }

    @Test
    @DisplayName("OpenAI 429 always fails with rate limit message after max retries")
    void openAI_rateLimit429_exhaustRetries_throws() {
        stubFor(post(urlPathEqualTo("/v1/chat/completions"))
                .willReturn(aResponse().withStatus(429).withBody("Rate limit exceeded")));

        Throwable thrown = catchThrowable(() -> openAIClient.generate("Hi").await().atMost(AWAIT));
        assertThat(thrown).isNotNull();
        String message = thrown.getMessage();
        Throwable cause = thrown.getCause();
        boolean hasRateLimit = (message != null && message.contains("Rate limit"))
                || (cause != null && cause.getMessage() != null && cause.getMessage().contains("Rate limit"));
        assertThat(hasRateLimit).as("Exception or cause should mention rate limit").isTrue();
    }

    @Test
    @DisplayName("OpenAI 503 then 200 succeeds after retry")
    void openAI_5xx_thenSuccess() {
        stubFor(post(urlPathEqualTo("/v1/chat/completions"))
                .inScenario("5xx")
                .whenScenarioStateIs(STARTED)
                .willReturn(aResponse().withStatus(503).withBody("Service Unavailable"))
                .willSetStateTo("ok"));
        stubFor(post(urlPathEqualTo("/v1/chat/completions"))
                .inScenario("5xx")
                .whenScenarioStateIs("ok")
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"choices\":[{\"message\":{\"content\":\"After 5xx retry\"}}]}")));

        String response = openAIClient.generate("Hi").await().atMost(AWAIT);

        assertThat(response).contains("After 5xx retry");
    }

    @Test
    @DisplayName("OpenAI request sends API key in Authorization header")
    void openAI_apiKey_passedInHeader() {
        stubFor(post(urlPathEqualTo("/v1/chat/completions"))
                .withHeader("Authorization", containing("Bearer"))
                .withHeader("Authorization", containing("sk-"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"choices\":[{\"message\":{\"content\":\"OK\"}}]}")));

        String response = openAIClient.generate("Hello").await().atMost(AWAIT);

        assertThat(response).contains("OK");
        verify(postRequestedFor(urlPathEqualTo("/v1/chat/completions"))
                .withHeader("Authorization", containing("sk-")));
    }

    @Test
    @DisplayName("Anthropic request sends API key in header")
    void anthropic_apiKey_passedInHeader() {
        stubFor(post(urlPathEqualTo("/v1/messages"))
                .withHeader("x-api-key", containing("sk-ant-"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"content\":[{\"type\":\"text\",\"text\":\"OK\"}]}")));

        String response = anthropicClient.generate("Hello").await().atMost(AWAIT);

        assertThat(response).contains("OK");
        verify(postRequestedFor(urlPathEqualTo("/v1/messages"))
                .withHeader("x-api-key", containing("sk-ant-")));
    }
}
