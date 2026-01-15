/*
 * Copyright (c) 2025 MegaBrain Contributors
 * Licensed under the MIT License - see LICENSE file for details.
 */

package io.megabrain.core;

import io.smallrye.mutiny.Uni;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.WildcardQuery;
import org.apache.lucene.index.Term;
import org.jboss.logging.Logger;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Service for parsing user search queries into Lucene Query objects.
 * <p>
 * Supports advanced Lucene query syntax including:
 * - Boolean operators: AND, OR, NOT
 * - Phrase queries: "exact phrase"
 * - Wildcards: * and ?
 * - Field-specific queries: field:value
 * - Fuzzy searches: term~
 * - Proximity searches: "term1 term2"~distance
 * <p>
 * Provides graceful error handling and fallbacks for invalid syntax.
 */
@ApplicationScoped
public class QueryParserService {

    private static final Logger LOG = Logger.getLogger(QueryParserService.class);

    @Inject
    CodeAwareAnalyzer analyzer;

    // Default search fields with their boost values
    private static final String[] DEFAULT_SEARCH_FIELDS = {
            LuceneSchema.FIELD_CONTENT,
            LuceneSchema.FIELD_ENTITY_NAME,
            LuceneSchema.FIELD_DOC_SUMMARY,
            LuceneSchema.FIELD_ENTITY_NAME_KEYWORD,
            LuceneSchema.FIELD_REPOSITORY,
            LuceneSchema.FIELD_LANGUAGE,
            LuceneSchema.FIELD_ENTITY_TYPE
    };

    // Field boost values for relevance scoring
    private static final Map<String, Float> FIELD_BOOSTS = Map.of(
            LuceneSchema.FIELD_CONTENT, 1.0f,
            LuceneSchema.FIELD_ENTITY_NAME, 2.0f,
            LuceneSchema.FIELD_DOC_SUMMARY, 1.5f,
            LuceneSchema.FIELD_ENTITY_NAME_KEYWORD, 3.0f,
            LuceneSchema.FIELD_REPOSITORY, 1.0f,
            LuceneSchema.FIELD_LANGUAGE, 1.0f,
            LuceneSchema.FIELD_ENTITY_TYPE, 1.0f
    );

    private MultiFieldQueryParser multiFieldQueryParser;
    private QueryParser defaultQueryParser;

    @PostConstruct
    void initialize() {
        LOG.info("Initializing query parser service");

        // Create multi-field query parser for searching across multiple fields
        multiFieldQueryParser = new MultiFieldQueryParser(DEFAULT_SEARCH_FIELDS, analyzer);
        multiFieldQueryParser.setDefaultOperator(QueryParser.Operator.OR);

        // Note: Field boosts are handled at query time, not parser configuration
        // Boosts will be applied when creating BooleanQuery in search methods

        // Create default query parser for field-specific and complex queries
        defaultQueryParser = new QueryParser(LuceneSchema.FIELD_CONTENT, analyzer);
        defaultQueryParser.setDefaultOperator(QueryParser.Operator.OR);

        // Allow all fields to be searched by setting allowed fields
        String[] allFields = java.util.stream.Stream.concat(
            java.util.Arrays.stream(DEFAULT_SEARCH_FIELDS),
            java.util.Arrays.stream(new String[]{LuceneSchema.FIELD_DOCUMENT_ID, LuceneSchema.FIELD_START_LINE, LuceneSchema.FIELD_END_LINE, LuceneSchema.FIELD_START_BYTE, LuceneSchema.FIELD_END_BYTE})
        ).toArray(String[]::new);
        defaultQueryParser.setMultiTermRewriteMethod(org.apache.lucene.search.MultiTermQuery.CONSTANT_SCORE_BOOLEAN_REWRITE);

        LOG.info("Query parser service initialized successfully");
    }

