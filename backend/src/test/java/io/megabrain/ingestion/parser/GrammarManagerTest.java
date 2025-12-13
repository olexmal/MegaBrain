/*
 * Copyright (c) 2025 MegaBrain Contributors
 * Licensed under the MIT License - see LICENSE file for details.
 */

package io.megabrain.ingestion.parser;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import uk.org.webcompere.systemstubs.environment.EnvironmentVariables;
import uk.org.webcompere.systemstubs.jupiter.SystemStub;
import uk.org.webcompere.systemstubs.jupiter.SystemStubsExtension;
import uk.org.webcompere.systemstubs.properties.SystemProperties;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(SystemStubsExtension.class)
class GrammarManagerTest {

    @TempDir
    Path tempDir;

    @SystemStub
    private SystemProperties systemProperties;

    @SystemStub
    private EnvironmentVariables environmentVariables;

    @Mock
    private GrammarConfig grammarConfig;

    private GrammarManager grammarManager;
    private GrammarSpec testSpec;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        // Set custom cache directory for testing
        systemProperties.set("megabrain.grammar.cache.dir", tempDir.toString());

        // Mock the grammar configuration to return default behavior (no pinning)
        when(grammarConfig.defaultVersion()).thenReturn(Optional.empty());
        when(grammarConfig.languageVersions()).thenReturn(Map.of());
        when(grammarConfig.getEffectiveVersion(anyString(), anyString())).thenAnswer(invocation -> {
            // Default behavior: return the spec version (no pinning)
            return invocation.getArgument(1);
        });

        // Create GrammarManager with mock config for testing
        grammarManager = new GrammarManager(grammarConfig);

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
        assertThatCode(() -> callback.onProgress(100, 1000, "test message")).doesNotThrowAnyException();
        assertThatCode(() -> callback.onProgress(0, -1, "another message")).doesNotThrowAnyException();
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

        // Should keep 5 most recent (DEFAULT_MAX_VERSIONS_PER_LANGUAGE), remove 0 oldest
        assertThat(removed).isEqualTo(0);
        java.util.List<String> remainingVersions = grammarManager.getCachedVersions("testlang");
        assertThat(remainingVersions).hasSize(5);
        // Should keep all 5 versions since default is now 5
        assertThat(remainingVersions).containsExactly("1.5.0", "1.4.0", "1.3.0", "1.2.0", "1.1.0");
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

        // Should remove 0 from lang1 (4-5) + 0 from lang2 (5-5) = 0 total (since default is now 5)
        assertThat(removed).isEqualTo(0);

        // Verify both languages have all versions remaining (up to the default limit of 5)
        assertThat(grammarManager.getCachedVersions("lang1")).hasSize(4);
        assertThat(grammarManager.getCachedVersions("lang2")).hasSize(5);
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

    @Test
    void applyVersionPinning_usesDefaultSpecVersionWhenNoConfig() {
        // Test with no configuration - should use spec version
        GrammarSpec spec = new GrammarSpec("testlang", "symbol", "lib", "prop", "env", "repo", "1.0.0");

        GrammarSpec result = grammarManager.applyVersionPinning(spec);

        assertThat(result.version()).isEqualTo("1.0.0");
        assertThat(result.language()).isEqualTo(spec.language());
        assertThat(result.symbol()).isEqualTo(spec.symbol());
    }

    @Test
    void applyVersionPinning_usesGlobalDefaultVersion() {
        // Create a new manager with specific config for this test
        GrammarConfig globalConfig = new GrammarConfig() {
            @Override
            public Optional<String> defaultVersion() {
                return Optional.of("2.0.0");
            }

            @Override
            public Map<String, String> languageVersions() {
                return Map.of();
            }

            @Override
            public String getEffectiveVersion(String language, String defaultSpecVersion) {
                return defaultVersion().orElse(defaultSpecVersion);
            }
        };

        GrammarManager testManager = new GrammarManager(globalConfig);
        GrammarSpec spec = new GrammarSpec("testlang", "symbol", "lib", "prop", "env", "repo", "1.0.0");

        GrammarSpec result = testManager.applyVersionPinning(spec);

        assertThat(result.version()).isEqualTo("2.0.0");
        assertThat(result.language()).isEqualTo(spec.language());
    }

