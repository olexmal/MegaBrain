/*
 * Copyright (c) 2025 MegaBrain Contributors
 * Licensed under the MIT License - see LICENSE file for details.
 */

package io.megabrain.cli;

import io.quarkus.picocli.runtime.annotations.TopCommand;
import picocli.CommandLine;

/**
 * Top-level CLI command for MegaBrain. Dispatches to subcommands such as {@code ingest}.
 */
@TopCommand
@CommandLine.Command(
    name = "megabrain",
    description = "MegaBrain CLI: ingest repositories and search code.",
    mixinStandardHelpOptions = true,
    subcommands = { IngestCommand.class }
)
public class MegaBrainCommand implements Runnable {

    @Override
    public void run() {
        // No subcommand specified: help is shown by Picocli when --help is used
    }
}
