/*
 * Copyright (c) 2025 MegaBrain Contributors
 * Licensed under the MIT License - see LICENSE file for details.
 */

package io.megabrain.core;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * PostgreSQL-based implementation of VectorStore using pgvector extension.
 * Provides efficient vector storage and similarity search capabilities.
 */
@ApplicationScoped
public class PgVectorStore implements VectorStore {

    private static final Logger LOG = Logger.getLogger(PgVectorStore.class);

    public static final int VECTOR_DIMENSION = 384; // all-MiniLM-L6-v2 dimension

    @Inject
    Instance<DataSource> dataSourceInstance;

    @ConfigProperty(name = "megabrain.vector.batch-size", defaultValue = "100")
    int batchSize;

    @ConfigProperty(name = "megabrain.vector.ef-search", defaultValue = "40")
    int efSearch;

    private Executor asyncExecutor;
    private DataSource dataSource;

    @PostConstruct
    void init() {
        LOG.info("Initializing PgVectorStore");

        if (dataSourceInstance.isResolvable()) {
            this.dataSource = dataSourceInstance.get();
            LOG.info("DataSource available - PgVectorStore will be fully functional");
        } else {
            LOG.warn("No DataSource configured - PgVectorStore operations will fail");
        }

        this.asyncExecutor = Infrastructure.getDefaultExecutor();
        LOG.infof("PgVectorStore initialized with batch size: %d", batchSize);
    }

    @PreDestroy
    void destroy() {
        LOG.info("Shutting down PgVectorStore");
    }

    @Override
    public Uni<Void> store(String id, float[] vector, VectorMetadata metadata) {
        return Uni.createFrom().completionStage(
            CompletableFuture.runAsync(() -> {
                checkDataSourceAvailable();
                validateVector(vector);

                String sql = """
                    INSERT INTO code_chunks_vectors (
                        id, content, language, entity_type, entity_name,
                        source_file, start_line, end_line, start_byte, end_byte, embedding
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    ON CONFLICT (id) DO UPDATE SET
                        content = EXCLUDED.content,
                        language = EXCLUDED.language,
                        entity_type = EXCLUDED.entity_type,
                        entity_name = EXCLUDED.entity_name,
                        source_file = EXCLUDED.source_file,
                        start_line = EXCLUDED.start_line,
                        end_line = EXCLUDED.end_line,
                        start_byte = EXCLUDED.start_byte,
                        end_byte = EXCLUDED.end_byte,
                        embedding = EXCLUDED.embedding,
                        updated_at = CURRENT_TIMESTAMP
                    """;

                try (Connection conn = dataSource.getConnection();
                     PreparedStatement stmt = conn.prepareStatement(sql)) {

                    stmt.setString(1, id);
                    stmt.setString(2, metadata.content());
                    stmt.setString(3, metadata.language());
                    stmt.setString(4, metadata.entityType());
                    stmt.setString(5, metadata.entityName());
                    stmt.setString(6, metadata.sourceFile());
                    stmt.setInt(7, metadata.startLine());
                    stmt.setInt(8, metadata.endLine());
                    stmt.setInt(9, metadata.startByte());
                    stmt.setInt(10, metadata.endByte());
                    stmt.setString(11, vectorToPgVector(vector));

                    int rowsAffected = stmt.executeUpdate();
                    LOG.debugf("Stored vector with id: %s, rows affected: %d", id, rowsAffected);

                } catch (SQLException e) {
                    LOG.errorf(e, "Failed to store vector with id: %s", id);
                    throw new RuntimeException("Failed to store vector", e);
                }
            }, asyncExecutor)
        );
    }

    @Override
    public Uni<Void> storeBatch(List<VectorEntry> vectors) {
        return Uni.createFrom().completionStage(
            CompletableFuture.runAsync(() -> {
                if (vectors == null || vectors.isEmpty()) {
                    return;
                }

                LOG.debugf("Storing batch of %d vectors", vectors.size());

                // Process in batches to avoid memory issues
                for (int i = 0; i < vectors.size(); i += batchSize) {
                    int endIndex = Math.min(i + batchSize, vectors.size());
                    List<VectorEntry> batch = vectors.subList(i, endIndex);
                    storeBatchInternal(batch);
                }

                LOG.debugf("Successfully stored batch of %d vectors", vectors.size());
            }, asyncExecutor)
        );
    }