    /**
     * Parses a user query string into a Lucene Query object.
     * <p>
     * Supports full Lucene query syntax including boolean operators,
     * phrase queries, wildcards, and field-specific searches.
     *
     * @param queryString the user query string
     * @return a Uni that emits the parsed Query, or a fallback query on parse errors
     */
    public Uni<Query> parseQuery(String queryString) {
        return Uni.createFrom().item(() -> {
            if (queryString == null || queryString.trim().isEmpty()) {
                LOG.debug("Empty query string, returning match-all query");
                return createMatchAllQuery();
            }

            String trimmedQuery = queryString.trim();
            LOG.debugf("Parsing query: %s", trimmedQuery);

            try {
                // Check if query contains field-specific syntax (field:value)
                if (containsFieldSyntax(trimmedQuery)) {
                    // Use regular QueryParser for field-specific queries
                    Query parsedQuery = defaultQueryParser.parse(trimmedQuery);
                    LOG.debugf("Successfully parsed field-specific query: %s", parsedQuery);
                    return parsedQuery;
                } else {
                    // Use MultiFieldQueryParser for general multi-field search
                    Query parsedQuery = multiFieldQueryParser.parse(trimmedQuery);
                    LOG.debugf("Successfully parsed multi-field query: %s", parsedQuery);
                    return parsedQuery;
                }

            } catch (ParseException e) {
                LOG.warnf(e, "Failed to parse query '%s', attempting fallback parsing", trimmedQuery);

                // Try fallback parsing strategies
                Query fallbackQuery = tryFallbackParsing(trimmedQuery);
                if (fallbackQuery != null) {
                    LOG.debugf("Fallback parsing successful: %s", fallbackQuery);
                    return fallbackQuery;
                }

                // Last resort: treat as simple term query
                LOG.warnf("All parsing attempts failed for query '%s', using term fallback", trimmedQuery);
                return createTermFallbackQuery(trimmedQuery);
            }
        });
    }

    /**
     * Parses a query restricted to a specific field.
     *
     * @param fieldName the field to search in
     * @param queryString the query string
     * @return a Uni that emits the parsed Query
     */
    public Uni<Query> parseFieldQuery(String fieldName, String queryString) {
        return Uni.createFrom().item(() -> {
            if (queryString == null || queryString.trim().isEmpty()) {
                return createMatchAllQuery();
            }

            try {
                QueryParser fieldParser = new QueryParser(fieldName, analyzer);
                fieldParser.setDefaultOperator(QueryParser.Operator.OR);
                return fieldParser.parse(queryString.trim());

            } catch (ParseException e) {
                LOG.warnf(e, "Failed to parse field query for field '%s': '%s'",
                         fieldName, queryString);
                return createFieldTermFallbackQuery(fieldName, queryString.trim());
            }
        });
    }

    /**
     * Attempts fallback parsing strategies when main parsing fails.
     */
    private Query tryFallbackParsing(String queryString) {
        // Try 1: Simple phrase query if the query looks like a phrase
        if (queryString.contains(" ") && !queryString.contains("\"")) {
            try {
                String phraseQuery = "\"" + queryString + "\"";
                return multiFieldQueryParser.parse(phraseQuery);
            } catch (ParseException e) {
                // Continue to next fallback
            }
        }

        // Try 2: Escape special characters and try again
        try {
            String escapedQuery = escapeLuceneSpecialChars(queryString);
            if (!escapedQuery.equals(queryString)) {
                return multiFieldQueryParser.parse(escapedQuery);
            }
        } catch (ParseException e) {
            // Continue to next fallback
        }

        // Try 3: Split on common separators and create boolean query
        if (queryString.contains(",") || queryString.contains(";")) {
            String[] parts = queryString.split("[,;]");
            if (parts.length > 1) {
                BooleanQuery.Builder builder = new BooleanQuery.Builder();
                for (String part : parts) {
                    String trimmedPart = part.trim();
                    if (!trimmedPart.isEmpty()) {
                        try {
                            Query partQuery = multiFieldQueryParser.parse(trimmedPart);
                            builder.add(partQuery, BooleanClause.Occur.SHOULD);
                        } catch (ParseException e) {
                            // Skip invalid parts
                        }
                    }
                }
                Query compoundQuery = builder.build();
                if (compoundQuery instanceof BooleanQuery booleanQuery && !booleanQuery.clauses().isEmpty()) {
                    return compoundQuery;
                }
            }
        }

        return null; // All fallbacks failed
    }

