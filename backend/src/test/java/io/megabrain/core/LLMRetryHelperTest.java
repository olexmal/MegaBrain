/*
 * Copyright (c) 2025 MegaBrain Contributors
 * Licensed under the MIT License - see LICENSE file for details.
 */

package io.megabrain.core;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for LLMRetryHelper (US-03-02 T5).
 * Tests retry behavior: succeed on Nth attempt, fail after max retries with clear message,
 * non-retryable exception fails immediately.
 */
@ExtendWith(MockitoExtension.class)
class LLMRetryHelperTest {

    private LLMRetryHelper helper;

    @BeforeEach
    void setUp() {
        helper = new LLMRetryHelper();
    }

    @Test
    @DisplayName("succeeds on first attempt when call returns")
    void executeWithRetry_successOnFirstAttempt_returnsResult() {
        String expected = "Hello";
        String actual = helper.executeWithRetry(() -> expected, "Test", 3, 100L);
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    @DisplayName("succeeds on second attempt when first throws retryable")
    void executeWithRetry_succeedsOnSecondAttempt_returnsResult() {
        AtomicInteger attempts = new AtomicInteger(0);
        String result = helper.executeWithRetry(() -> {
            if (attempts.incrementAndGet() < 2) {
                throw new RuntimeException("HTTP 429 rate limit exceeded");
            }
            return "ok";
        }, "Test", 3, 10L);
        assertThat(result).isEqualTo("ok");
        assertThat(attempts.get()).isEqualTo(2);
    }

    @Test
    @DisplayName("succeeds on third attempt when first two throw retryable")
    void executeWithRetry_succeedsOnThirdAttempt_returnsResult() {
        AtomicInteger attempts = new AtomicInteger(0);
        String result = helper.executeWithRetry(() -> {
            if (attempts.incrementAndGet() < 3) {
                throw new RuntimeException("503 Service Unavailable");
            }
            return "done";
        }, "Test", 4, 10L);
        assertThat(result).isEqualTo("done");
        assertThat(attempts.get()).isEqualTo(3);
    }

    @Test
    @DisplayName("after max retries throws with clear rate limit message")
    void executeWithRetry_alwaysRateLimit_throwsWithClearMessage() {
        assertThatThrownBy(() -> helper.executeWithRetry(
                () -> {
                    throw new RuntimeException("429 Too Many Requests");
                },
                "OpenAI", 2, 5L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Rate limit exceeded. Please try again later.")
                .hasCauseInstanceOf(RuntimeException.class)
                .cause()
                .hasMessageContaining("429");
    }

    @Test
    @DisplayName("after max retries throws with clear service unavailable message")
    void executeWithRetry_always5xx_throwsWithClearMessage() {
        assertThatThrownBy(() -> helper.executeWithRetry(
                () -> {
                    throw new RuntimeException("503 Service Unavailable");
                },
                "Anthropic", 2, 5L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Service temporarily unavailable. Please try again later.")
                .hasCauseInstanceOf(RuntimeException.class);
    }

    @Test
    @DisplayName("non-retryable exception fails immediately without retry")
    void executeWithRetry_nonRetryableException_throwsImmediately() {
        AtomicInteger attempts = new AtomicInteger(0);
        assertThatThrownBy(() -> helper.executeWithRetry(() -> {
            attempts.incrementAndGet();
            throw new IllegalArgumentException("Invalid request");
        }, "Test", 3, 10L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid request");
        assertThat(attempts.get()).isEqualTo(1);
    }

    @Test
    @DisplayName("maxRetries 0 runs call once without retry")
    void executeWithRetry_zeroMaxRetries_runsOnce() {
        AtomicInteger attempts = new AtomicInteger(0);
        String result = helper.executeWithRetry(() -> {
            attempts.incrementAndGet();
            return "x";
        }, "Test", 0, 1000L);
        assertThat(result).isEqualTo("x");
        assertThat(attempts.get()).isEqualTo(1);
    }

    @Test
    @DisplayName("isRetryable returns true for 429 message")
    void isRetryable_429_returnsTrue() {
        assertThat(helper.isRetryable(new RuntimeException("HTTP 429"))).isTrue();
        assertThat(helper.isRetryable(new RuntimeException("rate limit exceeded"))).isTrue();
        assertThat(helper.isRetryable(new Exception("Too many requests"))).isTrue();
    }

    @Test
    @DisplayName("isRetryable returns true for 5xx message")
    void isRetryable_5xx_returnsTrue() {
        assertThat(helper.isRetryable(new RuntimeException("503 Service Unavailable"))).isTrue();
        assertThat(helper.isRetryable(new RuntimeException("502 Bad Gateway"))).isTrue();
        assertThat(helper.isRetryable(new RuntimeException("500 Internal Server Error"))).isTrue();
    }

    @Test
    @DisplayName("isRetryable returns false for non-retryable")
    void isRetryable_nonRetryable_returnsFalse() {
        assertThat(helper.isRetryable(new IllegalArgumentException("bad input"))).isFalse();
        assertThat(helper.isRetryable(new RuntimeException("401 Unauthorized"))).isFalse();
        assertThat(helper.isRetryable(null)).isFalse();
    }
}
