/*
 * Copyright (c) 2025 MegaBrain Contributors
 * Licensed under the MIT License - see LICENSE file for details.
 */

package io.megabrain.repository;

import io.megabrain.ingestion.RepositoryIndexState;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
class FileBasedRepositoryIndexStateRepositoryTest {

    @Inject
    FileBasedRepositoryIndexStateRepository repository;

    private static final String TEST_REPO_URL = "https://github.com/test/repo";
    private static final String TEST_COMMIT_SHA = "abc123def456";

    private String uniqueId = java.util.UUID.randomUUID().toString().substring(0, 8);

    @Test
    void saveAndFindByRepositoryUrl_shouldPersistAndRetrieveState() {
        String uniqueRepoUrl = TEST_REPO_URL + "/" + uniqueId;
        // Create test state
        RepositoryIndexState state = RepositoryIndexState.create(uniqueRepoUrl, TEST_COMMIT_SHA);

        // Save state
        RepositoryIndexState saved = repository.save(state)
                .subscribe().withSubscriber(UniAssertSubscriber.create())
                .awaitItem().getItem();

        assertThat(saved).isNotNull();
        assertThat(saved.repositoryUrl()).isEqualTo(uniqueRepoUrl);
        assertThat(saved.lastIndexedCommitSha()).isEqualTo(TEST_COMMIT_SHA);

        // Retrieve state
        Optional<RepositoryIndexState> retrieved = repository.findByRepositoryUrl(uniqueRepoUrl)
                .subscribe().withSubscriber(UniAssertSubscriber.create())
                .awaitItem().getItem();

        assertThat(retrieved).isPresent();
        assertThat(retrieved.get().repositoryUrl()).isEqualTo(uniqueRepoUrl);
        assertThat(retrieved.get().lastIndexedCommitSha()).isEqualTo(TEST_COMMIT_SHA);
    }

    @Test
    void existsByRepositoryUrl_shouldReturnTrueAfterSave() {
        String uniqueRepoUrl = TEST_REPO_URL + "/" + uniqueId + "1";
        RepositoryIndexState state = RepositoryIndexState.create(uniqueRepoUrl, TEST_COMMIT_SHA);

        // Should not exist initially
        Boolean existsBefore = repository.existsByRepositoryUrl(uniqueRepoUrl)
                .subscribe().withSubscriber(UniAssertSubscriber.create())
                .awaitItem().getItem();
        assertThat(existsBefore).isFalse();

        // Save and check again
        repository.save(state).await().indefinitely();

        Boolean existsAfter = repository.existsByRepositoryUrl(uniqueRepoUrl)
                .subscribe().withSubscriber(UniAssertSubscriber.create())
                .awaitItem().getItem();
        assertThat(existsAfter).isTrue();
    }

    @Test
    void deleteByRepositoryUrl_shouldRemoveState() {
        String uniqueRepoUrl = TEST_REPO_URL + "/" + uniqueId + "2";
        RepositoryIndexState state = RepositoryIndexState.create(uniqueRepoUrl, TEST_COMMIT_SHA);

        // Save state
        repository.save(state).await().indefinitely();

        // Verify it exists
        Boolean exists = repository.existsByRepositoryUrl(uniqueRepoUrl)
                .subscribe().withSubscriber(UniAssertSubscriber.create())
                .awaitItem().getItem();
        assertThat(exists).isTrue();

        // Delete state
        Boolean deleted = repository.deleteByRepositoryUrl(uniqueRepoUrl)
                .subscribe().withSubscriber(UniAssertSubscriber.create())
                .awaitItem().getItem();
        assertThat(deleted).isTrue();

        // Verify it's gone
        Boolean existsAfterDelete = repository.existsByRepositoryUrl(uniqueRepoUrl)
                .subscribe().withSubscriber(UniAssertSubscriber.create())
                .awaitItem().getItem();
        assertThat(existsAfterDelete).isFalse();
    }

    @Test
    void findByRepositoryUrl_shouldReturnEmptyForUnknownRepository() {
        String unknownRepo = "https://github.com/unknown/repo";

        Optional<RepositoryIndexState> result = repository.findByRepositoryUrl(unknownRepo)
                .subscribe().withSubscriber(UniAssertSubscriber.create())
                .awaitItem().getItem();

        assertThat(result).isEmpty();
    }
}
