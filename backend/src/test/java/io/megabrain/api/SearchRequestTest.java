/*
 * Copyright (c) 2025 MegaBrain Contributors
 * Licensed under the MIT License - see LICENSE file for details.
 */

package io.megabrain.api;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for SearchRequest DTO.
 * Tests filter parameter handling, validation, and multiple value support.
 */
class SearchRequestTest {

    @Test
    @DisplayName("initializes query and empty filters with defaults")
    void constructor_withQuery_initializesFiltersAndDefaults() {
        // When
        SearchRequest actual = new SearchRequest("test query");

        // Then
        assertThat(actual.getQuery()).isEqualTo("test query");
        assertThat(actual.getLanguages()).isEmpty();
        assertThat(actual.getRepositories()).isEmpty();
        assertThat(actual.getFilePaths()).isEmpty();
        assertThat(actual.getEntityTypes()).isEmpty();
        assertThat(actual.getLimit()).isEqualTo(10);
        assertThat(actual.getOffset()).isEqualTo(0);
        assertThat(actual.isTransitive()).isFalse();
    }

    @Test
    @DisplayName("adds languages and sets hasFilters true")
    void addLanguage_addsToLanguagesAndHasFiltersTrue() {
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
    @DisplayName("does not throw for valid request")
    void validate_withValidRequest_doesNotThrow() {
        // Given
        SearchRequest request = new SearchRequest("test");
        request.setLimit(20);
        request.setOffset(10);

        // When/Then
        request.validate(); // Should not throw
    }

    @Test
    @DisplayName("throws when query is blank")
    void validate_withBlankQuery_throwsIllegalArgument() {
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
    @DisplayName("toString includes transitive flag")
    void toString_includesTransitive() {
        // Given
        SearchRequest request = new SearchRequest("q");
        request.setTransitive(true);

        // When
        String actual = request.toString();

        // Then
        assertThat(actual).contains("transitive=true");
    }
}
