/*
 * Copyright (c) 2025 MegaBrain Contributors
 * Licensed under the MIT License - see LICENSE file for details.
 */

package io.megabrain.core;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for LuceneSchema field definitions and utility methods.
 */
public class LuceneSchemaTest {

    @Test
    public void testFieldNameConstants() {
        // Verify all field name constants are defined
        assertNotNull(LuceneSchema.FIELD_CONTENT);
        assertNotNull(LuceneSchema.FIELD_ENTITY_NAME);
        assertNotNull(LuceneSchema.FIELD_ENTITY_NAME_KEYWORD);
        assertNotNull(LuceneSchema.FIELD_LANGUAGE);
        assertNotNull(LuceneSchema.FIELD_ENTITY_TYPE);
        assertNotNull(LuceneSchema.FIELD_FILE_PATH);
        assertNotNull(LuceneSchema.FIELD_REPOSITORY);
        assertNotNull(LuceneSchema.FIELD_DOC_SUMMARY);
        assertNotNull(LuceneSchema.FIELD_START_LINE);
        assertNotNull(LuceneSchema.FIELD_END_LINE);
        assertNotNull(LuceneSchema.FIELD_START_BYTE);
        assertNotNull(LuceneSchema.FIELD_END_BYTE);
        assertNotNull(LuceneSchema.FIELD_DOCUMENT_ID);
    }

    @Test
    public void testFieldTypesAreNotNull() {
        // Verify all field type constants are properly initialized
        assertNotNull(LuceneSchema.CONTENT_FIELD_TYPE);
        assertNotNull(LuceneSchema.ENTITY_NAME_FIELD_TYPE);
        assertNotNull(LuceneSchema.KEYWORD_FIELD_TYPE);
        assertNotNull(LuceneSchema.STORED_ONLY_FIELD_TYPE);
    }

    @Test
    public void testContentFieldTypeConfiguration() {
        var fieldType = LuceneSchema.CONTENT_FIELD_TYPE;

        // Content field should be stored and tokenized for full-text search
        assertTrue(fieldType.stored());
        assertTrue(fieldType.tokenized());

        // Should support positions and offsets for phrase queries
        assertEquals(org.apache.lucene.index.IndexOptions.DOCS_AND_FREQS_AND_POSITIONS_AND_OFFSETS,
                    fieldType.indexOptions());
    }

    @Test
    public void testEntityNameFieldTypeConfiguration() {
        var fieldType = LuceneSchema.ENTITY_NAME_FIELD_TYPE;

        // Entity name should be stored and tokenized for search
        assertTrue(fieldType.stored());
        assertTrue(fieldType.tokenized());

        // Should support positions for phrase queries
        assertEquals(org.apache.lucene.index.IndexOptions.DOCS_AND_FREQS_AND_POSITIONS,
                    fieldType.indexOptions());
    }

    @Test
    public void testKeywordFieldTypeConfiguration() {
        var fieldType = LuceneSchema.KEYWORD_FIELD_TYPE;

        // Keyword fields should be stored but not tokenized
        assertTrue(fieldType.stored());
        assertFalse(fieldType.tokenized());

        // Should be indexed for exact matches and filtering
        assertEquals(org.apache.lucene.index.IndexOptions.DOCS, fieldType.indexOptions());
    }

    @Test
    public void testStoredOnlyFieldTypeConfiguration() {
        var fieldType = LuceneSchema.STORED_ONLY_FIELD_TYPE;

        // Stored-only fields should be stored but not indexed
        assertTrue(fieldType.stored());
        assertFalse(fieldType.tokenized());
        assertEquals(org.apache.lucene.index.IndexOptions.NONE, fieldType.indexOptions());
    }

    @Test
    public void testCreateMetadataFieldName() {
        assertEquals("meta_doc_summary", LuceneSchema.createMetadataFieldName("doc_summary"));
        assertEquals("meta_author", LuceneSchema.createMetadataFieldName("Author"));
        assertEquals("meta_visibility_modifier", LuceneSchema.createMetadataFieldName("visibility-modifier"));
        assertEquals("meta_access_modifier", LuceneSchema.createMetadataFieldName("access_modifier"));
    }

    @Test
    public void testExtractRepositoryFromPath() {
        // Standard Maven/Gradle project structure
        assertEquals("myproject", LuceneSchema.extractRepositoryFromPath("/home/user/projects/myproject/src/main/java/Example.java"));
        assertEquals("myproject", LuceneSchema.extractRepositoryFromPath("myproject/src/main/java/Example.java"));

        // GitHub style paths
        assertEquals("myrepo", LuceneSchema.extractRepositoryFromPath("github.com/owner/myrepo/src/main/java/Example.java"));
        assertEquals("myrepo", LuceneSchema.extractRepositoryFromPath("gitlab.com/owner/myrepo/src/main/java/Example.java"));

        // Simple repo/file patterns
        assertEquals("myrepo", LuceneSchema.extractRepositoryFromPath("owner/myrepo/file.java"));
        assertEquals("myrepo", LuceneSchema.extractRepositoryFromPath("myrepo/file.java"));

        // Edge cases
        assertEquals("unknown", LuceneSchema.extractRepositoryFromPath(""));
        assertEquals("unknown", LuceneSchema.extractRepositoryFromPath(null));
        assertEquals("unknown", LuceneSchema.extractRepositoryFromPath("singlefile.java"));
    }

    @Test
    public void testAttributeConstants() {
        // Verify attribute key constants are defined
        assertNotNull(LuceneSchema.ATTR_DOC_SUMMARY);
        assertNotNull(LuceneSchema.ATTR_REPOSITORY);
        assertNotNull(LuceneSchema.METADATA_FIELD_PREFIX);
    }
}
