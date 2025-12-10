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
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Tree-sitter parser for Python source code.
 */
public class PythonTreeSitterParser extends TreeSitterParser {

    private static final Logger LOG = Logger.getLogger(PythonTreeSitterParser.class);
    private static final String LANGUAGE = "python";
    private static final Set<String> SUPPORTED_EXTENSIONS = Set.of("py", "pyw", "pyi");
    private static final String LIBRARY_ENV = "TREE_SITTER_PYTHON_LIB";
    private static final String LIBRARY_PROPERTY = "tree.sitter.python.library";
    private static final String LANGUAGE_SYMBOL = "tree_sitter_python";

    public PythonTreeSitterParser() {
        this(new io.megabrain.ingestion.parser.GrammarManager());
    }

    public PythonTreeSitterParser(io.megabrain.ingestion.parser.GrammarManager grammarManager) {
        this(grammarManager.languageSupplier(PYTHON_SPEC), grammarManager.nativeLoader(PYTHON_SPEC));
    }

    PythonTreeSitterParser(Supplier<Language> languageSupplier, Runnable nativeLoader) {
        super(LANGUAGE, SUPPORTED_EXTENSIONS, languageSupplier, nativeLoader);
    }

    @Override
    protected List<QueryDefinition> languageQueries() {
        return List.of(
                new QueryDefinition("functions", """
                        (function_definition
                            name: (identifier) @function.name
                            parameters: (parameters) @function.parameters)?
                        """),
                new QueryDefinition("async_functions", """
                        (async_function_definition
                            name: (identifier) @function.name
                            parameters: (parameters) @function.parameters)?
                        """),
                new QueryDefinition("classes", """
                        (class_definition
                            name: (identifier) @class.name)?
                        """)
        );
    }

    @Override
    protected List<TextChunk> extractChunks(Node rootNode, Tree tree, TreeSitterSource source) {
        List<TextChunk> chunks = new ArrayList<>();
        ArrayDeque<String> classStack = new ArrayDeque<>();
        Set<String> seen = new HashSet<>();
        walk(rootNode, source, classStack, chunks, seen);
        return chunks;
    }

    private void walk(Node node, TreeSitterSource source, ArrayDeque<String> classStack, List<TextChunk> out, Set<String> seen) {
        if ("decorated_definition".equals(node.getType())) {
            Node definition = node.getNamedChildren().stream()
                    .filter(this::isDefinitionNode)
                    .findFirst()
                    .orElse(null);
            if (definition != null) {
                processDefinition(definition, node, source, classStack, out, seen);
                // descend into the wrapped definition's children (avoid double-processing the definition node)
                definition.getNamedChildren().forEach(child -> walk(child, source, classStack, out, seen));
            } else {
                node.getNamedChildren().forEach(child -> walk(child, source, classStack, out, seen));
            }
            return;
        }

        processDefinition(node, node, source, classStack, out, seen);
        node.getNamedChildren().forEach(child -> walk(child, source, classStack, out, seen));
    }

    private void processDefinition(Node node,
                                   Node decoratorContainer,
                                   TreeSitterSource source,
                                   ArrayDeque<String> classStack,
                                   List<TextChunk> out,
                                   Set<String> seen) {
        if (isClassNode(node)) {
            Optional<String> name = identifier(node, "name");
            if (name.isEmpty()) {
                return;
            }
            Map<String, String> attributes = buildClassAttributes(decoratorContainer, source);
            addIfNotSeen(out, seen, toChunk(node, "class", name.get(), source, attributes));
            classStack.push(name.get());
            node.getNamedChildren().forEach(child -> walk(child, source, classStack, out, seen));
            classStack.pop();
        } else if (isFunctionNode(node)) {
            Optional<String> name = identifier(node, "name");
            if (name.isEmpty()) {
                return;
            }
            boolean parentIsClass = node.getParent().map(this::isClassNode).orElse(false);
            boolean insideClass = !classStack.isEmpty() || parentIsClass;
            if (parentIsClass && classStack.isEmpty()) {
                // Skip duplicate traversal when the class stack was not propagated but parent is a class.
                return;
            }
            boolean async = isAsync(node, source);
            String entityType = insideClass ? "method" : "function";
            String entityName = insideClass ? String.join(".", classStack) + "." + name.get() : name.get();
            Map<String, String> attributes = buildFunctionAttributes(node, decoratorContainer, source, async);
            addIfNotSeen(out, seen, toChunk(node, entityType, entityName, source, attributes));
        }
    }

