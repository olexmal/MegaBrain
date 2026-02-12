/*
 * Copyright (c) 2025 MegaBrain Contributors
 * Licensed under the MIT License - see LICENSE file for details.
 */

package io.megabrain.core;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for StructuralQueryParser (US-02-06, T3/T4).
 */
class StructuralQueryParserTest {

    @Test
    @DisplayName("returns interface name for implements: query")
    void parseImplementsTarget_implementsQuery_returnsInterfaceName() {
        // Given
        String query = "implements:IRepository";

        // When
        Optional<String> actual = StructuralQueryParser.parseImplementsTarget(query);

        // Then
        Optional<String> expected = Optional.of("IRepository");
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    @DisplayName("returns first token when query has trailing terms")
    void parseImplementsTarget_withTrailingTerms_returnsFirstToken() {
        // Given
        String query = "implements:IRepository other terms";

        // When
        Optional<String> actual = StructuralQueryParser.parseImplementsTarget(query);

        // Then
        Optional<String> expected = Optional.of("IRepository");
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    @DisplayName("returns empty for extends query")
    void parseImplementsTarget_extendsQuery_returnsEmpty() {
        // Given
        String query = "extends:Base";

        // When
        Optional<String> actual = StructuralQueryParser.parseImplementsTarget(query);

        // Then
        assertThat(actual).isEmpty();
    }

    @Test
    @DisplayName("returns empty for plain query")
    void parseImplementsTarget_plainQuery_returnsEmpty() {
        // Given
        String query = "some query";

        // When
        Optional<String> actual = StructuralQueryParser.parseImplementsTarget(query);

        // Then
        assertThat(actual).isEmpty();
    }

    @Test
    @DisplayName("returns empty for null input")
    void parseImplementsTarget_nullInput_returnsEmpty() {
        // Given
        String query = null;

        // When
        Optional<String> actual = StructuralQueryParser.parseImplementsTarget(query);

        // Then
        assertThat(actual).isEmpty();
    }

    @Test
    @DisplayName("returns empty when nothing after implements: prefix")
    void parseImplementsTarget_blankAfterPrefix_returnsEmpty() {
        // Given
        String query = "implements:";

        // When
        Optional<String> actual = StructuralQueryParser.parseImplementsTarget(query);

        // Then
        assertThat(actual).isEmpty();
    }

    @Test
    @DisplayName("returns class name for extends: query")
    void parseExtendsTarget_extendsQuery_returnsClassName() {
        // Given
        String query = "extends:BaseClass";

        // When
        Optional<String> actual = StructuralQueryParser.parseExtendsTarget(query);

        // Then
        Optional<String> expected = Optional.of("BaseClass");
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    @DisplayName("returns first token when query has trailing terms")
    void parseExtendsTarget_withTrailingTerms_returnsFirstToken() {
        // Given
        String query = "extends:Base other";

        // When
        Optional<String> actual = StructuralQueryParser.parseExtendsTarget(query);

        // Then
        Optional<String> expected = Optional.of("Base");
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    @DisplayName("returns empty for implements query")
    void parseExtendsTarget_implementsQuery_returnsEmpty() {
        // Given
        String query = "implements:IRepo";

        // When
        Optional<String> actual = StructuralQueryParser.parseExtendsTarget(query);

        // Then
        assertThat(actual).isEmpty();
    }

    @Test
    @DisplayName("returns empty for plain query")
    void parseExtendsTarget_plainQuery_returnsEmpty() {
        // Given
        String query = "query";

        // When
        Optional<String> actual = StructuralQueryParser.parseExtendsTarget(query);

        // Then
        assertThat(actual).isEmpty();
    }

    @Test
    @DisplayName("returns empty for null input")
    void parseExtendsTarget_nullInput_returnsEmpty() {
        // Given
        String query = null;

        // When
        Optional<String> actual = StructuralQueryParser.parseExtendsTarget(query);

        // Then
        assertThat(actual).isEmpty();
    }

    @Test
    @DisplayName("returns empty when nothing after extends: prefix")
    void parseExtendsTarget_blankAfterPrefix_returnsEmpty() {
        // Given
        String query = "extends:";

        // When
        Optional<String> actual = StructuralQueryParser.parseExtendsTarget(query);

        // Then
        assertThat(actual).isEmpty();
    }
}
