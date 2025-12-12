/*
 * Copyright (c) 2025 MegaBrain Contributors
 * Licensed under the MIT License - see LICENSE file for details.
 */

package io.megabrain.ingestion.parser;

import io.megabrain.ingestion.parser.java.JavaParserService;
import io.megabrain.ingestion.parser.treesitter.CTreeSitterParser;
import io.megabrain.ingestion.parser.treesitter.CppTreeSitterParser;
import io.megabrain.ingestion.parser.treesitter.JavaScriptTreeSitterParser;
import io.megabrain.ingestion.parser.treesitter.PythonTreeSitterParser;
import io.megabrain.ingestion.parser.treesitter.TypeScriptTreeSitterParser;
import jakarta.enterprise.context.ApplicationScoped;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry that routes file extensions to the appropriate {@link CodeParser}.
 * Supports dynamic registration of parsers and provides O(1) lookup performance.
 */
@ApplicationScoped
public class ParserRegistry {

    private final Map<String, ParserFactory> factoriesByExtension = new ConcurrentHashMap<>();
    private final Map<String, CodeParser> parsersByExtension = new ConcurrentHashMap<>();

    public ParserRegistry() {
        registerDefaultParsers();
    }

    ParserRegistry(Map<String, ParserFactory> factoriesByExtension) {
        Objects.requireNonNull(factoriesByExtension, "factoriesByExtension");
        this.factoriesByExtension.putAll(factoriesByExtension);
    }

    /**
     * Finds the parser for the given path based on file extension (case-insensitive).
     *
     * @param filePath path to resolve
     * @return optional parser, empty when no mapping exists
     */
    public Optional<CodeParser> findParser(Path filePath) {
        if (filePath == null) {
            return Optional.empty();
        }
        return findParser(filePath.getFileName().toString());
    }

    /**
     * Finds the parser for the given file name based on extension (case-insensitive).
     *
     * @param fileName file name to resolve
     * @return optional parser, empty when no mapping exists
     */
    public Optional<CodeParser> findParser(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return Optional.empty();
        }
        int dot = fileName.lastIndexOf('.');
        if (dot < 0 || dot == fileName.length() - 1) {
            return Optional.empty();
        }
        String ext = fileName.substring(dot + 1).toLowerCase(Locale.ROOT);

        // First try to get cached parser instance
        CodeParser cached = parsersByExtension.get(ext);
        if (cached != null) {
            return Optional.of(cached);
        }

        // If not cached, create from factory and cache it
        ParserFactory factory = factoriesByExtension.get(ext);
        if (factory != null) {
            CodeParser parser = factory.createParser();
            parsersByExtension.put(ext, parser);
            return Optional.of(parser);
        }

        return Optional.empty();
    }

    /**
     * Dynamically registers a parser for the given file extensions.
     * Multiple extensions can map to the same parser factory.
     *
     * @param factory the parser factory to register
     * @param extensions list of file extensions (without dots, case-insensitive)
     * @throws IllegalArgumentException if factory or extensions is null/empty
     */
    public void registerParser(ParserFactory factory, List<String> extensions) {
        if (factory == null) {
            throw new IllegalArgumentException("factory cannot be null");
        }
        if (extensions == null || extensions.isEmpty()) {
            throw new IllegalArgumentException("extensions cannot be null or empty");
        }

        for (String ext : extensions) {
            if (ext == null || ext.isBlank()) {
                throw new IllegalArgumentException("extension cannot be null or blank");
            }
            String normalizedExt = ext.toLowerCase(Locale.ROOT);
            factoriesByExtension.put(normalizedExt, factory);
            // Remove any cached parser for this extension to force recreation
            parsersByExtension.remove(normalizedExt);
        }
    }

    /**
     * Dynamically registers a parser for a single file extension.
     *
     * @param factory the parser factory to register
     * @param extension file extension (without dot, case-insensitive)
     * @throws IllegalArgumentException if factory or extension is null/empty
     */
    public void registerParser(ParserFactory factory, String extension) {
        registerParser(factory, List.of(extension));
    }

    /**
     * Unregisters a parser for the given file extension.
     * Removes both the factory mapping and any cached parser instance.
     *
     * @param extension file extension (without dot, case-insensitive)
     */
    public void unregisterParser(String extension) {
        if (extension == null || extension.isBlank()) {
            return;
        }
        String normalizedExt = extension.toLowerCase(Locale.ROOT);
        factoriesByExtension.remove(normalizedExt);
        parsersByExtension.remove(normalizedExt);
    }

    /**
     * @return immutable view of supported extensions.
     */
    public List<String> supportedExtensions() {
        return List.copyOf(factoriesByExtension.keySet());
    }

    private void registerDefaultParsers() {
        // Register Tree-sitter parsers
        registerParser(new PythonParserFactory(), List.of("py", "pyw", "pyi"));
        registerParser(new JavaScriptParserFactory(), List.of("js", "jsx", "mjs", "cjs"));
        registerParser(new TypeScriptParserFactory(), List.of("ts", "tsx"));
        registerParser(new CParserFactory(), List.of("c", "h")); // prefer C for .h
        registerParser(new CppParserFactory(), List.of("cpp", "cc", "cxx", "hpp", "hh"));

        // Register Java parser
        registerParser(new JavaParserFactory(), List.of("java"));
    }

    /**
     * Factory for Python Tree-sitter parsers.
     */
    private static class PythonParserFactory implements ParserFactory {
        @Override
        public CodeParser createParser() {
            return new PythonTreeSitterParser(new GrammarManager());
        }

        @Override
        public String language() {
            return "python";
        }
    }

    /**
     * Factory for JavaScript Tree-sitter parsers.
     */
    private static class JavaScriptParserFactory implements ParserFactory {
        @Override
        public CodeParser createParser() {
            return new JavaScriptTreeSitterParser(new GrammarManager());
        }

        @Override
        public String language() {
            return "javascript";
        }
    }

    /**
     * Factory for TypeScript Tree-sitter parsers.
     */
    private static class TypeScriptParserFactory implements ParserFactory {
        @Override
        public CodeParser createParser() {
            return new TypeScriptTreeSitterParser(new GrammarManager());
        }

        @Override
        public String language() {
            return "typescript";
        }
    }

    /**
     * Factory for C Tree-sitter parsers.
     */
    private static class CParserFactory implements ParserFactory {
        @Override
        public CodeParser createParser() {
            return new CTreeSitterParser(new GrammarManager());
        }

        @Override
        public String language() {
            return "c";
        }
    }

    /**
     * Factory for C++ Tree-sitter parsers.
     */
    private static class CppParserFactory implements ParserFactory {
        @Override
        public CodeParser createParser() {
            return new CppTreeSitterParser(new GrammarManager());
        }

        @Override
        public String language() {
            return "cpp";
        }
    }

    /**
     * Factory for Java parsers.
     */
    private static class JavaParserFactory implements ParserFactory {
        @Override
        public CodeParser createParser() {
            return new JavaParserService();
        }

        @Override
        public String language() {
            return "java";
        }
    }
}

