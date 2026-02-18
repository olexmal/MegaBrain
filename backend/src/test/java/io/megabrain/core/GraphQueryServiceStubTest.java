/*
 * Copyright (c) 2025 MegaBrain Contributors
 * Licensed under the MIT License - see LICENSE file for details.
 */

package io.megabrain.core;

import io.smallrye.mutiny.Uni;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
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
 * Unit tests for GraphQueryServiceStub (US-02-06, T2, T3, T4).
 */
@ExtendWith(MockitoExtension.class)
class GraphQueryServiceStubTest {

    @Mock
    private ImplementsClosureQuery implementsClosureQuery;

    @Mock
    private ExtendsClosureQuery extendsClosureQuery;

    private GraphQueryServiceStub stub;

    @BeforeEach
    void setUp() {
        stub = new GraphQueryServiceStub(implementsClosureQuery, extendsClosureQuery);
    }

    @Test
    @DisplayName("delegates to implements closure and returns empty list")
    void findRelatedEntities_implementsQuery_delegatesToClosureAndReturnsEmpty() {
        // Given
        when(implementsClosureQuery.findImplementationsOf(eq("IRepository"), eq(5)))
                .thenReturn(Uni.createFrom().item(List.<GraphRelatedEntity>of()));

        // When
        Uni<List<GraphRelatedEntity>> result = stub.findRelatedEntities("implements:IRepository", null, 5);
        List<GraphRelatedEntity> actual = result.await().indefinitely();

        // Then
        assertThat(actual).isEmpty();
        verify(implementsClosureQuery).findImplementationsOf(eq("IRepository"), eq(5));
    }

    @Test
    @DisplayName("delegates to implements closure and returns entities")
    void findRelatedEntities_implementsQuery_delegatesToClosureAndReturnsEntities() {
        // Given
        List<GraphRelatedEntity> expected = List.of(GraphRelatedEntity.ofName("ConcreteRepo"));
        when(implementsClosureQuery.findImplementationsOf(eq("IRepo"), eq(5)))
                .thenReturn(Uni.createFrom().item(expected));

        // When
        Uni<List<GraphRelatedEntity>> result = stub.findRelatedEntities("implements:IRepo", null, 5);
        List<GraphRelatedEntity> actual = result.await().indefinitely();

        // Then
        assertThat(actual).hasSize(1);
        assertThat(actual.get(0).entityName()).isEqualTo("ConcreteRepo");
        verify(implementsClosureQuery).findImplementationsOf(eq("IRepo"), eq(5));
    }

    @Test
    @DisplayName("delegates to extends closure and returns empty list")
    void findRelatedEntities_extendsQuery_delegatesToExtendsClosureAndReturnsEmpty() {
        // Given
        when(extendsClosureQuery.findSubclassesOf(eq("Base"), eq(3)))
                .thenReturn(Uni.createFrom().item(List.<GraphRelatedEntity>of()));

        // When
        Uni<List<GraphRelatedEntity>> result = stub.findRelatedEntities("extends:Base", null, 3);
        List<GraphRelatedEntity> actual = result.await().indefinitely();

        // Then
        assertThat(actual).isEmpty();
        verify(extendsClosureQuery).findSubclassesOf(eq("Base"), eq(3));
    }

    @Test
    @DisplayName("delegates to extends closure and returns entities")
    void findRelatedEntities_extendsQuery_delegatesToExtendsClosureAndReturnsEntities() {
        // Given
        List<GraphRelatedEntity> expected = List.of(
                GraphRelatedEntity.ofName("SubClassA"),
                GraphRelatedEntity.ofName("SubClassB"));
        when(extendsClosureQuery.findSubclassesOf(eq("BaseClass"), eq(5)))
                .thenReturn(Uni.createFrom().item(expected));

        // When
        Uni<List<GraphRelatedEntity>> result = stub.findRelatedEntities("extends:BaseClass", null, 5);
        List<GraphRelatedEntity> actual = result.await().indefinitely();

        // Then
        assertThat(actual).hasSize(2);
        assertThat(actual.get(0).entityName()).isEqualTo("SubClassA");
        assertThat(actual.get(1).entityName()).isEqualTo("SubClassB");
        verify(extendsClosureQuery).findSubclassesOf(eq("BaseClass"), eq(5));
    }

