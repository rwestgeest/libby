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

## Investigation

Date: 2026-10-20. Cause confirmed in the factory source (`/home/rob/dev/libby/factory/src/main/kotlin/TargetRepository.kt`): `recordTaskChanges()` ran `git commit --only -m "Record factory task"`, a hardcoded subject. No machine instruction mentioned commit messages. Run `qubigo-004a` produced about 15 more such commits (`1ef94de2` … `d894cf74`). Open question answered: the message was fixed in the factory and not configurable per line.

## Resolution

Date: 2026-10-20. Status: countermeasure applied; effectiveness not yet demonstrated.

- **Fix (prevention).** The doer proposes the subject in a `commit` field of its JSON result, following `.gitmessage` (`<type>(<scope>): <subject>`). The factory (`TaskLifecycle`) keeps the latest proposal for the current task and passes it to `TargetRepository.recordTaskChanges(subject)`. Without a proposal it falls back to "Record factory task", so existing lines keep working. Chosen over deriving a subject from changed paths because the doer knows what the task delivered and result shapes vary too much to scrape.
- **Changed paths.** Factory repo (libby, commit `8f39e6f`): `factory/src/main/kotlin/{TargetRepository,TaskLifecycle,FactoryRun,Main}.kt`, `factory/spec/features/target.feature` (new rule "A work commit says what the task did", two examples), test double and steps. This repo: `.assembly-lines/.machines/doer/AGENTS.md` (asks for the `commit` field).
- **Validation.** Test-first: the new example failed (every commit still "Record factory task"), then `./test` in `libby/factory` passed, 143 scenarios and 2063 steps. Docs-only on the qubigo side; no build run.
- **Expected result.** Next run's task commits carry descriptive subjects. Not yet observed; depends on the doer actually emitting the field (a doer ignoring it keeps the generic subject).
- **Not addressed.** Planner/plan commits and the final catch-up commit use the generic subject unless a doer proposal is pending. Commits already made keep their messages.
- **Review point.** After the next run, `git log` for `Record factory task`; reopen if the doer omits the field.
