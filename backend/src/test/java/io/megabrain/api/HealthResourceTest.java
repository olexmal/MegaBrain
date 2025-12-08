/*
 * Copyright (c) 2025 MegaBrain Contributors
 * Licensed under the MIT License - see LICENSE file for details.
 */

package io.megabrain.api;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;

@QuarkusTest
class HealthResourceTest {

    @Test
    void health_shouldReturnUpStatus() {
        // Note: /q/health is handled by Quarkus SmallRye Health extension
        // This tests the Quarkus health endpoint which returns status and checks
        given()
          .when().get("/q/health")
          .then()
             .statusCode(200)
             .body("status", is("UP"));
    }

    @Test
    void health_shouldReturnJsonContentType() {
        given()
          .when().get("/q/health")
          .then()
             .statusCode(200)
             .contentType("application/json");
    }
}