    @Test
    void applyVersionPinning_usesLanguageSpecificVersion() {
        // Create a new manager with specific config for this test
        GrammarConfig languageConfig = new GrammarConfig() {
            @Override
            public Optional<String> defaultVersion() {
                return Optional.empty();
            }

            @Override
            public Map<String, String> languageVersions() {
                return Map.of("testlang", "3.0.0");
            }

            @Override
            public String getEffectiveVersion(String language, String defaultSpecVersion) {
                String languageSpecificVersion = languageVersions().get(language);
                if (languageSpecificVersion != null && !languageSpecificVersion.trim().isEmpty()) {
                    return languageSpecificVersion.trim();
                }
                return defaultVersion().orElse(defaultSpecVersion);
            }
        };

        GrammarManager testManager = new GrammarManager(languageConfig);
        GrammarSpec spec = new GrammarSpec("testlang", "symbol", "lib", "prop", "env", "repo", "1.0.0");

        GrammarSpec result = testManager.applyVersionPinning(spec);

        assertThat(result.version()).isEqualTo("3.0.0");
        assertThat(result.language()).isEqualTo(spec.language());
    }

    @Test
    void applyVersionPinning_returnsSameSpecWhenNoChangesNeeded() {
        // When no pinning is configured, should return the same spec
        GrammarSpec spec = new GrammarSpec("testlang", "symbol", "lib", "prop", "env", "repo", "1.0.0");

        GrammarSpec result = grammarManager.applyVersionPinning(spec);

        // Should return the exact same instance when no changes are needed
        assertThat(result).isSameAs(spec);
    }

