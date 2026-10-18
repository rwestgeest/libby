# Problem: task split put tests after implementation; red evidence needed mutation checks

Date: 2026-10-16

## Context
Iteration 003 tasks 2-5. The seed and backend guidance call for test-first work. The plan states "Future tasks must demonstrate behavioral red tests before code" (investigation file).

## Expected standard
Each task shows a failing behavioral test before the production change that fixes it.

## What happened
- Tasks 2 and 3 followed test-first: task 2's targeted run had 4 of 5 failing before the adapter; task 3 had 6 failures out of 34 before wiring.
- Task 4 (restart integration tests) and task 5 (process and browser acceptance) came after the behavior already existed from task 3, so their tests passed on first run. Both say "this task needed no production implementation change" and fall back on a temporary mutation: swap production wiring to the memory adapter, observe one failure, restore.
- Task 2 needed a local non-autowire-candidate bean in `RefreshTokenDatabaseFixture` so the adapter test could run while production still used memory storage.

## Impact
- The mutation check is honest and effective, but it is manual and leaves a risk of forgetting to restore the change (both tasks state they restored it).
- The first red evidence for the headline behavior (restart persistence) appears in task 2 at repository level, and for end-to-end behavior only through mutation.

## What allowed it to happen
- The plan decomposed work by test layer (adapter, wiring, restart, process, client, final matrix) rather than by behavior slice, so the real implementation landed before the acceptance tests existed.
- Plan reviewers approved this ordering unanimously (all three, no findings), so the review lens does not check test-first sequencing.

## Observations
- The end result is strong: 35 new tests, zero failures, and sensitivity to the original defect was shown twice.
- The validator lens is responsibility-driven design; no machine checks test-first discipline.

## Why this matters
If test-first matters, the line should enforce it structurally; otherwise it depends on the doer volunteering mutation checks.

## Open questions
Would a thinner vertical slice (restart test first, then adapter, then wiring) have cost more or less overall?

## Possible prevention ideas
Ask the planner to order tasks so each behavior's failing test comes before its implementation; give the validator a "red evidence present" check.

## Additional observation and countermeasure — 18 October 2026

In the collaborative qubigo-004 investigation, we identified a related planning
pressure: instructions demanded acceptance checks for happy paths, errors and
edge cases for every task, despite also asking for small increments. This could
encourage future design elaboration; it does not prove why every earlier choice
was made. We agreed to keep a journey/capability outline and detail only the next
releasable capability, with behavior, relevant rules and explicit deferrals.

Implemented on main: initializer/planner/revision now use that distinction.
The doer explicitly test-drives bounded behavior and lets collaborators/ports
emerge; reviewers honor limited scope and inspect actual TDD evidence instead
of treating passing tests as proof. Backend guidance permits outside-in and
inside-out rather than a mandatory layer order. Existing design checkpoints
still handle new scope/architecture decisions.

### Outcome and follow-up

Thread 04 is done as an instruction/process adjustment, by the user's decision.
Effectiveness testing is deliberately assigned to the next iteration-004 attempt,
004a: observe capability scope, failing-test-first work and emerging domain
collaborators. No separate trial is required before moving on. This is not a
claim of observed improvement or that 004a has started. No active experiment or
review date is inferred. Documentation consistency/whitespace checks passed;
no product code changed or tests rerun for these instruction edits.
