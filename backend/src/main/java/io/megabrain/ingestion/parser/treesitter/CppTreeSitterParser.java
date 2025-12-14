/*
 * Copyright (c) 2025 MegaBrain Contributors
 * Licensed under the MIT License - see LICENSE file for details.
 */

package io.megabrain.ingestion.parser.treesitter;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import io.github.treesitter.jtreesitter.Language;
import io.github.treesitter.jtreesitter.Node;
import io.github.treesitter.jtreesitter.Tree;
import io.megabrain.ingestion.parser.GrammarManager;
import io.megabrain.ingestion.parser.GrammarSpec;
import io.megabrain.ingestion.parser.TextChunk;

/**
 * Tree-sitter parser for C++ source code.
 */
public class CppTreeSitterParser extends TreeSitterParser {

    private static final String LANGUAGE = "cpp";
    private static final Set<String> SUPPORTED_EXTENSIONS = Set.of("cpp", "cc", "cxx", "hpp", "hh", "h");
    private static final String LIBRARY_ENV = "TREE_SITTER_CPP_LIB";
    private static final String LIBRARY_PROPERTY = "tree.sitter.cpp.library";
    private static final String LANGUAGE_SYMBOL = "tree_sitter_cpp";

    public CppTreeSitterParser() {
        this(new GrammarManager());
    }

    public CppTreeSitterParser(GrammarManager grammarManager) {
        this(grammarManager.languageSupplier(CPP_SPEC), grammarManager.nativeLoader(CPP_SPEC));
    }

    CppTreeSitterParser(Supplier<Language> languageSupplier, Runnable nativeLoader) {
        super(LANGUAGE, SUPPORTED_EXTENSIONS, languageSupplier, nativeLoader);
    }

    @Override
    protected List<QueryDefinition> languageQueries() {
        return List.of(
                new QueryDefinition("functions", """
                        (function_definition
                            declarator: (function_declarator
                                declarator: (identifier) @function.name
                                parameters: (parameter_list) @function.parameters)?)
                        """),
                new QueryDefinition("classes", """
                        (class_specifier
                            name: (type_identifier) @class.name)?
                        """),
                new QueryDefinition("namespaces", """
                        (namespace_definition
                            name: (identifier) @namespace.name)?
                        """)
        );
    }

    @Override
    protected List<TextChunk> extractChunks(Node rootNode, Tree tree, TreeSitterSource source) {
        List<TextChunk> chunks = new ArrayList<>();
        ArrayDeque<String> namespaceStack = new ArrayDeque<>();
        ArrayDeque<String> classStack = new ArrayDeque<>();
        Set<String> seen = Collections.newSetFromMap(new ConcurrentHashMap<>());
        walk(rootNode, source, chunks, namespaceStack, classStack, null, seen);
        return chunks;
    }

    private void walk(Node node,
                      TreeSitterSource source,
                      List<TextChunk> out,
                      ArrayDeque<String> namespaceStack,
                      ArrayDeque<String> classStack,
                      String templateParams,
                      Set<String> seen) {
        if ("namespace_definition".equals(node.getType())) {
            Optional<String> name = identifier(node, "name");
            name.ifPresent(namespaceStack::push);
            node.getNamedChildren().forEach(child -> walk(child, source, out, namespaceStack, classStack, templateParams, seen));
            name.ifPresent(n -> namespaceStack.pop());
            return;
        }

        if ("template_declaration".equals(node.getType())) {
            String tpl = source.slice(node.getStartByte(), Math.min(node.getEndByte(), node.getStartByte() + 200)).trim();
            node.getNamedChildren().forEach(child -> walk(child, source, out, namespaceStack, classStack, tpl, seen));
            return;
        }

        if ("class_specifier".equals(node.getType()) || "struct_specifier".equals(node.getType())) {
            processClassLike(node, source, out, namespaceStack, classStack, templateParams, seen);
            return;
        }

        if ("function_definition".equals(node.getType())) {
            processFunction(node, source, out, namespaceStack, classStack, templateParams, seen);
        }

        node.getNamedChildren().forEach(child -> walk(child, source, out, namespaceStack, classStack, templateParams, seen));
    }

