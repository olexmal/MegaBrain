/*
 * Copyright (c) 2025 MegaBrain Contributors
 * Licensed under the MIT License - see LICENSE file for details.
 */

package io.megabrain.ingestion.parser.treesitter;

import io.github.treesitter.jtreesitter.InputEncoding;
import io.github.treesitter.jtreesitter.Language;
import io.github.treesitter.jtreesitter.Node;
import io.github.treesitter.jtreesitter.Parser;
import io.github.treesitter.jtreesitter.Point;
import io.github.treesitter.jtreesitter.Tree;
import io.megabrain.ingestion.parser.CodeParser;
import io.megabrain.ingestion.parser.TextChunk;
import org.jboss.logging.Logger;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

/**
 * Base Tree-sitter parser that handles grammar loading, parsing, and common error handling.
 * Subclasses provide language-specific queries and chunk extraction.
 */
public abstract class TreeSitterParser implements CodeParser {

    private static final Logger LOG = Logger.getLogger(TreeSitterParser.class);
    private static final InputEncoding ENCODING = InputEncoding.UTF_8;

    private final String language;
    private final Set<String> supportedExtensions;
    private final Supplier<Language> languageSupplier;
    private final AtomicReference<Language> languageRef = new AtomicReference<>();

    protected TreeSitterParser(String language, Set<String> supportedExtensions, Supplier<Language> languageSupplier) {
        this.language = Objects.requireNonNull(language, "language");
        Objects.requireNonNull(supportedExtensions, "supportedExtensions");
        if (supportedExtensions.isEmpty()) {
            throw new IllegalArgumentException("supportedExtensions must not be empty");
        }
        this.supportedExtensions = Set.copyOf(supportedExtensions);
        this.languageSupplier = Objects.requireNonNull(languageSupplier, "languageSupplier");
    }

    @Override
    public boolean supports(Path filePath) {
        if (filePath == null) {
            return false;
        }
        String fileName = filePath.getFileName().toString().toLowerCase(Locale.ROOT);
        return supportedExtensions.stream().anyMatch(fileName::endsWith);
    }

    @Override
    public List<TextChunk> parse(Path filePath) {
        Objects.requireNonNull(filePath, "filePath");

        if (!supports(filePath)) {
            return List.of();
        }
        if (!Files.isRegularFile(filePath)) {
            throw new IllegalArgumentException("File does not exist or is not a regular file: " + filePath);
        }

        String source;
        try {
            source = Files.readString(filePath);
        } catch (IOException e) {
            LOG.errorf(e, "Failed to read source file: %s", filePath);
            return List.of();
        }

        Language lang = resolveLanguage();
        if (lang == null) {
            LOG.errorf("Tree-sitter language failed to load for %s; skipping parse", filePath);
            return List.of();
        }

        try (Parser parser = createParser(lang)) {
            Optional<Tree> parsed = parse(parser, source);
            if (parsed.isEmpty()) {
                LOG.warnf("Tree-sitter did not produce a parse tree for %s; returning empty chunk list", filePath);
                return List.of();
            }

            try (Tree tree = parsed.get()) {
                Node root = tree.getRootNode();
                TreeSitterSource tsSource = new TreeSitterSource(source, filePath, StandardCharsets.UTF_8);
                return List.copyOf(extractChunks(root, tree, tsSource));
            }
        } catch (Exception | UnsatisfiedLinkError e) {
            LOG.errorf(e, "Tree-sitter parse failed for %s", filePath);
            return List.of();
        }
    }

    @Override
    public String language() {
        return language;
    }

    protected Parser createParser(Language lang) {
        return new Parser(lang);
    }

    /**
     * Parses the source code into a Tree-sitter tree. Exposed for testing overrides.
     */
    protected Optional<Tree> parse(Parser parser, String source) {
        return parser.parse(source, ENCODING);
    }

    /**
     * Convert a Tree-sitter node into a {@link TextChunk} using common metadata mapping.
     *
     * @param node       Tree-sitter node
     * @param entityType normalized entity type (e.g., function, class, method)
     * @param entityName fully qualified entity name
     * @param source     source helper
     * @param attributes optional attributes map
     * @return immutable text chunk
     */
    protected TextChunk toChunk(Node node, String entityType, String entityName, TreeSitterSource source, Map<String, String> attributes) {
        Objects.requireNonNull(node, "node");
        Objects.requireNonNull(entityType, "entityType");
        Objects.requireNonNull(entityName, "entityName");
        Objects.requireNonNull(source, "source");

        Point startPoint = node.getStartPoint();
        Point endPoint = node.getEndPoint();
        int startLine = source.toLineNumber(startPoint);
        int endLine = source.toLineNumber(endPoint);
        int startByte = node.getStartByte();
        int endByte = node.getEndByte();
        String content = source.slice(startByte, endByte);

        Map<String, String> safeAttributes = attributes == null ? Map.of() : Map.copyOf(attributes);

        return new TextChunk(
                content,
                language(),
                entityType,
                entityName,
                source.filePath().toString(),
                startLine,
                endLine,
                startByte,
                endByte,
                safeAttributes
        );
    }

    /**
     * Implemented by language-specific parsers to build {@link TextChunk} records from the parsed tree.
     */
    protected abstract List<TextChunk> extractChunks(Node rootNode, Tree tree, TreeSitterSource source);

    private Language resolveLanguage() {
        Language existing = languageRef.get();
        if (existing != null) {
            return existing;
        }
        Language loaded = loadLanguageSafely();
        if (loaded != null && languageRef.compareAndSet(null, loaded)) {
            return loaded;
        }
        return languageRef.get();
    }

    private Language loadLanguageSafely() {
        try {
            Language loaded = Objects.requireNonNull(languageSupplier.get(), "languageSupplier returned null language");
            LOG.debugf("Loaded Tree-sitter language: %s", loaded.getName());
            return loaded;
        } catch (Exception | UnsatisfiedLinkError e) {
            LOG.errorf(e, "Failed to load Tree-sitter language for %s", language);
            return null;
        }
    }
}

