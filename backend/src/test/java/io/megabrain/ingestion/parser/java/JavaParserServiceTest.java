/*
 * Copyright (c) 2025 MegaBrain Contributors
 * Licensed under the MIT License - see LICENSE file for details.
 */

package io.megabrain.ingestion.parser.java;

import io.megabrain.ingestion.parser.TextChunk;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class JavaParserServiceTest {

    private final JavaParserService parser = new JavaParserService();

    @Test
    void supportsJavaExtension(@TempDir Path tempDir) throws IOException {
        Path javaFile = tempDir.resolve("Sample.java");
        Files.writeString(javaFile, "public class Sample {}");

        Path otherFile = tempDir.resolve("script.py");
        Files.writeString(otherFile, "print('hi')");

        assertThat(parser.supports(javaFile)).isTrue();
        assertThat(parser.supports(otherFile)).isFalse();
    }

    @Test
    void extractsClassesMethodsAndFieldsWithMetadata(@TempDir Path tempDir) throws IOException {
        Path javaFile = tempDir.resolve("Sample.java");
        String source = """
                package com.example;

                public class Sample {
                    private String name;

                    public Sample(String name) {
                        this.name = name;
                    }

                    public String greet(String target) {
                        return "Hello " + target + " from " + name;
                    }
                }
                """;
        Files.writeString(javaFile, source);

        List<TextChunk> chunks = parser.parse(javaFile);

        TextChunk classChunk = chunks.stream()
                .filter(chunk -> chunk.entityType().equals("class"))
                .findFirst()
                .orElseThrow();
        assertThat(classChunk.entityName()).isEqualTo("com.example.Sample");
        assertThat(classChunk.attributes().get("modifiers")).contains("public");
        assertThat(classChunk.startLine()).isEqualTo(3);
        assertThat(classChunk.endLine()).isGreaterThan(classChunk.startLine());

        TextChunk fieldChunk = chunks.stream()
                .filter(chunk -> chunk.entityType().equals("field"))
                .findFirst()
                .orElseThrow();
        assertThat(fieldChunk.entityName()).isEqualTo("com.example.Sample#name");
        assertThat(fieldChunk.attributes()).containsEntry("fieldType", "String");
        assertThat(fieldChunk.attributes().get("modifiers")).contains("private");

        TextChunk constructorChunk = chunks.stream()
                .filter(chunk -> chunk.entityType().equals("constructor"))
                .findFirst()
                .orElseThrow();
        assertThat(constructorChunk.entityName()).contains("Sample#Sample(String)");
        assertThat(constructorChunk.attributes().get("parameters")).contains("String name");

        TextChunk methodChunk = chunks.stream()
                .filter(chunk -> chunk.entityType().equals("method") && chunk.entityName().contains("greet"))
                .findFirst()
                .orElseThrow();
        assertThat(methodChunk.entityName()).isEqualTo("com.example.Sample#greet(String)");
        assertThat(methodChunk.attributes()).containsEntry("returnType", "String");
        assertThat(methodChunk.attributes().get("parameters")).contains("String target");
        assertThat(methodChunk.content()).contains("greet");
    }

    @Test
    void extractsNestedAndAnonymousClasses(@TempDir Path tempDir) throws IOException {
        Path javaFile = tempDir.resolve("Outer.java");
        String source = """
                package com.example;

                public class Outer {
                    class Inner {
                        void doWork() {}
                    }

                    void createRunnable() {
                        Runnable r = new Runnable() {
                            @Override
                            public void run() {}
                        };
                        r.run();
                    }
                }
                """;
        Files.writeString(javaFile, source);

        List<TextChunk> chunks = parser.parse(javaFile);

        TextChunk innerClass = chunks.stream()
                .filter(chunk -> chunk.entityType().equals("class") && chunk.entityName().endsWith("Outer.Inner"))
                .findFirst()
                .orElseThrow();
        assertThat(innerClass.attributes()).containsEntry("parent", "com.example.Outer");

        TextChunk anonymousClass = chunks.stream()
                .filter(chunk -> chunk.entityType().equals("anonymous_class"))
                .findFirst()
                .orElseThrow();
        assertThat(anonymousClass.entityName()).contains("Outer.AnonymousClass");
        assertThat(anonymousClass.attributes()).containsEntry("parent", "com.example.Outer");

        TextChunk anonymousRun = chunks.stream()
                .filter(chunk -> chunk.entityType().equals("method") && chunk.entityName().contains("AnonymousClass") && chunk.entityName().contains("run"))
                .findFirst()
                .orElseThrow();
        assertThat(anonymousRun.attributes().get("parent")).contains("AnonymousClass");
    }

    @Test
    void handlesMalformedFilesGracefully(@TempDir Path tempDir) throws IOException {
        Path javaFile = tempDir.resolve("Broken.java");
        String source = """
                package com.example;
                public class Broken {
                    void brokenMethod(
                """;
        Files.writeString(javaFile, source);

        assertThatCode(() -> parser.parse(javaFile)).doesNotThrowAnyException();
        List<TextChunk> chunks = parser.parse(javaFile);
        // We may still extract partial information, but we should never throw
        assertThat(chunks).isNotNull();
    }

    @Test
    void meetsPerformanceTargetForLargeFile(@TempDir Path tempDir) throws IOException {
        Path javaFile = tempDir.resolve("Large.java");
        StringBuilder builder = new StringBuilder("package com.example;\npublic class Large {\n");
        // ~12,000 LOC: 12,000 methods on separate lines
        for (int i = 0; i < 12000; i++) {
            builder.append("    public void method").append(i).append("() {}\n");
        }
        builder.append("}\n");
        Files.writeString(javaFile, builder.toString());

        long start = System.nanoTime();
        List<TextChunk> chunks = parser.parse(javaFile);
        long durationMs = (System.nanoTime() - start) / 1_000_000;

        assertThat(chunks).isNotEmpty();
        // 12k LOC at a target of 10k LOC/min gives a 72s budget
        assertThat(durationMs).isLessThan(72_000);
    }
}

