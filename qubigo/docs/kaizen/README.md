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
| 01 Added guarantees / 02 Context boundaries | Design discussion checkpoint implemented; [related investigation](#plan-repairs). | In 004a |
| 03 Domain design | ADRs 0007/0008 and machine guidance implemented; [ledger item](#domain-design). | In 004a |
| 04 Increments / TDD | Done: detail the next capability and let tests drive design; [ledger item](#test-design). | In 004a |
| 05 Output format | One tool-free format repair implemented for pi doers; [ledger item](#output-format). | Regression suite; observe in 004a |

“Implemented” records the adjustment, not demonstrated effectiveness. Threads
01–02 share a checkpoint; the linked plan note is related evidence, not a dedicated
ledger item for those threads.

## Ledger

| First observed | Kaizen note | Status | Review due |
| --- | --- | --- | --- |
| 2026-10-16 | [Problem: validator loop on evidence the line cannot produce (criterion 6)](2026-10-16-validator-loops-on-evidence-no-agent-can-produce.md) | New | — |
| 2026-10-16 | [Problem: `yx list --ready` unavailable in every task](2026-10-16-yx-command-missing-in-every-task.md) | Closed | — |
| 2026-10-16 | [Problem: Java 25 harness; machine-local paths in evidence](2026-10-16-java-25-harness-and-machine-local-paths-in-evidence.md) | New | — |
| 2026-10-16 | [Problem: agents run `./gradlew` from the wrong directory](2026-10-16-recurring-setup-failures-gradlew-wrong-directory.md) | New | — |
| 2026-10-16 | <a id="plan-repairs"></a>[Problem: plan outside repo, doubles as log, stale checkboxes](2026-10-16-plan-lives-outside-repo-and-doubles-as-a-log.md) | Open | — |
| 2026-10-16 | <a id="test-design"></a>[Problem: upfront task detail and tests after implementation (04)](2026-10-16-tests-after-implementation-split-by-task.md) | Closed | — |
| 2026-10-16 | [Problem: commits all named "Record factory task"](2026-10-16-uninformative-record-factory-task-commits.md) | New | — |
| 2026-10-16 | <a id="test-readability"></a>[Problem: branch tests hard to understand and badly structured](2026-10-16-session-persistence-tests-hard-to-read-and-badly-structured.md) | Open | — |
| 2026-10-18 | <a id="output-format"></a>[Problem: prose-only doer output stops the run before validation](2026-10-18-doer-prose-output-stops-before-validation.md) | Closed | — |
| 2026-10-18 | <a id="domain-design"></a>[Problem: domain design intent is partly missing and documented responsibilities are not upheld](2026-10-18-domain-design-intent-missing-and-documented-responsibilities-not-upheld.md) | Closed | — |
