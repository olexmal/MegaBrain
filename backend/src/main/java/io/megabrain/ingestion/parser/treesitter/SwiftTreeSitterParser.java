/*
 * Copyright (c) 2025 MegaBrain Contributors
 * Licensed under the MIT License - see LICENSE file for details.
 */

package io.megabrain.ingestion.parser.treesitter;

import io.github.treesitter.jtreesitter.Node;
import io.github.treesitter.jtreesitter.Tree;
import io.megabrain.ingestion.parser.TextChunk;
import org.jboss.logging.Logger;

import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Tree-sitter parser for Swift source code.
 */
public class SwiftTreeSitterParser extends TreeSitterParser {

    private static final Logger LOG = Logger.getLogger(SwiftTreeSitterParser.class);
    private static final String LANGUAGE = "swift";
    private static final Set<String> SUPPORTED_EXTENSIONS = Set.of(LANGUAGE);
    private static final String LIBRARY_ENV = "TREE_SITTER_SWIFT_LIB";
    private static final String LIBRARY_PROPERTY = "tree.sitter.swift.library";
    private static final String LANGUAGE_SYMBOL = "tree_sitter_swift";

    private static final Set<String> TYPE_NODE_TYPES = Set.of(
            "class_declaration",
            "struct_declaration",
            "enum_declaration",
            "protocol_declaration",
            "extension_declaration"
    );

    public SwiftTreeSitterParser() {
        this(new io.megabrain.ingestion.parser.GrammarManager());
    }

    public SwiftTreeSitterParser(io.megabrain.ingestion.parser.GrammarManager grammarManager) {
        this(grammarManager.languageSupplier(SWIFT_SPEC), grammarManager.nativeLoader(SWIFT_SPEC));
    }

    SwiftTreeSitterParser(java.util.function.Supplier<io.github.treesitter.jtreesitter.Language> languageSupplier, Runnable nativeLoader) {
        super(LANGUAGE, SUPPORTED_EXTENSIONS, languageSupplier, nativeLoader);
    }

    @Override
    protected List<QueryDefinition> languageQueries() {
        return List.of(
                new QueryDefinition("classes", """
                        (class_declaration
                            name: (type_identifier) @class.name)?
                        """),
                new QueryDefinition("structs", """
                        (struct_declaration
                            name: (type_identifier) @struct.name)?
                        """),
                new QueryDefinition("enums", """
                        (enum_declaration
                            name: (type_identifier) @enum.name)?
                        """),
                new QueryDefinition("protocols", """
                        (protocol_declaration
                            name: (type_identifier) @protocol.name)?
                        """),
                new QueryDefinition("extensions", """
                        (extension_declaration
                            extended_type: (type_identifier) @extension.type)?
                        """),
                new QueryDefinition("functions", """
                        (function_declaration
                            name: (identifier) @function.name
                            parameter_list: (parameter_list)? @function.parameters
                            return_type: (_)? @function.return_type)?
                        """),
                new QueryDefinition("methods", """
                        (function_declaration
                            name: (identifier) @method.name
                            parameter_list: (parameter_list)? @method.parameters
                            return_type: (_)? @method.return_type)?
                        """)
        );
    }

    @Override
    protected List<TextChunk> extractChunks(Node rootNode, Tree tree, TreeSitterSource source) {
        SwiftContext context = new SwiftContext();
        List<TextChunk> chunks = new ArrayList<>();
        walk(rootNode, source, context, new ArrayDeque<>(), new HashSet<>(), chunks);
        return chunks;
    }

    private void walk(Node node,
                      TreeSitterSource source,
                      SwiftContext context,
                      ArrayDeque<String> typeStack,
                      Set<String> seen,
                      List<TextChunk> out) {
        if (isTypeNode(node)) {
            processType(node, source, context, typeStack, seen, out);
            return;
        }
        if (isFunctionNode(node)) {
            processFunction(node, source, context, typeStack, seen, out);
        }
        node.getNamedChildren().forEach(child -> walk(child, source, context, typeStack, seen, out));
    }

