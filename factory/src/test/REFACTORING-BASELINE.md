# Refactoring coverage baseline

## Before changes

`./test` exited 1: **45 scenarios undefined**, 598 steps (90 passed,
463 skipped, 45 undefined), no failing assertions. Every non-real-agent
scenario in the fetched suite was undefined. Locations by feature:

- `assembly-line.feature`: 39, 43, 50, 58
- `machine.feature`: 29, 38, 47, 54, 66, 73, 88, 93, 98, 109
- `orchestration.feature`: 27, 39, 50, 62, 73, 81, 86, 103, 115, 120,
  132, 138, 151, 159
- `planning.feature`: 28, 38, 44, 61, 74, 80, 87, 99, 111, 118, 126
- `target.feature`: 21, 30, 40
- `validation.feature`: 17, 30, 52

The existing steps/doubles exercise a factory-directory assembly line and
machines, `--seed` / `--target`, and a target-local `.factory/plan.md`.
The fetched scenarios use a newer named-run protocol and compound setup
steps that are not defined. Adding aliases would not safely characterize
current behavior: several requirements are not implemented. No fetched
features or production source were changed.

## Added characterization coverage

`src/test/features/refactoring-characterization.feature` reuses existing
steps, temporary Git fixtures, and Python harness doubles. Missing assertions
and double controls were added for:

- Last parsable JSON line selection (an earlier contradictory result loses;
  a trailing valid JSON array prevents object-result decoding).
- Labeled routing with a custom decision field, rejection/retry, and missing
  result-field refusal.
- Three-attempt exhaustion, finding forwarding, no commits on rejection,
  and an unfinished plan.
- Work committed before the accepted planner invocation, with a clean target
  and unfinished committed task at invocation time; planner completion
  committed separately afterward for each task.
- Tracked and untracked product diffs; `.factory` metadata exclusion;
  containing-repository reuse; target-only commits; preservation of unrelated
  staged, unstaged, and untracked factory files.

Planner call snapshots are test-only observations of Git state before the
planner mutates the plan. They do not change the double's result or behavior.
Snapshots use a separate suffix so they do not affect call numbering.

## Verification after changes

- `mvn test -Dcucumber.tags=@characterization`: exit 0, **7 scenarios and
  97 steps passed**, against unchanged production code. Full output:
  `target/characterization.log`.
- `./test`: exit 1, **7 passed, the same 45 undefined**, 695 steps
  (187 passed, 463 skipped, 45 undefined), no failing assertions. Full output:
  `target/test.log`.
- No real-agent examples were run.

The additional feature directory is included in the normal Maven test
execution, so these checks run alongside the fetched features. The focused
command selects only characterization scenarios when the inherited undefined
steps would otherwise make the overall build red.
