/*
 * Copyright (c) 2025 MegaBrain Contributors
 * Licensed under the MIT License - see LICENSE file for details.
 */

package io.megabrain.core;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for InMemoryLLMUsageRecorder (US-03-02 T6).
 */
class InMemoryLLMUsageRecorderTest {

    private InMemoryLLMUsageRecorder recorder;

    @BeforeEach
    void setUp() {
        recorder = new InMemoryLLMUsageRecorder(100);
    }

    @Test
    @DisplayName("record stores record and getRecent returns it")
    void record_thenGetRecent_returnsRecord() {
        LLMUsageRecord r = new LLMUsageRecord("openai", "gpt-4", 10, 20, 0.001, Instant.now());
        recorder.record(r);
        List<LLMUsageRecord> recent = recorder.getRecent(10);
        assertThat(recent).hasSize(1);
        assertThat(recent.get(0).provider()).isEqualTo("openai");
        assertThat(recent.get(0).model()).isEqualTo("gpt-4");
        assertThat(recent.get(0).inputTokens()).isEqualTo(10);
        assertThat(recent.get(0).outputTokens()).isEqualTo(20);
        assertThat(recent.get(0).costEstimate()).isEqualTo(0.001);
    }

    @Test
    @DisplayName("getRecent returns newest first")
    void getRecent_returnsNewestFirst() {
        recorder.record(new LLMUsageRecord("openai", "gpt-4", 1, 1, 0.001, Instant.now()));
        recorder.record(new LLMUsageRecord("anthropic", "claude", 2, 2, 0.002, Instant.now()));
        List<LLMUsageRecord> recent = recorder.getRecent(10);
        assertThat(recent).hasSize(2);
        assertThat(recent.get(0).provider()).isEqualTo("anthropic");
        assertThat(recent.get(1).provider()).isEqualTo("openai");
    }

    @Test
    @DisplayName("getRecent limit truncates")
    void getRecent_limitTruncates() {
        recorder.record(new LLMUsageRecord("openai", "gpt-4", 1, 1, 0.001, Instant.now()));
        recorder.record(new LLMUsageRecord("openai", "gpt-4", 2, 2, 0.002, Instant.now()));
        recorder.record(new LLMUsageRecord("openai", "gpt-4", 3, 3, 0.003, Instant.now()));
        List<LLMUsageRecord> recent = recorder.getRecent(2);
        assertThat(recent).hasSize(2);
    }

    @Test
    @DisplayName("getTotalCostEstimate sums cost")
    void getTotalCostEstimate_sumsCost() {
        recorder.record(new LLMUsageRecord("openai", "gpt-4", 1, 1, 0.001, Instant.now()));
        recorder.record(new LLMUsageRecord("openai", "gpt-4", 1, 1, 0.002, Instant.now()));
        assertThat(recorder.getTotalCostEstimate()).isEqualTo(0.003);
    }

    @Test
    @DisplayName("record null is no-op")
    void record_null_isNoOp() {
        recorder.record(null);
        assertThat(recorder.getRecent(10)).isEmpty();
        assertThat(recorder.getTotalCostEstimate()).isZero();
    }

    @Test
    @DisplayName("bounded capacity drops oldest")
    void record_overCapacity_dropsOldest() {
        InMemoryLLMUsageRecorder small = new InMemoryLLMUsageRecorder(2);
        small.record(new LLMUsageRecord("a", "m", 1, 1, 0.001, Instant.now()));
        small.record(new LLMUsageRecord("b", "m", 1, 1, 0.002, Instant.now()));
        small.record(new LLMUsageRecord("c", "m", 1, 1, 0.003, Instant.now()));
        List<LLMUsageRecord> recent = small.getRecent(10);
        assertThat(recent).hasSize(2);
        assertThat(recent.get(0).provider()).isEqualTo("c");
        assertThat(recent.get(1).provider()).isEqualTo("b");
    }
}
