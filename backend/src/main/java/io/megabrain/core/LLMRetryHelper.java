/*
 * Copyright (c) 2025 MegaBrain Contributors
 * Licensed under the MIT License - see LICENSE file for details.
 */

package io.megabrain.core;

import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;

import java.util.function.Supplier;

/**
 * Applies retry with exponential backoff around LLM API calls (US-03-02 T5).
 * Handles HTTP 429 (rate limit) and 5xx (server errors) by retrying with configurable
 * attempts and base delay. Produces clear error messages when retries are exhausted.
 */
@ApplicationScoped
public class LLMRetryHelper {

    private static final Logger LOG = Logger.getLogger(LLMRetryHelper.class);

    /**
     * Executes the given call with retry. Retries on 429 and 5xx-like failures
     * using exponential backoff (baseDelayMs, 2x, 4x, 8x). After max retries,
     * throws with a clear message (rate limit or service unavailable).
     *
     * @param call         the API call to execute (e.g. chatModel.chat(...))
     * @param providerName name for logging (e.g. "OpenAI", "Anthropic")
     * @param maxRetries   number of retries (total attempts = maxRetries + 1)
     * @param baseDelayMs  base delay in ms for exponential backoff
     * @return the result of the call
     * @throws IllegalStateException after all retries exhausted, with a clear message
     */
    public String executeWithRetry(Supplier<String> call, String providerName, int maxRetries, long baseDelayMs) {
        if (maxRetries <= 0) {
            return call.get();
        }
        int maxAttempts = maxRetries + 1;
        long delayMs = baseDelayMs;
        Exception lastFailure = null;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                return call.get();
            } catch (Exception e) {
                lastFailure = e;
                if (!isRetryable(e)) {
                    throw e instanceof RuntimeException re ? re : new RuntimeException(e);
                }
                if (attempt == maxAttempts) {
                    String message = userFacingMessage(e);
                    LOG.warnf(e, "%s API call failed after %d attempt(s): %s", providerName, attempt, message);
                    throw new IllegalStateException(message, e);
                }
                LOG.debugf("%s API call attempt %d/%d failed (retryable), waiting %d ms: %s",
                        providerName, attempt, maxAttempts, delayMs, e.getMessage());
                sleep(delayMs);
                delayMs = Math.min(delayMs * 2, 30_000);
            }
        }
        String message = userFacingMessage(lastFailure);
        LOG.warnf(lastFailure != null ? lastFailure : new Throwable(), "%s API call failed after retries: %s", providerName, message);
        throw new IllegalStateException(message, lastFailure);
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting before retry", ie);
        }
    }

    /**
     * Returns true if the throwable indicates a retryable failure (429 or 5xx).
     * LangChain4j and HTTP clients often expose status in message or cause.
     */
    public boolean isRetryable(Throwable t) {
        if (t == null) {
            return false;
        }
        String msg = (t.getMessage() != null) ? t.getMessage().toLowerCase() : "";
        Throwable cause = t.getCause();
        if (cause != null) {
            String causeMsg = cause.getMessage() != null ? cause.getMessage().toLowerCase() : "";
            if (isRetryableMessage(causeMsg)) {
                return true;
            }
        }
        return isRetryableMessage(msg);
    }

    private static boolean isRetryableMessage(String msg) {
        if (msg == null) {
            return false;
        }
        return msg.contains("429") || msg.contains("rate limit") || msg.contains("rate_limit")
                || msg.contains("too many requests")
                || msg.contains("503") || msg.contains("502") || msg.contains("500")
                || msg.contains("service unavailable") || msg.contains("bad gateway")
                || msg.contains("internal server error");
    }

    private static String userFacingMessage(Throwable e) {
        if (e == null) {
            return "Service temporarily unavailable. Please try again later.";
        }
        String msg = e.getMessage() != null ? e.getMessage().toLowerCase() : "";
        Throwable cause = e.getCause();
        if (cause != null) {
            String c = cause.getMessage() != null ? cause.getMessage().toLowerCase() : "";
            if (c.contains("429") || c.contains("rate limit") || c.contains("too many requests")) {
                return "Rate limit exceeded. Please try again later.";
            }
            if (c.contains("503") || c.contains("502") || c.contains("500") || c.contains("service unavailable")) {
                return "Service temporarily unavailable. Please try again later.";
            }
        }
        if (msg.contains("429") || msg.contains("rate limit") || msg.contains("too many requests")) {
            return "Rate limit exceeded. Please try again later.";
        }
        if (msg.contains("503") || msg.contains("502") || msg.contains("500") || msg.contains("service unavailable")) {
            return "Service temporarily unavailable. Please try again later.";
        }
        return "Service temporarily unavailable. Please try again later.";
    }
}
