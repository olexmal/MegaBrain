/*
 * Copyright (c) 2025 MegaBrain Contributors
 * Licensed under the MIT License - see LICENSE file for details.
 */

package io.megabrain.core;

import io.megabrain.api.SearchRequest;
import io.smallrye.mutiny.Uni;
import org.apache.lucene.document.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Comprehensive tests for transitive search functionality (US-02-06, T7).
 * Covers implements transitive closure, extends transitive closure, depth limits,
 * and result marking using a mocked graph with known relationship hierarchy.
 */
@ExtendWith(MockitoExtension.class)
class TransitiveSearchComprehensiveTest {

    @Mock
    private HybridIndexService hybridIndexService;

    @Mock
    private LuceneIndexService luceneIndexService;

    @Mock
    private GraphQueryService graphQueryService;

    @Mock
    private ResultMerger resultMerger;

    private SearchOrchestrator searchOrchestrator;

    @BeforeEach
    void setUp() {
        searchOrchestrator = new SearchOrchestrator(
                hybridIndexService, luceneIndexService, graphQueryService, resultMerger);
    }

    @Nested
    @DisplayName("Implements transitive closure")
    class ImplementsTransitiveClosure {

        @Test
        @DisplayName("finds direct and transitive implementations when graph returns interface→abstract→concrete")
        void implementsQuery_withGraphHierarchy_combinesAndMarksResults() {
            // Given: test graph hierarchy IRepository → BaseRepo → ConcreteRepo
            SearchRequest request = new SearchRequest("implements:IRepository");
            request.setLimit(10);
            request.setTransitive(true);

            List<ResultMerger.MergedResult> hybridResults = List.of(createMergedResult("hybrid-1", "OtherClass"));
            Map<String, List<FacetValue>> facets = Map.of();
            List<String> path = List.of("IRepository", "BaseRepo", "ConcreteRepo");
            List<GraphRelatedEntity> relatedEntities = List.of(
                    GraphRelatedEntity.ofNameWithPath("ConcreteRepo", path));

            Document doc = docForEntity("ConcreteRepo", "ConcreteRepo.java");
            List<LuceneIndexService.LuceneScoredResult> graphLuceneResults = List.of(
                    new LuceneIndexService.LuceneScoredResult(doc, 1.0f, null));
            List<ResultMerger.MergedResult> graphMerged = List.of(
                    ResultMerger.MergedResult.fromLucene("graph-concrete", doc, 1.0));

            when(hybridIndexService.search(any(), anyInt(), eq(SearchMode.HYBRID), nullable(SearchFilters.class), eq(false)))
                    .thenReturn(Uni.createFrom().item(hybridResults));
            when(luceneIndexService.computeFacets(any(), nullable(SearchFilters.class), anyInt()))
                    .thenReturn(Uni.createFrom().item(facets));
            when(graphQueryService.findRelatedEntities(eq("implements:IRepository"), nullable(SearchFilters.class), eq(5)))
                    .thenReturn(Uni.createFrom().item(relatedEntities));
            when(luceneIndexService.lookupByEntityNames(eq(List.of("ConcreteRepo")), eq(10), nullable(SearchFilters.class)))
                    .thenReturn(Uni.createFrom().item(graphLuceneResults));
            when(resultMerger.merge(eq(graphLuceneResults), eq(List.of())))
                    .thenReturn(graphMerged);

            // When
            SearchOrchestrator.OrchestratorResult result = searchOrchestrator
                    .orchestrate(request, SearchMode.HYBRID, 5, 5)
                    .await().indefinitely();

            // Then: combined results, graph result marked with transitive path
            assertThat(result.mergedResults()).hasSize(2);
            ResultMerger.MergedResult graphResult = result.mergedResults().stream()
                    .filter(mr -> "graph-concrete".equals(mr.chunkId()))
                    .findFirst()
                    .orElseThrow();
            assertThat(graphResult.transitivePath())
                    .containsExactly("IRepository", "BaseRepo", "ConcreteRepo");
            verify(graphQueryService).findRelatedEntities(eq("implements:IRepository"), nullable(SearchFilters.class), eq(5));
        }
    }

    @Nested
    @DisplayName("Extends transitive closure")
    class ExtendsTransitiveClosure {

