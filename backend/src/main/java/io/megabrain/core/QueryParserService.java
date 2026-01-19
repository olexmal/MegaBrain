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
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.BoostQuery;
import org.apache.lucene.search.PhraseQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.WildcardQuery;
import org.apache.lucene.index.Term;
import org.jboss.logging.Logger;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Service for parsing user search queries into Lucene Query objects with relevance scoring.
 * <p>
 * Supports advanced Lucene query syntax including:
 * - Boolean operators: AND, OR, NOT
 * - Phrase queries: "exact phrase"
 * - Wildcards: * and ?
 * - Field-specific queries: field:value
 * - Fuzzy searches: term~
 * - Proximity searches: "term1 term2"~distance
 * <p>
 * Applies configurable field boosts for relevance scoring:
 * - entity_name: 3.0 (highest priority for exact matches)
 * - doc_summary: 2.0 (important documentation)
 * - content: 1.0 (base relevance)
 * - Other fields: configurable defaults
 * <p>
 * Provides graceful error handling and fallbacks for invalid syntax.
 */
@ApplicationScoped
public class QueryParserService {

    private static final Logger LOG = Logger.getLogger(QueryParserService.class);

    @Inject
    CodeAwareAnalyzer analyzer;

    // Configurable field boost values for relevance scoring
    @ConfigProperty(name = "megabrain.search.boost.content", defaultValue = "1.0")
    float contentBoost;

    @ConfigProperty(name = "megabrain.search.boost.entity_name", defaultValue = "3.0")
    float entityNameBoost;

    @ConfigProperty(name = "megabrain.search.boost.doc_summary", defaultValue = "2.0")
    float docSummaryBoost;

    @ConfigProperty(name = "megabrain.search.boost.entity_name_keyword", defaultValue = "3.0")
    float entityNameKeywordBoost;

    @ConfigProperty(name = "megabrain.search.boost.repository", defaultValue = "1.0")
    float repositoryBoost;

    @ConfigProperty(name = "megabrain.search.boost.language", defaultValue = "1.0")
    float languageBoost;

    @ConfigProperty(name = "megabrain.search.boost.entity_type", defaultValue = "1.0")
    float entityTypeBoost;

    // Default search fields
    private static final String[] DEFAULT_SEARCH_FIELDS = {
            LuceneSchema.FIELD_CONTENT,
            LuceneSchema.FIELD_ENTITY_NAME,
            LuceneSchema.FIELD_DOC_SUMMARY,
            LuceneSchema.FIELD_ENTITY_NAME_KEYWORD,
            LuceneSchema.FIELD_REPOSITORY,
            LuceneSchema.FIELD_LANGUAGE,
            LuceneSchema.FIELD_ENTITY_TYPE
    };

    // Fields that support phrase queries (must have position data)
    private static final String[] PHRASE_SEARCH_FIELDS = {
            LuceneSchema.FIELD_CONTENT,
            LuceneSchema.FIELD_ENTITY_NAME,
            LuceneSchema.FIELD_DOC_SUMMARY
    };

    private MultiFieldQueryParser multiFieldQueryParser;
    private QueryParser defaultQueryParser;

