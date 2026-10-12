# factory-0042 — Inspection and baseline evidence

## Current verification: authorized local test evolution (2026-10-12)

The product owner clarified that the doer should change tests locally when an explicitly requested behavior supersedes their old expectations, using TDD. This overrides the external course/spec-owner restriction recorded in the historical sections below. The target-owned-plan contract from 0041 supersedes the old factory-owned-plan expectation in `spec/features/planning.feature`. The local feature, exact-path step assertion and accompanying spec prose now reflect that contract; unrelated coverage was retained.

Repository instructions, machine prompts and the 0042 seed now authorize these changes. Planner instructions treat them as work; doer instructions require TDD and a recorded rationale; validator instructions review against the requested behavior and reject weakened coverage. The run plan preserves historical failures but supersedes the obsolete blocker.

TDD evidence for the instruction contract: the new `@test-evolution` scenario failed before the prompt update because the planner lacked the precedence instruction (1 scenario failed). After the update, `./test -Dkotlin.compiler.daemon=false` passed: **118 scenarios, 1690 steps, all passed**, duration 0m 36.359s. No scenarios were excluded beyond the normal `@real-agent` exclusion. The compiler daemon was disabled for this verification after its startup failed; this does not change test selection. The revised planning expectation already matches the 0041 implementation, so no artificial red phase or production path change was necessary for it.

The deterministic scenario verifies the instructions delivered to all three machines, including later calls. It does not demonstrate that a live agent will follow them. No live factory run, manual acceptance or acceptance of the remaining 0042 work is claimed. Runtime test-gate enforcement and separate evidence commits remain work for 0042.

## Task 1: observed existing lifecycle

This is inspection evidence, not acceptance of iteration 0042. No product code, fetched specs, assembly-line configuration, or other runs were changed. No commits were made by the doer. All paths below are relative to the current target (`factory/`).

- `Main.kt` (under `src/main/kotlin/`) opens `RunStore`, loads the target-owned assembly line/configuration, prepares Git, and constructs the runner/jobs/lifecycle. `FactoryRun.kt` routes machine results and prints `factory stopped` on finish; it has no independent acceptance or test check.
- `NamedRun.kt`: authoritative plan is `.factory/runs/<name>/plan.md`; private settings remain in factory-side `runs/<name>/settings.json`. Named resumption reuses target, seed, line, and plan-location settings, refusing conflicting arguments and legacy ownership. Plan aliases and seed collisions are rejected. The factory checks plan existence, not its progress or evidence contents.
- `MachineJobs.kt`: every machine gets the same seed and plan paths and target-only/JSON/commit instructions. Initial planner creates a missing plan or reports existing status unchanged. Only planner receives explicit instructions to read referenced target-local evidence/product-owner acceptance on resumption, retain outstanding manual checks, and avoid inferring acceptance from task completion. Doer implements the first unfinished task, records validator findings as subtasks, and leaves task completion to planner. Validator reviews supplied product diffs only, reports findings without editing, and must not treat plan status as acceptance evidence. No shared instruction currently selects evidence beside the plan.
- `MachineRunner.kt`: launches the configured harness in the target, accepts its last JSON object, and stops on process failure. There is no required-test command/configuration or test invocation in production code. Validator satisfaction is graph routing input, not an independent test gate.
- `TaskLifecycle.kt`: doer starts an attempt; validator findings are retained across retries. Validator-to-doer routing enforces the attempt limit. Preparing a planner after work invokes `recordTaskChanges()` before planner runs, even on validator-free lines. Planner then marks progress; `finishPlanning()` calls `recordPlan()` before further routing. Initial planning also commits the plan. Commit errors terminate execution. Neither boundary checks required tests.
- `TargetRepository.kt`: product diff, status, staging, and commit pathspecs are `.` excluding `.factory` and `.assembly-lines`. Product diffs include untracked nonignored files. Task commits use `Record factory task` (no run identifier). Plan commits use a literal path for the selected run's exact plan and a run-labelled planning/progress message. There is no evidence-selection/staging/commit operation; files beside the plan are excluded from product commits and not included by plan commits. Plan staging uses ordinary `git add`, not ignored-file adoption. All Git operations run in the target, supporting a containing repository.
- Existing deterministic coverage is in `src/test/features/{target-owned-plan,authoritative-planning,plan-history,target-acceptance,task-lifecycle,refactoring-characterization,target-repository,machine-invocation,assembly-line}.feature`. It covers ownership, resumption, routing, retry exhaustion, product/plan commit ordering, commit failures and isolation. `target-acceptance.feature` explicitly uses synthetic owner/manual-check fixtures, not genuine human/device checks. New evidence commits and required-test gate scenarios are not implemented yet.

