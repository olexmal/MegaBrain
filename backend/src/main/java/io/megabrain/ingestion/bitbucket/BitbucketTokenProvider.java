/*
 * Copyright (c) 2025 MegaBrain Contributors
 * Licensed under the MIT License - see LICENSE file for details.
 */

package io.megabrain.ingestion.bitbucket;

import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * Provides Bitbucket authentication credentials.
 * Supports both Bitbucket Cloud (app passwords) and Bitbucket Server (personal access tokens).
 * Securely retrieves credentials from configuration without exposing them in logs.
 */
@ApplicationScoped
public class BitbucketTokenProvider {

    private final Optional<String> cloudUsername;
    private final Optional<String> cloudAppPassword;
    private final Optional<String> serverUsername;
    private final Optional<String> serverToken;

    @Inject
    public BitbucketTokenProvider(
            @ConfigProperty(name = "megabrain.bitbucket.cloud.username") Optional<String> cloudUsername,
            @ConfigProperty(name = "megabrain.bitbucket.cloud.app-password") Optional<String> cloudAppPassword,
            @ConfigProperty(name = "megabrain.bitbucket.server.username") Optional<String> serverUsername,
            @ConfigProperty(name = "megabrain.bitbucket.server.token") Optional<String> serverToken) {
        this.cloudUsername = cloudUsername;
        this.cloudAppPassword = cloudAppPassword;
        this.serverUsername = serverUsername;
        this.serverToken = serverToken;
    }

    /**
     * Gets Bitbucket credentials for the specified platform.
     *
     * @param isCloud true for Bitbucket Cloud, false for Bitbucket Server
     * @return the credentials, or null if not configured
     */
    public BitbucketCredentials getCredentials(boolean isCloud) {
        return isCloud ? getCloudCredentials() : getServerCredentials();
    }

    private BitbucketCredentials getCloudCredentials() {
        return buildCredentials(cloudUsername, cloudAppPassword);
    }

    private BitbucketCredentials getServerCredentials() {
        return buildCredentials(serverUsername, serverToken);
    }

    private BitbucketCredentials buildCredentials(Optional<String> usernameOpt, Optional<String> passwordOpt) {
        if (usernameOpt.isEmpty() || passwordOpt.isEmpty()) {
            return null;
        }
        String username = usernameOpt.get();
        String password = passwordOpt.get();
        if (username.isBlank() || password.isBlank()) {
            return null;
        }
        return new BitbucketCredentials(username, password);
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
