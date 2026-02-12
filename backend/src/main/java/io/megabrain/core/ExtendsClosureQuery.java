/*
 * Copyright (c) 2025 MegaBrain Contributors
 * Licensed under the MIT License - see LICENSE file for details.
 */

package io.megabrain.core;

import io.smallrye.mutiny.Uni;

import java.util.List;

/**
 * Transitive closure for "extends" relationships (US-02-06, T4).
 * Given a class name and a depth limit, returns all entities (subclasses)
 * that extend the class directly or transitively via EXTENDS edges.
 * <p>
 * Handles multiple inheritance paths by deduplicating results so each subclass
 * appears at most once. Implementations may be backed by Neo4j (Cypher) or
 * a stub that returns empty when the graph is not available.
 */
public interface ExtendsClosureQuery {

    /**
     * Finds all subclasses of the given class directly or transitively,
     * up to the specified traversal depth.
     * <p>
     * Depth is applied to the variable-length EXTENDS path. Results are
     * deduplicated so each entity appears at most once even if reachable
     * via multiple paths.
     *
     * @param className canonical class name (e.g. BaseClass)
     * @param depth      maximum traversal depth (1–10); limits path length to prevent performance issues
     * @return list of related entities (possibly empty); never null
     */
    Uni<List<GraphRelatedEntity>> findSubclassesOf(String className, int depth);
}
