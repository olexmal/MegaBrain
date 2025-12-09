/*
 * Copyright (c) 2025 MegaBrain Contributors
 * Licensed under the MIT License - see LICENSE file for details.
 */

package io.megabrain.ingestion.parser.treesitter;

import io.github.treesitter.jtreesitter.Language;
import io.github.treesitter.jtreesitter.Node;
import io.github.treesitter.jtreesitter.Parser;
import io.github.treesitter.jtreesitter.Tree;
import io.megabrain.ingestion.parser.TextChunk;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TreeSitterParserTest {

    @Test
    void supportsOnlyConfiguredExtensions(@TempDir Path tempDir) throws IOException {
        TreeSitterParser parser = new NoopParser(() -> null);

        Path supported = tempDir.resolve("sample.test");
        Files.writeString(supported, "content");
        Path unsupported = tempDir.resolve("sample.txt");
        Files.writeString(unsupported, "content");

        assertThat(parser.supports(supported)).isTrue();
        assertThat(parser.supports(unsupported)).isFalse();
        assertThat(parser.supports(null)).isFalse();
    }

    @Test
    void parseReturnsEmptyForUnsupportedExtension(@TempDir Path tempDir) throws IOException {
        TreeSitterParser parser = new NoopParser(() -> null);
        Path file = tempDir.resolve("sample.txt");
        Files.writeString(file, "content");

        List<TextChunk> chunks = parser.parse(file);

        assertThat(chunks).isEmpty();
    }

    @Test
    void parseReturnsEmptyWhenLanguageFailsToLoad(@TempDir Path tempDir) throws IOException {
        TreeSitterParser parser = new NoopParser(() -> null);
        Path file = tempDir.resolve("sample.test");
        Files.writeString(file, "content");

        List<TextChunk> chunks = parser.parse(file);

        assertThat(chunks).isEmpty();
    }

    @Test
    void parseThrowsForMissingFile() {
        TreeSitterParser parser = new NoopParser(() -> null);
        Path file = Path.of("does-not-exist.test");

        assertThatThrownBy(() -> parser.parse(file))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void traverseDepthFirstVisitsNodesPreOrder() {
        Node root = mock(Node.class);
        Node childA = mock(Node.class);
        Node childB = mock(Node.class);
        Node grandChild = mock(Node.class);

        when(root.getChildCount()).thenReturn(2);
        when(root.getChild(0)).thenReturn(Optional.of(childA));
        when(root.getChild(1)).thenReturn(Optional.of(childB));

        when(childA.getChildCount()).thenReturn(1);
        when(childA.getChild(0)).thenReturn(Optional.of(grandChild));
        when(childB.getChildCount()).thenReturn(0);
        when(grandChild.getChildCount()).thenReturn(0);

        TreeSitterParser parser = new NoopParser(() -> null);
        List<Node> visited = new ArrayList<>();

        parser.traverseDepthFirst(root, visited::add);

        assertThat(visited).containsExactly(root, childA, grandChild, childB);
    }

    @Test
    void nativeLoaderFailureShortCircuitsParse(@TempDir Path tempDir) throws IOException {
        AtomicBoolean languageInvoked = new AtomicBoolean(false);
        AtomicInteger loaderInvocations = new AtomicInteger();
        TreeSitterParser parser = new NoopParser(() -> {
            languageInvoked.set(true);
            return null;
        }, () -> {
            loaderInvocations.incrementAndGet();
            throw new UnsatisfiedLinkError("native missing");
        });

        Path file = tempDir.resolve("sample.test");
        Files.writeString(file, "content");

        List<TextChunk> chunks = parser.parse(file);

        assertThat(chunks).isEmpty();
        assertThat(languageInvoked).isFalse();
        assertThat(loaderInvocations.get()).isEqualTo(1);
    }

    @Test
    void nativeLoaderRunsOnceAcrossParses(@TempDir Path tempDir) throws IOException {
        AtomicInteger loaderInvocations = new AtomicInteger();
        TreeSitterParser parser = new NoopParser(() -> null, loaderInvocations::incrementAndGet);

        Path file = tempDir.resolve("sample.test");
        Files.writeString(file, "content");

        parser.parse(file);
        parser.parse(file);

        assertThat(loaderInvocations.get()).isEqualTo(1);
    }

    @Test
    void queriesByNameExposesDefinitions() {
        TreeSitterParser parser = new QueryParser(() -> null);

        assertThat(parser.queriesByName())
                .containsEntry("functions", "(function_definition) @fn")
                .containsEntry("classes", "(class_definition) @cls");
    }

    private static final class NoopParser extends TreeSitterParser {
        NoopParser(Supplier<Language> languageSupplier) {
            this(languageSupplier, () -> {
            });
        }

        NoopParser(Supplier<Language> languageSupplier, Runnable nativeLibraryLoader) {
            super("test", Set.of("test"), languageSupplier, nativeLibraryLoader);
        }

        @Override
        protected Parser createParser(Language lang) {
            return new Parser();
        }

        @Override
        protected Optional<Tree> parse(Parser parser, String source) {
            return Optional.empty();
        }

        @Override
        protected List<TextChunk> extractChunks(io.github.treesitter.jtreesitter.Node rootNode,
                                                io.github.treesitter.jtreesitter.Tree tree,
                                                TreeSitterSource source) {
            return List.of();
        }

        @Override
        protected List<QueryDefinition> languageQueries() {
            return List.of();
        }
    }

    private static final class QueryParser extends TreeSitterParser {
        QueryParser(Supplier<Language> languageSupplier) {
            super("test", Set.of("test"), languageSupplier);
        }

        @Override
        protected Optional<Tree> parse(Parser parser, String source) {
            return Optional.empty();
        }

        @Override
        protected List<TextChunk> extractChunks(Node rootNode, Tree tree, TreeSitterSource source) {
            return List.of();
        }

        @Override
        protected List<QueryDefinition> languageQueries() {
            return List.of(
                    new QueryDefinition("functions", "(function_definition) @fn"),
                    new QueryDefinition("classes", "(class_definition) @cls")
            );
        }
    }
}

