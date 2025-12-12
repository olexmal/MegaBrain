/*
 * Copyright (c) 2025 MegaBrain Contributors
 * Licensed under the MIT License - see LICENSE file for details.
 */

package io.megabrain.ingestion;

import io.megabrain.repository.RepositoryIndexStateRepository;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
class RepositoryIndexStateServiceTest {

    @Inject
    RepositoryIndexStateService service;

    RepositoryIndexStateRepository repository;

    private static final String TEST_REPO_URL = "https://github.com/test/repo";
    private static final String TEST_COMMIT_SHA = "abc123def456";

    private String uniqueId = java.util.UUID.randomUUID().toString().substring(0, 8);

    @BeforeEach
    void setUp() {
        // Note: In a real test, we'd use @InjectMock, but since we're using file-based storage,
        // we'll test the actual service. For now, we'll create a minimal test.
    }

    @Test
    void updateLastIndexedCommitSha_shouldCreateNewState() {
        String uniqueRepoUrl = TEST_REPO_URL + "/" + uniqueId;
        RepositoryIndexState result = service.updateLastIndexedCommitSha(uniqueRepoUrl, TEST_COMMIT_SHA)
                .subscribe().withSubscriber(UniAssertSubscriber.create())
                .awaitItem().getItem();

        assertThat(result).isNotNull();
        assertThat(result.repositoryUrl()).isEqualTo(uniqueRepoUrl);
        assertThat(result.lastIndexedCommitSha()).isEqualTo(TEST_COMMIT_SHA);
        assertThat(result.lastIndexedAt()).isNotBlank();
    }

    @Test
    void getLastIndexedCommitSha_shouldReturnEmptyForUnknownRepository() {
        String unknownRepo = "https://github.com/unknown/repo/" + uniqueId;

        Optional<String> result = service.getLastIndexedCommitSha(unknownRepo)
                .subscribe().withSubscriber(UniAssertSubscriber.create())
                .awaitItem().getItem();

        assertThat(result).isEmpty();
    }

    @Test
    void hasBeenIndexed_shouldReturnFalseForUnknownRepository() {
        String unknownRepo = "https://github.com/unknown/repo/" + uniqueId + "2";

        Boolean result = service.hasBeenIndexed(unknownRepo)
                .subscribe().withSubscriber(UniAssertSubscriber.create())
                .awaitItem().getItem();

        assertThat(result).isFalse();
    }
}
