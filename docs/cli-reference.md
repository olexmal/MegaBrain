<!--
Copyright (c) 2025 MegaBrain Contributors
Licensed under the MIT License - see LICENSE file for details.
-->

# CLI Reference

The MegaBrain CLI provides commands to ingest repositories and (when implemented) search code from the command line.

## Running the CLI

After building the backend, run the CLI with Java or the native executable:

```bash
cd backend
mvn package
java -jar target/quarkus-app/quarkus-run.jar [command] [options]
```

Or with the native binary (after building with `-Dquarkus.native.enabled=true`):

```bash
./target/megabrain-runner [command] [options]
```

## Commands

### megabrain ingest

Ingest a repository (GitHub, GitLab, Bitbucket, or local path) into the MegaBrain index.

**Usage:**

```bash
megabrain ingest --help
```

Options (e.g. `--source`, `--repo`, `--branch`) are added in later tasks. See [API Reference](api-reference.md#cli) and [Implemented Features](implemented-features.md#us-04-04-cli-ingest-command-partial--t1-of-6) for current status.

### Top-level help

```bash
megabrain --help
```

Shows available subcommands (e.g. `ingest`).
