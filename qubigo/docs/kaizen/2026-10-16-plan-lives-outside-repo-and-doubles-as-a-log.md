# Problem: the run plan lives outside the repo and doubles as a log with stale checkboxes

Date: 2026-10-16

## Context
Iteration 003 plan: `/home/rob/dev/libby/factory/runs/qubigo-003/plan.md` (167 lines, about 45 KB). Committed evidence refers to it by that absolute path. In-repo `.factory/runs/` holds only `qubigo-001` and `qubigo-002`.

## Expected standard
Evidence committed to the repo can be followed from the repo, and a checked box means the item is done.

## What happened
- Task evidence files point at `/home/rob/dev/libby/factory/runs/qubigo-003/plan.md` (for example the task 6 handoff), which is not in this repository.
- The plan's status paragraph is one very long paragraph that accretes: "Historical pending statements below are superseded by this completion record".
- Three validator-finding items under task 5 are marked `[x]` while their own text says "Blocked ... Keep this finding and task 5 open". The later items 5a-5c are what actually closed the work.
- Findings from the doer are recorded as subtasks of the task, as the doer's `AGENTS.md` instructs, so the plan holds task definitions, findings and history together.

## Impact
- A reader cannot tell the state of task 5 from the checkboxes; they have to read the prose.
- Reviewing or comparing iterations requires access to a path outside this repository.
- Agents must read 45 KB of mixed history to find the first unfinished task.

## What allowed it to happen
- The factory writes runs under its own checkout; only some runs were copied into `.factory/runs/` (001 and 002, likely by an earlier "Record factory planning" commit).
- The doer is told to mark resolved findings done, with no distinction between "resolved" and "recorded".
- Nothing keeps the plan's prose short or separates a log from the task list.

## Observations
- `.factory/runs/qubigo-00{1,2}/plan.md` are 133 and 83 lines, so 003 is the largest by a wide margin (167 lines with very long lines).
- The target `/home/rob/dev/libby/q` is the same checkout as `/home/rob/dev/qubigo` (the git top-level resolves to the latter), so two paths name one repository.

## Why this matters
The plan is the source of truth for the line, and it is the main artifact for learning from a run. If it is outside the repo and hard to read, kaizen reviews lose their best evidence.

## Open questions
Does the factory copy runs into `.factory/runs/` automatically, and why was 003 not copied?

## Possible prevention ideas
Copy the final plan into `.factory/runs/` at the end of the run; separate "findings log" from "task list"; use a distinct marker for findings recorded but unresolved.
