/*
 * Copyright (c) 2025 MegaBrain Contributors
 * Licensed under the MIT License - see LICENSE file for details.
 */

package io.megabrain.api;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Represents a single search result from the code search index.
 *
 * This DTO contains the content and metadata of a code chunk that matched
 * a search query, along with relevance scoring and source information.
 * When the result was found via transitive traversal (US-02-06, T6),
 * {@code isTransitive} is true and optional {@code relationshipPath} shows
 * the traversal path (e.g. ["Interface", "AbstractClass", "ConcreteClass"]).
 */
public class SearchResult {

    @JsonProperty("content")
    private final String content;

    @JsonProperty("entity_name")
    private final String entityName;

    @JsonProperty("entity_type")
    private final String entityType;

    @JsonProperty("source_file")
    private final String sourceFile;

    @JsonProperty("language")
    private final String language;

    @JsonProperty("repository")
    private final String repository;

    @JsonProperty("score")
    private final float score;

    @JsonProperty("line_range")
    private final LineRange lineRange;

    @JsonProperty("doc_summary")
    private final String docSummary;

    @JsonProperty("field_match")
    private final FieldMatchInfo fieldMatch;

    /** True when the result was found via transitive relationship traversal (implements/extends). */
    @JsonProperty("is_transitive")
    private final boolean isTransitive;

    /** Traversal path for transitive results, e.g. ["Interface", "AbstractClass", "ConcreteClass"]. Optional. */
    @JsonProperty("relationship_path")
    private final List<String> relationshipPath;

    // Default constructor for Jackson deserialization
    public SearchResult() {
        this.content = "";
        this.entityName = "";
        this.entityType = "";
        this.sourceFile = "";
        this.language = "";
        this.repository = "";
        this.score = 0.0f;
        this.lineRange = new LineRange(1, 1);
        this.docSummary = null;
        this.fieldMatch = null;
        this.isTransitive = false;
        this.relationshipPath = null;
    }

    /**
     * Creates a new SearchResult.
     *
     * @param content the matched content text
     * @param entityName the name of the code entity (class, method, etc.)
     * @param entityType the type of code entity
     * @param sourceFile the source file path
     * @param language the programming language
     * @param repository the repository identifier
     * @param score the relevance score from search
     * @param lineRange the line range information
     * @param docSummary optional documentation summary
     * @param fieldMatch optional field match info (which fields matched and per-field scores); null to omit
     * @param isTransitive true when result was found via transitive traversal (US-02-06, T6)
     * @param relationshipPath optional traversal path for transitive results; null when not transitive or path unknown
     */
    public SearchResult(String content, String entityName, String entityType, String sourceFile,
                       String language, String repository, float score, LineRange lineRange,
                       String docSummary, FieldMatchInfo fieldMatch,
                       boolean isTransitive, List<String> relationshipPath) {
        this.content = content;
        this.entityName = entityName;
        this.entityType = entityType;
        this.sourceFile = sourceFile;
        this.language = language;
        this.repository = repository;
        this.score = score;
        this.lineRange = lineRange;
        this.docSummary = docSummary;
        this.fieldMatch = fieldMatch;
        this.isTransitive = isTransitive;
        this.relationshipPath = relationshipPath;
    }

    /**
     * Creates a SearchResult from the essential fields.
     * Use this for basic search results without detailed metadata.
     *
     * @param content the matched content text
     * @param entityName the name of the code entity
     * @param entityType the type of code entity
     * @param sourceFile the source file path
     * @param language the programming language
     * @param repository the repository identifier
     * @param score the relevance score from search
     * @param lineRange the line range information
     * @return a new SearchResult instance
     */
    public static SearchResult create(String content, String entityName, String entityType,
                                     String sourceFile, String language, String repository,
                                     float score, LineRange lineRange) {
        return new SearchResult(content, entityName, entityType, sourceFile,
                               language, repository, score, lineRange, null, null, false, null);
    }

    public String getContent() {
        return content;
    }

    public String getEntityName() {
        return entityName;
    }

    public String getEntityType() {
        return entityType;
    }

    public String getSourceFile() {
        return sourceFile;
    }

    public String getLanguage() {
        return language;
    }

    public String getRepository() {
        return repository;
    }

    public float getScore() {
        return score;
    }

    public LineRange getLineRange() {
        return lineRange;
    }

    public String getDocSummary() {
        return docSummary;
    }

    /**
     * Optional field match information (US-02-05, T4).
     * Present when requested via {@code include_field_match=true}; shows which fields matched and per-field scores.
     *
     * @return field match info, or null if not requested
     */
    public FieldMatchInfo getFieldMatch() {
        return fieldMatch;
    }

    /**
     * Whether this result was found via transitive relationship traversal (implements/extends) (US-02-06, T6).
     *
     * @return true when the result came from graph transitive closure; false for direct/hybrid search results
     */
    public boolean isTransitive() {
        return isTransitive;
    }

    /**
     * Traversal path for transitive results, e.g. ["Interface", "AbstractClass", "ConcreteClass"].
     * Present only when {@link #isTransitive()} is true and the path is known; null otherwise.
     *
     * @return relationship path, or null
     */
    public List<String> getRelationshipPath() {
        return relationshipPath;
    }

    @Override
    public String toString() {
        String truncatedContent = truncateContent(content, 42);
        String truncatedDocSummary = docSummary != null ?
            docSummary.substring(0, Math.min(30, docSummary.length())) + "..." : null;

        return "SearchResult{" +
                "content='" + truncatedContent + '\'' +
                ", entityName='" + entityName + '\'' +
                ", entityType='" + entityType + '\'' +
                ", sourceFile='" + sourceFile + '\'' +
                ", language='" + language + '\'' +
                ", repository='" + repository + '\'' +
                ", score=" + score +
                ", lineRange=" + lineRange +
                ", docSummary='" + truncatedDocSummary + '\'' +
                ", isTransitive=" + isTransitive +
                ", relationshipPath=" + relationshipPath +
                '}';
    }

    private String truncateContent(String text, int maxLength) {
        if (text == null || text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength) + "...";
    }
}