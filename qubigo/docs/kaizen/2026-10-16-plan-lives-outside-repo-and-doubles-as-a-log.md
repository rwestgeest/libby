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

## Investigation — 18 October 2026: task 7 design and repair amplification

Run `qubigo-004` is stopped, tasks 1–6 checked and task 7 open. The objective
was event competitions on the `careful-plan` line. This assessment extends the
plan/history concern; it does not establish that file size causes bad design.
No product, workflow or run artifacts changed, and no tests ran.

### Evidence and expected condition

Local artifacts require the Libby checkout:
[plan](/home/rob/dev/libby/factory/runs/qubigo-004/plan.md),
[record](/home/rob/dev/libby/factory/runs/qubigo-004/record.jsonl),
[design review](/home/rob/dev/libby/docs/runs/qubigo-004/design-review.md).
The current plan is 98,647 bytes. Lines 22–24 require observable increments,
test-first work and real acceptance checks; task 7 starts at line 112 and the
review-history section starts at 221. Tasks 8–18 remain unfinished.

The [requirements](../iterations/004-competitions.md#results-and-points) require
full-board completion, one frozen competition result, authoritative ordering
at closure and ordinary play continuing afterward. These are genuine behavior
constraints; SQLite locking, marker columns, collaborator names and a separate
result transaction are implementation choices, not product-owner requirements.

### Five whys: why did repair accumulate machinery?

1. **Why did task 7 need repeated design repairs?** Its collaborations disagreed
   about transaction ownership, clock sampling and finalization. Record lines
   1534, 1573 and 1604 identify production wiring missing the competition path,
   event/projection writes split across transactions, time sampled before the
   write boundary, and recovery without a caller. These are specific integration
   failures, not merely a preference for fewer classes.
2. **Why did repairs expand the plan?** Revision outputs resolve each review
   finding by adding prescriptions to the shared plan. Record lines 227–234
   choose separate event/result transactions; 268–278 repair the notification
   contract this creates; 593–608 add durable cutoff-observed state for clock
   regression; 884–902 add recovery for full games without results. The plan's
   rounds and task-7 criteria retain these decisions and their history.
3. **Why do those choices generate further obligations?** Separate commits allow
   a move to survive result failure, requiring retry/reconciliation. Allowing
   ordinary play past closure requires a durable boundary for the history that
   counts. A revision-prefix marker alone cannot withstand advancing after
   an excluded move, so the chosen protocol needs permanent freezing. These
   dependencies explain some complexity; they do not prove this representation
   or transaction split is the simplest adequate design.
4. **Why did agreement on the plan not settle implementation?** The line's
   [graph](../../.assembly-lines/careful-plan.dot) approves a plan before the
   doer implements it; approval cannot execute or validate the intended wiring.
   The final unanimous reports precede implementation reviews that still reject
   task 7. Plan feasibility and implementation evidence are different gates.

Stop before attributing this to agent intelligence or reviewer count. The record
supports local repair amplification and an unresolved integration boundary;
it does not demonstrate that review caused every extra layer, or that a rewrite
would reduce cost. Earlier provider interruptions are separate events.

### Escape path and the test boundary

The validator repeatedly catches the omissions, so this is not unchecked final
acceptance. The defect escapes the doer's own evidence: record 1744 rejects a
full-game test that calls the protocol; 1778 rejects supposedly wired tests that
still omit the decorator/finalizer/notifications. Passing suites measure the
fixture exercised, not the omitted production collaboration. See the
[test-structure follow-up](2026-10-16-session-persistence-tests-hard-to-read-and-badly-structured.md#investigation--18-october-2026-production-collaboration-still-missing).

### Challenge assumptions, options and recommendation

We need frozen results and reliable committed moves. We have not established
that we need two persistence routes, separate result commits, or a plan carrying
SQL recipes plus its revision log. Removing historical narrative from active
instructions may help navigation but would not fix transaction correctness.
Likewise fewer classes alone would not prove simpler ownership.

Recommend the smallest diagnostic step before selecting a rewrite: describe
who owns admission, move persistence and completion; then demonstrate one legal
full game through the actual production composition, asserting one durable
result. Inventory which requirements force each durable fact and transaction.
Compare retaining the present split with composing completion into the move
boundary; for either option account explicitly for closure, retry, notifications
and ordinary play. Do not remove the cutoff safeguard to simplify a diagram.

Containment: require concrete evidence at that boundary before more helper-level
repairs. Prevention candidate: guide the next increment by one observable
collaboration, choosing adapter mechanics only as that example needs them.
A fresh branch could make comparison easier but introduces rewrite cost; first
try the example against preserved work. Retain requirements, accepted increments,
journal knowledge and useful tests until evidence says otherwise.

Validate with a wiring-sensitive example that fails if finalization is removed,
then explicit rollback, concurrent-final-move and restart variations. Inspect
readability with the student before widening scope. Unknowns: whether a single
transaction is feasible within existing Spring/SQLite ownership; which accepted
increments exist on main; and whether simplifying current work is cheaper than
rebuilding. These need inspection/experiments, not assumptions. No redesign,
experiment or run 004a is authorized or launched by this assessment.
