/*
 * Copyright (c) 2025 MegaBrain Contributors
 * Licensed under the MIT License - see LICENSE file for details.
 */

package io.megabrain.cli;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import picocli.CommandLine;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for IngestCommand (US-04-04 T1, T2).
 */
class IngestCommandTest {

    @Test
    @DisplayName("command name is ingest")
    void commandSpec_name_isIngest() {
        CommandLine cmd = new CommandLine(new IngestCommand());
        assertThat(cmd.getCommandSpec().name()).isEqualTo("ingest");
    }

    @Test
    @DisplayName("--help prints usage containing ingest and option descriptions")
    void execute_help_printsUsageWithIngestAndOptions() {
        IngestCommand command = new IngestCommand();
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
    }

    @Test
    @DisplayName("default branch when --branch omitted")
    void execute_sourceAndRepoOnly_defaultBranchIsMain() {
        IngestCommand command = new IngestCommand();
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
        IngestCommand command = new IngestCommand();
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
        IngestCommand command = new IngestCommand();
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
        IngestCommand command = new IngestCommand();
        CommandLine cmd = new CommandLine(command);
        ByteArrayOutputStream outBa = new ByteArrayOutputStream();
        cmd.setOut(new PrintWriter(new java.io.OutputStreamWriter(outBa, StandardCharsets.UTF_8)));
        cmd.setErr(new PrintWriter(new java.io.OutputStreamWriter(new ByteArrayOutputStream(), StandardCharsets.UTF_8)));

        int exitCode = cmd.execute("--source", sourceValue, "--repo", "some/repo");

        assertThat(exitCode).isEqualTo(0);
        command.run();
    }

    @Test
    @DisplayName("invalid source fails with clear message")
    void execute_invalidSource_failsWithClearMessage() {
        IngestCommand command = new IngestCommand();
        CommandLine cmd = new CommandLine(command);
        ByteArrayOutputStream outBa = new ByteArrayOutputStream();
        ByteArrayOutputStream errBa = new ByteArrayOutputStream();
        PrintWriter out = new PrintWriter(new java.io.OutputStreamWriter(outBa, StandardCharsets.UTF_8));
        PrintWriter err = new PrintWriter(new java.io.OutputStreamWriter(errBa, StandardCharsets.UTF_8));
        cmd.setOut(out);
        cmd.setErr(err);

        int exitCode = cmd.execute("--source", "invalid", "--repo", "owner/repo");

        out.flush();
        err.flush();
        assertThat(exitCode).isNotEqualTo(0);
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
        IngestCommand command = new IngestCommand();
        CommandLine cmd = new CommandLine(command);
        ByteArrayOutputStream outBa = new ByteArrayOutputStream();
        cmd.setOut(new PrintWriter(new java.io.OutputStreamWriter(outBa, StandardCharsets.UTF_8)));
        cmd.setErr(new PrintWriter(new java.io.OutputStreamWriter(new ByteArrayOutputStream(), StandardCharsets.UTF_8)));

        int exitCode = cmd.execute("--source", "github", "--repo", "owner/repo", "--token", "secret-token");

        assertThat(exitCode).isEqualTo(0);
        assertThat(command.token).isEqualTo("secret-token");
        command.run();
    }

    @Test
    @DisplayName("run does not throw after valid parse")
    void execute_validParse_runDoesNotThrow() {
        IngestCommand command = new IngestCommand();
        CommandLine cmd = new CommandLine(command);
        ByteArrayOutputStream outBa = new ByteArrayOutputStream();
        cmd.setOut(new PrintWriter(new java.io.OutputStreamWriter(outBa, StandardCharsets.UTF_8)));
        cmd.setErr(new PrintWriter(new java.io.OutputStreamWriter(new ByteArrayOutputStream(), StandardCharsets.UTF_8)));

        cmd.execute("--source", "github", "--repo", "owner/repo");

        command.run();
    }

    @Test
    @DisplayName("missing --repo fails with clear message")
    void execute_missingRepo_failsWithClearMessage() {
        IngestCommand command = new IngestCommand();
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
        IngestCommand command = new IngestCommand();
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
}
