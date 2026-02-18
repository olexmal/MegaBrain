/*
 * Copyright (c) 2025 MegaBrain Contributors
 * Licensed under the MIT License - see LICENSE file for details.
 */

package io.megabrain.core;

import java.util.Optional;

/**
 * Parses structural search query predicates (implements:, extends:, usages:) from the raw query string (US-02-06, T3/T4, AC3).
 * Used by the graph query layer to decide which transitive closure to run.
 */
public final class StructuralQueryParser {

    private static final String IMPLEMENTS_PREFIX = "implements:";
    private static final String EXTENDS_PREFIX = "extends:";
    private static final String USAGES_PREFIX = "usages:";

    private StructuralQueryParser() {}

    /**
     * Extracts the interface name from a query that starts with {@code implements:InterfaceName}.
     * The rest of the query (e.g. other terms) is ignored for structural lookup; only the first
     * structural predicate is considered.
     *
     * @param query full search query (e.g. "implements:IRepository" or "implements:IRepo other terms")
     * @return the interface name if the query is an implements-predicate, empty otherwise
     */
    public static Optional<String> parseImplementsTarget(String query) {
        if (query == null || !query.startsWith(IMPLEMENTS_PREFIX)) {
            return Optional.empty();
        }
        String rest = query.substring(IMPLEMENTS_PREFIX.length()).trim();
        if (rest.isEmpty()) {
            return Optional.empty();
        }
        // First token only (e.g. "IRepository" from "implements:IRepository" or "implements:IRepository foo")
        int space = rest.indexOf(' ');
        String name = space < 0 ? rest : rest.substring(0, space);
        return Optional.of(name).filter(s -> !s.isEmpty());
    }

    /**
     * Extracts the class name from a query that starts with {@code extends:ClassName} (US-02-06, T4).
     *
     * @param query full search query (e.g. "extends:BaseClass")
     * @return the class name if the query is an extends-predicate, empty otherwise
     */
    public static Optional<String> parseExtendsTarget(String query) {
        if (query == null || !query.startsWith(EXTENDS_PREFIX)) {
            return Optional.empty();
        }
        String rest = query.substring(EXTENDS_PREFIX.length()).trim();
        if (rest.isEmpty()) {
            return Optional.empty();
        }
        int space = rest.indexOf(' ');
        String name = space < 0 ? rest : rest.substring(0, space);
        return Optional.of(name).filter(s -> !s.isEmpty());
    }

    /**
     * Extracts the type name from a query that starts with {@code usages:TypeName} (US-02-06, AC3).
     * "Find usages of X" with transitive=true expands to X and all implementations/subclasses
     * so that polymorphic call sites (code using any subtype) are included.
     *
     * @param query full search query (e.g. "usages:IRepository" or "usages:BaseClass")
     * @return the type name if the query is a usages-predicate, empty otherwise
     */
    public static Optional<String> parseUsagesTarget(String query) {
        if (query == null || !query.startsWith(USAGES_PREFIX)) {
            return Optional.empty();
        }
        String rest = query.substring(USAGES_PREFIX.length()).trim();
        if (rest.isEmpty()) {
            return Optional.empty();
        }
        int space = rest.indexOf(' ');
        String name = space < 0 ? rest : rest.substring(0, space);
        return Optional.of(name).filter(s -> !s.isEmpty());
    }
}