    private void processType(Node node,
                             TreeSitterSource source,
                             SwiftContext context,
                             ArrayDeque<String> typeStack,
                             Set<String> seen,
                             List<TextChunk> out) {
        Optional<String> name = identifier(node, "name");
        Optional<String> extendedType = extractExtendedType(node, source);

        String entityName = name.orElse(extendedType.orElse(""));
        if (entityName.isEmpty()) {
            return;
        }

        String entityType = typeEntity(node);
        String qualifiedName = qualifyName(typeStack, entityName);
        Map<String, String> attributes = buildTypeAttributes(node, source, context);
        extendedType.ifPresent(et -> attributes.put("extended_type", et));

        addIfNotSeen(out, seen, toChunk(node, entityType, qualifiedName, source, attributes));

        typeStack.push(entityName);
        node.getNamedChildren().forEach(child -> walk(child, source, context, typeStack, seen, out));
        typeStack.pop();
    }

    private void processFunction(Node node,
                                 TreeSitterSource source,
                                 SwiftContext context,
                                 ArrayDeque<String> typeStack,
                                 Set<String> seen,
                                 List<TextChunk> out) {
        Optional<String> name = identifier(node, "name");
        if (name.isEmpty()) {
            return;
        }
        String qualifiedName = qualifyName(typeStack, name.get());
        Map<String, String> attributes = buildCallableAttributes(node, source, context, typeStack);
        addIfNotSeen(out, seen, toChunk(node, "function", qualifiedName, source, attributes));
    }

    private Map<String, String> buildTypeAttributes(Node node, TreeSitterSource source, SwiftContext context) {
        Map<String, String> attributes = new LinkedHashMap<>();
        sliceField(node, "generic_parameter_clause", source).ifPresent(gp -> attributes.put("generic_parameters", gp));
        sliceField(node, "type_inheritance_clause", source).ifPresent(inheritance -> attributes.put("inheritance", inheritance));
        return attributes;
    }

    private Map<String, String> buildCallableAttributes(Node node,
                                                        TreeSitterSource source,
                                                        SwiftContext context,
                                                        ArrayDeque<String> typeStack) {
        Map<String, String> attributes = new LinkedHashMap<>();
        if (!typeStack.isEmpty()) {
            attributes.put("enclosing_type", String.join(".", typeStack));
        }
        sliceField(node, "modifiers", source).ifPresent(mods -> attributes.put("modifiers", mods));
        sliceField(node, "parameter_list", source).ifPresent(params -> attributes.put("parameters", params));
        sliceField(node, "return_type", source).ifPresent(ret -> attributes.put("return_type", ret));
        return attributes;
    }

    private Optional<String> extractExtendedType(Node extensionNode, TreeSitterSource source) {
        return extensionNode.getChildByFieldName("extended_type")
                .map(Node::getText)
                .map(String::trim);
    }

    private Optional<String> identifier(Node node, String fieldName) {
        return node.getChildByFieldName(fieldName).map(Node::getText).map(String::trim);
    }

    private Optional<String> sliceField(Node node, String fieldName, TreeSitterSource source) {
        return node.getChildByFieldName(fieldName)
                .map(child -> source.slice(child.getStartByte(), child.getEndByte()).trim());
    }

    private String qualifyName(ArrayDeque<String> typeStack, String leaf) {
        List<String> parts = new ArrayList<>();
        parts.addAll(typeStack);
        parts.add(leaf);
        return String.join(".", parts);
    }

    private void addIfNotSeen(List<TextChunk> out, Set<String> seen, TextChunk chunk) {
        String key = chunk.entityName() + "|" + chunk.startByte() + "|" + chunk.endByte();
        if (seen.add(key)) {
            out.add(chunk);
        }
    }

    private boolean isTypeNode(Node node) {
        return TYPE_NODE_TYPES.contains(node.getType());
    }

    private boolean isFunctionNode(Node node) {
        return "function_declaration".equals(node.getType());
    }

    private String typeEntity(Node node) {
        return switch (node.getType()) {
            case "struct_declaration" -> "struct";
            case "enum_declaration" -> "enum";
            case "protocol_declaration" -> "protocol";
            case "extension_declaration" -> "extension";
            default -> "class";
        };
    }

    private static final io.megabrain.ingestion.parser.GrammarSpec SWIFT_SPEC =
            new io.megabrain.ingestion.parser.GrammarSpec(
                    LANGUAGE,
                    LANGUAGE_SYMBOL,
                    "tree-sitter-swift",
                    LIBRARY_PROPERTY,
                    LIBRARY_ENV,
                    "tree-sitter-swift",
                    "0.6.0"
            );

    private record SwiftContext() {
    }
}
