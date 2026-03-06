/*
 * Copyright (c) 2025 MegaBrain Contributors
 * Licensed under the MIT License - see LICENSE file for details.
 */

package io.megabrain.cli;

import io.megabrain.ingestion.IngestionService;
import io.megabrain.ingestion.ProgressEvent;
import io.smallrye.mutiny.Multi;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import picocli.CommandLine;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for IngestCommand (US-04-04 T1–T4).
 */
class IngestCommandTest {

    private static IngestionService mockIngestionServiceCompleting() {
        IngestionService service = mock(IngestionService.class);
        when(service.ingestRepository(anyString())).thenReturn(
            Multi.createFrom().items(ProgressEvent.of("Done", 100.0)));
        when(service.ingestRepositoryIncrementally(anyString())).thenReturn(
            Multi.createFrom().items(ProgressEvent.of("Incremental done", 100.0)));
        return service;
    }

    private static CommandLine createCommandLineForExitCodeTests(IngestionService ingestionService) {
        return new CommandLine(new IngestCommand(ingestionService));
    }

    @Test
    @DisplayName("command name is ingest")
    void commandSpec_name_isIngest() {
        CommandLine cmd = new CommandLine(new IngestCommand(mockIngestionServiceCompleting()));
        assertThat(cmd.getCommandSpec().name()).isEqualTo("ingest");
    }

    @Test
    @DisplayName("--help prints usage containing ingest and option descriptions")
    void execute_help_printsUsageWithIngestAndOptions() {
        IngestCommand command = new IngestCommand(mockIngestionServiceCompleting());
        CommandLine cmd = new CommandLine(command);
        ByteArrayOutputStream outBa = new ByteArrayOutputStream();
        ByteArrayOutputStream errBa = new ByteArrayOutputStream();
        PrintWriter out = new PrintWriter(new java.io.OutputStreamWriter(outBa, StandardCharsets.UTF_8));
        PrintWriter err = new PrintWriter(new java.io.OutputStreamWriter(errBa, StandardCharsets.UTF_8));
        cmd.setOut(out);
        cmd.setErr(err);

        int exitCode = cmd.execute("--help");

        out.flush();
        err.flush();
        String output = new String(outBa.toByteArray(), StandardCharsets.UTF_8);
        assertThat(exitCode).isEqualTo(0);
        assertThat(output).contains("ingest");
        assertThat(output).contains("--help");
        assertThat(output).contains("Ingest a repository");
        assertThat(output).contains("--source");
        assertThat(output).contains("--repo");
        assertThat(output).contains("--branch");
        assertThat(output).contains("--token");
        assertThat(output).contains("--incremental");
        assertThat(output).contains("--verbose");
    }

    @Test
    @DisplayName("default branch when --branch omitted")
    void execute_sourceAndRepoOnly_defaultBranchIsMain() {
        IngestCommand command = new IngestCommand(mockIngestionServiceCompleting());
        CommandLine cmd = new CommandLine(command);
        ByteArrayOutputStream outBa = new ByteArrayOutputStream();
        cmd.setOut(new PrintWriter(new java.io.OutputStreamWriter(outBa, StandardCharsets.UTF_8)));
        cmd.setErr(new PrintWriter(new java.io.OutputStreamWriter(new ByteArrayOutputStream(), StandardCharsets.UTF_8)));

        cmd.execute("--source", "github", "--repo", "owner/repo");

        assertThat(command.branch).isEqualTo("main");
    }

    @Test
    @DisplayName("default incremental false when omitted")
    void execute_incrementalOmitted_defaultIsFalse() {
        IngestCommand command = new IngestCommand(mockIngestionServiceCompleting());
        CommandLine cmd = new CommandLine(command);
        ByteArrayOutputStream outBa = new ByteArrayOutputStream();
        cmd.setOut(new PrintWriter(new java.io.OutputStreamWriter(outBa, StandardCharsets.UTF_8)));
        cmd.setErr(new PrintWriter(new java.io.OutputStreamWriter(new ByteArrayOutputStream(), StandardCharsets.UTF_8)));

        cmd.execute("--source", "github", "--repo", "owner/repo");

        assertThat(command.incremental).isFalse();
    }

