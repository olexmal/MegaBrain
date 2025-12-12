/*
 * Copyright (c) 2025 MegaBrain Contributors
 * Licensed under the MIT License - see LICENSE file for details.
 */

package io.megabrain.ingestion.parser;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import io.github.treesitter.jtreesitter.Language;
import org.jboss.logging.Logger;

import java.lang.foreign.SymbolLookup;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages Tree-sitter grammar loading and native library lifecycle.
 * Caches loaded grammars and libraries to avoid repeated loads.
 * Tracks grammar versions and provides version management.
 */
public class GrammarManager {

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
            Objects.requireNonNull(language, "language");
            Objects.requireNonNull(version, "version");
            Objects.requireNonNull(repository, "repository");
            Objects.requireNonNull(downloadedAt, "downloadedAt");
            Objects.requireNonNull(platform, "platform");
        }
    }

    private static final Logger LOG = Logger.getLogger(GrammarManager.class);
    private static final String CACHE_DIR_PROPERTY = "megabrain.grammar.cache.dir";
    private static final String CACHE_DIR_ENV = "MEGABRAIN_GRAMMAR_CACHE_DIR";
    private static final Duration HTTP_TIMEOUT = Duration.ofSeconds(20);
    private static final ObjectMapper OBJECT_MAPPER = JsonMapper.builder()
            .addModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule())
            .build();

    private final Map<String, Language> loadedLanguages = new ConcurrentHashMap<>();
    private final Map<String, Boolean> nativeLoaded = new ConcurrentHashMap<>();
    private final Map<String, GrammarVersionMetadata> versionCache = new ConcurrentHashMap<>();
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(HTTP_TIMEOUT)
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    /**
     * Load (or return cached) Tree-sitter language for the given spec.
     *
     * @param spec grammar spec metadata
     * @return loaded Language or null if loading failed
     */
    public Language loadLanguage(GrammarSpec spec) {
        Objects.requireNonNull(spec, "spec");
        Language cached = loadedLanguages.get(spec.symbol());
        if (cached != null) {
            return cached;
        }
        if (!ensureNativeLibraryLoaded(spec)) {
            return null;
        }
        try {
            Language lang = Language.load(SymbolLookup.loaderLookup(), spec.symbol());
            loadedLanguages.put(spec.symbol(), lang);
            LOG.debugf("Loaded Tree-sitter language %s via symbol %s", spec.language(), spec.symbol());
            return lang;
        } catch (Exception | UnsatisfiedLinkError e) {
            LOG.errorf(e, "Failed to load grammar for %s via symbol %s", spec.language(), spec.symbol());
            return null;
        }
    }

    /**
     * Provides a supplier suitable for TreeSitterParser constructors.
     */
    public java.util.function.Supplier<Language> languageSupplier(GrammarSpec spec) {
        return () -> loadLanguage(spec);
    }

    /**
     * Provides a native loader runnable suitable for TreeSitterParser constructors.
     */
    public Runnable nativeLoader(GrammarSpec spec) {
        return () -> ensureNativeLibraryLoaded(spec);
    }

    private boolean ensureNativeLibraryLoaded(GrammarSpec spec) {
        if (Boolean.TRUE.equals(nativeLoaded.get(spec.symbol()))) {
            return true;
        }
        try {
            Optional<Path> configured = resolveConfiguredPath(spec).map(Path::of);
            if (configured.isPresent() && tryLoad(configured.get(), spec)) {
                nativeLoaded.put(spec.symbol(), true);
                return true;
            }
            Path cached = ensureCachedLibrary(spec);
            if (tryLoad(cached, spec)) {
                nativeLoaded.put(spec.symbol(), true);
                return true;
            }
            // Fallback to system library path if nothing else worked.
            System.loadLibrary(spec.libraryName());
            nativeLoaded.put(spec.symbol(), true);
            return true;
        } catch (Exception | UnsatisfiedLinkError e) {
            LOG.errorf(e, "Failed to load native library for %s. Set %s or %s to the library path.", spec.language(), spec.propertyKey(), spec.envKey());
            nativeLoaded.put(spec.symbol(), false);
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
        downloadGrammar(spec, versionedLibPath);

        // Store version metadata
        saveVersionMetadata(spec, platform, versionedLibPath);

        return versionedLibPath;
    }

    private void downloadGrammar(GrammarSpec spec, Path target) throws Exception {
        String url = buildDownloadUrl(spec);
        LOG.infof("Downloading Tree-sitter grammar for %s from %s", spec.language(), url);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(HTTP_TIMEOUT)
                .GET()
                .build();
        HttpResponse<Path> response = httpClient.send(request, HttpResponse.BodyHandlers.ofFile(target));
        if (response.statusCode() >= 200 && response.statusCode() < 300) {
            LOG.infof("Downloaded grammar for %s to %s", spec.language(), target);
        } else {
            LOG.warnf("Failed to download grammar for %s. HTTP %s", spec.language(), response.statusCode());
            Files.deleteIfExists(target);
            throw new IllegalStateException("Grammar download failed with status " + response.statusCode());
        }
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
        Objects.requireNonNull(language, "language");
        String platform = platformName();

        if (version == null) {
            // Try to find the latest version by checking directory structure
            try {
                Path cacheDir = resolveCacheDir();
                Path langDir = cacheDir.resolve(language);
                if (Files.exists(langDir)) {
                    Optional<String> latestVersion = Files.list(langDir)
                            .filter(Files::isDirectory)
                            .map(Path::getFileName)
                            .map(Path::toString)
                            .max(String::compareTo);
                    if (latestVersion.isPresent()) {
                        version = latestVersion.get();
                    }
                }
            } catch (Exception e) {
                LOG.debugf(e, "Failed to determine latest version for %s", language);
            }
        }

        if (version != null) {
            GrammarVersionMetadata metadata = loadVersionMetadata(
                    new GrammarSpec(language, "", "", "", "", "", version),
                    platform
            );
            return Optional.ofNullable(metadata);
        }

        return Optional.empty();
    }
}


