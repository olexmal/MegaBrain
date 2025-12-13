/*
 * Copyright (c) 2025 MegaBrain Contributors
 * Licensed under the MIT License - see LICENSE file for details.
 */

package io.megabrain.ingestion.parser;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for GrammarManager that test real file system operations
 * and end-to-end functionality.
 */
@QuarkusTest
class GrammarManagerIntegrationTest {

    @TempDir
    Path tempDir;

    @Test
    void integrationTest_grammarManagerOperations() {
        // This is an integration test that exercises the real GrammarManager
        // with actual file system operations

        GrammarConfig config = new GrammarConfig() {
            @Override
            public Optional<String> defaultVersion() {
                return Optional.empty();
            }

            @Override
            public Map<String, String> languageVersions() {
                return Map.of();
            }
        };

        GrammarManager manager = new GrammarManager(config);

        // Test basic operations that don't require actual downloads
        List<String> cachedVersions = manager.getCachedVersions("nonexistent-language");
        assertThat(cachedVersions).isEmpty();

        Optional<GrammarManager.GrammarVersionMetadata> versionInfo =
            manager.getVersionInfo("nonexistent-language", "1.0.0");
        assertThat(versionInfo).isEmpty();

        // Test cache stats
        GrammarManager.CacheStats stats = manager.getCacheStats();
        assertThat(stats).isNotNull();
        assertThat(stats.totalLanguages).isGreaterThanOrEqualTo(0);
        assertThat(stats.totalVersions).isGreaterThanOrEqualTo(0);

        // Test rollback operations on non-existent languages (should not fail)
        GrammarManager.RollbackResult rollbackResult =
            manager.rollbackToVersion("nonexistent", "1.0.0");
        assertThat(rollbackResult.success()).isFalse();

        GrammarManager.RollbackResult rollbackPrevResult =
            manager.rollbackToPrevious("nonexistent");
        assertThat(rollbackPrevResult.success()).isFalse();

        // Test version history
        List<GrammarManager.VersionHistoryEntry> history =
            manager.getVersionHistory("nonexistent");
        assertThat(history).isEmpty();

        // Test cleanup operations (should not fail on empty cache)
        manager.cleanupOldVersions("nonexistent", 5);
        manager.cleanupAllOldVersions(5);
    }

    @Test
    void integrationTest_parserRegistryWithAllParsers() {
        // Test that the default ParserRegistry includes all expected parsers
        ParserRegistry registry = new ParserRegistry();

        // Test all the languages we support
        String[] testFiles = {
            "main.py", "script.js", "component.tsx", "program.c", "header.h",
            "library.cpp", "util.hpp", "Main.java", "main.go", "lib.rs",
            "App.kt", "utils.rb", "Program.scala", "ViewController.swift",
            "index.php", "Program.cs"
        };

        String[] expectedLanguages = {
            "python", "javascript", "typescript", "c", "c",
            "cpp", "cpp", "java", "go", "rust",
            "kotlin", "ruby", "scala", "swift",
            "php", "csharp"
        };

        for (int i = 0; i < testFiles.length; i++) {
            Optional<CodeParser> parser = registry.findParser(testFiles[i]);
            assertThat(parser)
                .describedAs("Parser for file %s should be found", testFiles[i])
                .isPresent();
            assertThat(parser.get().language())
                .describedAs("Parser for file %s should have correct language", testFiles[i])
                .isEqualTo(expectedLanguages[i]);
        }

        // Test some unsupported extensions
        String[] unsupportedFiles = {"README.md", "data.json", "style.css", "noextension"};

        for (String file : unsupportedFiles) {
            Optional<CodeParser> parser = registry.findParser(file);
            assertThat(parser)
                .describedAs("Parser for unsupported file %s should not be found", file)
                .isEmpty();
        }
    }
}
