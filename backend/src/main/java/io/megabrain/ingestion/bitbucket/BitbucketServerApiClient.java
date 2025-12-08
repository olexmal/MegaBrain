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
 * REST client for Bitbucket Server/Data Center API.
 * Used to fetch repository metadata from Bitbucket Server instances.
 */
@RegisterRestClient(configKey = "bitbucket-server-api")
@RegisterClientHeaders(BitbucketServerApiHeadersFactory.class)
public interface BitbucketServerApiClient {

    /**
     * Fetches repository information from Bitbucket Server.
     *
     * @param project the project key
     * @param repo the repository slug
     * @return the repository information
     */
    @GET
    @Path("/rest/api/1.0/projects/{project}/repos/{repo}")
    @Produces(MediaType.APPLICATION_JSON)
    BitbucketServerRepositoryInfo getRepository(
            @PathParam("project") String project,
            @PathParam("repo") String repo
    );

    /**
     * Fetches the latest commit SHA for a branch from Bitbucket Server.
     *
     * @param project the project key
     * @param repo the repository slug
     * @param branch the branch name
     * @return the commit information
     */
    @GET
    @Path("/rest/api/1.0/projects/{project}/repos/{repo}/commits?until={branch}&limit=1")
    @Produces(MediaType.APPLICATION_JSON)
    BitbucketServerCommitInfo getCommit(
            @PathParam("project") String project,
            @PathParam("repo") String repo,
            @PathParam("branch") String branch
    );
}
