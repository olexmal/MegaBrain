/*
 * Copyright (c) 2025 MegaBrain Contributors
 * Licensed under the MIT License - see LICENSE file for details.
 */

package io.megabrain.core;

import io.megabrain.ingestion.parser.TextChunk;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * JMH benchmark for LuceneIndexService performance testing with 100K chunks.
 *
 * Measures indexing throughput, search latency, and memory usage.
 * Target: search latency < 500ms for 95th percentile.
 */
@State(Scope.Benchmark)
@BenchmarkMode({Mode.Throughput, Mode.AverageTime, Mode.SampleTime})
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 3, time = 10, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 30, timeUnit = TimeUnit.SECONDS)
public class LuceneIndexServiceBenchmark {

    @Param({"10000", "50000", "100000"})
    public int chunkCount;

    @Param({"simple", "complex", "mixed"})
    public String queryType;

    private LuceneIndexService indexService;
    private List<TextChunk> testChunks;
    private List<String> searchQueries;
    private Path tempIndexDir;
    private final MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();

    @Setup(Level.Trial)
    public void setupTrial() throws Exception {
        // Create temporary directory for index
        tempIndexDir = Files.createTempDirectory("lucene-bench-");

        // Initialize index service with temp directory
        indexService = new LuceneIndexService();
        indexService.indexDirectoryPath = tempIndexDir.toString();
        indexService.batchSize = 1000;
        indexService.commitOnBatch = false;
        indexService.initialize();

        // Generate test data
        testChunks = generateTestChunks(chunkCount);
        searchQueries = generateSearchQueries(queryType);

        System.out.printf("Setup complete: %d chunks, %d queries, index dir: %s%n",
                         chunkCount, searchQueries.size(), tempIndexDir);
    }

    @Setup(Level.Iteration)
    public void setupIteration() throws Exception {
        // For search benchmarks, we need to index chunks first
        // This is handled in each benchmark method for isolation
        System.out.printf("Setup iteration with %d chunks%n", testChunks.size());
    }

    @TearDown(Level.Trial)
    public void tearDownTrial() throws Exception {
        if (indexService != null) {
            indexService.shutdown();
        }
        if (tempIndexDir != null) {
            // Clean up temp directory
            try {
                Files.walk(tempIndexDir)
                     .sorted((a, b) -> b.compareTo(a)) // reverse order
                     .forEach(path -> {
                         try {
                             Files.delete(path);
                         } catch (IOException e) {
                             // Ignore cleanup errors
                         }
                     });
            } catch (IOException e) {
                // Ignore cleanup errors
            }
        }
        System.out.println("Teardown complete");
    }

    @Benchmark
    public void benchmarkIndexing(Blackhole blackhole) throws Exception {
        // Create fresh service for this benchmark
        LuceneIndexService freshService = new LuceneIndexService();
        freshService.indexDirectoryPath = tempIndexDir.resolve("index-" + System.nanoTime()).toString();
        freshService.batchSize = 1000;
        freshService.commitOnBatch = false;
        freshService.initialize();

        try {
            // Measure indexing time
            freshService.addChunksBatch(testChunks, 1000).await().indefinitely();

            // Consume result to avoid dead code elimination
            blackhole.consume(freshService.getIndexStats().await().indefinitely());
        } finally {
            freshService.shutdown();
        }
    }

    @Benchmark
    public void benchmarkSearch(Blackhole blackhole) throws Exception {
        // Create fresh service and index for this benchmark
        LuceneIndexService searchService = new LuceneIndexService();
        searchService.indexDirectoryPath = tempIndexDir.resolve("search-" + System.nanoTime()).toString();
        searchService.batchSize = 1000;
        searchService.commitOnBatch = false;
        searchService.initialize();

        try {
            // Index chunks for this search benchmark
            searchService.addChunksBatch(testChunks, 1000).await().indefinitely();

            // Use a random query for each benchmark iteration
            Random random = new Random();
            String query = searchQueries.get(random.nextInt(searchQueries.size()));

            // Execute search
            var results = searchService.search(query, 50).await().indefinitely();

            // Consume results to avoid dead code elimination
            blackhole.consume(results);
        } finally {
            searchService.shutdown();
        }
    }