## Required commands and exclusions

Repository instructions and `README.md` prescribe `./test` from this target as the default required deterministic suite. `test` runs `mvn -B -Dstyle.color=never test`, captures the full log in `target/test.log`, prints Cucumber summaries, and returns Maven's status. `pom.xml` runs both `spec/features` and `src/test/features`, with default tags `not @real-agent`, JVM target 21. `mvn test` is the raw-output counterpart. `./test -Preal-agent` explicitly runs real-agent examples only; it was not run here and is not a substitute for the default suite.

Environment observed: Temurin Java 25.0.4, Maven 3.10.0. Compilation and scenario execution succeeded on this environment before the assertion failure below; the configured JVM target remains 21.

`git check-ignore -v .factory/runs/factory-0042/validation.md target/test.log` reported only `factory/.gitignore:2:/target/` for `target/test.log`. The selected plan/evidence paths are not currently ignored by this repository. The general `.factory` exclusion relevant to the seed is the production Git pathspec, independent of `.gitignore`. Future staging must account for both. No exclusion or configuration was changed.

## Baseline: required suite blocked

Command: `./test`, before any product edits. Exit **1**.

Selected output:

```text
Failed scenarios:
  1) The first invocation of a run named "tetris" # file:///home/rob/dev/libby/factory/spec/features/planning.feature:61
       java.lang.IllegalStateException: Check failed.
        at steps.FactorySteps.planInRunsFolder(FactorySteps.kt:1177)

117 scenarios (116 passed, 1 failed)
1677 steps (1675 passed, 1 skipped, 1 failed)
0m 35.200s
[ERROR] BUILD FAILURE
```

`spec/features/planning.feature:56–65` requires a factory-owned `runs/tetris/plan.md` and no target plan. `FactorySteps.kt:1177` asserts that factory-side file exists. This conflicts with the existing iteration-0041 target-owned plan contract. `docs/target-owned-plan-validation.md` already records the identical failure from 0041. Thus it predates this 0042 attempt; the historical record correctly describes its origin as a deliberate 0041 contract change rather than an unrelated failure predating 0041.

**Blocker:** all required tests do not pass. Baseline documentation grants no waiver: accepted-task implementation, evidence and completion-status commits, advancement and completion must remain blocked. Do not edit fetched `spec/`, weaken/filter required tests, create a competing plan, or expand 0042 to silently reconcile the course specification. Course/spec-owner reconciliation is outstanding. Findings from the previous validator attempt were `[]`; there are none to fix or mark resolved. The baseline failure remains unresolved, not an accepted finding fix.

## Task 1 resumption verification

Reread the seed, plan, and this referenced evidence. Rechecked `TaskLifecycle.kt`, `TargetRepository.kt`, the fetched planning scenario, and its step assertion: the inspection above remains accurate. No validator findings were supplied (`[]`), so no finding subtasks were added or resolved.

Reran the unmodified required command `./test`: exit **1**, 117 scenarios (116 passed, 1 failed), 1677 steps (1675 passed, 1 skipped, 1 failed), duration 0m 35.583s. The same `spec/features/planning.feature:61` scenario fails at `FactorySteps.kt:1177`. Course/spec-owner reconciliation is still required; no fetched specs, tests, or production code were changed.

