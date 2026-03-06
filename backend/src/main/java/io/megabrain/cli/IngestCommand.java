/*
 * Copyright (c) 2025 MegaBrain Contributors
 * Licensed under the MIT License - see LICENSE file for details.
 */

package io.megabrain.cli;

import org.jboss.logging.Logger;
import picocli.CommandLine;

/**
 * CLI command to ingest a repository into the MegaBrain index.
 * Use {@code megabrain ingest --help} for usage.
 */
@CommandLine.Command(
    name = "ingest",
    description = "Ingest a repository (GitHub, GitLab, Bitbucket, or local path) into the MegaBrain index.",
    mixinStandardHelpOptions = true
)
public class IngestCommand implements Runnable {

    private static final Logger LOG = Logger.getLogger(IngestCommand.class);

    @Override
    public void run() {
        // T1: structure only; options and ingestion logic in T2–T4
        LOG.debug("ingest command invoked (no options yet)");
    }
}
