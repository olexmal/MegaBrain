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
import java.util.ArrayDeque;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import java.util.stream.Collectors;

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
    private final Runnable nativeLibraryLoader;
    private final AtomicBoolean nativeLibraryLoaded = new AtomicBoolean(false);
    private final AtomicBoolean nativeLibraryFailed = new AtomicBoolean(false);

    protected TreeSitterParser(String language, Set<String> supportedExtensions, Supplier<Language> languageSupplier) {
        this(language, supportedExtensions, languageSupplier, () -> {
        });
    }

    protected TreeSitterParser(String language,
                               Set<String> supportedExtensions,
                               Supplier<Language> languageSupplier,
                               Runnable nativeLibraryLoader) {
        this.language = Objects.requireNonNull(language, "language");
        Objects.requireNonNull(supportedExtensions, "supportedExtensions");
        if (supportedExtensions.isEmpty()) {
            throw new IllegalArgumentException("supportedExtensions must not be empty");
        }
        this.supportedExtensions = Set.copyOf(supportedExtensions);
        this.languageSupplier = Objects.requireNonNull(languageSupplier, "languageSupplier");
        this.nativeLibraryLoader = Objects.requireNonNull(nativeLibraryLoader, "nativeLibraryLoader");
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

    /**
     * Optional accessor for the currently resolved language (may be empty when load failed).
     */
    protected Optional<Language> currentLanguage() {
        return Optional.ofNullable(languageRef.get());
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
     * Language-specific Tree-sitter query definitions to be provided by subclasses.
     */
    protected abstract List<QueryDefinition> languageQueries();

    /**
     * Resolves query definitions by name for convenience and duplicate detection.
     */
    protected final Map<String, String> queriesByName() {
        List<QueryDefinition> definitions = languageQueries();
        if (definitions == null || definitions.isEmpty()) {
            return Map.of();
        }
        return definitions.stream()
                .collect(Collectors.toUnmodifiableMap(QueryDefinition::name, QueryDefinition::query, (first, second) -> {
                    throw new IllegalStateException("Duplicate Tree-sitter query name: " + first);
                }));
    }

    /**
     * Depth-first (pre-order) traversal helper for subclasses.
     */
    protected final void traverseDepthFirst(Node rootNode, java.util.function.Consumer<Node> visitor) {
        Objects.requireNonNull(rootNode, "rootNode");
        Objects.requireNonNull(visitor, "visitor");

        ArrayDeque<Node> stack = new ArrayDeque<>();
        stack.push(rootNode);
        while (!stack.isEmpty()) {
            Node current = stack.pop();
            visitor.accept(current);
            int childCount = current.getChildCount();
            for (int i = childCount - 1; i >= 0; i--) {
                Optional<Node> child = current.getChild(i);
                child.ifPresent(stack::push);
            }
        }
    }

    /**
     * Definition of a Tree-sitter query identified by a unique name.
     */
    protected record QueryDefinition(String name, String query) {
        public QueryDefinition {
            Objects.requireNonNull(name, "name");
            Objects.requireNonNull(query, "query");
            if (name.isBlank()) {
                throw new IllegalArgumentException("query name must not be blank");
            }
            if (query.isBlank()) {
                throw new IllegalArgumentException("query must not be blank");
            }
        }
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
        if (!ensureNativeLibraryLoaded()) {
            return null;
        }
        Language loaded = loadLanguageSafely();
        if (loaded != null && languageRef.compareAndSet(null, loaded)) {
            return loaded;
        }
        return languageRef.get();
    }

    private boolean ensureNativeLibraryLoaded() {
        if (nativeLibraryLoaded.get()) {
            return true;
        }
        if (nativeLibraryFailed.get()) {
            return false;
        }
        try {
            nativeLibraryLoader.run();
            nativeLibraryLoaded.set(true);
            return true;
        } catch (Exception | UnsatisfiedLinkError e) {
            nativeLibraryFailed.set(true);
            LOG.errorf(e, "Failed to load Tree-sitter native library for %s", language);
            return false;
        }
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

