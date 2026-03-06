# Implement Story Tasks (All Tasks in Sequence)

Orchestrates implementation of all tasks from a MegaBrain `*-tasks.md` file in order. For each task T1, T2, … that is not yet completed, runs a **task council** (three subagents in parallel) to produce an implementation brief, then runs the implement-task workflow via a subagent using that brief (without the full test suite per task), commits the result, then runs the next task. When all tasks are implemented, runs the full test suite once via a test-runner subagent to fix any failures.

## Usage

```
/implement-story-tasks <path-to-task-file>
```

Example:
```
/implement-story-tasks features/user-stories/epic-03-rag/US-03-02-openai-integration-tasks.md
```

**Parameter:** `path-to-task-file` – Path to the tasks file (e.g. `US-03-01-ollama-integration-tasks.md` or full path from repo root). If the user only names the file, resolve it under `features/user-stories/` or repo root.

## Orchestration Rules

1. **Parse the task file**
   - Read the file at the given path.
   - Find every task section: headings matching `### T\d+:` (e.g. `### T1:`, `### T2:`).
   - For each section, determine status from the line `- **Status:** ...` (e.g. `Not Started`, `In Progress`, `Completed`).
   - Build an ordered list of tasks that are **not** `Completed`: only these will be implemented in this run.
   - If the list is empty, respond: "All tasks in this file are already completed." and stop.

2. **For each non-completed task in order (T1, then T2, …)**
   - Extract the **full task block** for that task (from the `### Tn:` heading through the end of that task section, i.e. until the next `### T...` or `---` / end of Task List).
   - Derive the **story id** from the file (e.g. from "Story: US-03-02" in the file or from the filename like `US-03-02-openai-integration-tasks.md`).

   - **2a. Task council** — Before implementing, run a council of three subagents in parallel to get recommendations. Invoke **three** `mcp_task` calls **in the same turn** (parallel), so the orchestrator waits for all three results before proceeding. Use the following:
     - **Shared context** (include in each council prompt): story id, task id, full task block, task file path, path to related user story (same directory, `-tasks.md` replaced by `.md`), and the project's `docs/` folder as the canonical place for documentation updates.
     - **Council 1 (best practices & implementation):** `subagent_type`: `code-reviewer` (or custom `council-best-practices` if available). **Prompt:** "You are acting as a task council member. Given the task below, recommend only: (1) best practices and patterns to follow for this codebase for this task, (2) best way to implement this task (interfaces, classes, error handling). Reply with a concise, actionable list. Do not implement code. Context: [story id], [task id], [full task block], [task file path], [related user story path], docs folder: docs/."
     - **Council 2 (unit testing):** `subagent_type`: `generalPurpose` (or custom `council-testing` if available). **Prompt:** "You are acting as a task council member. Given the task below, recommend only: best way to unit test this task (test classes, scenarios, mocks, coverage focus). Reply with a concise, actionable list. Do not implement code. Context: [story id], [task id], [full task block], [task file path], [related user story path], docs folder: docs/."
     - **Council 3 (documentation):** `subagent_type`: `doc-generator` (or custom `council-docs` if available). **Prompt:** "You are acting as a task council member. Given the task below, recommend only: what should be added or updated in the project's `docs/` folder (e.g. configuration-reference.md, getting-started.md, new or updated pages). Reply with a concise, actionable list. Do not edit files. Context: [story id], [task id], [full task block], [task file path], [related user story path], docs folder: docs/."

   - **2b. Synthesis** — From the three council replies, the **coordinator** (orchestrator) produces a short **implementation brief**: a single bullet list merging (1) best practices to follow, (2) recommended implementation approach, (3) unit-test approach, (4) docs updates for `docs/`. Keep the brief concise and actionable.

   - **Invoke a subagent** to implement this single task:
     - Use `mcp_task` with `subagent_type`: `generalPurpose`.
     - **Prompt** (provide full context; subagents have no prior context):
       - **Start with the council brief:** "Use the following council recommendations when implementing this task: [paste the implementation brief from 2b]. Then follow the implement-task process below."
       - Instruct the subagent to implement **only** this task (the given task id and task block).
       - Tell the subagent to follow the **implement-task** process in `.cursor/commands/implement-task.md` for the single task: task analysis, implementation planning, code, unit test authoring, and documentation/completion updates to the **tasks file** and the **related user story** file.
       - **Important:** Tell the subagent: "You are running as part of implement-story-tasks. **Do not run the full test suite** (`mvn clean install`) at the end of this task. Run only `mvn compile` and, if needed, specific tests (e.g. `mvn test -Dtest=ClassName`) to catch obvious errors. Skip the 'MANDATORY (Final Validation): mvn clean install' and the Documentation & Completion gate that requires build success. Still update the task status to Completed and update the tasks file and related user story. The orchestrator will run the full test suite once at the end of the story."
       - Give the **task file path** and the **exact task block** (copy the markdown for this task only).
       - Tell the subagent: "When the task is implemented and task/user story docs are updated, do **not** commit. Reply with exactly: `all done :)` and stop. The orchestrator will commit after you finish."
     - Do **not** run the next task until this subagent completes.
   - **After the subagent returns**
     - If the subagent's response indicates success (e.g. contains "all done :)") and the implementation is done:
       - Run: `git add .` and `git status` to see changes.
       - Commit with a message following the implement-task Git workflow, e.g.:
         `git commit -m "feat: implement <taskId>: <short description>\n\n- Key changes\n- Resolves <taskId>: <description>"`
       - Then proceed to the **next** task in the list (re-parse the task file if you want to skip any task that was marked Completed by the subagent; otherwise just advance to the next in your list).
     - If the subagent failed or did not respond with "all done :)", report the failure and ask the user whether to continue to the next task or stop.
   - After committing, **re-read the task file** to get the updated list of non-completed tasks for the next iteration (in case statuses changed). Or maintain the list and remove the current task; either way, process only non-completed tasks in order.

