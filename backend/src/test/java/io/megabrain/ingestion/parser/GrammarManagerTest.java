/*
 * Copyright (c) 2025 MegaBrain Contributors
 * Licensed under the MIT License - see LICENSE file for details.
 */

package io.megabrain.ingestion.parser;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import uk.org.webcompere.systemstubs.environment.EnvironmentVariables;
import uk.org.webcompere.systemstubs.jupiter.SystemStub;
import uk.org.webcompere.systemstubs.jupiter.SystemStubsExtension;
import uk.org.webcompere.systemstubs.properties.SystemProperties;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(SystemStubsExtension.class)
class GrammarManagerTest {

    @TempDir
    Path tempDir;

    @SystemStub
    private SystemProperties systemProperties;

    @SystemStub
    private EnvironmentVariables environmentVariables;

    private GrammarManager grammarManager;
    private GrammarSpec testSpec;

    @BeforeEach
    void setUp() {
        // Set custom cache directory for testing
        systemProperties.set("megabrain.grammar.cache.dir", tempDir.toString());

        grammarManager = new GrammarManager();
        testSpec = new GrammarSpec(
                "testlang",
                "tree_sitter_testlang",
                "tree-sitter-testlang",
                "megabrain.grammar.testlang.path",
                "MEGABRAIN_GRAMMAR_TESTLANG_PATH",
                "tree-sitter-testlang",
                "1.0.0"
        );
    }

    @Test
    void resolveCacheDir_usesConfiguredProperty() throws Exception {
        String customPath = "/custom/cache/path";
        systemProperties.set("megabrain.grammar.cache.dir", customPath);

        GrammarManager customManager = new GrammarManager();
        // Access private method via reflection for testing
        java.lang.reflect.Method method = GrammarManager.class.getDeclaredMethod("resolveCacheDir");
        method.setAccessible(true);
        Path result = (Path) method.invoke(customManager);

        assertThat(result).isEqualTo(Path.of(customPath));
    }

    @Test
    void resolveCacheDir_usesEnvironmentVariable() throws Exception {
        String customPath = "/env/cache/path";

        // Clear system property so environment variable takes precedence
        systemProperties.remove("megabrain.grammar.cache.dir");
        environmentVariables.set("MEGABRAIN_GRAMMAR_CACHE_DIR", customPath);

        GrammarManager customManager = new GrammarManager();
        // Access private method via reflection for testing
        java.lang.reflect.Method method = GrammarManager.class.getDeclaredMethod("resolveCacheDir");
        method.setAccessible(true);
        Path result = (Path) method.invoke(customManager);

        assertThat(result).isEqualTo(Path.of(customPath));
    }

