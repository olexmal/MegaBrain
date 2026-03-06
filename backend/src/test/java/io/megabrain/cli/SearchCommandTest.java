/*
 * Copyright (c) 2025 MegaBrain Contributors
 * Licensed under the MIT License - see LICENSE file for details.
 */

package io.megabrain.cli;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import picocli.CommandLine;
import uk.org.webcompere.systemstubs.environment.EnvironmentVariables;
import uk.org.webcompere.systemstubs.jupiter.SystemStub;
import uk.org.webcompere.systemstubs.jupiter.SystemStubsExtension;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.megabrain.core.ResultMerger;
import io.megabrain.core.SearchOrchestrator;
import io.smallrye.mutiny.Uni;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

/**
 * Unit tests for SearchCommand (US-04-05 T1–T6).
 */
@ExtendWith(SystemStubsExtension.class)
class SearchCommandTest {

    @SystemStub
    private EnvironmentVariables environmentVariables;

    private static final java.nio.charset.Charset UTF8 = StandardCharsets.UTF_8;
    private static final ObjectMapper JSON = new ObjectMapper();

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
            .contains("--quiet")
            .contains("--no-color");
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

    // ---------- T3: result formatting, orchestrator integration ----------

    @Test
    @DisplayName("when not --json stdout contains formatted result")
    void execute_withQueryAndMockOrchestrator_stdoutContainsFormattedResult() {
        SearchOrchestrator mockOrchestrator = mock(SearchOrchestrator.class);
        List<ResultMerger.MergedResult> merged = createMockMergedResults(1);
        when(mockOrchestrator.orchestrate(any(), eq(io.megabrain.core.SearchMode.HYBRID), anyInt(), anyInt()))
                .thenReturn(Uni.createFrom().item(new SearchOrchestrator.OrchestratorResult(merged, Map.of())));

        SearchResultFormatter formatter = new HumanReadableSearchResultFormatter();
        SearchCommand command = new SearchCommand(mockOrchestrator, formatter, JSON, 10, 5, 10);
        CommandLine cmd = new CommandLine(command);
        ByteArrayOutputStream outBa = new ByteArrayOutputStream();
        ByteArrayOutputStream errBa = new ByteArrayOutputStream();
        cmd.setOut(new PrintWriter(new java.io.OutputStreamWriter(outBa, UTF8)));
        cmd.setErr(new PrintWriter(new java.io.OutputStreamWriter(errBa, UTF8)));

        int exitCode = cmd.execute("foo");

        cmd.getOut().flush();
        assertThat(exitCode).isZero();
        String stdout = new String(outBa.toByteArray(), UTF8);
        assertThat(stdout).contains("File: Test0.java");
        assertThat(stdout).contains("Entity: TestEntity0");
        assertThat(stdout).contains("Score:");
        assertThat(stdout).contains("Test content 0");
        assertThat(stdout).contains("---");
    }

    @Test
    @DisplayName("empty results print No results.")
    void execute_emptyResults_printsNoResults() {
        SearchOrchestrator mockOrchestrator = mock(SearchOrchestrator.class);
        when(mockOrchestrator.orchestrate(any(), eq(io.megabrain.core.SearchMode.HYBRID), anyInt(), anyInt()))
                .thenReturn(Uni.createFrom().item(new SearchOrchestrator.OrchestratorResult(List.of(), Map.of())));

        SearchResultFormatter formatter = new HumanReadableSearchResultFormatter();
        SearchCommand command = new SearchCommand(mockOrchestrator, formatter, JSON, 10, 5, 10);
        CommandLine cmd = new CommandLine(command);
        ByteArrayOutputStream outBa = new ByteArrayOutputStream();
        cmd.setOut(new PrintWriter(new java.io.OutputStreamWriter(outBa, UTF8)));
        cmd.setErr(new PrintWriter(new ByteArrayOutputStream()));

        int exitCode = cmd.execute("query");

        cmd.getOut().flush();
        assertThat(exitCode).isZero();
        String stdout = new String(outBa.toByteArray(), UTF8);
        assertThat(stdout).contains("No results.");
    }

