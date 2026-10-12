# Iteration 0042 — Record evidence with the target-owned plan

Status: Draft — product-owner interview in progress.

## Context and goal

Iteration 0041 records the target-owned plan at `.factory/runs/<name>/plan.md`, but automatic product commits exclude the rest of `.factory/`. Evidence beside the plan is therefore omitted; evidence under `docs/iterations/` splits the run record.

Keep the plan and selected evidence together in the target's Git history, so maintainers can understand acceptance decisions, outstanding checks, and resumed work. This extends 0041 without changing its scope or acceptance criteria.

## Business rules

### One target-owned run record

- Store the authoritative plan and selected evidence under the target's `.factory/runs/<name>/`. Keep the seed separate as the agreed product scope.
- Retain concise validation results, selected logs, product-owner feedback, and manual acceptance evidence, referenced from the plan. Include failures that explain unresolved issues or later corrections.
- Give every machine consistent, role-appropriate instructions for locating, reading, and writing the record. On resumption, use existing progress and referenced evidence.
- Preserve selected evidence in Git; exclusions for private execution metadata must not omit it.

### Accepted-task commits

- Before advancing or reporting completion, record implementation, selected evidence, and updated plan progress in separate, clearly related commits.
- Scope run-record commits to the selected run. Protect other runs, unrelated changes, and assembly-line configuration.
- Never record failed or rejected work as accepted or complete.

### Required-test gate

- Use TDD for authorized behavior changes. When this seed, iteration 0041's target-owned-plan contract, or recorded product-owner instructions supersede an older test expectation, the doer must update the affected local spec, tests and step definitions to assert the requested behavior. This includes fetched specs in `spec/`; no upstream course change is required. Record the conflicting expectations and the reason for the update. Run the revised test, inspect red where the implementation needs changing, implement the behavior and rerun the required suite. If the implementation already satisfies the revised expectation, record that result without manufacturing a failure.
- Preserve coverage of requirements that still apply. Do not delete, skip, filter out or weaken checks merely to get green. Ambiguous requirements still need clarification. The test gate applies at acceptance, not to the red phase or the act of implementing a requested change.
- All required tests must pass before accepted-task implementation, evidence, or completion-status commits. Pre-existing failures block these commits too; documenting a baseline failure does not waive the gate.
- Validator satisfaction or a completed checkbox cannot override failing required tests. Report blockers without advancing or declaring completion; do not silently weaken tests or expand scope to fix unrelated failures.
- Passing tests does not waive other acceptance requirements. Unavailable or unperformed manual acceptance remains outstanding.

## Acceptance criteria

1. Selected evidence is stored and actually committed beside the authoritative plan, despite the general `.factory/` exclusion. All machines receive consistent run-record instructions.
2. An accepted task with all required tests passing produces separate, clearly related implementation, evidence, and plan-progress commits before advancement or completion.
3. Resumption by run name preserves progress and uses referenced evidence and product-owner feedback without factory-side progress edits.
4. New and pre-existing required-test failures prevent accepted-task commits, advancement, and completion, even if the validator reports satisfaction. A documented baseline failure grants no exception.
5. Passing required tests permits the normal validation and commit lifecycle, subject to other acceptance requirements.
6. Recording evidence leaves other runs, unrelated changes, and assembly-line configuration untouched.

## Validation expectations

Use deterministic scenarios covering first execution, resumption, committed file contents, separate commit ordering, and run isolation. Test new and pre-existing required-test failures, validator satisfaction despite failure, and the passing counterpart. Verify actual commits and advancement; messages and checkboxes alone are insufficient.

Distinguish synthetic acceptance fixtures from genuine human or device checks. Factory demonstrations cannot substitute for checks required by a product seed.

## Open questions

- Should required-test failures also block the initial planning commit? The agreed gate covers accepted-task commits.
- Should blocked attempts leave changes and selected evidence uncommitted for inspection and resumption?
- How should existing evidence elsewhere be adopted without losing history or creating competing records?

## Boundaries

This is a product iteration; the assembly line owns technical design and task breakdown. Iteration 0041 remains unchanged.

Outside scope: full agent transcripts, archives of every attempt's output, private execution bookkeeping, a new acceptance UI, and performing Qubigo's real-device checks.
