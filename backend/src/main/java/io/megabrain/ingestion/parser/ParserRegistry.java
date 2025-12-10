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

/**
 * Registry that routes file extensions to the appropriate {@link CodeParser}.
 * Lookup is O(1) via pre-built extension map and returns empty for unknown extensions.
 */
@ApplicationScoped
public class ParserRegistry {

    private final Map<String, CodeParser> byExtension;

    public ParserRegistry() {
        this(buildDefaultParsers());
    }

    ParserRegistry(Map<String, CodeParser> parsersByExtension) {
        Objects.requireNonNull(parsersByExtension, "parsersByExtension");
        this.byExtension = Map.copyOf(parsersByExtension);
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
        return Optional.ofNullable(byExtension.get(ext));
    }

    /**
     * @return immutable view of supported extensions.
     */
    public List<String> supportedExtensions() {
        return List.copyOf(byExtension.keySet());
    }

    private static Map<String, CodeParser> buildDefaultParsers() {
        Map<String, CodeParser> map = new HashMap<>();
        GrammarManager grammarManager = new GrammarManager();
        register(map, new PythonTreeSitterParser(grammarManager), List.of("py", "pyw", "pyi"));
        register(map, new JavaScriptTreeSitterParser(grammarManager), List.of("js", "jsx", "mjs", "cjs"));
        register(map, new TypeScriptTreeSitterParser(grammarManager), List.of("ts", "tsx"));
        register(map, new CTreeSitterParser(grammarManager), List.of("c", "h")); // prefer C for .h
        register(map, new CppTreeSitterParser(grammarManager), List.of("cpp", "cc", "cxx", "hpp", "hh"));
        register(map, new JavaParserService(), List.of("java"));
        return map;
    }

    private static void register(Map<String, CodeParser> map, CodeParser parser, List<String> extensions) {
        Objects.requireNonNull(parser, "parser");
        Objects.requireNonNull(extensions, "extensions");
        for (String ext : extensions) {
            map.put(ext.toLowerCase(Locale.ROOT), parser);
        }
    }
}

