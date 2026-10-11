# Iteration 0041 validation record

## Contract

The factory owns execution settings in `runs/<name>/`; the target owns the
work record in `.factory/runs/<name>/plan.md`. The seed is separate and remains
unchanged. All machines receive that exact plan path. Only the planner decides
completion; orchestration does not parse task checkboxes or acceptance evidence.

Initial planning is committed before implementation. Accepted product work is
committed before planner progress, which is committed before the next task or
completion. Plan commits include only the exact run's plan. Product commits
exclude `.factory` and `.assembly-lines`; unrelated staged/unstaged changes keep
their existing protections. Legacy factory-owned plans and settings lacking the
ownership marker are explicitly unsupported: no automatic migration, progress
replacement, or second authoritative plan is permitted.

## Reproducible checks

From `factory/`:

```sh
./test -Dcucumber.tags='@target-owned-plan or @authoritative-planning or @plan-history or @target-acceptance or @lifecycle or @repository'
./test
```

Task 5 execution results:

- Focused: exit 0, **40 scenarios / 627 steps passed**.
- Full deterministic suite: exit 1, **116 of 117 scenarios passed**;
  **1675 steps passed, 1 skipped, 1 failed**. No undefined scenarios reported.
  Full Maven output is in `target/test.log` (overwritten by each test invocation).
- The sole failure is `spec/features/planning.feature:61`, at its line 64
  assertion requiring a factory-owned `runs/tetris/plan.md`. Its next assertion
  requires no target plan. This fetched rule directly contradicts iteration
  0041; it was not edited, bypassed, or satisfied with a competing plan.
  The skipped step is in this failed example, not an unimplemented scenario.
  Tasks 1–4 recorded the same contract conflict. It is **not** a pre-existing
  baseline failure: the new contract intentionally supersedes this expectation.
  No additional regressions appeared in this validation run.

## Seed acceptance evidence

Paths below are relative to `factory/`. These are deterministic examples with
isolated targets, agent doubles and Git repositories, not live-agent promises.

| Criterion | Recorded evidence |
| --- | --- |
| 1. New target-local plan, distinct from seed, no factory copy | `src/test/features/target-owned-plan.feature`: “All machines share a target-local plan on first invocation and retry” asserts target location, no factory plan and unchanged seed. Hard-link alias rejection also protects seed separation. |
| 2. Shared confined path for planner, doer and validator | The same scenario verifies all recorded machine paths including retry. Filesystem-alias and unsafe-name examples reject escapes and in-target aliases. `src/test/features/authoritative-planning.feature` verifies target-local directory preparation and no fallback. |
| 3. Name-only resumption preserves progress | Both ownership and authoritative-planning features assert unchanged recorded completion and no doer invocation on resumption; failed planning preserves progress for later retry. |
| 4. Same-target run isolation | Ownership scenario “Two runs in one target have independent plans including resumption” checks distinct plans and unchanged other-run content. |
| 5. Initial and accepted task history; rejection is not completion | `src/test/features/plan-history.feature` checks durable initial planning, ordered product/progress commits, committed plan contents, rejection without completed tasks, and failures at all three commit boundaries. `src/test/features/task-lifecycle.feature` exercises multi-task routing and history. |
| 6. Outstanding manual check and owner evidence on resumption | `src/test/features/target-acceptance.feature` first retains outstanding status, including an unperformed-check document, then resumes with target-local owner evidence and unchanged factory settings. Passing owner evidence is synthetic test data only. |
| 7. Missing plan stops implementation | `src/test/features/authoritative-planning.feature` covers successful results without a plan (including completion and alternative paths), process failure, later deletion, name-only retry, and direct-doer routing. No missing-plan case starts product work. |
| 8. Existing protections remain | `src/test/features/plan-history.feature` checks exact plan scoping, other-run/private/configuration staged and unstaged changes, unrelated factory changes, and no pending product commit on planning-only completion. `src/test/features/target-repository.feature` checks containing-repository scoping. Validator guidance explicitly disallows treating status updates as product evidence. |

## Limits and outstanding decisions

The eight ownership criteria have deterministic evidence above. The full suite
is not green: the fetched legacy-path contract remains an explicit unresolved
specification conflict requiring course/spec-owner reconciliation, not a hidden
pass or a reason to create a second plan. The planner retains the overall
acceptance decision and task-completion decision after commits.

No real-agent run was requested or performed. No human or device acceptance
check was performed, including Qubigo's outstanding real-device checks. Such
checks are not required to prove this factory ownership contract and remain
outstanding for their respective target seeds. Target-local evidence makes them
resumable; moving the plan does not satisfy them. A finish route alone is not
acceptance. Owners must record genuine evidence in the target and use a suitable
handoff route while checks remain unavailable.