    @Test
    @DisplayName("--json full output is valid JSON with results, total, page, size, query, took_ms, facets")
    void execute_json_fullOutput_validJsonWithApiFields() throws Exception {
        SearchOrchestrator mockOrchestrator = mock(SearchOrchestrator.class);
        List<ResultMerger.MergedResult> merged = createMockMergedResults(1);
        when(mockOrchestrator.orchestrate(any(), eq(io.megabrain.core.SearchMode.HYBRID), anyInt(), anyInt()))
                .thenReturn(Uni.createFrom().item(new SearchOrchestrator.OrchestratorResult(merged, Map.of())));

        SearchResultFormatter formatter = new HumanReadableSearchResultFormatter();
        SearchCommand command = new SearchCommand(mockOrchestrator, formatter, JSON, 10, 5, 10);
        CommandLine cmd = new CommandLine(command);
        ByteArrayOutputStream outBa = new ByteArrayOutputStream();
        cmd.setOut(new PrintWriter(new java.io.OutputStreamWriter(outBa, UTF8)));
        cmd.setErr(new PrintWriter(new ByteArrayOutputStream()));

        int exitCode = cmd.execute("foo", "--json");

        cmd.getOut().flush();
        assertThat(exitCode).isZero();
        String stdout = new String(outBa.toByteArray(), UTF8).trim();
        JsonNode root = JSON.readTree(stdout);
        assertThat(root.has("results")).isTrue();
        assertThat(root.has("total")).isTrue();
        assertThat(root.has("page")).isTrue();
        assertThat(root.has("size")).isTrue();
        assertThat(root.has("query")).isTrue();
        assertThat(root.has("took_ms")).isTrue();
        assertThat(root.has("facets")).isTrue();
        assertThat(root.get("results").isArray()).isTrue();
        assertThat(root.get("results").size()).isEqualTo(1);
        JsonNode first = root.get("results").get(0);
        assertThat(first.has("source_file")).isTrue();
        assertThat(first.has("entity_name")).isTrue();
        assertThat(first.has("score")).isTrue();
    }

    @Test
    @DisplayName("--json --quiet output is JSON array of results")
    void execute_jsonQuiet_outputIsResultsArray() throws Exception {
        SearchOrchestrator mockOrchestrator = mock(SearchOrchestrator.class);
        List<ResultMerger.MergedResult> merged = createMockMergedResults(2);
        when(mockOrchestrator.orchestrate(any(), eq(io.megabrain.core.SearchMode.HYBRID), anyInt(), anyInt()))
                .thenReturn(Uni.createFrom().item(new SearchOrchestrator.OrchestratorResult(merged, Map.of())));

        SearchResultFormatter formatter = new HumanReadableSearchResultFormatter();
        SearchCommand command = new SearchCommand(mockOrchestrator, formatter, JSON, 10, 5, 10);
        CommandLine cmd = new CommandLine(command);
        ByteArrayOutputStream outBa = new ByteArrayOutputStream();
        cmd.setOut(new PrintWriter(new java.io.OutputStreamWriter(outBa, UTF8)));
        cmd.setErr(new PrintWriter(new ByteArrayOutputStream()));

        int exitCode = cmd.execute("foo", "--json", "--quiet");

        cmd.getOut().flush();
        assertThat(exitCode).isZero();
        String stdout = new String(outBa.toByteArray(), UTF8).trim();
        JsonNode arr = JSON.readTree(stdout);
        assertThat(arr.isArray()).isTrue();
        assertThat(arr.size()).isEqualTo(2);
        assertThat(arr.get(0).has("source_file")).isTrue();
        assertThat(arr.get(0).has("entity_name")).isTrue();
    }

    @Test
    @DisplayName("--json empty results: full JSON has results=[], total=0")
    void execute_jsonEmptyResults_fullJsonHasEmptyResultsAndTotalZero() throws Exception {
        SearchOrchestrator mockOrchestrator = mock(SearchOrchestrator.class);
        when(mockOrchestrator.orchestrate(any(), eq(io.megabrain.core.SearchMode.HYBRID), anyInt(), anyInt()))
                .thenReturn(Uni.createFrom().item(new SearchOrchestrator.OrchestratorResult(List.of(), Map.of())));

        SearchResultFormatter formatter = new HumanReadableSearchResultFormatter();
        SearchCommand command = new SearchCommand(mockOrchestrator, formatter, JSON, 10, 5, 10);
        CommandLine cmd = new CommandLine(command);
        ByteArrayOutputStream outBa = new ByteArrayOutputStream();
        cmd.setOut(new PrintWriter(new java.io.OutputStreamWriter(outBa, UTF8)));
        cmd.setErr(new PrintWriter(new ByteArrayOutputStream()));

        int exitCode = cmd.execute("query", "--json");

        cmd.getOut().flush();
        assertThat(exitCode).isZero();
        String stdout = new String(outBa.toByteArray(), UTF8).trim();
        JsonNode root = JSON.readTree(stdout);
        assertThat(root.get("results").isArray()).isTrue();
        assertThat(root.get("results").size()).isZero();
        assertThat(root.get("total").asLong()).isZero();
    }

