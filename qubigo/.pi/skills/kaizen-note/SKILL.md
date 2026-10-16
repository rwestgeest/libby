---
name: kaizen-note
description: Offer to capture and, when accepted, write a docs/kaizen observation when friction, imperfection, waste, opacity, brittleness or failure shows up in the machinery used to build the product (assembly lines, machine prompts, skills, scripts, handoffs, tooling). Also use when the user asks for a kaizen note. Not for ordinary product bugs.
---

# Kaizen Note

Use this skill when a conversation or run reveals an imperfection in the machinery used to create Qubigo: assembly lines and machine configs in `.assembly-lines/`, agent skills and prompts, planner/doer/validator handoffs, the factory, sandboxing, CI and dev scripts, model routing, observability, recovery, missing tools.

The purpose is defect prevention: do not just patch around the immediate problem; capture what allowed it to happen so the cause can be removed.

A kaizen note is not the fix. It is a timely observation that preserves the evidence a later fix needs: what we expected, what happened, what made it possible, and what kind of guardrail might have caught or prevented it. Keep it factual. Do not turn it into a solution plan unless the user asks. `kaizen-investigate` establishes the cause and `kaizen-fix` applies a countermeasure; `kaizen` runs all three.

## How to think while capturing

Treat delivery-machinery friction as a weakness in the work system, not a personal mistake. Ask:

- What should have made this impossible, obvious, or easy to recover from?
- Which workflow, prompt, script, checklist, validation gate or handoff failed to protect us?
- Did the problem appear where it was caused, or only later?
- Did we get a clear signal, or did the system hide or blur the abnormality?
- What evidence will `kaizen-investigate` need?

## Trigger habit

When you notice relevant friction, pause and offer:

> This looks like pipeline/workflow friction. Would you like me to write a kaizen note in `docs/kaizen/` capturing the context and observations?

If the user declines, acknowledge and continue. Do not write the note.

If the user explicitly invokes the skill, says to record a kaizen note, or otherwise clearly asks for one, that is consent. Write it unless important facts are missing.

Assembly-line machines do not write notes. They append observations to `<plan path>.friction.md`; a human session turns that log into notes (see "From a friction log").

## What qualifies

Good candidates:

- A line, tool, sandbox, checkpoint, branch, commit or handoff behaved badly around product work.
- A process was ambiguous, brittle, hard to resume, or needed manual archaeology.
- A prompt, skill, script, validation gate or review loop created avoidable waste or confusion, including loops where retries change nothing.
- An operator workaround should be remembered.
- The system hid the real cause or lacked evidence to debug quickly.

Usually not candidates:

- An ordinary application bug or failing product test with clear product-code ownership.
- A feature request for user-facing behaviour.
- A personal preference with no observed delivery friction.

## Writing checklist

1. Confirm consent unless the user explicitly asked for the note.
2. Gather context: what the user told you, `git status --short --branch`, relevant paths (iteration docs, `.factory/runs/*/plan.md`, `.assembly-lines/`, scripts, skills), run ids, exact commands and failure text.
3. Identify the suspected weakness without over-investigating: guardrail, validation, error message, default, documentation, checklist, handoff, recovery path, observability or ownership boundary. Note urgency: minor, repeated, blocking, quality risk.
4. Check for an existing note on the same symptom before creating one: list `docs/kaizen/*.md` excluding `README.md`, and search titles and bodies for the symptom, command, tool, skill or handoff. Prefer amending a matching note even if the cause is uncertain.
5. If one exists, amend it: add a dated `### Additional observation: YYYY-MM-DD` subsection, keep the original observation, and update other sections only where the new evidence changes them. Do not rewrite into a solution plan.
6. Otherwise create `docs/kaizen/YYYY-MM-DD-short-observation-slug.md` from the template below.
7. Maintain `docs/kaizen/README.md` in the same change:
   - new note: add exactly one row with first-observed date, linked title, `New`, and `—` for Review due;
   - amendment: verify the existing row and link; keep its status unless the conversation holds an explicit assessment or outcome that justifies a change under the ledger's definitions;
   - never infer a review date; it is set only for an explicitly approved active experiment;
   - verify every top-level note except `README.md` has exactly one valid linked row, no row points to a missing note, and at most one row is `Experiment`.
8. Commit the note and ledger together with the message `kaizen: note <problem>`. Exclude unrelated working-tree changes. Do not edit application code, machine configs or assembly lines. Do not push.

## From a friction log

When asked to turn `<plan path>.friction.md` into notes: group entries by symptom, skip ordinary product bugs, check existing notes (step 4), and propose the list of notes to the user before writing any. Write only the ones the user accepts. Cite the log entry in the note's evidence.

## Note template

```markdown
# Problem: <short description>

Date: YYYY-MM-DD

## Context

What were we trying to do? Iteration, line, machine, skill or script, plan path, run id, branch.

## Expected standard

What workflow, command, prompt, script or handoff did we expect to work? Link the standard where it is written down.

## What happened

Observed friction, failure, ambiguity, abnormality or waste. Exact commands, statuses and failure text.

## Impact

Slowed us down, repeated, blocked progress, threatened quality, or risked customer-facing impact?

## What allowed it to happen

The system weakness that let it through: missing guardrail, weak validation, confusing command, unclear error, slow feedback, unclear docs, manual step, brittle dependency, missing checklist, poor observability, weak handoff.

## Observations

- Facts that distinguish this from ordinary product work.
- Where the machinery made work slower, more brittle, less observable or harder to resume.
- Workarounds used or safe retry commands, if known.

## Why this matters

Risk or waste for future delivery.

## Open questions

- Unknowns to investigate later.

## Possible prevention ideas

- Optional, only when obvious from the evidence, and secondary to the observation.
```

Use extra headings when the evidence calls for them. Prefer a short useful note over a comprehensive report.

## Style

Plain language. Specific paths, commands, run ids, branches, statuses and exact errors. Separate facts from hypotheses. Describe the system, not individual agents or people. Never include secrets, tokens or cookies.
