/*
 * Copyright (c) 2025 MegaBrain Contributors
 * Licensed under the MIT License - see LICENSE file for details.
 */

package io.megabrain.ingestion;

import io.megabrain.core.InMemoryIndexService;
import io.megabrain.ingestion.parser.TextChunk;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;
import jakarta.inject.Inject;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
class IncrementalIndexingServiceTest {

    @Inject
    IncrementalIndexingServiceImpl indexingService;

    @Inject
    InMemoryIndexService indexService;

    @Inject
    RepositoryIndexStateService indexStateService;

    private Path tempDir;
    private Path repoPath;
    private String repoUrl = "https://github.com/test/repo";

    @BeforeEach
    void setUp() throws IOException, GitAPIException {
        tempDir = Files.createTempDirectory("incremental-indexing-test");
        repoPath = tempDir.resolve("test-repo");
        Files.createDirectories(repoPath);

        // Initialize git repository
        try (Git git = Git.init().setDirectory(repoPath.toFile()).call()) {
            // Create initial commit
            Files.writeString(repoPath.resolve("README.md"), "# Test Repo");
            git.add().addFilepattern(".").call();
            git.commit()
                    .setMessage("Initial commit")
                    .setAuthor("Test", "test@example.com")
                    .call();
        }

        // Clear index for clean test state
        indexService.clear();
    }

    @Test
    void processFileChanges_withAddedFiles_shouldParseAndIndexNewFiles() throws IOException {
        // Create a new file after initial commit
        Path newFile = repoPath.resolve("Hello.java");
        Files.writeString(newFile, """
                public class Hello {
                    public static void main(String[] args) {
                        System.out.println("Hello World");
                    }
                }
                """);

        // Create file change for the new file
        FileChange addedChange = FileChange.of(ChangeType.ADDED, "Hello.java");
        List<FileChange> changes = List.of(addedChange);

        // Process the changes
        Integer processedFiles = indexingService.processFileChanges(repoPath, repoUrl, changes)
                .subscribe().withSubscriber(UniAssertSubscriber.create())
                .awaitItem().getItem();

        // Verify
        assertThat(processedFiles).isOne();

        // Check that chunks were indexed
        List<TextChunk> indexedChunks = indexService.getAllChunks();
        assertThat(indexedChunks).isNotEmpty();
        assertThat(indexedChunks.stream().map(TextChunk::entityType))
                .contains("class");
        assertThat(indexedChunks.stream().map(TextChunk::entityName))
                .contains("Hello");
    }

    @Test
    void processFileChanges_withMultipleAddedFiles_shouldProcessAllFiles() throws IOException {
        // Create multiple new files
        Path javaFile = repoPath.resolve("Test.java");
        Files.writeString(javaFile, """
                public class Test {
                    public void method() {}
                }
                """);

        Path pyFile = repoPath.resolve("test.py");
        Files.writeString(pyFile, """
                def hello():
                    print("hello")
                """);

        // Create file changes
        List<FileChange> changes = List.of(
                FileChange.of(ChangeType.ADDED, "Test.java"),
                FileChange.of(ChangeType.ADDED, "test.py")
        );

        // Process the changes
        Integer processedFiles = indexingService.processFileChanges(repoPath, repoUrl, changes)
                .subscribe().withSubscriber(UniAssertSubscriber.create())
                .awaitItem().getItem();

        // Verify
        assertThat(processedFiles).isEqualTo(2);

        // Check that chunks were indexed for both files
        List<TextChunk> allChunks = indexService.getAllChunks();
        assertThat(allChunks.stream().map(TextChunk::sourceFile))
                .anyMatch(path -> path.endsWith("Test.java"));
        // Note: Python parsing fails due to missing Tree-sitter library, so we only check for Java
    }

