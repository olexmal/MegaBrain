/*
 * Copyright (c) 2025 MegaBrain Contributors
 * Licensed under the MIT License - see LICENSE file for details.
 */

package io.megabrain.core;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.util.List;
import java.util.Optional;

/**
 * Stub implementation of {@link GraphQueryService} that delegates "implements" transitive
 * closure to {@link ImplementsClosureQuery} (US-02-06, T3). When the query is an
 * implements-predicate (e.g. {@code implements:IRepository}), returns entities from the
 * closure; otherwise returns empty. Used when no full graph implementation is available
 * (US-06-02, US-06-03); the closure itself may be backed by Neo4j when configured.
 */
@ApplicationScoped
public class GraphQueryServiceStub implements GraphQueryService {

    private static final Logger LOG = Logger.getLogger(GraphQueryServiceStub.class);

    private final ImplementsClosureQuery implementsClosureQuery;

    @Inject
    public GraphQueryServiceStub(ImplementsClosureQuery implementsClosureQuery) {
        this.implementsClosureQuery = implementsClosureQuery;
    }

    @Override
    public Uni<List<GraphRelatedEntity>> findRelatedEntities(String query, SearchFilters filters, int depth) {
        Optional<String> interfaceName = StructuralQueryParser.parseImplementsTarget(query);
        if (interfaceName.isPresent()) {
            LOG.debugf("GraphQueryServiceStub: delegating implements transitive closure for interface=%s depth=%d",
                    interfaceName.get(), depth);
            return implementsClosureQuery.findImplementationsOf(interfaceName.get(), depth);
        }
        // extends: handled in T4; other queries return empty when using stub
        LOG.debugf("GraphQueryServiceStub: no structural predicate, returning empty for query=%s", query);
        return Uni.createFrom().item(List.<GraphRelatedEntity>of());
    }
}
