/*
 * Copyright (c) 2025 MegaBrain Contributors
 * Licensed under the MIT License - see LICENSE file for details.
 */

package io.megabrain.core.prompt;

import jakarta.enterprise.context.ApplicationScoped;

/**
 * Implementation of TokenCounter that uses an approximation rule
 * (e.g., 1 token ≈ 4 characters).
 */
@ApplicationScoped
public class ApproximateTokenCounter implements TokenCounter {

    private static final double CHARS_PER_TOKEN = 4.0;
    private static final int DEFAULT_CONTEXT_WINDOW = 8192;

    @Override
    public int estimateTokens(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        return (int) Math.ceil(text.length() / CHARS_PER_TOKEN);
    }

    @Override
    public int getMaxContextWindow(String modelName) {
        if (modelName == null || modelName.isEmpty()) {
            return DEFAULT_CONTEXT_WINDOW;
        }
        
        String lowerModel = modelName.toLowerCase();
        
        // Claude models
        if (lowerModel.contains("claude-3") || lowerModel.contains("claude-2")) {
            return 100000;
        }
        
        // GPT-4 models
        if (lowerModel.contains("gpt-4-32k")) {
            return 32768;
        } else if (lowerModel.contains("gpt-4")) {
            return 8192; // 8k default for gpt-4
        }
        
        // GPT-3.5 models
        if (lowerModel.contains("gpt-3.5-turbo-16k")) {
            return 16384;
        } else if (lowerModel.contains("gpt-3.5-turbo")) {
            return 4096;
        }
        
        // Llama models
        if (lowerModel.contains("llama-3") || lowerModel.contains("llama3")) {
            return 8192;
        }
        
        // Mistral models
        if (lowerModel.contains("mistral")) {
            return 8192;
        }
        
        // Phi models
        if (lowerModel.contains("phi")) {
            return 2048;
        }
        
        // Default
        return DEFAULT_CONTEXT_WINDOW;
    }
}
