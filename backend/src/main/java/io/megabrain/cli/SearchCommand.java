/*
 * Copyright (c) 2025 MegaBrain Contributors
 * Licensed under the MIT License - see LICENSE file for details.
 */

package io.megabrain.cli;

import io.megabrain.api.SearchRequest;
import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;
import picocli.CommandLine;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * CLI command to search the MegaBrain index from the command line.
 * Exit codes: 0 = success, 1 = execution failure, 2 = invalid arguments (e.g. missing or blank query).
 * Use {@code megabrain search --help} for usage.
 */
@ApplicationScoped
@CommandLine.Command(
    name = "search",
    description = "Search the MegaBrain index. Provide a query string; optional filters: --language, --repo, --type, --limit, --json, --quiet.",
    mixinStandardHelpOptions = true,
    exitCodeOnInvalidInput = 2,
    exitCodeOnExecutionException = 1
)
public class SearchCommand implements Runnable {

    private static final Logger LOG = Logger.getLogger(SearchCommand.class);

    /** Supported languages (aligned with Tree-sitter/grammar). */
    private static final Set<String> SUPPORTED_LANGUAGES = Set.of(
        "java", "python", "javascript", "typescript", "go", "rust", "kotlin",
        "ruby", "scala", "swift", "php", "c", "cpp"
    );

    /** Supported entity types for --type filter. */
    private static final Set<String> SUPPORTED_ENTITY_TYPES = Set.of(
        "class", "method", "function", "field", "interface", "enum", "module"
    );

    @CommandLine.Spec
    CommandLine.Model.CommandSpec spec;

    @CommandLine.Parameters(
        index = "0",
        description = "Search query string.",
        paramLabel = "<query>"
    )
    String query;

    @CommandLine.Option(
        names = "--language",
        description = "Filter by programming language (repeatable). Allowed: java, python, javascript, typescript, go, rust, kotlin, ruby, scala, swift, php, c, cpp.",
        paramLabel = "LANG"
    )
    List<String> language;

    @CommandLine.Option(
        names = "--repo",
        description = "Filter by repository name or identifier (repeatable).",
        paramLabel = "REPO"
    )
    List<String> repo;

    @CommandLine.Option(
        names = "--type",
        description = "Filter by entity type (repeatable). Allowed: class, method, function, field, interface, enum, module.",
        paramLabel = "TYPE"
    )
    List<String> type;

    @CommandLine.Option(
        names = "--limit",
        description = "Maximum number of results to return (1-100).",
        paramLabel = "N",
        defaultValue = "10"
    )
    int limit;

    @CommandLine.Option(
        names = "--json",
        description = "Output results as JSON (for T3/T5).",
        defaultValue = "false"
    )
    boolean json;

    @CommandLine.Option(
        names = "--quiet",
        description = "Minimal output, pipe-friendly (for T3/T5).",
        defaultValue = "false"
    )
    boolean quiet;

    /** Built after validation; used by T3/T5 for actual search and formatting. */
    private SearchRequest searchRequest;

    /**
     * Returns the validated search request built in run(). Null until run() has been called successfully.
     *
     * @return the SearchRequest with query and filters set, or null
     */
    public SearchRequest getSearchRequest() {
        return searchRequest;
    }

    @Override
    public void run() {
        if (query == null || query.isBlank()) {
            throw new CommandLine.ParameterException(
                spec.commandLine(),
                "Search query is required and must be non-blank."
            );
        }
        String trimmedQuery = query.trim();

        List<String> languages = language != null ? new ArrayList<>(language) : new ArrayList<>();
        List<String> repos = repo != null ? new ArrayList<>(repo) : new ArrayList<>();
        List<String> types = type != null ? new ArrayList<>(type) : new ArrayList<>();

        validateLanguages(languages);
        validateTypes(types);
        validateLimit();

        searchRequest = buildSearchRequest(trimmedQuery, languages, repos, types);

        LOG.debugf("Search command received query: %s, filters: language=%s, repo=%s, type=%s, limit=%d, json=%s, quiet=%s",
            trimmedQuery, languages, repos, types, limit, json, quiet);
        spec.commandLine().getOut().println("Query received: " + trimmedQuery);
        spec.commandLine().getOut().flush();
    }

    private void validateLanguages(List<String> languages) {
        for (String lang : languages) {
            String normalized = lang == null ? "" : lang.trim().toLowerCase();
            if (normalized.isEmpty()) {
                continue;
            }
            if (!SUPPORTED_LANGUAGES.contains(normalized)) {
                throw new CommandLine.ParameterException(
                    spec.commandLine(),
                    "Invalid --language '" + lang + "'. Allowed: " + String.join(", ", SUPPORTED_LANGUAGES) + "."
                );
            }
        }
    }

    private void validateTypes(List<String> types) {
        for (String t : types) {
            String normalized = t == null ? "" : t.trim().toLowerCase();
            if (normalized.isEmpty()) {
                continue;
            }
            if (!SUPPORTED_ENTITY_TYPES.contains(normalized)) {
                throw new CommandLine.ParameterException(
                    spec.commandLine(),
                    "Invalid --type '" + t + "'. Allowed: " + String.join(", ", SUPPORTED_ENTITY_TYPES) + "."
                );
            }
        }
    }

    private void validateLimit() {
        if (limit < 1 || limit > 100) {
            throw new CommandLine.ParameterException(
                spec.commandLine(),
                "Invalid --limit " + limit + ". Allowed range: 1-100."
            );
        }
    }

    private SearchRequest buildSearchRequest(String trimmedQuery, List<String> languages, List<String> repos, List<String> types) {
        SearchRequest req = new SearchRequest(trimmedQuery);
        for (String lang : languages) {
            if (lang != null && !lang.isBlank()) {
                req.addLanguage(lang.trim().toLowerCase());
            }
        }
        for (String r : repos) {
            if (r != null && !r.isBlank()) {
                req.addRepository(r.trim());
            }
        }
        for (String t : types) {
            if (t != null && !t.isBlank()) {
                req.addEntityType(t.trim().toLowerCase());
            }
        }
        req.setLimit(limit);
        return req;
    }
}
