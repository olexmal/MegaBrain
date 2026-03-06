/*
 * Copyright (c) 2025 MegaBrain Contributors
 * Licensed under the MIT License - see LICENSE file for details.
 */

package io.megabrain.cli;

/**
 * Highlights code snippets with ANSI color codes for terminal output.
 * Implementations may be keyword/pattern-based; unknown or blank language returns content unchanged.
 */
public interface SyntaxHighlighter {

    /**
     * Optionally highlights the given content for the specified language.
     *
     * @param content  the code snippet to highlight (may be empty)
     * @param language the programming language (e.g. java, python); null or blank → return content unchanged
     * @param useColor when false, returns content unchanged; when true, may wrap tokens in ANSI codes
     * @return content with ANSI codes when useColor is true and language is supported, else content unchanged
     */
    String highlight(String content, String language, boolean useColor);
}
