/*
 * Copyright (c) 2025 MegaBrain Contributors
 * Licensed under the MIT License - see LICENSE file for details.
 */

package io.megabrain.core.prompt;

/**
 * Service for estimating token counts and managing context windows.
 */
public interface TokenCounter {

    /**
     * Estimates the number of tokens in the given text.
     * 
     * @param text The text to estimate tokens for
     * @return The estimated number of tokens
     */
    int estimateTokens(String text);

    /**
     * Gets the maximum context window size (in tokens) for a given model.
     * 
     * @param modelName The name or identifier of the model
     * @return The maximum number of tokens allowed in the context window
     */
    int getMaxContextWindow(String modelName);
}
