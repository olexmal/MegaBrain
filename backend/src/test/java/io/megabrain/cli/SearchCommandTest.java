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
 * Unit tests for SearchCommand (US-04-05 T1, T2).
 */
class SearchCommandTest {

    private static final java.nio.charset.Charset UTF8 = StandardCharsets.UTF_8;

    private static String runAndGetOut(CommandLine cmd, String... args) {
        ByteArrayOutputStream outBa = new ByteArrayOutputStream();
        ByteArrayOutputStream errBa = new ByteArrayOutputStream();
        cmd.setOut(new PrintWriter(new java.io.OutputStreamWriter(outBa, UTF8)));
        cmd.setErr(new PrintWriter(new java.io.OutputStreamWriter(errBa, UTF8)));
        cmd.execute(args);
        cmd.getOut().flush();
        cmd.getErr().flush();
        return new String(outBa.toByteArray(), UTF8);
    }

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

    // ---------- T2: filter options, validation, defaults ----------

    @Test
    @DisplayName("defaults when only query: limit 10, json and quiet false, lists empty")
    void execute_onlyQuery_setsDefaults() {
        SearchCommand command = new SearchCommand();
        CommandLine cmd = new CommandLine(command);
        runAndGetOut(cmd, "foo");

        assertThat(command.limit).isEqualTo(10);
        assertThat(command.json).isFalse();
        assertThat(command.quiet).isFalse();
        assertThat(command.language).isNull();
        assertThat(command.repo).isNull();
        assertThat(command.type).isNull();
        assertThat(command.getSearchRequest()).isNotNull();
        assertThat(command.getSearchRequest().getLimit()).isEqualTo(10);
        assertThat(command.getSearchRequest().getLanguages()).isEmpty();
        assertThat(command.getSearchRequest().getRepositories()).isEmpty();
        assertThat(command.getSearchRequest().getEntityTypes()).isEmpty();
    }

    @Test
    @DisplayName("each option parsed when passed")
    void execute_withOptions_parsesAllOptionValues() {
        SearchCommand command = new SearchCommand();
        CommandLine cmd = new CommandLine(command);
        runAndGetOut(cmd, "q", "--language", "java", "--language", "python", "--repo", "r1", "--repo", "r2",
            "--type", "class", "--type", "method", "--limit", "5", "--json", "--quiet");

        assertThat(command.query).isEqualTo("q");
        assertThat(command.language).containsExactly("java", "python");
        assertThat(command.repo).containsExactly("r1", "r2");
        assertThat(command.type).containsExactly("class", "method");
        assertThat(command.limit).isEqualTo(5);
        assertThat(command.json).isTrue();
        assertThat(command.quiet).isTrue();
        assertThat(command.getSearchRequest().getLanguages()).containsExactly("java", "python");
        assertThat(command.getSearchRequest().getRepositories()).containsExactly("r1", "r2");
        assertThat(command.getSearchRequest().getEntityTypes()).containsExactly("class", "method");
        assertThat(command.getSearchRequest().getLimit()).isEqualTo(5);
    }

    @Test
    @DisplayName("multi-value --language, --repo, --type")
    void execute_multiValueFilters_buildsRequestWithAllValues() {
        SearchCommand command = new SearchCommand();
        CommandLine cmd = new CommandLine(command);
        runAndGetOut(cmd, "x", "--language", "go", "--language", "rust", "--repo", "a/b", "--repo", "c/d",
            "--type", "function", "--type", "interface");

        assertThat(command.getSearchRequest().getLanguages()).containsExactlyInAnyOrder("go", "rust");
        assertThat(command.getSearchRequest().getRepositories()).containsExactlyInAnyOrder("a/b", "c/d");
        assertThat(command.getSearchRequest().getEntityTypes()).containsExactlyInAnyOrder("function", "interface");
    }

    @Test
    @DisplayName("valid --language and --type exit 0")
    void execute_validLanguageAndType_exitZero() {
        SearchCommand command = new SearchCommand();
        CommandLine cmd = new CommandLine(command);
        int exit = cmd.execute("query", "--language", "java", "--type", "class");
        assertThat(exit).isZero();
    }

