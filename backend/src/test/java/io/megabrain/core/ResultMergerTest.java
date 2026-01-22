/*
 * Copyright (c) 2025 MegaBrain Contributors
 * Licensed under the MIT License - see LICENSE file for details.
 */

package io.megabrain.core;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.assertj.core.data.Offset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link ResultMerger}.
 * Covers result merging, deduplication, score combination, and sorting.
 */
@QuarkusTest
class ResultMergerTest {

    @Inject
    ResultMerger resultMerger;

    @Inject
    HybridScorer hybridScorer;

    @BeforeEach
    void setUp() {
        // Tests use default weights (0.6 keyword, 0.4 vector)
    }

    @Test
    void merge_emptyBothReturnsEmpty() {
        List<ResultMerger.MergedResult> results = resultMerger.merge(List.of(), List.of());
        assertThat(results).isEmpty();
    }

    @Test
    void merge_nullBothReturnsEmpty() {
        List<ResultMerger.MergedResult> results = resultMerger.merge(null, null);
        assertThat(results).isEmpty();
    }

    @Test
    void merge_luceneOnlyReturnsLuceneResults() {
        Document doc1 = createLuceneDocument("file1.java", "Class1", "class1:Class1:10:20");
        Document doc2 = createLuceneDocument("file2.java", "Method1", "class2:Method1:5:10");

        List<LuceneIndexService.LuceneScoredResult> luceneResults = List.of(
                new LuceneIndexService.LuceneScoredResult(doc1, 0.8f),
                new LuceneIndexService.LuceneScoredResult(doc2, 0.6f)
        );

        List<ResultMerger.MergedResult> results = resultMerger.merge(luceneResults, List.of());

        assertThat(results).hasSize(2);
        assertThat(results.get(0).combinedScore()).isCloseTo(0.8, Offset.offset(0.001));
        assertThat(results.get(1).combinedScore()).isCloseTo(0.6, Offset.offset(0.001));
        assertThat(results.get(0).luceneDocument()).isNotNull();
        assertThat(results.get(0).vectorResult()).isNull();
        assertThat(results.get(0).fromBothSources()).isFalse();
    }

    @Test
    void merge_vectorOnlyReturnsVectorResults() {
        VectorStore.SearchResult result1 = createVectorResult("file1.java", "Class1", 0.9);
        VectorStore.SearchResult result2 = createVectorResult("file2.java", "Method1", 0.7);

        List<VectorStore.SearchResult> vectorResults = List.of(result1, result2);

        List<ResultMerger.MergedResult> results = resultMerger.merge(List.of(), vectorResults);

        assertThat(results).hasSize(2);
        assertThat(results.get(0).combinedScore()).isCloseTo(0.9, Offset.offset(0.001));
        assertThat(results.get(1).combinedScore()).isCloseTo(0.7, Offset.offset(0.001));
        assertThat(results.get(0).luceneDocument()).isNull();
        assertThat(results.get(0).vectorResult()).isNotNull();
        assertThat(results.get(0).fromBothSources()).isFalse();
    }

    @Test
    void merge_bothSourcesNoDuplicates() {
        Document doc1 = createLuceneDocument("file1.java", "Class1", "file1.java:Class1:10:20");
        Document doc2 = createLuceneDocument("file2.java", "Method1", "file2.java:Method1:5:10");

        VectorStore.SearchResult vector1 = createVectorResult("file3.java", "Class2", 0.9);
        VectorStore.SearchResult vector2 = createVectorResult("file4.java", "Method2", 0.7);

        List<LuceneIndexService.LuceneScoredResult> luceneResults = List.of(
                new LuceneIndexService.LuceneScoredResult(doc1, 0.8f),
                new LuceneIndexService.LuceneScoredResult(doc2, 0.6f)
        );

        List<VectorStore.SearchResult> vectorResults = List.of(vector1, vector2);

        List<ResultMerger.MergedResult> results = resultMerger.merge(luceneResults, vectorResults);

        assertThat(results).hasSize(4);
        // Should be sorted by score descending
        assertThat(results.get(0).combinedScore()).isCloseTo(0.9, Offset.offset(0.001));
        assertThat(results.get(1).combinedScore()).isCloseTo(0.8, Offset.offset(0.001));
        assertThat(results.get(2).combinedScore()).isCloseTo(0.7, Offset.offset(0.001));
        assertThat(results.get(3).combinedScore()).isCloseTo(0.6, Offset.offset(0.001));
    }