    @PostConstruct
    void initialize() {
        LOG.info("Initializing query parser service");

        // Create field boost map from configured values
        Map<String, Float> fieldBoosts = Map.of(
                LuceneSchema.FIELD_CONTENT, contentBoost,
                LuceneSchema.FIELD_ENTITY_NAME, entityNameBoost,
                LuceneSchema.FIELD_DOC_SUMMARY, docSummaryBoost,
                LuceneSchema.FIELD_ENTITY_NAME_KEYWORD, entityNameKeywordBoost,
                LuceneSchema.FIELD_REPOSITORY, repositoryBoost,
                LuceneSchema.FIELD_LANGUAGE, languageBoost,
                LuceneSchema.FIELD_ENTITY_TYPE, entityTypeBoost
        );

        // Create multi-field query parser with field boosts for relevance scoring
        multiFieldQueryParser = new MultiFieldQueryParser(DEFAULT_SEARCH_FIELDS, analyzer, fieldBoosts);
        multiFieldQueryParser.setDefaultOperator(QueryParser.Operator.OR);

        LOG.debugf("Configured field boosts: content=%.1f, entity_name=%.1f, doc_summary=%.1f, entity_name_keyword=%.1f",
                   contentBoost, entityNameBoost, docSummaryBoost, entityNameKeywordBoost);

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
                } else if (containsWildcard(trimmedQuery)) {
                    // Handle wildcard queries with special parsing
                    Query wildcardQuery = parseWildcardQuery(trimmedQuery);
                    LOG.debugf("Successfully parsed wildcard query: %s", wildcardQuery);
                    return wildcardQuery;
                } else if (isPhraseQuery(trimmedQuery)) {
                    // Handle phrase queries with special parsing across all fields
                    Query parsedQuery = parsePhraseQuery(trimmedQuery);
                    LOG.debugf("Successfully parsed phrase query: %s", parsedQuery);
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
     * Applies field-specific boost values for relevance scoring.
     *
     * @param fieldName the field to search in
     * @param queryString the query string
     * @return a Uni that emits the parsed Query with appropriate boosts applied
     */
    public Uni<Query> parseFieldQuery(String fieldName, String queryString) {
        return Uni.createFrom().item(() -> {
            if (queryString == null || queryString.trim().isEmpty()) {
                return createMatchAllQuery();
            }

            try {
                QueryParser fieldParser = new QueryParser(fieldName, analyzer);
                fieldParser.setDefaultOperator(QueryParser.Operator.OR);
                Query parsedQuery = fieldParser.parse(queryString.trim());

                // Apply boost if configured for this field
                float boost = getBoostForField(fieldName);
                if (boost != 1.0f) {
                    parsedQuery = new BoostQuery(parsedQuery, boost);
                    LOG.debugf("Applied boost %.1f to field query for '%s': %s", boost, fieldName, queryString);
                }

                return parsedQuery;

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
    /**
     * Checks if a query string contains wildcard characters.
     */
    private boolean containsWildcard(String queryString) {
        return queryString.contains("*") || queryString.contains("?");
    }

    /**
     * Checks if a query string is a phrase query (enclosed in quotes).
     */
    private boolean isPhraseQuery(String queryString) {
        return queryString.trim().startsWith("\"") && queryString.trim().endsWith("\"");
    }

    /**
     * Parses a wildcard query by creating a boolean query across all fields.
     */
    private Query parseWildcardQuery(String queryString) throws ParseException {
        BooleanQuery.Builder booleanQuery = new BooleanQuery.Builder();

        // For each field, create a wildcard query and add it to the boolean query
        for (String field : DEFAULT_SEARCH_FIELDS) {
            Query fieldQuery = new WildcardQuery(new Term(field, queryString));
            booleanQuery.add(fieldQuery, BooleanClause.Occur.SHOULD);
        }

        return booleanQuery.build();
    }

    /**
     * Parses a phrase query by creating a boolean query with phrase queries across supported fields.
     */
    private Query parsePhraseQuery(String queryString) throws ParseException {
        // Remove quotes from the phrase
        String phrase = queryString.trim().substring(1, queryString.length() - 1);

        BooleanQuery.Builder booleanQuery = new BooleanQuery.Builder();

        // For each field that supports phrase queries, create a phrase query manually
        for (String field : PHRASE_SEARCH_FIELDS) {
            if (analyzer != null) {
                try {
                    // Analyze the phrase terms to get the actual tokens
                    java.util.List<String> analyzedTerms = new java.util.ArrayList<>();
                    try (org.apache.lucene.analysis.TokenStream tokenStream = analyzer.tokenStream(field, phrase)) {
                        tokenStream.reset();
                        while (tokenStream.incrementToken()) {
                            CharTermAttribute attr = tokenStream.getAttribute(CharTermAttribute.class);
                            analyzedTerms.add(attr.toString());
                        }
                        tokenStream.end();
                    }

                    if (!analyzedTerms.isEmpty()) {
                        PhraseQuery.Builder phraseBuilder = new PhraseQuery.Builder();
                        for (String term : analyzedTerms) {
                            phraseBuilder.add(new Term(field, term));
                        }
                        Query fieldQuery = phraseBuilder.build();
                        booleanQuery.add(fieldQuery, BooleanClause.Occur.SHOULD);
                    }
                } catch (Exception e) {
                    LOG.debugf("Could not create phrase query for field %s: %s", field, e.getMessage());
                }
            }
        }

        Query result = booleanQuery.build();
        if (result instanceof BooleanQuery && ((BooleanQuery) result).clauses().isEmpty()) {
            throw new ParseException("No valid phrase query could be created for: " + queryString);
        }

        return result;
    }

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
     * Applies field boosts to ensure proper relevance scoring.
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
                        Query wildcardQuery = new WildcardQuery(new Term(field, term));
                        float boost = getBoostForField(field);
                        if (boost != 1.0f) {
                            wildcardQuery = new BoostQuery(wildcardQuery, boost);
                        }
                        builder.add(wildcardQuery, BooleanClause.Occur.SHOULD);
                    }
                } else {
                    for (String field : DEFAULT_SEARCH_FIELDS) {
                        Query termQuery = new TermQuery(new Term(field, term));
                        float boost = getBoostForField(field);
                        if (boost != 1.0f) {
                            termQuery = new BoostQuery(termQuery, boost);
                        }
                        builder.add(termQuery, BooleanClause.Occur.SHOULD);
                    }
                }
            }
        }

        return builder.build();
    }

    /**
     * Creates a fallback query for a specific field.
     * Applies field boost for consistent relevance scoring.
     */
    private Query createFieldTermFallbackQuery(String fieldName, String queryString) {
        BooleanQuery.Builder builder = new BooleanQuery.Builder();

        String[] terms = queryString.split("\\s+");
        float boost = getBoostForField(fieldName);

        for (String term : terms) {
            if (!term.isEmpty()) {
                Query baseQuery;
                if (term.contains("*") || term.contains("?")) {
                    baseQuery = new WildcardQuery(new Term(fieldName, term));
                } else {
                    baseQuery = new TermQuery(new Term(fieldName, term));
                }

                // Apply boost if configured
                if (boost != 1.0f) {
                    baseQuery = new BoostQuery(baseQuery, boost);
                }

                builder.add(baseQuery, BooleanClause.Occur.SHOULD);
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
     * Gets the boost value for a specific field.
     *
     * @param fieldName the field name
     * @return the boost value (1.0f if no boost configured)
     */
    private float getBoostForField(String fieldName) {
        return switch (fieldName) {
            case LuceneSchema.FIELD_CONTENT -> contentBoost;
            case LuceneSchema.FIELD_ENTITY_NAME -> entityNameBoost;
            case LuceneSchema.FIELD_DOC_SUMMARY -> docSummaryBoost;
            case LuceneSchema.FIELD_ENTITY_NAME_KEYWORD -> entityNameKeywordBoost;
            case LuceneSchema.FIELD_REPOSITORY -> repositoryBoost;
            case LuceneSchema.FIELD_LANGUAGE -> languageBoost;
            case LuceneSchema.FIELD_ENTITY_TYPE -> entityTypeBoost;
            default -> 1.0f;
        };
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