    @Test
    void processFileChanges_withUnsupportedFileType_shouldSkipFile() throws IOException {
        // Create a file with unsupported extension
        Path unsupportedFile = repoPath.resolve("test.txt");
        Files.writeString(unsupportedFile, "This is just text content");

        // Create file change
        FileChange change = FileChange.of(ChangeType.ADDED, "test.txt");
        List<FileChange> changes = List.of(change);

        // Process the changes
        Integer processedFiles = indexingService.processFileChanges(repoPath, repoUrl, changes)
                .subscribe().withSubscriber(UniAssertSubscriber.create())
                .awaitItem().getItem();

        // Verify - file should be processed (attempted) but no chunks indexed
        assertThat(processedFiles).isOne();
        assertThat(indexService.getChunksForFile("test.txt")).isEmpty();
    }

    @Test
    void processFileChanges_withNonExistentFile_shouldHandleGracefully() {
        // Create file change for non-existent file
        FileChange change = FileChange.of(ChangeType.ADDED, "nonexistent.java");
        List<FileChange> changes = List.of(change);

        // Process the changes
        Integer processedFiles = indexingService.processFileChanges(repoPath, repoUrl, changes)
                .subscribe().withSubscriber(UniAssertSubscriber.create())
                .awaitItem().getItem();

        // Should still count as processed (attempted)
        assertThat(processedFiles).isOne();
        assertThat(indexService.getChunksForFile("nonexistent.java")).isEmpty();
    }

    @Test
    void processFileChanges_withEmptyChanges_shouldReturnZero() {
        Integer processedFiles = indexingService.processFileChanges(repoPath, repoUrl, List.of())
                .subscribe().withSubscriber(UniAssertSubscriber.create())
                .awaitItem().getItem();

        assertThat(processedFiles).isZero();
    }

    @Test
    void processFileChanges_withModifiedFiles_shouldRemoveOldChunksAndReindex() throws IOException, GitAPIException {
        // First, add a file and index it
        Path testFile = repoPath.resolve("Modify.java");
        Files.writeString(testFile, """
                public class Modify {
                    public void oldMethod() {}
                }
                """);

        // Commit the initial version
        try (Git git = Git.open(repoPath.toFile())) {
            git.add().addFilepattern("Modify.java").call();
            git.commit()
                    .setMessage("Add Modify.java")
                    .setAuthor("Test", "test@example.com")
                    .call();
        }

        // Index the initial version
        FileChange addChange = FileChange.of(ChangeType.ADDED, "Modify.java");
        indexingService.processFileChanges(repoPath, repoUrl, List.of(addChange))
                .await().indefinitely();

        // Verify initial chunks exist
        List<TextChunk> initialChunks = indexService.getAllChunks();
        assertThat(initialChunks).isNotEmpty();
        assertThat(initialChunks.stream().map(TextChunk::entityName))
                .contains("Modify");

        // Modify the file
        Files.writeString(testFile, """
                public class Modify {
                    public void newMethod() {}
                    public void anotherMethod() {}
                }
                """);

        // Commit the modification
        try (Git git = Git.open(repoPath.toFile())) {
            git.add().addFilepattern("Modify.java").call();
            git.commit()
                    .setMessage("Modify Modify.java")
                    .setAuthor("Test", "test@example.com")
                    .call();
        }

        // Process the modification
        FileChange modifyChange = FileChange.of(ChangeType.MODIFIED, "Modify.java");
        Integer processedFiles = indexingService.processFileChanges(repoPath, repoUrl, List.of(modifyChange))
                .subscribe().withSubscriber(UniAssertSubscriber.create())
                .awaitItem().getItem();

        // Verify
        assertThat(processedFiles).isOne();

        // Check that chunks were updated (should have new methods and the class)
        List<TextChunk> updatedChunks = indexService.getAllChunks();
        assertThat(updatedChunks.stream().map(TextChunk::entityName))
                .contains("Modify", "Modify#newMethod()", "Modify#anotherMethod()");
    }

    @Test
    void processFileChanges_shouldUpdateLastIndexedCommitSha() throws IOException {
        // Create a new file
        Path newFile = repoPath.resolve("Update.java");
        Files.writeString(newFile, "public class Update {}");

        // Process changes
        FileChange change = FileChange.of(ChangeType.ADDED, "Update.java");
        indexingService.processFileChanges(repoPath, repoUrl, List.of(change))
                .await().indefinitely();

        // Verify that the last indexed commit SHA was updated
        Optional<String> lastSha = indexStateService.getLastIndexedCommitSha(repoUrl)
                .subscribe().withSubscriber(UniAssertSubscriber.create())
                .awaitItem().getItem();

        assertThat(lastSha).isPresent();
        assertThat(lastSha).contains("HEAD");
    }

