# Implement Story Tasks (All Tasks in Sequence)

Orchestrates implementation of all tasks from a MegaBrain `*-tasks.md` file in order. For each task T1, T2, … that is not yet completed, runs the implement-task workflow via a subagent, commits the result, then runs the next task until the whole file is marked as implemented.

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
   - **Invoke a subagent** to implement this single task:
     - Use `mcp_task` with `subagent_type`: `generalPurpose`.
     - **Prompt** (provide full context; subagents have no prior context):
       - Instruct the subagent to implement **only** this task (the given task id and task block).
       - Tell the subagent to follow the **implement-task** process: read and follow the instructions in `.cursor/commands/implement-task.md` for the single task (task analysis, implementation planning, code, unit tests, `mvn clean install`, documentation and completion updates to the **tasks file** and the **related user story** file).
       - Give the **task file path** and the **exact task block** (copy the markdown for this task only).
       - Tell the subagent: "When the task is fully implemented and verified (`mvn clean install` succeeded, task status and acceptance criteria updated in the tasks file and related user story), do **not** commit. Reply with exactly: `all done :)` and stop. The orchestrator will commit after you finish."
     - Do **not** run the next task until this subagent completes.
   - **After the subagent returns**
     - If the subagent's response indicates success (e.g. contains "all done :)") and the implementation is done:
       - Run: `git add .` and `git status` to see changes.
       - Commit with a message following the implement-task Git workflow, e.g.:
         `git commit -m "feat: implement <taskId>: <short description>\n\n- Key changes\n- Resolves <taskId>: <description>"`
       - Then proceed to the **next** task in the list (re-parse the task file if you want to skip any task that was marked Completed by the subagent; otherwise just advance to the next in your list).
     - If the subagent failed or did not respond with "all done :)", report the failure and ask the user whether to continue to the next task or stop.
   - After committing, **re-read the task file** to get the updated list of non-completed tasks for the next iteration (in case statuses changed). Or maintain the list and remove the current task; either way, process only non-completed tasks in order.

3. **When no tasks remain**
   - When every task in the file has `**Status:** Completed`, respond that the whole story tasks file has been implemented and list the tasks that were completed in this run.

## Task File and User Story Conventions

- **Tasks file:** e.g. `US-03-02-openai-integration-tasks.md` – contains `### T1:`, `### T2:`, etc., each with `- **Status:** ...`.
- **Related user story:** Same directory, same base name with `-tasks.md` replaced by `.md` (e.g. `US-03-02-openai-integration.md`). The subagent must update both: task status and checkboxes in the tasks file, and Technical Tasks / Acceptance Criteria in the user story file.

## Summary

| Step | Action |
|------|--------|
| 1 | Parse task file → ordered list of non-completed tasks (T1, T2, …). |
| 2 | If list empty → "All tasks already completed." → stop. |
| 3 | For each task in list: run subagent (implement-task for that task only; no commit) → on "all done :)", commit → next task. |
| 4 | When all tasks Completed → report done. |

Reference: [implement-task](.cursor/commands/implement-task.md) for the per-task process and subagent usage (explore, test-runner, verifier, etc.). The subagent should follow that command for the single task it is given.
