/*
 * Copyright (c) 2025 MegaBrain Contributors
 * Licensed under the MIT License - see LICENSE file for details.
 */

package io.megabrain.ingestion.parser.treesitter;

import io.github.treesitter.jtreesitter.Language;
import io.github.treesitter.jtreesitter.Node;
import io.github.treesitter.jtreesitter.Tree;
import io.megabrain.ingestion.parser.GrammarManager;
import io.megabrain.ingestion.parser.GrammarSpec;
import io.megabrain.ingestion.parser.TextChunk;
import org.jboss.logging.Logger;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Tree-sitter parser for Java source code.
 */
public class JavaTreeSitterParser extends TreeSitterParser {

    private static final Logger LOG = Logger.getLogger(JavaTreeSitterParser.class);
    private static final String LANGUAGE = "java";
    private static final Set<String> SUPPORTED_EXTENSIONS = Set.of("java");
    private static final String LIBRARY_ENV = "TREE_SITTER_JAVA_LIB";
    private static final String LIBRARY_PROPERTY = "tree.sitter.java.library";
    private static final String LANGUAGE_SYMBOL = "tree_sitter_java";

    private static final Set<String> TYPE_NODE_TYPES = Set.of(
            "class_declaration",
            "interface_declaration",
            "enum_declaration",
            "record_declaration",
            "annotation_type_declaration"
    );

    public JavaTreeSitterParser() {
        this(new GrammarManager());
    }

    public JavaTreeSitterParser(GrammarManager grammarManager) {
        this(grammarManager.languageSupplier(JAVA_SPEC), grammarManager.nativeLoader(JAVA_SPEC));
    }

