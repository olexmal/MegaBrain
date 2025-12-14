/*
 * Copyright (c) 2025 MegaBrain Contributors
 * Licensed under the MIT License - see LICENSE file for details.
 */

package io.megabrain.ingestion.bitbucket;

import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * Provides Bitbucket authentication credentials.
 * Supports both Bitbucket Cloud (app passwords) and Bitbucket Server (personal access tokens).
 * Securely retrieves credentials from configuration without exposing them in logs.
 */
@ApplicationScoped
public class BitbucketTokenProvider {

    @ConfigProperty(name = "megabrain.bitbucket.cloud.username")
    Optional<String> cloudUsername;

    @ConfigProperty(name = "megabrain.bitbucket.cloud.app-password")
    Optional<String> cloudAppPassword;

    @ConfigProperty(name = "megabrain.bitbucket.server.username")
    Optional<String> serverUsername;

    @ConfigProperty(name = "megabrain.bitbucket.server.token")
    Optional<String> serverToken;

    /**
     * Gets Bitbucket credentials for the specified platform.
     *
     * @param isCloud true for Bitbucket Cloud, false for Bitbucket Server
     * @return the credentials, or null if not configured
     */
    public BitbucketCredentials getCredentials(boolean isCloud) {
        if (isCloud) {
            if (cloudUsername.isPresent() && cloudAppPassword.isPresent()) {
                String username = cloudUsername.get();
                String password = cloudAppPassword.get();
                if (!username.isBlank() && !password.isBlank()) {
                    return new BitbucketCredentials(username, password);
                }
            }
        } else {
            if (serverUsername.isPresent() && serverToken.isPresent()) {
                String username = serverUsername.get();
                String token = serverToken.get();
                if (!username.isBlank() && !token.isBlank()) {
                    return new BitbucketCredentials(username, token);
                }
            }
        }
        return null;
    }

    /**
     * Checks if credentials are configured for the specified platform.
     *
     * @param isCloud true for Bitbucket Cloud, false for Bitbucket Server
     * @return true if credentials are configured, false otherwise
     */
    public boolean hasCredentials(boolean isCloud) {
        if (isCloud) {
            return cloudUsername.isPresent() && cloudAppPassword.isPresent() &&
                   !cloudUsername.get().isBlank() && !cloudAppPassword.get().isBlank();
        } else {
            return serverUsername.isPresent() && serverToken.isPresent() &&
                   !serverUsername.get().isBlank() && !serverToken.get().isBlank();
        }
    }

    /**
     * Record for Bitbucket credentials.
     */
    public record BitbucketCredentials(String username, String password) {
    }
}
