package io.megabrain.ingestion;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import java.nio.file.Path;
import java.util.List;

/**
 * Composite SourceControlClient that delegates to the appropriate implementation
 * based on the repository URL.
 */
@ApplicationScoped
public class CompositeSourceControlClient implements SourceControlClient {

    @Inject
    Instance<SourceControlClient> clients;

    @Override
    public boolean canHandle(String repositoryUrl) {
        return getClient(repositoryUrl) != null;
    }

    @Override
    public Uni<RepositoryMetadata> fetchMetadata(String repositoryUrl) {
        SourceControlClient client = getClient(repositoryUrl);
        if (client == null) {
            return Uni.createFrom().failure(new IllegalArgumentException("No client can handle URL: " + repositoryUrl));
        }
        return client.fetchMetadata(repositoryUrl);
    }

    @Override
    public Multi<ProgressEvent> cloneRepository(String repositoryUrl, String branch) {
        SourceControlClient client = getClient(repositoryUrl);
        if (client == null) {
            return Multi.createFrom().failure(new IllegalArgumentException("No client can handle URL: " + repositoryUrl));
        }
        return client.cloneRepository(repositoryUrl, branch);
    }

    @Override
    public Multi<ProgressEvent> extractFiles(Path repositoryPath) {
        // This doesn't depend on the URL, so we can use any client
        // For now, just use the first one
        List<SourceControlClient> clientList = clients.stream().toList();
        if (clientList.isEmpty()) {
            return Multi.createFrom().failure(new IllegalStateException("No SourceControlClient implementations available"));
        }
        return clientList.getFirst().extractFiles(repositoryPath);
    }

    @Override
    public Path getClonedRepositoryPath() {
        // This doesn't depend on the URL, so we can use any client
        List<SourceControlClient> clientList = clients.stream().toList();
        if (clientList.isEmpty()) {
            return null;
        }
        return clientList.getFirst().getClonedRepositoryPath();
    }

    private SourceControlClient getClient(String repositoryUrl) {
        // Get the actual instances to avoid CDI proxy issues
        List<SourceControlClient> clientList = clients.stream().toList();

        for (SourceControlClient client : clientList) {
            // Skip the composite client itself to avoid recursion
            if (client instanceof CompositeSourceControlClient) {
                continue;
            }
            try {
                if (client.canHandle(repositoryUrl)) {
                    return client;
                }
            } catch (Exception _) {
                // If one client fails, try the next one
            }
        }
        return null;
    }
}
