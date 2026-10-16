# Problem: agents repeatedly run `./gradlew` from the wrong directory

Date: 2026-10-16

## Context
Iteration 003, tasks 3 and 5. The Gradle wrapper is in `qubigo-backend`, not the repository root.

## Expected standard
Build and test commands in agent instructions name the directory they run from.

## What happened
Three setup failures from the same cause were recorded: task 3 ("An initial invocation from the monorepo root had no Gradle wrapper"), and task 5 twice ("An initial root-level `./gradlew` invocation failed because the wrapper is in `qubigo-backend`"; "initial invocation from the root lacked `gradlew`"). The plan also carries a "Finding" subtask for it (task 5).

## Impact
Small per occurrence (a wasted command and a documented finding each time), but it recurs even though task 1 wrote down "All commands below run from `qubigo-backend`".

## What allowed it to happen
- Each task starts with fresh agent context; the lesson from task 1 is in an evidence file, not in the doer's instructions.
- The doer's `AGENTS.md` is four lines and has no project commands. Product guidance is only in `qubigo-backend/AGENTS.md`, which agents must find and read.

## Observations
Related setup failures in the same iteration: wrong fixture APIs (task 3), wrong test UUID (task 1), duplicate `SqliteBasedGameListView` bean (task 2), missing libatk (task 5). These are ordinary discovery costs, not all preventable.

## Why this matters
It shows that lessons recorded by one task do not reach the next task unless they are in the always-loaded instructions.

## Open questions
Does the doer read the repository root `AGENTS.md`, or only the machine one?

## Possible prevention ideas
Put build and test commands, with directory, in the root `AGENTS.md` or the doer machine's `AGENTS.md`.
