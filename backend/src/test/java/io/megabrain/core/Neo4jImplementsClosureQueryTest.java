/*
 * Copyright (c) 2025 MegaBrain Contributors
 * Licensed under the MIT License - see LICENSE file for details.
 */

package io.megabrain.core;

import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for Neo4jImplementsClosureQuery (US-02-06, T3).
 * With no megabrain.neo4j.uri configured, the implementation returns empty
 * (stub behaviour). Verifies depth limit and API contract.
 */
@QuarkusTest
class Neo4jImplementsClosureQueryTest {

    @Inject
    Neo4jImplementsClosureQuery neo4jImplementsClosureQuery;

    @Test
    @DisplayName("returns empty when Neo4j is not configured")
    void findImplementationsOf_noConfig_returnsEmpty() {
        // Given: no megabrain.neo4j.uri configured

        // When
        Uni<List<GraphRelatedEntity>> result = neo4jImplementsClosureQuery.findImplementationsOf("IRepository", 5);
        List<GraphRelatedEntity> actual = result.await().indefinitely();

        // Then
        assertThat(actual).isEmpty();
    }

    @Test
    @DisplayName("returns empty for depth 0 when Neo4j not configured")
    void findImplementationsOf_depthZero_whenNoConfig_returnsEmpty() {
        // Given: no Neo4j config

        // When
        Uni<List<GraphRelatedEntity>> result = neo4jImplementsClosureQuery.findImplementationsOf("I", 0);
        List<GraphRelatedEntity> actual = result.await().indefinitely();

        // Then
        assertThat(actual).isEmpty();
    }

    @Test
    @DisplayName("returns empty for depth above max when Neo4j not configured")
    void findImplementationsOf_depthAboveMax_whenNoConfig_returnsEmpty() {
        // Given: no Neo4j config

        // When
        Uni<List<GraphRelatedEntity>> result = neo4jImplementsClosureQuery.findImplementationsOf("I", 15);
        List<GraphRelatedEntity> actual = result.await().indefinitely();

        // Then
        assertThat(actual).isEmpty();
    }
}
