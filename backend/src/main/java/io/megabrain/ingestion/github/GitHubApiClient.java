/*
 * Copyright (c) 2025 MegaBrain Contributors
 * Licensed under the MIT License - see LICENSE file for details.
 */

package io.megabrain.ingestion.github;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.annotation.RegisterClientHeaders;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

/**
 * REST client for GitHub API v3.
 * Used to fetch repository metadata.
 */
@RegisterRestClient(configKey = "github-api")
@RegisterClientHeaders(GitHubApiHeadersFactory.class)
@Path("/repos")
public interface GitHubApiClient {

    /**
     * Fetches repository information.
     *
     * @param owner the repository owner
     * @param repo the repository name
     * @return the repository information
     */
    @GET
    @Path("/{owner}/{repo}")
    @Produces(MediaType.APPLICATION_JSON)
    GitHubRepositoryInfo getRepository(
            @PathParam("owner") String owner,
            @PathParam("repo") String repo
    );

    /**
     * Fetches the latest commit SHA for a branch.
     *
     * @param owner the repository owner
     * @param repo the repository name
     * @param branch the branch name
     * @return the commit information
     */
    @GET
    @Path("/{owner}/{repo}/commits/{branch}")
    @Produces(MediaType.APPLICATION_JSON)
    GitHubCommitInfo getCommit(
            @PathParam("owner") String owner,
            @PathParam("repo") String repo,
            @PathParam("branch") String branch
    );
}

