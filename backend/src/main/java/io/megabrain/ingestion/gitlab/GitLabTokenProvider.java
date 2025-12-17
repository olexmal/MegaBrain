/*
 * Copyright (c) 2025 MegaBrain Contributors
 * Licensed under the MIT License - see LICENSE file for details.
 */

package io.megabrain.ingestion.gitlab;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Provides GitLab authentication tokens.
 * Securely retrieves tokens from configuration without exposing them in logs.
 */
@ApplicationScoped
public class GitLabTokenProvider {

    private final GitLabConfiguration config;

    @Inject
    public GitLabTokenProvider(GitLabConfiguration config) {
        this.config = config;
    }

    /**
     * Gets the GitLab token if configured.
     * Returns null if no token is configured (for public repositories).
     *
     * @return the GitLab token, or null if not configured
     */
    public String getToken() {
        return config.token().filter(t -> !t.isBlank()).orElse(null);
    }

    /**
     * Checks if a token is configured.
     *
     * @return true if a token is configured, false otherwise
     */
    public boolean hasToken() {
        return config.token().isPresent() && !config.token().get().isBlank();
    }
}
