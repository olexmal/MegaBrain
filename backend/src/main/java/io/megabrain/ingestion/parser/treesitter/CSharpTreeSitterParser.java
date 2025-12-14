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
 * Tree-sitter parser for C# source code.
 */
public class CSharpTreeSitterParser extends TreeSitterParser {

    private static final Logger LOG = Logger.getLogger(CSharpTreeSitterParser.class);
    private static final String LANGUAGE = "csharp";
    private static final Set<String> SUPPORTED_EXTENSIONS = Set.of("cs");
    private static final String LIBRARY_ENV = "TREE_SITTER_CSHARP_LIB";
    private static final String LIBRARY_PROPERTY = "tree.sitter.csharp.library";
    private static final String LANGUAGE_SYMBOL = "tree_sitter_csharp";

    // Attribute keys
    private static final String ATTR_NAMESPACE = "namespace";
    private static final String ATTR_MODIFIERS = "modifiers";
    private static final String ATTR_ENCLOSING_TYPE = "enclosing_type";

    // AST field names
    private static final String FIELD_MODIFIERS = "modifiers";

    private static final Set<String> TYPE_NODE_TYPES = Set.of(
            "class_declaration",
            "interface_declaration",
            "struct_declaration",
            "enum_declaration",
            "record_declaration"
    );

    public CSharpTreeSitterParser() {
        this(new io.megabrain.ingestion.parser.GrammarManager());
    }

    public CSharpTreeSitterParser(io.megabrain.ingestion.parser.GrammarManager grammarManager) {
        this(grammarManager.languageSupplier(CSHARP_SPEC), grammarManager.nativeLoader(CSHARP_SPEC));
    }

    CSharpTreeSitterParser(java.util.function.Supplier<io.github.treesitter.jtreesitter.Language> languageSupplier, Runnable nativeLoader) {
        super(LANGUAGE, SUPPORTED_EXTENSIONS, languageSupplier, nativeLoader);
    }

    @Override
    protected List<QueryDefinition> languageQueries() {
        return List.of(
                new QueryDefinition("classes", """
                        (class_declaration
                            name: (identifier) @class.name)?
                        """),
                new QueryDefinition("interfaces", """
                        (interface_declaration
                            name: (identifier) @interface.name)?
                        """),
                new QueryDefinition("structs", """
                        (struct_declaration
                            name: (identifier) @struct.name)?
                        """),
                new QueryDefinition("enums", """
                        (enum_declaration
                            name: (identifier) @enum.name)?
                        """),
                new QueryDefinition("records", """
                        (record_declaration
                            name: (identifier) @record.name)?
                        """),
                new QueryDefinition("methods", """
                        (method_declaration
                            name: (identifier) @method.name
                            parameter_list: (parameter_list)? @method.parameters
                            type: (_)? @method.return_type)?
                        """),
                new QueryDefinition("properties", """
                        (property_declaration
                            name: (identifier) @property.name
                            type: (_)? @property.type)?
                        """),
                new QueryDefinition("fields", """
                        (field_declaration
                            (variable_declaration
                                name: (identifier) @field.name
                                type: (_)? @field.type)?)?
                        """)
        );
    }

    @Override
    protected List<TextChunk> extractChunks(Node rootNode, Tree tree, TreeSitterSource source) {
        CSharpContext context = buildContext(rootNode, source);
        List<TextChunk> chunks = new ArrayList<>();
        walk(rootNode, source, context, new ArrayDeque<>(), new HashSet<>(), chunks);
        return chunks;
    }

    private void walk(Node node,
                      TreeSitterSource source,
                      CSharpContext context,
                      ArrayDeque<String> typeStack,
                      Set<String> seen,
                      List<TextChunk> out) {
        if (isTypeNode(node)) {
            processType(node, source, context, typeStack, seen, out);
            return;
        }
        if (isMethodNode(node)) {
            processMethod(node, source, context, typeStack, seen, out);
        } else if (isPropertyNode(node)) {
            processProperty(node, source, context, typeStack, seen, out);
        } else if (isFieldNode(node)) {
            processField(node, source, context, typeStack, seen, out);
        }
        node.getNamedChildren().forEach(child -> walk(child, source, context, typeStack, seen, out));
    }

