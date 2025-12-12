/*
 * Copyright (c) 2025 MegaBrain Contributors
 * Licensed under the MIT License - see LICENSE file for details.
 */

package io.megabrain.api;

import io.quarkus.test.junit.QuarkusIntegrationTest;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test for SSE streaming endpoint.
 * Tests that the SSE endpoint is accessible and returns properly formatted events.
 */
@QuarkusIntegrationTest
class IngestionResourceSseIT {

    private static final String BASE_URL = "http://localhost:8081";

    @Test
    void sseEndpoint_shouldBeAccessibleAndReturnSSEFormat() throws Exception {
        // Given - test repository URL
        String testUrl = "https://github.com/test/repo";

        // When - make SSE request
        List<String> sseLines = collectSseLines(testUrl, false);

        // Then - verify SSE format
        assertThat(sseLines).isNotEmpty();
        assertThat(sseLines.get(0)).startsWith("data: ");

        // Verify SSE format: lines should start with "data: " and end with empty lines
        boolean foundDataLine = false;
        boolean foundEmptyLine = false;

        for (String line : sseLines) {
            if (line.startsWith("data: ")) {
                foundDataLine = true;
                // Verify it contains JSON
                assertThat(line.substring(6)).startsWith("{");
            } else if (line.isEmpty()) {
                foundEmptyLine = true;
            }
        }

        assertThat(foundDataLine).isTrue();
        assertThat(foundEmptyLine).isTrue();
    }

    @Test
    void sseEndpoint_shouldAcceptIncrementalParameter() throws Exception {
        // Given - test repository URL with incremental=true
        String testUrl = "https://github.com/test/repo";

        // When - make incremental SSE request
        List<String> sseLines = collectSseLines(testUrl, true);

        // Then - verify response format
        assertThat(sseLines).isNotEmpty();
        assertThat(sseLines.stream().anyMatch(line -> line.startsWith("data: "))).isTrue();
    }

    @Test
    void sseEndpoint_shouldHandleInvalidRepositoryUrlGracefully() throws Exception {
        // Given - invalid repository URL
        String invalidUrl = "not-a-valid-url";

        // When - make SSE request with invalid URL
        List<String> sseLines = collectSseLines(invalidUrl, false);

        // Then - should still return SSE format (even if it contains error events)
        assertThat(sseLines).isNotEmpty();
        // Should have at least one data line
        assertThat(sseLines.stream().anyMatch(line -> line.startsWith("data: "))).isTrue();
    }

    /**
     * Collects raw SSE lines from the streaming endpoint.
     */
    private List<String> collectSseLines(String repositoryUrl, boolean incremental) throws Exception {
        List<String> lines = new ArrayList<>();

        // Create HTTP connection to SSE endpoint
        URL url = new URL(BASE_URL + "/api/v1/ingestion/repositories/stream");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestProperty("Accept", "text/event-stream");
        connection.setDoOutput(true);

        // Send request body
        String requestBody = String.format("{\"repositoryUrl\":\"%s\",\"incremental\":%b}",
            repositoryUrl, incremental);
        connection.getOutputStream().write(requestBody.getBytes());

        // Read SSE stream with timeout
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(connection.getInputStream()))) {

            String line;
            int lineCount = 0;

            // Read up to 50 lines or until we get a complete event
            while ((line = reader.readLine()) != null && lineCount < 50) {
                lines.add(line);
                lineCount++;

                // Stop after we get a complete SSE event (empty line after data)
                if (lines.size() >= 2 &&
                    lines.get(lines.size() - 1).isEmpty() &&
                    lines.get(lines.size() - 2).startsWith("data: ")) {
                    break;
                }
            }
        } catch (IOException e) {
            // If we get an error, that's still valid for testing error handling
            // Just return what we got
        }

        return lines;
    }
}
