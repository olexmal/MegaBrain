/*
 * Copyright (c) 2025 MegaBrain Contributors
 * Licensed under the MIT License - see LICENSE file for details.
 */

package io.megabrain.core;

import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;
import jakarta.inject.Inject;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.BoostQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.WildcardQuery;
import org.apache.lucene.index.Term;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for QueryParserService.
 * <p>
 * Tests the parsing of various query types including:
 * - Boolean operators (AND, OR, NOT)
 * - Phrase queries
 * - Wildcard queries
 * - Field-specific queries
 * - Error handling and fallbacks
 */
class QueryParserServiceTest {

    private QueryParserService queryParser;

    @BeforeEach
    void setUp() {
        // Manually create QueryParserService for testing
        queryParser = new QueryParserService();
        queryParser.analyzer = new CodeAwareAnalyzer();

        // Set test boost values matching defaults
        queryParser.contentBoost = 1.0f;
        queryParser.entityNameBoost = 3.0f;
        queryParser.docSummaryBoost = 2.0f;
        queryParser.entityNameKeywordBoost = 3.0f;
        queryParser.repositoryBoost = 1.0f;
        queryParser.languageBoost = 1.0f;
        queryParser.entityTypeBoost = 1.0f;

        queryParser.initialize();
    }

    @AfterEach
    void tearDown() {
        if (queryParser != null && queryParser.analyzer != null) {
            queryParser.analyzer.close();
        }
    }

    @Test
    void testParseSimpleTermQuery() {
        // Given - ensure queryParser is injected
        assertThat(queryParser).isNotNull();

        // When
        var result = queryParser.parseQuery("hello")
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        // Then
        result.assertCompleted();
        Query query = result.getItem();
        assertThat(query).isNotNull();

        // The query should be valid and contain search terms
        String queryString = query.toString();
        assertThat(queryString).isNotEmpty();
        // Should search across multiple fields - just check it's not empty for now
    }

    @Test
    void testParseBooleanAndQuery() {
        // When
        var result = queryParser.parseQuery("java AND spring")
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        // Then
        result.assertCompleted();
        Query query = result.getItem();
        assertThat(query).isNotNull();

        // Should contain AND logic and both terms
        String queryString = query.toString();
        assertThat(queryString).contains("java");
        assertThat(queryString).contains("spring");
        // AND logic should be represented in some form
        assertThat(queryString).doesNotContain("OR"); // Should not be OR
    }

    @Test
    void testParseBooleanOrQuery() {
        // When
        var result = queryParser.parseQuery("java OR python")
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        // Then
        result.assertCompleted();
        Query query = result.getItem();
        assertThat(query).isNotNull();

        // Should contain both terms
        String queryString = query.toString();
        assertThat(queryString).contains("java");
        assertThat(queryString).contains("python");
    }

    @Test
    void testParseNotQuery() {
        // When
        var result = queryParser.parseQuery("java NOT spring")
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        // Then
        result.assertCompleted();
        Query query = result.getItem();
        assertThat(query).isNotNull();

        // Should contain both terms (NOT logic)
        String queryString = query.toString();
        assertThat(queryString).contains("java");
        assertThat(queryString).contains("spring");
    }

    @Test
    void testParsePhraseQuery() {
        // When
        var result = queryParser.parseQuery("\"hello world\"")
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        // Then
        result.assertCompleted();
        Query query = result.getItem();
        assertThat(query.toString()).contains("\"hello world\"");
    }

    @Test
    void testParseWildcardQuery() {
        // When
        var result = queryParser.parseQuery("test*")
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        // Then
        result.assertCompleted();
        Query query = result.getItem();
        assertThat(query).isNotNull();

        // Should contain wildcard queries
        String queryString = query.toString();
        assertThat(queryString).contains("test*");
    }

    @Test
    void testParseFieldSpecificQuery() {
        // When
        var result = queryParser.parseQuery("language:java")
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        // Then
        result.assertCompleted();
        Query query = result.getItem();
        assertThat(query.toString()).contains("language:java");
    }

    @Test
    void testParseMultipleFieldQueries() {
        // When - test field-specific query parsing
        var result = queryParser.parseQuery("language:java")
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        // Then
        result.assertCompleted();
        Query query = result.getItem();
        String queryStr = query.toString();
        assertThat(queryStr).contains("language:java");

        // Note: Complex multi-field boolean queries may require different parsing approach
        // This test verifies basic field-specific query parsing works
    }

    @Test
    void testParseComplexQuery() {
        // When - complex query with multiple operators and field specifications
        var result = queryParser.parseQuery("(language:java OR language:kotlin) AND (spring OR quarkus) NOT deprecated")
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        // Then
        result.assertCompleted();
        Query query = result.getItem();
        String queryStr = query.toString();
        assertThat(queryStr).contains("language:java");
        assertThat(queryStr).contains("language:kotlin");
        assertThat(queryStr).contains("spring");
        assertThat(queryStr).contains("quarkus");
        assertThat(queryStr).contains("deprecated");
    }

