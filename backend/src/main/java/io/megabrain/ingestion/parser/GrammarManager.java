/*
 * Copyright (c) 2025 MegaBrain Contributors
 * Licensed under the MIT License - see LICENSE file for details.
 */

package io.megabrain.ingestion.parser;

import java.io.IOException;
import java.io.InputStream;
import java.lang.foreign.SymbolLookup;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.HexFormat;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.stream.Stream;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.jboss.logging.Logger;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import io.github.treesitter.jtreesitter.Language;

/**
 * Manages Tree-sitter grammar loading and native library lifecycle.
 * Caches loaded grammars and libraries to avoid repeated loads.
 * Tracks grammar versions and provides version management.
 * Supports version pinning through configuration.
 */
@ApplicationScoped
public class GrammarManager {

    private static final String LANGUAGE = "language";
    private static final String VERSION = "version";
    private static final String UNKNOWN = "unknown";
    private final GrammarConfig grammarConfig;

    /**
     * Default constructor for non-CDI usage (e.g., from parsers).
     * Creates a GrammarManager with default configuration.
     */
    public GrammarManager() {
        this(new DefaultGrammarConfig());
    }

    @Inject
    public GrammarManager(GrammarConfig grammarConfig) {
        this.grammarConfig = grammarConfig;
    }

    /**
     * Default GrammarConfig implementation for non-CDI contexts.
     */
    private static class DefaultGrammarConfig implements GrammarConfig {
        @Override
        public Optional<String> defaultVersion() {
            return Optional.empty();
        }

        @Override
        public Map<String, String> languageVersions() {
            return Map.of();
        }
    }

    /**
     * Metadata for cached grammar versions.
     */
    public record GrammarVersionMetadata(
            String language,
            String version,
            String repository,
            Instant downloadedAt,
            String platform,
            long fileSize
    ) {
        public GrammarVersionMetadata {
            Objects.requireNonNull(language, LANGUAGE);
            Objects.requireNonNull(version, VERSION);
            Objects.requireNonNull(repository, "repository");
            Objects.requireNonNull(downloadedAt, "downloadedAt");
            Objects.requireNonNull(platform, "platform");
        }
    }

    /**
     * Version history entry for rollback tracking.
     */
    public record VersionHistoryEntry(
            String language,
            String version,
            Instant usedAt,
            boolean success,
            String errorMessage
    ) {
        public VersionHistoryEntry {
            Objects.requireNonNull(language, LANGUAGE);
            Objects.requireNonNull(version, VERSION);
            Objects.requireNonNull(usedAt, "usedAt");
        }
    }

    /**
     * Rollback result information.
     */
    public record RollbackResult(
            String language,
            String fromVersion,
            String toVersion,
            boolean success,
            String errorMessage
    ) {
        public RollbackResult {
            Objects.requireNonNull(language, LANGUAGE);
            Objects.requireNonNull(fromVersion, "fromVersion");
            Objects.requireNonNull(toVersion, "toVersion");
        }
    }

    /**
     * Interface for tracking download progress.
     */
    @FunctionalInterface
    public interface DownloadProgressCallback {
        void onProgress(long bytesDownloaded, long totalBytes, String message);
    }

    /**
     * No-op progress callback for when progress tracking is not needed.
     */
    public static final DownloadProgressCallback NO_PROGRESS = (downloaded, total, message) -> { /* no-op */ };

    private static final Logger LOG = Logger.getLogger(GrammarManager.class);
    private static final String CACHE_DIR_PROPERTY = "megabrain.grammar.cache.dir";
    private static final String CACHE_DIR_ENV = "MEGABRAIN_GRAMMAR_CACHE_DIR";
    private static final Duration HTTP_TIMEOUT = Duration.ofSeconds(20);
    private static final int MAX_DOWNLOAD_RETRIES = 3;
    private static final Duration INITIAL_RETRY_DELAY = Duration.ofSeconds(1);
    private static final int DEFAULT_MAX_VERSIONS_PER_LANGUAGE = 5; // Increased for rollback capability
    private static final int ROLLBACK_MAX_VERSIONS_PER_LANGUAGE = 10; // Keep more for rollback
    private static final int MAX_VERSION_HISTORY_ENTRIES = 100; // Limit history size
    private static final ObjectMapper OBJECT_MAPPER = JsonMapper.builder()
            .addModule(new JavaTimeModule())
            .build();

