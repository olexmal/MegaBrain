package io.megabrain.ingestion;

import io.smallrye.mutiny.Multi;

/**
 * Service for orchestrating repository ingestion operations.
 * Supports both full and incremental indexing modes.
 */
public interface IngestionService {

    /**
     * Ingests a repository using full indexing mode.
     * Clones the repository, extracts and indexes all files.
     *
     * @param repositoryUrl the repository URL to ingest
     * @return a Multi that emits progress events during ingestion
     */
    Multi<ProgressEvent> ingestRepository(String repositoryUrl);

    /**
     * Ingests a repository using incremental indexing mode.
     * Only processes changes since the last successful indexing.
     *
     * @param repositoryUrl the repository URL to ingest
     * @return a Multi that emits progress events during ingestion
     */
    Multi<ProgressEvent> ingestRepositoryIncrementally(String repositoryUrl);
}
