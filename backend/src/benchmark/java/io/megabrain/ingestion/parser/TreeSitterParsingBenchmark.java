/*
 * Copyright (c) 2025 MegaBrain Contributors
 * Licensed under the MIT License - see LICENSE file for details.
 */

package io.megabrain.ingestion.parser;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * JMH benchmark to measure parsing throughput across supported languages.
 * Interpreting results: LOC/min = linesPerFile * (ops/sec) * 60.
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
public class TreeSitterParsingBenchmark {

    @Param({"python", "javascript", "typescript", "c", "cpp", "java"})
    public String language;

    @Param({"200", "2000", "10000"})
    public int linesPerFile;

    private ParserRegistry registry;
    private Path sourceFile;

    @Setup(Level.Trial)
    public void setup() throws Exception {
        registry = new ParserRegistry();
        String ext = extensionFor(language);
        String content = generateSource(language, linesPerFile);
        Path dir = Files.createTempDirectory("ts-bench");
        sourceFile = dir.resolve("bench." + ext);
        Files.writeString(sourceFile, content, StandardCharsets.UTF_8);
    }

    @Benchmark
    public int parseFile() {
        Optional<CodeParser> parser = registry.findParser(sourceFile);
        if (parser.isEmpty()) {
            throw new IllegalStateException("No parser for file: " + sourceFile);
        }
        return parser.get().parse(sourceFile).size();
    }

    private String extensionFor(String lang) {
        return switch (lang.toLowerCase(Locale.ROOT)) {
            case "python" -> "py";
            case "javascript" -> "js";
            case "typescript" -> "ts";
            case "c" -> "c";
            case "cpp" -> "cpp";
            case "java" -> "java";
            default -> throw new IllegalArgumentException("Unsupported language: " + lang);
        };
    }

    private String generateSource(String lang, int lines) {
        String body;
        switch (lang.toLowerCase(Locale.ROOT)) {
            case "python" -> body = "def foo(i):\n    return i\n";
            case "javascript" -> body = "function foo(i) { return i; }\n";
            case "typescript" -> body = "function foo(i: number): number { return i; }\n";
            case "c" -> body = "int foo(int i) { return i; }\n";
            case "cpp" -> body = "int foo(int i) { return i; }\n";
            case "java" -> body = "class Foo { int foo(int i) { return i; } }\n";
            default -> throw new IllegalArgumentException("Unsupported language: " + lang);
        }

        StringBuilder sb = new StringBuilder(lines * (body.length() + 1));
        for (int i = 0; i < lines; i++) {
            sb.append(body);
        }
        return sb.toString();
    }
}