    @Test
    @DisplayName("explicit branch and incremental parsed")
    void execute_explicitBranchAndIncremental_parsedCorrectly() {
        IngestCommand command = new IngestCommand(mockIngestionServiceCompleting());
        CommandLine cmd = new CommandLine(command);
        ByteArrayOutputStream outBa = new ByteArrayOutputStream();
        cmd.setOut(new PrintWriter(new java.io.OutputStreamWriter(outBa, StandardCharsets.UTF_8)));
        cmd.setErr(new PrintWriter(new java.io.OutputStreamWriter(new ByteArrayOutputStream(), StandardCharsets.UTF_8)));

        cmd.execute("--source", "gitlab", "--repo", "gitlab.com/group/proj", "--branch", "develop", "--incremental");

        assertThat(command.branch).isEqualTo("develop");
        assertThat(command.incremental).isTrue();
        assertThat(command.source).isEqualTo("gitlab");
        assertThat(command.repo).isEqualTo("gitlab.com/group/proj");
    }

    @ParameterizedTest
    @ValueSource(strings = { "github", "GITHUB", "gitlab", "GITLAB", "bitbucket", "BITBUCKET", "local", "LOCAL" })
    @DisplayName("valid source for each enum value parses and run does not throw")
    void execute_validSource_parsesAndRunDoesNotThrow(String sourceValue) {
        IngestCommand command = new IngestCommand(mockIngestionServiceCompleting());
        CommandLine cmd = new CommandLine(command);
        ByteArrayOutputStream outBa = new ByteArrayOutputStream();
        cmd.setOut(new PrintWriter(new java.io.OutputStreamWriter(outBa, StandardCharsets.UTF_8)));
        cmd.setErr(new PrintWriter(new java.io.OutputStreamWriter(new ByteArrayOutputStream(), StandardCharsets.UTF_8)));

        int exitCode = cmd.execute("--source", sourceValue, "--repo", "some/repo");

        assertThat(exitCode).isEqualTo(0);
    }

    @Test
    @DisplayName("invalid source returns exit code 2")
    void execute_invalidSource_failsWithClearMessage() {
        CommandLine cmd = createCommandLineForExitCodeTests(mockIngestionServiceCompleting());
        ByteArrayOutputStream outBa = new ByteArrayOutputStream();
        ByteArrayOutputStream errBa = new ByteArrayOutputStream();
        PrintWriter out = new PrintWriter(new java.io.OutputStreamWriter(outBa, StandardCharsets.UTF_8));
        PrintWriter err = new PrintWriter(new java.io.OutputStreamWriter(errBa, StandardCharsets.UTF_8));
        cmd.setOut(out);
        cmd.setErr(err);

        int exitCode = cmd.execute("--source", "invalid", "--repo", "owner/repo");

        out.flush();
        err.flush();
        assertThat(exitCode).isEqualTo(2);
        String errOutput = new String(errBa.toByteArray(), StandardCharsets.UTF_8);
        assertThat(errOutput).contains("Invalid source");
        assertThat(errOutput).containsIgnoringCase("allowed");
        assertThat(errOutput).contains("github");
        assertThat(errOutput).contains("gitlab");
        assertThat(errOutput).contains("bitbucket");
        assertThat(errOutput).contains("local");
    }

    @Test
    @DisplayName("token optional and with value parses")
    void execute_tokenOptional_withValue_parses() {
        IngestCommand command = new IngestCommand(mockIngestionServiceCompleting());
        CommandLine cmd = new CommandLine(command);
        ByteArrayOutputStream outBa = new ByteArrayOutputStream();
        cmd.setOut(new PrintWriter(new java.io.OutputStreamWriter(outBa, StandardCharsets.UTF_8)));
        cmd.setErr(new PrintWriter(new java.io.OutputStreamWriter(new ByteArrayOutputStream(), StandardCharsets.UTF_8)));

        int exitCode = cmd.execute("--source", "github", "--repo", "owner/repo", "--token", "secret-token");

        assertThat(exitCode).isEqualTo(0);
        assertThat(command.token).isEqualTo("secret-token");
    }

    @Test
    @DisplayName("exit code 0 on success")
    void execute_success_returnsExitCodeZero() {
        CommandLine cmd = createCommandLineForExitCodeTests(mockIngestionServiceCompleting());
        ByteArrayOutputStream outBa = new ByteArrayOutputStream();
        ByteArrayOutputStream errBa = new ByteArrayOutputStream();
        cmd.setOut(new PrintWriter(new java.io.OutputStreamWriter(outBa, StandardCharsets.UTF_8)));
        cmd.setErr(new PrintWriter(new java.io.OutputStreamWriter(errBa, StandardCharsets.UTF_8)));

        int exitCode = cmd.execute("--source", "github", "--repo", "owner/repo");

        assertThat(exitCode).isEqualTo(0);
    }

