/*
 * Copyright (c) 2025 MegaBrain Contributors
 * Licensed under the MIT License - see LICENSE file for details.
 */

package io.megabrain.ingestion.parser.treesitter;

import io.github.treesitter.jtreesitter.Language;
import io.github.treesitter.jtreesitter.Node;
import io.github.treesitter.jtreesitter.Tree;
import io.megabrain.ingestion.parser.TextChunk;
import org.jboss.logging.Logger;

import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Tree-sitter parser for JavaScript source code (includes JS and JSX).
 */
public class JavaScriptTreeSitterParser extends TreeSitterParser {

    private static final Logger LOG = Logger.getLogger(JavaScriptTreeSitterParser.class);
    private static final String LANGUAGE = "javascript";
    private static final Set<String> SUPPORTED_EXTENSIONS = Set.of("js", "jsx", "mjs", "cjs");
    private static final String LIBRARY_ENV = "TREE_SITTER_JAVASCRIPT_LIB";
    private static final String LIBRARY_PROPERTY = "tree.sitter.javascript.library";
    private static final String LANGUAGE_SYMBOL = "tree_sitter_javascript";

    public JavaScriptTreeSitterParser() {
        this(new io.megabrain.ingestion.parser.GrammarManager());
    }

    public JavaScriptTreeSitterParser(io.megabrain.ingestion.parser.GrammarManager grammarManager) {
        this(grammarManager.languageSupplier(JS_SPEC), grammarManager.nativeLoader(JS_SPEC));
    }

    JavaScriptTreeSitterParser(Supplier<Language> languageSupplier, Runnable nativeLoader) {
        super(LANGUAGE, SUPPORTED_EXTENSIONS, languageSupplier, nativeLoader);
    }

    @Override
    protected List<QueryDefinition> languageQueries() {
        return List.of(
                new QueryDefinition("functions", """
                        (function_declaration
                            name: (identifier) @function.name
                            parameters: (formal_parameters) @function.parameters)?
                        """),
                new QueryDefinition("classes", """
                        (class_declaration
                            name: (identifier) @class.name)?
                        """),
                new QueryDefinition("methods", """
                        (method_definition
                            name: (property_identifier) @method.name
                            parameters: (formal_parameters) @method.parameters)?
                        """)
        );
    }

    @Override
    protected List<TextChunk> extractChunks(Node rootNode, Tree tree, TreeSitterSource source) {
        List<TextChunk> chunks = new ArrayList<>();
        ArrayDeque<String> classStack = new ArrayDeque<>();
        Set<String> seen = java.util.Collections.newSetFromMap(new java.util.concurrent.ConcurrentHashMap<>());
        walk(rootNode, source, classStack, chunks, seen);
        return chunks;
    }

    private void walk(Node node,
                      TreeSitterSource source,
                      ArrayDeque<String> classStack,
                      List<TextChunk> out,
                      Set<String> seen) {
        if (isClassNode(node)) {
            processClass(node, source, classStack, out, seen);
            return;
        }
        if (isMethodNode(node)) {
            processCallable(node, source, classStack, out, seen, true);
        } else if (isFunctionNode(node)) {
            processCallable(node, source, classStack, out, seen, false);
        }
        node.getNamedChildren().forEach(child -> walk(child, source, classStack, out, seen));
    }

    private void processClass(Node node,
                              TreeSitterSource source,
                              ArrayDeque<String> classStack,
                              List<TextChunk> out,
                              Set<String> seen) {
        Optional<String> name = identifier(node, "name");
        if (name.isEmpty()) {
            return;
        }
        Map<String, String> attributes = new LinkedHashMap<>();
        sliceField(node, "superclass", source).ifPresent(superClass -> attributes.put("superclass", superClass));
        addIfNotSeen(out, seen, toChunk(node, "class", name.get(), source, attributes));

        classStack.push(name.get());
        node.getNamedChildren().forEach(child -> walk(child, source, classStack, out, seen));
        classStack.pop();
    }

    private void processCallable(Node node,
                                 TreeSitterSource source,
                                 ArrayDeque<String> classStack,
                                 List<TextChunk> out,
                                 Set<String> seen,
                                 boolean isMethod) {
        Optional<String> name = identifier(node, "name");
        if (name.isEmpty()) {
            return;
        }
        boolean insideClass = !classStack.isEmpty();
        String entityType = isMethod || insideClass ? "method" : "function";
        String entityName = insideClass ? String.join(".", classStack) + "." + name.get() : name.get();
        Map<String, String> attributes = new LinkedHashMap<>();
        attributes.put("async", Boolean.toString(isAsync(node, source)));
        sliceField(node, "parameters", source).ifPresent(params -> attributes.put("parameters", params));
        addIfNotSeen(out, seen, toChunk(node, entityType, entityName, source, attributes));
    }

    private boolean isClassNode(Node node) {
        return "class_declaration".equals(node.getType());
    }

    private boolean isFunctionNode(Node node) {
        return "function_declaration".equals(node.getType());
    }

    private boolean isMethodNode(Node node) {
        return "method_definition".equals(node.getType());
    }

    private Optional<String> identifier(Node node, String fieldName) {
        return node.getChildByFieldName(fieldName).map(Node::getText).map(String::trim);
    }

    private Optional<String> sliceField(Node node, String fieldName, TreeSitterSource source) {
        return node.getChildByFieldName(fieldName)
                .map(child -> source.slice(child.getStartByte(), child.getEndByte()).trim());
    }

    private boolean isAsync(Node node, TreeSitterSource source) {
        String snippet = source.slice(node.getStartByte(), Math.min(node.getEndByte(), node.getStartByte() + 16)).trim();
        return snippet.startsWith("async");
    }

    private void addIfNotSeen(List<TextChunk> out, Set<String> seen, TextChunk chunk) {
        String key = chunk.entityName() + "|" + chunk.startByte() + "|" + chunk.endByte();
        if (seen.add(key)) {
            out.add(chunk);
        }
    }

    private static final io.megabrain.ingestion.parser.GrammarSpec JS_SPEC =
            new io.megabrain.ingestion.parser.GrammarSpec(
                    LANGUAGE,
                    LANGUAGE_SYMBOL,
                    "tree-sitter-javascript",
                    LIBRARY_PROPERTY,
                    LIBRARY_ENV,
                    "tree-sitter-javascript",
                    "0.21.0"
            );
}