    @Test
    @DisplayName("--json --quiet empty results: output is empty array")
    void execute_jsonQuietEmptyResults_outputIsEmptyArray() throws Exception {
        SearchOrchestrator mockOrchestrator = mock(SearchOrchestrator.class);
        when(mockOrchestrator.orchestrate(any(), eq(io.megabrain.core.SearchMode.HYBRID), anyInt(), anyInt()))
                .thenReturn(Uni.createFrom().item(new SearchOrchestrator.OrchestratorResult(List.of(), Map.of())));

        SearchResultFormatter formatter = new HumanReadableSearchResultFormatter();
        SearchCommand command = new SearchCommand(mockOrchestrator, formatter, JSON, 10, 5, 10);
        CommandLine cmd = new CommandLine(command);
        ByteArrayOutputStream outBa = new ByteArrayOutputStream();
        cmd.setOut(new PrintWriter(new java.io.OutputStreamWriter(outBa, UTF8)));
        cmd.setErr(new PrintWriter(new ByteArrayOutputStream()));

        int exitCode = cmd.execute("query", "--json", "--quiet");

        cmd.getOut().flush();
        assertThat(exitCode).isZero();
        String stdout = new String(outBa.toByteArray(), UTF8).trim();
        JsonNode arr = JSON.readTree(stdout);
        assertThat(arr.isArray()).isTrue();
        assertThat(arr.size()).isZero();
    }

    @Test
    @DisplayName("--no-color is parsed and useColor false passed to formatter")
    void execute_noColor_passesUseColorFalseToFormatter() {
        SearchOrchestrator mockOrchestrator = mock(SearchOrchestrator.class);
        List<ResultMerger.MergedResult> merged = createMockMergedResults(1);
        when(mockOrchestrator.orchestrate(any(), eq(io.megabrain.core.SearchMode.HYBRID), anyInt(), anyInt()))
                .thenReturn(Uni.createFrom().item(new SearchOrchestrator.OrchestratorResult(merged, Map.of())));

        CaptureUseColorFormatter captureFormatter = new CaptureUseColorFormatter();
        SearchCommand command = new SearchCommand(mockOrchestrator, captureFormatter, JSON, 10, 5, 10);
        CommandLine cmd = new CommandLine(command);
        ByteArrayOutputStream outBa = new ByteArrayOutputStream();
        cmd.setOut(new PrintWriter(new java.io.OutputStreamWriter(outBa, UTF8)));
        cmd.setErr(new PrintWriter(new ByteArrayOutputStream()));

        cmd.execute("foo", "--no-color");

        cmd.getOut().flush();
        assertThat(captureFormatter.lastUseColor).isFalse();
    }

    @Test
    @DisplayName("output with --no-color contains no ANSI escape")
    void execute_noColor_stdoutHasNoAnsi() {
        SearchOrchestrator mockOrchestrator = mock(SearchOrchestrator.class);
        List<ResultMerger.MergedResult> merged = createMockMergedResults(1);
        when(mockOrchestrator.orchestrate(any(), eq(io.megabrain.core.SearchMode.HYBRID), anyInt(), anyInt()))
                .thenReturn(Uni.createFrom().item(new SearchOrchestrator.OrchestratorResult(merged, Map.of())));

        SearchResultFormatter formatter = new HumanReadableSearchResultFormatter(new CliSyntaxHighlighter());
        SearchCommand command = new SearchCommand(mockOrchestrator, formatter, JSON, 10, 5, 10);
        CommandLine cmd = new CommandLine(command);
        ByteArrayOutputStream outBa = new ByteArrayOutputStream();
        cmd.setOut(new PrintWriter(new java.io.OutputStreamWriter(outBa, UTF8)));
        cmd.setErr(new PrintWriter(new ByteArrayOutputStream()));

        cmd.execute("foo", "--no-color");

        cmd.getOut().flush();
        String stdout = new String(outBa.toByteArray(), UTF8);
        assertThat(stdout).doesNotContain("\u001B[");
    }

