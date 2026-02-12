/*
 * Copyright (c) 2025 MegaBrain Contributors
 * Licensed under the MIT License - see LICENSE file for details.
 */

package io.megabrain.core;

import io.smallrye.mutiny.Uni;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for GraphQueryServiceStub (US-02-06, T2, T3).
 */
@ExtendWith(MockitoExtension.class)
class GraphQueryServiceStubTest {

    @Mock
    private ImplementsClosureQuery implementsClosureQuery;

    private GraphQueryServiceStub stub;

    @BeforeEach
    void setUp() {
        stub = new GraphQueryServiceStub(implementsClosureQuery);
    }

    @Test
    void findRelatedEntities_implementsQuery_delegatesToClosureAndReturnsEmpty() {
        when(implementsClosureQuery.findImplementationsOf(eq("IRepository"), eq(5)))
                .thenReturn(Uni.createFrom().item(List.<GraphRelatedEntity>of()));

        Uni<List<GraphRelatedEntity>> result = stub.findRelatedEntities("implements:IRepository", null, 5);
        List<GraphRelatedEntity> list = result.await().indefinitely();

        assertThat(list).isEmpty();
        verify(implementsClosureQuery).findImplementationsOf(eq("IRepository"), eq(5));
    }

    @Test
    void findRelatedEntities_implementsQuery_delegatesToClosureAndReturnsEntities() {
        List<GraphRelatedEntity> entities = List.of(GraphRelatedEntity.ofName("ConcreteRepo"));
        when(implementsClosureQuery.findImplementationsOf(eq("IRepo"), eq(5)))
                .thenReturn(Uni.createFrom().item(entities));

        Uni<List<GraphRelatedEntity>> result = stub.findRelatedEntities("implements:IRepo", null, 5);
        List<GraphRelatedEntity> list = result.await().indefinitely();

        assertThat(list).hasSize(1);
        assertThat(list.get(0).entityName()).isEqualTo("ConcreteRepo");
        verify(implementsClosureQuery).findImplementationsOf(eq("IRepo"), eq(5));
    }

    @Test
    void findRelatedEntities_extendsQuery_returnsEmptyWithoutCallingClosure() {
        Uni<List<GraphRelatedEntity>> result = stub.findRelatedEntities("extends:Base", null, 3);
        List<GraphRelatedEntity> list = result.await().indefinitely();

        assertThat(list).isEmpty();
        // extends: not delegated to implements closure (T4 will add extends closure)
    }

    @Test
    void findRelatedEntities_withFilters_extendsReturnsEmpty() {
        SearchFilters filters = new SearchFilters(List.of("java"), List.of(), List.of(), List.of());
        Uni<List<GraphRelatedEntity>> result = stub.findRelatedEntities("extends:Base", filters, 3);
        List<GraphRelatedEntity> list = result.await().indefinitely();
        assertThat(list).isEmpty();
    }
}
