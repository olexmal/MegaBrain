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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Tree-sitter parser for C source code.
 */
public class CTreeSitterParser extends TreeSitterParser {

    private static final Logger LOG = Logger.getLogger(CTreeSitterParser.class);
    private static final String LANGUAGE = "c";
    private static final Set<String> SUPPORTED_EXTENSIONS = Set.of("c", "h");
    private static final String LIBRARY_ENV = "TREE_SITTER_C_LIB";
    private static final String LIBRARY_PROPERTY = "tree.sitter.c.library";
    private static final String LANGUAGE_SYMBOL = "tree_sitter_c";

    public CTreeSitterParser() {
        this(new io.megabrain.ingestion.parser.GrammarManager());
    }

    public CTreeSitterParser(io.megabrain.ingestion.parser.GrammarManager grammarManager) {
        this(grammarManager.languageSupplier(C_SPEC), grammarManager.nativeLoader(C_SPEC));
    }

    CTreeSitterParser(Supplier<Language> languageSupplier, Runnable nativeLoader) {
        super(LANGUAGE, SUPPORTED_EXTENSIONS, languageSupplier, nativeLoader);
    }

    @Override
    protected List<QueryDefinition> languageQueries() {
        return List.of(
                new QueryDefinition("functions", """
                        (function_definition
                            declarator: (function_declarator
                                declarator: (identifier) @function.name
                                parameters: (parameter_list) @function.parameters)?)
                        """),
                new QueryDefinition("structs", """
                        (struct_specifier
                            name: (type_identifier) @struct.name)?
                        """)
        );
    }

    @Override
    protected List<TextChunk> extractChunks(Node rootNode, Tree tree, TreeSitterSource source) {
        List<TextChunk> chunks = new ArrayList<>();
        Set<String> seen = java.util.Collections.newSetFromMap(new java.util.concurrent.ConcurrentHashMap<>());
        traverseDepthFirst(rootNode, node -> {
            if (isStruct(node)) {
                processStruct(node, source, chunks, seen);
            } else if (isFunction(node)) {
                processFunction(node, source, chunks, seen);
            }
        });
        return chunks;
    }

    private boolean isStruct(Node node) {
        return "struct_specifier".equals(node.getType());
    }

    private boolean isFunction(Node node) {
        return "function_definition".equals(node.getType());
    }

    private void processStruct(Node node, TreeSitterSource source, List<TextChunk> out, Set<String> seen) {
        Optional<String> name = identifier(node, "name");
        if (name.isEmpty()) {
            return;
        }
        Map<String, String> attributes = new LinkedHashMap<>();
        addChunk(out, toChunk(node, "struct", name.get(), source, attributes), seen);
    }

    private void processFunction(Node node, TreeSitterSource source, List<TextChunk> out, Set<String> seen) {
        Optional<Node> declarator = node.getChildByFieldName("declarator");
        Optional<String> name = declarator.flatMap(this::findIdentifier);
        if (name.isEmpty()) {
            return;
        }
        Map<String, String> attributes = new LinkedHashMap<>();
        declarator.ifPresent(d -> attributes.put("signature", source.slice(d.getStartByte(), d.getEndByte())));
        sliceField(node, "type", source).ifPresent(type -> attributes.put("return_type", type));
        declarator
                .flatMap(d -> d.getChildByFieldName("parameters"))
                .ifPresent(params -> attributes.put("parameters", source.slice(params.getStartByte(), params.getEndByte())));
        addChunk(out, toChunk(node, "function", name.get(), source, attributes), seen);
    }

    private Optional<String> identifier(Node node, String fieldName) {
        return node.getChildByFieldName(fieldName).map(Node::getText).map(String::trim);
    }

    private Optional<String> sliceField(Node node, String fieldName, TreeSitterSource source) {
        return node.getChildByFieldName(fieldName)
                .map(child -> source.slice(child.getStartByte(), child.getEndByte()).trim());
    }

    private Optional<String> findIdentifier(Node node) {
        if ("identifier".equals(node.getType())) {
            return Optional.ofNullable(node.getText()).map(String::trim);
        }
        for (Node child : node.getNamedChildren()) {
            Optional<String> found = findIdentifier(child);
            if (found.isPresent()) {
                return found;
            }
        }
        return Optional.empty();
    }

    private void addChunk(List<TextChunk> out, TextChunk chunk, Set<String> seen) {
        String key = chunk.entityType() + "|" + chunk.entityName() + "|" + chunk.startByte() + "|" + chunk.endByte();
        if (seen.add(key)) {
            out.add(chunk);
        }
    }

    private static final io.megabrain.ingestion.parser.GrammarSpec C_SPEC =
            new io.megabrain.ingestion.parser.GrammarSpec(
                    LANGUAGE,
                    LANGUAGE_SYMBOL,
                    "tree-sitter-c",
                    LIBRARY_PROPERTY,
                    LIBRARY_ENV,
                    "tree-sitter-c",
                    "0.21.0"
            );
}

