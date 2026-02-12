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
 * Unit tests for ExtendsClosureQuery (US-02-06, T4).
 * Uses Neo4jExtendsClosureQuery with no Neo4j config (megabrain.neo4j.uri unset),
 * so findSubclassesOf returns empty. Verifies direct and transitive behaviour
 * when graph is not available.
 */
@QuarkusTest
class ExtendsClosureQueryTest {

    @Inject
    ExtendsClosureQuery extendsClosureQuery;

    @Test
    @DisplayName("returns empty when Neo4j is not configured")
    void findSubclassesOf_whenNoNeo4jConfig_returnsEmpty() {
        // Given: no megabrain.neo4j.uri configured

        // When
        Uni<List<GraphRelatedEntity>> result = extendsClosureQuery.findSubclassesOf("BaseClass", 5);
        List<GraphRelatedEntity> actual = result.await().indefinitely();

        // Then
        assertThat(actual).isEmpty();
    }

    @Test
    @DisplayName("returns empty for given depth when Neo4j not configured")
    void findSubclassesOf_withDepth_whenNoNeo4jConfig_returnsEmpty() {
        // Given: no Neo4j config

        // When
        Uni<List<GraphRelatedEntity>> result = extendsClosureQuery.findSubclassesOf("Base", 3);
        List<GraphRelatedEntity> actual = result.await().indefinitely();

        // Then
        assertThat(actual).isEmpty();
    }
}
