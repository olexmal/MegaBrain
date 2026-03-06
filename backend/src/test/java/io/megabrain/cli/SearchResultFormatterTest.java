/*
 * Copyright (c) 2025 MegaBrain Contributors
 * Licensed under the MIT License - see LICENSE file for details.
 */

package io.megabrain.cli;

import io.megabrain.api.LineRange;
import io.megabrain.api.SearchResponse;
import io.megabrain.api.SearchResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for HumanReadableSearchResultFormatter (US-04-05 T3).
 */
class SearchResultFormatterTest {

    private HumanReadableSearchResultFormatter formatter;

    @BeforeEach
    void setUp() {
        formatter = new HumanReadableSearchResultFormatter();
    }

    @Test
    @DisplayName("empty results returns No results.")
    void format_emptyResults_returnsNoResults() {
        SearchResponse response = new SearchResponse(List.of(), 0, 0, 10, "q", 5L);
        String out = formatter.format(response);
        assertThat(out).isEqualTo("No results.");
    }

    @Test
    @DisplayName("null response returns No results.")
    void format_nullResponse_returnsNoResults() {
        String out = formatter.format(null);
        assertThat(out).isEqualTo("No results.");
    }

    @Test
    @DisplayName("single result has File, Entity, Score, snippet and separator")
    void format_singleResult_includesFileEntityScoreSnippet() {
        SearchResult result = SearchResult.create(
                "public void run() { }",
                "MyClass.run()",
                "method",
                "src/MyClass.java",
                "java",
                "repo1",
                0.95f,
                new LineRange(10, 12)
        );
        SearchResponse response = new SearchResponse(List.of(result), 1, 0, 10, "run", 10L);
        String out = formatter.format(response);

        assertThat(out).contains("File: src/MyClass.java");
        assertThat(out).contains("Entity: MyClass.run()");
        assertThat(out).contains("Score: 0.95");
        assertThat(out).contains("public void run() { }");
        assertThat(out).contains(HumanReadableSearchResultFormatter.RESULT_SEPARATOR);
    }

    @Test
    @DisplayName("multiple results have separators between them")
    void format_multipleResults_includesSeparators() {
        SearchResult r1 = SearchResult.create("c1", "E1", "class", "f1.java", "java", "", 0.9f, new LineRange(1, 1));
        SearchResult r2 = SearchResult.create("c2", "E2", "method", "f2.java", "java", "", 0.8f, new LineRange(1, 1));
        SearchResponse response = new SearchResponse(List.of(r1, r2), 2, 0, 10, "query", 5L);
        String out = formatter.format(response);

        assertThat(out).contains("File: f1.java");
        assertThat(out).contains("File: f2.java");
        assertThat(out).contains("---");
        // Each result block ends with ---; with 2 results we get two separators
        assertThat(out.indexOf("---")).isGreaterThanOrEqualTo(0);
    }

    @Test
    @DisplayName("long snippet is truncated by line count")
    void format_longSnippet_truncatedByLineCount() {
        StringBuilder content = new StringBuilder();
        for (int i = 0; i < 25; i++) {
            content.append("line ").append(i).append("\n");
        }
        SearchResult result = SearchResult.create(
                content.toString(),
                "X",
                "class",
                "f.java",
                "java",
                "",
                0.5f,
                new LineRange(1, 25)
        );
        SearchResponse response = new SearchResponse(List.of(result), 1, 0, 10, "q", 1L);
        String out = formatter.format(response);

        assertThat(out).contains("(truncated)");
        assertThat(out).contains("line 0");
        assertThat(out).doesNotContain("line 20");
    }

    @Test
    @DisplayName("long line is truncated by line length")
    void format_longLine_truncatedByLineLength() {
        String longLine = "a".repeat(200);
        SearchResult result = SearchResult.create(longLine, "E", "method", "f.java", "java", "", 0.5f, new LineRange(1, 1));
        SearchResponse response = new SearchResponse(List.of(result), 1, 0, 10, "q", 1L);
        String out = formatter.format(response);

        assertThat(out).contains("...");
        assertThat(out).doesNotContain("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa");
    }

