/*
 * Copyright (c) 2025 MegaBrain Contributors
 * Licensed under the MIT License - see LICENSE file for details.
 */

package io.megabrain.core;

import java.io.Reader;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.CharArraySet;
import org.apache.lucene.analysis.LowerCaseFilter;
import org.apache.lucene.analysis.StopFilter;
import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.miscellaneous.WordDelimiterGraphFilter;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute;

/**
 * Code-aware analyzer for intelligent tokenization of programming code.
 * <p>
 * This analyzer is designed specifically for code search and provides:
 * - CamelCase identifier splitting (getUserName → get, user, name)
 * - Snake_case identifier splitting (get_user_name → get, user, name)
 * - Preservation of important code constructs
 * - Improved search relevance for code patterns
 * <p>
 * The analyzer uses a pipeline of tokenizers and filters:
 * 1. StandardTokenizer for basic tokenization
 * 2. WordDelimiterGraphFilter for compound word splitting
 * 3. LowerCaseFilter for case normalization
 * 4. StopFilter for removing common programming noise words
 */
public class CodeAwareAnalyzer extends Analyzer {

    // Stop words that are common programming noise but not useful for search
    private static final CharArraySet CODE_STOP_WORDS = CharArraySet.unmodifiableSet(
        new CharArraySet(
            java.util.Arrays.asList(
                "a", "an", "and", "are", "as", "at", "be", "by", "for", "from",
                "has", "he", "in", "is", "it", "its", "of", "on", "that", "the",
                "to", "was", "will", "with", "would",
                // Programming-specific noise words
                "public", "private", "protected", "static", "final", "class",
                "interface", "enum", "abstract", "extends", "implements",
                "import", "return", "void", "int", "long", "double",
                "float", "boolean", "char", "byte", "short", "new", "this",
                "super", "null", "true", "false", "if", "else", "for", "while",
                "do", "switch", "case", "default", "try", "catch", "finally",
                "throw", "throws", "break", "continue"
            ),
            true // ignore case
        )
    );

    @Override
    protected TokenStreamComponents createComponents(String fieldName) {
        // Use StandardTokenizer as the base tokenizer - handles most text patterns well
        Tokenizer tokenizer = new StandardTokenizer();

        // Create the token filter pipeline
        TokenStream tokenStream = tokenizer;

        // WordDelimiterGraphFilter for basic compound word splitting
        tokenStream = new WordDelimiterGraphFilter(tokenStream,
            WordDelimiterGraphFilter.GENERATE_WORD_PARTS |
            WordDelimiterGraphFilter.GENERATE_NUMBER_PARTS |
            WordDelimiterGraphFilter.SPLIT_ON_CASE_CHANGE |
            WordDelimiterGraphFilter.SPLIT_ON_NUMERICS |
            WordDelimiterGraphFilter.PRESERVE_ORIGINAL |
            WordDelimiterGraphFilter.STEM_ENGLISH_POSSESSIVE,
            null); // no protected words

        // Custom code-aware splitting for better camelCase and snake_case handling
        tokenStream = new CodeSplittingFilter(tokenStream);

        // Convert to lowercase for case-insensitive search
        tokenStream = new LowerCaseFilter(tokenStream);

        // Remove stop words that don't add value to code search
        tokenStream = new StopFilter(tokenStream, CODE_STOP_WORDS);

        return new TokenStreamComponents(tokenizer, tokenStream);
    }

    @Override
    protected Reader initReader(String fieldName, Reader reader) {
        // No special reader initialization needed
        return super.initReader(fieldName, reader);
    }

    /**
     * Gets the stop words set used by this analyzer.
     * Useful for testing and debugging.
     */
    public static CharArraySet getStopWords() {
        return CODE_STOP_WORDS;
    }

    /**
     * Custom token filter that provides enhanced code-aware token splitting.
     * This filter handles cases that WordDelimiterGraphFilter misses, particularly:
     * - Better camelCase splitting (e.g., "XMLParser" -> "xml", "parser")
     * - Snake_case splitting (e.g., "get_user_name" -> "get", "user", "name")
     * - Mixed patterns
     */
    private static class CodeSplittingFilter extends TokenFilter {

