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
| `--json` | No | `false` | Output results as JSON (see [JSON output](#json-output--json)). |
| `--quiet` | No | `false` | Minimal output, pipe-friendly (with `--json`: results array only; otherwise one line per result). |
| `--no-color` | No | `false` | Disable syntax highlighting and ANSI color in output. |
| `--help` | No | - | Show usage and options. |

#### JSON output (`--json`)

When `--json` is set, output matches the REST API search response format: `results`, `total`, `page`, `size`, `query`, `took_ms`, `facets`. With `--quiet`, only the `results` array is printed (no wrapper object). Pretty-printing is used when output is a TTY and `--quiet` and `--no-color` are not set; otherwise output is compact (e.g. for piping or scripting).

**Validation:** Query must be non-blank. Each `--language` and `--type` value must be from the allowed sets above. `--limit` must be between 1 and 100. Invalid values produce exit code 2 and an error message listing allowed values.

**Default output (human-readable):** When `--json` is not set, results are printed in a readable layout. Each result shows:

- **File:** Source file path
- **Entity:** Code entity name (e.g. class or method)
- **Score:** Relevance score
- A code snippet (content), **syntax-highlighted** when color is enabled (see below)

Results are separated by `---`. Optional header shows query, total count, and time taken (ms). Long snippets are truncated: at most 15 lines and 120 characters per line; excess is replaced with “… (truncated)” or “…” at line end. Null or blank path/entity are shown as “(no path)” and “(no entity)”. If there are no results, the output is “No results.” With `--quiet`, output is minimal: one line per result with path and entity separated by tab, suitable for piping.

**Syntax highlighting:** Snippets are language-aware (Java, Python, JavaScript, TypeScript; other languages shown as plain text). Color is enabled when the output is a TTY and neither `--no-color` nor the `NO_COLOR` environment variable is set. Use `--no-color` to force plain output (e.g. when piping or in CI).

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

# Plain output (no syntax highlighting), e.g. for piping or CI
megabrain search "service" --no-color

# JSON output for scripting (full object or results-only with --quiet)
megabrain search "service" --json
megabrain search "service" --json --quiet
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
