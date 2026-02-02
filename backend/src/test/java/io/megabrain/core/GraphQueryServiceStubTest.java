/*
 * Copyright (c) 2025 MegaBrain Contributors
 * Licensed under the MIT License - see LICENSE file for details.
 */

package io.megabrain.core;

import io.smallrye.mutiny.Uni;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for GraphQueryServiceStub (US-02-06, T2).
 */
class GraphQueryServiceStubTest {

    private final GraphQueryServiceStub stub = new GraphQueryServiceStub();

    @Test
    void findRelatedEntities_returnsEmptyList() {
        Uni<List<GraphRelatedEntity>> result = stub.findRelatedEntities("implements:IRepository", null, 5);
        List<GraphRelatedEntity> list = result.await().indefinitely();
        assertThat(list).isEmpty();
    }

    @Test
    void findRelatedEntities_withFilters_returnsEmptyList() {
        SearchFilters filters = new SearchFilters(List.of("java"), List.of(), List.of(), List.of());
        Uni<List<GraphRelatedEntity>> result = stub.findRelatedEntities("extends:Base", filters, 3);
        List<GraphRelatedEntity> list = result.await().indefinitely();
        assertThat(list).isEmpty();
    }
}
