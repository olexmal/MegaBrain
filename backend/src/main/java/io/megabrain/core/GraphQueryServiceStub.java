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
 * closure to {@link ImplementsClosureQuery} (US-02-06, T3) and "extends" to
 * {@link ExtendsClosureQuery} (US-02-06, T4). When the query is an implements-predicate
 * (e.g. {@code implements:IRepository}) or extends-predicate (e.g. {@code extends:BaseClass}),
 * returns entities from the corresponding closure; otherwise returns empty. Used when no full
 * graph implementation is available (US-06-02, US-06-03); closures may be backed by Neo4j when configured.
 */
@ApplicationScoped
public class GraphQueryServiceStub implements GraphQueryService {

    private static final Logger LOG = Logger.getLogger(GraphQueryServiceStub.class);

    private final ImplementsClosureQuery implementsClosureQuery;
    private final ExtendsClosureQuery extendsClosureQuery;

    @Inject
    public GraphQueryServiceStub(ImplementsClosureQuery implementsClosureQuery,
            ExtendsClosureQuery extendsClosureQuery) {
        this.implementsClosureQuery = implementsClosureQuery;
        this.extendsClosureQuery = extendsClosureQuery;
    }

    @Override
    public Uni<List<GraphRelatedEntity>> findRelatedEntities(String query, SearchFilters filters, int depth) {
        Optional<String> interfaceName = StructuralQueryParser.parseImplementsTarget(query);
        if (interfaceName.isPresent()) {
            LOG.debugf("GraphQueryServiceStub: delegating implements transitive closure for interface=%s depth=%d",
                    interfaceName.get(), depth);
            return implementsClosureQuery.findImplementationsOf(interfaceName.get(), depth);
        }
        Optional<String> className = StructuralQueryParser.parseExtendsTarget(query);
        if (className.isPresent()) {
            LOG.debugf("GraphQueryServiceStub: delegating extends transitive closure for class=%s depth=%d",
                    className.get(), depth);
            return extendsClosureQuery.findSubclassesOf(className.get(), depth);
        }
        LOG.debugf("GraphQueryServiceStub: no structural predicate, returning empty for query=%s", query);
        return Uni.createFrom().item(List.<GraphRelatedEntity>of());
    }
}
