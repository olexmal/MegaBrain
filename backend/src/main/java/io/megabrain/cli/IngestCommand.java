/*
 * Copyright (c) 2025 MegaBrain Contributors
 * Licensed under the MIT License - see LICENSE file for details.
 */

package io.megabrain.cli;

import io.megabrain.api.IngestionResource;
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

    @CommandLine.Spec
    CommandLine.Model.CommandSpec spec;

    @CommandLine.Option(
        names = "--source",
        required = true,
        description = "Source type: github, gitlab, bitbucket, or local."
    )
    String source;

    @CommandLine.Option(
        names = "--repo",
        required = true,
        description = "Repository URL or identifier (e.g. owner/repo or file path for local)."
    )
    String repo;

    @CommandLine.Option(
        names = "--branch",
        defaultValue = "main",
        description = "Branch to ingest (default: main)."
    )
    String branch;

    @CommandLine.Option(
        names = "--token",
        description = "Authentication token for private repositories (never logged)."
    )
    String token;

    @CommandLine.Option(
        names = "--incremental",
        defaultValue = "false",
        description = "Perform incremental ingestion (default: false)."
    )
    boolean incremental;

    @Override
    public void run() {
        IngestionResource.SourceType sourceType = IngestionResource.SourceType.fromString(source);
        if (sourceType == null) {
            throw new CommandLine.ParameterException(
                spec.commandLine(),
                "Invalid source: '" + source + "'. Allowed: github, gitlab, bitbucket, local."
            );
        }
        if (repo == null || repo.isBlank()) {
            throw new CommandLine.ParameterException(
                spec.commandLine(),
                "Repository (--repo) is required and must be non-blank."
            );
        }
        // T2: options validated; no ingestion call yet. Never log token.
        LOG.debugf("ingest command: source=%s, repo=%s, branch=%s, incremental=%s",
            sourceType, repo, branch, incremental);
    }
}
