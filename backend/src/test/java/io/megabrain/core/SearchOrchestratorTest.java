/*
 * Copyright (c) 2025 MegaBrain Contributors
 * Licensed under the MIT License - see LICENSE file for details.
 */

package io.megabrain.core;

import io.megabrain.api.SearchRequest;
import io.smallrye.mutiny.Uni;
import org.apache.lucene.document.Document;
import org.junit.jupiter.api.BeforeEach;
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
 * Unit tests for SearchOrchestrator (US-02-06, T2).
 */
@ExtendWith(MockitoExtension.class)
class SearchOrchestratorTest {

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

    @Test
    void orchestrate_whenTransitiveFalse_returnsHybridResultsAndFacets() {
        SearchRequest request = new SearchRequest("query");
        request.setLimit(10);
        request.setTransitive(false);

        List<ResultMerger.MergedResult> hybridResults = List.of(createMergedResult("id1"));
        Map<String, List<FacetValue>> facets = Map.of("language", List.of(new FacetValue("java", 2)));

        when(hybridIndexService.search(eq("query"), eq(10), eq(SearchMode.HYBRID), nullable(SearchFilters.class), eq(false)))
                .thenReturn(Uni.createFrom().item(hybridResults));
        when(luceneIndexService.computeFacets(eq("query"), nullable(SearchFilters.class), eq(5)))
                .thenReturn(Uni.createFrom().item(facets));

        SearchOrchestrator.OrchestratorResult result = searchOrchestrator
                .orchestrate(request, SearchMode.HYBRID, 5, 5)
                .await().indefinitely();

        assertThat(result.mergedResults()).isEqualTo(hybridResults);
        assertThat(result.facets()).isEqualTo(facets);
        verify(hybridIndexService).search(eq("query"), eq(10), eq(SearchMode.HYBRID), nullable(SearchFilters.class), eq(false));
    }

    @Test
    void orchestrate_whenTransitiveTrueAndGraphEmpty_returnsHybridResultsOnly() {
        SearchRequest request = new SearchRequest("implements:IRepo");
        request.setLimit(10);
        request.setTransitive(true);

        List<ResultMerger.MergedResult> hybridResults = List.of(createMergedResult("id1"));
        Map<String, List<FacetValue>> facets = Map.of();

        when(hybridIndexService.search(any(), anyInt(), eq(SearchMode.HYBRID), nullable(SearchFilters.class), eq(false)))
                .thenReturn(Uni.createFrom().item(hybridResults));
        when(luceneIndexService.computeFacets(any(), nullable(SearchFilters.class), anyInt()))
                .thenReturn(Uni.createFrom().item(facets));
        when(graphQueryService.findRelatedEntities(eq("implements:IRepo"), nullable(SearchFilters.class), eq(5)))
                .thenReturn(Uni.createFrom().item(List.<GraphRelatedEntity>of()));

        SearchOrchestrator.OrchestratorResult result = searchOrchestrator
                .orchestrate(request, SearchMode.HYBRID, 5, 5)
                .await().indefinitely();

        assertThat(result.mergedResults()).hasSize(1);
        assertThat(result.mergedResults()).isEqualTo(hybridResults);
        verify(graphQueryService).findRelatedEntities(eq("implements:IRepo"), nullable(SearchFilters.class), eq(5));
    }

