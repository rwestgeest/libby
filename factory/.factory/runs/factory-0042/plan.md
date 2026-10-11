# factory-0042 — Target-owned evidence

## Scope and status

Seed: `iterations/0042-EvidenceOwnedByTarget.md` (unchanged; Draft, product-owner interview in progress).

Status: planned; no implementation or validation performed. Completion: **false**.

The authoritative progress and selected evidence live in this run directory, `.factory/runs/factory-0042/`. The seed defines scope, not progress. Preserve iteration 0041 and avoid assembly-line configuration changes, other runs, and unrelated changes. The factory handles commits; this planning step does not implement product work.

## Run-record and resumption rules

- On every resumption, read this plan and every referenced target-local evidence document, including product-owner feedback. Preserve existing progress and explicitly outstanding checks; do not regenerate from the seed or require factory-side acceptance edits.
- Store concise results, relevant failure logs, commit verification, and acceptance feedback beside this plan. Exclude private execution metadata and full transcripts.
- Record commands, outcomes, relevant file contents, commit IDs/order, and unresolved issues. Clearly label synthetic acceptance fixtures; they are not genuine human or device acceptance.
- Reference evidence below when it exists. Do not invent results or mark unavailable checks passed.
- Before accepted-task implementation, evidence, or completion-status commits, all required tests must pass, including pre-existing failures. Validator satisfaction and baseline documentation cannot override this gate.
- If blocked, report the blocker without advancement or completion. Do not silently weaken tests or fix unrelated failures. Keep the initial-planning-commit policy and disposition of blocked changes explicit pending clarification; do not assume a gate exception for accepted work.
- For each accepted task, the factory must make separate, clearly related implementation, selected-evidence, and plan-progress commits, in that order, before advancing or reporting completion. Verify actual commits, not just messages or checkboxes. Scope run-record commits to this run only.

## Implementation tasks (not started)

1. [ ] Inspect existing planning, machine instructions, resumption, required-test execution, validation, staging, and commit lifecycle. Identify required test commands and existing exclusions without modifying scope. Record findings and any baseline failures locally.
2. [ ] Make every relevant machine use the same authoritative run record, preserving recorded progress and reading referenced evidence and product-owner feedback on resumption. Keep seed and private execution metadata separate.
3. [ ] Preserve explicitly selected evidence beside the plan in Git despite general `.factory/` exclusions. Restrict staging to the selected run and protect other runs, unrelated changes, and assembly-line configuration.
4. [ ] Enforce the required-test gate on accepted-task implementation, evidence, and completion-status commits, advancement, and completion. Cover pre-existing as well as new failures, even when the validator reports satisfaction. Retain normal lifecycle when tests pass, subject to other acceptance requirements.
5. [ ] Produce and verify separate, related implementation, evidence, and plan-progress commits for accepted tasks before advancement/completion.
6. [ ] Add deterministic coverage for first execution, named resumption, actual committed contents and commit ordering, failure gates, passing counterpart, and isolation. Exercise acceptance end to end and record selected results.
7. [ ] Review all acceptance evidence and outstanding checks; report completion only when every required criterion is supported. Automated task completion alone is insufficient.

Each accepted task must follow the gate and commit protocol above. These checkboxes record implementation progress only and never establish acceptance by themselves.

## Acceptance evidence ledger

All entries are outstanding. No referenced evidence files exist yet.

| Criterion | Required evidence | Status |
| --- | --- | --- |
| 1. Evidence committed beside plan; consistent instructions | Deterministic first-run result, actual committed plan/evidence contents despite exclusions, and inspection of all relevant machine instructions | Outstanding; not run |
| 2. Separate related commits before advancement/completion | Actual commit IDs, ordering, contents, and advancement timing for an accepted task with passing required tests | Outstanding; not run |
| 3. Named resumption preserves progress and uses evidence/feedback | Resumption scenario with target-local recorded progress, referenced evidence and labeled product-owner feedback fixture; no factory-side progress edits | Outstanding; not run |
| 4. Failure gate, including baseline failures | New and pre-existing failing required-test scenarios, validator-satisfied variants, actual commit/advancement observations proving no accepted-task or completion-status commits and no advancement/completion; documented baseline gives no waiver | Outstanding; not run |
| 5. Passing counterpart | Passing required tests with normal validation and ordered commit lifecycle; other acceptance checks still enforced | Outstanding; not run |
| 6. Run and workspace isolation | Actual staged/committed contents plus before/after observations showing other runs, unrelated changes, and assembly-line configuration untouched | Outstanding; not run |

Planned target-local evidence files (create only when recording real observations):
- `validation.md`: required commands, test results, deterministic scenario evidence, selected failure output, commit IDs/order and isolation checks.
- `acceptance.md`: genuine product-owner feedback, manual requirements, decisions, evidence and explicitly outstanding checks. Label any synthetic feedback fixtures separately in `validation.md`.

## Manual acceptance and scope clarification

No manual acceptance evidence has been supplied or performed. The seed does not prescribe a specific real-device check for this iteration; Qubigo's real-device checks are explicitly outside scope. Synthetic factory demonstrations must never be recorded as satisfying checks required by another product seed.

Outstanding product-owner clarification (seed is still Draft):
- Confirm whether further human acceptance is required by the agreed scope; if required, record genuine evidence in `acceptance.md`. Until clarified, do not infer product-owner approval from automated results.
- Resolve whether required-test failures block the initial planning commit (the agreed gate currently covers accepted-task commits).
- Resolve whether blocked changes and selected evidence remain uncommitted for inspection/resumption.
- Resolve how to adopt existing evidence elsewhere without losing history or creating competing authoritative records.

These open questions do not grant permission to relax the explicit accepted-task test gate or expand scope. Completion remains pending acceptance evidence and any required manual checks; unavailable or unperformed checks must remain outstanding.

## Recorded progress

- Initial planning: seed read; this target-owned plan created. No product changes, tests, commits, or acceptance checks performed by the planner.
- Automated implementation: not started.
- Automated acceptance: not performed.
- Product-owner/manual acceptance: not performed; clarification outstanding.
- Blockers: no observed test failure yet; required commands and baseline still need inspection. Draft-scope/manual clarification remains outstanding.