    private void storeBatchInternal(List<VectorEntry> vectors) {
        checkDataSourceAvailable();
        String sql = """
            INSERT INTO code_chunks_vectors (
                id, content, language, entity_type, entity_name,
                source_file, start_line, end_line, start_byte, end_byte, embedding
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT (id) DO UPDATE SET
                content = EXCLUDED.content,
                language = EXCLUDED.language,
                entity_type = EXCLUDED.entity_type,
                entity_name = EXCLUDED.entity_name,
                source_file = EXCLUDED.source_file,
                start_line = EXCLUDED.start_line,
                end_line = EXCLUDED.end_line,
                start_byte = EXCLUDED.start_byte,
                end_byte = EXCLUDED.end_byte,
                embedding = EXCLUDED.embedding,
                updated_at = CURRENT_TIMESTAMP
            """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            for (VectorEntry entry : vectors) {
                validateVector(entry.vector());

                stmt.setString(1, entry.id());
                stmt.setString(2, entry.metadata().content());
                stmt.setString(3, entry.metadata().language());
                stmt.setString(4, entry.metadata().entityType());
                stmt.setString(5, entry.metadata().entityName());
                stmt.setString(6, entry.metadata().sourceFile());
                stmt.setInt(7, entry.metadata().startLine());
                stmt.setInt(8, entry.metadata().endLine());
                stmt.setInt(9, entry.metadata().startByte());
                stmt.setInt(10, entry.metadata().endByte());
                stmt.setString(11, vectorToPgVector(entry.vector()));

                stmt.addBatch();
            }

            int[] results = stmt.executeBatch();
            int totalAffected = 0;
            for (int result : results) {
                if (result > 0) totalAffected += result;
            }

            LOG.debugf("Batch insert/update affected %d rows", totalAffected);

        } catch (SQLException e) {
            LOG.errorf(e, "Failed to store batch of %d vectors", vectors.size());
            throw new RuntimeException("Failed to store vector batch", e);
        }
    }

    @Override
    public Uni<List<SearchResult>> search(float[] queryVector, int k) {
        return search(queryVector, k, 0.0);
    }

    @Override
    public Uni<List<SearchResult>> search(float[] queryVector, int k, double similarityThreshold) {
        return Uni.createFrom().completionStage(
            CompletableFuture.supplyAsync(() -> {
                checkDataSourceAvailable();
                validateVector(queryVector);

                // Set HNSW ef_search parameter for optimal performance
                String setEfSearch = "SET hnsw.ef_search = " + efSearch;

                String sql = """
                    SELECT id, content, language, entity_type, entity_name,
                           source_file, start_line, end_line, start_byte, end_byte,
                           embedding, 1 - (embedding <=> ?) AS similarity
                    FROM code_chunks_vectors
                    WHERE 1 - (embedding <=> ?) >= ?
                    ORDER BY embedding <=> ?
                    LIMIT ?
                    """;

                List<SearchResult> results = new ArrayList<>();

                try (Connection conn = dataSource.getConnection()) {

                    // Set the ef_search parameter for this connection
                    try (PreparedStatement setStmt = conn.prepareStatement(setEfSearch)) {
                        setStmt.execute();
                    }

                    try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                        String pgVector = vectorToPgVector(queryVector);

                        stmt.setString(1, pgVector); // for SELECT similarity calculation
                        stmt.setString(2, pgVector); // for WHERE clause
                        stmt.setDouble(3, similarityThreshold);
                        stmt.setString(4, pgVector); // for ORDER BY
                        stmt.setInt(5, k);

                        try (ResultSet rs = stmt.executeQuery()) {
                            while (rs.next()) {
                                VectorMetadata metadata = new VectorMetadata(
                                    rs.getString("content"),
                                    rs.getString("language"),
                                    rs.getString("entity_type"),
                                    rs.getString("entity_name"),
                                    rs.getString("source_file"),
                                    rs.getInt("start_line"),
                                    rs.getInt("end_line"),
                                    rs.getInt("start_byte"),
                                    rs.getInt("end_byte")
                                );

                                float[] vector = pgVectorToFloatArray(rs.getString("embedding"));
                                double similarity = rs.getDouble("similarity");

                                results.add(new SearchResult(
                                    rs.getString("id"),
                                    vector,
                                    metadata,
                                    similarity
                                ));
                            }
                        }
                    }

                    LOG.debugf("Found %d similar vectors for query (ef_search=%d)", results.size(), efSearch);

                } catch (SQLException e) {
                    LOG.errorf(e, "Failed to search vectors");
                    throw new RuntimeException("Failed to search vectors", e);
                }

                return results;
            }, asyncExecutor)
        );
    }

    @Override
    public Uni<Boolean> delete(String id) {
        return Uni.createFrom().completionStage(
            CompletableFuture.supplyAsync(() -> {
                checkDataSourceAvailable();
                String sql = "DELETE FROM code_chunks_vectors WHERE id = ?";

                try (Connection conn = dataSource.getConnection();
                     PreparedStatement stmt = conn.prepareStatement(sql)) {

                    stmt.setString(1, id);
                    int rowsAffected = stmt.executeUpdate();

                    boolean deleted = rowsAffected > 0;
                    LOG.debugf("Deleted vector with id: %s, deleted: %s", id, deleted);

                    return deleted;

                } catch (SQLException e) {
                    LOG.errorf(e, "Failed to delete vector with id: %s", id);
                    throw new RuntimeException("Failed to delete vector", e);
                }
            }, asyncExecutor)
        );
    }

    @Override
    public Uni<Integer> deleteBatch(List<String> ids) {
        return Uni.createFrom().completionStage(
            CompletableFuture.supplyAsync(() -> {
                checkDataSourceAvailable();
                if (ids == null || ids.isEmpty()) {
                    return 0;
                }

                String placeholders = String.join(",", ids.stream().map(id -> "?").toArray(String[]::new));
                String sql = "DELETE FROM code_chunks_vectors WHERE id IN (" + placeholders + ")";

                try (Connection conn = dataSource.getConnection();
                     PreparedStatement stmt = conn.prepareStatement(sql)) {

                    for (int i = 0; i < ids.size(); i++) {
                        stmt.setString(i + 1, ids.get(i));
                    }

                    int rowsAffected = stmt.executeUpdate();
                    LOG.debugf("Deleted %d vectors from batch", rowsAffected);

                    return rowsAffected;

                } catch (SQLException e) {
                    LOG.errorf(e, "Failed to delete batch of %d vectors", ids.size());
                    throw new RuntimeException("Failed to delete vector batch", e);
                }
            }, asyncExecutor)
        );
    }

    @Override
    public int getVectorDimension() {
        return VECTOR_DIMENSION;
    }

    @Override
    public Uni<Boolean> healthCheck() {
        return Uni.createFrom().completionStage(
            CompletableFuture.supplyAsync(() -> {
                if (dataSource == null) {
                    return false;
                }
                String sql = "SELECT COUNT(*) FROM code_chunks_vectors LIMIT 1";

                try (Connection conn = dataSource.getConnection();
                     PreparedStatement stmt = conn.prepareStatement(sql);
                     ResultSet rs = stmt.executeQuery()) {

                    rs.next(); // Just check if we can execute a query
                    return true;

                } catch (SQLException e) {
                    LOG.warnf(e, "Health check failed");
                    return false;
                }
            }, asyncExecutor)
        );
    }

    @Override
    public Uni<VectorStoreStats> getStats() {
        return Uni.createFrom().completionStage(
            CompletableFuture.supplyAsync(() -> {
                checkDataSourceAvailable();
                String sql = """
                    SELECT
                        COUNT(*) as total_vectors,
                        pg_size_pretty(pg_total_relation_size('code_chunks_vectors')) as table_size
                    FROM code_chunks_vectors
                    """;

                try (Connection conn = dataSource.getConnection();
                     PreparedStatement stmt = conn.prepareStatement(sql);
                     ResultSet rs = stmt.executeQuery()) {

                    rs.next();
                    long totalVectors = rs.getLong("total_vectors");

                    // Estimate index size (simplified)
                    long indexSizeBytes = estimateIndexSize(conn);

                    return new VectorStoreStats(
                        totalVectors,
                        VECTOR_DIMENSION,
                        "pgvector",
                        indexSizeBytes
                    );

                } catch (SQLException e) {
                    LOG.errorf(e, "Failed to get vector store stats");
                    return new VectorStoreStats(0, VECTOR_DIMENSION, "pgvector", 0);
                }
            }, asyncExecutor)
        );
    }

    private long estimateIndexSize(Connection conn) throws SQLException {
        // Get size of all indexes on the table
        String sql = """
            SELECT SUM(pg_relation_size(indexrelid)) as index_size
            FROM pg_index i
            JOIN pg_class c ON c.oid = i.indrelid
            WHERE c.relname = 'code_chunks_vectors'
            """;

        try (PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            return rs.next() ? rs.getLong("index_size") : 0;
        }
    }

    private void validateVector(float[] vector) {
        if (vector == null) {
            throw new IllegalArgumentException("Vector cannot be null");
        }
        if (vector.length != VECTOR_DIMENSION) {
            throw new IllegalArgumentException(
                "Vector dimension " + vector.length + " does not match expected dimension " + VECTOR_DIMENSION);
        }
    }

    private void checkDataSourceAvailable() {
        if (dataSource == null) {
            throw new IllegalStateException("No DataSource configured. PgVectorStore requires a PostgreSQL database to be configured.");
        }
    }

    /**
     * Converts a float array to PostgreSQL vector format string.
     * Format: [1.0,2.0,3.0,...]
     */
    private String vectorToPgVector(float[] vector) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < vector.length; i++) {
            if (i > 0) sb.append(",");
            sb.append(vector[i]);
        }
        sb.append("]");
        return sb.toString();
    }

    /**
     * Converts a PostgreSQL vector string back to float array.
     * Expected format: [1.0,2.0,3.0,...]
     */
    private float[] pgVectorToFloatArray(String pgVector) {
        if (pgVector == null || pgVector.length() < 2) {
            throw new IllegalArgumentException("Invalid pgvector format: " + pgVector);
        }

        // Remove brackets and split by comma
        String content = pgVector.substring(1, pgVector.length() - 1);
        if (content.isEmpty()) {
            return new float[0];
        }

        String[] parts = content.split(",");
        float[] result = new float[parts.length];

        for (int i = 0; i < parts.length; i++) {
            result[i] = Float.parseFloat(parts[i].trim());
        }

        return result;
    }
}