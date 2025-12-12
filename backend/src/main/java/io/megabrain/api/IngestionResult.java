package io.megabrain.api;

import java.time.Duration;

/**
 * Result of an ingestion operation with performance metrics.
 */
public class IngestionResult {

    private final String repositoryUrl;
    private final boolean incremental;
    private final int filesProcessed;
    private final Duration duration;
    private final String status;
    private final String lastCommitSha;

    public IngestionResult(String repositoryUrl, boolean incremental, int filesProcessed,
                          Duration duration, String status, String lastCommitSha) {
        this.repositoryUrl = repositoryUrl;
        this.incremental = incremental;
        this.filesProcessed = filesProcessed;
        this.duration = duration;
        this.status = status;
        this.lastCommitSha = lastCommitSha;
    }

    public String getRepositoryUrl() {
        return repositoryUrl;
    }

    public boolean isIncremental() {
        return incremental;
    }

    public int getFilesProcessed() {
        return filesProcessed;
    }

    public Duration getDuration() {
        return duration;
    }

    public long getDurationMillis() {
        return duration.toMillis();
    }

    public String getStatus() {
        return status;
    }

    public String getLastCommitSha() {
        return lastCommitSha;
    }

    @Override
    public String toString() {
        return "IngestionResult{" +
                "repositoryUrl='" + repositoryUrl + '\'' +
                ", incremental=" + incremental +
                ", filesProcessed=" + filesProcessed +
                ", duration=" + duration +
                ", status='" + status + '\'' +
                ", lastCommitSha='" + lastCommitSha + '\'' +
                '}';
    }
}