    @Test
    void processFileChanges_withDeletedFiles_shouldRemoveChunksFromIndex() throws IOException, GitAPIException {
        // First, add a file and index it
        Path deleteFile = repoPath.resolve("ToDelete.java");
        Files.writeString(deleteFile, """
                public class ToDelete {
                    public void method() {}
                }
                """);

        // Commit the file
        try (Git git = Git.open(repoPath.toFile())) {
            git.add().addFilepattern("ToDelete.java").call();
            git.commit()
                    .setMessage("Add ToDelete.java")
                    .setAuthor("Test", "test@example.com")
                    .call();
        }

        // Index the file
        FileChange addChange = FileChange.of(ChangeType.ADDED, "ToDelete.java");
        indexingService.processFileChanges(repoPath, repoUrl, List.of(addChange))
                .await().indefinitely();

        // Verify chunks exist
        List<TextChunk> chunksBefore = indexService.getAllChunks();
        assertThat(chunksBefore.stream().map(TextChunk::sourceFile))
                .anyMatch(path -> path.endsWith("ToDelete.java"));

        // Delete the file
        Files.delete(deleteFile);

        // Commit the deletion
        try (Git git = Git.open(repoPath.toFile())) {
            git.rm().addFilepattern("ToDelete.java").call();
            git.commit()
                    .setMessage("Delete ToDelete.java")
                    .setAuthor("Test", "test@example.com")
                    .call();
        }

        // Process the deletion
        FileChange deleteChange = FileChange.of(ChangeType.DELETED, "ToDelete.java");
        Integer processedFiles = indexingService.processFileChanges(repoPath, repoUrl, List.of(deleteChange))
                .subscribe().withSubscriber(UniAssertSubscriber.create())
                .awaitItem().getItem();

        // Verify
        assertThat(processedFiles).isOne();

        // Check that chunks were removed
        List<TextChunk> chunksAfter = indexService.getAllChunks();
        assertThat(chunksAfter.stream().map(TextChunk::sourceFile))
                .noneMatch(path -> path.endsWith("ToDelete.java"));
    }

    @Test
    void processFileChanges_withRenamedFiles_shouldMoveChunksFromOldToNewPath() throws IOException, GitAPIException {
        // First, add a file and index it
        Path originalFile = repoPath.resolve("Original.java");
        Files.writeString(originalFile, """
                public class Original {
                    public void method() {}
                }
                """);

        // Commit the original file
        try (Git git = Git.open(repoPath.toFile())) {
            git.add().addFilepattern("Original.java").call();
            git.commit().setMessage("Add Original.java").setAuthor("Test", "test@example.com").call();
        }

        // Index the original file
        FileChange addChange = FileChange.of(ChangeType.ADDED, "Original.java");
        indexingService.processFileChanges(repoPath, repoUrl, List.of(addChange)).await().indefinitely();

        // Verify chunks exist at original path
        List<TextChunk> chunksBefore = indexService.getAllChunks();
        assertThat(chunksBefore.stream().map(TextChunk::sourceFile)).anyMatch(path -> path.endsWith("Original.java")).noneMatch(path -> path.endsWith("Renamed.java"));

        // Rename the file
        Path renamedFile = repoPath.resolve("Renamed.java");
        Files.move(originalFile, renamedFile);

        // Commit the rename
        try (Git git = Git.open(repoPath.toFile())) {
            git.rm().addFilepattern("Original.java").call();
            git.add().addFilepattern("Renamed.java").call();
            git.commit().setMessage("Rename Original.java to Renamed.java").setAuthor("Test", "test@example.com").call();
        }

        // Process the rename
        FileChange renameChange = FileChange.renamed("Original.java", "Renamed.java");
        Integer processedFiles = indexingService.processFileChanges(repoPath, repoUrl, List.of(renameChange)).subscribe().withSubscriber(UniAssertSubscriber.create()).awaitItem().getItem();

        // Verify
        assertThat(processedFiles).isOne();

        // Check that chunks moved from old path to new path
        List<TextChunk> chunksAfter = indexService.getAllChunks();
        assertThat(chunksAfter.stream().map(TextChunk::sourceFile)).noneMatch(path -> path.endsWith("Original.java"))

                .anyMatch(path -> path.endsWith("Renamed.java"));

        // Verify the same number of chunks exist
        assertThat(chunksAfter).hasSameSizeAs(chunksBefore);
    }

