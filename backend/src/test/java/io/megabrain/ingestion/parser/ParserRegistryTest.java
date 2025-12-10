/*
 * Copyright (c) 2025 MegaBrain Contributors
 * Licensed under the MIT License - see LICENSE file for details.
 */

package io.megabrain.ingestion.parser;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ParserRegistryTest {

    private final CodeParser python = new FakeParser("python", ".py");
    private final CodeParser js = new FakeParser("javascript", ".js");
    private final CodeParser ts = new FakeParser("typescript", ".ts");
    private final CodeParser c = new FakeParser("c", ".c");
    private final CodeParser cpp = new FakeParser("cpp", ".cpp");
    private final CodeParser java = new FakeParser("java", ".java");

    private ParserRegistry registry = new ParserRegistry(Map.of(
            "py", python,
            "js", js,
            "jsx", js,
            "ts", ts,
            "tsx", ts,
            "c", c,
            "h", c,
            "cpp", cpp,
            "hpp", cpp,
            "java", java
    ));

    @Test
    void resolvesParserByExtension_caseInsensitive() {
        Optional<CodeParser> parser = registry.findParser(Path.of("/repo/src/FOO.JsX"));

        assertThat(parser).contains(js);
    }

    @Test
    void returnsEmptyForUnknownExtension() {
        Optional<CodeParser> parser = registry.findParser(Path.of("README.md"));

        assertThat(parser).isEmpty();
    }

    @Test
    void mapsMultipleExtensionsToSameParser() {
        assertThat(registry.findParser("file.tsx")).contains(ts);
        assertThat(registry.findParser("file.ts")).contains(ts);
    }

    @Test
    void prefersCForHeaderFiles() {
        assertThat(registry.findParser("foo.h")).contains(c);
    }

    private static final class FakeParser implements CodeParser {
        private final String language;
        private final String suffix;

        FakeParser(String language, String suffix) {
            this.language = language;
            this.suffix = suffix;
        }

        @Override
        public boolean supports(Path filePath) {
            return filePath != null && filePath.toString().endsWith(suffix);
        }

        @Override
        public List<io.megabrain.ingestion.parser.TextChunk> parse(Path filePath) {
            return List.of();
        }

        @Override
        public String language() {
            return language;
        }
    }
}