    @Test
    @DisplayName("missing --repo fails with clear message")
    void execute_missingRepo_failsWithClearMessage() {
        IngestCommand command = new IngestCommand(mockIngestionServiceCompleting());
        CommandLine cmd = new CommandLine(command);
        ByteArrayOutputStream errBa = new ByteArrayOutputStream();
        PrintWriter err = new PrintWriter(new java.io.OutputStreamWriter(errBa, StandardCharsets.UTF_8));
        cmd.setOut(new PrintWriter(new ByteArrayOutputStream()));
        cmd.setErr(err);

        int exitCode = cmd.execute("--source", "github");

        err.flush();
        assertThat(exitCode).isNotEqualTo(0);
        String errOutput = new String(errBa.toByteArray(), StandardCharsets.UTF_8);
        assertThat(errOutput).contains("--repo");
    }

    @Test
    @DisplayName("blank --repo fails with clear message")
    void execute_blankRepo_failsWithClearMessage() {
        IngestCommand command = new IngestCommand(mockIngestionServiceCompleting());
        CommandLine cmd = new CommandLine(command);
        ByteArrayOutputStream errBa = new ByteArrayOutputStream();
        PrintWriter err = new PrintWriter(new java.io.OutputStreamWriter(errBa, StandardCharsets.UTF_8));
        cmd.setOut(new PrintWriter(new ByteArrayOutputStream()));
        cmd.setErr(err);

        int exitCode = cmd.execute("--source", "github", "--repo", "   ");

        err.flush();
        assertThat(exitCode).isNotEqualTo(0);
        String errOutput = new String(errBa.toByteArray(), StandardCharsets.UTF_8);
        assertThat(errOutput).contains("non-blank");
    }

    @Test
    @DisplayName("full ingest prints progress events and completes")
    void execute_fullIngest_printsProgressAndCompletes() {
        IngestionService mockService = mock(IngestionService.class);
        when(mockService.ingestRepository(anyString())).thenReturn(Multi.createFrom().items(
            ProgressEvent.of("Cloning", 10.0),
            ProgressEvent.of("Parsing", 50.0),
            ProgressEvent.of("Done", 100.0)));
        when(mockService.ingestRepositoryIncrementally(anyString())).thenReturn(Multi.createFrom().empty());

        IngestCommand command = new IngestCommand(mockService);
        CommandLine cmd = new CommandLine(command);
        ByteArrayOutputStream outBa = new ByteArrayOutputStream();
        ByteArrayOutputStream errBa = new ByteArrayOutputStream();
        PrintWriter out = new PrintWriter(new java.io.OutputStreamWriter(outBa, StandardCharsets.UTF_8));
        PrintWriter err = new PrintWriter(new java.io.OutputStreamWriter(errBa, StandardCharsets.UTF_8));
        cmd.setOut(out);
        cmd.setErr(err);

        int exitCode = cmd.execute("--source", "github", "--repo", "owner/repo");

        out.flush();
        err.flush();
        String output = new String(outBa.toByteArray(), StandardCharsets.UTF_8);
        assertThat(exitCode).isEqualTo(0);
        assertThat(output).contains("Cloning");
        assertThat(output).contains("Parsing");
        assertThat(output).contains("Done");
        assertThat(output).contains("10.0");
        assertThat(output).contains("50.0");
        assertThat(output).contains("100.0");
        verify(mockService).ingestRepository("owner/repo");
        verify(mockService, never()).ingestRepositoryIncrementally(anyString());
    }

