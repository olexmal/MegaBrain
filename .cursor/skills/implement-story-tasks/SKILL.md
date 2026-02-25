---
name: implement-story-tasks
description: Implements all tasks from a MegaBrain *-tasks.md file in sequence. For each task (T1, T2, ...) not yet completed, runs the implement-task workflow via a subagent, commits the result, then runs the next task until the whole file is marked as implemented. Use when the user asks to implement a story's tasks file, run all tasks from a task file, implement US-03-02 tasks, or complete every task in a *-tasks.md file.
---

# Implement Story Tasks

## When to Use

Apply this skill when the user wants to:

- Implement **all tasks** from a MegaBrain task file (e.g. `US-03-01-ollama-integration-tasks.md`, `US-03-02-openai-integration-tasks.md`)
- Run tasks T1, T2, … in sequence until the file is fully implemented
- Have each task implemented by a subagent (implement-task), then committed, then the next task run

Do **not** use for implementing a **single** task (use the implement-task command instead).

## What to Do

1. **Resolve the task file path**
   - If the user provides a full path, use it.
   - If the user gives only a filename (e.g. `US-03-02-openai-integration-tasks.md`), look under `features/user-stories/` or the repo root.

2. **Follow the implement-story-tasks command**
   - Execute the workflow defined in [.cursor/commands/implement-story-tasks.md](../../commands/implement-story-tasks.md).
   - In short:
     - Parse the task file for `### Tn:` and `- **Status:**`. Build an ordered list of tasks that are **not** `Completed`.
     - For each such task in order: invoke a **generalPurpose** subagent with the implement-task process and the single task block; instruct the subagent not to commit and to reply `all done :)` when done.
     - After each successful subagent run: commit the changes with a message derived from the task id and description.
     - Continue until every task in the file has `Status: Completed`.

3. **Report**
   - If all tasks were already completed, say so.
   - Otherwise, after each task report progress (e.g. "T1 committed, starting T2") and at the end list the tasks completed in this run.

## Reference

- Per-task process: [.cursor/commands/implement-task.md](../../commands/implement-task.md)
- Orchestration details: [.cursor/commands/implement-story-tasks.md](../../commands/implement-story-tasks.md)
