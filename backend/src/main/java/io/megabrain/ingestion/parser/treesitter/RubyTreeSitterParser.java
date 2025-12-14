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
 * Tree-sitter parser for Ruby source code.
 */
public class RubyTreeSitterParser extends TreeSitterParser {

    private static final Logger LOG = Logger.getLogger(RubyTreeSitterParser.class);
    private static final String LANGUAGE = "ruby";
    private static final Set<String> SUPPORTED_EXTENSIONS = Set.of("rb");
    private static final String LIBRARY_ENV = "TREE_SITTER_RUBY_LIB";
    private static final String LIBRARY_PROPERTY = "tree.sitter.ruby.library";
    private static final String LANGUAGE_SYMBOL = "tree_sitter_ruby";

    // Node types
    private static final String NODE_MODULE = "module";
    private static final String NODE_SINGLETON_CLASS = "singleton_class";

    private static final Set<String> TYPE_NODE_TYPES = Set.of(
            "class",
            NODE_MODULE,
            NODE_SINGLETON_CLASS
    );

    public RubyTreeSitterParser() {
        this(new GrammarManager());
    }

    public RubyTreeSitterParser(GrammarManager grammarManager) {
        this(grammarManager.languageSupplier(RUBY_SPEC), grammarManager.nativeLoader(RUBY_SPEC));
    }

    RubyTreeSitterParser(Supplier<Language> languageSupplier, Runnable nativeLoader) {
        super(LANGUAGE, SUPPORTED_EXTENSIONS, languageSupplier, nativeLoader);
    }

    @Override
    protected List<QueryDefinition> languageQueries() {
        return List.of(
                new QueryDefinition("classes", """
                        (class
                            name: (_) @class.name)?
                        """),
                new QueryDefinition("modules", """
                        (module
                            name: (_) @module.name)?
                        """),
                new QueryDefinition("methods", """
                        (method
                            name: (_) @method.name
                            parameters: (method_parameters)? @method.parameters)?
                        """),
                new QueryDefinition("singleton_methods", """
                        (singleton_method
                            object: (_) @singleton.object
                            name: (_) @singleton.name
                            parameters: (method_parameters)? @singleton.parameters)?
                        """),
                new QueryDefinition("functions", """
                        (function
                            name: (_) @function.name
                            parameters: (method_parameters)? @function.parameters)?
                        """),
                new QueryDefinition("constants", """
                        (constant
                            name: (_) @constant.name)?
                        """)
        );
    }

    @Override
    protected List<TextChunk> extractChunks(Node rootNode, Tree tree, TreeSitterSource source) {
        RubyContext context = new RubyContext();
        List<TextChunk> chunks = new ArrayList<>();
        walk(rootNode, source, context, new ArrayDeque<>(), new HashSet<>(), chunks);
        return chunks;
    }

    private void walk(Node node,
                      TreeSitterSource source,
                      RubyContext context,
                      ArrayDeque<String> typeStack,
                      Set<String> seen,
                      List<TextChunk> out) {
        if (isTypeNode(node)) {
            processType(node, source, context, typeStack, seen, out);
            return;
        }
        if (isMethodNode(node)) {
            processMethod(node, source, context, typeStack, seen, out);
        } else if (isSingletonMethodNode(node)) {
            processSingletonMethod(node, source, context, typeStack, seen, out);
        } else if (isFunctionNode(node)) {
            processFunction(node, source, context, typeStack, seen, out);
        } else if (isConstantNode(node)) {
            processConstant(node, source, context, typeStack, seen, out);
        }
        node.getNamedChildren().forEach(child -> walk(child, source, context, typeStack, seen, out));
    }

