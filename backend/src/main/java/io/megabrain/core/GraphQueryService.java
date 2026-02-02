/*
 * Copyright (c) 2025 MegaBrain Contributors
 * Licensed under the MIT License - see LICENSE file for details.
 */

package io.megabrain.core;

import io.smallrye.mutiny.Uni;

import java.util.List;

/**
 * Abstraction over the graph database for structural queries (implements, extends).
 * When transitive search is enabled, the search pipeline uses this service to find
 * related entities and combines them with Lucene search results (US-02-06, T2).
 * <p>
 * Implementations may be backed by Neo4j/JanusGraph (US-06-02, US-06-03) or a stub
 * that returns empty results when the graph is not available.
 */
public interface GraphQueryService {

    /**
     * Finds entities related to the given structural query (e.g. implements:IRepository,
     * extends:BaseClass) up to the specified traversal depth.
     * <p>
     * When the graph is not populated or not available, implementations should return
     * an empty list rather than failing.
     *
     * @param query   the search query (may contain structural predicates like implements:, extends:)
     * @param filters optional metadata filters to restrict results; null or empty to skip
     * @param depth   maximum traversal depth (1–10); default 5 when not yet configurable
     * @return list of related entities (possibly empty); never null
     */
    Uni<List<GraphRelatedEntity>> findRelatedEntities(String query, SearchFilters filters, int depth);
}
