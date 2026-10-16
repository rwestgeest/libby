# Problem: factory commits are all named "Record factory task"

Date: 2026-10-16

## Context
`git log` on `feature/003-sessions-survive-backend-deployments` and earlier. The planner machine never commits (its `AGENTS.md` says it only maintains the plan and marks tasks done once told `Task work committed`), so the commits appear to come from the factory itself.

## Expected standard
Commit messages say what changed so history can be read without opening each commit. The repo has a `.gitmessage` template.

## What happened
Of the last 30 commits, 22 start with "Record factory task" (or close variants: "Record factory planning (qubigo-001)", "Record factory accepted task progress (qubigo-001)"). The iteration 003 commits (27303c86, cabccaae, 228adf63, 1e1e7b31, 2444ff7c, f502417c) carry no task number or subject. Their contents range from a characterization test commit to the production wiring change (228adf63 changes `SecurityConfig.kt`).

## Impact
- To build the timeline for these notes it was necessary to read `git show --stat` for each commit.
- Bisecting, blame and release notes get no help from the messages.

## What allowed it to happen
No machine instruction mentions commit messages. The factory commits after the doer and validator accept, apparently with a fixed message. Not verified, as the factory source is not in this repo.

## Observations
Manual commits in the same log ("preparation for iteration 003", "updated machine config for use with iteration 005a") are descriptive.

## Why this matters
History is part of the evidence for every future kaizen review.

## Open questions
Is the message fixed in the factory, and can it be configured per line?

## Possible prevention ideas
Have the factory build the message from the run id and task title, or let the doer propose a one-line summary.