    private void addIfNotSeen(List<TextChunk> out, Set<String> seen, TextChunk chunk) {
        String key = chunk.entityName() + "|" + chunk.startByte() + "|" + chunk.endByte();
        if (seen.add(key)) {
            out.add(chunk);
        }
    }

    private Map<String, String> buildClassAttributes(Node decoratorContainer, TreeSitterSource source) {
        List<String> decorators = collectDecorators(decoratorContainer, source);
        if (decorators.isEmpty()) {
            return Map.of();
        }
        return Map.of("decorators", String.join(",", decorators));
    }

    private Map<String, String> buildFunctionAttributes(Node node, Node decoratorContainer, TreeSitterSource source, boolean async) {
        List<String> decorators = collectDecorators(decoratorContainer, source);
        Optional<String> params = sliceField(node, "parameters", source);
        Optional<String> returnType = sliceField(node, "return_type", source);
        Optional<String> docstring = findDocstring(node, source);

        return Map.ofEntries(
                Map.entry("async", Boolean.toString(async)),
                Map.entry("parameters", params.orElse("")),
                Map.entry("return_type", returnType.orElse("")),
                Map.entry("decorators", String.join(",", decorators)),
                Map.entry("docstring", docstring.orElse(""))
        );
    }

    private Optional<String> identifier(Node node, String fieldName) {
        return node.getChildByFieldName(fieldName).map(Node::getText);
    }

    private Optional<String> sliceField(Node node, String fieldName, TreeSitterSource source) {
        return node.getChildByFieldName(fieldName)
                .map(child -> source.slice(child.getStartByte(), child.getEndByte()));
    }

    private Optional<String> findDocstring(Node node, TreeSitterSource source) {
        return node.getChildByFieldName("body")
                .flatMap(body -> body.getNamedChildren().stream().findFirst())
                .filter(first -> "expression_statement".equals(first.getType()))
                .flatMap(first -> first.getNamedChildren().stream().findFirst())
                .filter(stringNode -> "string".equals(stringNode.getType()))
                .map(str -> source.slice(str.getStartByte(), str.getEndByte()));
    }

    private boolean isAsync(Node node, TreeSitterSource source) {
        if ("async_function_definition".equals(node.getType())) {
            return true;
        }
        // Fallback: inspect leading text for async keyword
        String content = source.slice(node.getStartByte(), Math.min(node.getEndByte(), node.getStartByte() + 20))
                .toLowerCase(Locale.ROOT);
        return content.startsWith("async ");
    }

    private boolean isClassNode(Node node) {
        return "class_definition".equals(node.getType());
    }

    private boolean isFunctionNode(Node node) {
        return "function_definition".equals(node.getType()) || "async_function_definition".equals(node.getType());
    }

    private boolean isDefinitionNode(Node node) {
        return isClassNode(node) || isFunctionNode(node);
    }

    private List<String> collectDecorators(Node decoratorContainer, TreeSitterSource source) {
        List<String> decorators = new ArrayList<>();
        for (Node child : decoratorContainer.getChildren()) {
            if ("decorator".equals(child.getType())) {
                decorators.add(source.slice(child.getStartByte(), child.getEndByte()).trim());
            }
        }
        return decorators;
    }

    private static final io.megabrain.ingestion.parser.GrammarSpec PYTHON_SPEC =
            new io.megabrain.ingestion.parser.GrammarSpec(
                    LANGUAGE,
                    LANGUAGE_SYMBOL,
                    "tree-sitter-python",
                    LIBRARY_PROPERTY,
                    LIBRARY_ENV,
                    "tree-sitter-python",
                    "0.20.4"
            );
}