    JavaTreeSitterParser(Supplier<Language> languageSupplier, Runnable nativeLoader) {
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
                new QueryDefinition("enums", """
                        (enum_declaration
                            name: (identifier) @enum.name)?
                        """),
                new QueryDefinition("records", """
                        (record_declaration
                            name: (identifier) @record.name)?
                        """),
                new QueryDefinition("constructors", """
                        (constructor_declaration
                            name: (identifier) @constructor.name
                            parameters: (formal_parameters) @constructor.parameters)?
                        """),
                new QueryDefinition("methods", """
                        (method_declaration
                            name: (identifier) @method.name
                            parameters: (formal_parameters) @method.parameters
                            type: (_)? @method.return)?
                        """)
        );
    }

    @Override
    protected List<TextChunk> extractChunks(Node rootNode, Tree tree, TreeSitterSource source) {
        JavaContext context = buildContext(rootNode, source);
        List<TextChunk> chunks = new ArrayList<>();
        walk(rootNode, source, context, new ArrayDeque<>(), new HashSet<>(), chunks);
        return chunks;
    }

    private void walk(Node node,
                      TreeSitterSource source,
                      JavaContext context,
                      ArrayDeque<String> typeStack,
                      Set<String> seen,
                      List<TextChunk> out) {
        if (isTypeNode(node)) {
            processType(node, source, context, typeStack, seen, out);
            return;
        }
        if (isConstructorNode(node)) {
            processConstructor(node, source, context, typeStack, seen, out);
        } else if (isMethodNode(node)) {
            processMethod(node, source, context, typeStack, seen, out);
        }
        node.getNamedChildren().forEach(child -> walk(child, source, context, typeStack, seen, out));
    }

    private void processType(Node node,
                             TreeSitterSource source,
                             JavaContext context,
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

    private void processMethod(Node node,
                               TreeSitterSource source,
                               JavaContext context,
                               ArrayDeque<String> typeStack,
                               Set<String> seen,
                               List<TextChunk> out) {
        Optional<String> name = identifier(node, "name");
        if (name.isEmpty()) {
            return;
        }
        String qualifiedName = qualifyName(context.packageName(), typeStack, name.get());
        Map<String, String> attributes = buildCallableAttributes(node, source, context, typeStack, false);
        addIfNotSeen(out, seen, toChunk(node, "method", qualifiedName, source, attributes));
    }

    private void processConstructor(Node node,
                                    TreeSitterSource source,
                                    JavaContext context,
                                    ArrayDeque<String> typeStack,
                                    Set<String> seen,
                                    List<TextChunk> out) {
        Optional<String> name = identifier(node, "name");
        if (name.isEmpty()) {
            return;
        }
        String qualifiedName = qualifyName(context.packageName(), typeStack, name.get());
        Map<String, String> attributes = buildCallableAttributes(node, source, context, typeStack, true);
        addIfNotSeen(out, seen, toChunk(node, "constructor", qualifiedName, source, attributes));
    }

    private Map<String, String> buildTypeAttributes(Node node, TreeSitterSource source, JavaContext context) {
        Map<String, String> attributes = new LinkedHashMap<>();
        context.packageName().ifPresent(pkg -> attributes.put("package", pkg));
        if (!context.imports().isEmpty()) {
            attributes.put("imports", String.join(",", context.imports()));
        }
        modifiers(node, source).ifPresent(mods -> attributes.put("modifiers", mods));
        List<String> annotations = collectAnnotations(node, source);
        if (!annotations.isEmpty()) {
            attributes.put("annotations", String.join(",", annotations));
        }
        sliceField(node, "type_parameters", source).ifPresent(tp -> attributes.put("type_parameters", tp));
        sliceField(node, "superclass", source).ifPresent(superClass -> attributes.put("superclass", superClass));
        sliceField(node, "interfaces", source).ifPresent(interfaces -> attributes.put("interfaces", interfaces));
        return attributes;
    }

    private Map<String, String> buildCallableAttributes(Node node,
                                                        TreeSitterSource source,
                                                        JavaContext context,
                                                        ArrayDeque<String> typeStack,
                                                        boolean isConstructor) {
        Map<String, String> attributes = new LinkedHashMap<>();
        context.packageName().ifPresent(pkg -> attributes.put("package", pkg));
        if (!context.imports().isEmpty()) {
            attributes.put("imports", String.join(",", context.imports()));
        }
        if (!typeStack.isEmpty()) {
            attributes.put("enclosing_type", String.join(".", typeStack));
        }
        modifiers(node, source).ifPresent(mods -> attributes.put("modifiers", mods));
        List<String> annotations = collectAnnotations(node, source);
        if (!annotations.isEmpty()) {
            attributes.put("annotations", String.join(",", annotations));
        }
        sliceField(node, "type_parameters", source).ifPresent(tp -> attributes.put("type_parameters", tp));
        sliceField(node, "parameters", source).ifPresent(params -> attributes.put("parameters", params));
        if (!isConstructor) {
            sliceField(node, "type", source).ifPresent(ret -> attributes.put("return_type", ret));
        } else {
            attributes.put("return_type", "");
        }
        sliceField(node, "throws", source).ifPresent(throwsClause -> attributes.put("throws", throwsClause));
        return attributes;
    }

    private JavaContext buildContext(Node rootNode, TreeSitterSource source) {
        String packageName = null;
        List<String> imports = new ArrayList<>();

        for (Node child : rootNode.getNamedChildren()) {
            switch (child.getType()) {
                case "package_declaration" -> packageName = extractPackageName(child, source).orElse(packageName);
                case "import_declaration" -> imports.add(source.slice(child.getStartByte(), child.getEndByte()).trim());
                default -> {
                }
            }
        }
        return new JavaContext(Optional.ofNullable(packageName), List.copyOf(imports));
    }

    private Optional<String> extractPackageName(Node packageNode, TreeSitterSource source) {
        Optional<Node> nameField = packageNode.getChildByFieldName("name");
        if (nameField.isPresent()) {
            return nameField.map(Node::getText).map(String::trim);
        }
        return packageNode.getNamedChildren().stream().findFirst()
                .map(node -> source.slice(node.getStartByte(), node.getEndByte()).trim());
    }

    private Optional<String> modifiers(Node node, TreeSitterSource source) {
        return node.getChildByFieldName("modifiers")
                .map(modNode -> source.slice(modNode.getStartByte(), modNode.getEndByte()).trim());
    }

    private List<String> collectAnnotations(Node node, TreeSitterSource source) {
        List<String> annotations = new ArrayList<>();
        node.getChildByFieldName("modifiers")
                .ifPresent(modifiers -> modifiers.getChildren().forEach(child -> {
                    if (child.getType().contains("annotation")) {
                        annotations.add(source.slice(child.getStartByte(), child.getEndByte()).trim());
                    }
                }));
        return annotations;
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

    private boolean isMethodNode(Node node) {
        return "method_declaration".equals(node.getType());
    }

    private boolean isConstructorNode(Node node) {
        return "constructor_declaration".equals(node.getType())
                || "compact_constructor_declaration".equals(node.getType());
    }

    private String typeEntity(Node node) {
        return switch (node.getType()) {
            case "interface_declaration" -> "interface";
            case "enum_declaration" -> "enum";
            case "record_declaration" -> "record";
            case "annotation_type_declaration" -> "annotation";
            default -> "class";
        };
    }

    private static final GrammarSpec JAVA_SPEC =
            new GrammarSpec(
                    LANGUAGE,
                    LANGUAGE_SYMBOL,
                    "tree-sitter-java",
                    LIBRARY_PROPERTY,
                    LIBRARY_ENV,
                    "tree-sitter-java",
                    "0.21.0"
            );

    private record JavaContext(Optional<String> packageName, List<String> imports) {
    }
}

