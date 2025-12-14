/*
 * Copyright (c) 2025 MegaBrain Contributors
 * Licensed under the MIT License - see LICENSE file for details.
 */

package io.megabrain.ingestion;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Stream event emitted during repository ingestion operations.
 * Represents progress events that are streamed to clients via Server-Sent Events (SSE).
 */
public record StreamEvent(
        @JsonProperty("stage") Stage stage,
        @JsonProperty("message") String message,
        @JsonProperty("percentage") int percentage,
        @JsonProperty("timestamp") Instant timestamp,
        @JsonProperty("metadata") Map<String, Object> metadata
) {

    /**
     * Stages of the ingestion process.
     */
    public enum Stage {
        CLONING,
        PARSING,
        INDEXING,
        COMPLETE,
        FAILED
    }

    /**
     * Creates a StreamEvent with the specified stage, message, and percentage.
     * Timestamp is set to current time, metadata is empty.
     */
    public static StreamEvent of(Stage stage, String message, int percentage) {
        return new StreamEvent(stage, message, percentage, Instant.now(), Map.of());
    }

    /**
     * Creates a StreamEvent with the specified stage, message, percentage, and metadata.
     * Timestamp is set to current time.
     */
    public static StreamEvent of(Stage stage, String message, int percentage, Map<String, Object> metadata) {
        return new StreamEvent(stage, message, percentage, Instant.now(), metadata != null ? metadata : Map.of());
    }

    /**
     * Creates a StreamEvent with all fields specified.
     */
    public static StreamEvent of(Stage stage, String message, int percentage, Instant timestamp, Map<String, Object> metadata) {
        return new StreamEvent(stage, message, percentage, timestamp, metadata != null ? metadata : Map.of());
    }

    /**
     * Gets metadata value by key, returning empty Optional if key not present.
     */
    public Optional<Object> getMetadata(String key) {
        return Optional.ofNullable(metadata.get(key));
    }

    /**
     * Checks if this event represents completion (either success or failure).
     */
    public boolean isTerminal() {
        return stage == Stage.COMPLETE || stage == Stage.FAILED;
    }

    /**
     * Creates a copy of this event with updated percentage.
     */
    public StreamEvent withPercentage(int newPercentage) {
        return new StreamEvent(stage, message, newPercentage, timestamp, metadata);
    }

    /**
     * Creates a copy of this event with updated message.
     */
    public StreamEvent withMessage(String newMessage) {
        return new StreamEvent(stage, newMessage, percentage, timestamp, metadata);
    }

    /**
     * Creates a copy of this event with additional metadata.
     */
    public StreamEvent withMetadata(String key, Object value) {
        Map<String, Object> newMetadata = Map.copyOf(metadata);
        Map<String, Object> updatedMetadata = new HashMap<>(newMetadata);
        updatedMetadata.put(key, value);
        return new StreamEvent(stage, message, percentage, timestamp, Map.copyOf(updatedMetadata));
    }
}
