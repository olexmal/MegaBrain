/*
 * Copyright (c) 2025 MegaBrain Contributors
 * Licensed under the MIT License - see LICENSE file for details.
 */

package io.megabrain.ingestion.bitbucket;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.annotation.RegisterClientHeaders;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

/**
 * REST client for Bitbucket Cloud API v2.0.
 * Used to fetch repository metadata from Bitbucket Cloud (bitbucket.org).
 */
@RegisterRestClient(configKey = "bitbucket-cloud-api")
@RegisterClientHeaders(BitbucketCloudApiHeadersFactory.class)
@Path("/2.0")
public interface BitbucketCloudApiClient {

    /**
     * Fetches repository information from Bitbucket Cloud.
     *
     * @param workspace the workspace ID
     * @param repo the repository slug
     * @return the repository information
     */
    @GET
    @Path("/repositories/{workspace}/{repo}")
    @Produces(MediaType.APPLICATION_JSON)
    BitbucketCloudRepositoryInfo getRepository(
            @PathParam("workspace") String workspace,
            @PathParam("repo") String repo
    );

    /**
     * Fetches the latest commit SHA for a branch from Bitbucket Cloud.
     *
     * @param workspace the workspace ID
     * @param repo the repository slug
     * @param branch the branch name
     * @return the commit information
     */
    @GET
    @Path("/repositories/{workspace}/{repo}/commits/{branch}")
    @Produces(MediaType.APPLICATION_JSON)
    BitbucketCloudCommitInfo getCommit(
            @PathParam("workspace") String workspace,
            @PathParam("repo") String repo,
            @PathParam("branch") String branch
    );
}
