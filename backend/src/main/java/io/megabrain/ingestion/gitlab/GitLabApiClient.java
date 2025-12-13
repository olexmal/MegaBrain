/*
 * Copyright (c) 2025 MegaBrain Contributors
 * Licensed under the MIT License - see LICENSE file for details.
 */

package io.megabrain.ingestion.gitlab;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.annotation.RegisterClientHeaders;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

/**
 * REST client for GitLab API v4.
 * Used to fetch repository metadata.
 */
@RegisterRestClient(configKey = "gitlab-api")
@RegisterClientHeaders(GitLabApiHeadersFactory.class)
@Path("/api/v4")
public interface GitLabApiClient {

    /**
     * Fetches project information by ID or namespace/project path.
     *
     * @param projectIdOrPath the project ID or namespace/project path (e.g., "namespace/project")
     * @return the project information
     */
    @GET
    @Path("/projects/{projectIdOrPath}")
    @Produces(MediaType.APPLICATION_JSON)
    GitLabRepositoryInfo getProject(
            @PathParam("projectIdOrPath") String projectIdOrPath
    );

    /**
     * Fetches the latest commit for a project's default branch.
     *
     * @param projectIdOrPath the project ID or namespace/project path
     * @param ref the branch or tag name (optional, defaults to default branch)
     * @return the commit information
     */
    @GET
    @Path("/projects/{projectIdOrPath}/repository/commits")
    @Produces(MediaType.APPLICATION_JSON)
    GitLabCommitInfo[] getCommits(
            @PathParam("projectIdOrPath") String projectIdOrPath,
            @QueryParam("ref_name") String ref,
            @QueryParam("per_page") Integer perPage
    );
}
