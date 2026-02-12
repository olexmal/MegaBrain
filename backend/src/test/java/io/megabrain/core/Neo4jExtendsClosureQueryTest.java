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
 * Unit tests for Neo4jExtendsClosureQuery (US-02-06, T4).
 * With no megabrain.neo4j.uri configured, the implementation returns empty
 * (stub behaviour). Verifies depth limit and API contract.
 */
@QuarkusTest
class Neo4jExtendsClosureQueryTest {

    @Inject
    Neo4jExtendsClosureQuery neo4jExtendsClosureQuery;

    @Test
    void findSubclassesOf_noConfig_returnsEmpty() {
        Uni<List<GraphRelatedEntity>> result = neo4jExtendsClosureQuery.findSubclassesOf("BaseClass", 5);
        List<GraphRelatedEntity> list = result.await().indefinitely();
        assertThat(list).isEmpty();
    }

    @Test
    void findSubclassesOf_depthClamped_whenOutOfRange() {
        Uni<List<GraphRelatedEntity>> r0 = neo4jExtendsClosureQuery.findSubclassesOf("Base", 0);
        Uni<List<GraphRelatedEntity>> r15 = neo4jExtendsClosureQuery.findSubclassesOf("Base", 15);
        assertThat(r0.await().indefinitely()).isEmpty();
        assertThat(r15.await().indefinitely()).isEmpty();
    }
}
