/*
 * Copyright (c) 2025 MegaBrain Contributors
 * Licensed under the MIT License - see LICENSE file for details.
 */

package io.megabrain.cli;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for SearchCommand (US-04-05 T1).
 */
class SearchCommandTest {

    @Test
    @DisplayName("command name is search")
    void commandSpec_name_isSearch() {
        CommandLine cmd = new CommandLine(new SearchCommand());
        assertThat(cmd.getCommandSpec().name()).isEqualTo("search");
    }

    @Test
    @DisplayName("--help prints usage containing search and description")
    void execute_help_printsUsageWithSearchAndDescription() {
        SearchCommand command = new SearchCommand();
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
        assertThat(exitCode).isZero();
        assertThat(output)
            .contains("search")
            .contains("--help")
            .contains("Search the MegaBrain index");
    }

    @Test
    @DisplayName("execute with one query arg parses and runs")
    void execute_withQueryArg_parsesAndRuns() {
        SearchCommand command = new SearchCommand();
        CommandLine cmd = new CommandLine(command);
        ByteArrayOutputStream outBa = new ByteArrayOutputStream();
        ByteArrayOutputStream errBa = new ByteArrayOutputStream();
        PrintWriter out = new PrintWriter(new java.io.OutputStreamWriter(outBa, StandardCharsets.UTF_8));
        PrintWriter err = new PrintWriter(new java.io.OutputStreamWriter(errBa, StandardCharsets.UTF_8));
        cmd.setOut(out);
        cmd.setErr(err);

        int exitCode = cmd.execute("hello world");

        out.flush();
        err.flush();
        assertThat(exitCode).isZero();
        assertThat(command.query).isEqualTo("hello world");
        String stdout = new String(outBa.toByteArray(), StandardCharsets.UTF_8);
        assertThat(stdout).contains("Query received: hello world");
    }

    @Test
    @DisplayName("blank query returns exit code 2 and error message")
    void execute_blankQuery_returnsExitCode2AndErrorMessage() {
        CommandLine cmd = new CommandLine(new SearchCommand());
        ByteArrayOutputStream errBa = new ByteArrayOutputStream();
        cmd.setOut(new PrintWriter(new ByteArrayOutputStream()));
        cmd.setErr(new PrintWriter(new java.io.OutputStreamWriter(errBa, StandardCharsets.UTF_8)));

        int exitCode = cmd.execute("   ");

        cmd.getErr().flush();
        assertThat(exitCode).isEqualTo(2);
        String errOutput = new String(errBa.toByteArray(), StandardCharsets.UTF_8);
        assertThat(errOutput).containsIgnoringCase("non-blank");
    }
}
