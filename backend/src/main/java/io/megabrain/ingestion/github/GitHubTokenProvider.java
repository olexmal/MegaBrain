/*
 * Copyright (c) 2025 MegaBrain Contributors
 * Licensed under the MIT License - see LICENSE file for details.
 */

package io.megabrain.ingestion.github;

import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * Provides GitHub authentication tokens.
 * Securely retrieves tokens from configuration without exposing them in logs.
 */
@ApplicationScoped
public class GitHubTokenProvider {

    @ConfigProperty(name = "megabrain.github.token")
    java.util.Optional<String> token;

    /**
     * Gets the GitHub token if configured.
     * Returns null if no token is configured (for public repositories).
     *
     * @return the GitHub token, or null if not configured
     */
    public String getToken() {
        return token.filter(t -> !t.isBlank()).orElse(null);
    }

    /**
     * Checks if a token is configured.
     *
     * @return true if a token is configured, false otherwise
     */
    public boolean hasToken() {
        return token.isPresent() && !token.get().isBlank();
    }
}

