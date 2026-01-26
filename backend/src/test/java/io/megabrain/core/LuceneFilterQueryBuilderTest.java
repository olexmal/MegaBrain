/*
 * Copyright (c) 2025 MegaBrain Contributors
 * Licensed under the MIT License - see LICENSE file for details.
 */

package io.megabrain.core;

import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for LuceneFilterQueryBuilder (US-02-04, T2).
 */
class LuceneFilterQueryBuilderTest {

    @Nested
    @DisplayName("build returns empty when no filters")
    class NoFilters {

        @Test
        void buildWithNullReturnsEmpty() {
            assertThat(LuceneFilterQueryBuilder.build(null)).isEmpty();
        }

        @Test
        void buildWithEmptyFiltersReturnsEmpty() {
            assertThat(LuceneFilterQueryBuilder.build(SearchFilters.empty())).isEmpty();
        }

        @Test
        void buildWithAllEmptyListsReturnsEmpty() {
            SearchFilters f = new SearchFilters(List.of(), List.of(), List.of(), List.of());
            assertThat(LuceneFilterQueryBuilder.build(f)).isEmpty();
        }
    }

    @Nested
    @DisplayName("single-dimension filters")
    class SingleDimension {

        @Test
        void languageFilterProducesFilterQuery() {
            SearchFilters f = new SearchFilters(List.of("java"), List.of(), List.of(), List.of());
            Query q = LuceneFilterQueryBuilder.build(f).orElseThrow();
            assertThat(q).isInstanceOf(BooleanQuery.class);
            assertThat(q.toString()).contains(LuceneSchema.FIELD_LANGUAGE).contains("java");
        }

        @Test
        void repositoryFilterProducesFilterQuery() {
            SearchFilters f = new SearchFilters(List.of(), List.of("my-repo"), List.of(), List.of());
            Query q = LuceneFilterQueryBuilder.build(f).orElseThrow();
            assertThat(q).isInstanceOf(BooleanQuery.class);
            assertThat(q.toString()).contains(LuceneSchema.FIELD_REPOSITORY).contains("my-repo");
        }

        @Test
        void filePathFilterProducesPrefixFilterQuery() {
            SearchFilters f = new SearchFilters(List.of(), List.of(), List.of("src/auth/"), List.of());
            Query q = LuceneFilterQueryBuilder.build(f).orElseThrow();
            assertThat(q).isInstanceOf(BooleanQuery.class);
            assertThat(q.toString()).contains(LuceneSchema.FIELD_FILE_PATH).contains("src/auth");
        }

        @Test
        void entityTypeFilterProducesFilterQuery() {
            SearchFilters f = new SearchFilters(List.of(), List.of(), List.of(), List.of("method"));
            Query q = LuceneFilterQueryBuilder.build(f).orElseThrow();
            assertThat(q).isInstanceOf(BooleanQuery.class);
            assertThat(q.toString()).contains(LuceneSchema.FIELD_ENTITY_TYPE).contains("method");
        }
    }

    @Nested
    @DisplayName("multiple values per dimension (OR)")
    class MultipleValuesPerDimension {

        @Test
        void multipleLanguagesProduceBooleanQueryWithShould() {
            SearchFilters f = new SearchFilters(
                    List.of("java", "python"), List.of(), List.of(), List.of());
            Query q = LuceneFilterQueryBuilder.build(f).orElseThrow();
            assertThat(q).isInstanceOf(BooleanQuery.class);
            assertThat(q.toString()).contains("java").contains("python").contains(LuceneSchema.FIELD_LANGUAGE);
        }

        @Test
        void multipleFilePathsProduceBooleanQueryWithPrefixQueries() {
            SearchFilters f = new SearchFilters(
                    List.of(), List.of(), List.of("src/", "test/"), List.of());
            Query q = LuceneFilterQueryBuilder.build(f).orElseThrow();
            assertThat(q).isInstanceOf(BooleanQuery.class);
            assertThat(q.toString()).contains(LuceneSchema.FIELD_FILE_PATH).contains("src").contains("test");
        }
    }

    @Nested
    @DisplayName("combined dimensions (AND)")
    class CombinedDimensions {

        @Test
        void languageAndEntityTypeProduceBooleanQueryWithMustClauses() {
            SearchFilters f = new SearchFilters(
                    List.of("java"), List.of(), List.of(), List.of("class"));
            Query q = LuceneFilterQueryBuilder.build(f).orElseThrow();
            assertThat(q).isInstanceOf(BooleanQuery.class);
            BooleanQuery bq = (BooleanQuery) q;
            assertThat(bq.clauses()).hasSize(2);
            assertThat(q.toString()).contains(LuceneSchema.FIELD_LANGUAGE)
                    .contains("java")
                    .contains(LuceneSchema.FIELD_ENTITY_TYPE)
                    .contains("class");
        }

        @Test
        void allDimensionsProduceCombinedFilter() {
            SearchFilters f = new SearchFilters(
                    List.of("java"),
                    List.of("backend"),
                    List.of("src/main/"),
                    List.of("method"));
            Query q = LuceneFilterQueryBuilder.build(f).orElseThrow();
            assertThat(q).isInstanceOf(BooleanQuery.class);
            BooleanQuery bq = (BooleanQuery) q;
            assertThat(bq.clauses()).hasSize(4);
            String s = q.toString();
            assertThat(s).contains(LuceneSchema.FIELD_LANGUAGE).contains("java")
                    .contains(LuceneSchema.FIELD_REPOSITORY).contains("backend")
                    .contains(LuceneSchema.FIELD_FILE_PATH).contains("src/main")
                    .contains(LuceneSchema.FIELD_ENTITY_TYPE).contains("method");
        }
    }

    @Nested
    @DisplayName("SearchFilters record")
    class SearchFiltersRecord {

        @Test
        void hasFiltersReturnsFalseWhenEmpty() {
            assertThat(SearchFilters.empty().hasFilters()).isFalse();
        }

        @Test
        void hasFiltersReturnsTrueWhenAnyDimensionSet() {
            assertThat(new SearchFilters(List.of("java"), List.of(), List.of(), List.of()).hasFilters()).isTrue();
            assertThat(new SearchFilters(List.of(), List.of("r"), List.of(), List.of()).hasFilters()).isTrue();
            assertThat(new SearchFilters(List.of(), List.of(), List.of("p"), List.of()).hasFilters()).isTrue();
            assertThat(new SearchFilters(List.of(), List.of(), List.of(), List.of("t")).hasFilters()).isTrue();
        }

        @Test
        void nullListsBecomeEmpty() {
            SearchFilters f = new SearchFilters(null, null, null, null);
            assertThat(f.languages()).isEmpty();
            assertThat(f.repositories()).isEmpty();
            assertThat(f.filePaths()).isEmpty();
            assertThat(f.entityTypes()).isEmpty();
            assertThat(f.hasFilters()).isFalse();
        }
    }
}