    @Test
    void merge_duplicateChunkCombinesScores() {
        // Same chunk appears in both result sets
        Document doc = createLuceneDocument("file1.java", "Class1", "file1.java:Class1:10:20");
        VectorStore.SearchResult vector = createVectorResult("file1.java", "Class1", 0.9, 10, 20);

        List<LuceneIndexService.LuceneScoredResult> luceneResults = List.of(
                new LuceneIndexService.LuceneScoredResult(doc, 0.8f)
        );

        List<VectorStore.SearchResult> vectorResults = List.of(vector);

        List<ResultMerger.MergedResult> results = resultMerger.merge(luceneResults, vectorResults);

        assertThat(results).hasSize(1);
        ResultMerger.MergedResult merged = results.get(0);
        assertThat(merged.fromBothSources()).isTrue();
        assertThat(merged.luceneDocument()).isNotNull();
        assertThat(merged.vectorResult()).isNotNull();

        // Combined score: 0.6 * 0.8 + 0.4 * 0.9 = 0.48 + 0.36 = 0.84
        double expectedScore = hybridScorer.combine(0.8, 0.9);
        assertThat(merged.combinedScore()).isCloseTo(expectedScore, Offset.offset(0.001));
    }

    @Test
    void merge_multipleDuplicatesCombinesCorrectly() {
        Document doc1 = createLuceneDocument("file1.java", "Class1", "file1.java:Class1:10:20");
        Document doc2 = createLuceneDocument("file2.java", "Method1", "file2.java:Method1:5:10");

        VectorStore.SearchResult vector1 = createVectorResult("file1.java", "Class1", 0.9, 10, 20);
        VectorStore.SearchResult vector2 = createVectorResult("file2.java", "Method1", 0.7, 5, 10);
        VectorStore.SearchResult vector3 = createVectorResult("file3.java", "Class2", 0.6);

        List<LuceneIndexService.LuceneScoredResult> luceneResults = List.of(
                new LuceneIndexService.LuceneScoredResult(doc1, 0.8f),
                new LuceneIndexService.LuceneScoredResult(doc2, 0.5f)
        );

        List<VectorStore.SearchResult> vectorResults = List.of(vector1, vector2, vector3);

        List<ResultMerger.MergedResult> results = resultMerger.merge(luceneResults, vectorResults);

        assertThat(results).hasSize(3); // 2 duplicates + 1 unique

        // Find the merged results
        ResultMerger.MergedResult merged1 = results.stream()
                .filter(r -> r.chunkId().contains("file1.java:Class1"))
                .findFirst().orElseThrow();
        ResultMerger.MergedResult merged2 = results.stream()
                .filter(r -> r.chunkId().contains("file2.java:Method1"))
                .findFirst().orElseThrow();
        ResultMerger.MergedResult unique = results.stream()
                .filter(r -> r.chunkId().contains("file3.java:Class2"))
                .findFirst().orElseThrow();

        assertThat(merged1.fromBothSources()).isTrue();
        assertThat(merged2.fromBothSources()).isTrue();
        assertThat(unique.fromBothSources()).isFalse();

        // Verify combined scores
        double expected1 = hybridScorer.combine(0.8, 0.9);
        double expected2 = hybridScorer.combine(0.5, 0.7);
        assertThat(merged1.combinedScore()).isCloseTo(expected1, Offset.offset(0.001));
        assertThat(merged2.combinedScore()).isCloseTo(expected2, Offset.offset(0.001));
        assertThat(unique.combinedScore()).isCloseTo(0.6, Offset.offset(0.001));
    }

