/*
 * Copyright (c) 2025 MegaBrain Contributors
 * Licensed under the MIT License - see LICENSE file for details.
 */

package io.megabrain.ingestion.github;

import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.rest.client.ext.ClientHeadersFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;

/**
 * Factory for GitHub API request headers.
 * Adds authentication token if configured.
 */
@ApplicationScoped
public class GitHubApiHeadersFactory implements ClientHeadersFactory {

    private static final String GITHUB_TOKEN_PROPERTY = "megabrain.github.token";
    private static final String GITHUB_API_VERSION_HEADER = "X-GitHub-Api-Version";
    private static final String GITHUB_API_VERSION = "2022-11-28";

    @Override
    public MultivaluedMap<String, String> update(
            MultivaluedMap<String, String> incomingHeaders,
            MultivaluedMap<String, String> clientOutgoingHeaders) {

        MultivaluedMap<String, String> headers = new MultivaluedHashMap<>();
        
        // Add GitHub API version header
        headers.add(GITHUB_API_VERSION_HEADER, GITHUB_API_VERSION);
        
        // Add authentication token if configured
        ConfigProvider.getConfig()
                .getOptionalValue(GITHUB_TOKEN_PROPERTY, String.class)
                .ifPresent(token -> {
                    // Support both "token" and "Bearer" formats
                    if (token.startsWith("Bearer ") || token.startsWith("token ")) {
                        headers.add("Authorization", token);
                    } else {
                        headers.add("Authorization", "token " + token);
                    }
                });

        return headers;
    }
}