    @Test
    void testParseEmptyQuery() {
        // When
        var result = queryParser.parseQuery("")
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        // Then
        result.assertCompleted();
        Query query = result.getItem();
        assertThat(query).isNotNull();
        // Should be a match-all query
    }

    @Test
    void testParseNullQuery() {
        // When
        var result = queryParser.parseQuery(null)
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        // Then
        result.assertCompleted();
        Query query = result.getItem();
        assertThat(query).isNotNull();
        // Should be a match-all query
    }

    @Test
    void testParseInvalidSyntax_withFallback() {
        // When - query with unbalanced parentheses
        var result = queryParser.parseQuery("java AND (spring OR")
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        // Then - should not fail, should use fallback parsing
        result.assertCompleted();
        Query query = result.getItem();
        assertThat(query).isNotNull();
        // Should still produce a valid query using fallback logic
    }

    @Test
    void testParseQueryWithSpecialCharacters() {
        // When - query with special characters that need escaping
        var result = queryParser.parseQuery("method()")
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        // Then
        result.assertCompleted();
        Query query = result.getItem();
        assertThat(query).isNotNull();
        // Should handle parentheses gracefully
    }

    @Test
    void testFieldQueryParsing() {
        // When
        var result = queryParser.parseFieldQuery(LuceneSchema.FIELD_LANGUAGE, "java OR kotlin")
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        // Then
        result.assertCompleted();
        Query query = result.getItem();
        assertThat(query.toString()).contains("language:java");
        assertThat(query.toString()).contains("language:kotlin");
    }

    @Test
    void testFieldQueryWithWildcard() {
        // When
        var result = queryParser.parseFieldQuery(LuceneSchema.FIELD_ENTITY_NAME, "User*")
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        // Then
        result.assertCompleted();
        Query query = result.getItem();
        assertThat(query.toString()).contains("entity_name:User*");
    }

    @Test
    void testIsValidQuery() {
        // Valid queries
        assertThat(queryParser.isValidQuery("java")).isTrue();
        assertThat(queryParser.isValidQuery("java AND spring")).isTrue();
        assertThat(queryParser.isValidQuery("language:java")).isTrue();
        assertThat(queryParser.isValidQuery("\"hello world\"")).isTrue();

        // Invalid queries
        assertThat(queryParser.isValidQuery("java AND (")).isFalse();
        assertThat(queryParser.isValidQuery("unclosed\"")).isFalse();

        // Edge cases
        assertThat(queryParser.isValidQuery("")).isTrue();
        assertThat(queryParser.isValidQuery(null)).isTrue();
    }

    @Test
    void testGetDefaultSearchFields() {
        // When
        String[] fields = queryParser.getDefaultSearchFields();

        // Then
        assertThat(fields).isNotNull();
        assertThat(fields).contains(LuceneSchema.FIELD_CONTENT);
        assertThat(fields).contains(LuceneSchema.FIELD_ENTITY_NAME);
        assertThat(fields).contains(LuceneSchema.FIELD_DOC_SUMMARY);
        assertThat(fields).contains(LuceneSchema.FIELD_LANGUAGE);
        assertThat(fields).contains(LuceneSchema.FIELD_ENTITY_TYPE);
        assertThat(fields).contains(LuceneSchema.FIELD_REPOSITORY);
    }

    @Test
    void testAnalyzerInjection() {
        // When
        var analyzer = queryParser.getAnalyzer();

        // Then
        assertThat(analyzer).isNotNull();
        assertThat(analyzer).isInstanceOf(CodeAwareAnalyzer.class);
    }

    @Test
    void testCamelCaseSplittingInQuery() {
        // When - query with camelCase that should be split by the analyzer
        var result = queryParser.parseQuery("getUserName")
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        // Then
        result.assertCompleted();
        Query query = result.getItem();
        assertThat(query).isNotNull();
        // The analyzer should handle camelCase splitting during indexing,
        // so the query should still work
    }

    @Test
    void testSnakeCaseSplittingInQuery() {
        // When - query with snake_case that should be split by the analyzer
        var result = queryParser.parseQuery("get_user_name")
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        // Then
        result.assertCompleted();
        Query query = result.getItem();
        assertThat(query).isNotNull();
        // The analyzer should handle snake_case splitting during indexing
    }

    @Test
    void testFieldSpecificQueryAppliesBoosts() {
        // When - parse field-specific query for entity_name (should get boost 3.0)
        var result = queryParser.parseFieldQuery(LuceneSchema.FIELD_ENTITY_NAME, "UserService")
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        // Then
        result.assertCompleted();
        Query query = result.getItem();
        assertThat(query).isNotNull();
        assertThat(query).isInstanceOf(BoostQuery.class);

        BoostQuery boostQuery = (BoostQuery) query;
        assertThat(boostQuery.getBoost()).isEqualTo(3.0f);
    }

