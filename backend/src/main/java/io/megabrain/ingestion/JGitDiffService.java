/*
 * Copyright (c) 2025 MegaBrain Contributors
 * Licensed under the MIT License - see LICENSE file for details.
 */

package io.megabrain.ingestion;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.RenameDetector;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.jboss.logging.Logger;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * JGit-based implementation of GitDiffService.
 * Uses JGit's DiffCommand and RenameDetector to identify file changes between commits.
 */
@ApplicationScoped
public class JGitDiffService implements GitDiffService {

    private static final Logger LOG = Logger.getLogger(JGitDiffService.class);
    private static final int RENAME_SIMILARITY_THRESHOLD = 50; // 50% similarity for rename detection

    private final RepositoryIndexStateService indexStateService;

    @Inject
    public JGitDiffService(RepositoryIndexStateService indexStateService) {
        this.indexStateService = indexStateService;
    }

    @Override
    public Uni<List<FileChange>> detectChanges(Path repositoryPath, String oldCommitSha, String newCommitSha) {
        return Uni.createFrom().item(() -> {
            try (Git git = Git.open(repositoryPath.toFile())) {
                Repository repository = git.getRepository();

                // Resolve commit objects
                ObjectId oldCommitId = resolveCommit(repository, oldCommitSha);
                ObjectId newCommitId = resolveCommit(repository, newCommitSha);

                // Get tree parsers for both commits
                CanonicalTreeParser oldTree = getTreeParser(repository, oldCommitId);
                CanonicalTreeParser newTree = getTreeParser(repository, newCommitId);

                // Perform diff
                List<DiffEntry> diffEntries = git.diff()
                        .setOldTree(oldTree)
                        .setNewTree(newTree)
                        .call();

                // Detect renames
                RenameDetector renameDetector = new RenameDetector(repository);
                renameDetector.addAll(diffEntries);
                renameDetector.setRenameScore(RENAME_SIMILARITY_THRESHOLD);
                List<DiffEntry> renamedEntries = renameDetector.compute();

                // Convert to FileChange objects
                return convertToFileChanges(diffEntries, renamedEntries);

            } catch (IOException | GitAPIException e) {
                LOG.errorf(e, "Failed to detect changes between commits %s and %s in repository %s",
                        oldCommitSha, newCommitSha, repositoryPath);
                throw new IngestionException("Git diff failed", e);
            }
        });
    }

    @Override
    public Uni<List<FileChange>> detectChangesSinceLastIndex(Path repositoryPath, String repositoryUrl) {
        return indexStateService.getLastIndexedCommitSha(repositoryUrl)
                .flatMap(lastIndexedSha -> {
                    if (lastIndexedSha.isEmpty()) {
                        LOG.debugf("No last indexed commit found for repository %s, cannot perform incremental diff", repositoryUrl);
                        return Uni.createFrom().item(List.of());
                    }
                    return detectChanges(repositoryPath, lastIndexedSha.get(), "HEAD");
                });
    }

    private ObjectId resolveCommit(Repository repository, String commitSha) throws IOException {
        try (RevWalk revWalk = new RevWalk(repository)) {
            if ("HEAD".equals(commitSha)) {
                return repository.resolve("HEAD");
            }
            return repository.resolve(commitSha);
        }
    }

    private CanonicalTreeParser getTreeParser(Repository repository, ObjectId commitId) throws IOException {
        try (RevWalk revWalk = new RevWalk(repository)) {
            RevCommit commit = revWalk.parseCommit(commitId);
            CanonicalTreeParser treeParser = new CanonicalTreeParser();
            treeParser.reset(repository.newObjectReader(), commit.getTree());
            return treeParser;
        }
    }

    private List<FileChange> convertToFileChanges(List<DiffEntry> diffEntries, List<DiffEntry> renamedEntries) {
        List<FileChange> changes = new ArrayList<>();

        // Process renamed entries first (they may have been detected separately)
        for (DiffEntry entry : renamedEntries) {
            if (entry.getChangeType() == DiffEntry.ChangeType.RENAME) {
                changes.add(FileChange.renamed(entry.getOldPath(), entry.getNewPath()));
            }
        }

        // Process other entries, skipping those already handled as renames
        for (DiffEntry entry : diffEntries) {
            String newPath = entry.getNewPath();
            String oldPath = entry.getOldPath();

            // Skip if this was already handled as a rename
            boolean alreadyHandled = changes.stream()
                    .anyMatch(change -> change.changeType() == ChangeType.RENAMED &&
                                       ((change.oldPath() != null && change.oldPath().equals(oldPath)) ||
                                        change.filePath().equals(newPath)));

            if (alreadyHandled) {
                continue;
            }

            FileChange change = switch (entry.getChangeType()) {
                case ADD -> FileChange.of(ChangeType.ADDED, newPath);
                case MODIFY -> FileChange.of(ChangeType.MODIFIED, newPath);
                case DELETE -> FileChange.of(ChangeType.DELETED, oldPath);
                case RENAME -> FileChange.renamed(oldPath, newPath);
                case COPY -> FileChange.of(ChangeType.ADDED, newPath); // Treat copies as adds
            };

            changes.add(change);
        }

        LOG.debugf("Detected %d file changes", changes.size());
        return changes;
    }
}