    private final Map<String, Language> loadedLanguages = new ConcurrentHashMap<>();
    private final Map<String, Boolean> nativeLoaded = new ConcurrentHashMap<>();
    private final Map<String, GrammarVersionMetadata> versionCache = new ConcurrentHashMap<>();
    private final Map<String, Deque<VersionHistoryEntry>> versionHistory = new ConcurrentHashMap<>();
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(HTTP_TIMEOUT)
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    /**
     * Apply version pinning configuration to a grammar spec.
     * Returns a new GrammarSpec with the effective version based on configuration.
     *
     * @param spec the original grammar spec
     * @return grammar spec with version pinning applied
     */
    public GrammarSpec applyVersionPinning(GrammarSpec spec) {
        Objects.requireNonNull(spec, "spec");

        String effectiveVersion = grammarConfig.getEffectiveVersion(spec.language(), spec.version());
        if (effectiveVersion.equals(spec.version())) {
            // No change needed
            return spec;
        }

        LOG.debugf("Applied version pinning for %s: %s -> %s", spec.language(), spec.version(), effectiveVersion);
        return new GrammarSpec(
                spec.language(),
                spec.symbol(),
                spec.libraryName(),
                spec.propertyKey(),
                spec.envKey(),
                spec.repository(),
                effectiveVersion
        );
    }

    /**
     * Load (or return cached) Tree-sitter language for the given spec.
     * Applies version pinning configuration before loading.
     *
     * @param spec grammar spec metadata
     * @return loaded Language or null if loading failed
     */
    public Language loadLanguage(GrammarSpec spec) {
        long startTime = System.nanoTime();

        try {
            // Apply version pinning first
            GrammarSpec pinnedSpec = applyVersionPinning(spec);

            Objects.requireNonNull(pinnedSpec, "pinnedSpec");
            Language cached = loadedLanguages.get(pinnedSpec.symbol());
            if (cached != null) {
                // Record successful usage of cached language
                recordVersionUsage(pinnedSpec.language(), pinnedSpec.version(), true, null);
                return cached;
            }

            // Record attempt to load this version
            recordVersionUsage(pinnedSpec.language(), pinnedSpec.version(), false, null);

            if (!ensureNativeLibraryLoaded(pinnedSpec)) {
                recordVersionUsage(pinnedSpec.language(), pinnedSpec.version(), false, "Failed to load native library");
                return null;
            }
            return loadLanguageFromNative(pinnedSpec, startTime);
        } catch (Exception e) {
            long loadTimeMs = (System.nanoTime() - startTime) / 1_000_000;
            LOG.errorf(e, "Unexpected error loading grammar for %s after %d ms", spec.language(), loadTimeMs);
            throw e;
        }
    }

    private Language loadLanguageFromNative(GrammarSpec pinnedSpec, long startTime) {
        try {
            Language lang = Language.load(SymbolLookup.loaderLookup(), pinnedSpec.symbol());
            loadedLanguages.put(pinnedSpec.symbol(), lang);
            recordVersionUsage(pinnedSpec.language(), pinnedSpec.version(), true, null);

            long loadTimeMs = (System.nanoTime() - startTime) / 1_000_000;
            LOG.infof("Loaded Tree-sitter language %s via symbol %s in %d ms", pinnedSpec.language(), pinnedSpec.symbol(), loadTimeMs);

            if (loadTimeMs > 500) {
                LOG.warnf("Grammar loading for %s exceeded 500ms threshold: %d ms (AC5 violation)", pinnedSpec.language(), loadTimeMs);
            }

            return lang;
        } catch (Exception | UnsatisfiedLinkError e) {
            long loadTimeMs = (System.nanoTime() - startTime) / 1_000_000;
            LOG.errorf(e, "Failed to load grammar for %s via symbol %s after %d ms", pinnedSpec.language(), pinnedSpec.symbol(), loadTimeMs);
            recordVersionUsage(pinnedSpec.language(), pinnedSpec.version(), false, e.getMessage());
            return tryAutomaticRollback(pinnedSpec);
        }
    }

    /**
     * Record version usage in history for rollback tracking.
     */
    private void recordVersionUsage(String language, String version, boolean success, String errorMessage) {
        Objects.requireNonNull(language, LANGUAGE);
        Objects.requireNonNull(version, VERSION);

        VersionHistoryEntry entry = new VersionHistoryEntry(
                language,
                version,
                Instant.now(),
                success,
                errorMessage
        );

        versionHistory.computeIfAbsent(language, k -> new LinkedList<>()).addFirst(entry);

        // Limit history size to prevent memory leaks
        Deque<VersionHistoryEntry> history = versionHistory.get(language);
        while (history.size() > MAX_VERSION_HISTORY_ENTRIES) {
            history.removeLast();
        }

        LOG.debugf("Recorded version usage: %s v%s - %s%s", language, version,
                   success ? "SUCCESS" : "FAILED",
                   errorMessage != null ? " (" + errorMessage + ")" : "");
    }

