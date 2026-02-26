/*
 * Copyright (c) 2025 MegaBrain Contributors
 * Licensed under the MIT License - see LICENSE file for details.
 */

package io.megabrain.core;

import java.util.List;

/**
 * Records and retrieves LLM usage for cost tracking and reporting (US-03-02 T6).
 */
public interface LLMUsageRecorder {

    /**
     * Records one usage event (e.g. after a successful chat call).
     *
     * @param record usage record to store
     */
    void record(LLMUsageRecord record);

    /**
     * Returns recent usage records for reporting (newest first).
     * Implementation may limit count or time window.
     *
     * @param limit maximum number of records to return (e.g. 1000)
     * @return list of usage records, never null
     */
    List<LLMUsageRecord> getRecent(int limit);

    /**
     * Returns total estimated cost from all stored records.
     *
     * @return total cost in USD
     */
    double getTotalCostEstimate();
}
