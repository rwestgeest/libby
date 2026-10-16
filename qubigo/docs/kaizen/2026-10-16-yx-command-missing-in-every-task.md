# Problem: `yx list --ready` is required but unavailable in every task

Date: 2026-10-16

## Context
Iteration 003, all tasks and the product-owner interview. The instruction lives in `qubigo-backend/AGENTS.md`. `yx` is not installed on the host (`which yx` finds nothing).

## Expected standard
The task discovery command named in the agent instructions runs, or the instruction is removed or marked optional.

## What happened
Each iteration 003 evidence file records an attempt and the failure: the seed ("`yx: command not found`"), the investigation, tasks 2, 3, 4 and 6, and task 5 three times (one record states exit 127). Seven iteration documents contain the string `yx list`.

## Impact
Each task spends a call and a paragraph of evidence rediscovering the same failure. The repeated statements ("no task state was inferred") add noise to the evidence documents. No functional impact was observed.

## What allowed it to happen
- The requirement is in a product-side `AGENTS.md`, not in the machine configs (`.assembly-lines/.machines/*/AGENTS.md` do not mention `yx`), so the line does not install or check for it.
- Nothing verifies tool prerequisites before a run starts.
- Agents are told to report the failure but not told it is a known, accepted condition.

## Observations
- `.gitignore` already lists `.yaks`, so the tool was in use at some point.
- Agents handled the absence conservatively and did not invent task state.

## Why this matters
It is cheap noise today. It also trains agents and readers to ignore failing prerequisite checks, which hides real ones.

## Open questions
Is `yx` meant to be part of this workflow at all now that the factory plan is the task list?

## Possible prevention ideas
Remove or condition the `AGENTS.md` instruction; or add a preflight step to the line that checks required tools once and records the result.
