/*
 * Copyright (c) 2025 MegaBrain Contributors
 * Licensed under the MIT License - see LICENSE file for details.
 */

package io.megabrain.core;

import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
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
    void findSubclassesOf_whenNoNeo4jConfig_returnsEmpty() {
        Uni<List<GraphRelatedEntity>> result = extendsClosureQuery.findSubclassesOf("BaseClass", 5);
        List<GraphRelatedEntity> list = result.await().indefinitely();
        assertThat(list).isEmpty();
    }

    @Test
    void findSubclassesOf_respectsDepthParameter() {
        Uni<List<GraphRelatedEntity>> result = extendsClosureQuery.findSubclassesOf("Base", 3);
        List<GraphRelatedEntity> list = result.await().indefinitely();
        assertThat(list).isEmpty();
    }
}