3. **When no tasks remain – run full test suite**
   - When every task in the file has `**Status:** Completed`, run the full backend build and tests once, and fix any failures:
     - Invoke a **test-runner** subagent via `mcp_task` with `subagent_type`: `test-runner`.
     - **Prompt:** "Run the full backend build and test suite: `mvn clean install` in the backend directory. Fix any compilation or test failures. Report when the build succeeds or if you cannot fix the failures."
     - Wait for the subagent to complete. If it reports success, optionally commit with a message like: `chore: verify story build – mvn clean install passes`. If it reports unresolved failures, report them to the user and do not commit.
   - Then respond that the whole story tasks file has been implemented, list the tasks completed in this run, and confirm whether the final build passed.

## Task File and User Story Conventions

- **Tasks file:** e.g. `US-03-02-openai-integration-tasks.md` – contains `### T1:`, `### T2:`, etc., each with `- **Status:** ...`.
- **Related user story:** Same directory, same base name with `-tasks.md` replaced by `.md` (e.g. `US-03-02-openai-integration.md`). The subagent must update both: task status and checkboxes in the tasks file, and Technical Tasks / Acceptance Criteria in the user story file.
- **Documentation:** The project's `docs/` folder is the canonical place for council-recommended documentation updates (e.g. configuration-reference.md, getting-started.md, new or updated pages).

## Task council

Before each task is implemented, a **council** of three subagents runs in parallel to produce recommendations. The **coordinator** (orchestrator) synthesizes their outputs into an **implementation brief** and passes it to the implement-task subagent.

- **Council 1 – Best practices & implementation:** Recommends patterns, architecture, and the best way to implement the task. Use `code-reviewer` or a custom agent such as `.cursor/agents/council-best-practices.md`.
- **Council 2 – Unit testing:** Recommends how to unit test the task (scenarios, mocks, coverage). Use `generalPurpose` with a testing-strategy prompt or a custom agent such as `.cursor/agents/council-testing.md`.
- **Council 3 – Documentation:** Recommends what to add or update in `docs/`. Use `doc-generator` or a custom agent such as `.cursor/agents/council-docs.md`.

Custom council agents under `.cursor/agents/` can set different `model` values (e.g. `inherit` for one, `fast` for others) for model diversity; the orchestrator invokes them by the agent name when available.

## Summary

| Step | Action |
|------|--------|
| 1 | Parse task file → ordered list of non-completed tasks (T1, T2, …). |
| 2 | If list empty → "All tasks already completed." → stop. |
| 2a–2b | For each task: run **task council** (3 subagents in parallel: code-reviewer / council-best-practices, generalPurpose / council-testing, doc-generator / council-docs) → coordinator **synthesizes implementation brief** → pass brief to implement-task subagent. |
| 3 | For each task in list: run **generalPurpose** subagent (implement-task for that task only, **with council brief** in prompt; **skip full mvn clean install**; use mvn compile / targeted tests only) → on "all done :)", commit → next task. |
| 4 | When all tasks Completed → run **test-runner** subagent: `mvn clean install`, fix failures → optionally commit → report done. |

Reference: [implement-task](.cursor/commands/implement-task.md) for the per-task process and subagent usage (explore, test-runner, verifier, etc.). The subagent should follow that command for the single task it is given.