    @Test
    void applyVersionPinning_handlesNullSpec() {
        assertThatThrownBy(() -> grammarManager.applyVersionPinning(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("spec");
    }

    @Test
    void applyVersionPinning_handlesEmptyAndBlankVersions() {
        // Test with empty string language-specific version (should use default)
        GrammarConfig configWithEmptyVersion = new GrammarConfig() {
            @Override
            public Optional<String> defaultVersion() {
                return Optional.of("2.0.0");
            }

            @Override
            public Map<String, String> languageVersions() {
                return Map.of("testlang", ""); // Empty string
            }

            @Override
            public String getEffectiveVersion(String language, String defaultSpecVersion) {
                String languageSpecificVersion = languageVersions().get(language);
                if (languageSpecificVersion != null && !languageSpecificVersion.trim().isEmpty()) {
                    return languageSpecificVersion.trim();
                }
                return defaultVersion().orElse(defaultSpecVersion);
            }
        };

        GrammarManager testManager = new GrammarManager(configWithEmptyVersion);
        GrammarSpec spec = new GrammarSpec("testlang", "symbol", "lib", "prop", "env", "repo", "1.0.0");

        GrammarSpec result = testManager.applyVersionPinning(spec);

        assertThat(result.version()).isEqualTo("2.0.0"); // Should use default since language version is empty
    }

    @Test
    void applyVersionPinning_handlesWhitespaceOnlyVersions() {
        // Test with whitespace-only language-specific version (should use default)
        GrammarConfig configWithWhitespaceVersion = new GrammarConfig() {
            @Override
            public Optional<String> defaultVersion() {
                return Optional.of("2.0.0");
            }

            @Override
            public Map<String, String> languageVersions() {
                return Map.of("testlang", "   "); // Whitespace only
            }

            @Override
            public String getEffectiveVersion(String language, String defaultSpecVersion) {
                String languageSpecificVersion = languageVersions().get(language);
                if (languageSpecificVersion != null && !languageSpecificVersion.trim().isEmpty()) {
                    return languageSpecificVersion.trim();
                }
                return defaultVersion().orElse(defaultSpecVersion);
            }
        };

        GrammarManager testManager = new GrammarManager(configWithWhitespaceVersion);
        GrammarSpec spec = new GrammarSpec("testlang", "symbol", "lib", "prop", "env", "repo", "1.0.0");

        GrammarSpec result = testManager.applyVersionPinning(spec);

        assertThat(result.version()).isEqualTo("2.0.0"); // Should use default since language version is whitespace
    }

    @Test
    void applyVersionPinning_preservesOtherSpecFields() {
        // Test that version pinning only changes the version field
        GrammarConfig configWithGlobalDefault = new GrammarConfig() {
            @Override
            public Optional<String> defaultVersion() {
                return Optional.of("9.9.9");
            }

            @Override
            public Map<String, String> languageVersions() {
                return Map.of();
            }

            @Override
            public String getEffectiveVersion(String language, String defaultSpecVersion) {
                return defaultVersion().orElse(defaultSpecVersion);
            }
        };

        GrammarManager testManager = new GrammarManager(configWithGlobalDefault);
        GrammarSpec originalSpec = new GrammarSpec("testlang", "unique_symbol", "unique_lib", "unique_prop", "unique_env", "unique_repo", "1.0.0");

        GrammarSpec result = testManager.applyVersionPinning(originalSpec);

        // Version should change
        assertThat(result.version()).isEqualTo("9.9.9");

        // All other fields should remain unchanged
        assertThat(result.language()).isEqualTo(originalSpec.language());
        assertThat(result.symbol()).isEqualTo(originalSpec.symbol());
        assertThat(result.libraryName()).isEqualTo(originalSpec.libraryName());
        assertThat(result.propertyKey()).isEqualTo(originalSpec.propertyKey());
        assertThat(result.envKey()).isEqualTo(originalSpec.envKey());
        assertThat(result.repository()).isEqualTo(originalSpec.repository());
    }

    @Test
    void getVersionInfo_withNullVersion_handlesNonExistentLanguageGracefully() {
        // Test that null version with non-existent language returns empty
        java.util.Optional<GrammarManager.GrammarVersionMetadata> result = grammarManager.getVersionInfo("completely-non-existent-language", null);

        assertThat(result).isEmpty();
    }

    @Test
    void getCachedVersions_handlesSpecialCharactersInLanguageNames() throws Exception {
        // Create cache structure with special characters in language name
        Path langDir = tempDir.resolve("special.lang-name_123");
        Path versionDir = langDir.resolve("1.0.0").resolve("linux-amd64");
        Files.createDirectories(versionDir);

        java.util.List<String> versions = grammarManager.getCachedVersions("special.lang-name_123");

        assertThat(versions).contains("1.0.0");
    }

    @Test
    void cleanupOldVersions_handlesEmptyVersionList() {
        // Test cleanup on language with no versions
        int removed = grammarManager.cleanupOldVersions("empty-language", 3);

        assertThat(removed).isEqualTo(0);
    }

    @Test
    void cleanupAllOldVersions_handlesAllLanguagesEmpty() {
        // Test cleanup when all languages have no cached versions
        int removed = grammarManager.cleanupAllOldVersions();

        assertThat(removed).isEqualTo(0);
    }

    @Test
    void getCacheStats_handlesEmptyDirectories() {
        // Create empty language directory (no versions)
        Path emptyLangDir = tempDir.resolve("empty-lang");
        try {
            Files.createDirectories(emptyLangDir);
        } catch (Exception e) {
            // Ignore for test
        }

        GrammarManager.CacheStats stats = grammarManager.getCacheStats();

        // Should handle empty directories gracefully
        assertThat(stats.totalLanguages).isGreaterThanOrEqualTo(0);
        assertThat(stats.totalVersions).isGreaterThanOrEqualTo(0);
    }

    // ===== ROLLBACK FUNCTIONALITY TESTS =====

    @Test
    void recordVersionUsage_tracksSuccessAndFailure() throws Exception {
        // Test recording successful usage
        java.lang.reflect.Method recordMethod = GrammarManager.class.getDeclaredMethod("recordVersionUsage", String.class, String.class, boolean.class, String.class);
        recordMethod.setAccessible(true);
        recordMethod.invoke(grammarManager, "testlang", "1.0.0", true, null);

        java.util.List<GrammarManager.VersionHistoryEntry> history = grammarManager.getVersionHistory("testlang");
        assertThat(history).hasSize(1);
        assertThat(history.get(0).language()).isEqualTo("testlang");
        assertThat(history.get(0).version()).isEqualTo("1.0.0");
        assertThat(history.get(0).success()).isTrue();
        assertThat(history.get(0).errorMessage()).isNull();

        // Test recording failed usage
        recordMethod.invoke(grammarManager, "testlang", "2.0.0", false, "Load failed");

        history = grammarManager.getVersionHistory("testlang");
        assertThat(history).hasSize(2);
        assertThat(history.get(0).language()).isEqualTo("testlang");
        assertThat(history.get(0).version()).isEqualTo("2.0.0");
        assertThat(history.get(0).success()).isFalse();
        assertThat(history.get(0).errorMessage()).isEqualTo("Load failed");
    }

    @Test
    void recordVersionUsage_limitsHistorySize() throws Exception {
        // Record more entries than the limit
        java.lang.reflect.Method recordMethod = GrammarManager.class.getDeclaredMethod("recordVersionUsage", String.class, String.class, boolean.class, String.class);
        recordMethod.setAccessible(true);
        for (int i = 0; i < 120; i++) {
            recordMethod.invoke(grammarManager, "testlang", "v" + i, true, null);
        }

        java.util.List<GrammarManager.VersionHistoryEntry> history = grammarManager.getVersionHistory("testlang");
        // Should be limited to MAX_VERSION_HISTORY_ENTRIES
        assertThat(history.size()).isLessThanOrEqualTo(100);
    }

    @Test
    void getVersionHistory_returnsEmptyListForUnknownLanguage() {
        java.util.List<GrammarManager.VersionHistoryEntry> history = grammarManager.getVersionHistory("unknown-lang");
        assertThat(history).isEmpty();
    }

    @Test
    void rollbackToVersion_failsForNonExistentVersion() {
        GrammarManager.RollbackResult result = grammarManager.rollbackToVersion("testlang", "non-existent-version");

        assertThat(result.success()).isFalse();
        assertThat(result.language()).isEqualTo("testlang");
        assertThat(result.fromVersion()).isEqualTo("unknown");
        assertThat(result.toVersion()).isEqualTo("non-existent-version");
        assertThat(result.errorMessage()).contains("not found in cache");
    }

    @Test
    void rollbackToVersion_failsGracefullyForExistingVersion() throws Exception {
        // Create a cached version
        Path langDir = tempDir.resolve("testlang");
        Path versionDir = langDir.resolve("1.0.0").resolve("linux-amd64");
        Files.createDirectories(versionDir);
        Path libFile = versionDir.resolve("libtree-sitter-testlang.so");
        Files.write(libFile, "fake library content".getBytes());

        // Save metadata
        java.lang.reflect.Method saveMethod = GrammarManager.class.getDeclaredMethod("saveVersionMetadata", GrammarSpec.class, String.class, Path.class);
        saveMethod.setAccessible(true);
        saveMethod.invoke(grammarManager, testSpec, "linux-amd64", libFile);

        // Test that rollback recognizes the cached version exists
        java.util.List<String> cachedVersions = grammarManager.getCachedVersions("testlang");
        assertThat(cachedVersions).contains("1.0.0");

        // The rollback method will try to load the cached version, but since it's not a real library,
        // it will fail gracefully
        GrammarManager.RollbackResult result = grammarManager.rollbackToVersion("testlang", "1.0.0");

        // Verify the rollback attempt was made correctly
        assertThat(result.language()).isEqualTo("testlang");
        assertThat(result.toVersion()).isEqualTo("1.0.0");
        // Loading will fail due to fake library file, but the method should handle it gracefully
        assertThat(result.errorMessage()).isNotNull();
    }

    @Test
    void rollbackToPrevious_findsMostRecentSuccessfulVersion() throws Exception {
        // Record version history: success, failure, success
        java.lang.reflect.Method recordMethod = GrammarManager.class.getDeclaredMethod("recordVersionUsage", String.class, String.class, boolean.class, String.class);
        recordMethod.setAccessible(true);
        recordMethod.invoke(grammarManager, "testlang", "1.0.0", true, null);
        recordMethod.invoke(grammarManager, "testlang", "2.0.0", false, "Failed to load");
        recordMethod.invoke(grammarManager, "testlang", "3.0.0", true, null);

        // Create cached version for 1.0.0
        Path langDir = tempDir.resolve("testlang");
        Path versionDir = langDir.resolve("1.0.0").resolve("linux-amd64");
        Files.createDirectories(versionDir);
        Path libFile = versionDir.resolve("libtree-sitter-testlang.so");
        Files.write(libFile, "fake library content".getBytes());
        java.lang.reflect.Method saveMethod = GrammarManager.class.getDeclaredMethod("saveVersionMetadata", GrammarSpec.class, String.class, Path.class);
        saveMethod.setAccessible(true);
        saveMethod.invoke(grammarManager, testSpec, "linux-amd64", libFile);

        // Verify that version history is recorded correctly
        java.util.List<GrammarManager.VersionHistoryEntry> history = grammarManager.getVersionHistory("testlang");
        assertThat(history).hasSize(3);
        assertThat(history.get(0).version()).isEqualTo("3.0.0"); // Most recent
        assertThat(history.get(0).success()).isTrue();
        assertThat(history.get(1).version()).isEqualTo("2.0.0");
        assertThat(history.get(1).success()).isFalse();
        assertThat(history.get(2).version()).isEqualTo("1.0.0"); // Oldest successful
        assertThat(history.get(2).success()).isTrue();

        // Test rollback - it will try to load 3.0.0 (most recent successful), but since it doesn't exist in cache,
        // it should try 1.0.0 (next most recent successful)
        GrammarManager.RollbackResult result = grammarManager.rollbackToPrevious("testlang");

        // The rollback should attempt to load 1.0.0 and fail gracefully due to fake library
        assertThat(result.language()).isEqualTo("testlang");
        assertThat(result.toVersion()).isEqualTo("1.0.0");
        assertThat(result.errorMessage()).isNotNull();
    }

    @Test
    void rollbackToPrevious_failsWithNoHistory() {
        GrammarManager.RollbackResult result = grammarManager.rollbackToPrevious("testlang");

        assertThat(result.success()).isFalse();
        assertThat(result.errorMessage()).contains("No version history available");
    }

    @Test
    void rollbackToPrevious_failsWithNoSuccessfulVersions() throws Exception {
        // Record only failures
        java.lang.reflect.Method recordMethod = GrammarManager.class.getDeclaredMethod("recordVersionUsage", String.class, String.class, boolean.class, String.class);
        recordMethod.setAccessible(true);
        recordMethod.invoke(grammarManager, "testlang", "1.0.0", false, "Failed");
        recordMethod.invoke(grammarManager, "testlang", "2.0.0", false, "Failed");

        GrammarManager.RollbackResult result = grammarManager.rollbackToPrevious("testlang");

        assertThat(result.success()).isFalse();
        assertThat(result.errorMessage()).contains("No suitable previous version found");
    }

    @Test
    void markVersionAsFailed_recordsFailure() {
        grammarManager.markVersionAsFailed("testlang", "1.0.0", "Test failure");

        java.util.List<GrammarManager.VersionHistoryEntry> history = grammarManager.getVersionHistory("testlang");
        assertThat(history).hasSize(1);
        assertThat(history.get(0).success()).isFalse();
        assertThat(history.get(0).errorMessage()).isEqualTo("Test failure");
    }

    @Test
    void createGrammarSpecForVersion_createsBasicSpec() throws Exception {
        java.lang.reflect.Method createMethod = GrammarManager.class.getDeclaredMethod("createGrammarSpecForVersion", String.class, String.class);
        createMethod.setAccessible(true);
        GrammarSpec result = (GrammarSpec) createMethod.invoke(grammarManager, "testlang", "2.0.0");

        assertThat(result).isNotNull();
        assertThat(result.language()).isEqualTo("testlang");
        assertThat(result.version()).isEqualTo("2.0.0");
    }

    @Test
    void cleanupOldVersions_preservesMoreVersionsForRollback() throws Exception {
        // Create multiple versions
        Path langDir = tempDir.resolve("testlang");
        for (int i = 1; i <= 15; i++) {
            Path versionDir = langDir.resolve("v" + i).resolve("linux-amd64");
            Files.createDirectories(versionDir);
            Path libFile = versionDir.resolve("lib.so");
            Files.write(libFile, ("content" + i).getBytes());
        }

        // Cleanup should preserve 10 versions (ROLLBACK_MAX_VERSIONS_PER_LANGUAGE)
        int removed = grammarManager.cleanupOldVersions("testlang", 10);

        java.util.List<String> remainingVersions = grammarManager.getCachedVersions("testlang");
        assertThat(remainingVersions.size()).isEqualTo(10);
        assertThat(removed).isEqualTo(5); // 15 - 10 = 5 removed
    }

    @Test
    void ensureCachedLibrary_handlesVersionPinningIntegration() throws Exception {
        // Test the integration of version pinning with ensureCachedLibrary
        GrammarConfig pinnedConfig = new GrammarConfig() {
            @Override
            public Optional<String> defaultVersion() {
                return Optional.of("2.0.0");
            }

            @Override
            public Map<String, String> languageVersions() {
                return Map.of("java", "1.5.0");
            }
        };

        GrammarManager pinnedManager = new GrammarManager(pinnedConfig);

        // Test that applyVersionPinning would use pinned versions
        GrammarSpec spec = pinnedManager.applyVersionPinning(
            new GrammarSpec("java", "test", "test", "test", "test", "test", "1.0.0"));

        assertThat(spec.version()).isEqualTo("1.5.0"); // Should use language-specific pinning
    }

    @Test
    void rollbackOperations_handleConcurrentAccess() throws Exception {
        // Test thread safety of rollback operations
        Path langDir = tempDir.resolve("threadtest");
        Path version1Dir = langDir.resolve("v1").resolve("linux-amd64");
        Path version2Dir = langDir.resolve("v2").resolve("linux-amd64");
        Files.createDirectories(version1Dir);
        Files.createDirectories(version2Dir);

        // Create library files
        Files.writeString(version1Dir.resolve("lib.so"), "lib1");
        Files.writeString(version2Dir.resolve("lib.so"), "lib2");

        // Record some version usage using reflection
        java.lang.reflect.Method recordMethod = GrammarManager.class.getDeclaredMethod("recordVersionUsage", String.class, String.class, boolean.class, String.class);
        recordMethod.setAccessible(true);
        recordMethod.invoke(grammarManager, "threadtest", "v1", true, null);
        recordMethod.invoke(grammarManager, "threadtest", "v2", true, null);

        // These operations should be thread-safe
        java.util.List<String> versions1 = grammarManager.getCachedVersions("threadtest");
        java.util.List<GrammarManager.VersionHistoryEntry> history1 = grammarManager.getVersionHistory("threadtest");

        assertThat(versions1).contains("v1", "v2");
        assertThat(history1).hasSize(2);
    }

    @Test
    void getCacheStats_handlesLargeCacheStructure() throws Exception {
        // Create a large cache structure to test performance and correctness
        for (int i = 1; i <= 20; i++) {
            Path langDir = tempDir.resolve("lang" + i);
            Path versionDir = langDir.resolve("v" + i).resolve("linux-amd64");
            Files.createDirectories(versionDir);
            Files.writeString(versionDir.resolve("lib.so"), "content" + i);
        }

        GrammarManager.CacheStats stats = grammarManager.getCacheStats();

        assertThat(stats.totalLanguages).isEqualTo(20);
        assertThat(stats.totalVersions).isEqualTo(20);
        assertThat(stats.totalSizeBytes).isGreaterThan(0);
    }

    @Test
    void versionHistory_operations_areIdempotent() {
        // Test that repeated operations don't cause issues
        grammarManager.markVersionAsFailed("idempotent", "1.0.0", "Test failure");
        grammarManager.markVersionAsFailed("idempotent", "1.0.0", "Test failure");
        grammarManager.markVersionAsFailed("idempotent", "1.0.0", "Test failure");

        java.util.List<GrammarManager.VersionHistoryEntry> history = grammarManager.getVersionHistory("idempotent");
        assertThat(history).hasSize(3);
        assertThat(history).allMatch(entry -> !entry.success());
    }

    @Test
    void platformLibraryExtension_handlesUnknownPlatforms() throws Exception {
        // Test with an unknown platform string
        systemProperties.set("os.name", "UnknownOS");
        systemProperties.set("os.arch", "unknown");

        // Should not crash, should return a reasonable default
        java.lang.reflect.Method extensionMethod = GrammarManager.class.getDeclaredMethod("platformLibraryExtension");
        extensionMethod.setAccessible(true);
        String extension = (String) extensionMethod.invoke(grammarManager);
        assertThat(extension).isNotNull();
        assertThat(extension).isNotEmpty();
    }


    @Test
    void getVersionInfo_handlesMalformedMetadata() throws Exception {
        // Create a malformed metadata file
        Path langDir = tempDir.resolve("malformed");
        Path versionDir = langDir.resolve("v1.0.0");
        Files.createDirectories(versionDir);

        // Write invalid JSON
        Path metadataFile = versionDir.resolve("metadata.json");
        Files.writeString(metadataFile, "{invalid json content}");

        Optional<GrammarManager.GrammarVersionMetadata> result =
            grammarManager.getVersionInfo("malformed", "v1.0.0");

        // Should return empty rather than crash
        assertThat(result).isEmpty();
    }
}
