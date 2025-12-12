/*
 * Copyright (c) 2025 MegaBrain Contributors
 * Licensed under the MIT License - see LICENSE file for details.
 */

package io.megabrain.ingestion;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FileChangeTest {

    @Test
    void constructor_shouldAcceptValidAddedChange() {
        FileChange change = FileChange.of(ChangeType.ADDED, "src/main/java/Test.java");

        assertThat(change.changeType()).isEqualTo(ChangeType.ADDED);
        assertThat(change.filePath()).isEqualTo("src/main/java/Test.java");
        assertThat(change.oldPath()).isNull();
    }

    @Test
    void constructor_shouldAcceptValidModifiedChange() {
        FileChange change = FileChange.of(ChangeType.MODIFIED, "README.md");

        assertThat(change.changeType()).isEqualTo(ChangeType.MODIFIED);
        assertThat(change.filePath()).isEqualTo("README.md");
        assertThat(change.oldPath()).isNull();
    }

    @Test
    void constructor_shouldAcceptValidDeletedChange() {
        FileChange change = FileChange.of(ChangeType.DELETED, "old-file.txt");

        assertThat(change.changeType()).isEqualTo(ChangeType.DELETED);
        assertThat(change.filePath()).isEqualTo("old-file.txt");
        assertThat(change.oldPath()).isNull();
    }

    @Test
    void constructor_shouldAcceptValidRenamedChange() {
        FileChange change = FileChange.renamed("old-name.java", "new-name.java");

        assertThat(change.changeType()).isEqualTo(ChangeType.RENAMED);
        assertThat(change.filePath()).isEqualTo("new-name.java");
        assertThat(change.oldPath()).isEqualTo("old-name.java");
    }

    @Test
    void constructor_shouldRejectNullChangeType() {
        assertThatThrownBy(() -> new FileChange(null, "test.txt", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("changeType must not be null");
    }

    @Test
    void constructor_shouldRejectNullFilePath() {
        assertThatThrownBy(() -> FileChange.of(ChangeType.ADDED, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("filePath must not be null or blank");
    }

    @Test
    void constructor_shouldRejectBlankFilePath() {
        assertThatThrownBy(() -> FileChange.of(ChangeType.ADDED, ""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("filePath must not be null or blank");
    }

    @Test
    void constructor_shouldRejectRenamedWithoutOldPath() {
        assertThatThrownBy(() -> new FileChange(ChangeType.RENAMED, "new.txt", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("oldPath must be provided for RENAMED changes");
    }

    @Test
    void constructor_shouldRejectNonRenamedWithOldPath() {
        assertThatThrownBy(() -> new FileChange(ChangeType.ADDED, "test.txt", "old.txt"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("oldPath should only be set for RENAMED changes");
    }

    @Test
    void renamedFactoryMethod_shouldRejectNullOldPath() {
        assertThatThrownBy(() -> FileChange.renamed(null, "new.txt"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("oldPath must be provided for RENAMED changes");
    }

    @Test
    void ofFactoryMethod_shouldRejectRenamedType() {
        assertThatThrownBy(() -> FileChange.of(ChangeType.RENAMED, "new.txt"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Use of(ChangeType, String, String) for RENAMED changes");
    }
}