At resumption, `git status --short` showed only this evidence file untracked. `git log -5 --oneline` now includes `5e618a3 Record factory accepted task progress (factory-0042)` above initial planning `5a1ef40`. That history observation supersedes the planner's earlier history snapshot, but does not establish acceptance or a passing required-test gate. This doer made no commits and leaves task 1 unchecked, with its blocker unresolved. Only this run's plan and selected evidence are updated by this attempt.

## Latest task 1 baseline recheck

Reread the seed, authoritative plan, and referenced evidence. Rechecked the fetched scenario and step assertion, target-owned plan location, product exclusions, and lifecycle commit calls. The unresolved contract conflict remains unchanged. Previous validator findings are `[]`; no finding subtasks need recording or resolution.

Executed unmodified `./test` again: exit **1**, 117 scenarios (116 passed, 1 failed), 1677 steps (1675 passed, 1 skipped, 1 failed), duration 0m 35.630s. The failure remains `spec/features/planning.feature:61`, `FactorySteps.kt:1177`. No test filtering, fetched-spec edits, or product implementation was performed. Course/spec-owner reconciliation remains required.

Current history includes `4edc21c Record factory accepted task progress (factory-0042)`, `5e618a3`, and initial planning `5a1ef40`. These observed progress commits do not override the failing required-test gate. Before this attempt's evidence updates, status showed only this evidence file untracked. This attempt updates only this run's plan and evidence, makes no commits, leaves task 1 unchecked, and does not advance or claim acceptance/completion.

## Current task 1 recheck

Rechecked the lifecycle commit boundaries, product pathspec exclusions, fetched planning rule and assertion. No change to the documented conflict: the fetched rule requires the factory-owned plan, while the existing target-owned contract uses `.factory/runs/<name>/plan.md`.

Unmodified `./test` exited **1**: 117 scenarios (116 passed, 1 failed), 1677 steps (1675 passed, 1 skipped, 1 failed), 0m 35.352s. The failing scenario remains `spec/features/planning.feature:61`, with `FactorySteps.kt:1177` asserting the factory-owned path. Required-test acceptance remains blocked; course/spec-owner reconciliation is needed. No fetched specs, production code, or tests were edited.

Before this attempt, `git status --short` showed only this evidence file untracked; latest observed commit was `a59cd04 Record factory accepted task progress (factory-0042)`. That progress commit supplies neither passing-test evidence nor a waiver. Validator findings were `[]`; there are no finding subtasks to add or resolve. This attempt changes only the selected run's plan/evidence, makes no commits, leaves task 1 unchecked, and does not advance or claim completion.

## Latest doer verification

Rechecked lifecycle commit calls, product exclusions, and the factory-owned planning assertion. Ran the unchanged required suite `./test`: exit **1**, 117 scenarios (116 passed, 1 failed), 1677 steps (1675 passed, 1 skipped, 1 failed), 0m 35.274s. The same fetched scenario at `spec/features/planning.feature:61` fails at `FactorySteps.kt:1177`. Course/spec-owner reconciliation remains necessary; no scope expansion, spec edits, or gate waiver is authorized.

Observed latest commit: `614aea1 Record factory accepted task progress (factory-0042)`. It does not establish passing tests. Validator findings were `[]`; no finding subtasks were required. Only this run's plan and evidence were updated; no product changes or commits were made. Task 1 remains unchecked, with no advancement or acceptance.

## Recheck following planner verification

Reread the seed, plan, and referenced evidence; inspected `TaskLifecycle.kt`, `TargetRepository.kt`, the fetched planning rule, and `FactorySteps.kt:1177`. The lifecycle still commits without a required-test gate, product staging still excludes `.factory`, and the fetched assertion still requires the conflicting factory-owned plan.

Ran unmodified `./test`: exit **1**, 117 scenarios (116 passed, 1 failed), 1677 steps (1675 passed, 1 skipped, 1 failed), 0m 36.264s. The same scenario at `spec/features/planning.feature:61` fails at `FactorySteps.kt:1177`. No authorized reconciliation or passing baseline is available. Latest observed commit was `6b11894 Record factory accepted task progress (factory-0042)`; before this attempt only this evidence file was untracked. This history does not establish acceptance.