    /**
     * Attempt automatic rollback to a previous working version when loading fails.
     */
    private Language tryAutomaticRollback(GrammarSpec failedSpec) {
        Objects.requireNonNull(failedSpec, "failedSpec");

        Deque<VersionHistoryEntry> history = versionHistory.get(failedSpec.language());
        if (history == null || history.isEmpty()) {
            LOG.debugf("No version history available for %s, cannot rollback", failedSpec.language());
            return null;
        }

        // Find the most recent successful version that's different from the failed one
        for (VersionHistoryEntry entry : history) {
            if (entry.success() && !entry.version().equals(failedSpec.version())) {
                LOG.infof("Attempting automatic rollback for %s from %s to %s",
                         failedSpec.language(), failedSpec.version(), entry.version());

                // Create a new spec with the rollback version
                GrammarSpec rollbackSpec = new GrammarSpec(
                        failedSpec.language(),
                        failedSpec.symbol(),
                        failedSpec.libraryName(),
                        failedSpec.propertyKey(),
                        failedSpec.envKey(),
                        failedSpec.repository(),
                        entry.version()
                );

                // Try to load with the rollback version
                Language rollbackResult = loadLanguageWithVersion(rollbackSpec, false); // Don't recurse rollback
                if (rollbackResult != null) {
                    LOG.infof("Automatic rollback successful for %s to version %s",
                             failedSpec.language(), entry.version());
                    return rollbackResult;
                } else {
                    LOG.debugf("Automatic rollback failed for %s to version %s",
                              failedSpec.language(), entry.version());
                }
            }
        }

        LOG.debugf("No suitable rollback version found for %s", failedSpec.language());
        return null;
    }

    /**
     * Load language with a specific version, used for rollback operations.
     * Similar to loadLanguage but skips automatic rollback to prevent recursion.
     */
    private Language loadLanguageWithVersion(GrammarSpec spec, boolean allowRollback) {
        Objects.requireNonNull(spec, "spec");

        Language cached = loadedLanguages.get(spec.symbol());
        if (cached != null) {
            if (allowRollback) {
                recordVersionUsage(spec.language(), spec.version(), true, null);
            }
            return cached;
        }

        if (!ensureNativeLibraryLoaded(spec)) {
            if (allowRollback) {
                recordVersionUsage(spec.language(), spec.version(), false, "Failed to load native library");
            }
            return null;
        }

        try {
            Language lang = Language.load(SymbolLookup.loaderLookup(), spec.symbol());
            loadedLanguages.put(spec.symbol(), lang);
            if (allowRollback) {
                recordVersionUsage(spec.language(), spec.version(), true, null);
            }
            LOG.debugf("Loaded Tree-sitter language %s via symbol %s", spec.language(), spec.symbol());
            return lang;
        } catch (Exception | UnsatisfiedLinkError e) {
            LOG.errorf(e, "Failed to load grammar for %s via symbol %s", spec.language(), spec.symbol());
            if (allowRollback) {
                recordVersionUsage(spec.language(), spec.version(), false, e.getMessage());
                return tryAutomaticRollback(spec);
            }
            return null;
        }
    }

    /**
     * Provides a supplier suitable for TreeSitterParser constructors.
     */
    public Supplier<Language> languageSupplier(GrammarSpec spec) {
        return () -> loadLanguage(spec);
    }

    /**
     * Provides a native loader runnable suitable for TreeSitterParser constructors.
     */
    public Runnable nativeLoader(GrammarSpec spec) {
        return () -> ensureNativeLibraryLoaded(spec);
    }

    private boolean ensureNativeLibraryLoaded(GrammarSpec spec) {
        // Apply version pinning to ensure we're using the correct version
        GrammarSpec pinnedSpec = applyVersionPinning(spec);

        if (Boolean.TRUE.equals(nativeLoaded.get(pinnedSpec.symbol()))) {
            return true;
        }
        try {
            Optional<Path> configured = resolveConfiguredPath(pinnedSpec).map(Path::of);
            if (configured.isPresent() && tryLoad(configured.get(), pinnedSpec)) {
                nativeLoaded.put(pinnedSpec.symbol(), true);
                return true;
            }
            Path cached = ensureCachedLibrary(pinnedSpec);
            if (tryLoad(cached, pinnedSpec)) {
                nativeLoaded.put(pinnedSpec.symbol(), true);
                return true;
            }
            // Fallback to system library path if nothing else worked.
            System.loadLibrary(pinnedSpec.libraryName());
            nativeLoaded.put(pinnedSpec.symbol(), true);
            return true;
        } catch (Exception | UnsatisfiedLinkError e) {
            LOG.errorf(e, "Failed to load native library for %s. Set %s or %s to the library path.", pinnedSpec.language(), pinnedSpec.propertyKey(), pinnedSpec.envKey());
            nativeLoaded.put(pinnedSpec.symbol(), false);
            return false;
        }
    }

    private Optional<String> resolveConfiguredPath(GrammarSpec spec) {
        String configured = System.getProperty(spec.propertyKey());
        if (configured == null || configured.isBlank()) {
            configured = System.getenv(spec.envKey());
        }
        if (configured != null && !configured.isBlank()) {
            return Optional.of(configured);
        }
        return Optional.empty();
    }