    private void processType(Node node,
                             TreeSitterSource source,
                             RubyContext context,
                             ArrayDeque<String> typeStack,
                             Set<String> seen,
                             List<TextChunk> out) {
        Optional<String> name = extractName(node, source);
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

    private void processMethod(Node node,
                               TreeSitterSource source,
                               RubyContext context,
                               ArrayDeque<String> typeStack,
                               Set<String> seen,
                               List<TextChunk> out) {
        Optional<String> name = extractName(node, source);
        if (name.isEmpty()) {
            return;
        }
        String qualifiedName = qualifyName(typeStack, name.get());
        Map<String, String> attributes = buildCallableAttributes(node, source, context, typeStack);
        addIfNotSeen(out, seen, toChunk(node, "method", qualifiedName, source, attributes));
    }

    private void processSingletonMethod(Node node,
                                        TreeSitterSource source,
                                        RubyContext context,
                                        ArrayDeque<String> typeStack,
                                        Set<String> seen,
                                        List<TextChunk> out) {
        Optional<String> name = extractName(node, source);
        Optional<String> object = extractObject(node, source);
        if (name.isEmpty()) {
            return;
        }
        String qualifiedName = object.map(obj -> obj + "." + name.get()).orElse(name.get());
        qualifiedName = qualifyName(typeStack, qualifiedName);
        Map<String, String> attributes = buildCallableAttributes(node, source, context, typeStack);
        object.ifPresent(obj -> attributes.put("object", obj));
        addIfNotSeen(out, seen, toChunk(node, "singleton_method", qualifiedName, source, attributes));
    }

    private void processFunction(Node node,
                                 TreeSitterSource source,
                                 RubyContext context,
                                 ArrayDeque<String> typeStack,
                                 Set<String> seen,
                                 List<TextChunk> out) {
        Optional<String> name = extractName(node, source);
        if (name.isEmpty()) {
            return;
        }
        String qualifiedName = qualifyName(typeStack, name.get());
        Map<String, String> attributes = buildCallableAttributes(node, source, context, typeStack);
        addIfNotSeen(out, seen, toChunk(node, "function", qualifiedName, source, attributes));
    }

    private void processConstant(Node node,
                                 TreeSitterSource source,
                                 RubyContext context,
                                 ArrayDeque<String> typeStack,
                                 Set<String> seen,
                                 List<TextChunk> out) {
        Optional<String> name = extractName(node, source);
        if (name.isEmpty()) {
            return;
        }
        String qualifiedName = qualifyName(typeStack, name.get());
        Map<String, String> attributes = buildConstantAttributes(node, source, context, typeStack);
        addIfNotSeen(out, seen, toChunk(node, "constant", qualifiedName, source, attributes));
    }

    private Map<String, String> buildTypeAttributes(Node node, TreeSitterSource source, RubyContext context) {
        Map<String, String> attributes = new LinkedHashMap<>();
        sliceField(node, "superclass", source).ifPresent(superclass -> attributes.put("superclass", superclass));
        return attributes;
    }

    private Map<String, String> buildCallableAttributes(Node node,
                                                        TreeSitterSource source,
                                                        RubyContext context,
                                                        ArrayDeque<String> typeStack) {
        Map<String, String> attributes = new LinkedHashMap<>();
        if (!typeStack.isEmpty()) {
            attributes.put("enclosing_type", String.join("::", typeStack));
        }
        sliceField(node, "parameters", source).ifPresent(params -> attributes.put("parameters", params));
        return attributes;
    }

    private Map<String, String> buildConstantAttributes(Node node,
                                                        TreeSitterSource source,
                                                        RubyContext context,
                                                        ArrayDeque<String> typeStack) {
        Map<String, String> attributes = new LinkedHashMap<>();
        if (!typeStack.isEmpty()) {
            attributes.put("enclosing_type", String.join("::", typeStack));
        }
        return attributes;
    }

    private Optional<String> extractName(Node node, TreeSitterSource source) {
        return node.getChildByFieldName("name")
                .map(nameNode -> source.slice(nameNode.getStartByte(), nameNode.getEndByte()).trim());
    }

    private Optional<String> extractObject(Node node, TreeSitterSource source) {
        return node.getChildByFieldName("object")
                .map(objectNode -> source.slice(objectNode.getStartByte(), objectNode.getEndByte()).trim());
    }

    private Optional<String> sliceField(Node node, String fieldName, TreeSitterSource source) {
        return node.getChildByFieldName(fieldName)
                .map(child -> source.slice(child.getStartByte(), child.getEndByte()).trim());
    }

    private String qualifyName(ArrayDeque<String> typeStack, String leaf) {
        List<String> parts = new ArrayList<>(typeStack);
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

    private boolean isMethodNode(Node node) {
        return "method".equals(node.getType());
    }

    private boolean isSingletonMethodNode(Node node) {
        return "singleton_method".equals(node.getType());
    }

    private boolean isFunctionNode(Node node) {
        return "function".equals(node.getType());
    }

    private boolean isConstantNode(Node node) {
        return "constant".equals(node.getType());
    }

    private String typeEntity(Node node) {
        return switch (node.getType()) {
            case NODE_MODULE -> NODE_MODULE;
            case NODE_SINGLETON_CLASS -> NODE_SINGLETON_CLASS;
            default -> "class";
        };
    }

    private static final GrammarSpec RUBY_SPEC =
            new GrammarSpec(
                    LANGUAGE,
                    LANGUAGE_SYMBOL,
                    "tree-sitter-ruby",
                    LIBRARY_PROPERTY,
                    LIBRARY_ENV,
                    "tree-sitter-ruby",
                    "0.23.0"
            );

    private record RubyContext() {
    }
}
