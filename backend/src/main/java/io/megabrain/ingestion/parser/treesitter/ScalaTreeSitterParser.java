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
 * Tree-sitter parser for Scala source code.
 */
public class ScalaTreeSitterParser extends TreeSitterParser {

    private static final Logger LOG = Logger.getLogger(ScalaTreeSitterParser.class);
    private static final String LANGUAGE = "scala";
    private static final Set<String> SUPPORTED_EXTENSIONS = Set.of("scala", "sc");
    private static final String LIBRARY_ENV = "TREE_SITTER_SCALA_LIB";
    private static final String LIBRARY_PROPERTY = "tree.sitter.scala.library";
    private static final String LANGUAGE_SYMBOL = "tree_sitter_scala";

    private static final Set<String> TYPE_NODE_TYPES = Set.of(
            "class_definition",
            "trait_definition",
            "object_definition",
            "case_class_definition"
    );

    public ScalaTreeSitterParser() {
        this(new io.megabrain.ingestion.parser.GrammarManager());
    }

    public ScalaTreeSitterParser(io.megabrain.ingestion.parser.GrammarManager grammarManager) {
        this(grammarManager.languageSupplier(SCALA_SPEC), grammarManager.nativeLoader(SCALA_SPEC));
    }

    ScalaTreeSitterParser(java.util.function.Supplier<io.github.treesitter.jtreesitter.Language> languageSupplier, Runnable nativeLoader) {
        super(LANGUAGE, SUPPORTED_EXTENSIONS, languageSupplier, nativeLoader);
    }

    @Override
    protected List<QueryDefinition> languageQueries() {
        return List.of(
                new QueryDefinition("classes", """
                        (class_definition
                            name: (identifier) @class.name)?
                        """),
                new QueryDefinition("traits", """
                        (trait_definition
                            name: (identifier) @trait.name)?
                        """),
                new QueryDefinition("objects", """
                        (object_definition
                            name: (identifier) @object.name)?
                        """),
                new QueryDefinition("case_classes", """
                        (case_class_definition
                            name: (identifier) @case_class.name)?
                        """),
                new QueryDefinition("functions", """
                        (function_definition
                            name: (identifier) @function.name
                            parameter_list: (parameter_list)? @function.parameters
                            return_type: (_)? @function.return_type)?
                        """),
                new QueryDefinition("methods", """
                        (function_definition
                            name: (identifier) @method.name
                            parameter_list: (parameter_list)? @method.parameters
                            return_type: (_)? @method.return_type)?
                        """)
        );
    }

    @Override
    protected List<TextChunk> extractChunks(Node rootNode, Tree tree, TreeSitterSource source) {
        ScalaContext context = buildContext(rootNode, source);
        List<TextChunk> chunks = new ArrayList<>();
        walk(rootNode, source, context, new ArrayDeque<>(), new HashSet<>(), chunks);
        return chunks;
    }

    private void walk(Node node,
                      TreeSitterSource source,
                      ScalaContext context,
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
                             ScalaContext context,
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
                                 ScalaContext context,
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

    private Map<String, String> buildTypeAttributes(Node node, TreeSitterSource source, ScalaContext context) {
        Map<String, String> attributes = new LinkedHashMap<>();
        context.packageName().ifPresent(pkg -> attributes.put("package", pkg));
        sliceField(node, "type_parameters", source).ifPresent(tp -> attributes.put("type_parameters", tp));
        sliceField(node, "extends_clause", source).ifPresent(extendsClause -> attributes.put("extends", extendsClause));
        return attributes;
    }

    private Map<String, String> buildCallableAttributes(Node node,
                                                        TreeSitterSource source,
                                                        ScalaContext context,
                                                        ArrayDeque<String> typeStack) {
        Map<String, String> attributes = new LinkedHashMap<>();
        context.packageName().ifPresent(pkg -> attributes.put("package", pkg));
        if (!typeStack.isEmpty()) {
            attributes.put("enclosing_type", String.join(".", typeStack));
        }
        sliceField(node, "modifiers", source).ifPresent(mods -> attributes.put("modifiers", mods));
        sliceField(node, "parameter_list", source).ifPresent(params -> attributes.put("parameters", params));
        sliceField(node, "return_type", source).ifPresent(ret -> attributes.put("return_type", ret));
        return attributes;
    }

    private ScalaContext buildContext(Node rootNode, TreeSitterSource source) {
        String packageName = null;

        for (Node child : rootNode.getNamedChildren()) {
            if ("package_clause".equals(child.getType())) {
                packageName = extractPackageName(child, source).orElse(packageName);
            }
        }
        return new ScalaContext(Optional.ofNullable(packageName));
    }

    private Optional<String> extractPackageName(Node packageNode, TreeSitterSource source) {
        return packageNode.getNamedChildren().stream()
                .findFirst()
                .map(node -> source.slice(node.getStartByte(), node.getEndByte()).trim());
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
        return "function_definition".equals(node.getType());
    }

    private String typeEntity(Node node) {
        return switch (node.getType()) {
            case "trait_definition" -> "trait";
            case "object_definition" -> "object";
            case "case_class_definition" -> "case_class";
            default -> "class";
        };
    }

    private static final io.megabrain.ingestion.parser.GrammarSpec SCALA_SPEC =
            new io.megabrain.ingestion.parser.GrammarSpec(
                    LANGUAGE,
                    LANGUAGE_SYMBOL,
                    "tree-sitter-scala",
                    LIBRARY_PROPERTY,
                    LIBRARY_ENV,
                    "tree-sitter-scala",
                    "0.23.0"
            );

    private record ScalaContext(Optional<String> packageName) {
    }
}