Validator findings were `[]`; no finding subtasks were added. Task 1 inspection remains performed but blocked, with its existing baseline-blocker subtask unresolved. Only this run's plan/evidence were changed; no product/spec changes, commits, advancement, or manual acceptance occurred. Course/spec-owner reconciliation remains required before accepted-task commits.

## Doer verification after `15b1c19`

Reread the seed, plan, and referenced evidence; rechecked `TaskLifecycle.kt`, `TargetRepository.kt`, and `FactorySteps.kt:1177`. Existing inspection remains accurate: accepted-work boundaries have no required-test gate, product staging excludes `.factory`, and the fetched assertion still requires a factory-owned plan conflicting with 0041.

Ran unmodified `./test`: exit **1**, 117 scenarios (116 passed, 1 failed), 1677 steps (1675 passed, 1 skipped, 1 failed), 0m 35.257s. The same `spec/features/planning.feature:61` scenario fails at `FactorySteps.kt:1177`. The baseline blocker remains unresolved and requires course/spec-owner reconciliation; no fetched specs or product code were changed.

Latest observed commit was `15b1c19 Record factory accepted task progress (factory-0042)`; initial status showed only this evidence file untracked. Validator findings were `[]`, so no finding subtasks were needed. This attempt updates only this run's plan/evidence, makes no commits, leaves task 1 unchecked, and claims no advancement or acceptance. Repeated progress commits do not waive the failing required-test gate.

## Recheck after `e8d947a`

Reread the seed, plan, and all existing referenced evidence. Rechecked `TaskLifecycle.kt`, `TargetRepository.kt`, `spec/features/planning.feature:56–65`, and `FactorySteps.kt:1177`: the documented lifecycle boundaries, exclusions, and plan-location conflict are unchanged.

Unmodified `./test` exited **1**: 117 scenarios (116 passed, 1 failed), 1677 steps (1675 passed, 1 skipped, 1 failed), 0m 35.164s. The same fetched planning scenario fails at `FactorySteps.kt:1177`. The existing baseline-blocker subtask remains unresolved; course/spec-owner reconciliation is required. No fetched specs, tests, or product code were changed.

Initial Git status showed only this evidence file untracked; latest observed commit was `e8d947a Record factory accepted task progress (factory-0042)`. No passing-test evidence or gate waiver is established by that commit. Validator findings were `[]`, so no finding subtasks were added or resolved. Only this run's plan/evidence were updated; no commits, advancement, or acceptance performed. Task 1 remains unchecked.

## Recheck after `cb6043e`

Reread the seed, plan, and referenced evidence; inspected `TaskLifecycle.kt`, `TargetRepository.kt`, the fetched planning rule, and its step assertion. The existing inspection remains accurate: commit boundaries lack a required-test gate, product staging excludes `.factory`, and the fetched rule requires the conflicting factory-owned plan.

Unmodified `./test` exited **1**: 117 scenarios (116 passed, 1 failed), 1677 steps (1675 passed, 1 skipped, 1 failed), 0m 35.413s. The failure remains `spec/features/planning.feature:61`, `FactorySteps.kt:1177`. Course/spec-owner reconciliation is still required; the existing blocker subtask remains unresolved. No fetched specs, tests, or product code were changed.

Initial status showed only this evidence file untracked; latest observed commit was `cb6043e Record factory accepted task progress (factory-0042)`. That commit does not establish passing tests or acceptance. Validator findings were `[]`; there are no finding subtasks to record or resolve. Only this run's plan/evidence were updated; no commits, advancement, or acceptance performed. Task 1 remains unchecked.

## Recheck after `5b9433e`

Reread the seed, plan, and referenced evidence; verified `TaskLifecycle.kt`, `TargetRepository.kt`, the fetched planning rule, and `FactorySteps.kt:1177`. The documented inspection is unchanged: lifecycle commits lack a required-test gate, product pathspecs exclude `.factory`, and the fetched assertion requires the conflicting factory-owned plan.

