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
 * Tree-sitter parser for TypeScript source code (ts / tsx).
 */
public class TypeScriptTreeSitterParser extends TreeSitterParser {

    private static final Logger LOG = Logger.getLogger(TypeScriptTreeSitterParser.class);
    private static final String LANGUAGE = "typescript";
    private static final Set<String> SUPPORTED_EXTENSIONS = Set.of("ts", "tsx");
    private static final String LIBRARY_ENV = "TREE_SITTER_TYPESCRIPT_LIB";
    private static final String LIBRARY_PROPERTY = "tree.sitter.typescript.library";
    private static final String LANGUAGE_SYMBOL = "tree_sitter_typescript";

    public TypeScriptTreeSitterParser() {
        this(new io.megabrain.ingestion.parser.GrammarManager());
    }

    public TypeScriptTreeSitterParser(io.megabrain.ingestion.parser.GrammarManager grammarManager) {
        this(grammarManager.languageSupplier(TS_SPEC), grammarManager.nativeLoader(TS_SPEC));
    }

    TypeScriptTreeSitterParser(Supplier<Language> languageSupplier, Runnable nativeLoader) {
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
                        (method_signature
                            name: (property_identifier) @method.name
                            parameters: (formal_parameters) @method.parameters)?
                        """),
                new QueryDefinition("interfaces", """
                        (interface_declaration
                            name: (type_identifier) @interface.name)?
                        """),
                new QueryDefinition("type_alias", """
                        (type_alias_declaration
                            name: (type_identifier) @type.name)?
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
            processTypeLike(node, source, classStack, out, seen, "class");
            return;
        }
        if (isInterfaceNode(node)) {
            processTypeLike(node, source, classStack, out, seen, "interface");
            return;
        }
        if (isTypeAliasNode(node)) {
            processTypeAlias(node, source, out, seen);
            return;
        }
        if (isMethodNode(node)) {
            processCallable(node, source, classStack, out, seen, true);
        } else if (isFunctionNode(node)) {
            processCallable(node, source, classStack, out, seen, false);
        }
        node.getNamedChildren().forEach(child -> walk(child, source, classStack, out, seen));
    }

    private void processTypeLike(Node node,
                                 TreeSitterSource source,
                                 ArrayDeque<String> classStack,
                                 List<TextChunk> out,
                                 Set<String> seen,
                                 String entityType) {
        Optional<String> name = identifier(node, "name");
        if (name.isEmpty()) {
            return;
        }
        Map<String, String> attributes = new LinkedHashMap<>();
        sliceField(node, "type_parameters", source).ifPresent(tp -> attributes.put("type_parameters", tp));
        sliceField(node, "heritage_clause", source).ifPresent(heritage -> attributes.put("heritage", heritage));
        addIfNotSeen(out, seen, toChunk(node, entityType, name.get(), source, attributes));

        if ("class".equals(entityType)) {
            classStack.push(name.get());
            node.getNamedChildren().forEach(child -> walk(child, source, classStack, out, seen));
            classStack.pop();
        }
    }

    private void processTypeAlias(Node node,
                                  TreeSitterSource source,
                                  List<TextChunk> out,
                                  Set<String> seen) {
        Optional<String> name = identifier(node, "name");
        if (name.isEmpty()) {
            return;
        }
        Map<String, String> attributes = new LinkedHashMap<>();
        sliceField(node, "value", source).ifPresent(val -> attributes.put("value", val));
        addIfNotSeen(out, seen, toChunk(node, "type_alias", name.get(), source, attributes));
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
        sliceField(node, "type", source).ifPresent(ret -> attributes.put("return_type", ret));
        addIfNotSeen(out, seen, toChunk(node, entityType, entityName, source, attributes));
    }

    private boolean isClassNode(Node node) {
        return "class_declaration".equals(node.getType());
    }

    private boolean isInterfaceNode(Node node) {
        return "interface_declaration".equals(node.getType());
    }

    private boolean isTypeAliasNode(Node node) {
        return "type_alias_declaration".equals(node.getType());
    }

    private boolean isFunctionNode(Node node) {
        return "function_declaration".equals(node.getType());
    }

    private boolean isMethodNode(Node node) {
        return "method_signature".equals(node.getType()) || "method_definition".equals(node.getType());
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

    private static final io.megabrain.ingestion.parser.GrammarSpec TS_SPEC =
            new io.megabrain.ingestion.parser.GrammarSpec(
                    LANGUAGE,
                    LANGUAGE_SYMBOL,
                    "tree-sitter-typescript",
                    LIBRARY_PROPERTY,
                    LIBRARY_ENV,
                    "tree-sitter-typescript",
                    "0.21.0"
            );
}