    @Test
    void orchestrate_whenTransitiveTrueAndGraphReturnsEntities_combinesResults() {
        SearchRequest request = new SearchRequest("implements:IRepo");
        request.setLimit(10);
        request.setTransitive(true);

        List<ResultMerger.MergedResult> hybridResults = List.of(createMergedResult("hybrid-id"));
        Map<String, List<FacetValue>> facets = Map.of();
        List<GraphRelatedEntity> relatedEntities = List.of(GraphRelatedEntity.ofName("ConcreteRepo"));
        Document doc = new Document();
        doc.add(new org.apache.lucene.document.StringField("content", "class ConcreteRepo", org.apache.lucene.document.Field.Store.YES));
        doc.add(new org.apache.lucene.document.StringField("entity_name", "ConcreteRepo", org.apache.lucene.document.Field.Store.YES));
        doc.add(new org.apache.lucene.document.StringField("entity_name_keyword", "ConcreteRepo", org.apache.lucene.document.Field.Store.YES));
        doc.add(new org.apache.lucene.document.StringField("entity_type", "class", org.apache.lucene.document.Field.Store.YES));
        doc.add(new org.apache.lucene.document.StringField("source_file", "ConcreteRepo.java", org.apache.lucene.document.Field.Store.YES));
        doc.add(new org.apache.lucene.document.StringField("language", "java", org.apache.lucene.document.Field.Store.YES));
        doc.add(new org.apache.lucene.document.StringField("repository", "repo", org.apache.lucene.document.Field.Store.YES));
        doc.add(new org.apache.lucene.document.StringField("start_line", "1", org.apache.lucene.document.Field.Store.YES));
        doc.add(new org.apache.lucene.document.StringField("end_line", "5", org.apache.lucene.document.Field.Store.YES));
        List<LuceneIndexService.LuceneScoredResult> graphLuceneResults = List.of(
                new LuceneIndexService.LuceneScoredResult(doc, 1.0f, null));
        List<ResultMerger.MergedResult> graphMerged = List.of(
                ResultMerger.MergedResult.fromLucene("graph-id", doc, 1.0));

        when(hybridIndexService.search(any(), anyInt(), eq(SearchMode.HYBRID), nullable(SearchFilters.class), eq(false)))
                .thenReturn(Uni.createFrom().item(hybridResults));
        when(luceneIndexService.computeFacets(any(), nullable(SearchFilters.class), anyInt()))
                .thenReturn(Uni.createFrom().item(facets));
        when(graphQueryService.findRelatedEntities(eq("implements:IRepo"), nullable(SearchFilters.class), eq(5)))
                .thenReturn(Uni.createFrom().item(relatedEntities));
        when(luceneIndexService.lookupByEntityNames(eq(List.of("ConcreteRepo")), eq(10), nullable(SearchFilters.class)))
                .thenReturn(Uni.createFrom().item(graphLuceneResults));
        when(resultMerger.merge(eq(graphLuceneResults), eq(List.of())))
                .thenReturn(graphMerged);

        SearchOrchestrator.OrchestratorResult result = searchOrchestrator
                .orchestrate(request, SearchMode.HYBRID, 5, 5)
                .await().indefinitely();

        assertThat(result.mergedResults()).hasSize(2);
        // Combined list is sorted by score descending: graph (1.0) then hybrid (0.9)
        assertThat(result.mergedResults().get(0).chunkId()).isEqualTo("graph-id");
        assertThat(result.mergedResults().get(1).chunkId()).isEqualTo("hybrid-id");
        verify(graphQueryService).findRelatedEntities(eq("implements:IRepo"), nullable(SearchFilters.class), eq(5));
        verify(luceneIndexService).lookupByEntityNames(eq(List.of("ConcreteRepo")), eq(10), nullable(SearchFilters.class));
    }

    @Test
    void orchestrate_whenTransitiveTrue_passesRequestedDepthToGraph() {
        SearchRequest request = new SearchRequest("extends:Base");
        request.setLimit(10);
        request.setTransitive(true);

        List<ResultMerger.MergedResult> hybridResults = List.of(createMergedResult("id1"));
        Map<String, List<FacetValue>> facets = Map.of();

        when(hybridIndexService.search(any(), anyInt(), eq(SearchMode.HYBRID), nullable(SearchFilters.class), eq(false)))
                .thenReturn(Uni.createFrom().item(hybridResults));
        when(luceneIndexService.computeFacets(any(), nullable(SearchFilters.class), anyInt()))
                .thenReturn(Uni.createFrom().item(facets));
        when(graphQueryService.findRelatedEntities(eq("extends:Base"), nullable(SearchFilters.class), eq(3)))
                .thenReturn(Uni.createFrom().item(List.<GraphRelatedEntity>of()));

        SearchOrchestrator.OrchestratorResult result = searchOrchestrator
                .orchestrate(request, SearchMode.HYBRID, 5, 3)
                .await().indefinitely();

        assertThat(result.mergedResults()).hasSize(1);
        verify(graphQueryService).findRelatedEntities(eq("extends:Base"), nullable(SearchFilters.class), eq(3));
    }

    private static ResultMerger.MergedResult createMergedResult(String chunkId) {
        Document doc = new Document();
        doc.add(new org.apache.lucene.document.StringField("content", "content", org.apache.lucene.document.Field.Store.YES));
        doc.add(new org.apache.lucene.document.StringField("entity_name", "Entity", org.apache.lucene.document.Field.Store.YES));
        doc.add(new org.apache.lucene.document.StringField("entity_type", "class", org.apache.lucene.document.Field.Store.YES));
        doc.add(new org.apache.lucene.document.StringField("source_file", "File.java", org.apache.lucene.document.Field.Store.YES));
        doc.add(new org.apache.lucene.document.StringField("language", "java", org.apache.lucene.document.Field.Store.YES));
        doc.add(new org.apache.lucene.document.StringField("repository", "repo", org.apache.lucene.document.Field.Store.YES));
        doc.add(new org.apache.lucene.document.StringField("start_line", "1", org.apache.lucene.document.Field.Store.YES));
        doc.add(new org.apache.lucene.document.StringField("end_line", "2", org.apache.lucene.document.Field.Store.YES));
        return ResultMerger.MergedResult.fromLucene(chunkId, doc, 0.9);
    }
}
