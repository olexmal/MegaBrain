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
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for IngestCommand (US-04-04 T1).
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
    void execute_help_printsUsageWithIngestAndOptions() throws UnsupportedEncodingException {
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
        String output = outBa.toString(StandardCharsets.UTF_8.name());
        assertThat(exitCode).isEqualTo(0);
        assertThat(output).contains("ingest");
        assertThat(output).contains("--help");
        assertThat(output).contains("Ingest a repository");
    }

    @Test
    @DisplayName("parse with no args does not throw")
    void execute_noArgs_doesNotThrow() throws UnsupportedEncodingException {
        IngestCommand command = new IngestCommand();
        CommandLine cmd = new CommandLine(command);
        ByteArrayOutputStream outBa = new ByteArrayOutputStream();
        PrintWriter out = new PrintWriter(new java.io.OutputStreamWriter(outBa, StandardCharsets.UTF_8));
        cmd.setOut(out);
        cmd.setErr(new PrintWriter(new java.io.OutputStreamWriter(new ByteArrayOutputStream(), StandardCharsets.UTF_8)));

        int exitCode = cmd.execute();

        assertThat(exitCode).isEqualTo(0);
    }

    @Test
    @DisplayName("run invokes without throwing")
    void run_invokesWithoutThrowing() {
        IngestCommand command = new IngestCommand();
        command.run();
    }
}
