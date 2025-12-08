/*
 * Copyright (c) 2025 MegaBrain Contributors
 * Licensed under the MIT License - see LICENSE file for details.
 */

package io.megabrain.ingestion.parser.java;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.CompilationUnit;
import io.megabrain.ingestion.parser.TextChunk;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class JavaAstVisitorTest {

    @Test
    void extractsEntitiesAndMetadata() {
        String source = """
                package com.example;

                public class Sample {
                    private String name;

                    public Sample(String name) {
                        this.name = name;
                    }

                    public String greet(String target) {
                        return "hi " + target;
                    }

                    void makeRunnable() {
                        Runnable r = new Runnable() {
                            @Override
                            public void run() {}
                        };
                        r.run();
                    }
                }
                """;

        List<TextChunk> chunks = parseWithVisitor(source, Path.of("Sample.java"));

        TextChunk classChunk = findByType(chunks, "class");
        assertThat(classChunk.entityName()).isEqualTo("com.example.Sample");
        assertThat(classChunk.attributes().get("modifiers")).contains("public");
        assertThat(classChunk.attributes().get("package")).isEqualTo("com.example");

        TextChunk fieldChunk = findByType(chunks, "field");
        assertThat(fieldChunk.entityName()).isEqualTo("com.example.Sample#name");
        assertThat(fieldChunk.attributes().get("fieldType")).isEqualTo("String");

        TextChunk ctorChunk = findByType(chunks, "constructor");
        assertThat(ctorChunk.entityName()).contains("Sample#Sample(String)");
        assertThat(ctorChunk.attributes().get("parent")).isEqualTo("com.example.Sample");

        TextChunk methodChunk = chunks.stream()
                .filter(c -> c.entityType().equals("method") && c.entityName().contains("greet"))
                .findFirst()
                .orElseThrow();
        assertThat(methodChunk.attributes().get("returnType")).isEqualTo("String");
        assertThat(methodChunk.attributes().get("parameters")).contains("String target");

        TextChunk anonymousClass = chunks.stream()
                .filter(c -> c.entityType().equals("anonymous_class"))
                .findFirst()
                .orElseThrow();
        assertThat(anonymousClass.entityName()).contains("AnonymousClass");
        assertThat(anonymousClass.attributes().get("parent")).isEqualTo("com.example.Sample");

        TextChunk anonymousRun = chunks.stream()
                .filter(c -> c.entityType().equals("method") && c.entityName().contains("AnonymousClass") && c.entityName().contains("run"))
                .findFirst()
                .orElseThrow();
        assertThat(anonymousRun.attributes().get("parent")).contains("AnonymousClass");
    }

    private List<TextChunk> parseWithVisitor(String source, Path sourcePath) {
        JavaParser parser = new JavaParser(new ParserConfiguration()
                .setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_21));
        Optional<CompilationUnit> cu = parser.parse(source).getResult();
        assertThat(cu).isPresent();

        String packageName = cu.get()
                .getPackageDeclaration()
                .map(pd -> pd.getName().asString())
                .orElse("");

        SourceCoordinates coordinates = new SourceCoordinates(source);
        JavaAstVisitor visitor = new JavaAstVisitor(
                "java",
                packageName,
                sourcePath,
                source,
                coordinates,
                source.getBytes(StandardCharsets.UTF_8).length
        );

        cu.get().accept(visitor, new JavaAstVisitor.Context());
        return visitor.getChunks();
    }

    private TextChunk findByType(List<TextChunk> chunks, String entityType) {
        return chunks.stream()
                .filter(c -> c.entityType().equals(entityType))
                .findFirst()
                .orElseThrow();
    }
}

