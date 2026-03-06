<!--
Copyright (c) 2025 MegaBrain Contributors
Licensed under the MIT License - see LICENSE file for details.
-->

# CLI Reference

The MegaBrain CLI provides commands to ingest repositories and search code from the command line.

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

### megabrain search

Search the MegaBrain index from the command line. Provide a query string as the first argument. Optional filters and output options are supported.

**Options:**

| Option | Required | Default | Description |
|:-------|:---------|:--------|:-------------|
| `<query>` | Yes | - | Search query string (first positional argument). |
| `--language` | No | - | Filter by programming language (repeatable). Allowed: java, python, javascript, typescript, go, rust, kotlin, ruby, scala, swift, php, c, cpp. |
| `--repo` | No | - | Filter by repository name or identifier (repeatable). |
| `--type` | No | - | Filter by entity type (repeatable). Allowed: class, method, function, field, interface, enum, module. |
| `--limit` | No | `10` | Maximum number of results (1–100). |
| `--json` | No | `false` | Output results as JSON (see T5). |
| `--quiet` | No | `false` | Minimal output, pipe-friendly (see T5). |
| `--help` | No | - | Show usage and options. |

**Validation:** Query must be non-blank. Each `--language` and `--type` value must be from the allowed sets above. `--limit` must be between 1 and 100. Invalid values produce exit code 2 and an error message listing allowed values.

**Examples:**

```bash
# Show usage and all options
megabrain search --help

# Basic search
megabrain search "authentication"

# With filters and limit
megabrain search "service" --language java --language python --type class --limit 5

# Filter by repository
megabrain search "config" --repo olexmal/MegaBrain --limit 20
```

**Exit codes**

| Code | Meaning |
|:-----|:--------|
| `0` | Success |
| `1` | Execution failure (e.g. search backend error) |
| `2` | Invalid arguments (missing/blank query, invalid --language/--type/--limit) |

### Verbose / debugging

Use `--verbose` to enable detailed progress (no message truncation), debug logging for the `io.megabrain` logger, and full stack traces on ingestion failure. Example: `megabrain ingest --source github --repo owner/repo --verbose`.

### Progress output

Progress is shown in real time during ingestion. Typical stages include **cloning**, **parsing**, and **indexing**. When the output is a **TTY** (interactive terminal), progress updates in place on a single line; when not a TTY (e.g. redirect or CI), each event is printed on a new line. Message length is capped for readability.

### Top-level help

```bash
megabrain --help
```

Shows available subcommands (e.g. `ingest`, `search`).
