/*
 * Copyright (c) 2025 MegaBrain Contributors
 * Licensed under the MIT License - see LICENSE file for details.
 */

package io.megabrain.core;

import com.github.tomakehurst.wiremock.WireMockServer;
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;

import java.util.Map;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;

/**
 * Quarkus test resource that starts a WireMock server for mocking OpenAI and Anthropic HTTP APIs.
 * Used by cloud LLM integration tests (US-03-02 T7).
 * Exposes base URL and API keys via config overrides so clients use the mock server.
 */
public class WireMockLLMTestResource implements QuarkusTestResourceLifecycleManager {

    private static final String OPENAI_API_KEY = "sk-test-openai-key-for-wiremock";
    private static final String ANTHROPIC_API_KEY = "sk-ant-test-anthropic-key-for-wiremock";

    static WireMockServer wireMockServer;

    @Override
    public Map<String, String> start() {
        wireMockServer = new WireMockServer(wireMockConfig().dynamicPort());
        wireMockServer.start();
        int port = wireMockServer.port();
        String baseUrl = "http://localhost:" + port + "/v1";

        return Map.ofEntries(
                Map.entry("megabrain.llm.openai.api-key", OPENAI_API_KEY),
                Map.entry("megabrain.llm.openai.base-url", baseUrl),
                Map.entry("megabrain.llm.openai.model", "gpt-4"),
                Map.entry("megabrain.llm.openai.max-retries", "2"),
                Map.entry("megabrain.llm.openai.base-delay-ms", "10"),
                Map.entry("megabrain.llm.anthropic.api-key", ANTHROPIC_API_KEY),
                Map.entry("megabrain.llm.anthropic.base-url", baseUrl),
                Map.entry("megabrain.llm.anthropic.model", "claude-3-5-sonnet-20241022"),
                Map.entry("megabrain.llm.anthropic.max-retries", "2"),
                Map.entry("megabrain.llm.anthropic.base-delay-ms", "10")
        );
    }

    @Override
    public void stop() {
        if (wireMockServer != null) {
            wireMockServer.stop();
            wireMockServer = null;
        }
    }

    /**
     * Returns the WireMock server port so tests can configure stubs.
     */
    static int getPort() {
        return wireMockServer != null ? wireMockServer.port() : -1;
    }
}
