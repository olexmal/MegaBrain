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
 * Tree-sitter parser for Kotlin source code.
 */
public class KotlinTreeSitterParser extends TreeSitterParser {

    private static final Logger LOG = Logger.getLogger(KotlinTreeSitterParser.class);
    private static final String LANGUAGE = "kotlin";
    private static final Set<String> SUPPORTED_EXTENSIONS = Set.of("kt", "kts");
    private static final String LIBRARY_ENV = "TREE_SITTER_KOTLIN_LIB";
    private static final String LIBRARY_PROPERTY = "tree.sitter.kotlin.library";
    private static final String LANGUAGE_SYMBOL = "tree_sitter_kotlin";

    // Attribute keys
    private static final String ATTR_PACKAGE = "package";
    private static final String ATTR_IMPORTS = "imports";
    private static final String ATTR_MODIFIERS = "modifiers";

    private static final Set<String> TYPE_NODE_TYPES = Set.of(
            "class_declaration",
            "interface_declaration",
            "enum_class_declaration",
            "object_declaration",
            "data_class_declaration",
            "sealed_class_declaration",
            "annotation_declaration"
    );

    public KotlinTreeSitterParser() {
        this(new io.megabrain.ingestion.parser.GrammarManager());
    }

    public KotlinTreeSitterParser(io.megabrain.ingestion.parser.GrammarManager grammarManager) {
        this(grammarManager.languageSupplier(KOTLIN_SPEC), grammarManager.nativeLoader(KOTLIN_SPEC));
    }

    KotlinTreeSitterParser(java.util.function.Supplier<io.github.treesitter.jtreesitter.Language> languageSupplier, Runnable nativeLoader) {
        super(LANGUAGE, SUPPORTED_EXTENSIONS, languageSupplier, nativeLoader);
    }

    @Override
    protected List<QueryDefinition> languageQueries() {
        return List.of(
                new QueryDefinition("classes", """
                        (class_declaration
                            name: (simple_identifier) @class.name)?
                        """),
                new QueryDefinition("interfaces", """
                        (interface_declaration
                            name: (simple_identifier) @interface.name)?
                        """),
                new QueryDefinition("enums", """
                        (enum_class_declaration
                            name: (simple_identifier) @enum.name)?
                        """),
                new QueryDefinition("objects", """
                        (object_declaration
                            name: (simple_identifier) @object.name)?
                        """),
                new QueryDefinition("data_classes", """
                        (data_class_declaration
                            name: (simple_identifier) @data_class.name)?
                        """),
                new QueryDefinition("functions", """
                        (function_declaration
                            name: (simple_identifier) @function.name
                            parameters: (parameter_list)? @function.parameters
                            type: (_)? @function.return_type)?
                        """),
                new QueryDefinition("properties", """
                        (property_declaration
                            name: (simple_identifier) @property.name
                            type: (_)? @property.type)?
                        """)
        );
    }

    @Override
    protected List<TextChunk> extractChunks(Node rootNode, Tree tree, TreeSitterSource source) {
        KotlinContext context = buildContext(rootNode, source);
        List<TextChunk> chunks = new ArrayList<>();
        walk(rootNode, source, context, new ArrayDeque<>(), new HashSet<>(), chunks);
        return chunks;
    }

    private void walk(Node node,
                      TreeSitterSource source,
                      KotlinContext context,
                      ArrayDeque<String> typeStack,
                      Set<String> seen,
                      List<TextChunk> out) {
        if (isTypeNode(node)) {
            processType(node, source, context, typeStack, seen, out);
            return;
        }
        if (isFunctionNode(node)) {
            processFunction(node, source, context, typeStack, seen, out);
        } else if (isPropertyNode(node)) {
            processProperty(node, source, context, typeStack, seen, out);
        }
        node.getNamedChildren().forEach(child -> walk(child, source, context, typeStack, seen, out));
    }

    private void processType(Node node,
                             TreeSitterSource source,
                             KotlinContext context,
                             ArrayDeque<String> typeStack,
                             Set<String> seen,
                             List<TextChunk> out) {
        Optional<String> name = identifier(node, "name");
        if (name.isEmpty()) {
            return;
        }
        String entityType = typeEntity(node);
        String qualifiedName = qualifyName(context.packageName(), typeStack, name.get());
        Map<String, String> attributes = buildTypeAttributes(node, source, context);
        addIfNotSeen(out, seen, toChunk(node, entityType, qualifiedName, source, attributes));

        typeStack.push(name.get());
        node.getNamedChildren().forEach(child -> walk(child, source, context, typeStack, seen, out));
        typeStack.pop();
    }

    private void processFunction(Node node,
                                 TreeSitterSource source,
                                 KotlinContext context,
                                 ArrayDeque<String> typeStack,
                                 Set<String> seen,
                                 List<TextChunk> out) {
        Optional<String> name = identifier(node, "name");
        if (name.isEmpty()) {
            return;
        }
        String qualifiedName = qualifyName(context.packageName(), typeStack, name.get());
        Map<String, String> attributes = buildCallableAttributes(node, source, context, typeStack);
        addIfNotSeen(out, seen, toChunk(node, "function", qualifiedName, source, attributes));
    }