    /**
     * Creates a fallback term query when all parsing fails.
     */
    private Query createTermFallbackQuery(String queryString) {
        // Split on whitespace and create OR query
        String[] terms = queryString.split("\\s+");
        BooleanQuery.Builder builder = new BooleanQuery.Builder();

        for (String term : terms) {
            if (!term.isEmpty()) {
                // Add wildcard support for partial matches
                if (term.contains("*") || term.contains("?")) {
                    for (String field : DEFAULT_SEARCH_FIELDS) {
                        builder.add(new WildcardQuery(new Term(field, term)), BooleanClause.Occur.SHOULD);
                    }
                } else {
                    for (String field : DEFAULT_SEARCH_FIELDS) {
                        builder.add(new TermQuery(new Term(field, term)), BooleanClause.Occur.SHOULD);
                    }
                }
            }
        }

        return builder.build();
    }

    /**
     * Creates a fallback query for a specific field.
     */
    private Query createFieldTermFallbackQuery(String fieldName, String queryString) {
        BooleanQuery.Builder builder = new BooleanQuery.Builder();

        String[] terms = queryString.split("\\s+");
        for (String term : terms) {
            if (!term.isEmpty()) {
                if (term.contains("*") || term.contains("?")) {
                    builder.add(new WildcardQuery(new Term(fieldName, term)), BooleanClause.Occur.SHOULD);
                } else {
                    builder.add(new TermQuery(new Term(fieldName, term)), BooleanClause.Occur.SHOULD);
                }
            }
        }

        return builder.build();
    }

    /**
     * Creates a match-all query for empty searches.
     */
    private Query createMatchAllQuery() {
        // Create a query that matches everything by searching for a common term
        // This is a simple approximation - in a real system you might use MatchAllDocsQuery
        return new WildcardQuery(new Term(LuceneSchema.FIELD_CONTENT, "*"));
    }

    /**
     * Escapes special Lucene query characters.
     */
    private String escapeLuceneSpecialChars(String input) {
        // Lucene special characters that need escaping
        String[] specialChars = {"+", "-", "&&", "||", "!", "(", ")", "{", "}", "[", "]", "^", "\"", "~", "*", "?", ":", "\\"};

        String result = input;
        for (String specialChar : specialChars) {
            if (result.contains(specialChar)) {
                result = result.replace(specialChar, "\\" + specialChar);
            }
        }

        return result;
    }

    /**
     * Validates if a query string has valid Lucene syntax.
     *
     * @param queryString the query to validate
     * @return true if the query is valid, false otherwise
     */
    public boolean isValidQuery(String queryString) {
        if (queryString == null || queryString.trim().isEmpty()) {
            return true; // Empty queries are valid
        }

        try {
            multiFieldQueryParser.parse(queryString.trim());
            return true;
        } catch (ParseException e) {
            return false;
        }
    }

    /**
     * Gets the default search fields used by this parser.
     */
    public String[] getDefaultSearchFields() {
        return Arrays.copyOf(DEFAULT_SEARCH_FIELDS, DEFAULT_SEARCH_FIELDS.length);
    }

    /**
     * Gets the analyzer used by this query parser.
     */
    public Analyzer getAnalyzer() {
        return analyzer;
    }

    /**
     * Checks if a query string contains field-specific syntax (field:value).
     *
     * @param queryString the query string to check
     * @return true if the query contains field syntax, false otherwise
     */
    private boolean containsFieldSyntax(String queryString) {
        // Look for field:value patterns (field followed by colon)
        // This is a simple heuristic - a field name followed by a colon
        return queryString.contains(":");
    }
}