    private void processClassLike(Node node,
                                  TreeSitterSource source,
                                  List<TextChunk> out,
                                  ArrayDeque<String> namespaceStack,
                                  ArrayDeque<String> classStack,
                                  String templateParams,
                                  Set<String> seen) {
        Optional<String> name = identifier(node, "name");
        if (name.isEmpty()) {
            return;
        }
        String entityType = "class_specifier".equals(node.getType()) ? "class" : "struct";
        String qualifiedName = qualify(namespaceStack, classStack, name.get());
        Map<String, String> attributes = new LinkedHashMap<>();
        sliceField(node, "base_class_clause", source).ifPresent(base -> attributes.put("bases", base));
        sliceField(node, "name", source).ifPresent(id -> attributes.put("identifier", id));
        if (templateParams != null) {
            attributes.put("template_parameters", templateParams);
        }
        addChunk(out, toChunk(node, entityType, qualifiedName, source, attributes), seen);

        classStack.push(name.get());
        node.getNamedChildren().forEach(child -> walk(child, source, out, namespaceStack, classStack, templateParams, seen));
        classStack.pop();
    }

    private void processFunction(Node node,
                                 TreeSitterSource source,
                                 List<TextChunk> out,
                                 ArrayDeque<String> namespaceStack,
                                 ArrayDeque<String> classStack,
                                  String templateParams,
                                  Set<String> seen) {
        Optional<Node> declarator = node.getChildByFieldName("declarator");
        Optional<String> name = declarator.flatMap(this::findIdentifier);
        if (name.isEmpty()) {
            return;
        }
        boolean insideClass = !classStack.isEmpty();
        String entityType = insideClass ? "method" : "function";
        String qualifiedName = qualify(namespaceStack, classStack, name.get());
        Map<String, String> attributes = new LinkedHashMap<>();
        declarator
                .flatMap(d -> d.getChildByFieldName("parameters"))
                .ifPresent(params -> attributes.put("parameters", source.slice(params.getStartByte(), params.getEndByte())));
        sliceField(node, "type", source).ifPresent(type -> attributes.put("return_type", type));
        if (templateParams != null) {
            attributes.put("template_parameters", templateParams);
        }
        addChunk(out, toChunk(node, entityType, qualifiedName, source, attributes), seen);
    }

    private Optional<String> identifier(Node node, String fieldName) {
        return node.getChildByFieldName(fieldName).map(Node::getText).map(String::trim);
    }

    private Optional<String> sliceField(Node node, String fieldName, TreeSitterSource source) {
        return node.getChildByFieldName(fieldName)
                .map(child -> source.slice(child.getStartByte(), child.getEndByte()).trim());
    }

    private Optional<String> findIdentifier(Node node) {
        if ("identifier".equals(node.getType()) || "field_identifier".equals(node.getType()) || "type_identifier".equals(node.getType())) {
            return Optional.ofNullable(node.getText()).map(String::trim);
        }
        for (Node child : node.getNamedChildren()) {
            Optional<String> found = findIdentifier(child);
            if (found.isPresent()) {
                return found;
            }
        }
        return Optional.empty();
    }

    private String qualify(ArrayDeque<String> namespaceStack, ArrayDeque<String> classStack, String leaf) {
        List<String> parts = new ArrayList<>();
        parts.addAll(new ArrayDeque<>(namespaceStack));
        parts.addAll(classStack);
        parts.add(leaf);
        return String.join(".", parts);
    }

    private void addChunk(List<TextChunk> out, TextChunk chunk, Set<String> seen) {
        String key = chunk.entityType() + "|" + chunk.entityName() + "|" + chunk.startByte() + "|" + chunk.endByte();
        if (seen.add(key)) {
            out.add(chunk);
        }
    }

    private static final GrammarSpec CPP_SPEC =
            new GrammarSpec(
                    LANGUAGE,
                    LANGUAGE_SYMBOL,
                    "tree-sitter-cpp",
                    LIBRARY_PROPERTY,
                    LIBRARY_ENV,
                    "tree-sitter-cpp",
                    "0.21.0"
            );
}

