/*
 * Copyright (c) 2025 MegaBrain Contributors
 * Licensed under the MIT License - see LICENSE file for details.
 */

package io.megabrain.cli;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for CliSyntaxHighlighter (US-04-05 T4).
 */
class CliSyntaxHighlighterTest {

    private final CliSyntaxHighlighter highlighter = new CliSyntaxHighlighter();

    private static final String ANSI_ESCAPE = "\u001B[";

    @Test
    @DisplayName("color on produces output containing ANSI escape")
    void highlight_useColorTrue_containsAnsi() {
        String code = "public class Foo { }";
        String out = highlighter.highlight(code, "java", true);
        assertThat(out).contains(ANSI_ESCAPE);
    }

    @Test
    @DisplayName("color off returns content unchanged")
    void highlight_useColorFalse_unchanged() {
        String code = "public class Foo { }";
        String out = highlighter.highlight(code, "java", false);
        assertThat(out).isEqualTo(code);
        assertThat(out).doesNotContain(ANSI_ESCAPE);
    }

    @Test
    @DisplayName("Java snippet with keyword gets highlighting")
    void highlight_javaKeyword_containsAnsi() {
        String code = "public void run() { return 42; }";
        String out = highlighter.highlight(code, "java", true);
        assertThat(out).contains(ANSI_ESCAPE);
        assertThat(out).contains("public");
        assertThat(out).contains("return");
    }

    @Test
    @DisplayName("Python snippet with keyword gets highlighting")
    void highlight_pythonKeyword_containsAnsi() {
        String code = "def hello(): return None";
        String out = highlighter.highlight(code, "python", true);
        assertThat(out).contains(ANSI_ESCAPE);
        assertThat(out).contains("def");
        assertThat(out).contains("return");
    }

    @Test
    @DisplayName("JavaScript snippet gets highlighting")
    void highlight_javascript_containsAnsi() {
        String code = "const x = 1; function f() {}";
        String out = highlighter.highlight(code, "javascript", true);
        assertThat(out).contains(ANSI_ESCAPE);
    }

    @Test
    @DisplayName("unknown language returns content unchanged")
    void highlight_unknownLanguage_unchanged() {
        String code = "public class Foo { }";
        String out = highlighter.highlight(code, "haskell", true);
        assertThat(out).isEqualTo(code);
        assertThat(out).doesNotContain(ANSI_ESCAPE);
    }

    @Test
    @DisplayName("null language returns content unchanged")
    void highlight_nullLanguage_unchanged() {
        String code = "public class Foo { }";
        String out = highlighter.highlight(code, null, true);
        assertThat(out).isEqualTo(code);
        assertThat(out).doesNotContain(ANSI_ESCAPE);
    }

    @Test
    @DisplayName("blank language returns content unchanged")
    void highlight_blankLanguage_unchanged() {
        String code = "public class Foo { }";
        String out = highlighter.highlight(code, "  ", true);
        assertThat(out).isEqualTo(code);
        assertThat(out).doesNotContain(ANSI_ESCAPE);
    }

    @Test
    @DisplayName("empty snippet returns empty string")
    void highlight_emptySnippet_returnsEmpty() {
        String out = highlighter.highlight("", "java", true);
        assertThat(out).isEmpty();
    }

    @Test
    @DisplayName("null content returns empty string")
    void highlight_nullContent_returnsEmpty() {
        String out = highlighter.highlight(null, "java", true);
        assertThat(out).isEmpty();
    }
}