    private Path ensureCachedLibrary(GrammarSpec spec) throws Exception {
        Path cacheDir = resolveCacheDir();
        Files.createDirectories(cacheDir);

        String platform = platformName();
        Path versionedLibPath = cacheDir.resolve(spec.language())
                .resolve(spec.version())
                .resolve(platform)
                .resolve(spec.libraryName() + platformLibraryExtension());

        // Check if we have the correct version cached
        GrammarVersionMetadata cachedMetadata = loadVersionMetadata(spec, platform);
        if (cachedMetadata != null && cachedMetadata.version().equals(spec.version())) {
            if (Files.exists(versionedLibPath) && Files.size(versionedLibPath) == cachedMetadata.fileSize()) {
                LOG.debugf("Using cached grammar %s v%s for platform %s", spec.language(), spec.version(), platform);
                return versionedLibPath;
            }
        }

        // Download new version
        Files.createDirectories(versionedLibPath.getParent());
        downloadGrammar(spec, versionedLibPath, (downloaded, total, message) ->
                LOG.infof("Download progress for %s: %s", spec.language(), message));

        // Store version metadata
        saveVersionMetadata(spec, platform, versionedLibPath);

        // Clean up old versions after successful download
        // Keep more versions for rollback capability
        cleanupOldVersions(spec.language(), ROLLBACK_MAX_VERSIONS_PER_LANGUAGE);

        return versionedLibPath;
    }

    private void downloadGrammar(GrammarSpec spec, Path target) throws Exception {
        downloadGrammar(spec, target, NO_PROGRESS);
    }

    private void downloadGrammar(GrammarSpec spec, Path target, DownloadProgressCallback progressCallback) throws Exception {
        String url = buildDownloadUrl(spec);
        LOG.infof("Downloading Tree-sitter grammar for %s from %s", spec.language(), url);

        Exception lastException = null;
        for (int attempt = 1; attempt <= MAX_DOWNLOAD_RETRIES; attempt++) {
            try {
                progressCallback.onProgress(0, -1, String.format("Attempting download (attempt %d/%d)", attempt, MAX_DOWNLOAD_RETRIES));
                downloadWithProgress(url, target, progressCallback);
                progressCallback.onProgress(100, 100, "Download completed successfully");

                // Verify file integrity if possible
                verifyDownloadedFile(spec, target, progressCallback);

                LOG.infof("Downloaded grammar for %s to %s", spec.language(), target);
                return;

            } catch (Exception e) {
                lastException = e;
                LOG.warnf(e, "Download attempt %d/%d failed for %s: %s", attempt, MAX_DOWNLOAD_RETRIES, spec.language(), e.getMessage());

                if (attempt < MAX_DOWNLOAD_RETRIES) {
                    Duration delay = INITIAL_RETRY_DELAY.multipliedBy(1L << (attempt - 1)); // exponential backoff
                    progressCallback.onProgress(0, -1, String.format("Retrying in %d seconds...", delay.getSeconds()));
                    Thread.sleep(delay.toMillis());
                } else {
                    // Clean up failed download
                    Files.deleteIfExists(target);
                }
            }
        }

        throw new IllegalStateException("Failed to download grammar for " + spec.language() + " after " + MAX_DOWNLOAD_RETRIES + " attempts", lastException);
    }

    private void downloadWithProgress(String url, Path target, DownloadProgressCallback progressCallback) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(HTTP_TIMEOUT)
                .GET()
                .build();

        HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("HTTP " + response.statusCode() + ": " + response.statusCode());
        }

        long contentLength = response.headers().firstValueAsLong("content-length").orElse(-1L);

        try (InputStream inputStream = response.body()) {
            Files.copy(inputStream, target, StandardCopyOption.REPLACE_EXISTING);
            // Note: For true progress tracking, we'd need to wrap the InputStream
            // For now, we show basic progress
            if (contentLength > 0) {
                progressCallback.onProgress(contentLength, contentLength, "File downloaded");
            } else {
                progressCallback.onProgress(0, -1, "File downloaded (size unknown)");
            }
        }
    }

    private void verifyDownloadedFile(GrammarSpec spec, Path filePath, DownloadProgressCallback progressCallback) throws Exception {
        if (!Files.exists(filePath)) {
            throw new IOException("Downloaded file does not exist: " + filePath);
        }

        long fileSize = Files.size(filePath);
        if (fileSize == 0) {
            throw new IOException("Downloaded file is empty: " + filePath);
        }

        progressCallback.onProgress(100, 100, String.format("Downloaded %d bytes", fileSize));

        // Check for SHA256 checksum file (common pattern in GitHub releases)
        String checksumUrl = buildDownloadUrl(spec) + ".sha256";
        try {
            Optional<String> expectedChecksum = fetchChecksum(checksumUrl);
            if (expectedChecksum.isPresent()) {
                String actualChecksum = calculateSha256(filePath);
                if (!expectedChecksum.get().equalsIgnoreCase(actualChecksum)) {
                    throw new IOException(String.format("Checksum verification failed for %s. Expected: %s, Actual: %s",
                            spec.language(), expectedChecksum.get(), actualChecksum));
                }
                progressCallback.onProgress(100, 100, "Checksum verification passed");
                LOG.debugf("Checksum verification passed for %s", spec.language());
            } else {
                LOG.debugf("No checksum file available for %s, skipping verification", spec.language());
            }
        } catch (Exception e) {
            LOG.warnf(e, "Checksum verification failed for %s, but continuing: %s", spec.language(), e.getMessage());
            // Don't fail the download for checksum issues - just warn
        }
    }

    private Optional<String> fetchChecksum(String checksumUrl) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(checksumUrl))
                    .timeout(HTTP_TIMEOUT)
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                String checksumLine = response.body().trim();
                // GitHub checksum files typically contain "checksum filename"
                String[] parts = checksumLine.split("\\s+");
                return parts.length > 0 ? Optional.of(parts[0]) : Optional.empty();
            }
        } catch (Exception e) {
            LOG.debugf(e, "Failed to fetch checksum from %s: %s", checksumUrl, e.getMessage());
        }
        return Optional.empty();
    }

    private String calculateSha256(Path filePath) throws IOException, NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (InputStream inputStream = Files.newInputStream(filePath)) {
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                digest.update(buffer, 0, bytesRead);
            }
        }
        byte[] hashBytes = digest.digest();
        return HexFormat.of().formatHex(hashBytes);
    }

    private String buildDownloadUrl(GrammarSpec spec) {
        String baseName = spec.libraryName();
        String ext = platformLibraryExtension();
        return "https://github.com/tree-sitter/" + spec.repository()
                + "/releases/download/v" + spec.version()
                + "/" + baseName + ext;
    }

    private Path resolveCacheDir() {
        String configured = System.getProperty(CACHE_DIR_PROPERTY);
        if (configured == null || configured.isBlank()) {
            configured = System.getenv(CACHE_DIR_ENV);
        }
        if (configured != null && !configured.isBlank()) {
            return Path.of(configured);
        }
        return Path.of(System.getProperty("user.home"), ".megabrain", "grammars");
    }

    private boolean tryLoad(Path libPath, GrammarSpec spec) {
        try {
            if (!Files.exists(libPath)) {
                return false;
            }
            System.load(libPath.toAbsolutePath().toString());
            LOG.debugf("Loaded native library for %s from %s", spec.language(), libPath);
            return true;
        } catch (UnsatisfiedLinkError e) {
            LOG.warnf(e, "Native library load failed for %s at %s", spec.language(), libPath);
            return false;
        } catch (Exception e) {
            LOG.warnf(e, "Unexpected error loading native library for %s at %s", spec.language(), libPath);
            return false;
        }
    }

    private String platformLibraryExtension() {
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        if (os.contains("mac")) {
            return ".dylib";
        }
        if (os.contains("win")) {
            return ".dll";
        }
        return ".so"; // default to linux/unix
    }

    private String platformName() {
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        String arch = System.getProperty("os.arch", "").toLowerCase(Locale.ROOT);

        if (os.contains("mac")) {
            return "macos-" + arch;
        }
        if (os.contains("win")) {
            return "windows-" + arch;
        }
        if (os.contains("linux")) {
            return "linux-" + arch;
        }
        return os + "-" + arch; // fallback
    }

    private Path getVersionMetadataPath(GrammarSpec spec, String platform) {
        Path cacheDir = resolveCacheDir();
        return cacheDir.resolve(spec.language())
                .resolve(spec.version())
                .resolve(platform)
                .resolve("metadata.json");
    }

    private GrammarVersionMetadata loadVersionMetadata(GrammarSpec spec, String platform) {
        String cacheKey = spec.language() + "-" + spec.version() + "-" + platform;
        GrammarVersionMetadata cached = versionCache.get(cacheKey);
        if (cached != null) {
            return cached;
        }

        try {
            Path metadataPath = getVersionMetadataPath(spec, platform);
            if (Files.exists(metadataPath)) {
                GrammarVersionMetadata metadata = OBJECT_MAPPER.readValue(metadataPath.toFile(), GrammarVersionMetadata.class);
                versionCache.put(cacheKey, metadata);
                return metadata;
            }
        } catch (Exception e) {
            LOG.debugf(e, "Failed to load version metadata for %s v%s on %s", spec.language(), spec.version(), platform);
        }
        return null;
    }

    private void saveVersionMetadata(GrammarSpec spec, String platform, Path libPath) {
        try {
            long fileSize = Files.size(libPath);
            GrammarVersionMetadata metadata = new GrammarVersionMetadata(
                    spec.language(),
                    spec.version(),
                    spec.repository(),
                    Instant.now(),
                    platform,
                    fileSize
            );

            Path metadataPath = getVersionMetadataPath(spec, platform);
            Files.createDirectories(metadataPath.getParent());
            OBJECT_MAPPER.writeValue(metadataPath.toFile(), metadata);

            String cacheKey = spec.language() + "-" + spec.version() + "-" + platform;
            versionCache.put(cacheKey, metadata);

            LOG.debugf("Saved version metadata for %s v%s on %s", spec.language(), spec.version(), platform);
        } catch (Exception e) {
            LOG.warnf(e, "Failed to save version metadata for %s v%s on %s", spec.language(), spec.version(), platform);
        }
    }

    /**
     * Get version information for a cached grammar.
     *
     * @param language the language name
     * @param version the version (optional, uses latest if null)
     * @return optional metadata if available
     */
    public Optional<GrammarVersionMetadata> getVersionInfo(String language, String version) {
        Objects.requireNonNull(language, LANGUAGE);
        String platform = platformName();

        if (version == null) {
            // Try to find the latest version by checking directory structure
            try {
                Path cacheDir = resolveCacheDir();
                Path langDir = cacheDir.resolve(language);
                if (Files.exists(langDir)) {
                    try (Stream<Path> paths = Files.list(langDir)) {
                        Optional<String> latestVersion = paths
                                .filter(Files::isDirectory)
                                .map(Path::getFileName)
                                .map(Path::toString)
                                .max(String::compareTo);
                        if (latestVersion.isPresent()) {
                            version = latestVersion.get();
                        }
                    }
                }
            } catch (Exception e) {
                LOG.debugf(e, "Failed to determine latest version for %s", language);
            }
        }

        if (version != null) {
            // Create a spec with pinned version for metadata lookup
            GrammarSpec spec = new GrammarSpec(language, "", "", "", "", "", version);
            GrammarSpec pinnedSpec = applyVersionPinning(spec);

            GrammarVersionMetadata metadata = loadVersionMetadata(pinnedSpec, platform);
            return Optional.ofNullable(metadata);
        }

        return Optional.empty();
    }

    /**
     * Get all cached versions for a language, sorted by version (newest first).
     *
     * @param language the language name
     * @return list of cached versions
     */
    public List<String> getCachedVersions(String language) {
        Objects.requireNonNull(language, LANGUAGE);
        List<String> versions = new ArrayList<>();

        try {
            Path cacheDir = resolveCacheDir();
            Path langDir = cacheDir.resolve(language);
            if (Files.exists(langDir)) {
                try (Stream<Path> paths = Files.list(langDir)) {
                    paths
                            .filter(Files::isDirectory)
                            .map(Path::getFileName)
                            .map(Path::toString)
                            .sorted(Comparator.reverseOrder()) // newest first
                            .forEach(versions::add);
                }
            }
        } catch (Exception e) {
            LOG.debugf(e, "Failed to list cached versions for %s", language);
        }

        return versions;
    }

    /**
     * Clean up old cached versions for a language, keeping only the most recent versions.
     *
     * @param language the language name
     * @param maxVersions maximum number of versions to keep (default: 3)
     * @return number of versions removed
     */
    public int cleanupOldVersions(String language, int maxVersions) {
        Objects.requireNonNull(language, LANGUAGE);
        if (maxVersions < 1) {
            throw new IllegalArgumentException("maxVersions must be at least 1");
        }

        int removedCount = 0;
        try {
            Path cacheDir = resolveCacheDir();
            Path langDir = cacheDir.resolve(language);
            if (!Files.exists(langDir)) {
                return 0;
            }

            List<Path> versionDirs;
            try (Stream<Path> paths = Files.list(langDir)) {
                versionDirs = paths
                        .filter(Files::isDirectory)
                        .sorted((a, b) -> b.getFileName().compareTo(a.getFileName())) // newest first
                        .toList();
            }

            // Keep the first maxVersions, remove the rest
            for (int i = maxVersions; i < versionDirs.size(); i++) {
                removedCount += removeVersionDirectory(versionDirs.get(i), language);
            }

            if (removedCount > 0) {
                LOG.infof("Cleaned up %d old versions for language %s, keeping %d most recent", removedCount, language, maxVersions);
            }

        } catch (Exception e) {
            LOG.warnf(e, "Failed to cleanup old versions for language %s", language);
        }

        return removedCount;
    }

    private int removeVersionDirectory(Path versionDir, String language) {
        try {
            deleteDirectoryRecursively(versionDir);
            String version = versionDir.getFileName().toString();

            String platform = platformName();
            String cacheKey = language + "-" + version + "-" + platform;
            versionCache.remove(cacheKey);

            LOG.infof("Removed old cached version %s for language %s", version, language);
            return 1;
        } catch (Exception e) {
            LOG.warnf(e, "Failed to remove cached version %s for language %s", versionDir.getFileName(), language);
            return 0;
        }
    }

    /**
     * Clean up old cached versions for a language using default retention policy.
     *
     * @param language the language name
     * @return number of versions removed
     */
    public int cleanupOldVersions(String language) {
        return cleanupOldVersions(language, DEFAULT_MAX_VERSIONS_PER_LANGUAGE);
    }

    /**
     * Clean up old cached versions for all languages.
     *
     * @param maxVersionsPerLanguage maximum number of versions to keep per language
     * @return total number of versions removed across all languages
     */
    public int cleanupAllOldVersions(int maxVersionsPerLanguage) {
        int totalRemoved = 0;
        try {
            Path cacheDir = resolveCacheDir();
            if (!Files.exists(cacheDir)) {
                return 0;
            }

            List<String> languages;
            try (Stream<Path> paths = Files.list(cacheDir)) {
                languages = paths
                        .filter(Files::isDirectory)
                        .map(Path::getFileName)
                        .map(Path::toString)
                        .toList();
            }

            for (String language : languages) {
                totalRemoved += cleanupOldVersions(language, maxVersionsPerLanguage);
            }

            if (totalRemoved > 0) {
                LOG.infof("Cleaned up total of %d old versions across all languages", totalRemoved);
            }

        } catch (Exception e) {
            LOG.warnf(e, "Failed to cleanup old versions for all languages");
        }

        return totalRemoved;
    }

    /**
     * Clean up old cached versions for all languages using default retention policy.
     *
     * @return total number of versions removed across all languages
     */
    public int cleanupAllOldVersions() {
        return cleanupAllOldVersions(DEFAULT_MAX_VERSIONS_PER_LANGUAGE);
    }

    /**
     * Get cache statistics for monitoring.
     *
     * @return cache statistics
     */
    public CacheStats getCacheStats() {
        CacheStats stats = new CacheStats();
        try {
            Path cacheDir = resolveCacheDir();
            if (!Files.exists(cacheDir)) {
                return stats;
            }

            try (Stream<Path> files = Files.walk(cacheDir)) {
                files
                        .filter(Files::isRegularFile)
                        .forEach(file -> {
                            try {
                                long size = Files.size(file);
                                stats.totalSizeBytes += size;
                                stats.totalFiles++;

                                if (file.getFileName().toString().endsWith(platformLibraryExtension())) {
                                    stats.libraryFiles++;
                                    stats.librarySizeBytes += size;
                                } else if (file.getFileName().toString().equals("metadata.json")) {
                                    stats.metadataFiles++;
                                }
                            } catch (Exception e) {
                                LOG.debugf(e, "Failed to get size for file %s", file);
                            }
                        });
            }

            // Count languages and versions
            try (Stream<Path> langDirs = Files.list(cacheDir)) {
                langDirs
                        .filter(Files::isDirectory)
                        .forEach(langDir -> {
                            stats.totalLanguages++;
                            try {
                                try (Stream<Path> versionDirs = Files.list(langDir)) {
                                    stats.totalVersions += (int) versionDirs
                                            .filter(Files::isDirectory)
                                            .count();
                                }
                            } catch (Exception e) {
                                LOG.debugf(e, "Failed to count versions for language %s", langDir.getFileName());
                            }
                        });
            }

        } catch (Exception e) {
            LOG.warnf(e, "Failed to collect cache statistics");
        }

        return stats;
    }

    /**
     * Cache statistics record.
     */
    public static class CacheStats {
        public long totalSizeBytes = 0;
        public long librarySizeBytes = 0;
        public int totalFiles = 0;
        public int libraryFiles = 0;
        public int metadataFiles = 0;
        public int totalLanguages = 0;
        public int totalVersions = 0;

        @Override
        public String toString() {
            return String.format("CacheStats{languages=%d, versions=%d, files=%d (libs=%d, meta=%d), size=%d bytes (libs=%d bytes)}",
                    totalLanguages, totalVersions, totalFiles, libraryFiles, metadataFiles, totalSizeBytes, librarySizeBytes);
        }
    }

    private void deleteDirectoryRecursively(Path path) throws IOException {
        if (!Files.exists(path)) {
            return;
        }

        // Walk the tree in reverse order and delete
        try (Stream<Path> paths = Files.walk(path)) {
            paths
                    .sorted(Comparator.reverseOrder())
                    .forEach(p -> {
                        try {
                            Files.delete(p);
                        } catch (IOException e) {
                            LOG.warnf(e, "Failed to delete %s during cleanup", p);
                        }
                    });
        }
    }

    /**
     * Rollback to a specific previous version for a language.
     *
     * @param language the language name
     * @param version the version to rollback to
     * @return rollback result information
     */
    public RollbackResult rollbackToVersion(String language, String version) {
        Objects.requireNonNull(language, LANGUAGE);
        Objects.requireNonNull(version, VERSION);

        LOG.infof("Manual rollback requested for %s to version %s", language, version);

        // Check if the version exists in cache
        List<String> cachedVersions = getCachedVersions(language);
        if (!cachedVersions.contains(version)) {
            String error = String.format("Version %s not found in cache for language %s", version, language);
            LOG.errorf(error);
            return new RollbackResult(language, UNKNOWN, version, false, error);
        }

        // Find the currently loaded version (if any)
        String currentVersion = findCurrentLoadedVersion(language);
        if (currentVersion != null && currentVersion.equals(version)) {
            String message = String.format("Already using version %s for language %s", version, language);
            LOG.infof(message);
            return new RollbackResult(language, currentVersion, version, true, message);
        }

        // Create spec for the rollback version
        GrammarSpec rollbackSpec = createGrammarSpecForVersion(language, version);
        if (rollbackSpec == null) {
            String error = String.format("Failed to create grammar spec for %s version %s", language, version);
            LOG.errorf(error);
            return new RollbackResult(language, currentVersion != null ? currentVersion : UNKNOWN, version, false, error);
        }

        // Try to load the rollback version
        Language loaded = loadLanguageWithVersion(rollbackSpec, false);
        if (loaded != null) {
            LOG.infof("Manual rollback successful for %s to version %s", language, version);
            return new RollbackResult(language, currentVersion != null ? currentVersion : UNKNOWN, version, true, null);
        } else {
            String error = String.format("Failed to load language %s with version %s during rollback", language, version);
            LOG.errorf(error);
            return new RollbackResult(language, currentVersion != null ? currentVersion : UNKNOWN, version, false, error);
        }
    }

    /**
     * Rollback to the previous working version for a language.
     *
     * @param language the language name
     * @return rollback result information
     */
    public RollbackResult rollbackToPrevious(String language) {
        Objects.requireNonNull(language, LANGUAGE);

        LOG.infof("Rolling back %s to previous working version", language);

        Deque<VersionHistoryEntry> history = versionHistory.get(language);
        if (history == null || history.isEmpty()) {
            String error = String.format("No version history available for language %s", language);
            LOG.errorf(error);
            return new RollbackResult(language, UNKNOWN, UNKNOWN, false, error);
        }

        String currentVersion = findCurrentLoadedVersion(language);

        // Find the most recent successful version that's different from current
        for (VersionHistoryEntry entry : history) {
            if (entry.success() && (!entry.version().equals(currentVersion))) {
                return rollbackToVersion(language, entry.version());
            }
        }

        String error = String.format("No suitable previous version found for language %s", language);
        LOG.errorf(error);
        return new RollbackResult(language, currentVersion != null ? currentVersion : UNKNOWN, UNKNOWN, false, error);
    }

    /**
     * Mark a version as failed/problematic for future reference.
     *
     * @param language the language name
     * @param version the version to mark as failed
     * @param errorMessage optional error message
     */
    public void markVersionAsFailed(String language, String version, String errorMessage) {
        Objects.requireNonNull(language, LANGUAGE);
        Objects.requireNonNull(version, VERSION);

        recordVersionUsage(language, version, false, errorMessage);
        LOG.infof("Marked version %s as failed for language %s%s", version, language,
                  errorMessage != null ? ": " + errorMessage : "");
    }

    /**
     * Get version history for a language.
     *
     * @param language the language name
     * @return list of version history entries (most recent first)
     */
    public List<VersionHistoryEntry> getVersionHistory(String language) {
        Objects.requireNonNull(language, LANGUAGE);

        Deque<VersionHistoryEntry> history = versionHistory.get(language);
        if (history == null) {
            return Collections.emptyList();
        }

        return List.copyOf(history);
    }

    /**
     * Find the currently loaded version for a language.
     */
    private String findCurrentLoadedVersion(String language) {
        // This is a simplified approach - in a real implementation,
        // we might need to track which version is currently active per language
        // For now, we'll look at the most recent successful history entry
        Deque<VersionHistoryEntry> history = versionHistory.get(language);
        if (history != null) {
            for (VersionHistoryEntry entry : history) {
                if (entry.success()) {
                    return entry.version();
                }
            }
        }
        return null;
    }

    /**
     * Create a GrammarSpec for a specific language and version.
     * Uses a basic spec that can be enhanced with version pinning.
     */
    public GrammarSpec createGrammarSpecForVersion(String language, String version) {
        // Create a basic spec with the target version and standard naming patterns
        // Use non-empty keys to avoid System.getProperty issues in tests
        String symbol = "tree_sitter_" + language;
        String libraryName = "tree-sitter-" + language;
        String propertyKey = "megabrain.grammar." + language + ".path";
        String envKey = "MEGABRAIN_GRAMMAR_" + language.toUpperCase() + "_PATH";
        String repository = "tree-sitter/" + language;

        GrammarSpec basicSpec = new GrammarSpec(language, symbol, libraryName, propertyKey, envKey, repository, version);

        // Apply version pinning to get the effective spec
        return applyVersionPinning(basicSpec);
    }
}


