# From factory to eternity

## Observation: activity without advancement

The `factory-0042` run repeatedly inspected the same blocker, reran the same failing test suite, and appended progress notes. Ten consecutive commits from `5e618a3` to `ee06d9f` were titled “Record factory accepted task progress”, but changed only the plan. Task 1 remained unchecked; no product implementation was accepted in the plan.

The recorded baseline was 116 of 117 scenarios passing. The failing fetched scenario required a factory-owned plan (`factory/spec/features/planning.feature:56–65`), while iteration 0041 had introduced a target-owned plan. Repeating the suite could confirm the disagreement, but could not resolve it.

The cycle was:

```text
doer records blocker and failing tests
  → validator reports no findings
  → factory treats work as accepted
  → planner preserves the blocker and reports incomplete
  → doer repeats the same task
```

The validator reviewed changed product code through an object-design lens. With no product changes, an empty findings list did not establish that the required tests passed. The lifecycle nevertheless treated the route back to the planner as acceptance. Its attempt limit covered validator-to-doer retries, while visiting the planner reset the count. This cycle therefore escaped the retry limit.

## Learning: a test gate must leave room for TDD

The student's question exposed an incomplete diagnosis: why ask for external reconciliation when a developer doing TDD would change an existing test to express the new requirement, observe red, and implement the desired behavior?

That is a normal development sequence. A failing test during implementation is expected. A requirement that all tests pass before **accepting and committing accepted work** must not become a prohibition on changing tests or entering the red phase.

Here the obstacle was conflicting authority. The older executable spec required one behavior; the newer iteration required another. Repository instructions prohibited editing fetched `factory/spec/`, and the 0042 seed explicitly made pre-existing failures block acceptance without a baseline waiver. The run's plan treated reconciliation as outside scope. The doer followed those restrictions, but the factory kept requesting another attempt.

The initial advice to change the course repository was too restrictive for the student's intent. On 2026-10-12 the product owner clarified the desired rule: the doer is allowed and expected to update existing tests locally when an explicitly requested behavior supersedes their old expectations. That includes fetched specs. No tutorial fork or upstream approval is needed for this project's authorized change.

The updated repository instructions, machine prompts, 0042 seed and current run plan express that rule. The doer must record why each expectation changed and preserve coverage of requirements that still apply. The planner must treat the necessary test changes as part of the work; the validator must review them against the requested behavior. If the implementation already satisfies the corrected test, record that result without manufacturing red. Required tests still have to pass before acceptance.

## Questions for the factory contract

- When a new iteration supersedes an older requirement, how is that precedence expressed so the doer can distinguish an obsolete expectation from a regression?
- How should later course fetches preserve or expose conflicts with locally authorized spec changes rather than silently replacing them?
- How do we explicitly permit the TDD sequence—change the expectation, observe red, implement, reach green—while still enforcing passing tests at acceptance?
- How does a machine report “blocked pending a decision” so the factory stops and exposes the decision, rather than routing another identical attempt?
- What constitutes task advancement, and what bounds repeated cycles that produce notes or commits without advancing a task?
- What evidence must support acceptance? An empty findings list, a route through the graph, and the existence of a commit each proved insufficient here.

## Direction for resolution

Update the superseded planning expectation here to reflect the target-owned behavior requested in 0041. Preserve the distinction between tests failing during development and tests failing at acceptance. A meaningful test change follows a changed requirement; removing coverage merely to get green does not.

Give the factory an explicit blocked outcome and a bound on repeated cycles without advancement. Enforce the required-test gate before accepted-task commits, and avoid labeling blocked observations as accepted progress. Keep selected evidence with the run record so resumption can use the blocker already discovered instead of repeatedly rediscovering it.

Allowing local test evolution makes this work actionable. Acceptance enforcement and stopping on genuinely unresolved blockers remain separate concerns; changing the test-evolution policy alone does not implement those safeguards.

Sources: [run plan](../factory/.factory/runs/factory-0042/plan.md), [0042 seed](../factory/iterations/0042-EvidenceOwnedByTarget.md), [task lifecycle](../factory/src/main/kotlin/TaskLifecycle.kt), [machine instructions](../factory/src/main/kotlin/MachineJobs.kt), [Git boundaries](../factory/src/main/kotlin/TargetRepository.kt), and [earlier plan-ownership discussion](2026-10-11-factory-plan-ownership.md). This document records the observed run and design questions; it does not change the factory or its specs.
