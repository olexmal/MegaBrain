/*
 * Copyright (c) 2025 MegaBrain Contributors
 * Licensed under the MIT License - see LICENSE file for details.
 */

package io.megabrain.ingestion.parser.java;

import io.megabrain.ingestion.parser.TextChunk;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * JMH benchmark for JavaParserService throughput.
 *
 * Run (fat jar, forked) from repo root:
 *   mvn -Pbenchmark -DskipTests -DskipITs clean package
 *   java -jar backend/target/megabrain-1.0.0-SNAPSHOT-benchmarks.jar \
 *        -rf text -rff backend/target/jmh-java-parser.txt
 */
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@Warmup(iterations = 1)
@Measurement(iterations = 3)
@Fork(1)
public class JavaParserServiceBenchmark {

    @State(Scope.Benchmark)
    public static class BenchmarkState {
        JavaParserService service;
        Path tempFile;

        @Setup(Level.Trial)
        public void setup() throws IOException {
            service = new JavaParserService();
            StringBuilder builder = new StringBuilder("package com.example;\npublic class Large {\n");
            for (int i = 0; i < 12000; i++) {
                builder.append("    public void method").append(i).append("() {}\n");
            }
            builder.append("}\n");
            tempFile = Files.createTempFile("javaparser-bench-", ".java");
            Files.writeString(tempFile, builder.toString());
        }
    }

    @Benchmark
    public List<TextChunk> parseLargeFile(BenchmarkState state) {
        return state.service.parse(state.tempFile);
    }
}