    @Benchmark
    public void benchmarkMemoryUsage(Blackhole blackhole) throws Exception {
        // Create fresh service and index for this benchmark
        LuceneIndexService memoryService = new LuceneIndexService();
        memoryService.indexDirectoryPath = tempIndexDir.resolve("memory-" + System.nanoTime()).toString();
        memoryService.batchSize = 1000;
        memoryService.commitOnBatch = false;
        memoryService.initialize();

        try {
            // Index chunks for this memory benchmark
            memoryService.addChunksBatch(testChunks, 1000).await().indefinitely();

            // Measure memory usage during search
            MemoryUsage heapUsage = memoryBean.getHeapMemoryUsage();
            MemoryUsage nonHeapUsage = memoryBean.getNonHeapMemoryUsage();

            // Execute a search to measure memory impact
            Random random = new Random();
            String query = searchQueries.get(random.nextInt(searchQueries.size()));
            var results = memoryService.search(query, 50).await().indefinitely();

            // Return memory stats
            Map<String, Long> memoryStats = Map.of(
                "heapUsed", heapUsage.getUsed(),
                "heapCommitted", heapUsage.getCommitted(),
                "nonHeapUsed", nonHeapUsage.getUsed(),
                "resultsCount", (long) results.size()
            );

            blackhole.consume(memoryStats);
        } finally {
            memoryService.shutdown();
        }
    }

    private List<TextChunk> generateTestChunks(int count) {
        List<TextChunk> chunks = new ArrayList<>(count);
        Random random = new Random(42); // Deterministic seed

        String[] languages = {"java", "python", "javascript", "typescript", "go", "rust"};
        String[] entityTypes = {"class", "method", "function", "interface", "struct"};
        String[] modifiers = {"public", "private", "protected", "static", "final"};

        for (int i = 0; i < count; i++) {
            String language = languages[random.nextInt(languages.length)];
            String entityType = entityTypes[random.nextInt(entityTypes.length)];
            String entityName = generateEntityName(language, entityType, i, random);

            // Generate realistic content based on language and entity type
            String content = generateContent(language, entityType, entityName, random);

            // Generate source file path
            String sourceFile = String.format("src/main/%s/com/example/%s.%s",
                language, entityName.toLowerCase(), getExtension(language));

            // Generate random line numbers
            int startLine = random.nextInt(1000) + 1;
            int lineCount = random.nextInt(50) + 1;
            int endLine = startLine + lineCount - 1;

            // Generate attributes
            Map<String, String> attributes = new HashMap<>();
            if (random.nextBoolean()) {
                attributes.put("modifier", modifiers[random.nextInt(modifiers.length)]);
            }
            if (random.nextBoolean()) {
                attributes.put("signature", generateSignature(language, entityType, entityName));
            }

            TextChunk chunk = new TextChunk(
                content,
                language,
                entityType,
                entityName,
                sourceFile,
                startLine,
                endLine,
                startLine * 80, // approximate byte offset
                endLine * 80,
                attributes
            );

            chunks.add(chunk);
        }

        return chunks;
    }

    private List<String> generateSearchQueries(String type) {
        List<String> queries = new ArrayList<>();
        Random random = new Random(123); // Deterministic seed

        switch (type) {
            case "simple":
                // Simple term queries
                String[] simpleTerms = {"getUser", "process", "calculate", "parse", "validate", "UserService", "DataProcessor"};
                for (String term : simpleTerms) {
                    queries.add(term);
                    queries.add("\"" + term + "\""); // phrase queries
                }
                break;

            case "complex":
                // Complex queries with operators
                queries.add("getUser AND process");
                queries.add("calculate OR compute");
                queries.add("UserService NOT test");
                queries.add("parse AND (validate OR check)");
                queries.add("entityName:getUser AND language:java");
                queries.add("content:\"public static\" AND entityType:method");
                break;

            case "mixed":
                // Mix of simple and complex queries
                String[] mixedQueries = {
                    "getUser", "processData", "calculateTotal", "parseJson", "validateInput",
                    "getUser AND process", "calculate OR compute", "parse AND validate",
                    "entityName:UserService", "language:java AND entityType:class",
                    "\"public static void\"", "process* AND data"
                };
                for (String query : mixedQueries) {
                    queries.add(query);
                }
                break;
        }

        return queries;
    }

    private String generateEntityName(String language, String entityType, int index, Random random) {
        String[] prefixes = {"get", "set", "process", "calculate", "parse", "validate", "convert", "create", "build"};
        String[] suffixes = {"Data", "Info", "Service", "Manager", "Processor", "Handler", "Controller", "Repository"};

        String prefix = prefixes[random.nextInt(prefixes.length)];
        String suffix = suffixes[random.nextInt(suffixes.length)];

        return prefix + suffix + index;
    }

