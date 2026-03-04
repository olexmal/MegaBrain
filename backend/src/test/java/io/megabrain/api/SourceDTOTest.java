/*
 * Copyright (c) 2025 MegaBrain Contributors
 * Licensed under the MIT License - see LICENSE file for details.
 */

package io.megabrain.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.megabrain.core.ExtractedCitation;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for SourceDTO (US-03-05 T4).
 */
class SourceDTOTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("fromSearchResult maps SearchResult to SourceDTO with all fields")
    void fromSearchResult_mapsAllFields() {
        LineRange range = new LineRange(10, 25);
        SearchResult r = SearchResult.create(
                "content", "MyClass", "class", "src/MyClass.java", "java", "my-repo",
                0.85f, range);

        SourceDTO dto = SourceDTO.fromSearchResult(r, "chunk-0");

        assertThat(dto.filePath()).isEqualTo("src/MyClass.java");
        assertThat(dto.entityName()).isEqualTo("MyClass");
        assertThat(dto.lineStart()).isEqualTo(10);
        assertThat(dto.lineEnd()).isEqualTo(25);
        assertThat(dto.relevanceScore()).isEqualTo(0.85f);
        assertThat(dto.repository()).isEqualTo("my-repo");
        assertThat(dto.language()).isEqualTo("java");
        assertThat(dto.chunkId()).isEqualTo("chunk-0");
    }

    @Test
    @DisplayName("fromCitation maps ExtractedCitation to SourceDTO")
    void fromCitation_mapsCitation() {
        ExtractedCitation c = new ExtractedCitation("path/to/File.java", 42, 42, "[Source: path/to/File.java:42]");

        SourceDTO dto = SourceDTO.fromCitation(c);

        assertThat(dto.filePath()).isEqualTo("path/to/File.java");
        assertThat(dto.entityName()).isNull();
        assertThat(dto.lineStart()).isEqualTo(42);
        assertThat(dto.lineEnd()).isEqualTo(42);
        assertThat(dto.relevanceScore()).isNull();
        assertThat(dto.repository()).isNull();
        assertThat(dto.language()).isNull();
        assertThat(dto.chunkId()).isNull();
    }

    @Test
    @DisplayName("serializes to JSON with snake_case and omits nulls")
    void serialization_toJson_snakeCaseAndNonNull() throws Exception {
        SourceDTO dto = new SourceDTO(
                "src/A.java", "A", 1, 10, 0.9f, "repo", "java", "chunk-0");

        String json = objectMapper.writeValueAsString(dto);

        assertThat(json).contains("\"file_path\":\"src/A.java\"");
        assertThat(json).contains("\"entity_name\":\"A\"");
        assertThat(json).contains("\"line_start\":1");
        assertThat(json).contains("\"line_end\":10");
        assertThat(json).contains("\"relevance_score\":0.9");
        assertThat(json).contains("\"repository\":\"repo\"");
        assertThat(json).contains("\"language\":\"java\"");
        assertThat(json).contains("\"chunk_id\":\"chunk-0\"");
    }

    @Test
    @DisplayName("deserializes from JSON")
    void deserialization_fromJson() throws Exception {
        String json = "{\"file_path\":\"p.java\",\"entity_name\":\"X\",\"line_start\":5,\"line_end\":15,\"relevance_score\":0.7,\"repository\":\"r\",\"language\":\"java\",\"chunk_id\":\"c1\"}";

        SourceDTO dto = objectMapper.readValue(json, SourceDTO.class);

        assertThat(dto.filePath()).isEqualTo("p.java");
        assertThat(dto.entityName()).isEqualTo("X");
        assertThat(dto.lineStart()).isEqualTo(5);
        assertThat(dto.lineEnd()).isEqualTo(15);
        assertThat(dto.relevanceScore()).isEqualTo(0.7f);
        assertThat(dto.repository()).isEqualTo("r");
        assertThat(dto.language()).isEqualTo("java");
        assertThat(dto.chunkId()).isEqualTo("c1");
    }

    @Test
    @DisplayName("null filePath is defaulted to empty string")
    void constructor_nullFilePath_defaultsToEmpty() {
        SourceDTO dto = new SourceDTO(null, null, null, null, null, null, null, null);
        assertThat(dto.filePath()).isEmpty();
    }
}
