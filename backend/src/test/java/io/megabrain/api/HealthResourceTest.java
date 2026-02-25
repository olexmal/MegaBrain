/*
 * Copyright (c) 2025 MegaBrain Contributors
 * Licensed under the MIT License - see LICENSE file for details.
 */

package io.megabrain.api;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.anyOf;

@QuarkusTest
class HealthResourceTest {

    @Test
    void health_shouldReturnUpOrDownStatus() {
        // /q/health aggregates liveness and readiness. Ollama readiness check can be DOWN when
        // Ollama is not running or model not available, so we accept either 200 (UP) or 503 (DOWN).
        given()
          .when().get("/q/health")
          .then()
             .statusCode(anyOf(is(200), is(503)))
             .body("status", anyOf(is("UP"), is("DOWN")))
             .contentType("application/json");
    }

    @Test
    void health_shouldReturnJsonContentType() {
        given()
          .when().get("/q/health")
          .then()
             .statusCode(anyOf(is(200), is(503)))
             .contentType("application/json");
    }
}
