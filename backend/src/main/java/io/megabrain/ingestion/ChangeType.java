/*
 * Copyright (c) 2025 MegaBrain Contributors
 * Licensed under the MIT License - see LICENSE file for details.
 */

package io.megabrain.ingestion;

/**
 * Types of file changes that can be detected in a git diff.
 */
public enum ChangeType {
    /**
     * File was added to the repository.
     */
    ADDED,

    /**
     * File was modified (content changed).
     */
    MODIFIED,

    /**
     * File was deleted from the repository.
     */
    DELETED,

    /**
     * File was renamed (may also include content changes).
     */
    RENAMED
}
