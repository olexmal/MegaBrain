/*
 * Copyright (c) 2025 MegaBrain Contributors
 * Licensed under the MIT License - see LICENSE file for details.
 */

package io.megabrain.ingestion.parser.treesitter;

import io.github.treesitter.jtreesitter.Language;
import io.github.treesitter.jtreesitter.Parser;
import io.github.treesitter.jtreesitter.Tree;
import io.megabrain.ingestion.parser.TextChunk;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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

    private static final class NoopParser extends TreeSitterParser {
        NoopParser(Supplier<Language> languageSupplier) {
            super("test", Set.of("test"), languageSupplier);
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
    }
}

