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
 * Headers factory for Bitbucket Server API authentication.
 * Uses Bearer token authentication with personal access tokens.
 */
@ApplicationScoped
public class BitbucketServerApiHeadersFactory implements ClientHeadersFactory {

    @Inject
    BitbucketTokenProvider tokenProvider;

    @Override
    public MultivaluedMap<String, String> update(
            MultivaluedMap<String, String> incomingHeaders,
            MultivaluedMap<String, String> outgoingHeaders) {

        // Get Bitbucket Server credentials
        BitbucketTokenProvider.BitbucketCredentials credentials = tokenProvider.getCredentials(false);
        if (credentials != null) {
            // Use Basic auth: username + personal access token as password
            String basic = credentials.username() + ":" + credentials.password();
            String encoded = Base64.getEncoder().encodeToString(basic.getBytes());
            outgoingHeaders.add("Authorization", "Basic " + encoded);
        }

        // Add User-Agent header
        outgoingHeaders.add("User-Agent", "MegaBrain/1.0");

        // Add Accept header for JSON
        outgoingHeaders.add("Accept", "application/json");

        return outgoingHeaders;
    }
}
