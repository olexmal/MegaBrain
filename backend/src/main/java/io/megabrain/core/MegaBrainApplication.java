package io.megabrain.core;

import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;

/**
 * Main application class for MegaBrain RAG Pipeline.
 * Configures JAX-RS application path.
 */
@ApplicationPath("/api/v1")
public class MegaBrainApplication extends Application {
    // JAX-RS application configuration
}

