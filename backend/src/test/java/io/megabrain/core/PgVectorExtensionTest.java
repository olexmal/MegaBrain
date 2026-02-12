/*
 * Copyright (c) 2025 MegaBrain Contributors
 * Licensed under the MIT License - see LICENSE file for details.
 */

package io.megabrain.core;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test to verify pgvector extension installation and basic functionality.
 * This test uses Testcontainers to run PostgreSQL with pgvector extension.
 */
@QuarkusTest
@Testcontainers(disabledWithoutDocker = true)
public class PgVectorExtensionTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("pgvector/pgvector:pg16")
            .withDatabaseName("testdb")
            .withUsername("testuser")
            .withPassword("testpass");

    @Test
    public void testPgVectorExtensionEnabled() throws Exception {
        // Connect to the test database
        try (Connection conn = DriverManager.getConnection(
                postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
             Statement stmt = conn.createStatement()) {

            // Check if pgvector extension is available
            ResultSet rs = stmt.executeQuery(
                "SELECT name FROM pg_available_extensions WHERE name = 'vector'");
            assertThat(rs.next()).isTrue();
            assertThat(rs.getString("name")).isEqualTo("vector");

            // Enable the extension
            stmt.execute("CREATE EXTENSION IF NOT EXISTS vector");

            // Verify extension is installed
            rs = stmt.executeQuery(
                "SELECT extname FROM pg_extension WHERE extname = 'vector'");
            assertThat(rs.next()).isTrue();
            assertThat(rs.getString("extname")).isEqualTo("vector");

            // Test basic vector operations
            stmt.execute("CREATE TABLE test_vectors (id SERIAL PRIMARY KEY, embedding vector(3))");

            // Insert test vectors
            stmt.execute("INSERT INTO test_vectors (embedding) VALUES ('[1,2,3]'), ('[4,5,6]')");

            // Test cosine similarity
            rs = stmt.executeQuery(
                "SELECT embedding <=> '[1,2,3]' as distance FROM test_vectors ORDER BY distance LIMIT 1");
            assertThat(rs.next()).isTrue();
            assertThat(rs.getDouble("distance")).isEqualTo(0.0); // Same vector should have distance 0

            // Clean up
            stmt.execute("DROP TABLE test_vectors");
        }
    }
}