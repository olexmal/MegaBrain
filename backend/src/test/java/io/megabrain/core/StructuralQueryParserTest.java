/*
 * Copyright (c) 2025 MegaBrain Contributors
 * Licensed under the MIT License - see LICENSE file for details.
 */

package io.megabrain.core;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for StructuralQueryParser (US-02-06, T3/T4).
 */
class StructuralQueryParserTest {

    @Test
    void parseImplementsTarget_returnsInterfaceName() {
        assertThat(StructuralQueryParser.parseImplementsTarget("implements:IRepository"))
                .isEqualTo(Optional.of("IRepository"));
        assertThat(StructuralQueryParser.parseImplementsTarget("implements:IRepo"))
                .isEqualTo(Optional.of("IRepo"));
    }

    @Test
    void parseImplementsTarget_withTrailingSpaceAndTerms_returnsFirstToken() {
        assertThat(StructuralQueryParser.parseImplementsTarget("implements:IRepository other terms"))
                .isEqualTo(Optional.of("IRepository"));
        assertThat(StructuralQueryParser.parseImplementsTarget("implements:IX foo"))
                .isEqualTo(Optional.of("IX"));
    }

    @Test
    void parseImplementsTarget_nonImplementsQuery_returnsEmpty() {
        assertThat(StructuralQueryParser.parseImplementsTarget("extends:Base")).isEmpty();
        assertThat(StructuralQueryParser.parseImplementsTarget("some query")).isEmpty();
        assertThat(StructuralQueryParser.parseImplementsTarget(null)).isEmpty();
    }

    @Test
    void parseImplementsTarget_blankAfterPrefix_returnsEmpty() {
        assertThat(StructuralQueryParser.parseImplementsTarget("implements:")).isEmpty();
        assertThat(StructuralQueryParser.parseImplementsTarget("implements:   ")).isEmpty();
    }

    @Test
    void parseExtendsTarget_returnsClassName() {
        assertThat(StructuralQueryParser.parseExtendsTarget("extends:BaseClass"))
                .isEqualTo(Optional.of("BaseClass"));
        assertThat(StructuralQueryParser.parseExtendsTarget("extends:Base"))
                .isEqualTo(Optional.of("Base"));
    }

    @Test
    void parseExtendsTarget_withTrailingTerms_returnsFirstToken() {
        assertThat(StructuralQueryParser.parseExtendsTarget("extends:Base other"))
                .isEqualTo(Optional.of("Base"));
    }

    @Test
    void parseExtendsTarget_nonExtendsQuery_returnsEmpty() {
        assertThat(StructuralQueryParser.parseExtendsTarget("implements:IRepo")).isEmpty();
        assertThat(StructuralQueryParser.parseExtendsTarget("query")).isEmpty();
        assertThat(StructuralQueryParser.parseExtendsTarget(null)).isEmpty();
    }

    @Test
    void parseExtendsTarget_blankAfterPrefix_returnsEmpty() {
        assertThat(StructuralQueryParser.parseExtendsTarget("extends:")).isEmpty();
        assertThat(StructuralQueryParser.parseExtendsTarget("extends:   ")).isEmpty();
    }
}
