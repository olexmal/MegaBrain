/*
 * Copyright (c) 2025 MegaBrain Contributors
 * Licensed under the MIT License - see LICENSE file for details.
 */

package io.megabrain.core.prompt;

import io.megabrain.api.LineRange;
import io.megabrain.api.SearchResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ContextFormatterTest {

    private ContextFormatter formatter;

    @BeforeEach
    void setUp() {
        formatter = new ContextFormatterImpl();
    }

    @Test
    void testFormatSingleChunk() {
        SearchResult result = SearchResult.create(
                "public boolean login(String user, String pass) {\n    return true;\n}",
                "AuthService.login",
                "method",
                "src/auth/AuthService.java",
                "Java",
                "repo1",
                0.9f,
                new LineRange(25, 27)
        );

        String formatted = formatter.format(result);

        String expected = "[Source: src/auth/AuthService.java - AuthService.login() (lines 25-27)]\n" +
                "```java\n" +
                "public boolean login(String user, String pass) {\n" +
                "    return true;\n" +
                "}\n" +
                "```";

        assertEquals(expected, formatted);
    }

    @Test
    void testFormatChunkWithoutMethodParens() {
        SearchResult result = SearchResult.create(
                "def calculate_total():\n    pass",
                "calculate_total",
                "function",
                "src/billing/invoice.py",
                "Python",
                "repo1",
                0.8f,
                new LineRange(10, 11)
        );

        String formatted = formatter.format(result);

        String expected = "[Source: src/billing/invoice.py - calculate_total() (lines 10-11)]\n" +
                "```python\n" +
                "def calculate_total():\n" +
                "    pass\n" +
                "```";

        assertEquals(expected, formatted);
    }

    @Test
    void testFormatChunkWithClass() {
        SearchResult result = SearchResult.create(
                "public class User {\n    private String name;\n}",
                "User",
                "class",
                "src/models/User.java",
                "Java",
                "repo1",
                0.95f,
                new LineRange(5, 7)
        );

        String formatted = formatter.format(result);

        String expected = "[Source: src/models/User.java - User (lines 5-7)]\n" +
                "```java\n" +
                "public class User {\n" +
                "    private String name;\n" +
                "}\n" +
                "```";

        assertEquals(expected, formatted);
    }

    @Test
    void testFormatAllChunks() {
        SearchResult r1 = SearchResult.create(
                "const x = 1;",
                "config",
                "variable",
                "src/index.js",
                "JavaScript",
                "repo1",
                0.9f,
                new LineRange(1, 1)
        );
        
        SearchResult r2 = SearchResult.create(
                "func main() {}",
                "main",
                "function",
                "main.go",
                "Go",
                "repo1",
                0.8f,
                new LineRange(5, 5)
        );

        String formatted = formatter.formatAll(List.of(r1, r2));

        String expected = "[Source: src/index.js - config (lines 1-1)]\n" +
                "```javascript\n" +
                "const x = 1;\n" +
                "```\n" +
                "\n" +
                "[Source: main.go - main() (lines 5-5)]\n" +
                "```go\n" +
                "func main() {}\n" +
                "```";

        assertEquals(expected, formatted);
    }

    @Test
    void testFormatNullOrEmpty() {
        assertEquals("", formatter.format(null));
        assertEquals("", formatter.formatAll(null));
        assertEquals("", formatter.formatAll(List.of()));
    }
    
    @Test
    void testFormatMissingMetadata() {
        SearchResult r = new SearchResult(
            "code", null, null, "file.txt", null, null, 0, null, null, null, false, null
        );
        String formatted = formatter.format(r);
        assertTrue(formatted.contains("[Source: file.txt]"));
        assertTrue(formatted.contains("```\ncode\n```"));
    }
}
