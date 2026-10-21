# Kaizen ledger

This ledger indexes every top-level Markdown note in `docs/kaizen/`. Repeated occurrences and evidence belong in the linked note, not in extra ledger columns. Notes about friction in the machinery that builds Qubigo (assembly lines, machine prompts, skills, scripts, handoffs, tooling); see `.pi/skills/kaizen-note/SKILL.md`.

## Status semantics and WIP

- **New**: captured, not assessed.
- **Open**: assessed and worth addressing.
- **Experiment**: an approved experiment currently in progress. **WIP limit: at most one.**
- **Closed**: no further action is intended, or the outcome is documented.

Use `Review due` only for the agreed review date of an active experiment. Use `—` for New, Open and Closed; do not infer triage dates. Classify from evidence in the note or an explicit decision, not from headings, age or experiment-like language.

## Qubigo 004 — current follow-up

| Map thread | Adjustment | Verification |
| --- | --- | --- |
| 01 Added guarantees / 02 Context boundaries | Design discussion checkpoint implemented; [related investigation](#plan-repairs). | Run `qubigo-004a` (2026-10-19): checkpoint ran for 1a, 2b, 3c and 4a; no owner question was reopened afterwards. Plan stale-status friction persists. |
| 03 Domain design | ADRs 0007/0008 and machine guidance implemented; [ledger item](#domain-design). | Run `qubigo-004a`: validator still raised responsibility findings (gate coverage, test scope); effectiveness not yet judged. |
| 04 Increments / TDD | Done: detail the next capability and let tests drive design; [ledger item](#test-design). | Run `qubigo-004a`: 4a evidence admits its supplied partial tests were not test-driven; later tasks record red-to-green. Not yet judged. |
| 05 Output format | One tool-free format repair implemented for pi doers; [ledger item](#output-format). | Regression suite; run `qubigo-004a` shows no prose-only stop in `record.jsonl` (per-machine check not done). |

“Implemented” records the adjustment, not demonstrated effectiveness. Threads
01–02 share a checkpoint; the linked plan note is related evidence, not a dedicated
ledger item for those threads.

## Priority order (set 2026-10-20, from run `qubigo-004a` evidence)

Take these in order; the WIP limit still applies.

1. [SQLite shared-cache locks and OOM](2026-10-19-sqlite-shared-cache-locks-fail-full-backend-verification.md): hits the full-suite quality gate; small fix (uniquely named in-memory database per context).
2. [`./gradlew` from the wrong directory](2026-10-16-recurring-setup-failures-gradlew-wrong-directory.md): 11+ recurrences; one-line prompt or wrapper fix.
3. [Plan outside repo, doubles as log](2026-10-16-plan-lives-outside-repo-and-doubles-as-a-log.md): 137 KB plan, stale status reconciled every round; larger fix, suits an experiment.
4. [Validator loops on evidence no agent can produce](2026-10-16-validator-loops-on-evidence-no-agent-can-produce.md): stopped the run at task 6.
5. [Commits all named "Record factory task"](2026-10-16-uninformative-record-factory-task-commits.md): readability cost only.
6. [Branch tests hard to read](2026-10-16-session-persistence-tests-hard-to-read-and-badly-structured.md): no new evidence.
7. [Java 25 harness; machine-local paths](2026-10-16-java-25-harness-and-machine-local-paths-in-evidence.md): still New; triage later.

## Ledger

| First observed | Kaizen note | Status | Review due |
| --- | --- | --- | --- |
| 2026-10-16 | [Problem: validator loop on evidence the line cannot produce (criterion 6)](2026-10-16-validator-loops-on-evidence-no-agent-can-produce.md) | Open | — |
| 2026-10-16 | [Problem: `yx list --ready` unavailable in every task](2026-10-16-yx-command-missing-in-every-task.md) | Closed | — |
| 2026-10-16 | [Problem: Java 25 harness; machine-local paths in evidence](2026-10-16-java-25-harness-and-machine-local-paths-in-evidence.md) | New | — |
| 2026-10-16 | [Problem: agents run `./gradlew` from the wrong directory](2026-10-16-recurring-setup-failures-gradlew-wrong-directory.md) | Open | — |
| 2026-10-16 | <a id="plan-repairs"></a>[Problem: plan outside repo, doubles as log, stale checkboxes](2026-10-16-plan-lives-outside-repo-and-doubles-as-a-log.md) | Open | — |
| 2026-10-16 | <a id="test-design"></a>[Problem: upfront task detail and tests after implementation (04)](2026-10-16-tests-after-implementation-split-by-task.md) | Closed | — |
| 2026-10-16 | [Problem: commits all named "Record factory task"](2026-10-16-uninformative-record-factory-task-commits.md) | New | — |
| 2026-10-16 | <a id="test-readability"></a>[Problem: branch tests hard to understand and badly structured](2026-10-16-session-persistence-tests-hard-to-read-and-badly-structured.md) | Open | — |
| 2026-10-18 | <a id="output-format"></a>[Problem: prose-only doer output stops the run before validation](2026-10-18-doer-prose-output-stops-before-validation.md) | Closed | — |
| 2026-10-18 | <a id="domain-design"></a>[Problem: domain design intent is partly missing and documented responsibilities are not upheld](2026-10-18-domain-design-intent-missing-and-documented-responsibilities-not-upheld.md) | Closed | — |
| 2026-10-19 | [Problem: SQLite shared-cache locks and heap exhaustion intermittently fail full backend verification](2026-10-19-sqlite-shared-cache-locks-fail-full-backend-verification.md) | Open | — |
