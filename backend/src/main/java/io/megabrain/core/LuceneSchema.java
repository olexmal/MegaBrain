/*
 * Copyright (c) 2025 MegaBrain Contributors
 * Licensed under the MIT License - see LICENSE file for details.
 */

package io.megabrain.core;

import org.apache.lucene.document.FieldType;
import org.apache.lucene.index.IndexOptions;

/**
 * Defines the Lucene index schema for MegaBrain code search.
 *
 * This class provides field definitions and configurations for indexing TextChunks
 * in a Lucene index. Each field is configured with appropriate indexing and storage
 * options based on its intended use (searching, filtering, retrieval).
 */
public final class LuceneSchema {

    // Prevent instantiation
    private LuceneSchema() {}

    // ===== FIELD NAMES =====

    /** Primary content field - tokenized for full-text search */
    public static final String FIELD_CONTENT = "content";

    /** Entity name field - tokenized version for search */
    public static final String FIELD_ENTITY_NAME = "entity_name";

    /** Entity name field - exact match version for filtering */
    public static final String FIELD_ENTITY_NAME_KEYWORD = "entity_name_keyword";

    /** Programming language identifier */
    public static final String FIELD_LANGUAGE = "language";

    /** Entity type (class, method, field, etc.) */
    public static final String FIELD_ENTITY_TYPE = "entity_type";

    /** Source file path */
    public static final String FIELD_FILE_PATH = "file_path";

    /** Repository identifier (extracted from file path) */
    public static final String FIELD_REPOSITORY = "repository";

    /** Documentation summary (if available) */
    public static final String FIELD_DOC_SUMMARY = "doc_summary";

    /** Line number where entity starts */
    public static final String FIELD_START_LINE = "start_line";

    /** Line number where entity ends */
    public static final String FIELD_END_LINE = "end_line";

    /** Byte offset where entity starts in file */
    public static final String FIELD_START_BYTE = "start_byte";

    /** Byte offset where entity ends in file */
    public static final String FIELD_END_BYTE = "end_byte";

    /** Unique document identifier for updates/deletions */
    public static final String FIELD_DOCUMENT_ID = "document_id";

// ===== FIELD TYPE DEFINITIONS =====

/** Default repository name when extraction fails */
public static final String DEFAULT_REPOSITORY = "unknown";

/** Field type for full-text content - tokenized and searchable */
public static final FieldType CONTENT_FIELD_TYPE = createContentFieldType();

    /** Field type for tokenized entity names - searchable */
    public static final FieldType ENTITY_NAME_FIELD_TYPE = createEntityNameFieldType();

    /** Field type for keyword fields - exact match, filterable */
    public static final FieldType KEYWORD_FIELD_TYPE = createKeywordFieldType();

    /** Field type for stored-only fields - not searchable */
    public static final FieldType STORED_ONLY_FIELD_TYPE = createStoredOnlyFieldType();

    /**
     * Creates field type for content fields.
     * - Tokenized for full-text search
     * - Stored for retrieval
     * - Positions and offsets indexed for phrase queries
     */
    private static FieldType createContentFieldType() {
        FieldType fieldType = new FieldType();
        fieldType.setStored(true);
        fieldType.setIndexOptions(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS_AND_OFFSETS);
        fieldType.setTokenized(true);
        fieldType.freeze();
        return fieldType;
    }

    /**
     * Creates field type for entity names.
     * - Tokenized for search (allows partial matches)
     * - Stored for retrieval
     * - Positions indexed for phrase queries
     */
    private static FieldType createEntityNameFieldType() {
        FieldType fieldType = new FieldType();
        fieldType.setStored(true);
        fieldType.setIndexOptions(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS);
        fieldType.setTokenized(true);
        fieldType.freeze();
        return fieldType;
    }

    /**
     * Creates field type for keyword fields.
     * - Not tokenized (exact match only)
     * - Stored for retrieval
     * - Indexed for filtering and exact searches
     */
    private static FieldType createKeywordFieldType() {
        FieldType fieldType = new FieldType();
        fieldType.setStored(true);
        fieldType.setIndexOptions(IndexOptions.DOCS);
        fieldType.setTokenized(false);
        fieldType.freeze();
        return fieldType;
    }

    /**
     * Creates field type for stored-only fields.
     * - Not indexed (not searchable)
     * - Stored for retrieval
     */
    private static FieldType createStoredOnlyFieldType() {
        FieldType fieldType = new FieldType();
        fieldType.setStored(true);
        fieldType.setIndexOptions(IndexOptions.NONE);
        fieldType.setTokenized(false);
        fieldType.freeze();
        return fieldType;
    }

    // ===== FIELD MAPPING CONSTANTS =====

    /** TextChunk attribute key for documentation summary */
    public static final String ATTR_DOC_SUMMARY = "doc_summary";

    /** TextChunk attribute key for repository identifier */
    public static final String ATTR_REPOSITORY = "repository";

    /** Prefix for dynamic metadata fields from TextChunk attributes */
    public static final String METADATA_FIELD_PREFIX = "meta_";

    // ===== UTILITY METHODS =====

