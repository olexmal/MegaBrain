/*
 * Copyright (c) 2025 MegaBrain Contributors
 * Licensed under the MIT License - see LICENSE file for details.
 */

package io.megabrain.ingestion.parser.treesitter;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

import io.github.treesitter.jtreesitter.Language;
import io.github.treesitter.jtreesitter.Node;
import io.github.treesitter.jtreesitter.Tree;
import io.megabrain.ingestion.parser.GrammarManager;
import io.megabrain.ingestion.parser.GrammarSpec;
import io.megabrain.ingestion.parser.TextChunk;

/**
 * Tree-sitter parser for Go source code.
 */
public class GoTreeSitterParser extends TreeSitterParser {

    private static final String LANGUAGE = "go";
    private static final Set<String> SUPPORTED_EXTENSIONS = Set.of("go");
    private static final String LIBRARY_ENV = "TREE_SITTER_GO_LIB";
    private static final String LIBRARY_PROPERTY = "tree.sitter.go.library";
    private static final String LANGUAGE_SYMBOL = "tree_sitter_go";

    private static final Set<String> TYPE_NODE_TYPES = Set.of(
            "type_declaration",
            "interface_type",
            "struct_type"
    );

    public GoTreeSitterParser() {
        this(new GrammarManager());
    }

    public GoTreeSitterParser(GrammarManager grammarManager) {
        this(grammarManager.languageSupplier(GO_SPEC), grammarManager.nativeLoader(GO_SPEC));
    }

    GoTreeSitterParser(Supplier<Language> languageSupplier, Runnable nativeLoader) {
        super(LANGUAGE, SUPPORTED_EXTENSIONS, languageSupplier, nativeLoader);
    }

    @Override
    protected List<QueryDefinition> languageQueries() {
        return List.of(
                new QueryDefinition("functions", """
                        (function_declaration
                            name: (identifier) @function.name
                            parameters: (parameter_list) @function.parameters
                            result: (_)? @function.result)?
                        """),
                new QueryDefinition("methods", """
                        (method_declaration
                            receiver: (parameter_declaration) @method.receiver
                            name: (identifier) @method.name
                            parameters: (parameter_list) @method.parameters
                            result: (_)? @method.result)?
                        """),
                new QueryDefinition("types", """
                        (type_declaration
                            name: (type_identifier) @type.name)?
                        """),
                new QueryDefinition("interfaces", """
                        (interface_type
                            (method_spec
                                name: (identifier) @interface.method.name
                                parameters: (parameter_list) @interface.method.parameters
                                result: (_)? @interface.method.result)?)?
                        """),
                new QueryDefinition("structs", """
                        (struct_type
                            (field_declaration
                                name: (field_identifier) @struct.field.name)?)?
                        """)
        );
    }

    @Override
    protected List<TextChunk> extractChunks(Node rootNode, Tree tree, TreeSitterSource source) {
        GoContext context = buildContext(rootNode, source);
        List<TextChunk> chunks = new ArrayList<>();
        walk(rootNode, source, context, new ArrayDeque<>(), new HashSet<>(), chunks);
        return chunks;
    }

    private void walk(Node node,
                      TreeSitterSource source,
                      GoContext context,
                      ArrayDeque<String> typeStack,
                      Set<String> seen,
                      List<TextChunk> out) {
        if (isTypeNode(node)) {
            processType(node, source, context, typeStack, seen, out);
            return;
        }
        if (isFunctionNode(node)) {
            processFunction(node, source, context, typeStack, seen, out);
        } else if (isMethodNode(node)) {
            processMethod(node, source, context, typeStack, seen, out);
        }
        node.getNamedChildren().forEach(child -> walk(child, source, context, typeStack, seen, out));
    }

