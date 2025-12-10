/*
 * Copyright (c) 2025 MegaBrain Contributors
 * Licensed under the MIT License - see LICENSE file for details.
 */

package io.megabrain.ingestion.bitbucket;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.MultivaluedMap;

import org.eclipse.microprofile.rest.client.ext.ClientHeadersFactory;

import java.util.Base64;

/**
 * Headers factory for Bitbucket Cloud API authentication.
 * Uses Basic Authentication with username and app password.
 */
@ApplicationScoped
public class BitbucketCloudApiHeadersFactory implements ClientHeadersFactory {

    @Inject
    BitbucketTokenProvider tokenProvider;

    @Override
    public MultivaluedMap<String, String> update(
            MultivaluedMap<String, String> incomingHeaders,
            MultivaluedMap<String, String> outgoingHeaders) {

        // Get Bitbucket Cloud credentials
        BitbucketTokenProvider.BitbucketCredentials credentials = tokenProvider.getCredentials(true);
        if (credentials != null) {
            // Create Basic Auth header: "Basic base64(username:app-password)"
            String authString = credentials.username() + ":" + credentials.password();
            String encodedAuth = Base64.getEncoder().encodeToString(authString.getBytes());
            outgoingHeaders.add("Authorization", "Basic " + encodedAuth);
        }

        // Add User-Agent header
        outgoingHeaders.add("User-Agent", "MegaBrain/1.0");

        // Add Accept header for JSON
        outgoingHeaders.add("Accept", "application/json");

        return outgoingHeaders;
    }
}