    /**
     * Creates a metadata field name from an attribute key.
     * @param attributeKey the attribute key from TextChunk.attributes
     * @return the corresponding Lucene field name
     */
    public static String createMetadataFieldName(String attributeKey) {
        return METADATA_FIELD_PREFIX + attributeKey.toLowerCase().replaceAll("[^a-z0-9_]", "_");
    }

    /**
     * Extracts repository name from a file path.
     * Supports common repository path patterns like:
     * - /path/to/repo/src/file.java -> repo
     * - github.com/owner/repo/file.java -> repo
     * - owner/repo/file.java -> repo
     *
     * @param filePath the file path
     * @return the repository name, or {@link #DEFAULT_REPOSITORY} if cannot be determined
     */
    public static String extractRepositoryFromPath(String filePath) {
        if (filePath == null || filePath.isBlank()) {
            return DEFAULT_REPOSITORY;
        }

        String normalizedPath = filePath.replace('\\', '/');
        String[] parts = normalizedPath.split("/");

        if (parts.length == 0) {
            return DEFAULT_REPOSITORY;
        }

        // Try different extraction strategies in order of preference
        String repo = extractFromHostingService(normalizedPath, parts);
        if (!DEFAULT_REPOSITORY.equals(repo)) return repo;

        repo = extractFromProjectStructure(parts);
        if (!DEFAULT_REPOSITORY.equals(repo)) return repo;

        repo = extractFromBuildFiles(parts);
        if (!DEFAULT_REPOSITORY.equals(repo)) return repo;

        repo = extractFromOwnerRepoPattern(parts);
        if (!DEFAULT_REPOSITORY.equals(repo)) return repo;

        return extractFallback(parts);
    }

    private static String extractFromHostingService(String normalizedPath, String[] parts) {
        if (normalizedPath.contains("github.com") || normalizedPath.contains("gitlab.com") || normalizedPath.contains("bitbucket.org")) {
            for (int i = 0; i < parts.length - 2; i++) {
                if (isHostingService(parts[i])) {
                    String repo = parts[i + 2];
                    if (isValidRepoName(repo)) {
                        return repo.toLowerCase();
                    }
                }
            }
        }
        return DEFAULT_REPOSITORY;
    }

    private static String extractFromProjectStructure(String[] parts) {
        for (int i = 0; i < parts.length - 1; i++) {
            if (isProjectStructureDir(parts[i + 1])) {
                String potentialRepo = parts[i];
                if (isValidRepoName(potentialRepo) && !isCommonDirName(potentialRepo)) {
                    return potentialRepo.toLowerCase();
                }
            }
        }
        return DEFAULT_REPOSITORY;
    }

    private static String extractFromBuildFiles(String[] parts) {
        for (int i = 1; i < parts.length; i++) {
            if (isBuildFile(parts[i])) {
                String potentialRepo = parts[i - 1];
                if (isValidRepoName(potentialRepo)) {
                    return potentialRepo.toLowerCase();
                }
            }
        }
        return DEFAULT_REPOSITORY;
    }

    private static String extractFromOwnerRepoPattern(String[] parts) {
        for (int i = parts.length - 2; i >= 0; i--) {
            String part = parts[i];
            String nextPart = (i + 1 < parts.length) ? parts[i + 1] : "";

            if (isValidRepoName(part) && isValidRepoName(nextPart) &&
                !isCommonDirName(part) && !isCommonDirName(nextPart)) {
                return nextPart.toLowerCase();
            }

            if (isValidRepoName(part)) {
                break;
            }
        }
        return DEFAULT_REPOSITORY;
    }

    private static String extractFallback(String[] parts) {
        for (int i = parts.length - 2; i >= 0; i--) {
            String part = parts[i];
            if (isValidRepoName(part) && !isBuildOrCommonDir(part)) {
                return part.toLowerCase();
            }
        }
        return DEFAULT_REPOSITORY;
    }

    private static boolean isHostingService(String part) {
        return "github.com".equals(part) || "gitlab.com".equals(part) || "bitbucket.org".equals(part);
    }

    private static boolean isProjectStructureDir(String part) {
        return "src".equals(part) || "main".equals(part) || "test".equals(part) || "docs".equals(part);
    }

    private static boolean isBuildFile(String part) {
        return "README".equals(part) || "package.json".equals(part) || "pom.xml".equals(part) ||
               "build.gradle".equals(part) || "Cargo.toml".equals(part) || "go.mod".equals(part);
    }

    private static boolean isValidRepoName(String name) {
        return name != null && !name.isEmpty() && !name.contains(".") && name.length() > 1;
    }

    private static boolean isCommonDirName(String name) {
        return "java".equals(name) || "com".equals(name) || "org".equals(name) ||
               "net".equals(name) || "io".equals(name);
    }

    private static boolean isBuildOrCommonDir(String name) {
        return isCommonDirName(name) || "src".equals(name) || "main".equals(name) ||
               "test".equals(name) || "docs".equals(name) || "bin".equals(name) ||
               "target".equals(name) || "build".equals(name) || "out".equals(name) || "dist".equals(name);
    }
}
