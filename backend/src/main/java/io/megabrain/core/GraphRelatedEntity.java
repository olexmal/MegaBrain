/*
 * Copyright (c) 2025 MegaBrain Contributors
 * Licensed under the MIT License - see LICENSE file for details.
 */

package io.megabrain.core;

import java.util.List;

/**
 * Represents an entity found via graph traversal (implements/extends relationships).
 * Used to resolve graph results to Lucene documents for inclusion in search results (US-02-06, T2).
 * Optional {@code relationshipPath} (US-02-06, T6) shows the traversal path, e.g.
 * ["Interface", "AbstractClass", "ConcreteClass"].
 *
 * @param entityName       canonical entity name (e.g. class or interface name)
 * @param entityType       optional entity type (class, interface, etc.)
 * @param sourceFile       optional source file path when known from graph
 * @param relationshipPath optional traversal path from root to this entity; null when not available
 */
public record GraphRelatedEntity(String entityName, String entityType, String sourceFile,
                                 List<String> relationshipPath) {

    /**
     * Creates a related entity with only the name (type, source file, and path null).
     *
     * @param entityName the entity name
     * @return a new GraphRelatedEntity
     */
    public static GraphRelatedEntity ofName(String entityName) {
        return new GraphRelatedEntity(entityName, null, null, null);
    }

    /**
     * Creates a related entity with name and optional relationship path (US-02-06, T6).
     *
     * @param entityName       the entity name
     * @param relationshipPath traversal path e.g. ["Interface", "AbstractClass", "ConcreteClass"]; may be null
     * @return a new GraphRelatedEntity
     */
    public static GraphRelatedEntity ofNameWithPath(String entityName, List<String> relationshipPath) {
        return new GraphRelatedEntity(entityName, null, null, relationshipPath);
    }
}
