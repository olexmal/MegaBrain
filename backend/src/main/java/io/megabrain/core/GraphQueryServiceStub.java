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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Stub implementation of {@link GraphQueryService} that delegates "implements" transitive
 * closure to {@link ImplementsClosureQuery} (US-02-06, T3), "extends" to
 * {@link ExtendsClosureQuery} (US-02-06, T4), and "usages" (AC3) to both closures so that
 * find-usages includes the type and all implementations/subclasses (polymorphic call sites).
 * When the query is an implements-predicate, extends-predicate, or usages-predicate,
 * returns the corresponding related entities; otherwise returns empty.
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
        Optional<String> usagesTarget = StructuralQueryParser.parseUsagesTarget(query);
        if (usagesTarget.isPresent()) {
            String typeName = usagesTarget.get();
            LOG.debugf("GraphQueryServiceStub: usages transitive closure for type=%s depth=%d (AC3)", typeName, depth);
            Uni<List<GraphRelatedEntity>> implUni = implementsClosureQuery.findImplementationsOf(typeName, depth);
            Uni<List<GraphRelatedEntity>> extendsUni = extendsClosureQuery.findSubclassesOf(typeName, depth);
            return Uni.combine().all().unis(implUni, extendsUni).asTuple()
                    .map(tuple -> {
                        List<GraphRelatedEntity> impls = tuple.getItem1() != null ? tuple.getItem1() : List.of();
                        List<GraphRelatedEntity> subs = tuple.getItem2() != null ? tuple.getItem2() : List.of();
                        Set<String> seen = new LinkedHashSet<>();
                        List<GraphRelatedEntity> result = new ArrayList<>();
                        result.add(GraphRelatedEntity.ofName(typeName));
                        seen.add(typeName);
                        Stream.concat(impls.stream(), subs.stream())
                                .filter(e -> e.entityName() != null && !e.entityName().isBlank())
                                .filter(e -> seen.add(e.entityName()))
                                .forEach(result::add);
                        return result;
                    });
        }
        LOG.debugf("GraphQueryServiceStub: no structural predicate, returning empty for query=%s", query);
        return Uni.createFrom().item(List.<GraphRelatedEntity>of());
    }
}
