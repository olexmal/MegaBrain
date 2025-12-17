/*
 * Copyright (c) 2025 MegaBrain Contributors
 * Licensed under the MIT License - see LICENSE file for details.
 */

package io.megabrain.api;

import io.megabrain.ingestion.parser.GrammarManager;
import io.megabrain.ingestion.parser.ParserRegistry;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.HealthCheckResponseBuilder;
import org.eclipse.microprofile.health.Readiness;
import org.jboss.logging.Logger;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Health check for Tree-sitter grammar status.
 * Verifies that all required grammars are loaded and available.
 */
@Readiness
@ApplicationScoped
public class GrammarHealthCheck implements HealthCheck {

    private static final Logger LOG = Logger.getLogger(GrammarHealthCheck.class);
    private static final String GRAMMAR_PREFIX = "grammar.";

    private final GrammarManager grammarManager;
    private final ParserRegistry parserRegistry;

    @Inject
    public GrammarHealthCheck(GrammarManager grammarManager, ParserRegistry parserRegistry) {
        this.grammarManager = grammarManager;
        this.parserRegistry = parserRegistry;
    }

    @Override
    public HealthCheckResponse call() {
        long checkStartTime = System.nanoTime();

        HealthCheckResponseBuilder builder = HealthCheckResponse.named("grammar-status")
                .up();

        try {
            GrammarHealthStatus status = checkGrammarHealth();
            long totalCheckTime = (System.nanoTime() - checkStartTime) / 1_000_000;

            builder.withData("totalGrammars", status.totalGrammars())
                   .withData("loadedGrammars", status.loadedGrammars())
                   .withData("failedGrammars", status.failedGrammars())
                   .withData("totalCheckTimeMs", totalCheckTime)
                   .withData("averageCheckTimeMs", String.format("%.2f", totalCheckTime / (double) status.totalGrammars()));

            // Add details for each grammar
            for (GrammarDetail detail : status.grammarDetails()) {
                builder.withData(GRAMMAR_PREFIX + detail.language() + ".status",
                               detail.status().name())
                       .withData(GRAMMAR_PREFIX + detail.language() + ".version",
                               detail.version() != null ? detail.version() : "unknown")
                       .withData(GRAMMAR_PREFIX + detail.language() + ".error",
                               detail.errorMessage() != null ? detail.errorMessage() : "");
            }

            // Mark as down if any grammars failed to load
            if (status.failedGrammars() > 0) {
                builder.down()
                       .withData("error", status.failedGrammars() + " grammars failed to load");
            }

            // Performance check: health check should complete within reasonable time
            if (totalCheckTime > 2000) { // 2 second threshold for health check
                LOG.warnf("Grammar health check exceeded 2000ms threshold: %d ms", totalCheckTime);
            }

            LOG.debugf("Grammar health check: %d/%d grammars loaded, %d failed, %d ms total",
                      status.loadedGrammars(), status.totalGrammars(), status.failedGrammars(), totalCheckTime);

        } catch (Exception e) {
            long totalCheckTime = (System.nanoTime() - checkStartTime) / 1_000_000;
            LOG.errorf(e, "Failed to perform grammar health check after %d ms", totalCheckTime);
            builder.down()
                   .withData("error", "Health check failed: " + e.getMessage());
        }

        return builder.build();
    }

    /**
     * Check the health status of all grammars.
     */
    private GrammarHealthStatus checkGrammarHealth() {
        List<String> expectedLanguages = getExpectedLanguages();
        Map<String, GrammarDetail> grammarDetails = new ConcurrentHashMap<>();

        GrammarCounts counts = expectedLanguages.stream()
            .map(language -> checkSingleGrammar(language, grammarDetails))
            .reduce(new GrammarCounts(0, 0), GrammarCounts::add);

        return new GrammarHealthStatus(expectedLanguages.size(), counts.loaded(), counts.failed(),
                                     grammarDetails.values().stream().toList());
    }

    /**
     * Get the list of expected grammar languages.
     */
    private List<String> getExpectedLanguages() {
        return List.of("java", "python", "javascript", "typescript",
                      "c", "cpp", "go", "rust", "kotlin", "ruby",
                      "scala", "swift", "php", "csharp");
    }

    /**
     * Check the health of a single grammar and update the details map.
     */
    private GrammarCounts checkSingleGrammar(String language, Map<String, GrammarDetail> grammarDetails) {
        long startTime = System.nanoTime();

        try {
            GrammarDetail detail = checkGrammarDetail(language, startTime);
            grammarDetails.put(language, detail);

            if (detail.status() == GrammarStatus.LOADED) {
                return new GrammarCounts(1, 0);
            } else {
                int failed = detail.status() == GrammarStatus.FAILED ? 1 : 0;
                return new GrammarCounts(0, failed);
            }
        } catch (Exception e) {
            long checkTime = (System.nanoTime() - startTime) / 1_000_000;
            GrammarDetail failedDetail = new GrammarDetail(language, GrammarStatus.FAILED, null, e.getMessage());
            grammarDetails.put(language, failedDetail);
            LOG.debugf("Failed to check grammar status for %s after %d ms: %s", language, checkTime, e.getMessage());
            return new GrammarCounts(0, 1);
        }
    }

    /**
     * Check the detailed status of a single grammar.
     */
    private GrammarDetail checkGrammarDetail(String language, long startTime) {
        var versionInfo = grammarManager.getVersionInfo(language, null);

        if (versionInfo.isEmpty()) {
            return new GrammarDetail(language, GrammarStatus.NOT_CACHED, null, null);
        }

        String version = versionInfo.get().version();
        var grammarSpec = grammarManager.createGrammarSpecForVersion(language, version);

        if (grammarSpec == null) {
            return new GrammarDetail(language, GrammarStatus.FAILED, version, "Failed to create grammar spec");
        }

        var languageObj = grammarManager.loadLanguage(grammarSpec);
        if (languageObj == null) {
            return new GrammarDetail(language, GrammarStatus.FAILED, version, "Failed to load grammar");
        }

        // Performance check: AC5 requirement (<500ms cold start)
        long loadTimeMs = (System.nanoTime() - startTime) / 1_000_000;
        if (loadTimeMs > 500) {
            LOG.warnf("Grammar loading for %s exceeded 500ms threshold: %d ms (AC5 violation)", language, loadTimeMs);
        }

        return new GrammarDetail(language, GrammarStatus.LOADED, version, null);
    }

    /**
     * Simple record to track loaded/failed grammar counts.
     */
    private record GrammarCounts(int loaded, int failed) {
        GrammarCounts add(GrammarCounts other) {
            return new GrammarCounts(loaded + other.loaded, failed + other.failed);
        }
    }

    /**
     * Grammar status enumeration.
     */
    public enum GrammarStatus {
        LOADED("Grammar is loaded and available"),
        NOT_CACHED("Grammar not cached locally"),
        NOT_CONFIGURED("Grammar not configured"),
        FAILED("Grammar failed to load");

        private final String description;

        GrammarStatus(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * Details about a specific grammar's status.
     */
    public record GrammarDetail(
            String language,
            GrammarStatus status,
            String version,
            String errorMessage
    ) {}

    /**
     * Overall grammar health status.
     */
    public record GrammarHealthStatus(
            int totalGrammars,
            int loadedGrammars,
            int failedGrammars,
            List<GrammarDetail> grammarDetails
    ) {}
}