    @Test
    void merge_sortedByCombinedScoreDescending() {
        Document doc1 = createLuceneDocument("file1.java", "Class1", "file1.java:Class1:10:20");
        Document doc2 = createLuceneDocument("file2.java", "Method1", "file2.java:Method1:5:10");

        VectorStore.SearchResult vector1 = createVectorResult("file1.java", "Class1", 0.5, 10, 20);
        VectorStore.SearchResult vector2 = createVectorResult("file2.java", "Method1", 0.9, 5, 10);

        List<LuceneIndexService.LuceneScoredResult> luceneResults = List.of(
                new LuceneIndexService.LuceneScoredResult(doc1, 0.9f), // High Lucene score
                new LuceneIndexService.LuceneScoredResult(doc2, 0.3f) // Low Lucene score
        );

        List<VectorStore.SearchResult> vectorResults = List.of(vector1, vector2);

        List<ResultMerger.MergedResult> results = resultMerger.merge(luceneResults, vectorResults);

        assertThat(results).hasSize(2);

        // file2 should be first: combine(0.3, 0.9) = 0.6 * 0.3 + 0.4 * 0.9 = 0.18 + 0.36 = 0.54
        // file1 should be second: combine(0.9, 0.5) = 0.6 * 0.9 + 0.4 * 0.5 = 0.54 + 0.20 = 0.74
        // Actually, file1 should be first (0.74 > 0.54)
        double score1 = hybridScorer.combine(0.9, 0.5);
        double score2 = hybridScorer.combine(0.3, 0.9);

        assertThat(results.get(0).combinedScore()).isGreaterThanOrEqualTo(results.get(1).combinedScore());
        assertThat(results.get(0).combinedScore()).isCloseTo(Math.max(score1, score2), Offset.offset(0.001));
    }

    @Test
    void merge_allDuplicatesStillDeduplicates() {
        // All chunks appear in both result sets
        Document doc1 = createLuceneDocument("file1.java", "Class1", "file1.java:Class1:10:20");
        Document doc2 = createLuceneDocument("file2.java", "Method1", "file2.java:Method1:5:10");

        VectorStore.SearchResult vector1 = createVectorResult("file1.java", "Class1", 0.9, 10, 20);
        VectorStore.SearchResult vector2 = createVectorResult("file2.java", "Method1", 0.7, 5, 10);

        List<LuceneIndexService.LuceneScoredResult> luceneResults = List.of(
                new LuceneIndexService.LuceneScoredResult(doc1, 0.8f),
                new LuceneIndexService.LuceneScoredResult(doc2, 0.6f)
        );

        List<VectorStore.SearchResult> vectorResults = List.of(vector1, vector2);

        List<ResultMerger.MergedResult> results = resultMerger.merge(luceneResults, vectorResults);

        assertThat(results).hasSize(2); // Deduplicated
        assertThat(results.get(0).fromBothSources()).isTrue();
        assertThat(results.get(1).fromBothSources()).isTrue();
    }

    @Test
    void merge_luceneEmptyVectorOnly() {
        VectorStore.SearchResult result = createVectorResult("file1.java", "Class1", 0.9);

        List<ResultMerger.MergedResult> results = resultMerger.merge(List.of(), List.of(result));

        assertThat(results).hasSize(1);
        assertThat(results.get(0).combinedScore()).isCloseTo(0.9, Offset.offset(0.001));
        assertThat(results.get(0).vectorResult()).isNotNull();
        assertThat(results.get(0).luceneDocument()).isNull();
    }

