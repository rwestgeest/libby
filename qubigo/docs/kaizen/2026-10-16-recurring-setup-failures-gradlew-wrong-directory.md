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

## Investigation

Date: 2026-10-20. Run `qubigo-004a` (`plan.md.friction.md`) shows 11 more recurrences: root-level `./gradlew` or `npm` in tasks 1a, 2a, 2b, 3b, 3d (twice), 4a and 5a, plus a validator each in 1a and 3b. The same entries show guessed paths (`qubigo-frontend/src`, `qubigo-backend/src`, nonexistent test-report directories) in tasks 1d, 2b, 3a, 3b, 3c, 3e, 4a and 5a, and a full `./gradlew test` that exceeded a 200-second tool timeout.

Open question answered: every machine prompt tells the agent to read the target root `AGENTS.md` (doer: "Read and follow the target repository's root `AGENTS.md`"; `.assembly-lines/careful-plan.md`), so that file is always loaded. It named no commands or directories; those lived only in `qubigo-backend/AGENTS.md` and `qubigo-frontend/AGENTS.md`, which agents must find first. Cause confirmed: the instruction was missing from the always-loaded file, not ignored.

## Resolution

Date: 2026-10-20. Status: countermeasure applied; effectiveness not yet demonstrated.

- **Fix (prevention).** Added a "Where Commands Run" section to the root `AGENTS.md`: the wrapper and `npm` run only from `qubigo-backend` / `qubigo-frontend`, where test reports land, that frontend source is `app/` (no `src/`), and a 10-minute timeout floor for the full backend suite. It sits in the file every machine already reads, so no machine prompt changed.
- **Validation.** Docs-only change; no build run. Checked the claims against the repo: wrapper only in `qubigo-backend`, `package.json` only in `qubigo-frontend`, report path from `build.gradle.kts`.
- **Expected result.** Next run's friction log has no root-level `./gradlew`/`npm` entries and no `qubigo-*/src` path guesses. Not yet observed.
- **Changed paths:** `AGENTS.md`.
- **Review point:** check `plan.md.friction.md` after the next factory run; reopen if root-level invocations recur. Guessed file names inside `app/` are ordinary discovery and are not covered.