    // ---------- T6: council-recommended and coverage tests ----------

    @Test
    @DisplayName("orchestrator failure returns exit 1 and stderr contains Search failed or cause message")
    void execute_orchestratorFailure_exit1AndStderrContainsSearchFailed() {
        SearchOrchestrator mockOrchestrator = mock(SearchOrchestrator.class);
        when(mockOrchestrator.orchestrate(any(), eq(io.megabrain.core.SearchMode.HYBRID), anyInt(), anyInt()))
                .thenReturn(Uni.createFrom().failure(new RuntimeException("orchestrator error")));

        SearchResultFormatter formatter = new HumanReadableSearchResultFormatter();
        SearchCommand command = new SearchCommand(mockOrchestrator, formatter, JSON, 10, 5, 10);
        CommandLine cmd = new CommandLine(command);
        ByteArrayOutputStream outBa = new ByteArrayOutputStream();
        ByteArrayOutputStream errBa = new ByteArrayOutputStream();
        cmd.setOut(new PrintWriter(new java.io.OutputStreamWriter(outBa, UTF8)));
        cmd.setErr(new PrintWriter(new java.io.OutputStreamWriter(errBa, UTF8)));

        int exitCode = cmd.execute("foo");

        cmd.getErr().flush();
        assertThat(exitCode).isEqualTo(1);
        String stderr = new String(errBa.toByteArray(), UTF8);
        assertThat(stderr).satisfiesAnyOf(
            s -> assertThat(s).contains("Search failed"),
            s -> assertThat(s).contains("orchestrator error")
        );
    }

    @Test
    @DisplayName("--json with null ObjectMapper returns exit 1 and message about JSON requiring ObjectMapper")
    void execute_jsonWithNullObjectMapper_exit1AndMessageAboutObjectMapper() {
        SearchOrchestrator mockOrchestrator = mock(SearchOrchestrator.class);
        List<ResultMerger.MergedResult> merged = createMockMergedResults(1);
        when(mockOrchestrator.orchestrate(any(), eq(io.megabrain.core.SearchMode.HYBRID), anyInt(), anyInt()))
                .thenReturn(Uni.createFrom().item(new SearchOrchestrator.OrchestratorResult(merged, Map.of())));

        SearchResultFormatter formatter = new HumanReadableSearchResultFormatter();
        SearchCommand command = new SearchCommand(mockOrchestrator, formatter, null, 10, 5, 10);
        CommandLine cmd = new CommandLine(command);
        ByteArrayOutputStream outBa = new ByteArrayOutputStream();
        ByteArrayOutputStream errBa = new ByteArrayOutputStream();
        cmd.setOut(new PrintWriter(new java.io.OutputStreamWriter(outBa, UTF8)));
        cmd.setErr(new PrintWriter(new java.io.OutputStreamWriter(errBa, UTF8)));

        int exitCode = cmd.execute("foo", "--json");

        cmd.getErr().flush();
        assertThat(exitCode).isEqualTo(1);
        String stderr = new String(errBa.toByteArray(), UTF8);
        assertThat(stderr).contains("JSON").contains("ObjectMapper");
    }

    @Test
    @DisplayName("JSON serialization failure returns exit 1 and message about JSON serialization failed")
    void execute_jsonSerializationFailure_exit1AndMessageAboutSerialization() throws Exception {
        SearchOrchestrator mockOrchestrator = mock(SearchOrchestrator.class);
        List<ResultMerger.MergedResult> merged = createMockMergedResults(1);
        when(mockOrchestrator.orchestrate(any(), eq(io.megabrain.core.SearchMode.HYBRID), anyInt(), anyInt()))
                .thenReturn(Uni.createFrom().item(new SearchOrchestrator.OrchestratorResult(merged, Map.of())));

        ObjectMapper failingMapper = spy(new ObjectMapper());
        doThrow(new IOException("mock io")).when(failingMapper).writeValue(any(java.io.Writer.class), any());

        SearchResultFormatter formatter = new HumanReadableSearchResultFormatter();
        SearchCommand command = new SearchCommand(mockOrchestrator, formatter, failingMapper, 10, 5, 10);
        CommandLine cmd = new CommandLine(command);
        ByteArrayOutputStream outBa = new ByteArrayOutputStream();
        ByteArrayOutputStream errBa = new ByteArrayOutputStream();
        cmd.setOut(new PrintWriter(new java.io.OutputStreamWriter(outBa, UTF8)));
        cmd.setErr(new PrintWriter(new java.io.OutputStreamWriter(errBa, UTF8)));

        int exitCode = cmd.execute("foo", "--json");

        cmd.getErr().flush();
        assertThat(exitCode).isEqualTo(1);
        String stderr = new String(errBa.toByteArray(), UTF8);
        assertThat(stderr).contains("JSON serialization failed").contains("mock io");
    }