        @Test
        @DisplayName("finds direct and transitive subclasses when graph returns base→subA, subB")
        void extendsQuery_withGraphHierarchy_combinesAndMarksResults() {
            // Given: test graph Base → SubClassA, SubClassB (multiple paths)
            SearchRequest request = new SearchRequest("extends:Base");
            request.setLimit(10);
            request.setTransitive(true);

            List<ResultMerger.MergedResult> hybridResults = List.of();
            Map<String, List<FacetValue>> facets = Map.of();
            List<GraphRelatedEntity> relatedEntities = List.of(
                    GraphRelatedEntity.ofNameWithPath("SubClassA", List.of("Base", "SubClassA")),
                    GraphRelatedEntity.ofNameWithPath("SubClassB", List.of("Base", "SubClassB")));

            Document docA = docForEntity("SubClassA", "SubClassA.java");
            Document docB = docForEntity("SubClassB", "SubClassB.java");
            List<LuceneIndexService.LuceneScoredResult> graphLuceneResults = List.of(
                    new LuceneIndexService.LuceneScoredResult(docA, 0.9f, null),
                    new LuceneIndexService.LuceneScoredResult(docB, 0.8f, null));
            List<ResultMerger.MergedResult> graphMerged = List.of(
                    ResultMerger.MergedResult.fromLucene("graph-a", docA, 0.9),
                    ResultMerger.MergedResult.fromLucene("graph-b", docB, 0.8));

            when(hybridIndexService.search(any(), anyInt(), eq(SearchMode.HYBRID), nullable(SearchFilters.class), eq(false)))
                    .thenReturn(Uni.createFrom().item(hybridResults));
            when(luceneIndexService.computeFacets(any(), nullable(SearchFilters.class), anyInt()))
                    .thenReturn(Uni.createFrom().item(facets));
            when(graphQueryService.findRelatedEntities(eq("extends:Base"), nullable(SearchFilters.class), eq(5)))
                    .thenReturn(Uni.createFrom().item(relatedEntities));
            when(luceneIndexService.lookupByEntityNames(eq(List.of("SubClassA", "SubClassB")), eq(10), nullable(SearchFilters.class)))
                    .thenReturn(Uni.createFrom().item(graphLuceneResults));
            when(resultMerger.merge(eq(graphLuceneResults), eq(List.of())))
                    .thenReturn(graphMerged);

            // When
            SearchOrchestrator.OrchestratorResult result = searchOrchestrator
                    .orchestrate(request, SearchMode.HYBRID, 5, 5)
                    .await().indefinitely();

            // Then: both subclasses present, each with correct path
            assertThat(result.mergedResults()).hasSize(2);
            ResultMerger.MergedResult mergedA = result.mergedResults().stream()
                    .filter(mr -> "graph-a".equals(mr.chunkId()))
                    .findFirst()
                    .orElseThrow();
            ResultMerger.MergedResult mergedB = result.mergedResults().stream()
                    .filter(mr -> "graph-b".equals(mr.chunkId()))
                    .findFirst()
                    .orElseThrow();
            assertThat(mergedA.transitivePath()).containsExactly("Base", "SubClassA");
            assertThat(mergedB.transitivePath()).containsExactly("Base", "SubClassB");
            verify(graphQueryService).findRelatedEntities(eq("extends:Base"), nullable(SearchFilters.class), eq(5));
        }
    }

    @Nested
    @DisplayName("Depth limits")
    class DepthLimits {

        @Test
        @DisplayName("depth 1 passed to graph for implements query")
        void transitiveWithDepthOne_passesDepthOneToGraph() {
            SearchRequest request = new SearchRequest("implements:IX");
            request.setLimit(10);
            request.setTransitive(true);

            when(hybridIndexService.search(any(), anyInt(), eq(SearchMode.HYBRID), nullable(SearchFilters.class), eq(false)))
                    .thenReturn(Uni.createFrom().item(List.of()));
            when(luceneIndexService.computeFacets(any(), nullable(SearchFilters.class), anyInt()))
                    .thenReturn(Uni.createFrom().item(Map.of()));
            when(graphQueryService.findRelatedEntities(eq("implements:IX"), nullable(SearchFilters.class), eq(1)))
                    .thenReturn(Uni.createFrom().item(List.<GraphRelatedEntity>of()));

            searchOrchestrator.orchestrate(request, SearchMode.HYBRID, 5, 1).await().indefinitely();

            verify(graphQueryService).findRelatedEntities(eq("implements:IX"), nullable(SearchFilters.class), eq(1));
        }

        @Test
        @DisplayName("max depth 10 passed to graph for extends query")
        void transitiveWithDepthTen_passesDepthTenToGraph() {
            SearchRequest request = new SearchRequest("extends:Base");
            request.setLimit(10);
            request.setTransitive(true);

            when(hybridIndexService.search(any(), anyInt(), eq(SearchMode.HYBRID), nullable(SearchFilters.class), eq(false)))
                    .thenReturn(Uni.createFrom().item(List.of()));
            when(luceneIndexService.computeFacets(any(), nullable(SearchFilters.class), anyInt()))
                    .thenReturn(Uni.createFrom().item(Map.of()));
            when(graphQueryService.findRelatedEntities(eq("extends:Base"), nullable(SearchFilters.class), eq(10)))
                    .thenReturn(Uni.createFrom().item(List.<GraphRelatedEntity>of()));

            searchOrchestrator.orchestrate(request, SearchMode.HYBRID, 5, 10).await().indefinitely();

            verify(graphQueryService).findRelatedEntities(eq("extends:Base"), nullable(SearchFilters.class), eq(10));
        }
    }

