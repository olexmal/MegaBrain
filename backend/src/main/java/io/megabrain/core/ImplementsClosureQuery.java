/*
 * Copyright (c) 2025 MegaBrain Contributors
 * Licensed under the MIT License - see LICENSE file for details.
 */

package io.megabrain.core;

import io.smallrye.mutiny.Uni;

import java.util.List;

/**
 * Transitive closure for "implements" relationships (US-02-06, T3).
 * Given an interface name and a depth limit, returns all entities (classes or abstract classes)
 * that implement the interface directly or transitively through IMPLEMENTS/EXTENDS edges.
 * <p>
 * Implementations may be backed by Neo4j (Cypher) or a stub that returns empty when the graph
 * is not available.
 */
public interface ImplementsClosureQuery {

    /**
     * Finds all entities that implement the given interface directly or transitively
     * (through abstract classes), up to the specified traversal depth.
     * <p>
     * Depth is applied to the variable-length path (IMPLEMENTS and EXTENDS). Results are
     * deduplicated so each entity appears at most once even if reachable via multiple paths.
     *
     * @param interfaceName canonical interface name (e.g. IRepository)
     * @param depth         maximum traversal depth (1–10); limits path length to prevent performance issues
     * @return list of related entities (possibly empty); never null
     */
    Uni<List<GraphRelatedEntity>> findImplementationsOf(String interfaceName, int depth);
}