    @Test
    void processFileChanges_withMixedChangeTypes_shouldHandleAllTypesCorrectly() throws IOException, GitAPIException {
        // Set up initial repository state with multiple files
        Path addFile = repoPath.resolve("Add.java");
        Files.writeString(addFile, """
                public class Add {
                    public void method() {}
                }
                """);

        Path modifyFile = repoPath.resolve("Modify.java");
        Files.writeString(modifyFile, """
                public class Modify {
                    public void oldMethod() {}
                }
                """);

        Path deleteFile = repoPath.resolve("Delete.java");
        Files.writeString(deleteFile, """
                public class Delete {
                    public void method() {}
                }
                """);

        Path renameFile = repoPath.resolve("Rename.java");
        Files.writeString(renameFile, """
                public class Rename {
                    public void method() {}
                }
                """);

        // Commit all initial files
        try (Git git = Git.open(repoPath.toFile())) {
            git.add().addFilepattern(".").call();
            git.commit().setMessage("Add all test files").setAuthor("Test", "test@example.com").call();
        }

        // Index all files initially
        List<FileChange> initialChanges = List.of(FileChange.of(ChangeType.ADDED, "Add.java"), FileChange.of(ChangeType.ADDED, "Modify.java"), FileChange.of(ChangeType.ADDED, "Delete.java"), FileChange.of(ChangeType.ADDED, "Rename.java"));
        indexingService.processFileChanges(repoPath, repoUrl, initialChanges).await().indefinitely();

        // Verify initial state
        List<TextChunk> initialChunks = indexService.getAllChunks();
        assertThat(initialChunks.stream().map(TextChunk::sourceFile)).anyMatch(path -> path.endsWith("Add.java")).anyMatch(path -> path.endsWith("Modify.java")).anyMatch(path -> path.endsWith("Delete.java")).anyMatch(path -> path.endsWith("Rename.java"));

        // Make mixed changes:
        // 1. Add a new file
        Path newAddFile = repoPath.resolve("NewAdd.java");
        Files.writeString(newAddFile, """
                public class NewAdd {
                    public void newMethod() {}
                }
                """);

        // 2. Modify existing file
        Files.writeString(modifyFile, """
                public class Modify {
                    public void newMethod() {}
                    public void anotherMethod() {}
                }
                """);

        // 3. Delete a file
        Files.delete(deleteFile);

        // 4. Rename a file
        Path renamedFile = repoPath.resolve("Renamed.java");
        Files.move(renameFile, renamedFile);

        // Commit all changes
        try (Git git = Git.open(repoPath.toFile())) {
            git.add().addFilepattern("NewAdd.java").call();
            git.add().addFilepattern("Modify.java").call();
            git.rm().addFilepattern("Delete.java").call();
            git.rm().addFilepattern("Rename.java").call();
            git.add().addFilepattern("Renamed.java").call();
            git.commit().setMessage("Mixed changes: add, modify, delete, rename").setAuthor("Test", "test@example.com").call();
        }

        // Process mixed changes
        List<FileChange> mixedChanges = List.of(FileChange.of(ChangeType.ADDED, "NewAdd.java"), FileChange.of(ChangeType.MODIFIED, "Modify.java"), FileChange.of(ChangeType.DELETED, "Delete.java"), FileChange.renamed("Rename.java", "Renamed.java"));

        Integer processedFiles = indexingService.processFileChanges(repoPath, repoUrl, mixedChanges).subscribe().withSubscriber(UniAssertSubscriber.create()).awaitItem().getItem();

        // Verify
        assertThat(processedFiles).isEqualTo(4);

        // Check final state
        List<TextChunk> finalChunks = indexService.getAllChunks();

        // New file should be indexed
        assertThat(finalChunks.stream().map(TextChunk::sourceFile)).anyMatch(path -> path.endsWith("NewAdd.java"));
        assertThat(finalChunks.stream().map(TextChunk::entityName)).contains("NewAdd");

        // Modified file should have updated chunks
        assertThat(finalChunks.stream().map(TextChunk::sourceFile)).anyMatch(path -> path.endsWith("Modify.java"));
        assertThat(finalChunks.stream().map(TextChunk::entityName)).contains("Modify#newMethod()", "Modify#anotherMethod()");

        // Deleted file should have no chunks

        assertThat(finalChunks.stream().map(TextChunk::sourceFile)).noneMatch(path -> path.endsWith("Delete.java"))
                // Renamed file should have chunks at new path
                .noneMatch(path -> path.endsWith("Rename.java"))

                .anyMatch(path -> path.endsWith("Renamed.java"));
    }

