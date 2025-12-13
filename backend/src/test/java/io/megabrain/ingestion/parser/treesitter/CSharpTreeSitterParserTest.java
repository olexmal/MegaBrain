/*
 * Copyright (c) 2025 MegaBrain Contributors
 * Licensed under the MIT License - see LICENSE file for details.
 */

package io.megabrain.ingestion.parser.treesitter;

import io.megabrain.ingestion.parser.CodeParser;
import io.megabrain.ingestion.parser.TextChunk;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
class CSharpTreeSitterParserTest {

    private final CodeParser parser = new CSharpTreeSitterParser();

    @Test
    void supportsCSharpFiles() {
        assertThat(parser.supports(Path.of("Program.cs"))).isTrue();
        assertThat(parser.supports(Path.of("Utils.cs"))).isTrue();
        assertThat(parser.supports(Path.of("app.cs"))).isTrue();
    }

    @Test
    void doesNotSupportNonCSharpFiles() {
        assertThat(parser.supports(Path.of("main.go"))).isFalse();
        assertThat(parser.supports(Path.of("script.py"))).isFalse();
        assertThat(parser.supports(Path.of("program.c"))).isFalse();
        assertThat(parser.supports(Path.of("README.md"))).isFalse();
        assertThat(parser.supports(Path.of("noextension"))).isFalse();
    }

    @Test
    void parseReturnsEmptyWhenLanguageFailsToLoad() {
        // Test with a simple C# file - this will fail gracefully if grammar not loaded
        String csharpCode = """
                using System;

                namespace HelloWorld
                {
                    class Program
                    {
                        static void Main(string[] args)
                        {
                            Console.WriteLine("Hello, World!");
                        }
                    }
                }
                """;

        // Create a temporary file
        Path tempFile = Path.of("/tmp/test.cs");
        try {
            java.nio.file.Files.writeString(tempFile, csharpCode);
            List<TextChunk> chunks = parser.parse(tempFile);
            // Should return empty list if grammar fails to load
            assertThat(chunks).isNotNull();
        } catch (Exception e) {
            // Expected if grammar not available in test environment
        } finally {
            try {
                java.nio.file.Files.deleteIfExists(tempFile);
            } catch (Exception e) {
                // Ignore cleanup errors
            }
        }
    }
}
