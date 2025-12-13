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
 * Tree-sitter parser for Rust source code.
 */
public class RustTreeSitterParser extends TreeSitterParser {

    private static final Logger LOG = Logger.getLogger(RustTreeSitterParser.class);
    private static final String LANGUAGE = "rust";
    private static final Set<String> SUPPORTED_EXTENSIONS = Set.of("rs");
    private static final String LIBRARY_ENV = "TREE_SITTER_RUST_LIB";
    private static final String LIBRARY_PROPERTY = "tree.sitter.rust.library";
    private static final String LANGUAGE_SYMBOL = "tree_sitter_rust";

    // Node types and attributes
    private static final String NODE_TRAIT_ITEM = "trait_item";
    private static final String ATTR_TRAIT = "trait";

    private static final Set<String> TYPE_NODE_TYPES = Set.of(
            "struct_item",
            "enum_item",
            "union_item",
            "trait_item",
            "impl_item",
            "type_item"
    );

    public RustTreeSitterParser() {
        this(new io.megabrain.ingestion.parser.GrammarManager());
    }

    public RustTreeSitterParser(io.megabrain.ingestion.parser.GrammarManager grammarManager) {
        this(grammarManager.languageSupplier(RUST_SPEC), grammarManager.nativeLoader(RUST_SPEC));
    }

    RustTreeSitterParser(java.util.function.Supplier<io.github.treesitter.jtreesitter.Language> languageSupplier, Runnable nativeLoader) {
        super(LANGUAGE, SUPPORTED_EXTENSIONS, languageSupplier, nativeLoader);
    }

    @Override
    protected List<QueryDefinition> languageQueries() {
        return List.of(
                new QueryDefinition("functions", """
                        (function_item
                            name: (identifier) @function.name
                            parameters: (parameters) @function.parameters
                            return_type: (_)? @function.return_type)?
                        """),
                new QueryDefinition("structs", """
                        (struct_item
                            name: (type_identifier) @struct.name
                            field_declaration_list: (_)? @struct.fields)?
                        """),
                new QueryDefinition("enums", """
                        (enum_item
                            name: (type_identifier) @enum.name)?
                        """),
                new QueryDefinition("traits", """
                        (trait_item
                            name: (type_identifier) @trait.name)?
                        """),
                new QueryDefinition("impl_blocks", """
                        (impl_item
                            trait: (_)? @impl.trait
                            type: (type_identifier) @impl.type)?
                        """),
                new QueryDefinition("methods", """
                        (impl_item
                            (function_item
                                name: (identifier) @method.name
                                parameters: (parameters) @method.parameters
                                return_type: (_)? @method.return_type)?)?
                        """)
        );
    }

    @Override
    protected List<TextChunk> extractChunks(Node rootNode, Tree tree, TreeSitterSource source) {
        RustContext context = new RustContext();
        List<TextChunk> chunks = new ArrayList<>();
        walk(rootNode, source, context, new ArrayDeque<>(), new HashSet<>(), chunks);
        return chunks;
    }

    private void walk(Node node,
                      TreeSitterSource source,
                      RustContext context,
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
                             RustContext context,
                             ArrayDeque<String> typeStack,
                             Set<String> seen,
                             List<TextChunk> out) {
        Optional<String> name = identifier(node, "name");
        if (name.isEmpty()) {
            return;
        }
        String entityType = typeEntity(node);
        String qualifiedName = qualifyName(typeStack, name.get());
        Map<String, String> attributes = buildTypeAttributes(node, source, context);
        addIfNotSeen(out, seen, toChunk(node, entityType, qualifiedName, source, attributes));

        typeStack.push(name.get());
        node.getNamedChildren().forEach(child -> walk(child, source, context, typeStack, seen, out));
        typeStack.pop();
    }

    private void processFunction(Node node,
                                 TreeSitterSource source,
                                 RustContext context,
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

    private Map<String, String> buildTypeAttributes(Node node, TreeSitterSource source, RustContext context) {
        Map<String, String> attributes = new LinkedHashMap<>();
        sliceField(node, "fields", source).ifPresent(fields -> attributes.put("fields", fields));
        sliceField(node, ATTR_TRAIT, source).ifPresent(trait -> attributes.put(ATTR_TRAIT, trait));
        sliceField(node, "type", source).ifPresent(type -> attributes.put("type", type));
        return attributes;
    }

    private Map<String, String> buildCallableAttributes(Node node,
                                                        TreeSitterSource source,
                                                        RustContext context,
                                                        ArrayDeque<String> typeStack) {
        Map<String, String> attributes = new LinkedHashMap<>();
        if (!typeStack.isEmpty()) {
            attributes.put("enclosing_type", String.join("::", typeStack));
        }
        sliceField(node, "parameters", source).ifPresent(params -> attributes.put("parameters", params));
        sliceField(node, "return_type", source).ifPresent(ret -> attributes.put("return_type", ret));
        return attributes;
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
        return String.join("::", parts);
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
        return "function_item".equals(node.getType());
    }

    private String typeEntity(Node node) {
        return switch (node.getType()) {
            case "struct_item" -> "struct";
            case "enum_item" -> "enum";
            case "union_item" -> "union";
            case NODE_TRAIT_ITEM -> ATTR_TRAIT;
            case "impl_item" -> "impl";
            case "type_item" -> "type";
            default -> "type";
        };
    }

    private static final io.megabrain.ingestion.parser.GrammarSpec RUST_SPEC =
            new io.megabrain.ingestion.parser.GrammarSpec(
                    LANGUAGE,
                    LANGUAGE_SYMBOL,
                    "tree-sitter-rust",
                    LIBRARY_PROPERTY,
                    LIBRARY_ENV,
                    "tree-sitter-rust",
                    "0.23.0"
            );

    private record RustContext() {
    }
}
