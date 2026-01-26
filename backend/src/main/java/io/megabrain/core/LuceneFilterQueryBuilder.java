/*
 * Copyright (c) 2025 MegaBrain Contributors
 * Licensed under the MIT License - see LICENSE file for details.
 */

package io.megabrain.core;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.PrefixQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;

import java.util.List;
import java.util.Optional;

/**
 * Builds Lucene filter queries for metadata fields (US-02-04, T2).
 * <p>
 * Creates TermQuery filters for exact matches (language, repository, entity_type)
 * and PrefixQuery for file path prefix matches. Multiple values per dimension use
 * OR logic; dimensions are combined with AND (BooleanQuery MUST clauses).
 * <p>
 * The resulting query is intended to be used as a {@link BooleanClause.Occur#FILTER}
 * clause so filters are applied before scoring and do not affect relevance scores.
 */
public final class LuceneFilterQueryBuilder {

    private LuceneFilterQueryBuilder() {}

    /**
     * Builds a Lucene Query from filter criteria, or empty if no filters present.
     * <p>
     * Uses TermQuery for language, repository, and entity_type (exact match).
     * Uses PrefixQuery for file_path (prefix match). Multiple values per dimension
     * are combined with OR; dimensions are combined with AND.
     *
     * @param filters the filter criteria (may be null or empty)
     * @return the filter query, or {@link Optional#empty()} when no filters
     */
    public static Optional<Query> build(SearchFilters filters) {
        if (filters == null || !filters.hasFilters()) {
            return Optional.empty();
        }

        BooleanQuery.Builder andBuilder = new BooleanQuery.Builder();

        if (!filters.languages().isEmpty()) {
            Query langQuery = buildDimensionQuery(
                    LuceneSchema.FIELD_LANGUAGE, filters.languages(), false);
            andBuilder.add(langQuery, BooleanClause.Occur.MUST);
        }
        if (!filters.repositories().isEmpty()) {
            Query repoQuery = buildDimensionQuery(
                    LuceneSchema.FIELD_REPOSITORY, filters.repositories(), false);
            andBuilder.add(repoQuery, BooleanClause.Occur.MUST);
        }
        if (!filters.filePaths().isEmpty()) {
            Query pathQuery = buildDimensionQuery(
                    LuceneSchema.FIELD_FILE_PATH, filters.filePaths(), true);
            andBuilder.add(pathQuery, BooleanClause.Occur.MUST);
        }
        if (!filters.entityTypes().isEmpty()) {
            Query typeQuery = buildDimensionQuery(
                    LuceneSchema.FIELD_ENTITY_TYPE, filters.entityTypes(), false);
            andBuilder.add(typeQuery, BooleanClause.Occur.MUST);
        }

        return Optional.of(andBuilder.build());
    }

    /**
     * Builds a query for one filter dimension (OR of values).
     * Uses TermQuery for exact match, PrefixQuery when prefixMatch is true.
     * Blank values are ignored.
     */
    private static Query buildDimensionQuery(String field, List<String> values, boolean prefixMatch) {
        if (values == null || values.isEmpty()) {
            throw new IllegalArgumentException("values must not be null or empty");
        }
        List<String> nonBlank = values.stream()
                .filter(v -> v != null && !v.isBlank())
                .toList();
        if (nonBlank.isEmpty()) {
            throw new IllegalArgumentException("values must contain at least one non-blank value");
        }
        if (nonBlank.size() == 1) {
            String v = nonBlank.get(0);
            if (prefixMatch) {
                return new PrefixQuery(new Term(field, v));
            }
            return new TermQuery(new Term(field, v));
        }
        BooleanQuery.Builder orBuilder = new BooleanQuery.Builder();
        for (String value : nonBlank) {
            Query q = prefixMatch
                    ? new PrefixQuery(new Term(field, value))
                    : new TermQuery(new Term(field, value));
            orBuilder.add(q, BooleanClause.Occur.SHOULD);
        }
        return orBuilder.build();
    }
}
