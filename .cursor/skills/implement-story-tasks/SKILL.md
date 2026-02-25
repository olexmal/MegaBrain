---
name: implement-story-tasks
description: Implements all tasks from a MegaBrain *-tasks.md file in sequence. For each task (T1, T2, ...) not yet completed, runs the implement-task workflow via a subagent (without full mvn clean install per task), commits the result, then the next task. When all tasks are done, runs the full test suite once via a test-runner subagent to fix failures. Use when the user asks to implement a story's tasks file, run all tasks from a task file, implement US-03-02 tasks, or complete every task in a *-tasks.md file.
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
     - For each such task in order: invoke a **generalPurpose** subagent with the implement-task process and the single task block; tell the subagent to **skip full mvn clean install** (use mvn compile / targeted tests only) and to reply `all done :)` when done. After each run: commit.
     - When every task has `Status: Completed`: invoke a **test-runner** subagent to run `mvn clean install` and fix any failures; optionally commit, then report.

3. **Report**
   - If all tasks were already completed, say so.
   - Otherwise, after each task report progress (e.g. "T1 committed, starting T2"), then after the final test-runner run report whether the build passed and list the tasks completed in this run.

## Reference

- Per-task process: [.cursor/commands/implement-task.md](../../commands/implement-task.md)
- Orchestration details: [.cursor/commands/implement-story-tasks.md](../../commands/implement-story-tasks.md)