    @Test
    @DisplayName("NO_COLOR env without --no-color passes useColor false to formatter")
    void execute_noColorEnvWithoutNoColorFlag_useColorFalse() {
        environmentVariables.set("NO_COLOR", "1");

        SearchOrchestrator mockOrchestrator = mock(SearchOrchestrator.class);
        List<ResultMerger.MergedResult> merged = createMockMergedResults(1);
        when(mockOrchestrator.orchestrate(any(), eq(io.megabrain.core.SearchMode.HYBRID), anyInt(), anyInt()))
                .thenReturn(Uni.createFrom().item(new SearchOrchestrator.OrchestratorResult(merged, Map.of())));

        CaptureUseColorFormatter captureFormatter = new CaptureUseColorFormatter();
        SearchCommand command = new SearchCommand(mockOrchestrator, captureFormatter, JSON, 10, 5, 10);
        CommandLine cmd = new CommandLine(command);
        ByteArrayOutputStream outBa = new ByteArrayOutputStream();
        cmd.setOut(new PrintWriter(new java.io.OutputStreamWriter(outBa, UTF8)));
        cmd.setErr(new PrintWriter(new ByteArrayOutputStream()));

        cmd.execute("foo");

        cmd.getOut().flush();
        assertThat(captureFormatter.lastUseColor).isFalse();
    }

    @Test
    @DisplayName("--language with blank value is skipped in request")
    void execute_languageBlankValue_skippedInRequest() {
        SearchCommand command = new SearchCommand();
        CommandLine cmd = new CommandLine(command);
        runAndGetOut(cmd, "q", "--language", "  ");

        assertThat(command.getSearchRequest().getLanguages()).isEmpty();
    }

    @Test
    @DisplayName("--language JAVA normalized to java in request")
    void execute_languageUppercase_normalizedToLowercase() {
        SearchCommand command = new SearchCommand();
        CommandLine cmd = new CommandLine(command);
        runAndGetOut(cmd, "q", "--language", "JAVA");

        assertThat(command.getSearchRequest().getLanguages()).containsExactly("java");
    }

    @Test
    @DisplayName("--quiet human-readable: formatter called with quiet true, one line per result")
    void execute_quietHumanReadable_formatterQuietTrueOneLinePerResult() {
        SearchOrchestrator mockOrchestrator = mock(SearchOrchestrator.class);
        List<ResultMerger.MergedResult> merged = createMockMergedResults(2);
        when(mockOrchestrator.orchestrate(any(), eq(io.megabrain.core.SearchMode.HYBRID), anyInt(), anyInt()))
                .thenReturn(Uni.createFrom().item(new SearchOrchestrator.OrchestratorResult(merged, Map.of())));

        CaptureUseColorFormatter captureFormatter = new CaptureUseColorFormatter();
        SearchCommand command = new SearchCommand(mockOrchestrator, captureFormatter, JSON, 10, 5, 10);
        CommandLine cmd = new CommandLine(command);
        ByteArrayOutputStream outBa = new ByteArrayOutputStream();
        cmd.setOut(new PrintWriter(new java.io.OutputStreamWriter(outBa, UTF8)));
        cmd.setErr(new PrintWriter(new ByteArrayOutputStream()));

        cmd.execute("foo", "--quiet");

        cmd.getOut().flush();
        assertThat(captureFormatter.lastQuiet).isTrue();
        String stdout = new String(outBa.toByteArray(), UTF8);
        assertThat(stdout).contains("Test0.java\tTestEntity0");
        assertThat(stdout).contains("Test1.java\tTestEntity1");
        assertThat(stdout).doesNotContain("---");
    }

