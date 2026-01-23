/*
 * Copyright (c) 2025 MegaBrain Contributors
 * Licensed under the MIT License - see LICENSE file for details.
 */

package io.megabrain.core;

/**
 * Search mode for hybrid ranking (US-02-03, T6).
 * <p>
 * Controls which search systems are used:
 * <ul>
 *   <li>{@link #HYBRID}: Uses both Lucene keyword search and vector similarity search (default)</li>
 *   <li>{@link #KEYWORD}: Uses only Lucene keyword search, skips vector search</li>
 *   <li>{@link #VECTOR}: Uses only vector similarity search, skips Lucene search</li>
 * </ul>
 */
public enum SearchMode {
    /**
     * Hybrid mode: combines both Lucene keyword search and vector similarity search.
     * Results are merged and deduplicated using weighted score combination.
     */
    HYBRID,

    /**
     * Keyword-only mode: uses only Lucene keyword search, skips vector search.
     * Useful when semantic similarity is not needed or vector store is unavailable.
     */
    KEYWORD,

    /**
     * Vector-only mode: uses only vector similarity search, skips Lucene search.
     * Useful when keyword matching is not needed or Lucene index is unavailable.
     */
    VECTOR
}