    private void processType(Node node,
                             TreeSitterSource source,
                             CSharpContext context,
                             ArrayDeque<String> typeStack,
                             Set<String> seen,
                             List<TextChunk> out) {
        Optional<String> name = identifier(node, "name");
        if (name.isEmpty()) {
            return;
        }
        String entityType = typeEntity(node);
        String qualifiedName = qualifyName(context.namespace(), typeStack, name.get());
        Map<String, String> attributes = buildTypeAttributes(node, source, context);
        addIfNotSeen(out, seen, toChunk(node, entityType, qualifiedName, source, attributes));

        typeStack.push(name.get());
        node.getNamedChildren().forEach(child -> walk(child, source, context, typeStack, seen, out));
        typeStack.pop();
    }

    private void processMethod(Node node,
                               TreeSitterSource source,
                               CSharpContext context,
                               ArrayDeque<String> typeStack,
                               Set<String> seen,
                               List<TextChunk> out) {
        Optional<String> name = identifier(node, "name");
        if (name.isEmpty()) {
            return;
        }
        String qualifiedName = qualifyName(context.namespace(), typeStack, name.get());
        Map<String, String> attributes = buildCallableAttributes(node, source, context, typeStack);
        addIfNotSeen(out, seen, toChunk(node, "method", qualifiedName, source, attributes));
    }

    private void processProperty(Node node,
                                 TreeSitterSource source,
                                 CSharpContext context,
                                 ArrayDeque<String> typeStack,
                                 Set<String> seen,
                                 List<TextChunk> out) {
        Optional<String> name = identifier(node, "name");
        if (name.isEmpty()) {
            return;
        }
        String qualifiedName = qualifyName(context.namespace(), typeStack, name.get());
        Map<String, String> attributes = buildPropertyAttributes(node, source, context, typeStack);
        addIfNotSeen(out, seen, toChunk(node, "property", qualifiedName, source, attributes));
    }

    private void processField(Node node,
                              TreeSitterSource source,
                              CSharpContext context,
                              ArrayDeque<String> typeStack,
                              Set<String> seen,
                              List<TextChunk> out) {
        // Fields are handled through variable_declaration nodes within field_declaration
        for (Node child : node.getNamedChildren()) {
            if ("variable_declaration".equals(child.getType())) {
                Optional<String> name = identifier(child, "name");
                if (name.isPresent()) {
                    String qualifiedName = qualifyName(context.namespace(), typeStack, name.get());
                    Map<String, String> attributes = buildFieldAttributes(child, source, context, typeStack);
                    addIfNotSeen(out, seen, toChunk(child, "field", qualifiedName, source, attributes));
                }
            }
        }
    }

    private Map<String, String> buildTypeAttributes(Node node, TreeSitterSource source, CSharpContext context) {
        Map<String, String> attributes = new LinkedHashMap<>();
        context.namespace().ifPresent(ns -> attributes.put(ATTR_NAMESPACE, ns));
        sliceField(node, FIELD_MODIFIERS, source).ifPresent(mods -> attributes.put(ATTR_MODIFIERS, mods));
        sliceField(node, "type_parameters", source).ifPresent(tp -> attributes.put("type_parameters", tp));
        sliceField(node, "base_list", source).ifPresent(base -> attributes.put("base_list", base));
        return attributes;
    }

    private Map<String, String> buildCallableAttributes(Node node,
                                                        TreeSitterSource source,
                                                        CSharpContext context,
                                                        ArrayDeque<String> typeStack) {
        Map<String, String> attributes = new LinkedHashMap<>();
        context.namespace().ifPresent(ns -> attributes.put(ATTR_NAMESPACE, ns));
        if (!typeStack.isEmpty()) {
            attributes.put(ATTR_ENCLOSING_TYPE, String.join(".", typeStack));
        }
        sliceField(node, FIELD_MODIFIERS, source).ifPresent(mods -> attributes.put(ATTR_MODIFIERS, mods));
        sliceField(node, "parameter_list", source).ifPresent(params -> attributes.put("parameters", params));
        sliceField(node, "return_type", source).ifPresent(ret -> attributes.put("return_type", ret));
        return attributes;
    }