    @Test
    @DisplayName("extends query with filters delegates to closure")
    void findRelatedEntities_withFilters_extendsDelegatesToClosure() {
        // Given
        when(extendsClosureQuery.findSubclassesOf(eq("Base"), eq(3)))
                .thenReturn(Uni.createFrom().item(List.<GraphRelatedEntity>of()));
        SearchFilters filters = new SearchFilters(List.of("java"), List.of(), List.of(), List.of());

        // When
        Uni<List<GraphRelatedEntity>> result = stub.findRelatedEntities("extends:Base", filters, 3);
        List<GraphRelatedEntity> actual = result.await().indefinitely();

        // Then
        assertThat(actual).isEmpty();
        verify(extendsClosureQuery).findSubclassesOf(eq("Base"), eq(3));
    }

    @Test
    @DisplayName("non-structural query returns empty")
    void findRelatedEntities_nonStructuralQuery_returnsEmpty() {
        // Given: plain query, no implements: or extends:

        // When
        Uni<List<GraphRelatedEntity>> result = stub.findRelatedEntities("authentication service", null, 5);
        List<GraphRelatedEntity> actual = result.await().indefinitely();

        // Then
        assertThat(actual).isEmpty();
    }

    @Test
    @DisplayName("implements query returns entities with relationship path when closure provides path")
    void findRelatedEntities_implementsQuery_returnsEntitiesWithRelationshipPath() {
        // Given: transitive hierarchy interface → abstract → concrete (US-02-06, T6)
        List<String> path = List.of("IRepository", "BaseRepo", "ConcreteRepo");
        List<GraphRelatedEntity> expected = List.of(
                GraphRelatedEntity.ofNameWithPath("ConcreteRepo", path));
        when(implementsClosureQuery.findImplementationsOf(eq("IRepository"), eq(5)))
                .thenReturn(Uni.createFrom().item(expected));

        // When
        Uni<List<GraphRelatedEntity>> result = stub.findRelatedEntities("implements:IRepository", null, 5);
        List<GraphRelatedEntity> actual = result.await().indefinitely();

        // Then
        assertThat(actual).hasSize(1);
        assertThat(actual.get(0).entityName()).isEqualTo("ConcreteRepo");
        assertThat(actual.get(0).relationshipPath()).containsExactly("IRepository", "BaseRepo", "ConcreteRepo");
    }

    @Test
    @DisplayName("extends query returns multiple entities (multiple inheritance paths)")
    void findRelatedEntities_extendsQuery_returnsMultipleEntitiesMultiplePaths() {
        // Given: base class with two subclasses (multiple paths from root)
        List<GraphRelatedEntity> expected = List.of(
                GraphRelatedEntity.ofNameWithPath("SubClassA", List.of("Base", "SubClassA")),
                GraphRelatedEntity.ofNameWithPath("SubClassB", List.of("Base", "SubClassB")));
        when(extendsClosureQuery.findSubclassesOf(eq("Base"), eq(5)))
                .thenReturn(Uni.createFrom().item(expected));

        // When
        Uni<List<GraphRelatedEntity>> result = stub.findRelatedEntities("extends:Base", null, 5);
        List<GraphRelatedEntity> actual = result.await().indefinitely();

        // Then
        assertThat(actual).hasSize(2);
        assertThat(actual.get(0).entityName()).isEqualTo("SubClassA");
        assertThat(actual.get(1).entityName()).isEqualTo("SubClassB");
        assertThat(actual.get(0).relationshipPath()).containsExactly("Base", "SubClassA");
        assertThat(actual.get(1).relationshipPath()).containsExactly("Base", "SubClassB");
    }
}
