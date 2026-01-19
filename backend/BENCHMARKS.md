<!--
Copyright (c) 2025 MegaBrain Contributors
Licensed under the MIT License - see LICENSE file for details.
-->

# Backend Benchmarks (JMH)

## JavaParserService throughput benchmark

The shaded JMH jar includes `JavaParserServiceBenchmark.parseLargeFile`, which parses a synthetic 12k-method Java class.

### Build
```bash
cd backend
mvn -Pbenchmark -DskipTests -DskipITs clean package
```
Creates `target/megabrain-1.0.0-SNAPSHOT-benchmarks.jar`.

### Run
```bash
cd backend
java -jar target/megabrain-1.0.0-SNAPSHOT-benchmarks.jar \
  -rf text -rff target/jmh-java-parser.txt
```

### Output
- Results saved to `backend/target/jmh-java-parser.txt` (throughput ops/s).
- Default JMH settings in the benchmark: warmup 1 x 10s, measurement 3 x 10s, forks 1, mode Throughput.

## Lucene Index Service performance benchmark

The shaded JMH jar includes `LuceneIndexServiceBenchmark` for measuring indexing and search performance with large datasets.

### Test Scenarios
- **Indexing**: Measures throughput for adding 10K, 50K, and 100K chunks to the index
- **Search**: Measures latency for various query types (simple, complex, mixed) against indexed data
- **Memory**: Monitors heap and non-heap memory usage during operations

### Build
```bash
cd backend
mvn -Pbenchmark -DskipTests -DskipITs clean package
```
Creates `target/megabrain-1.0.0-SNAPSHOT-benchmarks.jar`.

### Run Indexing Benchmark
```bash
cd backend
java -jar target/megabrain-1.0.0-SNAPSHOT-benchmarks.jar \
  -rf text -rff target/jmh-lucene-indexing.txt \
  LuceneIndexServiceBenchmark.benchmarkIndexing
```

### Run Search Benchmark
```bash
cd backend
java -jar target/megabrain-1.0.0-SNAPSHOT-benchmarks.jar \
  -rf text -rff target/jmh-lucene-search.txt \
  LuceneIndexServiceBenchmark.benchmarkSearch
```

### Run Memory Benchmark
```bash
cd backend
java -jar target/megabrain-1.0.0-SNAPSHOT-benchmarks.jar \
  -rf text -rff target/jmh-lucene-memory.txt \
  LuceneIndexServiceBenchmark.benchmarkMemoryUsage
```

### Run All Benchmarks
```bash
cd backend
java -jar target/megabrain-1.0.0-SNAPSHOT-benchmarks.jar \
  -rf text -rff target/jmh-lucene-all.txt \
  LuceneIndexServiceBenchmark
```

### Performance Targets
- **Indexing Throughput**: Measure chunks/second for different dataset sizes
- **Search Latency**: Target <500ms for 95th percentile across all query types
- **Memory Usage**: Monitor heap usage patterns during large-scale operations

### Benchmark Parameters
- `chunkCount`: {10000, 50000, 100000} - Number of chunks to index
- `queryType`: {simple, complex, mixed} - Type of search queries to test

### Output
- Results saved to `backend/target/jmh-lucene-*.txt`
- JMH settings: warmup 3 x 10s, measurement 5 x 30s, multiple modes (Throughput, AverageTime, SampleTime)
