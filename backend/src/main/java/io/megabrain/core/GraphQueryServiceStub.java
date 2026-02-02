/*
 * Copyright (c) 2025 MegaBrain Contributors
 * Licensed under the MIT License - see LICENSE file for details.
 */

package io.megabrain.core;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;

import java.util.List;

/**
 * Stub implementation of {@link GraphQueryService} that returns no related entities.
 * Used when the graph database is not yet available (US-06-02, US-06-03) so that
 * transitive search can be integrated without failing (US-02-06, T2).
 * <p>
 * Replace with a real implementation (e.g. Neo4j) when graph storage and incoming
 * queries are implemented.
 */
@ApplicationScoped
public class GraphQueryServiceStub implements GraphQueryService {

    private static final Logger LOG = Logger.getLogger(GraphQueryServiceStub.class);

    @Override
    public Uni<List<GraphRelatedEntity>> findRelatedEntities(String query, SearchFilters filters, int depth) {
        LOG.debugf("GraphQueryServiceStub: no graph available, returning empty related entities for query=%s", query);
        return Uni.createFrom().item(List.<GraphRelatedEntity>of());
    }
}
