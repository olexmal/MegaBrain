/*
 * Copyright (c) 2025 MegaBrain Contributors
 * Licensed under the MIT License - see LICENSE file for details.
 */

package io.megabrain.api;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents a single search result from the code search index.
 *
 * This DTO contains the content and metadata of a code chunk that matched
 * a search query, along with relevance scoring and source information.
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
     */
    public SearchResult(String content, String entityName, String entityType, String sourceFile,
                       String language, String repository, float score, LineRange lineRange,
                       String docSummary) {
        this.content = content;
        this.entityName = entityName;
        this.entityType = entityType;
        this.sourceFile = sourceFile;
        this.language = language;
        this.repository = repository;
        this.score = score;
        this.lineRange = lineRange;
        this.docSummary = docSummary;
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
                               language, repository, score, lineRange, null);
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

    @Override
    public String toString() {
        return "SearchResult{" +
                "content='" + content.substring(0, Math.min(50, content.length())) + "...'" +
                ", entityName='" + entityName + '\'' +
                ", entityType='" + entityType + '\'' +
                ", sourceFile='" + sourceFile + '\'' +
                ", language='" + language + '\'' +
                ", repository='" + repository + '\'' +
                ", score=" + score +
                ", lineRange=" + lineRange +
                ", docSummary='" + (docSummary != null ? docSummary.substring(0, Math.min(30, docSummary.length())) + "..." : null) + '\'' +
                '}';
    }
}