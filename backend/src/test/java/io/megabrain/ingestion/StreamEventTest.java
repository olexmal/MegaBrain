/*
 * Copyright (c) 2025 MegaBrain Contributors
 * Licensed under the MIT License - see LICENSE file for details.
 */

package io.megabrain.ingestion;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for the StreamEvent record.
 */
@QuarkusTest
class StreamEventTest {

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Test
    void streamEvent_shouldCreateWithRequiredFields() {
        // Given
        StreamEvent.Stage stage = StreamEvent.Stage.CLONING;
        String message = "Starting clone";
        int percentage = 0;

        // When
        StreamEvent event = StreamEvent.of(stage, message, percentage);

        // Then
        assertThat(event.stage()).isEqualTo(stage);
        assertThat(event.message()).isEqualTo(message);
        assertThat(event.percentage()).isEqualTo(percentage);
        assertThat(event.timestamp()).isNotNull();
        assertThat(event.metadata()).isEmpty();
    }

    @Test
    void streamEvent_shouldCreateWithMetadata() {
        // Given
        StreamEvent.Stage stage = StreamEvent.Stage.PARSING;
        String message = "Parsing files";
        int percentage = 50;
        Map<String, Object> metadata = Map.of("filesProcessed", 25, "totalFiles", 50);

        // When
        StreamEvent event = StreamEvent.of(stage, message, percentage, metadata);

        // Then
        assertThat(event.stage()).isEqualTo(stage);
        assertThat(event.message()).isEqualTo(message);
        assertThat(event.percentage()).isEqualTo(percentage);
        assertThat(event.metadata()).isEqualTo(metadata);
        assertThat(event.getMetadata("filesProcessed")).contains(25);
        assertThat(event.getMetadata("totalFiles")).contains(50);
    }

    @Test
    void streamEvent_shouldCreateWithAllFields() {
        // Given
        StreamEvent.Stage stage = StreamEvent.Stage.COMPLETE;
        String message = "Ingestion completed";
        int percentage = 100;
        Instant timestamp = Instant.parse("2023-01-01T10:00:00Z");
        Map<String, Object> metadata = Map.of("totalChunks", 1500);

        // When
        StreamEvent event = StreamEvent.of(stage, message, percentage, timestamp, metadata);

        // Then
        assertThat(event.stage()).isEqualTo(stage);
        assertThat(event.message()).isEqualTo(message);
        assertThat(event.percentage()).isEqualTo(percentage);
        assertThat(event.timestamp()).isEqualTo(timestamp);
        assertThat(event.metadata()).isEqualTo(metadata);
    }

    @Test
    void streamEvent_shouldHandleNullMetadata() {
        // When
        StreamEvent event = StreamEvent.of(StreamEvent.Stage.CLONING, "test", 0, null);

        // Then
        assertThat(event.metadata()).isEmpty();
    }

    @Test
    void streamEvent_shouldIdentifyTerminalEvents() {
        // Given
        StreamEvent completeEvent = StreamEvent.of(StreamEvent.Stage.COMPLETE, "Done", 100);
        StreamEvent failedEvent = StreamEvent.of(StreamEvent.Stage.FAILED, "Error", 0);
        StreamEvent cloningEvent = StreamEvent.of(StreamEvent.Stage.CLONING, "Cloning", 50);

        // Then
        assertThat(completeEvent.isTerminal()).isTrue();
        assertThat(failedEvent.isTerminal()).isTrue();
        assertThat(cloningEvent.isTerminal()).isFalse();
    }

    @Test
    void streamEvent_shouldCreateModifiedCopies() {
        // Given
        StreamEvent original = StreamEvent.of(StreamEvent.Stage.INDEXING, "Original", 25);

        // When
        StreamEvent withNewPercentage = original.withPercentage(75);
        StreamEvent withNewMessage = original.withMessage("Updated message");
        StreamEvent withMetadata = original.withMetadata("chunks", 100);

        // Then
        assertThat(withNewPercentage.percentage()).isEqualTo(75);
        assertThat(withNewPercentage.message()).isEqualTo("Original");

        assertThat(withNewMessage.message()).isEqualTo("Updated message");
        assertThat(withNewMessage.percentage()).isEqualTo(25);

        assertThat(withMetadata.getMetadata("chunks")).contains(100);
        assertThat(withMetadata.metadata()).hasSize(1);
    }

    @Test
    void streamEvent_shouldSerializeToJson() throws Exception {
        // Given
        StreamEvent event = StreamEvent.of(StreamEvent.Stage.CLONING, "Starting clone", 0);

        // When
        String json = objectMapper.writeValueAsString(event);

        // Then
        assertThat(json).contains("\"stage\":\"CLONING\"").contains("\"message\":\"Starting clone\"").contains("\"percentage\":0").contains("\"timestamp\"").contains("\"metadata\":{}");
    }

    @Test
    void streamEvent_shouldDeserializeFromJson() throws Exception {
        // Given
        String json = """
                {
                    "stage": "PARSING",
                    "message": "Parsing files",
                    "percentage": 75,
                    "timestamp": "2023-01-01T10:00:00Z",
                    "metadata": {"files": 10}
                }
                """;

        // When
        StreamEvent event = objectMapper.readValue(json, StreamEvent.class);

        // Then
        assertThat(event.stage()).isEqualTo(StreamEvent.Stage.PARSING);
        assertThat(event.message()).isEqualTo("Parsing files");
        assertThat(event.percentage()).isEqualTo(75);
        assertThat(event.metadata()).containsEntry("files", 10);
    }

    @Test
    void streamEvent_enumValues_shouldBeDefined() {
        // Verify all expected enum values exist
        assertThat(StreamEvent.Stage.values()).containsExactlyInAnyOrder(
                StreamEvent.Stage.CLONING,
                StreamEvent.Stage.PARSING,
                StreamEvent.Stage.INDEXING,
                StreamEvent.Stage.COMPLETE,
                StreamEvent.Stage.FAILED
        );
    }
}