    @Test
    @DisplayName("incremental ingest prints progress and calls incremental only")
    void execute_incrementalIngest_printsProgressAndCallsIncrementalOnly() {
        IngestionService mockService = mock(IngestionService.class);
        when(mockService.ingestRepository(anyString())).thenReturn(Multi.createFrom().empty());
        when(mockService.ingestRepositoryIncrementally(anyString())).thenReturn(Multi.createFrom().items(
            ProgressEvent.of("Cloning", 10.0),
            ProgressEvent.of("Parsing", 50.0),
            ProgressEvent.of("Done", 100.0)));

        IngestCommand command = new IngestCommand(mockService);
        CommandLine cmd = new CommandLine(command);
        ByteArrayOutputStream outBa = new ByteArrayOutputStream();
        ByteArrayOutputStream errBa = new ByteArrayOutputStream();
        PrintWriter out = new PrintWriter(new java.io.OutputStreamWriter(outBa, StandardCharsets.UTF_8));
        PrintWriter err = new PrintWriter(new java.io.OutputStreamWriter(errBa, StandardCharsets.UTF_8));
        cmd.setOut(out);
        cmd.setErr(err);

        int exitCode = cmd.execute("--source", "github", "--repo", "owner/repo", "--incremental");

        out.flush();
        err.flush();
        String output = new String(outBa.toByteArray(), StandardCharsets.UTF_8);
        assertThat(exitCode).isEqualTo(0);
        assertThat(output).contains("Cloning");
        assertThat(output).contains("Parsing");
        assertThat(output).contains("Done");
        assertThat(output).contains("10");
        assertThat(output).contains("50");
        assertThat(output).contains("100");
        verify(mockService, never()).ingestRepository(anyString());
        verify(mockService).ingestRepositoryIncrementally("owner/repo");
    }

    @Test
    @DisplayName("ingestion failure returns exit code 1")
    void execute_streamFailure_exitsNonZeroAndShowsError() {
        IngestionService mockService = mock(IngestionService.class);
        when(mockService.ingestRepository(anyString())).thenReturn(
            Multi.createFrom().failure(new RuntimeException("Clone failed")));
        when(mockService.ingestRepositoryIncrementally(anyString())).thenReturn(Multi.createFrom().empty());

        CommandLine cmd = createCommandLineForExitCodeTests(mockService);
        ByteArrayOutputStream outBa = new ByteArrayOutputStream();
        ByteArrayOutputStream errBa = new ByteArrayOutputStream();
        PrintWriter out = new PrintWriter(new java.io.OutputStreamWriter(outBa, StandardCharsets.UTF_8));
        PrintWriter err = new PrintWriter(new java.io.OutputStreamWriter(errBa, StandardCharsets.UTF_8));
        cmd.setOut(out);
        cmd.setErr(err);

        int exitCode = cmd.execute("--source", "github", "--repo", "owner/repo");

        err.flush();
        assertThat(exitCode).isEqualTo(1);
        String errOutput = new String(errBa.toByteArray(), StandardCharsets.UTF_8);
        assertThat(errOutput).contains("Ingestion failed");
    }

    @Test
    @DisplayName("--verbose parses and sets verbose true")
    void execute_withVerbose_setsVerboseTrue() {
        IngestCommand command = new IngestCommand(mockIngestionServiceCompleting());
        CommandLine cmd = new CommandLine(command);
        ByteArrayOutputStream outBa = new ByteArrayOutputStream();
        cmd.setOut(new PrintWriter(new java.io.OutputStreamWriter(outBa, StandardCharsets.UTF_8)));
        cmd.setErr(new PrintWriter(new java.io.OutputStreamWriter(new ByteArrayOutputStream(), StandardCharsets.UTF_8)));

        cmd.execute("--source", "github", "--repo", "owner/repo", "--verbose");

        assertThat(command.verbose).isTrue();
    }

    @Test
    @DisplayName("verbose mode prints full progress message (no truncation)")
    void execute_verbose_longProgressLine_notTruncated() {
        String longMessage = "Cloning " + "x".repeat(300) + " done";
        IngestionService mockService = mock(IngestionService.class);
        when(mockService.ingestRepository(anyString())).thenReturn(
            Multi.createFrom().items(ProgressEvent.of(longMessage, 100.0)));
        when(mockService.ingestRepositoryIncrementally(anyString())).thenReturn(Multi.createFrom().empty());

        IngestCommand command = new IngestCommand(mockService);
        CommandLine cmd = new CommandLine(command);
        ByteArrayOutputStream outBa = new ByteArrayOutputStream();
        cmd.setOut(new PrintWriter(new java.io.OutputStreamWriter(outBa, StandardCharsets.UTF_8)));
        cmd.setErr(new PrintWriter(new java.io.OutputStreamWriter(new ByteArrayOutputStream(), StandardCharsets.UTF_8)));

        int exitCode = cmd.execute("--source", "github", "--repo", "owner/repo", "--verbose");

        cmd.getOut().flush();
        String output = new String(outBa.toByteArray(), StandardCharsets.UTF_8);
        assertThat(exitCode).isEqualTo(0);
        assertThat(output).contains("x".repeat(300));
    }
}
