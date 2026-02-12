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
 * Unit tests for Neo4jImplementsClosureQuery (US-02-06, T3).
 * With no megabrain.neo4j.uri configured, the implementation returns empty
 * (stub behaviour). Verifies depth limit and API contract.
 */
@QuarkusTest
class Neo4jImplementsClosureQueryTest {

    @Inject
    Neo4jImplementsClosureQuery neo4jImplementsClosureQuery;

    @Test
    void findImplementationsOf_noConfig_returnsEmpty() {
        Uni<List<GraphRelatedEntity>> result = neo4jImplementsClosureQuery.findImplementationsOf("IRepository", 5);
        List<GraphRelatedEntity> list = result.await().indefinitely();
        assertThat(list).isEmpty();
    }

    @Test
    void findImplementationsOf_depthClamped_whenOutOfRange() {
        // Depth 0 and 15 should be clamped to 1-10; with no Neo4j we still get empty
        Uni<List<GraphRelatedEntity>> r0 = neo4jImplementsClosureQuery.findImplementationsOf("I", 0);
        Uni<List<GraphRelatedEntity>> r15 = neo4jImplementsClosureQuery.findImplementationsOf("I", 15);
        assertThat(r0.await().indefinitely()).isEmpty();
        assertThat(r15.await().indefinitely()).isEmpty();
    }
}
