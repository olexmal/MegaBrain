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
import java.util.List;
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

    @Test
    void getCachedVersions_returnsEmptyListForNonExistentLanguage() {
        java.util.List<String> versions = grammarManager.getCachedVersions("nonexistent");

        assertThat(versions).isEmpty();
    }

    @Test
    void getCachedVersions_returnsVersionsInCorrectOrder() throws Exception {
        // Create mock cache structure with multiple versions
        Path langDir = tempDir.resolve("testlang");
        Path version1Dir = langDir.resolve("1.0.0").resolve("linux-x86_64");
        Path version2Dir = langDir.resolve("1.1.0").resolve("linux-x86_64");
        Path version3Dir = langDir.resolve("2.0.0").resolve("linux-x86_64");

        Files.createDirectories(version1Dir);
        Files.createDirectories(version2Dir);
        Files.createDirectories(version3Dir);

        java.util.List<String> versions = grammarManager.getCachedVersions("testlang");

        // Should be sorted newest first
        assertThat(versions).containsExactly("2.0.0", "1.1.0", "1.0.0");
    }

    @Test
    void cleanupOldVersions_keepsSpecifiedNumber() throws Exception {
        // Create mock cache structure with multiple versions
        Path langDir = tempDir.resolve("testlang");
        Path version1Dir = langDir.resolve("1.0.0").resolve("linux-x86_64");
        Path version2Dir = langDir.resolve("1.1.0").resolve("linux-x86_64");
        Path version3Dir = langDir.resolve("2.0.0").resolve("linux-x86_64");
        Path version4Dir = langDir.resolve("2.1.0").resolve("linux-x86_64");

        Files.createDirectories(version1Dir);
        Files.createDirectories(version2Dir);
        Files.createDirectories(version3Dir);
        Files.createDirectories(version4Dir);

        // Create some files in each version directory
        Files.writeString(version1Dir.resolve("test.so"), "lib1");
        Files.writeString(version2Dir.resolve("test.so"), "lib2");
        Files.writeString(version3Dir.resolve("test.so"), "lib3");
        Files.writeString(version4Dir.resolve("test.so"), "lib4");

        int removed = grammarManager.cleanupOldVersions("testlang", 2);

        assertThat(removed).isEqualTo(2); // Should remove 2 oldest versions

        // Check that only the 2 newest versions remain
        java.util.List<String> remainingVersions = grammarManager.getCachedVersions("testlang");
        assertThat(remainingVersions).containsExactly("2.1.0", "2.0.0");

        // Verify directories were actually deleted
        assertThat(Files.exists(version1Dir.getParent())).isFalse();
        assertThat(Files.exists(version2Dir.getParent())).isFalse();
        assertThat(Files.exists(version3Dir.getParent())).isTrue();
        assertThat(Files.exists(version4Dir.getParent())).isTrue();
    }

    @Test
    void cleanupOldVersions_throwsExceptionForInvalidMaxVersions() {
        assertThatThrownBy(() -> grammarManager.cleanupOldVersions("testlang", 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("maxVersions must be at least 1");
    }

    @Test
    void cleanupAllOldVersions_cleansMultipleLanguages() throws Exception {
        // Create cache structure for multiple languages
        Path lang1Dir = tempDir.resolve("lang1");
        Path lang2Dir = tempDir.resolve("lang2");

        // Lang1: 3 versions
        Files.createDirectories(lang1Dir.resolve("1.0.0").resolve("linux-x86_64"));
        Files.createDirectories(lang1Dir.resolve("1.1.0").resolve("linux-x86_64"));
        Files.createDirectories(lang1Dir.resolve("2.0.0").resolve("linux-x86_64"));

        // Lang2: 2 versions
        Files.createDirectories(lang2Dir.resolve("0.9.0").resolve("linux-x86_64"));
        Files.createDirectories(lang2Dir.resolve("1.0.0").resolve("linux-x86_64"));

        int removed = grammarManager.cleanupAllOldVersions(1); // Keep only 1 version per language

        assertThat(removed).isEqualTo(3); // Should remove 2 from lang1 + 1 from lang2

        // Verify only 1 version remains per language
        assertThat(grammarManager.getCachedVersions("lang1")).hasSize(1);
        assertThat(grammarManager.getCachedVersions("lang2")).hasSize(1);
    }

    @Test
    void getCacheStats_returnsCorrectStatistics() throws Exception {
        // Create mock cache structure
        Path langDir = tempDir.resolve("testlang");
        Path versionDir = langDir.resolve("1.0.0").resolve("linux-x86_64");
        Files.createDirectories(versionDir);

        // Create library file
        Path libFile = versionDir.resolve("test.so");
        Files.writeString(libFile, "test library content");

        // Create metadata file
        Path metadataFile = versionDir.resolve("metadata.json");
        Files.writeString(metadataFile, "{}");

        GrammarManager.CacheStats stats = grammarManager.getCacheStats();

        assertThat(stats.totalLanguages).isEqualTo(1);
        assertThat(stats.totalVersions).isEqualTo(1);
        assertThat(stats.totalFiles).isEqualTo(2);
        assertThat(stats.libraryFiles).isEqualTo(1);
        assertThat(stats.metadataFiles).isEqualTo(1);
        assertThat(stats.totalSizeBytes).isGreaterThan(0);
        assertThat(stats.librarySizeBytes).isGreaterThan(0);
    }

    @Test
    void getCacheStats_returnsEmptyStatsForEmptyCache() {
        GrammarManager.CacheStats stats = grammarManager.getCacheStats();

        assertThat(stats.totalLanguages).isEqualTo(0);
        assertThat(stats.totalVersions).isEqualTo(0);
        assertThat(stats.totalFiles).isEqualTo(0);
        assertThat(stats.libraryFiles).isEqualTo(0);
        assertThat(stats.metadataFiles).isEqualTo(0);
        assertThat(stats.totalSizeBytes).isEqualTo(0);
        assertThat(stats.librarySizeBytes).isEqualTo(0);
    }

    @Test
    void cleanupOldVersions_defaultOverload_usesDefaultMaxVersions() throws Exception {
        // Create mock cache structure with 5 versions
        Path langDir = tempDir.resolve("testlang");
        for (int i = 1; i <= 5; i++) {
            Path versionDir = langDir.resolve("1." + i + ".0").resolve("linux-x86_64");
            Files.createDirectories(versionDir);
            Files.writeString(versionDir.resolve("test.so"), "lib" + i);
        }

        int removed = grammarManager.cleanupOldVersions("testlang");

        // Should keep 3 most recent (DEFAULT_MAX_VERSIONS_PER_LANGUAGE), remove 2 oldest
        assertThat(removed).isEqualTo(2);
        java.util.List<String> remainingVersions = grammarManager.getCachedVersions("testlang");
        assertThat(remainingVersions).hasSize(3);
        // Should keep the 3 highest versions: 1.5.0, 1.4.0, 1.3.0
        assertThat(remainingVersions).containsExactly("1.5.0", "1.4.0", "1.3.0");
    }

    @Test
    void cleanupAllOldVersions_defaultOverload_usesDefaultMaxVersions() throws Exception {
        // Create cache structure for multiple languages
        Path lang1Dir = tempDir.resolve("lang1");
        Path lang2Dir = tempDir.resolve("lang2");

        // Lang1: 4 versions
        for (int i = 1; i <= 4; i++) {
            Path versionDir = lang1Dir.resolve("1." + i + ".0").resolve("linux-x86_64");
            Files.createDirectories(versionDir);
            Files.writeString(versionDir.resolve("test.so"), "lib" + i);
        }

        // Lang2: 5 versions
        for (int i = 1; i <= 5; i++) {
            Path versionDir = lang2Dir.resolve("2." + i + ".0").resolve("linux-x86_64");
            Files.createDirectories(versionDir);
            Files.writeString(versionDir.resolve("test.so"), "lib" + i);
        }

        int removed = grammarManager.cleanupAllOldVersions();

        // Should remove 1 from lang1 (4-3) + 2 from lang2 (5-3) = 3 total
        assertThat(removed).isEqualTo(3);

        // Verify both languages have exactly 3 versions remaining
        assertThat(grammarManager.getCachedVersions("lang1")).hasSize(3);
        assertThat(grammarManager.getCachedVersions("lang2")).hasSize(3);
    }

    @Test
    void cacheStats_toString_formatsCorrectly() throws Exception {
        // Create mock cache structure
        Path langDir = tempDir.resolve("testlang");
        Path versionDir = langDir.resolve("1.0.0").resolve("linux-x86_64");
        Files.createDirectories(versionDir);

        // Create library file
        Path libFile = versionDir.resolve("test.so");
        Files.writeString(libFile, "test library content 12345"); // 25 chars

        // Create metadata file
        Path metadataFile = versionDir.resolve("metadata.json");
        Files.writeString(metadataFile, "{}");

        GrammarManager.CacheStats stats = grammarManager.getCacheStats();
        String toString = stats.toString();

        // Verify the toString format
        assertThat(toString).contains("CacheStats{");
        assertThat(toString).contains("languages=1");
        assertThat(toString).contains("versions=1");
        assertThat(toString).contains("files=");
        assertThat(toString).contains("libs=");
        assertThat(toString).contains("meta=");
        assertThat(toString).contains("size=");
        assertThat(toString).contains("bytes");
        assertThat(toString).contains("libs=");
        assertThat(toString).contains("bytes");
    }

    @Test
    void getVersionInfo_withNullVersion_returnsLatestVersion() throws Exception {
        // Get the actual platform name used by the system
        java.lang.reflect.Method platformMethod = GrammarManager.class.getDeclaredMethod("platformName");
        platformMethod.setAccessible(true);
        String platform = (String) platformMethod.invoke(grammarManager);

        // Create mock cache structure with multiple versions using the correct platform
        Path langDir = tempDir.resolve("testlang");
        Path version1Dir = langDir.resolve("1.0.0").resolve(platform);
        Path version2Dir = langDir.resolve("1.1.0").resolve(platform);
        Path version3Dir = langDir.resolve("2.0.0").resolve(platform);

        Files.createDirectories(version1Dir);
        Files.createDirectories(version2Dir);
        Files.createDirectories(version3Dir);

        // Create metadata files
        createMockMetadataFile(version1Dir, "1.0.0");
        createMockMetadataFile(version2Dir, "1.1.0");
        createMockMetadataFile(version3Dir, "2.0.0");

        java.util.Optional<GrammarManager.GrammarVersionMetadata> result = grammarManager.getVersionInfo("testlang", null);

        assertThat(result).isPresent();
        assertThat(result.get().version()).isEqualTo("2.0.0"); // Should return latest version
    }

    @Test
    void getVersionInfo_withNullVersion_returnsEmptyWhenNoVersionsExist() {
        java.util.Optional<GrammarManager.GrammarVersionMetadata> result = grammarManager.getVersionInfo("nonexistent", null);

        assertThat(result).isEmpty();
    }

    @Test
    void getVersionInfo_withSpecificVersion_returnsCorrectVersion() throws Exception {
        // Get the actual platform name used by the system
        java.lang.reflect.Method platformMethod = GrammarManager.class.getDeclaredMethod("platformName");
        platformMethod.setAccessible(true);
        String platform = (String) platformMethod.invoke(grammarManager);

        // Create mock cache structure
        Path langDir = tempDir.resolve("testlang");
        Path versionDir = langDir.resolve("1.5.0").resolve(platform);
        Files.createDirectories(versionDir);

        // Create metadata file
        createMockMetadataFile(versionDir, "1.5.0");

        java.util.Optional<GrammarManager.GrammarVersionMetadata> result = grammarManager.getVersionInfo("testlang", "1.5.0");

        assertThat(result).isPresent();
        assertThat(result.get().version()).isEqualTo("1.5.0");
    }

    private void createMockMetadataFile(Path versionDir, String version) throws Exception {
        // Get the actual platform name
        java.lang.reflect.Method platformMethod = GrammarManager.class.getDeclaredMethod("platformName");
        platformMethod.setAccessible(true);
        String platform = (String) platformMethod.invoke(grammarManager);

        GrammarManager.GrammarVersionMetadata metadata = new GrammarManager.GrammarVersionMetadata(
                "testlang",
                version,
                "tree-sitter-testlang",
                java.time.Instant.now(),
                platform,
                1024L
        );

        Path metadataPath = versionDir.resolve("metadata.json");
        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        mapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
        mapper.writeValue(metadataPath.toFile(), metadata);
    }
}