    private Map<String, String> buildPropertyAttributes(Node node,
                                                        TreeSitterSource source,
                                                        CSharpContext context,
                                                        ArrayDeque<String> typeStack) {
        Map<String, String> attributes = new LinkedHashMap<>();
        context.namespace().ifPresent(ns -> attributes.put(ATTR_NAMESPACE, ns));
        if (!typeStack.isEmpty()) {
            attributes.put(ATTR_ENCLOSING_TYPE, String.join(".", typeStack));
        }
        sliceField(node, FIELD_MODIFIERS, source).ifPresent(mods -> attributes.put(ATTR_MODIFIERS, mods));
        sliceField(node, "type", source).ifPresent(type -> attributes.put("type", type));
        return attributes;
    }

    private Map<String, String> buildFieldAttributes(Node node,
                                                     TreeSitterSource source,
                                                     CSharpContext context,
                                                     ArrayDeque<String> typeStack) {
        Map<String, String> attributes = new LinkedHashMap<>();
        context.namespace().ifPresent(ns -> attributes.put(ATTR_NAMESPACE, ns));
        if (!typeStack.isEmpty()) {
            attributes.put(ATTR_ENCLOSING_TYPE, String.join(".", typeStack));
        }
        sliceField(node, "type", source).ifPresent(type -> attributes.put("type", type));
        return attributes;
    }

    private CSharpContext buildContext(Node rootNode, TreeSitterSource source) {
        String namespace = null;

        for (Node child : rootNode.getNamedChildren()) {
            if ("namespace_declaration".equals(child.getType())) {
                namespace = extractNamespace(child, source).orElse(namespace);
            }
        }
        return new CSharpContext(Optional.ofNullable(namespace));
    }

    private Optional<String> extractNamespace(Node namespaceNode, TreeSitterSource source) {
        return namespaceNode.getChildByFieldName("name")
                .map(nameNode -> source.slice(nameNode.getStartByte(), nameNode.getEndByte()).trim());
    }

    private Optional<String> identifier(Node node, String fieldName) {
        return node.getChildByFieldName(fieldName).map(Node::getText).map(String::trim);
    }

    private Optional<String> sliceField(Node node, String fieldName, TreeSitterSource source) {
        return node.getChildByFieldName(fieldName)
                .map(child -> source.slice(child.getStartByte(), child.getEndByte()).trim());
    }

    private String qualifyName(Optional<String> namespace, ArrayDeque<String> typeStack, String leaf) {
        List<String> parts = new ArrayList<>();
        namespace.ifPresent(parts::add);
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

    private boolean isMethodNode(Node node) {
        return "method_declaration".equals(node.getType());
    }

    private boolean isPropertyNode(Node node) {
        return "property_declaration".equals(node.getType());
    }

    private boolean isFieldNode(Node node) {
        return "field_declaration".equals(node.getType());
    }

    private String typeEntity(Node node) {
        return switch (node.getType()) {
            case "interface_declaration" -> "interface";
            case "struct_declaration" -> "struct";
            case "enum_declaration" -> "enum";
            case "record_declaration" -> "record";
            default -> "class";
        };
    }

    private static final io.megabrain.ingestion.parser.GrammarSpec CSHARP_SPEC =
            new io.megabrain.ingestion.parser.GrammarSpec(
                    LANGUAGE,
                    LANGUAGE_SYMBOL,
                    "tree-sitter-csharp",
                    LIBRARY_PROPERTY,
                    LIBRARY_ENV,
                    "tree-sitter-csharp",
                    "0.23.0"
            );

    private record CSharpContext(Optional<String> namespace) {
    }
}