    @Nested
    @DisplayName("Result marking")
    class ResultMarking {

        @Test
        @DisplayName("transitive results have path, direct (hybrid-only) results have no path")
        void transitiveResultsMarkedWithPath_directResultsHaveNoPath() {
            SearchRequest request = new SearchRequest("implements:IRepo");
            request.setLimit(10);
            request.setTransitive(true);

            ResultMerger.MergedResult hybridOnly = createMergedResult("direct-id", "DirectClass");
            List<ResultMerger.MergedResult> hybridResults = List.of(hybridOnly);
            Map<String, List<FacetValue>> facets = Map.of();
            List<GraphRelatedEntity> relatedEntities = List.of(
                    GraphRelatedEntity.ofNameWithPath("TransitiveClass", List.of("IRepo", "TransitiveClass")));

            Document doc = docForEntity("TransitiveClass", "TransitiveClass.java");
            List<LuceneIndexService.LuceneScoredResult> graphLuceneResults = List.of(
                    new LuceneIndexService.LuceneScoredResult(doc, 1.0f, null));
            List<ResultMerger.MergedResult> graphMerged = List.of(
                    ResultMerger.MergedResult.fromLucene("transitive-id", doc, 1.0));

            when(hybridIndexService.search(any(), anyInt(), eq(SearchMode.HYBRID), nullable(SearchFilters.class), eq(false)))
                    .thenReturn(Uni.createFrom().item(hybridResults));
            when(luceneIndexService.computeFacets(any(), nullable(SearchFilters.class), anyInt()))
                    .thenReturn(Uni.createFrom().item(facets));
            when(graphQueryService.findRelatedEntities(eq("implements:IRepo"), nullable(SearchFilters.class), eq(5)))
                    .thenReturn(Uni.createFrom().item(relatedEntities));
            when(luceneIndexService.lookupByEntityNames(eq(List.of("TransitiveClass")), eq(10), nullable(SearchFilters.class)))
                    .thenReturn(Uni.createFrom().item(graphLuceneResults));
            when(resultMerger.merge(eq(graphLuceneResults), eq(List.of())))
                    .thenReturn(graphMerged);

            SearchOrchestrator.OrchestratorResult result = searchOrchestrator
                    .orchestrate(request, SearchMode.HYBRID, 5, 5)
                    .await().indefinitely();

            ResultMerger.MergedResult directResult = result.mergedResults().stream()
                    .filter(mr -> "direct-id".equals(mr.chunkId()))
                    .findFirst()
                    .orElseThrow();
            ResultMerger.MergedResult transitiveResult = result.mergedResults().stream()
                    .filter(mr -> "transitive-id".equals(mr.chunkId()))
                    .findFirst()
                    .orElseThrow();

            assertThat(directResult.transitivePath()).isNull();
            assertThat(transitiveResult.transitivePath()).containsExactly("IRepo", "TransitiveClass");
        }
    }

    private static ResultMerger.MergedResult createMergedResult(String chunkId, String entityName) {
        Document doc = docForEntity(entityName, entityName + ".java");
        return ResultMerger.MergedResult.fromLucene(chunkId, doc, 0.9);
    }

    private static Document docForEntity(String entityName, String sourceFile) {
        Document doc = new Document();
        doc.add(new org.apache.lucene.document.StringField("content", "content", org.apache.lucene.document.Field.Store.YES));
        doc.add(new org.apache.lucene.document.StringField("entity_name", entityName, org.apache.lucene.document.Field.Store.YES));
        doc.add(new org.apache.lucene.document.StringField("entity_name_keyword", entityName, org.apache.lucene.document.Field.Store.YES));
        doc.add(new org.apache.lucene.document.StringField("entity_type", "class", org.apache.lucene.document.Field.Store.YES));
        doc.add(new org.apache.lucene.document.StringField("source_file", sourceFile, org.apache.lucene.document.Field.Store.YES));
        doc.add(new org.apache.lucene.document.StringField("language", "java", org.apache.lucene.document.Field.Store.YES));
        doc.add(new org.apache.lucene.document.StringField("repository", "repo", org.apache.lucene.document.Field.Store.YES));
        doc.add(new org.apache.lucene.document.StringField("start_line", "1", org.apache.lucene.document.Field.Store.YES));
        doc.add(new org.apache.lucene.document.StringField("end_line", "2", org.apache.lucene.document.Field.Store.YES));
        return doc;
    }
}
