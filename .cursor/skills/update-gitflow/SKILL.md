---
name: update-gitflow
description: Updates or reviews GitHub Actions workflows (CI and docs deploy). When the user says "update gitflow", "change CI", "update workflows", or similar, the agent focuses on .github/workflows/ci.yml and .github/workflows/docs.yml, applies the requested changes, and respects the project's workflow documentation in .cursor/rules/github-workflows.mdc.
---

# Update Gitflow (GitHub Actions Workflows)

This skill focuses edits and reviews on the project’s GitHub Actions workflow files so that "update gitflow"-style requests are applied to the right files with the right context.

## When to Use

Apply this skill when the user:

- Says **"update gitflow"**, **"update git flow"**, or **"change gitflow"**
- Asks to **update**, **change**, or **review** the **CI** or **GitHub Actions** workflows
- Mentions **workflows**, **CI pipeline**, **docs deploy**, or **GitHub Actions** in a way that implies editing the YAML

## Target Files

Always work with these two workflow files:

| File | Purpose |
|------|--------|
| **`.github/workflows/ci.yml`** | Main CI: backend/frontend tests, Codecov, SonarQube, optional benchmark and performance jobs |
| **`.github/workflows/docs.yml`** | Docs deploy: MkDocs build and publish to GitHub Pages |

If the user’s request applies to only one of them (e.g. "add a step to CI"), edit only that file. If it’s generic ("update gitflow"), read both and apply changes to whichever is relevant.

## What to Do

1. **Load context**
   - Read [.cursor/rules/github-workflows.mdc](../../rules/github-workflows.mdc) for triggers, jobs, and conventions.
   - Read the relevant workflow file(s): [.github/workflows/ci.yml](../../../.github/workflows/ci.yml) and/or [.github/workflows/docs.yml](../../../.github/workflows/docs.yml).

2. **Apply the user’s request**
   - Implement the requested changes in the appropriate workflow(s).
   - Preserve existing structure (triggers, job names, dependencies, manual-only jobs like `benchmark` and `test-backend-performance`).
   - Keep `ci.yml` and `docs.yml` consistent with the behavior described in the rule (e.g. performance tests only on `workflow_dispatch`).

3. **Confirm**
   - Summarize what was changed and in which file(s).

## Conventions (from github-workflows.mdc)

- **ci.yml:** Runs on push to `main`/`dev`, PRs, and `workflow_dispatch`. Jobs: `test-backend`, `test-frontend`, `sonar`, `sonar-frontend` run every time; `benchmark` and `test-backend-performance` only on manual run.
- **docs.yml:** Runs on push to `main` when `docs/**` or `mkdocs.yml` change, and on `workflow_dispatch`. Single job: build and deploy with MkDocs.

When adding or changing jobs or steps, align with these conventions and update the rule if the workflow behavior changes.
