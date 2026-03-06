/*
 * Copyright (c) 2025 MegaBrain Contributors
 * Licensed under the MIT License - see LICENSE file for details.
 */

package io.megabrain.cli;

import io.megabrain.api.IngestionResource;
import io.megabrain.ingestion.IngestionService;
import io.megabrain.ingestion.ProgressEvent;
import io.smallrye.mutiny.Multi;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;
import picocli.CommandLine;

import java.io.PrintWriter;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.jboss.logmanager.Level;

/**
 * CLI command to ingest a repository into the MegaBrain index.
 * Exit codes: 0 = success, 1 = execution/ingestion failure, 2 = invalid arguments.
 * Use {@code megabrain ingest --help} for usage.
 */
@ApplicationScoped
@CommandLine.Command(
    name = "ingest",
    description = "Ingest a repository (GitHub, GitLab, Bitbucket, or local path) into the MegaBrain index.",
    mixinStandardHelpOptions = true,
    exitCodeOnInvalidInput = 2,
    exitCodeOnExecutionException = 1
)
public class IngestCommand implements Runnable {

    private static final Logger LOG = Logger.getLogger(IngestCommand.class);
    private static final int MAX_MESSAGE_LENGTH = 200;

    private final IngestionService ingestionService;

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

    @CommandLine.Option(
        names = "--verbose",
        description = "Show detailed progress, debug messages, and stack traces on errors."
    )
    boolean verbose;

    @Inject
    public IngestCommand(IngestionService ingestionService) {
        this.ingestionService = ingestionService;
    }

    @Override
    public void run() {
        if (verbose) {
            org.jboss.logmanager.LogContext.getLogContext()
                .getLogger("io.megabrain")
                .setLevel(Level.DEBUG);
        }

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

        String repositoryUrl = repo.trim();
        Multi<ProgressEvent> progressStream = incremental
            ? ingestionService.ingestRepositoryIncrementally(repositoryUrl)
            : ingestionService.ingestRepository(repositoryUrl);

        boolean tty = System.console() != null;
        PrintWriter out = spec.commandLine().getOut();
        CountDownLatch latch = new CountDownLatch(1);
        AtomicBoolean failed = new AtomicBoolean(false);

        progressStream.subscribe().with(
            item -> {
                String msg = item.message() != null ? item.message() : "";
                if (!verbose && msg.length() > MAX_MESSAGE_LENGTH) {
                    msg = msg.substring(0, MAX_MESSAGE_LENGTH) + "...";
                }
                String line = String.format("%s %.1f%%", msg, item.progress());
                if (tty) {
                    out.print("\r" + line);
                    out.flush();
                } else {
                    out.println(line);
                    out.flush();
                }
            },
            err -> {
                if (verbose) {
                    LOG.error("Ingestion failed", err);
                } else {
                    LOG.errorf("Ingestion failed: %s", err.getMessage());
                }
                failed.set(true);
                latch.countDown();
            },
            latch::countDown
        );

        try {
            latch.await(1, TimeUnit.HOURS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOG.error("Ingestion interrupted");
            throw new CommandLine.ExecutionException(spec.commandLine(), "Interrupted");
        }
        if (tty) {
            out.println();
            out.flush();
        }
        if (failed.get()) {
            throw new CommandLine.ExecutionException(spec.commandLine(), "Ingestion failed.");
        }
    }
}