    @Test
    void merge_vectorEmptyLuceneOnly() {
        Document doc = createLuceneDocument("file1.java", "Class1", "file1.java:Class1:10:20");

        List<ResultMerger.MergedResult> results = resultMerger.merge(
                List.of(new LuceneIndexService.LuceneScoredResult(doc, 0.8f)),
                List.of()
        );

        assertThat(results).hasSize(1);
        assertThat(results.get(0).combinedScore()).isCloseTo(0.8, Offset.offset(0.001));
        assertThat(results.get(0).luceneDocument()).isNotNull();
        assertThat(results.get(0).vectorResult()).isNull();
    }

    @Test
    void merge_chunkIdExtractionFromDocumentId() {
        // Test that document_id field is used when available
        Document doc = createLuceneDocument("file1.java", "Class1", "custom:document:id:123");

        List<ResultMerger.MergedResult> results = resultMerger.merge(
                List.of(new LuceneIndexService.LuceneScoredResult(doc, 0.8f)),
                List.of()
        );

        assertThat(results).hasSize(1);
        assertThat(results.get(0).chunkId()).isEqualTo("custom:document:id:123");
    }

    @Test
    void merge_chunkIdExtractionFromFilePathAndEntityName() {
        // Test fallback to file_path + entity_name when document_id not available
        Document doc = new Document();
        doc.add(new Field(LuceneSchema.FIELD_FILE_PATH, "file1.java", LuceneSchema.KEYWORD_FIELD_TYPE));
        doc.add(new Field(LuceneSchema.FIELD_ENTITY_NAME, "Class1", LuceneSchema.KEYWORD_FIELD_TYPE));
        // No document_id field

        List<ResultMerger.MergedResult> results = resultMerger.merge(
                List.of(new LuceneIndexService.LuceneScoredResult(doc, 0.8f)),
                List.of()
        );

        assertThat(results).hasSize(1);
        assertThat(results.get(0).chunkId()).isEqualTo("file1.java:Class1");
    }

    @Test
    void merge_chunkIdMatchingForDeduplication() {
        // Test that chunks with same file_path + entity_name + line range are matched
        Document doc = createLuceneDocument("file1.java", "Class1", "file1.java:Class1:10:20");
        VectorStore.SearchResult vector = createVectorResult("file1.java", "Class1", 0.9, 10, 20);

        List<ResultMerger.MergedResult> results = resultMerger.merge(
                List.of(new LuceneIndexService.LuceneScoredResult(doc, 0.8f)),
                List.of(vector)
        );

        assertThat(results).hasSize(1);
        assertThat(results.get(0).fromBothSources()).isTrue();
    }

    // Helper methods

    private Document createLuceneDocument(String filePath, String entityName, String documentId) {
        Document doc = new Document();
        doc.add(new Field(LuceneSchema.FIELD_FILE_PATH, filePath, LuceneSchema.KEYWORD_FIELD_TYPE));
        doc.add(new Field(LuceneSchema.FIELD_ENTITY_NAME, entityName, LuceneSchema.KEYWORD_FIELD_TYPE));
        doc.add(new Field(LuceneSchema.FIELD_DOCUMENT_ID, documentId, LuceneSchema.KEYWORD_FIELD_TYPE));
        return doc;
    }

    private VectorStore.SearchResult createVectorResult(String filePath, String entityName, double similarity) {
        return createVectorResult(filePath, entityName, similarity, null, null);
    }

    private VectorStore.SearchResult createVectorResult(String filePath, String entityName, double similarity,
                                                        Integer startLine, Integer endLine) {
        VectorStore.VectorMetadata metadata = new VectorStore.VectorMetadata(
                "content",
                "java",
                "class",
                entityName,
                filePath,
                startLine != null ? startLine : 1,
                endLine != null ? endLine : 10,
                0,
                100
        );
        return new VectorStore.SearchResult(
                "vector-id-" + filePath + ":" + entityName,
                new float[384],
                metadata,
                similarity
        );
    }
}
