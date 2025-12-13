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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ParserRegistryTest {

    private final ParserFactory python = new FakeParserFactory("python", ".py");
    private final ParserFactory js = new FakeParserFactory("javascript", ".js");
    private final ParserFactory ts = new FakeParserFactory("typescript", ".ts");
    private final ParserFactory c = new FakeParserFactory("c", ".c");
    private final ParserFactory cpp = new FakeParserFactory("cpp", ".cpp");
    private final ParserFactory javaParser = new FakeParserFactory("java", ".java");

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
            "java", javaParser
    ));

    @Test
    void resolvesParserByExtension_caseInsensitive() {
        Optional<CodeParser> parser = registry.findParser(Path.of("/repo/src/FOO.JsX"));

        assertThat(parser).isPresent();
        assertThat(parser.get().language()).isEqualTo("javascript");
    }

    @Test
    void returnsEmptyForUnknownExtension() {
        Optional<CodeParser> parser = registry.findParser(Path.of("README.md"));

        assertThat(parser).isEmpty();
    }

    @Test
    void mapsMultipleExtensionsToSameParser() {
        Optional<CodeParser> tsxParser = registry.findParser("file.tsx");
        Optional<CodeParser> tsParser = registry.findParser("file.ts");

        assertThat(tsxParser).isPresent();
        assertThat(tsParser).isPresent();
        assertThat(tsxParser.get().language()).isEqualTo("typescript");
        assertThat(tsParser.get().language()).isEqualTo("typescript");
    }

    @Test
    void prefersCForHeaderFiles() {
        Optional<CodeParser> parser = registry.findParser("foo.h");

        assertThat(parser).isPresent();
        assertThat(parser.get().language()).isEqualTo("c");
    }

    @Test
    void supportsDynamicParserRegistration() {
        ParserRegistry dynamicRegistry = new ParserRegistry();

        // Register a new parser for .txt files
        ParserFactory txtFactory = new FakeParserFactory("text", ".txt");
        dynamicRegistry.registerParser(txtFactory, "txt");

        Optional<CodeParser> parser = dynamicRegistry.findParser("document.txt");
        assertThat(parser).isPresent();
        assertThat(parser.get().language()).isEqualTo("text");
    }

    @Test
    void supportsDynamicRegistrationOfMultipleExtensions() {
        ParserRegistry dynamicRegistry = new ParserRegistry();

        // Register a parser for multiple extensions
        ParserFactory customFactory = new FakeParserFactory("custom", ".custom");
        dynamicRegistry.registerParser(customFactory, List.of("custom", "cst", "myext"));

        assertThat(dynamicRegistry.findParser("file.custom")).isPresent();
        assertThat(dynamicRegistry.findParser("file.cst")).isPresent();
        assertThat(dynamicRegistry.findParser("file.myext")).isPresent();

        assertThat(dynamicRegistry.findParser("file.custom").get().language()).isEqualTo("custom");
        assertThat(dynamicRegistry.findParser("file.cst").get().language()).isEqualTo("custom");
        assertThat(dynamicRegistry.findParser("file.myext").get().language()).isEqualTo("custom");
    }

    @Test
    void supportsParserUnregistration() {
        ParserRegistry dynamicRegistry = new ParserRegistry();

        // Register and then unregister a parser
        ParserFactory tempFactory = new FakeParserFactory("temporary", ".tmp");
        dynamicRegistry.registerParser(tempFactory, "tmp");

        assertThat(dynamicRegistry.findParser("file.tmp")).isPresent();

        dynamicRegistry.unregisterParser("tmp");

        assertThat(dynamicRegistry.findParser("file.tmp")).isEmpty();
    }

    @Test
    void throwsExceptionForNullFactory() {
        ParserRegistry dynamicRegistry = new ParserRegistry();

        assertThatThrownBy(() -> dynamicRegistry.registerParser(null, "txt"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("factory cannot be null");
    }

    @Test
    void throwsExceptionForNullExtensions() {
        ParserRegistry dynamicRegistry = new ParserRegistry();
        ParserFactory factory = new FakeParserFactory("test", ".test");

        assertThatThrownBy(() -> dynamicRegistry.registerParser(factory, (List<String>) null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("extensions cannot be null or empty");
    }

    @Test
    void throwsExceptionForEmptyExtensions() {
        ParserRegistry dynamicRegistry = new ParserRegistry();
        ParserFactory factory = new FakeParserFactory("test", ".test");

        assertThatThrownBy(() -> dynamicRegistry.registerParser(factory, List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("extensions cannot be null or empty");
    }

    @Test
    void throwsExceptionForNullExtensionInList() {
        ParserRegistry dynamicRegistry = new ParserRegistry();
        ParserFactory factory = new FakeParserFactory("test", ".test");

        assertThatThrownBy(() -> dynamicRegistry.registerParser(factory, java.util.Arrays.asList("test", null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("extension cannot be null or blank");
    }

    @Test
    void throwsExceptionForBlankExtension() {
        ParserRegistry dynamicRegistry = new ParserRegistry();
        ParserFactory factory = new FakeParserFactory("test", ".test");

        assertThatThrownBy(() -> dynamicRegistry.registerParser(factory, "   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("extension cannot be null or blank");
    }

    @Test
    void cachesParserInstances() {
        ParserRegistry dynamicRegistry = new ParserRegistry();

        // Register a parser
        ParserFactory factory = new FakeParserFactory("cached", ".cache");
        dynamicRegistry.registerParser(factory, "cache");

        // Get parser twice
        Optional<CodeParser> parser1 = dynamicRegistry.findParser("file1.cache");
        Optional<CodeParser> parser2 = dynamicRegistry.findParser("file2.cache");

        // Should be the same instance (cached)
        assertThat(parser1).isPresent();
        assertThat(parser2).isPresent();
        assertThat(parser1.get()).isSameAs(parser2.get());
    }

    @Test
    void handlesPathWithMultipleDots() {
        Optional<CodeParser> parser = registry.findParser(Path.of("com.example.MyClass.java"));
        assertThat(parser).isPresent();
        assertThat(parser.get().language()).isEqualTo("java");
    }

    @Test
    void handlesComplexFileNames() {
        // Test various file naming scenarios
        String[] complexFiles = {
            "my-file.py", "test_file.js", "component.test.tsx",
            "header-file.h", "cpp-file.cpp", "template.hpp"
        };

        String[] expectedLanguages = {"python", "javascript", "typescript", "c", "cpp", "cpp"};

        for (int i = 0; i < complexFiles.length; i++) {
            Optional<CodeParser> parser = registry.findParser(complexFiles[i]);
            assertThat(parser)
                .describedAs("Parser for complex file %s", complexFiles[i])
                .isPresent();
            assertThat(parser.get().language())
                .describedAs("Language for complex file %s", complexFiles[i])
                .isEqualTo(expectedLanguages[i]);
        }
    }

    @Test
    void unregisterParser_clearsCache() {
        ParserRegistry dynamicRegistry = new ParserRegistry();

        // Register and use a parser
        dynamicRegistry.registerParser(new FakeParserFactory("cachetest", ".cachetest"), "cachetest");
        Optional<CodeParser> parser1 = dynamicRegistry.findParser("file.cachetest");
        assertThat(parser1).isPresent();

        // Unregister
        dynamicRegistry.unregisterParser("cachetest");

        // Should not find parser anymore
        Optional<CodeParser> parser2 = dynamicRegistry.findParser("file.cachetest");
        assertThat(parser2).isEmpty();
    }

    @Test
    void supportedExtensions_includesAllRegisteredParsers() {
        // Create an empty registry for this test
        ParserRegistry dynamicRegistry = new ParserRegistry(Map.of());

        // Register multiple parsers
        dynamicRegistry.registerParser(new FakeParserFactory("lang1", ".ext1"), "ext1");
        dynamicRegistry.registerParser(new FakeParserFactory("lang2", ".ext2"), "ext2");
        dynamicRegistry.registerParser(new FakeParserFactory("lang3", ".ext3"), List.of("ext3a", "ext3b"));

        List<String> extensions = dynamicRegistry.supportedExtensions();

        assertThat(extensions).contains("ext1", "ext2", "ext3a", "ext3b");
        assertThat(extensions).hasSize(4);
    }

    @Test
    void registerParser_overwritesExistingRegistration() {
        ParserRegistry dynamicRegistry = new ParserRegistry();

        // Register first parser
        dynamicRegistry.registerParser(new FakeParserFactory("lang1", ".shared"), "shared");
        Optional<CodeParser> parser1 = dynamicRegistry.findParser("file.shared");
        assertThat(parser1).isPresent();
        assertThat(parser1.get().language()).isEqualTo("lang1");

        // Register second parser for same extension
        dynamicRegistry.registerParser(new FakeParserFactory("lang2", ".shared"), "shared");
        Optional<CodeParser> parser2 = dynamicRegistry.findParser("file.shared");
        assertThat(parser2).isPresent();
        assertThat(parser2.get().language()).isEqualTo("lang2");
    }

    private static final class FakeParserFactory implements ParserFactory {
        private final String language;
        private final String suffix;

        FakeParserFactory(String language, String suffix) {
            this.language = language;
            this.suffix = suffix;
        }

        @Override
        public CodeParser createParser() {
            return new FakeParser(language, suffix);
        }

        @Override
        public String language() {
            return language;
        }
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