Ran unmodified `./test`: exit **1**, 117 scenarios (116 passed, 1 failed), 1677 steps (1675 passed, 1 skipped, 1 failed), 0m 36.165s. The same `spec/features/planning.feature:61` scenario fails at `FactorySteps.kt:1177`. The existing baseline-blocker subtask remains unresolved pending course/spec-owner reconciliation; no specs, tests, or product code were changed.

Initial status showed only this evidence file untracked; latest observed commit was `5b9433e Record factory accepted task progress (factory-0042)`. Validator findings were `[]`; no finding subtasks were needed. Only this run's plan/evidence are updated; no commits, advancement, acceptance, or manual checks were performed. Task 1 remains unchecked. Repeated baseline verification cannot resolve the external contract conflict or authorize accepted-task commits.

## Blocked resumption after `cb3b313`

Read the seed, plan, and all existing referenced evidence. Rechecked `TaskLifecycle.kt`, `TargetRepository.kt`, and the fetched planning assertion: the documented commit boundaries, exclusions, and contract conflict remain unchanged. Initial status showed only this evidence file untracked; latest observed commit was `cb3b313`. No reconciliation or passing required-test evidence was supplied.

Did not repeat the unchanged baseline: the latest recorded result remains exit **1**, 116/117 scenarios passed. Repeating it cannot resolve the course/spec-owner blocker. Task 1 inspection is already performed; its blocker remains unresolved and its checkbox stays unchecked. Validator findings were `[]`, so no finding subtasks were added. No product/spec changes, tests, commits, advancement, or acceptance performed. Only this run's inspection record was updated.

## Blocked resumption after `fb99dfe`

Reread seed, plan, and referenced evidence. Inspected current `TaskLifecycle.kt`, `TargetRepository.kt`, and `spec/features/planning.feature:56–65`; the commit boundaries, exclusions, and conflicting fetched assertion remain unchanged. Initial Git status showed only this evidence file untracked; latest commit was `fb99dfe`. No reconciliation or passing required-test result was supplied.

Task 1 inspection is already performed. Its existing baseline-blocker subtask cannot be resolved within this task's scope: course/spec-owner reconciliation is required. Did not repeat the identical baseline or modify fetched specs. Latest recorded required suite remains failed (116/117 scenarios passed). Validator findings were `[]`; no finding subtasks to add or resolve. No product changes, commits, advancement, or acceptance performed; task 1 remains unchecked. Updated only this run's local inspection evidence.

## Blocked resumption at `6cdbdd2`

Reread the seed, plan, and referenced evidence; inspected `TaskLifecycle.kt`, `TargetRepository.kt`, `spec/features/planning.feature:56–65`, and `FactorySteps.kt:1176–1178`. The existing inspection and external plan-location conflict remain unchanged. Git status shows only this evidence file untracked. No course/spec-owner reconciliation or passing required-test result has been supplied.

Task 1 inspection is already performed; its baseline blocker remains unresolved. No identical test rerun, fetched-spec changes, product changes, commits, advancement, or acceptance performed. Latest recorded required suite remains failed (116/117 scenarios passed). Validator findings are `[]`, so there are no finding subtasks to add or resolve. Task 1 remains unchecked; only this local evidence document was updated.

## Blocked resumption at `e0151da`

Reread seed, plan, and referenced evidence; inspected `TaskLifecycle.kt`, `TargetRepository.kt`, the fetched planning rule, and its step assertion. The documented lifecycle boundaries, exclusions, and external contract conflict remain unchanged. Git status shows only this evidence file untracked; latest commit is `e0151da`.

Task 1 inspection is already performed. No reconciliation or passing required-test evidence was supplied; the existing baseline-blocker subtask remains unresolved. Did not rerun the identical baseline, change product/spec files, make commits, or advance. Validator findings were `[]`, so no finding subtasks require recording or resolution. Task 1 remains unchecked. Only this local evidence file was updated; further work requires course/spec-owner reconciliation.

