/*
 * Copyright (c) 2025 MegaBrain Contributors
 * Licensed under the MIT License - see LICENSE file for details.
 */

package io.megabrain.core;

/**
 * Represents an entity found via graph traversal (implements/extends relationships).
 * Used to resolve graph results to Lucene documents for inclusion in search results (US-02-06, T2).
 *
 * @param entityName   canonical entity name (e.g. class or interface name)
 * @param entityType   optional entity type (class, interface, etc.)
 * @param sourceFile   optional source file path when known from graph
 */
public record GraphRelatedEntity(String entityName, String entityType, String sourceFile) {

    /**
     * Creates a related entity with only the name (type and source file null).
     *
     * @param entityName the entity name
     * @return a new GraphRelatedEntity
     */
    public static GraphRelatedEntity ofName(String entityName) {
        return new GraphRelatedEntity(entityName, null, null);
    }
}
