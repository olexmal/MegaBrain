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

**Options:**

| Option | Required | Default | Description |
|:-------|:---------|:--------|:-------------|
| `--source` | Yes | - | Source type: `github`, `gitlab`, `bitbucket`, or `local`. |
| `--repo` | Yes | - | Repository URL or identifier (e.g. `owner/repo` or file path for local). |
| `--branch` | No | `main` | Branch to ingest. |
| `--token` | No | - | Authentication token for private repositories (never logged). |
| `--incremental` | No | `false` | Perform incremental ingestion. |
| `--verbose` | No | `false` | Show detailed progress, debug messages, and stack traces on errors. |
| `--help` | No | - | Show usage and options. |

**Examples:**

```bash
# Show usage and all options
megabrain ingest --help

# Ingest a GitHub repository (default branch: main)
megabrain ingest --source github --repo olexmal/MegaBrain

# Ingest a specific branch with optional token
megabrain ingest --source github --repo owner/private-repo --branch develop --token YOUR_TOKEN

# Incremental ingestion
megabrain ingest --source github --repo olexmal/MegaBrain --incremental

# Local path
megabrain ingest --source local --repo /path/to/repo --branch main
```

**Exit codes**

| Code | Meaning |
|:-----|:--------|
| `0` | Success |
| `1` | Execution or ingestion failure (e.g. clone/parse/index error) |
| `2` | Invalid arguments (e.g. invalid `--source`, missing or blank `--repo`) |

Use these in scripts or CI to detect success or failure (e.g. `megabrain ingest ...; exit $?`).

### Verbose / debugging

Use `--verbose` to enable detailed progress (no message truncation), debug logging for the `io.megabrain` logger, and full stack traces on ingestion failure. Example: `megabrain ingest --source github --repo owner/repo --verbose`.

### Progress output

Progress is shown in real time during ingestion. Typical stages include **cloning**, **parsing**, and **indexing**. When the output is a **TTY** (interactive terminal), progress updates in place on a single line; when not a TTY (e.g. redirect or CI), each event is printed on a new line. Message length is capped for readability.

### Top-level help

```bash
megabrain --help
```

Shows available subcommands (e.g. `ingest`).
