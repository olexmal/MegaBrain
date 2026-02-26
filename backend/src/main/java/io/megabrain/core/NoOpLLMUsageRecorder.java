/*
 * Copyright (c) 2025 MegaBrain Contributors
 * Licensed under the MIT License - see LICENSE file for details.
 */

package io.megabrain.core;

import java.util.Collections;
import java.util.List;

/**
 * No-op implementation of LLMUsageRecorder for when recording is disabled or not injected (US-03-02 T6).
 */
public class NoOpLLMUsageRecorder implements LLMUsageRecorder {

    @Override
    public void record(LLMUsageRecord record) {
        // no-op
    }

    @Override
    public List<LLMUsageRecord> getRecent(int limit) {
        return Collections.emptyList();
    }

    @Override
    public double getTotalCostEstimate() {
        return 0.0;
    }
}
