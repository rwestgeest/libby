---
name: kaizen
description: "Carry a delivery-workflow problem end to end: capture or amend a kaizen note, investigate its cause, and apply and validate a fix. Use when asked to kaizen a problem or take a workflow/tooling observation through resolution; use the stage-specific skills for note-only, investigation-only, or fix-only requests."
---

# Kaizen

Run the three stages in order for delivery-machinery friction: [Kaizen Note](../kaizen-note/SKILL.md) → [Kaizen Investigate](../kaizen-investigate/SKILL.md) → [Kaizen Fix](../kaizen-fix/SKILL.md). Read and follow each skill at its stage; this skill does not replace their evidence, ledger, validation, and commit rules.

- An explicit request for `kaizen` authorizes capture of a note; when merely noticing friction during unrelated work, follow `kaizen-note` and ask before recording it. A kaizen request does not grant permission for risky, product-facing, or disputed fixes, external actions, or pushing.
- First identify the observation or existing matching note. Amend a matching note instead of duplicating it. If no observation or note can be identified, ask for the missing context. Preserve the original evidence; do not turn capture into an assumed root cause or solution.
- Investigate the selected note before changing the machinery. Reuse findings already in the note and check that they still hold. Record uncertainty and decisions; do not force an answer when evidence runs out.
- Proceed to fix when there is an evidence-backed, low-risk action within scope, or after the user chooses between meaningful alternatives. If the decision is pending, the cause is external, or action is unsafe, stop after documenting the investigation and clearly report why the fix is pending. Do not mark an unimplemented countermeasure resolved.
- Maintain one note and its ledger row throughout. Follow each stage's commit convention; if the stages run together, a single scoped commit covering capture, investigation, fix, and ledger is also acceptable. Follow project validation rules for the actual changes, and never include unrelated work or push without being asked.

Report the note path, supported cause, what was changed or what decision is needed, validation, commit status, and any effectiveness follow-up.
