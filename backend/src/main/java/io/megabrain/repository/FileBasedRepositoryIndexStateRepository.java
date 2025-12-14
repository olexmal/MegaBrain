/*
 * Copyright (c) 2025 MegaBrain Contributors
 * Licensed under the MIT License - see LICENSE file for details.
 */

package io.megabrain.repository;

import io.megabrain.ingestion.RepositoryIndexState;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * File-based implementation of RepositoryIndexStateRepository.
 * Stores repository index states as JSON files in a configurable directory.
 * This implementation is suitable for development and can be easily migrated to a database later.
 */
@ApplicationScoped
public class FileBasedRepositoryIndexStateRepository implements RepositoryIndexStateRepository {

    private static final Logger LOG = Logger.getLogger(FileBasedRepositoryIndexStateRepository.class);

    private static final String DEFAULT_DATA_DIR = "./data";
    private static final String INDEX_STATE_DIR = "index-state";

    @ConfigProperty(name = "megabrain.data.directory", defaultValue = DEFAULT_DATA_DIR)
    String dataDirectory;

    // In-memory cache for better performance
    private final Map<String, RepositoryIndexState> cache = new ConcurrentHashMap<>();

    private Path getIndexStateDirectory() {
        return Paths.get(dataDirectory, INDEX_STATE_DIR);
    }

    private Path getStateFilePath(String repositoryUrl) {
        // Create a safe filename from the repository URL
        String safeFileName = repositoryUrl
                .replace("https://", "")
                .replace("http://", "")
                .replace("git@", "")
                .replace(":", "_")
                .replace("/", "_")
                .replace(".", "_")
                .replaceAll("[^a-zA-Z0-9_-]", "_") + ".json";
        return getIndexStateDirectory().resolve(safeFileName);
    }

    @Override
    public Uni<Optional<RepositoryIndexState>> findByRepositoryUrl(String repositoryUrl) {
        return Uni.createFrom().item(() -> {
            // Check cache first
            RepositoryIndexState cached = cache.get(repositoryUrl);
            if (cached != null) {
                return Optional.of(cached);
            }

            // Load from file
            try {
                Path filePath = getStateFilePath(repositoryUrl);
                if (!Files.exists(filePath)) {
                    return Optional.empty();
                }

                String jsonContent = Files.readString(filePath);
                RepositoryIndexState state = parseFromJson(jsonContent);
                cache.put(repositoryUrl, state);
                return Optional.of(state);
            } catch (Exception e) {
                LOG.warnf(e, "Failed to load index state for repository: %s", repositoryUrl);
                return Optional.empty();
            }
        });
    }

    @Override
    public Uni<RepositoryIndexState> save(RepositoryIndexState state) {
        return Uni.createFrom().item(() -> {
            try {
                // Ensure directory exists
                Path dir = getIndexStateDirectory();
                Files.createDirectories(dir);

                // Write to file
                Path filePath = getStateFilePath(state.repositoryUrl());
                String jsonContent = toJson(state);
                Files.writeString(filePath, jsonContent);

                // Update cache
                cache.put(state.repositoryUrl(), state);

                LOG.debugf("Saved index state for repository: %s", state.repositoryUrl());
                return state;
            } catch (Exception e) {
                LOG.errorf(e, "Failed to save index state for repository: %s", state.repositoryUrl());
                throw new RuntimeException("Failed to save repository index state", e);
            }
        });
    }

    @Override
    public Uni<Boolean> deleteByRepositoryUrl(String repositoryUrl) {
        return Uni.createFrom().item(() -> {
            try {
                Path filePath = getStateFilePath(repositoryUrl);
                boolean deleted = Files.deleteIfExists(filePath);

                // Remove from cache
                cache.remove(repositoryUrl);

                if (deleted) {
                    LOG.debugf("Deleted index state for repository: %s", repositoryUrl);
                }
                return deleted;
            } catch (Exception e) {
                LOG.warnf(e, "Failed to delete index state for repository: %s", repositoryUrl);
                return false;
            }
        });
    }

    @Override
    public Uni<Boolean> existsByRepositoryUrl(String repositoryUrl) {
        return Uni.createFrom().item(() -> {
            // Check cache first
            if (cache.containsKey(repositoryUrl)) {
                return true;
            }

            // Check file system
            Path filePath = getStateFilePath(repositoryUrl);
            return Files.exists(filePath);
        });
    }

    private String toJson(RepositoryIndexState state) {
        return String.format("""
                {
                  "repositoryUrl": "%s",
                  "lastIndexedCommitSha": "%s",
                  "lastIndexedAt": "%s"
                }
                """,
                escapeJson(state.repositoryUrl()),
                escapeJson(state.lastIndexedCommitSha()),
                escapeJson(state.lastIndexedAt())
        );
    }

    private RepositoryIndexState parseFromJson(String json) {
        // Simple JSON parsing - in production, use a proper JSON library
        String repositoryUrl = extractJsonValue(json, "repositoryUrl");
        String lastIndexedCommitSha = extractJsonValue(json, "lastIndexedCommitSha");
        String lastIndexedAt = extractJsonValue(json, "lastIndexedAt");

        return new RepositoryIndexState(repositoryUrl, lastIndexedCommitSha, lastIndexedAt);
    }

    private String extractJsonValue(String json, String key) {
        String pattern = "\"" + key + "\": \"";
        int startIndex = json.indexOf(pattern);
        if (startIndex == -1) {
            throw new IllegalArgumentException("Key not found: " + key);
        }
        startIndex += pattern.length();
        int endIndex = json.indexOf("\"", startIndex);
        if (endIndex == -1) {
            throw new IllegalArgumentException("Invalid JSON format for key: " + key);
        }
        return unescapeJson(json.substring(startIndex, endIndex));
    }

    private String escapeJson(String value) {
        return value.replace("\\", "\\\\")
                   .replace("\"", "\\\"")
                   .replace("\n", "\\n")
                   .replace("\r", "\\r")
                   .replace("\t", "\\t");
    }

    private String unescapeJson(String value) {
        return value.replace("\\\"", "\"")
                   .replace("\\\\", "\\")
                   .replace("\\n", "\n")
                   .replace("\\r", "\r")
                   .replace("\\t", "\t");
    }
}
