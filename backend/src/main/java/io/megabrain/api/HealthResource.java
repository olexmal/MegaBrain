package io.megabrain.api;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * Health check endpoint for MegaBrain application.
 * Provides basic health status information.
 */
@Path("/q/health")
public class HealthResource {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response health() {
        return Response.ok()
                .entity(new HealthStatus("UP", "MegaBrain is running"))
                .build();
    }

    /**
     * Health status response model.
     */
    public record HealthStatus(String status, String message) {
    }
}