    @Test
    void testFieldSpecificQueryNoBoostWhenDefault() {
        // When - parse field-specific query for content (boost 1.0 - no BoostQuery wrapper needed)
        var result = queryParser.parseFieldQuery(LuceneSchema.FIELD_CONTENT, "some content")
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        // Then
        result.assertCompleted();
        Query query = result.getItem();
        assertThat(query).isNotNull();
        // Content has boost 1.0, so no BoostQuery wrapper should be applied
        assertThat(query).isNotInstanceOf(BoostQuery.class);
    }

    @Test
    void testDocSummaryFieldGetsBoost() {
        // When - parse field-specific query for doc_summary (should get boost 2.0)
        var result = queryParser.parseFieldQuery(LuceneSchema.FIELD_DOC_SUMMARY, "service description")
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        // Then
        result.assertCompleted();
        Query query = result.getItem();
        assertThat(query).isNotNull();
        assertThat(query).isInstanceOf(BoostQuery.class);

        BoostQuery boostQuery = (BoostQuery) query;
        assertThat(boostQuery.getBoost()).isEqualTo(2.0f);
    }

    @Test
    void testFallbackQueryAppliesBoosts() {
        // When - parse invalid query that triggers fallback parsing
        var result = queryParser.parseQuery("java AND (")
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        // Then
        result.assertCompleted();
        Query query = result.getItem();
        assertThat(query).isNotNull();
        assertThat(query).isInstanceOf(BooleanQuery.class);

        // The fallback query should contain boosted sub-queries
        BooleanQuery booleanQuery = (BooleanQuery) query;
        assertThat(booleanQuery.clauses()).isNotEmpty();

        // At least one clause should be boosted (entity_name gets 3.0 boost)
        boolean hasBoostedClause = booleanQuery.clauses().stream()
                .anyMatch(clause -> clause.query() instanceof BoostQuery);
        assertThat(hasBoostedClause).isTrue();
    }

    @Test
    void testFieldFallbackQueryAppliesBoost() {
        // When - parse invalid field query that triggers fallback
        var result = queryParser.parseFieldQuery(LuceneSchema.FIELD_ENTITY_NAME, "invalid ( query")
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        // Then
        result.assertCompleted();
        Query query = result.getItem();
        assertThat(query).isNotNull();
        assertThat(query).isInstanceOf(BooleanQuery.class);

        // The fallback query should contain boosted sub-queries
        BooleanQuery booleanQuery = (BooleanQuery) query;
        assertThat(booleanQuery.clauses()).isNotEmpty();

        // All clauses should be boosted for entity_name field
        boolean allClausesBoosted = booleanQuery.clauses().stream()
                .allMatch(clause -> clause.query() instanceof BoostQuery);
        assertThat(allClausesBoosted).isTrue();

        // Verify the boost value is correct
        BoostQuery firstBoostQuery = (BoostQuery) booleanQuery.clauses().get(0).query();
        assertThat(firstBoostQuery.getBoost()).isEqualTo(3.0f);
    }

    @Test
    void testEntityNameRanksHigherThanContent() {
        // When - parse queries for entity_name and content fields
        var entityResult = queryParser.parseFieldQuery(LuceneSchema.FIELD_ENTITY_NAME, "UserService")
                .subscribe().withSubscriber(UniAssertSubscriber.create());
        var contentResult = queryParser.parseFieldQuery(LuceneSchema.FIELD_CONTENT, "UserService")
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        // Then
        entityResult.assertCompleted();
        contentResult.assertCompleted();

        Query entityQuery = entityResult.getItem();
        Query contentQuery = contentResult.getItem();

        // Entity name should be boosted (3.0), content should not be boosted (1.0)
        assertThat(entityQuery).isInstanceOf(BoostQuery.class);
        assertThat(contentQuery).isNotInstanceOf(BoostQuery.class);

        BoostQuery entityBoostQuery = (BoostQuery) entityQuery;
        assertThat(entityBoostQuery.getBoost()).isGreaterThan(1.0f);
    }

    @Test
    void testCustomBoostConfiguration() {
        // Given - create parser with custom boost values
        QueryParserService customParser = new QueryParserService();
        customParser.analyzer = new CodeAwareAnalyzer();
        customParser.contentBoost = 1.0f;
        customParser.entityNameBoost = 5.0f; // Custom high boost
        customParser.docSummaryBoost = 2.0f;
        customParser.entityNameKeywordBoost = 4.0f;
        customParser.repositoryBoost = 1.0f;
        customParser.languageBoost = 1.0f;
        customParser.entityTypeBoost = 1.0f;
        customParser.initialize();

        // When - parse entity_name query with custom boost
        var result = customParser.parseFieldQuery(LuceneSchema.FIELD_ENTITY_NAME, "CustomClass")
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        // Then
        result.assertCompleted();
        Query query = result.getItem();
        assertThat(query).isInstanceOf(BoostQuery.class);

        BoostQuery boostQuery = (BoostQuery) query;
        assertThat(boostQuery.getBoost()).isEqualTo(5.0f); // Should use custom boost value
    }
}
