# Kaizen ledger

This ledger indexes every top-level Markdown note in `docs/kaizen/`. Repeated occurrences and evidence belong in the linked note, not in extra ledger columns. Notes about friction in the machinery that builds Qubigo (assembly lines, machine prompts, skills, scripts, handoffs, tooling); see `.pi/skills/kaizen-note/SKILL.md`.

## Status semantics and WIP

- **New**: captured, not assessed.
- **Open**: assessed and worth addressing.
- **Experiment**: an approved experiment currently in progress. **WIP limit: at most one.**
- **Closed**: no further action is intended, or the outcome is documented.

Use `Review due` only for the agreed review date of an active experiment. Use `—` for New, Open and Closed; do not infer triage dates. Classify from evidence in the note or an explicit decision, not from headings, age or experiment-like language.

## Ledger

| First observed | Kaizen note | Status | Review due |
| --- | --- | --- | --- |
| 2026-10-16 | [Problem: validator loop on evidence the line cannot produce (criterion 6)](2026-10-16-validator-loops-on-evidence-no-agent-can-produce.md) | New | — |
| 2026-10-16 | [Problem: `yx list --ready` unavailable in every task](2026-10-16-yx-command-missing-in-every-task.md) | Closed | — |
| 2026-10-16 | [Problem: Java 25 harness; machine-local paths in evidence](2026-10-16-java-25-harness-and-machine-local-paths-in-evidence.md) | New | — |
| 2026-10-16 | [Problem: agents run `./gradlew` from the wrong directory](2026-10-16-recurring-setup-failures-gradlew-wrong-directory.md) | New | — |
| 2026-10-16 | <a id="plan-repairs"></a>[Problem: plan outside repo, doubles as log, stale checkboxes](2026-10-16-plan-lives-outside-repo-and-doubles-as-a-log.md) | Open | — |
| 2026-10-16 | [Problem: task split put tests after implementation](2026-10-16-tests-after-implementation-split-by-task.md) | New | — |
| 2026-10-16 | [Problem: commits all named "Record factory task"](2026-10-16-uninformative-record-factory-task-commits.md) | New | — |
| 2026-10-16 | <a id="test-design"></a>[Problem: branch tests hard to understand and badly structured](2026-10-16-session-persistence-tests-hard-to-read-and-badly-structured.md) | Open | — |
| 2026-10-18 | <a id="output-format"></a>[Problem: prose-only doer output stops the run before validation](2026-10-18-doer-prose-output-stops-before-validation.md) | Open | — |
| 2026-10-18 | <a id="domain-design"></a>[Problem: domain design intent is partly missing and documented responsibilities are not upheld](2026-10-18-domain-design-intent-missing-and-documented-responsibilities-not-upheld.md) | New | — |
