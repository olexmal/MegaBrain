/*
 * Copyright (c) 2025 MegaBrain Contributors
 * Licensed under the MIT License - see LICENSE file for details.
 */

package io.megabrain.core;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for CitationParser (US-03-05 T2).
 */
class CitationParserTest {

    private CitationParser citationParser;

    @BeforeEach
    void setUp() {
        citationParser = new CitationParser();
    }

    @Test
    @DisplayName("parse extracts single citation with path and line")
    void parse_singleValidCitation_extractsPathAndLine() {
        String answer = "Auth is in AuthService [Source: src/auth/AuthService.java:25].";
        List<ExtractedCitation> citations = citationParser.parse(answer);

        assertThat(citations).hasSize(1);
        ExtractedCitation c = citations.get(0);
        assertThat(c.filePath()).isEqualTo("src/auth/AuthService.java");
        assertThat(c.lineStart()).isEqualTo(25);
        assertThat(c.lineEnd()).isEqualTo(25);
        assertThat(c.toSourceString()).isEqualTo("src/auth/AuthService.java:25");
    }

    @Test
    @DisplayName("parse extracts multiple citations")
    void parse_multipleCitations_extractsAll() {
        String answer = "Uses UserRepository [Source: src/data/UserRepository.java:42] and AuthService [Source: src/auth/AuthService.java:25].";
        List<ExtractedCitation> citations = citationParser.parse(answer);

        assertThat(citations).hasSize(2);
        assertThat(citations.get(0).filePath()).isEqualTo("src/data/UserRepository.java");
        assertThat(citations.get(0).lineStart()).isEqualTo(42);
        assertThat(citations.get(1).filePath()).isEqualTo("src/auth/AuthService.java");
        assertThat(citations.get(1).lineStart()).isEqualTo(25);
    }

    @Test
    @DisplayName("parse extracts line range when format is path:start-end")
    void parse_lineRangeCitation_extractsStartAndEnd() {
        String answer = "See [Source: src/main/App.java:10-20] for details.";
        List<ExtractedCitation> citations = citationParser.parse(answer);

        assertThat(citations).hasSize(1);
        ExtractedCitation c = citations.get(0);
        assertThat(c.filePath()).isEqualTo("src/main/App.java");
        assertThat(c.lineStart()).isEqualTo(10);
        assertThat(c.lineEnd()).isEqualTo(20);
        assertThat(c.toSourceString()).isEqualTo("src/main/App.java:10-20");
    }

    @Test
    @DisplayName("parse returns empty list for null or blank text")
    void parse_nullOrBlank_returnsEmptyList() {
        assertThat(citationParser.parse(null)).isEmpty();
        assertThat(citationParser.parse("")).isEmpty();
        assertThat(citationParser.parse("   ")).isEmpty();
    }

    @Test
    @DisplayName("parse returns empty list when no citations present")
    void parse_noCitations_returnsEmptyList() {
        String answer = "This answer has no citations at all.";
        assertThat(citationParser.parse(answer)).isEmpty();
    }

    @Test
    @DisplayName("parse skips empty citation segment gracefully")
    void parse_emptyCitationSegment_skipsGracefully() {
        String answer = "Text [Source:   ] more text.";
        List<ExtractedCitation> citations = citationParser.parse(answer);
        assertThat(citations).isEmpty();
    }

    @Test
    @DisplayName("parse skips citation with no colon in inner part")
    void parse_noColonInInner_skipsGracefully() {
        String answer = "Text [Source: no-colon-here] more.";
        List<ExtractedCitation> citations = citationParser.parse(answer);
        assertThat(citations).isEmpty();
    }

    @Test
    @DisplayName("parse skips citation with non-numeric line")
    void parse_nonNumericLine_skipsGracefully() {
        String answer = "Text [Source: src/App.java:abc] more.";
        List<ExtractedCitation> citations = citationParser.parse(answer);
        assertThat(citations).isEmpty();
    }

    @Test
    @DisplayName("parse skips citation with invalid line range")
    void parse_invalidLineRange_skipsGracefully() {
        String answer = "Text [Source: src/App.java:10-twenty] more.";
        List<ExtractedCitation> citations = citationParser.parse(answer);
        assertThat(citations).isEmpty();
    }

    @Test
    @DisplayName("parse skips citation with negative line")
    void parse_negativeLine_skipsGracefully() {
        String answer = "Text [Source: src/App.java:-5] more.";
        List<ExtractedCitation> citations = citationParser.parse(answer);
        assertThat(citations).isEmpty();
    }

    @Test
    @DisplayName("parse skips citation with zero line")
    void parse_zeroLine_skipsGracefully() {
        String answer = "Text [Source: src/App.java:0] more.";
        List<ExtractedCitation> citations = citationParser.parse(answer);
        assertThat(citations).isEmpty();
    }

    @Test
    @DisplayName("parse accepts path with spaces after colon in tag")
    void parse_spaceAfterSourceColon_extractsCitation() {
        String answer = "See [Source:  src/App.java:1].";
        List<ExtractedCitation> citations = citationParser.parse(answer);
        assertThat(citations).hasSize(1);
        assertThat(citations.get(0).filePath()).isEqualTo("src/App.java");
        assertThat(citations.get(0).lineStart()).isEqualTo(1);
    }

    @Test
    @DisplayName("parse mixes valid and invalid citations and returns only valid")
    void parse_mixedValidAndInvalid_returnsOnlyValid() {
        String answer = "One [Source: src/a.java:1] two [Source: bad] three [Source: src/b.java:2].";
        List<ExtractedCitation> citations = citationParser.parse(answer);
        assertThat(citations).hasSize(2);
        assertThat(citations.get(0).filePath()).isEqualTo("src/a.java");
        assertThat(citations.get(0).lineStart()).isEqualTo(1);
        assertThat(citations.get(1).filePath()).isEqualTo("src/b.java");
        assertThat(citations.get(1).lineStart()).isEqualTo(2);
    }
}
