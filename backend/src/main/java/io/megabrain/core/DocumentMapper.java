/*
 * Copyright (c) 2025 MegaBrain Contributors
 * Licensed under the MIT License - see LICENSE file for details.
 */

package io.megabrain.core;

import io.megabrain.ingestion.parser.TextChunk;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;

/**
 * Utility class for mapping TextChunk objects to Lucene Documents.
 *
 * This class handles the conversion of TextChunk objects into Lucene Documents
 * with proper field mapping and metadata extraction. It encapsulates all
 * document creation logic for better testability and maintainability.
 */
public final class DocumentMapper {

    // Prevent instantiation
    private DocumentMapper() {}

    /**
     * Converts a TextChunk into a Lucene Document.
     *
     * @param chunk the TextChunk to convert
     * @return the corresponding Lucene Document
     */
    public static Document toDocument(TextChunk chunk) {
        Document doc = new Document();

        // Core content and metadata fields
        doc.add(new Field(LuceneSchema.FIELD_CONTENT, chunk.content(), LuceneSchema.CONTENT_FIELD_TYPE));
        doc.add(new Field(LuceneSchema.FIELD_ENTITY_NAME, chunk.entityName(), LuceneSchema.ENTITY_NAME_FIELD_TYPE));
        doc.add(new Field(LuceneSchema.FIELD_ENTITY_NAME_KEYWORD, chunk.entityName(), LuceneSchema.KEYWORD_FIELD_TYPE));
        doc.add(new Field(LuceneSchema.FIELD_LANGUAGE, chunk.language(), LuceneSchema.KEYWORD_FIELD_TYPE));
        doc.add(new Field(LuceneSchema.FIELD_ENTITY_TYPE, chunk.entityType(), LuceneSchema.KEYWORD_FIELD_TYPE));
        doc.add(new Field(LuceneSchema.FIELD_FILE_PATH, chunk.sourceFile(), LuceneSchema.KEYWORD_FIELD_TYPE));

        // Repository extraction
        String repository = LuceneSchema.extractRepositoryFromPath(chunk.sourceFile());
        doc.add(new Field(LuceneSchema.FIELD_REPOSITORY, repository, LuceneSchema.KEYWORD_FIELD_TYPE));

        // Line and byte information (stored only)
        doc.add(new Field(LuceneSchema.FIELD_START_LINE, String.valueOf(chunk.startLine()), LuceneSchema.STORED_ONLY_FIELD_TYPE));
        doc.add(new Field(LuceneSchema.FIELD_END_LINE, String.valueOf(chunk.endLine()), LuceneSchema.STORED_ONLY_FIELD_TYPE));
        doc.add(new Field(LuceneSchema.FIELD_START_BYTE, String.valueOf(chunk.startByte()), LuceneSchema.STORED_ONLY_FIELD_TYPE));
        doc.add(new Field(LuceneSchema.FIELD_END_BYTE, String.valueOf(chunk.endByte()), LuceneSchema.STORED_ONLY_FIELD_TYPE));

        // Dynamic metadata fields from attributes
        if (chunk.attributes() != null) {
            for (var entry : chunk.attributes().entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();

                if (LuceneSchema.ATTR_DOC_SUMMARY.equals(key)) {
                    // Special handling for doc summary - make it searchable
                    doc.add(new Field(LuceneSchema.FIELD_DOC_SUMMARY, value, LuceneSchema.CONTENT_FIELD_TYPE));
                } else {
                    // Regular metadata fields as keywords
                    String fieldName = LuceneSchema.createMetadataFieldName(key);
                    doc.add(new Field(fieldName, value, LuceneSchema.KEYWORD_FIELD_TYPE));
                }
            }
        }

        return doc;
    }

    /**
     * Generates a unique document ID for the chunk.
     * This ID is used for document updates and deletions.
     *
     * @param chunk the TextChunk
     * @return unique document identifier
     */
    public static String generateDocumentId(TextChunk chunk) {
        return String.format("%s:%s:%d:%d",
                chunk.sourceFile(),
                chunk.entityName(),
                (int) chunk.startLine(),
                (int) chunk.endLine());
    }

    /**
     * Creates a document with its unique ID field added.
     * This is a convenience method for indexing operations.
     *
     * @param chunk the TextChunk to convert
     * @return the Lucene Document with document ID field
     */
    public static Document toDocumentWithId(TextChunk chunk) {
        Document doc = toDocument(chunk);
        String documentId = generateDocumentId(chunk);
        doc.add(new Field(LuceneSchema.FIELD_DOCUMENT_ID, documentId, LuceneSchema.KEYWORD_FIELD_TYPE));
        return doc;
    }
}
