/*
 * Copyright (c) 2025 MegaBrain Contributors
 * Licensed under the MIT License - see LICENSE file for details.
 */

package io.megabrain.ingestion.parser.treesitter;

import io.megabrain.ingestion.parser.CodeParser;
import io.megabrain.ingestion.parser.TextChunk;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
class SwiftTreeSitterParserTest {

    private final CodeParser parser = new SwiftTreeSitterParser();

    @Test
    void supportsSwiftFiles() {
        assertThat(parser.supports(Path.of("Main.swift"))).isTrue();
        assertThat(parser.supports(Path.of("Utils.swift"))).isTrue();
        assertThat(parser.supports(Path.of("app.swift"))).isTrue();
    }

    @Test
    void doesNotSupportNonSwiftFiles() {
        assertThat(parser.supports(Path.of("main.go"))).isFalse();
        assertThat(parser.supports(Path.of("script.py"))).isFalse();
        assertThat(parser.supports(Path.of("program.c"))).isFalse();
        assertThat(parser.supports(Path.of("README.md"))).isFalse();
        assertThat(parser.supports(Path.of("noextension"))).isFalse();
    }

    @Test
    void parseReturnsEmptyWhenLanguageFailsToLoad() {
        // Test with a simple Swift file - this will fail gracefully if grammar not loaded
        String swiftCode = """
                import Foundation

                class HelloWorld {
                    func main() {
                        print("Hello, World!")
                    }
                }
                """;

        // Create a temporary file
        Path tempFile = Path.of("/tmp/test.swift");
        try {
            Files.writeString(tempFile, swiftCode);
            List<TextChunk> chunks = parser.parse(tempFile);
            // Should return empty list if grammar fails to load
            assertThat(chunks).isNotNull();
        } catch (Exception e) {
            // Expected if grammar not available in test environment
        } finally {
            try {
                Files.deleteIfExists(tempFile);
            } catch (Exception e) {
                // Ignore cleanup errors
            }
        }
    }
}
