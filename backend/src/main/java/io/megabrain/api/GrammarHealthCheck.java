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

    @Inject
    GrammarManager grammarManager;

    @Inject
    ParserRegistry parserRegistry;

    @Override
    public HealthCheckResponse call() {
        HealthCheckResponseBuilder builder = HealthCheckResponse.named("grammar-status")
                .up();

        try {
            GrammarHealthStatus status = checkGrammarHealth();
            builder.withData("totalGrammars", status.totalGrammars())
                   .withData("loadedGrammars", status.loadedGrammars())
                   .withData("failedGrammars", status.failedGrammars());

            // Add details for each grammar
            for (GrammarDetail detail : status.grammarDetails()) {
                builder.withData("grammar." + detail.language() + ".status",
                               detail.status().name())
                       .withData("grammar." + detail.language() + ".version",
                               detail.version() != null ? detail.version() : "unknown")
                       .withData("grammar." + detail.language() + ".error",
                               detail.errorMessage() != null ? detail.errorMessage() : "");
            }

            // Mark as down if any grammars failed to load
            if (status.failedGrammars() > 0) {
                builder.down()
                       .withData("error", status.failedGrammars() + " grammars failed to load");
            }

            LOG.debugf("Grammar health check: %d/%d grammars loaded, %d failed",
                      status.loadedGrammars(), status.totalGrammars(), status.failedGrammars());

        } catch (Exception e) {
            LOG.errorf(e, "Failed to perform grammar health check");
            builder.down()
                   .withData("error", "Health check failed: " + e.getMessage());
        }

        return builder.build();
    }

    /**
     * Check the health status of all grammars.
     */
    private GrammarHealthStatus checkGrammarHealth() {
        // Get all known grammar languages from parser registry
        // For now, we'll check common languages that might be expected
        List<String> expectedLanguages = List.of("java", "python", "javascript", "typescript",
                                                "c", "cpp", "go", "rust", "kotlin", "ruby",
                                                "scala", "swift", "php", "csharp");

        Map<String, GrammarDetail> grammarDetails = new ConcurrentHashMap<>();
        int loadedCount = 0;
        int failedCount = 0;

        for (String language : expectedLanguages) {
            GrammarStatus status = GrammarStatus.NOT_CONFIGURED;
            String version = null;
            String errorMessage = null;

            try {
                // Try to get version info for this language
                var versionInfo = grammarManager.getVersionInfo(language, null);

                if (versionInfo.isPresent()) {
                    version = versionInfo.get().version();

                    // Try to load the grammar to verify it's working
                    var grammarSpec = grammarManager.createGrammarSpecForVersion(language, version);
                    if (grammarSpec != null) {
                        var languageObj = grammarManager.loadLanguage(grammarSpec);
                        if (languageObj != null) {
                            status = GrammarStatus.LOADED;
                            loadedCount++;
                        } else {
                            status = GrammarStatus.FAILED;
                            errorMessage = "Failed to load grammar";
                            failedCount++;
                        }
                    } else {
                        status = GrammarStatus.FAILED;
                        errorMessage = "Failed to create grammar spec";
                        failedCount++;
                    }
                } else {
                    // No cached version found
                    status = GrammarStatus.NOT_CACHED;
                }
            } catch (Exception e) {
                status = GrammarStatus.FAILED;
                errorMessage = e.getMessage();
                failedCount++;
                LOG.debugf(e, "Failed to check grammar status for %s", language);
            }

            grammarDetails.put(language, new GrammarDetail(language, status, version, errorMessage));
        }

        return new GrammarHealthStatus(expectedLanguages.size(), loadedCount, failedCount,
                                     grammarDetails.values().stream().toList());
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