    @Test
    void processFileChanges_withLargeNumberOfChanges_shouldHandleEfficiently() throws IOException, GitAPIException {
        // Create many files for a bulk operation test
        List<FileChange> changes = new ArrayList<>();

        for (int i = 0; i < 10; i++) {
            String fileName = "Bulk" + i + ".java";
            Path file = repoPath.resolve(fileName);
            String fileContent = "public class Bulk" + i + " {\n" +
                    "    public void method" + i + "() {}\n" +
                    "}\n";
            Files.writeString(file, fileContent);

            changes.add(FileChange.of(ChangeType.ADDED, fileName));
        }

        // Commit all files
        try (Git git = Git.open(repoPath.toFile())) {
            for (int i = 0; i < 10; i++) {
                git.add().addFilepattern("Bulk" + i + ".java").call();
            }
            git.commit()
                    .setMessage("Add bulk files")
                    .setAuthor("Test", "test@example.com")
                    .call();
        }

        // Process all changes
        Integer processedFiles = indexingService.processFileChanges(repoPath, repoUrl, changes)
                .subscribe().withSubscriber(UniAssertSubscriber.create())
                .awaitItem().getItem();

        // Verify all files were processed
        assertThat(processedFiles).isEqualTo(10);

        // Verify chunks were indexed
        List<TextChunk> allChunks = indexService.getAllChunks();
        for (int i = 0; i < 10; i++) {
            final int index = i; // Make effectively final
            assertThat(allChunks.stream().map(TextChunk::sourceFile))
                    .anyMatch(path -> path.endsWith("Bulk" + index + ".java"));
            assertThat(allChunks.stream().map(TextChunk::entityName))
                    .contains("Bulk" + index);
        }
    }

    @Test
    void processFileChanges_withNullChanges_shouldHandleGracefully() {
        Integer processedFiles = indexingService.processFileChanges(repoPath, repoUrl, null)
                .subscribe().withSubscriber(UniAssertSubscriber.create())
                .awaitItem().getItem();

        assertThat(processedFiles).isZero();
    }

    @Test
    void processFileChanges_withPartialFailures_shouldContinueProcessing() throws IOException {
        // Create mix of valid and invalid files
        Path validFile = repoPath.resolve("Valid.java");
        Files.writeString(validFile, """
                public class Valid {
                    public void method() {}
                }
                """);

        List<FileChange> changes = List.of(
                FileChange.of(ChangeType.ADDED, "Valid.java"),
                FileChange.of(ChangeType.ADDED, "nonexistent.java") // This will fail
        );

        Integer processedFiles = indexingService.processFileChanges(repoPath, repoUrl, changes)
                .subscribe().withSubscriber(UniAssertSubscriber.create())
                .awaitItem().getItem();

        // Should still count as processed (attempted) even if some fail
        assertThat(processedFiles).isEqualTo(2);

        // Valid file should be indexed
        List<TextChunk> chunks = indexService.getAllChunks();
        assertThat(chunks.stream().map(TextChunk::sourceFile))
                .anyMatch(path -> path.endsWith("Valid.java"));
    }
}