    @Test
    void resolveCacheDir_usesDefaultPath() {
        // Clear any custom configuration
        System.clearProperty("megabrain.grammar.cache.dir");
        System.clearProperty("MEGABRAIN_GRAMMAR_CACHE_DIR");

        try {
            java.lang.reflect.Method method = GrammarManager.class.getDeclaredMethod("resolveCacheDir");
            method.setAccessible(true);
            Path result = (Path) method.invoke(grammarManager);

            assertThat(result).isEqualTo(Path.of(System.getProperty("user.home"), ".megabrain", "grammars"));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void platformLibraryExtension_detectsLinux() {
        // This test assumes we're running on Linux (common for CI)
        try {
            java.lang.reflect.Method method = GrammarManager.class.getDeclaredMethod("platformLibraryExtension");
            method.setAccessible(true);
            String result = (String) method.invoke(grammarManager);

            String os = System.getProperty("os.name", "").toLowerCase();
            if (os.contains("linux")) {
                assertThat(result).isEqualTo(".so");
            } else if (os.contains("mac")) {
                assertThat(result).isEqualTo(".dylib");
            } else if (os.contains("win")) {
                assertThat(result).isEqualTo(".dll");
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void getVersionInfo_returnsEmptyForNonExistentGrammar() {
        Optional<GrammarManager.GrammarVersionMetadata> result = grammarManager.getVersionInfo("nonexistent", "1.0.0");

        assertThat(result).isEmpty();
    }

    @Test
    void versionMetadata_serializationAndDeserialization() throws IOException {
        // Create test metadata
        Instant testTime = Instant.parse("2023-12-12T10:00:00Z");
        GrammarManager.GrammarVersionMetadata metadata = new GrammarManager.GrammarVersionMetadata(
                "testlang",
                "1.0.0",
                "tree-sitter-testlang",
                testTime,
                "linux-x86_64",
                1024L
        );

        // Test serialization
        Path metadataPath = tempDir.resolve("test-metadata.json");
        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        mapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
        mapper.writeValue(metadataPath.toFile(), metadata);

        // Test deserialization
        GrammarManager.GrammarVersionMetadata deserialized = mapper.readValue(metadataPath.toFile(), GrammarManager.GrammarVersionMetadata.class);

        assertThat(deserialized.language()).isEqualTo(metadata.language());
        assertThat(deserialized.version()).isEqualTo(metadata.version());
        assertThat(deserialized.repository()).isEqualTo(metadata.repository());
        assertThat(deserialized.platform()).isEqualTo(metadata.platform());
        assertThat(deserialized.fileSize()).isEqualTo(metadata.fileSize());
        assertThat(deserialized.downloadedAt()).isEqualTo(metadata.downloadedAt());
    }

    @Test
    void loadLanguage_returnsNullForInvalidSpec() {
        GrammarSpec invalidSpec = new GrammarSpec(
                "invalid",
                "invalid_symbol",
                "invalid-lib",
                "test.prop",
                "TEST_ENV",
                "invalid-repo",
                "0.0.0"
        );

        var result = grammarManager.loadLanguage(invalidSpec);
        assertThat(result).isNull();
    }

    @Test
    void languageSupplier_returnsSupplier() {
        var supplier = grammarManager.languageSupplier(testSpec);
        assertThat(supplier).isNotNull();

        // Note: We don't call get() here as it would try to load an actual native library
    }

    @Test
    void nativeLoader_returnsRunnable() {
        var loader = grammarManager.nativeLoader(testSpec);
        assertThat(loader).isNotNull();

        // Note: We don't call run() here as it would try to load an actual native library
    }

    @Test
    void calculateSha256_calculatesCorrectHash() throws Exception {
        // Create a test file with known content
        String testContent = "Hello, World!";
        Path testFile = tempDir.resolve("test.txt");
        Files.writeString(testFile, testContent);

        // Expected SHA256 hash for "Hello, World!" (calculated externally)
        String expectedHash = "dffd6021bb2bd5b0af676290809ec3a53191dd81c7f70a4b28688a362182986f";

        // Access private method via reflection
        java.lang.reflect.Method method = GrammarManager.class.getDeclaredMethod("calculateSha256", Path.class);
        method.setAccessible(true);
        String actualHash = (String) method.invoke(grammarManager, testFile);

        assertThat(actualHash).isEqualTo(expectedHash);
    }

    @Test
    void calculateSha256_throwsExceptionForNonExistentFile() throws Exception {
        Path nonExistentFile = tempDir.resolve("nonexistent.txt");

        // Access private method via reflection
        java.lang.reflect.Method method = GrammarManager.class.getDeclaredMethod("calculateSha256", Path.class);
        method.setAccessible(true);

        assertThatThrownBy(() -> method.invoke(grammarManager, nonExistentFile))
                .isInstanceOf(java.lang.reflect.InvocationTargetException.class)
                .hasCauseInstanceOf(IOException.class);
    }

    @Test
    void verifyDownloadedFile_acceptsValidFile() throws Exception {
        // Create a valid test file
        String testContent = "test content";
        Path testFile = tempDir.resolve("valid-test.so");
        Files.writeString(testFile, testContent);

        // Track progress callback invocations
        java.util.List<String> progressMessages = new java.util.ArrayList<>();
        GrammarManager.DownloadProgressCallback callback = (downloaded, total, message) -> {
            progressMessages.add(message);
        };

        GrammarSpec spec = new GrammarSpec("test", "symbol", "lib", "prop", "env", "repo", "1.0.0");

        // Access private method via reflection
        java.lang.reflect.Method method = GrammarManager.class.getDeclaredMethod("verifyDownloadedFile",
                GrammarSpec.class, Path.class, GrammarManager.DownloadProgressCallback.class);
        method.setAccessible(true);

        // Should not throw exception
        method.invoke(grammarManager, spec, testFile, callback);

        // Should have received progress messages
        assertThat(progressMessages).isNotEmpty();
        assertThat(progressMessages.get(progressMessages.size() - 1)).contains("bytes");
    }

    @Test
    void verifyDownloadedFile_throwsExceptionForEmptyFile() throws Exception {
        // Create an empty file
        Path emptyFile = tempDir.resolve("empty.so");
        Files.createFile(emptyFile);

        GrammarManager.DownloadProgressCallback callback = (d, t, m) -> {};
        GrammarSpec spec = new GrammarSpec("test", "symbol", "lib", "prop", "env", "repo", "1.0.0");

        // Access private method via reflection
        java.lang.reflect.Method method = GrammarManager.class.getDeclaredMethod("verifyDownloadedFile",
                GrammarSpec.class, Path.class, GrammarManager.DownloadProgressCallback.class);
        method.setAccessible(true);

        assertThatThrownBy(() -> method.invoke(grammarManager, spec, emptyFile, callback))
                .isInstanceOf(java.lang.reflect.InvocationTargetException.class)
                .hasCauseInstanceOf(IOException.class)
                .hasRootCauseMessage("Downloaded file is empty: %s", emptyFile.toString());
    }

    @Test
    void verifyDownloadedFile_throwsExceptionForNonExistentFile() throws Exception {
        Path nonExistentFile = tempDir.resolve("nonexistent.so");

        GrammarManager.DownloadProgressCallback callback = (d, t, m) -> {};
        GrammarSpec spec = new GrammarSpec("test", "symbol", "lib", "prop", "env", "repo", "1.0.0");

        // Access private method via reflection
        java.lang.reflect.Method method = GrammarManager.class.getDeclaredMethod("verifyDownloadedFile",
                GrammarSpec.class, Path.class, GrammarManager.DownloadProgressCallback.class);
        method.setAccessible(true);

        assertThatThrownBy(() -> method.invoke(grammarManager, spec, nonExistentFile, callback))
                .isInstanceOf(java.lang.reflect.InvocationTargetException.class)
                .hasCauseInstanceOf(IOException.class)
                .hasRootCauseMessage("Downloaded file does not exist: %s", nonExistentFile.toString());
    }

    @Test
    void downloadProgressCallback_noOpImplementation() {
        // Test that NO_PROGRESS callback doesn't throw exceptions
        GrammarManager.DownloadProgressCallback callback = GrammarManager.NO_PROGRESS;

        // Should not throw any exceptions
        callback.onProgress(100, 1000, "test message");
        callback.onProgress(0, -1, "another message");
    }
}
