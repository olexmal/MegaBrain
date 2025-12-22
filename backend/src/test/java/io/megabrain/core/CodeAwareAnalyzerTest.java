/*
 * Copyright (c) 2025 MegaBrain Contributors
 * Licensed under the MIT License - see LICENSE file for details.
 */

package io.megabrain.core;

import org.apache.lucene.analysis.CharArraySet;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for CodeAwareAnalyzer.
 * <p>
 * Tests the tokenization behavior for code patterns including:
 * - CamelCase identifier splitting
 * - Snake_case identifier splitting
 * - Stop word filtering
 * - Case normalization
 */
class CodeAwareAnalyzerTest {

    private CodeAwareAnalyzer analyzer;

    @BeforeEach
    void setUp() {
        analyzer = new CodeAwareAnalyzer();
    }

    @AfterEach
    void tearDown() {
        if (analyzer != null) {
            analyzer.close();
        }
    }

    @Test
    void testCamelCaseSplitting() throws IOException {
        // Test basic camelCase splitting - produces original + parts
        List<String> tokens = analyze("getUserName");
        assertThat(tokens).contains("getusername", "get", "user", "name");

        // Test PascalCase splitting
        tokens = analyze("UserServiceImpl");
        assertThat(tokens).contains("userserviceimpl", "user", "service", "impl");

        // Test complex camelCase with numbers
        tokens = analyze("getUserById123");
        assertThat(tokens).contains("getuserbyid123", "get", "user", "id", "123");
    }

    @Test
    void testSnakeCaseSplitting() throws IOException {
        // Test basic snake_case splitting
        List<String> tokens = analyze("get_user_name");
        assertThat(tokens).contains("get_user_name", "get", "user", "name");

        // Test snake_case with numbers
        tokens = analyze("user_id_123");
        assertThat(tokens).contains("user_id_123", "user", "id", "123");

        // Test mixed snake_case
        tokens = analyze("get_user_by_id");
        assertThat(tokens).contains("get_user_by_id", "get", "user", "id");
    }

    @Test
    void testMixedCasePatterns() throws IOException {
        // Test camelCase with underscores (edge case)
        List<String> tokens = analyze("getUser_Name");
        assertThat(tokens).contains("getuser_name", "get", "user", "name");

        // Test multiple consecutive capitals
        tokens = analyze("XMLParser");
        assertThat(tokens).contains("xmlparser", "xml", "parser");

        // Test single character components (filtered out single chars)
        tokens = analyze("aBCdEF");
        assertThat(tokens).contains("abcdef", "cd", "ef");
    }

    @Test
    void testCodeConstructs() throws IOException {
        // Test method signatures
        List<String> tokens = analyze("public void getUserById(int userId)");
        assertThat(tokens).doesNotContain("public", "void", "int")
                // Stop words removed
                .contains("get", "user", "id");

        // Test class declarations
        tokens = analyze("public class UserServiceImpl implements UserService");
        assertThat(tokens).doesNotContain("public", "class", "implements")
                // Stop words removed
                .contains("user", "service", "impl");

        // Test package declarations (should preserve dots as separate tokens)
        tokens = analyze("package com.example.service");
        assertThat(tokens).contains("package", "com", "example", "service");
    }

    @Test
    void testStopWords() throws IOException {
        // Test that programming stop words are removed
        List<String> tokens = analyze("public static final String CONSTANT");
        assertThat(tokens).doesNotContain("public", "static", "final").contains("string", "constant");

        // Test that important words are kept
        tokens = analyze("private User user");
        assertThat(tokens).doesNotContain("private").contains("user");

        // Test that non-stop words are preserved
        tokens = analyze("database connection pool");
        assertThat(tokens).contains("database", "connection", "pool");
    }

    @Test
    void testCaseNormalization() throws IOException {
        // Test uppercase conversion to lowercase
        List<String> tokens = analyze("GET_USER_NAME");
        assertThat(tokens).contains("get_user_name", "get", "user", "name");

        // Test mixed case
        tokens = analyze("GetUserNAME");
        assertThat(tokens).contains("getusername", "get", "user", "name");
    }

    @Test
    void testNumbersAndSpecialChars() throws IOException {
        // Test numbers within identifiers
        List<String> tokens = analyze("user123Name");
        assertThat(tokens).contains("user123name", "user", "123", "name");

        // Test email-like patterns (should be split)
        tokens = analyze("user@example.com");
        assertThat(tokens).contains("user", "example", "com");

        // Test version numbers
        tokens = analyze("version1.2.3");
        assertThat(tokens).contains("version", "1", "2", "3");
    }

    @Test
    void testEmptyAndNullInput() throws IOException {
        // Test empty string
        List<String> tokens = analyze("");
        assertThat(tokens).isEmpty();

        // Test whitespace only
        tokens = analyze("   \t\n  ");
        assertThat(tokens).isEmpty();

        // Test null input should not crash (Lucene handles this)
        tokens = analyze(null);
        assertThat(tokens).isEmpty();
    }

    @Test
    void testStopWordsAccess() {
        // Test that stop words set is accessible
        CharArraySet stopWords = CodeAwareAnalyzer.getStopWords();
        assertThat(stopWords.contains("public".toCharArray())).isTrue();
        assertThat(stopWords.contains("important".toCharArray())).isFalse();
    }

    /**
     * Helper method to analyze text and return list of tokens.
     */
    private List<String> analyze(String text) throws IOException {
        List<String> tokens = new ArrayList<>();

        try (TokenStream tokenStream = analyzer.tokenStream("test", text != null ? text : "")) {
            CharTermAttribute termAttribute = tokenStream.addAttribute(CharTermAttribute.class);
            tokenStream.reset();

            while (tokenStream.incrementToken()) {
                tokens.add(termAttribute.toString());
            }

            tokenStream.end();
        }

        return tokens;
    }
}
