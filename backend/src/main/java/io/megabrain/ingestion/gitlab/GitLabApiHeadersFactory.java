/*
 * Copyright (c) 2025 MegaBrain Contributors
 * Licensed under the MIT License - see LICENSE file for details.
 */

package io.megabrain.ingestion.gitlab;

import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.rest.client.ext.ClientHeadersFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;

/**
 * Factory for GitLab API request headers.
 * Adds authentication token if configured.
 */
@ApplicationScoped
public class GitLabApiHeadersFactory implements ClientHeadersFactory {

    private static final String GITLAB_TOKEN_PROPERTY = "megabrain.gitlab.token";

    @Override
    public MultivaluedMap<String, String> update(
            MultivaluedMap<String, String> incomingHeaders,
            MultivaluedMap<String, String> clientOutgoingHeaders) {

        MultivaluedMap<String, String> headers = new MultivaluedHashMap<>();

        // Add authentication token if configured
        ConfigProvider.getConfig()
                .getOptionalValue(GITLAB_TOKEN_PROPERTY, String.class)
                .ifPresent(token -> {
                    // GitLab uses PRIVATE-TOKEN header
                    if (token.startsWith("PRIVATE-TOKEN ")) {
                        headers.add("PRIVATE-TOKEN", token.substring("PRIVATE-TOKEN ".length()));
                    } else {
                        headers.add("PRIVATE-TOKEN", token);
                    }
                });

        return headers;
    }
}
