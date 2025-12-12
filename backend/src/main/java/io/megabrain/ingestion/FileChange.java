/*
 * Copyright (c) 2025 MegaBrain Contributors
 * Licensed under the MIT License - see LICENSE file for details.
 */

package io.megabrain.ingestion;

/**
 * Represents a file change detected in a git diff operation.
 * Contains information about the type of change and the affected file path(s).
 */
public record FileChange(
        ChangeType changeType,
        String filePath,
        String oldPath // only set for renames, null otherwise
) {
    public FileChange {
        if (changeType == null) {
            throw new IllegalArgumentException("changeType must not be null");
        }
        if (filePath == null || filePath.isBlank()) {
            throw new IllegalArgumentException("filePath must not be null or blank");
        }
        if (changeType == ChangeType.RENAMED && (oldPath == null || oldPath.isBlank())) {
            throw new IllegalArgumentException("oldPath must be provided for RENAMED changes");
        }
        if (changeType != ChangeType.RENAMED && oldPath != null) {
            throw new IllegalArgumentException("oldPath should only be set for RENAMED changes");
        }
    }

    /**
     * Creates a new FileChange for added, modified, or deleted files.
     */
    public static FileChange of(ChangeType changeType, String filePath) {
        if (changeType == ChangeType.RENAMED) {
            throw new IllegalArgumentException("Use of(ChangeType, String, String) for RENAMED changes");
        }
        return new FileChange(changeType, filePath, null);
    }

    /**
     * Creates a new FileChange for renamed files.
     */
    public static FileChange renamed(String oldPath, String newPath) {
        return new FileChange(ChangeType.RENAMED, newPath, oldPath);
    }
}