    @Test
    @DisplayName("null and blank content/sourceFile/entityName do not cause NPE")
    void format_nullAndBlankFields_noNPE() {
        SearchResult result = new SearchResult(
                null,
                null,
                null,
                null,
                "",
                "",
                0f,
                new LineRange(1, 1),
                null,
                null,
                false,
                null
        );
        SearchResponse response = new SearchResponse(List.of(result), 1, 0, 10, "q", 1L);
        String out = formatter.format(response);

        assertThat(out).contains("(no path)");
        assertThat(out).contains("(no entity)");
        assertThat(out).contains("Score: 0.0");
    }

    @Test
    @DisplayName("quiet format: one line per result path and entity")
    void formatQuiet_multipleResults_oneLinePerResult() {
        SearchResult r1 = SearchResult.create("c1", "E1", "class", "path/a.java", "java", "", 0.9f, new LineRange(1, 1));
        SearchResult r2 = SearchResult.create("c2", "E2", "method", "path/b.java", "java", "", 0.8f, new LineRange(1, 1));
        SearchResponse response = new SearchResponse(List.of(r1, r2), 2, 0, 10, "q", 1L);
        String out = formatter.formatQuiet(response);

        assertThat(out).contains("path/a.java");
        assertThat(out).contains("E1");
        assertThat(out).contains("path/b.java");
        assertThat(out).contains("E2");
        assertThat(out.split("\n").length).isEqualTo(2);
    }

    @Test
    @DisplayName("quiet format empty returns No results.")
    void formatQuiet_empty_returnsNoResults() {
        SearchResponse response = new SearchResponse(List.of(), 0, 0, 10, "q", 0L);
        String out = formatter.formatQuiet(response);
        assertThat(out).isEqualTo("No results.");
    }

    @Test
    @DisplayName("format with quiet true uses minimal output")
    void format_responseWithQuietTrue_usesQuietFormat() {
        SearchResult r = SearchResult.create("x", "MyClass", "class", "p.java", "java", "", 0.9f, new LineRange(1, 1));
        SearchResponse response = new SearchResponse(List.of(r), 1, 0, 10, "q", 1L);
        String out = formatter.format(response, true);

        assertThat(out).contains("p.java");
        assertThat(out).contains("MyClass");
        assertThat(out).doesNotContain("Score:");
        assertThat(out).doesNotContain("Entity:");
    }

    private static final String ANSI_ESCAPE = "\u001B[";

    @Test
    @DisplayName("format with useColor true and highlighter produces ANSI in snippet")
    void format_useColorTrue_snippetContainsAnsi() {
        HumanReadableSearchResultFormatter formatterWithHighlighter =
                new HumanReadableSearchResultFormatter(new CliSyntaxHighlighter());
        SearchResult result = SearchResult.create(
                "public void run() { }",
                "MyClass.run()",
                "method",
                "src/MyClass.java",
                "java",
                "repo1",
                0.95f,
                new LineRange(10, 12)
        );
        SearchResponse response = new SearchResponse(List.of(result), 1, 0, 10, "run", 10L);
        String out = formatterWithHighlighter.format(response, false, true);

        assertThat(out).contains("File: src/MyClass.java");
        assertThat(out).contains(ANSI_ESCAPE);
    }

    @Test
    @DisplayName("format with useColor false has no ANSI in output")
    void format_useColorFalse_noAnsi() {
        HumanReadableSearchResultFormatter formatterWithHighlighter =
                new HumanReadableSearchResultFormatter(new CliSyntaxHighlighter());
        SearchResult result = SearchResult.create(
                "public void run() { }",
                "MyClass.run()",
                "method",
                "src/MyClass.java",
                "java",
                "repo1",
                0.95f,
                new LineRange(10, 12)
        );
        SearchResponse response = new SearchResponse(List.of(result), 1, 0, 10, "run", 10L);
        String out = formatterWithHighlighter.format(response, false, false);

        assertThat(out).contains("File: src/MyClass.java");
        assertThat(out).contains("public void run() { }");
        assertThat(out).doesNotContain(ANSI_ESCAPE);
    }
}
