/*
 * Copyright (c) 2025 MegaBrain Contributors
 * Licensed under the MIT License - see LICENSE file for details.
 */

package io.megabrain.core;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for NoOpLLMUsageRecorder (US-03-02 T6).
 */
class NoOpLLMUsageRecorderTest {

    @Test
    @DisplayName("record does nothing")
    void record_doesNothing() {
        NoOpLLMUsageRecorder recorder = new NoOpLLMUsageRecorder();
        recorder.record(new LLMUsageRecord("openai", "gpt-4", 1, 1, 0.001, Instant.now()));
        assertThat(recorder.getRecent(10)).isEmpty();
        assertThat(recorder.getTotalCostEstimate()).isZero();
    }

    @Test
    @DisplayName("getRecent returns empty list")
    void getRecent_returnsEmpty() {
        NoOpLLMUsageRecorder recorder = new NoOpLLMUsageRecorder();
        List<LLMUsageRecord> recent = recorder.getRecent(100);
        assertThat(recent).isEmpty();
    }

    @Test
    @DisplayName("getTotalCostEstimate returns 0")
    void getTotalCostEstimate_returnsZero() {
        NoOpLLMUsageRecorder recorder = new NoOpLLMUsageRecorder();
        assertThat(recorder.getTotalCostEstimate()).isZero();
    }
}
