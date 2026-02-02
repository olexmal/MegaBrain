/*
 * Copyright (c) 2025 MegaBrain Contributors
 * Licensed under the MIT License - see LICENSE file for details.
 */

package io.megabrain.api;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for SearchRequest DTO.
 * Tests filter parameter handling, validation, and multiple value support.
 */
class SearchRequestTest {

    @Test
    void constructor_withQuery_shouldInitializeFilters() {
        // When
        SearchRequest request = new SearchRequest("test query");

        // Then
        assertThat(request.getQuery()).isEqualTo("test query");
        assertThat(request.getLanguages()).isEmpty();
        assertThat(request.getRepositories()).isEmpty();
        assertThat(request.getFilePaths()).isEmpty();
        assertThat(request.getEntityTypes()).isEmpty();
        assertThat(request.getLimit()).isEqualTo(10);
        assertThat(request.getOffset()).isEqualTo(0);
        assertThat(request.isTransitive()).isFalse();
    }

    @Test
    void addLanguage_shouldAddToLanguagesList() {
        // Given
        SearchRequest request = new SearchRequest("test");

        // When
        request.addLanguage("java");
        request.addLanguage("python");

        // Then
        assertThat(request.getLanguages()).containsExactly("java", "python");
        assertThat(request.hasFilters()).isTrue();
    }

    @Test
    void validate_withValidRequest_shouldNotThrow() {
        // Given
        SearchRequest request = new SearchRequest("test");
        request.setLimit(20);
        request.setOffset(10);

        // When/Then
        request.validate(); // Should not throw
    }

    @Test
    void validate_withBlankQuery_shouldThrow() {
        // Given
        SearchRequest request = new SearchRequest("test");
        request.setQuery("   ");

        // When/Then
        assertThatThrownBy(request::validate)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("blank");
    }

    @Test
    void transitive_defaultsToFalse() {
        // When
        SearchRequest request = new SearchRequest("test");

        // Then
        assertThat(request.isTransitive()).isFalse();
    }

    @Test
    void setTransitive_true_setsTransitive() {
        // Given
        SearchRequest request = new SearchRequest("test");

        // When
        request.setTransitive(true);

        // Then
        assertThat(request.isTransitive()).isTrue();
    }

    @Test
    void setTransitive_false_keepsFalse() {
        // Given
        SearchRequest request = new SearchRequest("test");
        request.setTransitive(true);

        // When
        request.setTransitive(false);

        // Then
        assertThat(request.isTransitive()).isFalse();
    }

    @Test
    void toString_includesTransitive() {
        // Given
        SearchRequest request = new SearchRequest("q");
        request.setTransitive(true);

        // When
        String s = request.toString();

        // Then
        assertThat(s).contains("transitive=true");
    }
}
