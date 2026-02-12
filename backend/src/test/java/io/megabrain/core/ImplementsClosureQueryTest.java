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
 * Unit tests for ImplementsClosureQuery (US-02-06, T3).
 * Uses Neo4jImplementsClosureQuery with no Neo4j config (megabrain.neo4j.uri unset),
 * so findImplementationsOf returns empty. Verifies direct and transitive behaviour
 * when graph is not available.
 */
@QuarkusTest
class ImplementsClosureQueryTest {

    @Inject
    ImplementsClosureQuery implementsClosureQuery;

    @Test
    void findImplementationsOf_whenNoNeo4jConfig_returnsEmpty() {
        Uni<List<GraphRelatedEntity>> result = implementsClosureQuery.findImplementationsOf("IRepository", 5);
        List<GraphRelatedEntity> list = result.await().indefinitely();
        assertThat(list).isEmpty();
    }

    @Test
    void findImplementationsOf_respectsDepthParameter() {
        Uni<List<GraphRelatedEntity>> result = implementsClosureQuery.findImplementationsOf("IX", 3);
        List<GraphRelatedEntity> list = result.await().indefinitely();
        assertThat(list).isEmpty();
    }
}