    private String generateContent(String language, String entityType, String entityName, Random random) {
        return switch (language) {
            case "java" -> generateJavaContent(entityType, entityName, random);
            case "python" -> generatePythonContent(entityType, entityName, random);
            case "javascript" -> generateJavaScriptContent(entityType, entityName, random);
            case "typescript" -> generateTypeScriptContent(entityType, entityName, random);
            case "go" -> generateGoContent(entityType, entityName, random);
            case "rust" -> generateRustContent(entityType, entityName, random);
            default -> "function " + entityName + "() { return null; }";
        };
    }

    private String generateJavaContent(String entityType, String entityName, Random random) {
        return switch (entityType) {
            case "class" -> String.format("""
                public class %s {
                    private String data;

                    public %s(String data) {
                        this.data = data;
                    }

                    public String process() {
                        return data.toUpperCase();
                    }
                }
                """, entityName, entityName);
            case "method" -> String.format("""
                public void %s(String input) {
                    if (input == null) {
                        throw new IllegalArgumentException("Input cannot be null");
                    }
                    String result = input.trim();
                    System.out.println(result);
                }
                """, entityName);
            default -> String.format("public void %s() { /* implementation */ }", entityName);
        };
    }

    private String generatePythonContent(String entityType, String entityName, Random random) {
        return switch (entityType) {
            case "function" -> String.format("""
                def %s(data):
                    if not data:
                        raise ValueError("Data cannot be empty")
                    return data.upper()
                """, entityName);
            case "class" -> String.format("""
                class %s:
                    def __init__(self, data):
                        self.data = data

                    def process(self):
                        return self.data.upper()
                """, entityName);
            default -> String.format("def %s(): pass", entityName);
        };
    }

    private String generateJavaScriptContent(String entityType, String entityName, Random random) {
        return switch (entityType) {
            case "function" -> String.format("""
                function %s(data) {
                    if (!data) {
                        throw new Error('Data cannot be empty');
                    }
                    return data.toUpperCase();
                }
                """, entityName);
            case "class" -> String.format("""
                class %s {
                    constructor(data) {
                        this.data = data;
                    }

                    process() {
                        return this.data.toUpperCase();
                    }
                }
                """, entityName);
            default -> String.format("function %s() { }", entityName);
        };
    }

    private String generateTypeScriptContent(String entityType, String entityName, Random random) {
        return switch (entityType) {
            case "function" -> String.format("""
                function %s(data: string): string {
                    if (!data) {
                        throw new Error('Data cannot be empty');
                    }
                    return data.toUpperCase();
                }
                """, entityName);
            case "class" -> String.format("""
                class %s {
                    constructor(private data: string) {}

                    process(): string {
                        return this.data.toUpperCase();
                    }
                }
                """, entityName);
            default -> String.format("function %s(): void { }", entityName);
        };
    }

    private String generateGoContent(String entityType, String entityName, Random random) {
        return switch (entityType) {
            case "function" -> String.format("""
                func %s(data string) string {
                    if data == "" {
                        panic("Data cannot be empty")
                    }
                    return strings.ToUpper(data)
                }
                """, entityName);
            case "struct" -> String.format("""
                type %s struct {
                    Data string
                }

                func (p *%s) Process() string {
                    return strings.ToUpper(p.Data)
                }
                """, entityName, entityName);
            default -> String.format("func %s() { }", entityName);
        };
    }

    private String generateRustContent(String entityType, String entityName, Random random) {
        return switch (entityType) {
            case "function" -> String.format("""
                fn %s(data: &str) -> String {
                    if data.is_empty() {
                        panic!("Data cannot be empty");
                    }
                    data.to_uppercase()
                }
                """, entityName);
            case "struct" -> String.format("""
                struct %s {
                    data: String,
                }

                impl %s {
                    fn process(&self) -> String {
                        self.data.to_uppercase()
                    }
                }
                """, entityName, entityName);
            default -> String.format("fn %s() { }", entityName);
        };
    }

    private String getExtension(String language) {
        return switch (language) {
            case "java" -> "java";
            case "python" -> "py";
            case "javascript" -> "js";
            case "typescript" -> "ts";
            case "go" -> "go";
            case "rust" -> "rs";
            default -> "txt";
        };
    }

    private String generateSignature(String language, String entityType, String entityName) {
        return switch (language) {
            case "java" -> String.format("public void %s(String)", entityName);
            case "python" -> String.format("def %s(data)", entityName);
            case "javascript" -> String.format("function %s(data)", entityName);
            case "typescript" -> String.format("%s(data: string): string", entityName);
            case "go" -> String.format("func %s(data string) string", entityName);
            case "rust" -> String.format("fn %s(data: &str) -> String", entityName);
            default -> String.format("%s()", entityName);
        };
    }
}