    @Test
    @DisplayName("--json with non-empty facets outputs facets key in JSON")
    void execute_jsonWithFacets_outputHasFacetsKey() throws Exception {
        Map<String, List<io.megabrain.core.FacetValue>> facets = Map.of("language", List.of(
            new io.megabrain.core.FacetValue("java", 5),
            new io.megabrain.core.FacetValue("python", 2)
        ));
        SearchOrchestrator mockOrchestrator = mock(SearchOrchestrator.class);
        List<ResultMerger.MergedResult> merged = createMockMergedResults(1);
        when(mockOrchestrator.orchestrate(any(), eq(io.megabrain.core.SearchMode.HYBRID), anyInt(), anyInt()))
                .thenReturn(Uni.createFrom().item(new SearchOrchestrator.OrchestratorResult(merged, facets)));

        SearchResultFormatter formatter = new HumanReadableSearchResultFormatter();
        SearchCommand command = new SearchCommand(mockOrchestrator, formatter, JSON, 10, 5, 10);
        CommandLine cmd = new CommandLine(command);
        ByteArrayOutputStream outBa = new ByteArrayOutputStream();
        cmd.setOut(new PrintWriter(new java.io.OutputStreamWriter(outBa, UTF8)));
        cmd.setErr(new PrintWriter(new ByteArrayOutputStream()));

        int exitCode = cmd.execute("foo", "--json");

        cmd.getOut().flush();
        assertThat(exitCode).isZero();
        String stdout = new String(outBa.toByteArray(), UTF8).trim();
        JsonNode root = JSON.readTree(stdout);
        assertThat(root.has("facets")).isTrue();
        assertThat(root.get("facets").has("language")).isTrue();
        assertThat(root.get("facets").get("language").isArray()).isTrue();
        assertThat(root.get("facets").get("language").size()).isEqualTo(2);
    }

    /** Formatter that records the last useColor argument for testing. */
    private static final class CaptureUseColorFormatter implements SearchResultFormatter {
        Boolean lastUseColor = null;
        Boolean lastQuiet = null;

        @Override
        public String format(io.megabrain.api.SearchResponse response) {
            return format(response, false, true);
        }

        @Override
        public String format(io.megabrain.api.SearchResponse response, boolean quiet, boolean useColor) {
            this.lastUseColor = useColor;
            this.lastQuiet = quiet;
            if (quiet) {
                return formatQuiet(response);
            }
            if (response == null || response.getResults() == null || response.getResults().isEmpty()) {
                return "No results.";
            }
            StringBuilder sb = new StringBuilder();
            for (io.megabrain.api.SearchResult r : response.getResults()) {
                sb.append(r.getSourceFile()).append("\n");
            }
            return sb.toString();
        }

        @Override
        public String formatQuiet(io.megabrain.api.SearchResponse response) {
            if (response == null || response.getResults() == null || response.getResults().isEmpty()) {
                return "No results.";
            }
            StringBuilder sb = new StringBuilder();
            for (io.megabrain.api.SearchResult r : response.getResults()) {
                sb.append(r.getSourceFile()).append("\t").append(r.getEntityName()).append("\n");
            }
            return sb.toString();
        }
    }

    private static List<ResultMerger.MergedResult> createMockMergedResults(int count) {
        List<ResultMerger.MergedResult> results = new java.util.ArrayList<>();
        for (int i = 0; i < count; i++) {
            Document doc = new Document();
            doc.add(new StringField("content", "Test content " + i, Field.Store.YES));
            doc.add(new StringField("entity_name", "TestEntity" + i, Field.Store.YES));
            doc.add(new StringField("entity_type", "class", Field.Store.YES));
            doc.add(new StringField("source_file", "Test" + i + ".java", Field.Store.YES));
            doc.add(new StringField("language", "java", Field.Store.YES));
            doc.add(new StringField("repository", "test-repo", Field.Store.YES));
            doc.add(new StringField("start_line", "1", Field.Store.YES));
            doc.add(new StringField("end_line", "10", Field.Store.YES));
            String chunkId = "Test" + i + ".java:TestEntity" + i;
            results.add(ResultMerger.MergedResult.fromLucene(chunkId, doc, 0.8));
        }
        return results;
    }
}