        private final CharTermAttribute termAtt = addAttribute(CharTermAttribute.class);
        private final PositionIncrementAttribute posIncrAtt = addAttribute(PositionIncrementAttribute.class);

        private java.util.List<String> extraTokens = null;
        private int extraTokenIndex = 0;

        protected CodeSplittingFilter(TokenStream input) {
            super(input);
        }

        @Override
        public boolean incrementToken() throws java.io.IOException {
            // If we have extra tokens to emit from previous processing
            if (extraTokens != null && extraTokenIndex < extraTokens.size()) {
                String token = extraTokens.get(extraTokenIndex++);
                termAtt.setEmpty().append(token);
                posIncrAtt.setPositionIncrement(0); // Same position as original token
                return true;
            }

            // Get next token from input
            if (!input.incrementToken()) {
                return false;
            }

            String originalToken = termAtt.toString();

            // Apply code-aware splitting
            java.util.List<String> splits = splitCodeToken(originalToken);

            if (splits.size() > 1) {
                // Multiple tokens produced - emit original first, then queue the rest
                termAtt.setEmpty().append(originalToken);
                extraTokens = splits;
                extraTokenIndex = 0;
                posIncrAtt.setPositionIncrement(1);
            } else {
                // No additional splitting needed
                extraTokens = null;
                posIncrAtt.setPositionIncrement(1);
            }

            return true;
        }

        @Override
        public void reset() throws java.io.IOException {
            super.reset();
            extraTokens = null;
            extraTokenIndex = 0;
        }

        /**
         * Splits a code token into meaningful parts.
         * Handles camelCase, PascalCase, snake_case, and mixed patterns.
         */
        private java.util.List<String> splitCodeToken(String token) {
            java.util.List<String> parts = new java.util.ArrayList<>();

            // First try snake_case splitting
            if (token.contains("_")) {
                for (String part : token.split("_")) {
                    if (!part.isEmpty()) {
                        parts.addAll(splitCamelCase(part));
                    }
                }
            } else {
                // Try camelCase/PascalCase splitting
                parts.addAll(splitCamelCase(token));
            }

            // Remove duplicates and empty strings
            java.util.Set<String> uniqueParts = new java.util.LinkedHashSet<>();
            for (String part : parts) {
                if (part.length() > 1) { // Skip single characters
                    uniqueParts.add(part.toLowerCase());
                }
            }

            return new java.util.ArrayList<>(uniqueParts);
        }

        /**
         * Splits camelCase and PascalCase identifiers.
         * Examples: "getUserName" -> ["get", "user", "name"]
         *          "XMLParser" -> ["xml", "parser"]
         */
        private java.util.List<String> splitCamelCase(String token) {
            java.util.List<String> parts = new java.util.ArrayList<>();

            if (token.isEmpty()) {
                return parts;
            }

            // Handle all uppercase tokens (like "XML")
            if (token.equals(token.toUpperCase())) {
                parts.add(token.toLowerCase());
                return parts;
            }

            // Split on case transitions
            StringBuilder currentPart = new StringBuilder();
            for (int i = 0; i < token.length(); i++) {
                char c = token.charAt(i);
                boolean isUpper = Character.isUpperCase(c);

                if (i > 0) {
                    char prev = token.charAt(i - 1);
                    boolean prevIsLower = Character.isLowerCase(prev);

                    // Case transition: lower->upper (e.g., "getUser")
                    if (prevIsLower && isUpper) {
                        if (!currentPart.isEmpty()) {
                            parts.add(currentPart.toString().toLowerCase());
                            currentPart.setLength(0);
                        }
                    }
                    // Also split on upper->upper followed by lower (e.g., "XMLParser")
                    else if (i < token.length() - 1 &&
                             Character.isUpperCase(prev) && isUpper &&
                             Character.isLowerCase(token.charAt(i + 1))) {
                        if (!currentPart.isEmpty()) {
                            parts.add(currentPart.toString().toLowerCase());
                            currentPart.setLength(0);
                        }
                    }
                }

                currentPart.append(c);
            }

            // Add the last part
            if (!currentPart.isEmpty()) {
                parts.add(currentPart.toString().toLowerCase());
            }

            return parts;
        }
    }
}
