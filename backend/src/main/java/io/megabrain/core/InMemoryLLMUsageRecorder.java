/*
 * Copyright (c) 2025 MegaBrain Contributors
 * Licensed under the MIT License - see LICENSE file for details.
 */

package io.megabrain.core;

import jakarta.enterprise.context.ApplicationScoped;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * In-memory implementation of LLMUsageRecorder for usage metrics and cost reporting (US-03-02 T6).
 * Keeps a bounded list of recent records; oldest are dropped when capacity is exceeded.
 */
@ApplicationScoped
public class InMemoryLLMUsageRecorder implements LLMUsageRecorder {

    /** Default max records to retain. */
    public static final int DEFAULT_MAX_RECORDS = 10_000;

    private final int maxRecords;
    private final List<LLMUsageRecord> records = new CopyOnWriteArrayList<>();

    public InMemoryLLMUsageRecorder() {
        this(DEFAULT_MAX_RECORDS);
    }

    /**
     * @param maxRecords maximum number of records to retain (oldest dropped when exceeded)
     */
    public InMemoryLLMUsageRecorder(int maxRecords) {
        this.maxRecords = Math.max(1, maxRecords);
    }

    @Override
    public void record(LLMUsageRecord record) {
        if (record == null) return;
        synchronized (records) {
            records.add(record);
            while (records.size() > maxRecords) {
                records.remove(0);
            }
        }
    }

    @Override
    public List<LLMUsageRecord> getRecent(int limit) {
        int n = Math.max(0, limit);
        List<LLMUsageRecord> copy;
        synchronized (records) {
            int size = records.size();
            int from = Math.max(0, size - n);
            copy = new ArrayList<>(records.subList(from, size));
        }
        // newest last in list; reverse so newest first
        List<LLMUsageRecord> result = new ArrayList<>(copy);
        java.util.Collections.reverse(result);
        return result;
    }

    @Override
    public double getTotalCostEstimate() {
        synchronized (records) {
            return records.stream()
                    .mapToDouble(LLMUsageRecord::costEstimate)
                    .sum();
        }
    }
}
