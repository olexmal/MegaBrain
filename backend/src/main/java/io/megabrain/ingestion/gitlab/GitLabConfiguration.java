/*
 * Copyright (c) 2025 MegaBrain Contributors
 * Licensed under the MIT License - see LICENSE file for details.
 */

package io.megabrain.ingestion.gitlab;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithName;

import java.util.Optional;

/**
 * Configuration for GitLab integration, including self-hosted instance support.
 */
@ConfigMapping(prefix = "megabrain.gitlab")
public interface GitLabConfiguration {

    /**
     * GitLab API base URL.
     * Supports both gitlab.com and self-hosted instances.
     */
    @WithName("api-url")
    @WithDefault("https://gitlab.com")
    String apiUrl();

    /**
     * GitLab authentication token.
     * Used for accessing private repositories and higher rate limits.
     */
    Optional<String> token();

    /**
     * Connection timeout in milliseconds for GitLab API calls.
     */
    @WithName("connect-timeout")
    @WithDefault("10000")
    int connectTimeout();

    /**
     * Read timeout in milliseconds for GitLab API calls.
     */
    @WithName("read-timeout")
    @WithDefault("30000")
    int readTimeout();

    /**
     * SSL configuration for self-hosted GitLab instances.
     */
    Ssl ssl();

    interface Ssl {
        /**
         * Path to custom truststore file for self-hosted GitLab SSL certificates.
         */
        @WithName("trust-store")
        Optional<String> trustStore();

        /**
         * Password for the custom truststore.
         */
        @WithName("trust-store-password")
        Optional<String> trustStorePassword();

        /**
         * Type of the truststore (JKS, PKCS12, etc.).
         */
        @WithName("trust-store-type")
        @WithDefault("JKS")
        String trustStoreType();

        /**
         * Whether to verify SSL certificates for self-hosted instances.
         * Set to false only for testing/development environments.
         */
        @WithName("verify-ssl")
        @WithDefault("true")
        boolean verifySsl();
    }
}