## Blocked resumption at `a7c0583`

Reread seed, plan, and referenced evidence. Rechecked lifecycle commit calls, product exclusions, and `spec/features/planning.feature:61–65`: the documented conflict remains unchanged. Git status shows only this evidence file untracked. No course/spec-owner reconciliation or passing required-test result has been supplied. Task 1 inspection is already performed; its existing blocker remains unresolved. No identical baseline rerun, product/spec changes, commits, advancement, or acceptance performed. Validator findings were `[]`; no finding subtasks were needed. Task 1 remains unchecked. Only this run-local evidence was updated.

## Blocked resumption at `fc637d4`

Read the seed, plan, and all existing referenced evidence. Rechecked lifecycle commit calls, product exclusions, and the fetched step assertion (`FactorySteps.kt:1177`); the recorded conflict persists. Initial status shows only this evidence file untracked. Task 1 inspection is already performed, but its existing blocker requires course/spec-owner reconciliation. No passing required-test evidence or reconciliation was supplied. No identical baseline rerun, product/spec changes, commits, advancement, or acceptance performed. Validator findings were `[]`; no finding subtasks were required. Task 1 remains unchecked. Updated only this run-local evidence; latest recorded required suite remains failed (116/117 scenarios passed).

## Blocked resumption at `5aa9b07`

Reread the seed, plan, and referenced evidence. Inspected `TaskLifecycle.kt`, `TargetRepository.kt`, and `FactorySteps.kt:1176–1178`; the recorded commit boundaries, exclusions, and conflicting fetched assertion remain unchanged. Git status shows only this evidence file untracked. Task 1 inspection is already performed; its existing blocker requires course/spec-owner reconciliation, which has not been supplied. Latest recorded required tests remain failed (exit 1, 116/117 scenarios passed).

Validator findings were `[]`; no finding subtasks require recording or resolution. No identical baseline rerun, product/spec changes, commits, advancement, or acceptance performed. Task 1 and its blocker remain unchecked. Updated only this run-local evidence. Further identical attempts cannot resolve the external blocker; do not advance or treat this inspection as accepted work.

## Blocked resumption at `2063ebf`

Reread the seed, plan, and referenced evidence. Git status shows only this evidence file untracked. Verified that lifecycle commit calls and product exclusions remain unchanged and that the fetched planning rule and `FactorySteps.kt:1177` still require the conflicting factory-owned plan. Task 1 inspection is already performed; course/spec-owner reconciliation remains necessary to resolve its existing blocker. No passing required-test result or authorized reconciliation was supplied.

Validator findings were `[]`; no finding subtasks require recording or resolution. No identical baseline rerun, product/spec changes, commits, advancement, or acceptance performed. Latest recorded required tests remain failed (exit 1, 116/117 scenarios passed). Task 1 remains unchecked. Only this run-local evidence was updated; the factory must not treat blocked inspection as accepted work.

## Blocked resumption at `0b28758`

Read the seed, plan, and referenced evidence. Current status shows only this evidence file untracked; `acceptance.md` is absent. Rechecked lifecycle commit calls, product exclusions, and the fetched planning rule/assertion: the existing external contract conflict persists. Task 1 inspection is already performed, but its baseline-blocker subtask requires course/spec-owner reconciliation. Latest recorded required tests remain failed (exit 1, 116/117 scenarios passed); no reconciliation or passing evidence was supplied.

Validator findings were `[]`; no finding subtasks were needed. Did not repeat the identical baseline, modify product/spec files, commit, advance, or mark task 1 done. Updated only this run-local evidence. Blocked inspection must not be treated as accepted work.

## Limits

Only inspection and the full deterministic baseline were performed. No new 0042 acceptance scenarios, actual run-record commit verification, live agents, product-owner feedback, human or device acceptance were performed. No implementation/evidence/progress commit IDs or ordering are claimed. Other acceptance criteria and Draft-scope questions remain outstanding. Full transient output is in `target/test.log`; the selected failure above is retained beside the authoritative plan for resumption.
