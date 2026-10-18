---
name: discuss-factory-design
description: Pick up an open factory design discussion with the user, walk through journeys, events and policies, and record decisions before detailed planning. Use when asked to discuss a factory design sketch or resolve a design checkpoint; does not implement or resume the run.
---

# Discuss factory design

Conduct the discussion with the user; do not complete the design investigation
alone. Keep explanations short, use a small collaboration sketch when helpful,
and ask one substantive question at a time. A run waiting for a human decision
is not asking for implementation.

## Locate the handoff

Start at the factory repository root. Use the named run or explicit discussion
path. Discover run directories rather than assuming a single location: this
checkout uses `factory/runs/<run>/`; targets may use `.factory/runs/<run>/`.
Read `settings.json` to identify the live target and seed; keep the target
separate from documentation snapshots. Read relevant target instructions.

The handoff convention is **`<exact-plan-path>.discussion.md`**. For example,
`factory/runs/example/plan.md.discussion.md`. The live Qubigo `careful-plan` line now has a `design_walkthrough` machine
that writes this handoff and blocks for discussion. Other lines may not adopt
this convention. The factory itself does not parse or enforce its contents.

If no run was named, list existing discussion files and their status. Pick a
sole open discussion; ask which one when several are open. Never pick by lexical
order or invent `qubigo-004a`. If a named run lacks a handoff, inspect its plan
and blocked findings. Explain the gap and draft a short handoff from that evidence
when possible, leaving unmade decisions open. Ask for the run/path when still
ambiguous. Never overwrite an existing discussion or reopen a resolved one
without a new question or requested reassessment.

## Work through the design together

Also inspect blocked findings in `<plan>.plan_reviewer_1.json`,
`<plan>.plan_reviewer_2.json`, `<plan>.plan_reviewer_3.json` and the last run
status/output when relevant. Parallel reviewers do not edit the handoff: bring
their new design questions into its open questions, preserving existing decisions.
A stale report about an already settled question is evidence to reconcile, not
a reason to ask it again. Do not erase reports or run logs.

Read the handoff, the relevant seed requirements and referenced architectural
decisions/code examples. Briefly recap the capability and first open question.

- Start with the user's journey and the smallest useful, releasable capability.
- Walk through domain events, reacting policies, and responsibility ownership.
  Make changed dependencies between bounded contexts visible to the author.
- Separate agreed product behavior from implementation choices and proposed
  guarantees. For each extra guarantee ask what requires it, the consequence of
  omitting it, and whether handling it now is worth the complexity. Present the
  simplest adequate approach and alternatives without treating a possible
  failure as an automatic blocking requirement.
- Use the established command/aggregate/adapter collaboration as evidence.
  Clarify unwritten intent with the user. Do not prescribe transaction, retry,
  messaging or SQL infrastructure before the capability needs it.
- Record decisions as the conversation proceeds, including deferred guarantees
  and why. Silence, a candidate option, or a request to explore is not agreement.
  Do not turn a tentative next increment into a fixed implementation plan.

## Persistent discussion format

Use this small Markdown structure, preserving existing evidence and decisions:

```markdown
# Design discussion: <run> / <capability>

Status: open
Run: <run id>
Plan: <exact path>
Target: <live repository>
Seed: <path>

## Capability and sketch
<Short journey; events → policies → owning contexts. Links to relevant evidence.>

## Open questions
- <Decision needed and who can supply it. For added guarantees: reason and cost.>

## Decisions
- <Date, user decision, rationale, and affected scope/question.>

## Handoff
<Next open question, or decisions resolved and ready for planning.>
```

Keep `Status: open` while any required decision remains. Use `Status: resolved`
only when the user has settled all listed questions, including explicitly
accepted deferrals. Resolved means ready for planning of this scope, not approval
of future design changes or implementation acceptance. If the capability/sketch
changes materially, record the change and reopen affected questions.

End each session with a link to this file and the next question or readiness.
Preserve notes even when the conversation ends before decisions are complete.
Do not edit run status, mark implementation tasks complete, change production or
machine behavior, launch/resume a factory run, or commit/push merely by invoking
this skill. Separate explicit user instructions may authorize those actions.

## Invocation from this repository

In Codex: `Use $discuss-factory-design for run <run>.`
In either harness: `Pick up the open design discussion for run <run>.`

The pi entry at `.pi/skills/discuss-factory-design` points to this same skill.
If automatic loading is disabled, explicitly read this file, or launch pi from
the repository root with `pi --skill .agents/skills/discuss-factory-design`.
An existing session may need to reload its skill catalog; reading this file is
the fallback. Discovery does not start a discussion or resume a run by itself.
