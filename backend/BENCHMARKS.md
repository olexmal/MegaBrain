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

