---
name: kaizen-fix
description: Implement and validate a countermeasure for an investigated docs/kaizen note, then document its resolution. Use when asked to fix or resolve a kaizen note, not for investigation-only requests.
---

# Kaizen Fix

Apply an evidence-backed countermeasure for a delivery-machinery problem captured in `docs/kaizen/`. This is the implementation stage, not a shortcut around cause analysis. If no adequate investigation exists, follow [Kaizen Investigate](../kaizen-investigate/SKILL.md) first (including its note selection and debugging rules); for a direct fix request, continue to implementation in the same task rather than stopping after the assessment. When operating inside `kaizen`, do not repeat its completed investigation.

A note may identify an ordinary product bug as a symptom; change product code only if the supported cause is in delivery machinery and the authorized fix warrants it. Preserve original observation and investigation evidence.

## Fix workflow

1. Run `git status --short --branch` and protect unrelated work. Resolve a supplied note path/title/slug to one note; if unspecified, use the investigation skill's selection rules. Read the note and its investigation fully. Check current repository state and history: if the proposed fix already exists, document the evidence instead of reapplying it.
2. Confirm the causal mechanism, selected countermeasure, and validation plan. If evidence is incomplete, investigate using [Kaizen Investigate](../kaizen-investigate/SKILL.md) before acting. Use [A3](../kaizen-investigate/a3-problem-solving.md) to predict what the change should improve, and distinguish correction, containment, and prevention. An options list is not approval of a risky or disputed choice.
3. Apply the smallest low-risk fix within the authorized scope without an unnecessary approval round. For meaningful trade-offs, multiple plausible fixes, or risky/product-facing changes, brief the user on the impact, causal evidence, uncertainty, and viable options (including deferral where reasonable). Recommend an option and ask one focused decision question at a time. Summarize the agreed scope before acting if it differs materially from the chosen option. If a decision is deferred or the root cause is external, record findings/options and stop without claiming a fix.
4. Validate the change with focused tests first and the target project's required quality gates. Do not run the full build for docs-only changes (including skill, prompt and `.assembly-lines` text) unless asked. For code, config, executable scripts or app-behaviour changes run the quality gates in the relevant `AGENTS.md` (backend: `./gradlew test` and `./gradlew build` from `qubigo-backend`). Compare observed results with the predicted outcome; passing tests is not proof that recurrence has been prevented in production.
5. Append or update `## Resolution` in the original note (no duplicate headings): date, root cause, selected fix and rationale, changed paths, validation and observed versus expected results, remaining follow-up, and owner/review point if effectiveness needs later observation. If no fix was applied, retain `## Investigation` with the recommendation and pending decision instead of marking it resolved. Preserve the original observation.
6. Update the note's existing row in `docs/kaizen/README.md`: use `Open` when action remains worthwhile, `Closed` when no further action is intended or the outcome is documented. `Experiment` is only for an explicitly approved active experiment with an agreed review date; otherwise Review due is `—`. Verify each top-level note except `README.md` has exactly one valid linked row, no missing targets, and at most one `Experiment`.
7. Review `git status --short` and `git diff --stat`; commit only the fix, note, ledger, and directly supporting changes with `kaizen: <short resolved problem>` unless asked not to commit. Do not push or create a PR unless asked.

Report the note, cause, action (or pending choice), changed paths, validation, commit SHA/message (or why none), and remaining follow-up. Do not conflate a fix being applied with effectiveness demonstrated.