    private void processProperty(Node node,
                                 TreeSitterSource source,
                                 KotlinContext context,
                                 ArrayDeque<String> typeStack,
                                 Set<String> seen,
                                 List<TextChunk> out) {
        Optional<String> name = identifier(node, "name");
        if (name.isEmpty()) {
            return;
        }
        String qualifiedName = qualifyName(context.packageName(), typeStack, name.get());
        Map<String, String> attributes = buildPropertyAttributes(node, source, context, typeStack);
        addIfNotSeen(out, seen, toChunk(node, "property", qualifiedName, source, attributes));
    }

    private Map<String, String> buildTypeAttributes(Node node, TreeSitterSource source, KotlinContext context) {
        Map<String, String> attributes = new LinkedHashMap<>();
        context.packageName().ifPresent(pkg -> attributes.put(ATTR_PACKAGE, pkg));
        if (!context.imports().isEmpty()) {
            attributes.put(ATTR_IMPORTS, String.join(",", context.imports()));
        }
        sliceField(node, "modifiers", source).ifPresent(mods -> attributes.put(ATTR_MODIFIERS, mods));
        return attributes;
    }

    private Map<String, String> buildCallableAttributes(Node node,
                                                        TreeSitterSource source,
                                                        KotlinContext context,
                                                        ArrayDeque<String> typeStack) {
        Map<String, String> attributes = new LinkedHashMap<>();
        context.packageName().ifPresent(pkg -> attributes.put(ATTR_PACKAGE, pkg));
        if (!context.imports().isEmpty()) {
            attributes.put(ATTR_IMPORTS, String.join(",", context.imports()));
        }
        if (!typeStack.isEmpty()) {
            attributes.put("enclosing_type", String.join(".", typeStack));
        }
        sliceField(node, "modifiers", source).ifPresent(mods -> attributes.put(ATTR_MODIFIERS, mods));
        sliceField(node, "parameters", source).ifPresent(params -> attributes.put("parameters", params));
        sliceField(node, "return_type", source).ifPresent(ret -> attributes.put("return_type", ret));
        return attributes;
    }

    private Map<String, String> buildPropertyAttributes(Node node,
                                                        TreeSitterSource source,
                                                        KotlinContext context,
                                                        ArrayDeque<String> typeStack) {
        Map<String, String> attributes = new LinkedHashMap<>();
        context.packageName().ifPresent(pkg -> attributes.put(ATTR_PACKAGE, pkg));
        if (!context.imports().isEmpty()) {
            attributes.put(ATTR_IMPORTS, String.join(",", context.imports()));
        }
        if (!typeStack.isEmpty()) {
            attributes.put("enclosing_type", String.join(".", typeStack));
        }
        sliceField(node, "modifiers", source).ifPresent(mods -> attributes.put(ATTR_MODIFIERS, mods));
        sliceField(node, "type", source).ifPresent(type -> attributes.put("type", type));
        return attributes;
    }

    private KotlinContext buildContext(Node rootNode, TreeSitterSource source) {
        String packageName = null;
        List<String> imports = new ArrayList<>();

        for (Node child : rootNode.getNamedChildren()) {
            switch (child.getType()) {
                case "package_header" -> packageName = extractPackageName(child, source).orElse(packageName);
                case "import_list" -> imports.addAll(extractImports(child, source));
                default -> {
                }
            }
        }
        return new KotlinContext(Optional.ofNullable(packageName), List.copyOf(imports));
    }

    private Optional<String> extractPackageName(Node packageNode, TreeSitterSource source) {
        return packageNode.getChildByFieldName("identifier")
                .map(Node::getText)
                .map(String::trim);
    }

    private List<String> extractImports(Node importListNode, TreeSitterSource source) {
        List<String> imports = new ArrayList<>();
        importListNode.getNamedChildren().forEach(child -> {
            if ("import_header".equals(child.getType())) {
                child.getNamedChildren().stream()
                        .findFirst()
                        .map(node -> source.slice(node.getStartByte(), node.getEndByte()).trim())
                        .ifPresent(imports::add);
            }
        });
        return imports;
    }

    private Optional<String> identifier(Node node, String fieldName) {
        return node.getChildByFieldName(fieldName).map(Node::getText).map(String::trim);
    }

    private Optional<String> sliceField(Node node, String fieldName, TreeSitterSource source) {
        return node.getChildByFieldName(fieldName)
                .map(child -> source.slice(child.getStartByte(), child.getEndByte()).trim());
    }

    private String qualifyName(Optional<String> packageName, ArrayDeque<String> typeStack, String leaf) {
        List<String> parts = new ArrayList<>();
        packageName.ifPresent(parts::add);
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

    private boolean isPropertyNode(Node node) {
        return "property_declaration".equals(node.getType());
    }

    private String typeEntity(Node node) {
        return switch (node.getType()) {
            case "interface_declaration" -> "interface";
            case "enum_class_declaration" -> "enum";
            case "object_declaration" -> "object";
            case "data_class_declaration" -> "data_class";
            case "sealed_class_declaration" -> "sealed_class";
            case "annotation_declaration" -> "annotation";
            default -> "class";
        };
    }

    private static final io.megabrain.ingestion.parser.GrammarSpec KOTLIN_SPEC =
            new io.megabrain.ingestion.parser.GrammarSpec(
                    LANGUAGE,
                    LANGUAGE_SYMBOL,
                    "tree-sitter-kotlin",
                    LIBRARY_PROPERTY,
                    LIBRARY_ENV,
                    "tree-sitter-kotlin",
                    "0.3.0"
            );

    private record KotlinContext(Optional<String> packageName, List<String> imports) {
    }
}
