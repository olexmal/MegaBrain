/*
 * Copyright (c) 2025 MegaBrain Contributors
 * Licensed under the MIT License - see LICENSE file for details.
 */

package io.megabrain.core;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Coordinates Lucene/hybrid search with graph queries for transitive search (US-02-06, T2).
 * When {@code transitive=true}, runs hybrid search and graph-related-entity lookup in parallel,
 * then combines results (deduplicating by chunk id) so that graph-originated entities are
 * included in the response.
 * <p>
 * When the graph returns no related entities (e.g. stub implementation), behaviour is
 * equivalent to non-transitive search plus facets.
 */
@ApplicationScoped
public class SearchOrchestrator {

    private static final Logger LOG = Logger.getLogger(SearchOrchestrator.class);

    /** Default traversal depth for graph queries until T5 adds per-request/config depth. */
    private static final int DEFAULT_GRAPH_DEPTH = 5;

    private final HybridIndexService hybridIndexService;
    private final LuceneIndexService luceneIndexService;
    private final GraphQueryService graphQueryService;
    private final ResultMerger resultMerger;

    @Inject
    public SearchOrchestrator(
            @IndexType(IndexType.Type.HYBRID) HybridIndexService hybridIndexService,
            @IndexType(IndexType.Type.LUCENE) LuceneIndexService luceneIndexService,
            GraphQueryService graphQueryService,
            ResultMerger resultMerger) {
        this.hybridIndexService = hybridIndexService;
        this.luceneIndexService = luceneIndexService;
        this.graphQueryService = graphQueryService;
        this.resultMerger = resultMerger;
    }

    /**
     * Result of orchestrated search: merged results (hybrid + optional graph) and facets.
     */
    public record OrchestratorResult(
            List<ResultMerger.MergedResult> mergedResults,
            Map<String, List<FacetValue>> facets
    ) {}

    /**
     * Runs search with optional transitive graph integration.
     * When {@code request.isTransitive()} is true, graph queries run in parallel with hybrid search
     * and related entities are resolved via Lucene and merged into the result list (deduplicated).
     *
     * @param request the search request (query, filters, limit, transitive flag)
     * @param mode    search mode (hybrid, keyword, vector)
     * @param facetLimit max facet values per field
     * @return combined merged results and facets
     */
    public Uni<OrchestratorResult> orchestrate(io.megabrain.api.SearchRequest request,
                                               SearchMode mode,
                                               int facetLimit) {
        String query = request.getQuery();
        int limit = request.getLimit();
        SearchFilters filters = request.hasFilters()
                ? new SearchFilters(
                        request.getLanguages(),
                        request.getRepositories(),
                        request.getFilePaths(),
                        request.getEntityTypes())
                : null;

        Uni<List<ResultMerger.MergedResult>> hybridUni = hybridIndexService.search(
                query, limit, mode, filters, request.isIncludeFieldMatch());

        Uni<Map<String, List<FacetValue>>> facetsUni = (mode == SearchMode.VECTOR)
                ? Uni.createFrom().item(Map.of())
                : luceneIndexService.computeFacets(query, filters, facetLimit)
                        .onFailure().recoverWithItem(Map.of());

        if (!request.isTransitive()) {
            return Uni.combine().all().unis(hybridUni, facetsUni).asTuple()
                    .map(tuple -> new OrchestratorResult(tuple.getItem1(), tuple.getItem2()));
        }

        // Transitive: run hybrid + facets + graph in parallel
        Uni<List<GraphRelatedEntity>> graphUni = graphQueryService.findRelatedEntities(
                query, filters, DEFAULT_GRAPH_DEPTH);

        return Uni.combine().all().unis(hybridUni, facetsUni, graphUni).asTuple()
                .flatMap(tuple -> {
                    List<ResultMerger.MergedResult> hybridResults = tuple.getItem1();
                    Map<String, List<FacetValue>> facets = tuple.getItem2();
                    List<GraphRelatedEntity> relatedEntities = tuple.getItem3();

                    if (relatedEntities == null || relatedEntities.isEmpty()) {
                        return Uni.createFrom().item(new OrchestratorResult(hybridResults, facets));
                    }

                    List<String> entityNames = relatedEntities.stream()
                            .map(GraphRelatedEntity::entityName)
                            .filter(n -> n != null && !n.isBlank())
                            .distinct()
                            .toList();

                    if (entityNames.isEmpty()) {
                        return Uni.createFrom().item(new OrchestratorResult(hybridResults, facets));
                    }

                    return luceneIndexService.lookupByEntityNames(entityNames, limit, filters)
                            .map(graphLuceneResults -> {
                                List<ResultMerger.MergedResult> graphMerged = resultMerger.merge(
                                        graphLuceneResults, List.of());
                                List<ResultMerger.MergedResult> combined = combineAndDedupe(hybridResults, graphMerged);
                                LOG.debugf("Transitive search: hybrid=%d, graph entities=%d, graph docs=%d, combined=%d",
                                        hybridResults.size(), entityNames.size(), graphMerged.size(), combined.size());
                                return new OrchestratorResult(combined, facets);
                            });
                });
    }

    /**
     * Combines hybrid and graph-originated results, deduplicating by chunk id.
     * Hybrid results keep their order; graph results that are not already present are appended,
     * then the full list is sorted by combined score descending.
     */
    private List<ResultMerger.MergedResult> combineAndDedupe(
            List<ResultMerger.MergedResult> hybridResults,
            List<ResultMerger.MergedResult> graphResults) {
        if (graphResults == null || graphResults.isEmpty()) {
            return hybridResults;
        }
        Set<String> seenIds = new HashSet<>(hybridResults.stream()
                .map(ResultMerger.MergedResult::chunkId)
                .toList());
        List<ResultMerger.MergedResult> combined = new ArrayList<>(hybridResults);
        for (ResultMerger.MergedResult r : graphResults) {
            if (seenIds.add(r.chunkId())) {
                combined.add(r);
            }
        }
        combined.sort(Comparator.comparing(ResultMerger.MergedResult::combinedScore).reversed());
        return combined;
    }
}
