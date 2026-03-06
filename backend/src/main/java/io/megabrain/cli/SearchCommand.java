/*
 * Copyright (c) 2025 MegaBrain Contributors
 * Licensed under the MIT License - see LICENSE file for details.
 */

package io.megabrain.cli;

import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;
import picocli.CommandLine;

/**
 * CLI command to search the MegaBrain index from the command line.
 * Exit codes: 0 = success, 1 = execution failure, 2 = invalid arguments (e.g. missing or blank query).
 * Use {@code megabrain search --help} for usage.
 */
@ApplicationScoped
@CommandLine.Command(
    name = "search",
    description = "Search the MegaBrain index. Provide a query string; filters (e.g. --language, --repo) will be added in later tasks.",
    mixinStandardHelpOptions = true,
    exitCodeOnInvalidInput = 2,
    exitCodeOnExecutionException = 1
)
public class SearchCommand implements Runnable {

    private static final Logger LOG = Logger.getLogger(SearchCommand.class);

    @CommandLine.Spec
    CommandLine.Model.CommandSpec spec;

    @CommandLine.Parameters(
        index = "0",
        description = "Search query string.",
        paramLabel = "<query>"
    )
    String query;

    @Override
    public void run() {
        if (query == null || query.isBlank()) {
            throw new CommandLine.ParameterException(
                spec.commandLine(),
                "Search query is required and must be non-blank."
            );
        }
        String trimmedQuery = query.trim();
        LOG.debugf("Search command received query: %s", trimmedQuery);
        spec.commandLine().getOut().println("Query received: " + trimmedQuery);
        spec.commandLine().getOut().flush();
    }
}