    @Test
    @DisplayName("invalid --language exit 2 and stderr contains validation message")
    void execute_invalidLanguage_exit2AndStderrContainsAllowedValues() {
        SearchCommand command = new SearchCommand();
        CommandLine cmd = new CommandLine(command);
        ByteArrayOutputStream errBa = new ByteArrayOutputStream();
        cmd.setOut(new PrintWriter(new ByteArrayOutputStream()));
        cmd.setErr(new PrintWriter(new java.io.OutputStreamWriter(errBa, UTF8)));
        int exit = cmd.execute("q", "--language", "haskell");
        cmd.getErr().flush();
        assertThat(exit).isEqualTo(2);
        String err = new String(errBa.toByteArray(), UTF8);
        assertThat(err)
            .containsIgnoringCase("invalid")
            .contains("--language")
            .contains("haskell")
            .contains("Allowed:");
    }

    @Test
    @DisplayName("invalid --type exit 2 and stderr contains validation message")
    void execute_invalidType_exit2AndStderrContainsAllowedValues() {
        SearchCommand command = new SearchCommand();
        CommandLine cmd = new CommandLine(command);
        ByteArrayOutputStream errBa = new ByteArrayOutputStream();
        cmd.setOut(new PrintWriter(new ByteArrayOutputStream()));
        cmd.setErr(new PrintWriter(new java.io.OutputStreamWriter(errBa, UTF8)));
        int exit = cmd.execute("q", "--type", "unknown");
        cmd.getErr().flush();
        assertThat(exit).isEqualTo(2);
        String err = new String(errBa.toByteArray(), UTF8);
        assertThat(err)
            .containsIgnoringCase("invalid")
            .contains("--type")
            .contains("unknown")
            .contains("Allowed:");
    }

    @Test
    @DisplayName("--help contains all option names")
    void execute_help_containsAllOptionNames() {
        String out = runAndGetOut(new CommandLine(new SearchCommand()), "--help");
        assertThat(out)
            .contains("--language")
            .contains("--repo")
            .contains("--type")
            .contains("--limit")
            .contains("--json")
            .contains("--quiet");
    }

    @Test
    @DisplayName("--limit 1 and --limit 100 are valid")
    void execute_limit1And100_exitZero() {
        SearchCommand command = new SearchCommand();
        CommandLine cmd = new CommandLine(command);
        assertThat(cmd.execute("q", "--limit", "1")).isZero();
        assertThat(cmd.execute("q", "--limit", "100")).isZero();
    }

    @Test
    @DisplayName("--limit 0 or out of range exit 2")
    void execute_limit0OrOutOfRange_exit2() {
        SearchCommand command = new SearchCommand();
        CommandLine cmd = new CommandLine(command);
        ByteArrayOutputStream errBa = new ByteArrayOutputStream();
        cmd.setOut(new PrintWriter(new ByteArrayOutputStream()));
        cmd.setErr(new PrintWriter(new java.io.OutputStreamWriter(errBa, UTF8)));
        assertThat(cmd.execute("q", "--limit", "0")).isEqualTo(2);
        cmd.getErr().flush();
        assertThat(new String(errBa.toByteArray(), UTF8)).contains("limit").contains("1-100");

        cmd = new CommandLine(new SearchCommand());
        assertThat(cmd.execute("q", "--limit", "-1")).isEqualTo(2);
        cmd = new CommandLine(new SearchCommand());
        assertThat(cmd.execute("q", "--limit", "101")).isEqualTo(2);
    }

    @Test
    @DisplayName("missing query exit 2")
    void execute_noQuery_exit2() {
        CommandLine cmd = new CommandLine(new SearchCommand());
        ByteArrayOutputStream errBa = new ByteArrayOutputStream();
        cmd.setOut(new PrintWriter(new ByteArrayOutputStream()));
        cmd.setErr(new PrintWriter(new java.io.OutputStreamWriter(errBa, UTF8)));
        int exit = cmd.execute("--limit", "5");
        cmd.getErr().flush();
        assertThat(exit).isEqualTo(2);
        assertThat(new String(errBa.toByteArray(), UTF8)).containsIgnoringCase("query").containsIgnoringCase("required");
    }
}
