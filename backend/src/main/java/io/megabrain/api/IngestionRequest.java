package io.megabrain.api;

/**
 * Request DTO for repository ingestion operations.
 * Supports both full and incremental indexing modes.
 */
public class IngestionRequest {

    private String repositoryUrl;
    private boolean incremental = false;

    /**
     * Default constructor.
     */
    public IngestionRequest() {
    }

    /**
     * Constructor with parameters.
     */
    public IngestionRequest(String repositoryUrl, boolean incremental) {
        this.repositoryUrl = repositoryUrl;
        this.incremental = incremental;
    }

    /**
     * Gets the repository URL to ingest.
     * @return the repository URL
     */
    public String getRepositoryUrl() {
        return repositoryUrl;
    }

    /**
     * Sets the repository URL to ingest.
     * @param repositoryUrl the repository URL
     */
    public void setRepositoryUrl(String repositoryUrl) {
        this.repositoryUrl = repositoryUrl;
    }

    /**
     * Gets whether to perform incremental indexing.
     * When true, only processes changes since the last indexing.
     * When false (default), performs full indexing.
     * @return true for incremental indexing, false for full indexing
     */
    public boolean isIncremental() {
        return incremental;
    }

    /**
     * Sets whether to perform incremental indexing.
     * @param incremental true for incremental indexing, false for full indexing
     */
    public void setIncremental(boolean incremental) {
        this.incremental = incremental;
    }

    @Override
    public String toString() {
        return "IngestionRequest{" +
                "repositoryUrl='" + repositoryUrl + '\'' +
                ", incremental=" + incremental +
                '}';
    }
}
