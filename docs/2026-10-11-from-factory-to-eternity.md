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

The initial advice to reconcile the course spec was specific to that ownership rule. It should not be generalized into “every failing existing test needs external approval.” Within an authorized behavior change, updating superseded tests is part of implementation. Under this repository's current rules, course content is authored in the tutorial repository and fetched here, so that is where the conflicting executable requirement must be updated.

## Questions for the factory contract

- When a new iteration supersedes an older requirement, how is that precedence expressed so the doer can distinguish an obsolete expectation from a regression?
- Who may update the executable spec? If fetched specs remain immutable here, how does the factory request or perform the corresponding change in the authoritative course repository?
- How do we explicitly permit the TDD sequence—change the expectation, observe red, implement, reach green—while still enforcing passing tests at acceptance?
- How does a machine report “blocked pending a decision” so the factory stops and exposes the decision, rather than routing another identical attempt?
- What constitutes task advancement, and what bounds repeated cycles that produce notes or commits without advancing a task?
- What evidence must support acceptance? An empty findings list, a route through the graph, and the existence of a commit each proved insufficient here.

## Direction for resolution

Resolve the old/new planning expectation at its authoritative source and fetch the reconciled spec. Preserve the distinction between tests failing during development and tests failing at acceptance.

Give the factory an explicit blocked outcome and a bound on repeated cycles without advancement. Enforce the required-test gate before accepted-task commits, and avoid labeling blocked observations as accepted progress. Keep selected evidence with the run record so resumption can use the blocker already discovered instead of repeatedly rediscovering it.

These are related changes with separate purposes: reconciling the spec makes the work actionable; enforcing acceptance prevents false claims; stopping on a blocker prevents the factory from running to eternity.

Sources: [run plan](../factory/.factory/runs/factory-0042/plan.md), [0042 seed](../factory/iterations/0042-EvidenceOwnedByTarget.md), [task lifecycle](../factory/src/main/kotlin/TaskLifecycle.kt), [machine instructions](../factory/src/main/kotlin/MachineJobs.kt), [Git boundaries](../factory/src/main/kotlin/TargetRepository.kt), and [earlier plan-ownership discussion](2026-10-11-factory-plan-ownership.md). This document records the observed run and design questions; it does not change the factory or its specs.
