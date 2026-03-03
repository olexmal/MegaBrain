/*
 * Copyright (c) 2025 MegaBrain Contributors
 * Licensed under the MIT License - see LICENSE file for details.
 */

package io.megabrain.core.prompt;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ApproximateTokenCounterTest {

    private ApproximateTokenCounter tokenCounter;

    @BeforeEach
    void setUp() {
        tokenCounter = new ApproximateTokenCounter();
    }

    @Test
    void testEstimateTokens_NullOrEmpty() {
        assertEquals(0, tokenCounter.estimateTokens(null));
        assertEquals(0, tokenCounter.estimateTokens(""));
    }

    @Test
    void testEstimateTokens_CalculatesCorrectly() {
        // 1 token ≈ 4 characters
        assertEquals(1, tokenCounter.estimateTokens("123"));   // 3 chars
        assertEquals(1, tokenCounter.estimateTokens("1234"));  // 4 chars
        assertEquals(2, tokenCounter.estimateTokens("12345")); // 5 chars
        
        String longText = "This is a longer text that should take more tokens to process."; // 62 chars
        assertEquals(16, tokenCounter.estimateTokens(longText)); // 62 / 4 = 15.5 -> 16
    }

    @ParameterizedTest
    @CsvSource({
        "gpt-4-32k, 32768",
        "GPT-4-32K, 32768",
        "gpt-4, 8192",
        "GPT-4, 8192",
        "gpt-3.5-turbo-16k, 16384",
        "gpt-3.5-turbo, 4096",
        "claude-3-opus, 100000",
        "CLAUDE-2, 100000",
        "llama-3, 8192",
        "llama3, 8192",
        "mistral, 8192",
        "phi, 2048",
        "unknown-model, 8192",
        ", 8192"
    })
    void testGetMaxContextWindow(String modelName, int expectedWindow) {
        assertEquals(expectedWindow, tokenCounter.getMaxContextWindow(modelName));
    }
}
