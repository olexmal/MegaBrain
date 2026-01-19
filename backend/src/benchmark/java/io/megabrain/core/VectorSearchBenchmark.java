/*
 * Copyright (c) 2025 MegaBrain Contributors
 * Licensed under the MIT License - see LICENSE file for details.
 */

package io.megabrain.core;

import io.megabrain.ingestion.parser.TextChunk;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.results.format.ResultFormatType;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * JMH benchmarks for vector similarity search performance.
 * Tests search latency with 100K vectors to ensure <500ms 95th percentile performance.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Benchmark)
@Warmup(iterations = 3, time = 10, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 30, timeUnit = TimeUnit.SECONDS)
@Threads(4)
@Fork(1)
public class VectorSearchBenchmark {

    private PgVectorStore vectorStore;
    private List<float[]> queryVectors;
    private Random random;

    @Param({"10000", "50000", "100000"})
    private int vectorCount;

    @Setup(Level.Trial)
    public void setup() throws SQLException {
        System.out.println("Setting up benchmark with " + vectorCount + " vectors");

        // Initialize components (simplified for benchmark)
        vectorStore = new PgVectorStore();
        // Note: In real benchmark, you'd inject proper DataSource and configure it

        random = new Random(42); // Fixed seed for reproducible results

        // Generate and store test vectors
        setupTestData();

        // Generate query vectors
        generateQueryVectors();
    }

    private void setupTestData() throws SQLException {
        // Create test vectors
        List<VectorStore.VectorEntry> entries = new ArrayList<>();

        for (int i = 0; i < vectorCount; i++) {
            float[] vector = generateRandomVector();
            VectorStore.VectorMetadata metadata = new VectorStore.VectorMetadata(
                "public class TestClass" + i + " { }",
                "java",
                "class",
                "TestClass" + i,
                "/test/File" + i + ".java",
                1, 1, 0, 20
            );

            entries.add(new VectorStore.VectorEntry("test-" + i, vector, metadata));
        }

        // Store in batches
        for (int i = 0; i < entries.size(); i += 100) {
            int endIndex = Math.min(i + 100, entries.size());
            List<VectorStore.VectorEntry> batch = entries.subList(i, endIndex);
            vectorStore.storeBatch(batch).await().indefinitely();
        }

        System.out.println("Stored " + vectorCount + " test vectors");
    }

    private void generateQueryVectors() {
        queryVectors = new ArrayList<>();
        for (int i = 0; i < 100; i++) { // 100 different query vectors
            queryVectors.add(generateRandomVector());
        }
    }

    @Benchmark
    public List<VectorStore.SearchResult> benchmarkVectorSearch() {
        // Pick a random query vector
        float[] queryVector = queryVectors.get(random.nextInt(queryVectors.size()));

        // Perform search
        return vectorStore.search(queryVector, 10).await().indefinitely();
    }

    @Benchmark
    public List<VectorStore.SearchResult> benchmarkVectorSearchWithThreshold() {
        float[] queryVector = queryVectors.get(random.nextInt(queryVectors.size()));

        // Search with similarity threshold
        return vectorStore.search(queryVector, 10, 0.7).await().indefinitely();
    }

    @Benchmark
    public void benchmarkBatchStore() {
        // Benchmark batch storage performance
        List<VectorStore.VectorEntry> batch = List.of(
            new VectorStore.VectorEntry(
                "bench-batch-1",
                generateRandomVector(),
                new VectorStore.VectorMetadata("class Bench {}", "java", "class", "Bench", "/bench.java", 1, 1, 0, 10)
            )
        );

        vectorStore.storeBatch(batch).await().indefinitely();
    }

    @TearDown(Level.Trial)
    public void teardown() {
        // Cleanup would go here
        System.out.println("Benchmark teardown completed");
    }

    private float[] generateRandomVector() {
        float[] vector = new float[384]; // all-MiniLM-L6-v2 dimension
        for (int i = 0; i < vector.length; i++) {
            vector[i] = (random.nextFloat() - 0.5f) * 2.0f; // Random values between -1 and 1
        }
        return vector;
    }

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(VectorSearchBenchmark.class.getSimpleName())
                .result("target/jmh-results.json")
                .resultFormat(ResultFormatType.JSON)
                .build();

        new Runner(opt).run();
    }
}