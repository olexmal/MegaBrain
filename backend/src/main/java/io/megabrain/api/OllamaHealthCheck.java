/*
 * Copyright (c) 2025 MegaBrain Contributors
 * Licensed under the MIT License - see LICENSE file for details.
 */

package io.megabrain.api;

import io.megabrain.core.OllamaConfiguration;
import io.megabrain.core.OllamaLLMClient;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.HealthCheckResponseBuilder;
import org.eclipse.microprofile.health.Readiness;
import org.jboss.logging.Logger;

import java.time.Duration;

/**
 * Readiness health check for Ollama LLM service.
 * Verifies that the Ollama endpoint is reachable and the configured model is available.
 * Contributes to {@code /q/health/ready}.
 */
@Readiness
@ApplicationScoped
public class OllamaHealthCheck implements HealthCheck {

    private static final Logger LOG = Logger.getLogger(OllamaHealthCheck.class);
    private static final Duration HEALTH_CHECK_TIMEOUT = Duration.ofSeconds(10);

    private final OllamaLLMClient ollamaClient;
    private final OllamaConfiguration config;

    @Inject
    public OllamaHealthCheck(OllamaLLMClient ollamaClient, OllamaConfiguration config) {
        this.ollamaClient = ollamaClient;
        this.config = config;
    }

    @Override
    public HealthCheckResponse call() {
        long startTime = System.nanoTime();
        String endpoint = config.baseUrl();
        String model = config.model();

        HealthCheckResponseBuilder builder = HealthCheckResponse.named("ollama")
                .withData("endpoint", endpoint)
                .withData("model", model);

        try {
            if (!ollamaClient.isAvailable()) {
                long durationMs = (System.nanoTime() - startTime) / 1_000_000;
                LOG.debugf("Ollama health check: client not available (endpoint=%s) in %d ms", endpoint, durationMs);
                return builder.down()
                        .withData("message", "Ollama client not available. Check base URL and startup logs.")
                        .withData("checkTimeMs", durationMs)
                        .build();
            }

            Boolean modelAvailable = ollamaClient.isModelAvailable(model)
                    .await().atMost(HEALTH_CHECK_TIMEOUT);

            long durationMs = (System.nanoTime() - startTime) / 1_000_000;
            builder.withData("checkTimeMs", durationMs);

            if (Boolean.TRUE.equals(modelAvailable)) {
                LOG.debugf("Ollama health check: endpoint reachable, model '%s' available in %d ms", model, durationMs);
                return builder.up()
                        .withData("message", "Ollama reachable, configured model available.")
                        .build();
            }

            LOG.warnf("Ollama health check: model '%s' not available at %s (check in %d ms)", model, endpoint, durationMs);
            return builder.down()
                    .withData("message", "Configured model not available. Use 'ollama pull " + model + "' or check megabrain.llm.ollama.model.")
                    .build();

        } catch (Exception e) {
            long durationMs = (System.nanoTime() - startTime) / 1_000_000;
            LOG.errorf(e, "Ollama health check failed after %d ms", durationMs);
            return builder.down()
                    .withData("message", "Ollama endpoint unreachable or error: " + e.getMessage())
                    .withData("checkTimeMs", durationMs)
                    .build();
        }
    }
}