    private void processType(Node node,
                             TreeSitterSource source,
                             GoContext context,
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
                                 GoContext context,
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

    private void processMethod(Node node,
                               TreeSitterSource source,
                               GoContext context,
                               ArrayDeque<String> typeStack,
                               Set<String> seen,
                               List<TextChunk> out) {
        Optional<String> name = identifier(node, "name");
        if (name.isEmpty()) {
            return;
        }
        Optional<String> receiver = extractReceiver(node, source);
        String qualifiedName = receiver.map(r -> r + "." + name.get()).orElse(name.get());
        qualifiedName = qualifyName(context.packageName(), typeStack, qualifiedName);
        Map<String, String> attributes = buildCallableAttributes(node, source, context, typeStack);
        receiver.ifPresent(r -> attributes.put("receiver", r));
        addIfNotSeen(out, seen, toChunk(node, "method", qualifiedName, source, attributes));
    }

    private Map<String, String> buildTypeAttributes(Node node, TreeSitterSource source, GoContext context) {
        Map<String, String> attributes = new LinkedHashMap<>();
        context.packageName().ifPresent(pkg -> attributes.put("package", pkg));
        if (!context.imports().isEmpty()) {
            attributes.put("imports", String.join(",", context.imports()));
        }
        return attributes;
    }

    private Map<String, String> buildCallableAttributes(Node node,
                                                        TreeSitterSource source,
                                                        GoContext context,
                                                        ArrayDeque<String> typeStack) {
        Map<String, String> attributes = new LinkedHashMap<>();
        context.packageName().ifPresent(pkg -> attributes.put("package", pkg));
        if (!context.imports().isEmpty()) {
            attributes.put("imports", String.join(",", context.imports()));
        }
        if (!typeStack.isEmpty()) {
            attributes.put("enclosing_type", String.join(".", typeStack));
        }
        sliceField(node, "parameters", source).ifPresent(params -> attributes.put("parameters", params));
        sliceField(node, "result", source).ifPresent(result -> attributes.put("return_type", result));
        return attributes;
    }

    private GoContext buildContext(Node rootNode, TreeSitterSource source) {
        String packageName = null;
        List<String> imports = new ArrayList<>();

        for (Node child : rootNode.getNamedChildren()) {
            switch (child.getType()) {
                case "package_clause" -> packageName = extractPackageName(child, source).orElse(packageName);
                case "import_declaration" -> imports.addAll(extractImports(child, source));
                default -> {
                }
            }
        }
        return new GoContext(Optional.ofNullable(packageName), List.copyOf(imports));
    }

    private Optional<String> extractPackageName(Node packageNode, TreeSitterSource source) {
        return packageNode.getChildByFieldName("name")
                .map(Node::getText)
                .map(String::trim);
    }

    private List<String> extractImports(Node importNode, TreeSitterSource source) {
        List<String> imports = new ArrayList<>();
        importNode.getNamedChildren().forEach(child -> {
            if ("import_spec".equals(child.getType())) {
                child.getChildByFieldName("path")
                        .map(pathNode -> source.slice(pathNode.getStartByte(), pathNode.getEndByte()).trim())
                        .ifPresent(imports::add);
            }
        });
        return imports;
    }

    private Optional<String> extractReceiver(Node methodNode, TreeSitterSource source) {
        return methodNode.getChildByFieldName("receiver")
                .flatMap(receiver -> receiver.getNamedChildren().stream().findFirst())
                .map(receiverType -> {
                    if ("pointer_type".equals(receiverType.getType()) || "slice_type".equals(receiverType.getType())) {
                        return receiverType.getNamedChildren().stream().findFirst()
                                .map(Node::getText).orElse("");
                    }
                    return receiverType.getText();
                })
                .map(String::trim);
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

    private boolean isMethodNode(Node node) {
        return "method_declaration".equals(node.getType());
    }

    private String typeEntity(Node node) {
        return switch (node.getType()) {
            case "interface_type" -> "interface";
            case "struct_type" -> "struct";
            default -> "type";
        };
    }

    private static final GrammarSpec GO_SPEC =
            new GrammarSpec(
                    LANGUAGE,
                    LANGUAGE_SYMBOL,
                    "tree-sitter-go",
                    LIBRARY_PROPERTY,
                    LIBRARY_ENV,
                    "tree-sitter-go",
                    "0.23.0"
            );

    private record GoContext(Optional<String> packageName, List<String> imports) {
    }
}
