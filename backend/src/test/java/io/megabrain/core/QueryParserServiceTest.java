/*
 * Copyright (c) 2025 MegaBrain Contributors
 * Licensed under the MIT License - see LICENSE file for details.
 */

package io.megabrain.core;

import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;
import jakarta.inject.Inject;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.WildcardQuery;
import org.apache.lucene.index.Term;
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
@QuarkusTest
class QueryParserServiceTest {

    @Inject
    QueryParserService queryParser;

    @Test
    void testParseSimpleTermQuery() {
        // When
        var result = queryParser.parseQuery("hello")
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        // Then
        result.assertCompleted();
        Query query = result.getItem();
        assertThat(query).isInstanceOf(BooleanQuery.class);

        BooleanQuery booleanQuery = (BooleanQuery) query;
        assertThat(booleanQuery.clauses()).isNotEmpty();
        // Should search across multiple fields
    }

    @Test
    void testParseBooleanAndQuery() {
        // When
        var result = queryParser.parseQuery("java AND spring")
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        // Then
        result.assertCompleted();
        Query query = result.getItem();
        assertThat(query).isInstanceOf(BooleanQuery.class);

        BooleanQuery booleanQuery = (BooleanQuery) query;
        // Should contain AND logic
        assertThat(booleanQuery.toString()).contains("java").contains("spring");
    }

    @Test
    void testParseBooleanOrQuery() {
        // When
        var result = queryParser.parseQuery("java OR python")
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        // Then
        result.assertCompleted();
        Query query = result.getItem();
        assertThat(query).isInstanceOf(BooleanQuery.class);

        BooleanQuery booleanQuery = (BooleanQuery) query;
        assertThat(booleanQuery.toString()).contains("java").contains("python");
    }

    @Test
    void testParseNotQuery() {
        // When
        var result = queryParser.parseQuery("java NOT spring")
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        // Then
        result.assertCompleted();
        Query query = result.getItem();
        assertThat(query).isInstanceOf(BooleanQuery.class);

        BooleanQuery booleanQuery = (BooleanQuery) query;
        assertThat(booleanQuery.toString()).contains("java").contains("spring");
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
        assertThat(query).isInstanceOf(BooleanQuery.class);

        BooleanQuery booleanQuery = (BooleanQuery) query;
        // Should contain wildcard queries
        assertThat(booleanQuery.toString()).contains("test*");
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
        // When
        var result = queryParser.parseQuery("language:java AND entity_type:class")
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        // Then
        result.assertCompleted();
        Query query = result.getItem();
        String queryStr = query.toString();
        assertThat(queryStr).contains("language:java");
        assertThat(queryStr).contains("entity_type:class");
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
}
