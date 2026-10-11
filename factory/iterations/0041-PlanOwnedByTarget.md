# Iteration 0041 — The target owns its plan

## Context

The Qubigo run exposed a conflict in the factory's ownership contract: the factory supplied a plan path under its own `runs/` directory while instructing agents to work only inside the target. Agents created a target-local plan instead, but subsequent invocations continued to reference the missing factory-owned plan.

The problem is more than a path mismatch. Product changes belong to the target repository, but a factory-owned plan separates task progress and acceptance decisions from the history of the work they describe. A product owner should not have to update another repository to record acceptance of their product.

This iteration adopts **solution B** from [Where should a factory run's plan live?](../../docs/2026-10-11-factory-plan-ownership.md): **the factory owns execution; the target owns the work record.**

## Value and goal

A target maintainer can follow the implementation, task progress, and acceptance evidence together in the target's Git history. An operator can resume a named run without reconciling competing plans or granting agents write access outside the target.

The outcome is one authoritative, target-owned implementation plan per run, used consistently by every machine and preserved with the work it describes.

## Scope

### One authoritative plan

- Each run's implementation plan lives inside its target repository, at a stable, discoverable location distinct for that run.
- The factory and all machines refer to that same plan on first invocation, retries, and later invocations by run name alone.
- Different runs must not overwrite or accidentally reuse one another's plans, including when they use the same target.
- The factory does not maintain a second authoritative copy in its own `runs/` directory.
- The seed remains the statement of desired value and acceptance criteria. The implementation plan is a separate document; creating or updating it must not overwrite the seed.

### Work and progress share a history

- The initial plan and subsequent task-progress updates are recorded in the target's Git history at defined planning and task-completion boundaries.
- After a task is accepted, its implementation and the corresponding plan status are both preserved before the run proceeds to the next task or reports completion. They need not be in the same commit, but their relationship must be understandable from the recorded history.
- Failed or rejected work must not be recorded as a completed task.
- Plan changes are part of the target's work record, not disposable factory metadata. Validation must accommodate these documents without treating a status update as proof that the product meets its acceptance criteria.
- Existing protections for unrelated repository changes and assembly-line configuration remain in place.

### Acceptance belongs to the target

- Acceptance evidence and outstanding checks are recorded in the target, either in the plan or in target-local documents referenced by it.
- On resumption, the planner uses that record to distinguish completed work from outstanding acceptance. It must not discard recorded progress or silently recreate the plan from the seed.
- An unavailable manual check remains explicitly outstanding. Moving the plan does not turn an unperformed check into a pass or authorize the run to declare the iteration accepted.
- A product owner can record manual acceptance in the target and resume the run without editing factory-owned progress state.

### The factory still owns execution

- Run settings, routing, agent invocation, retries, and execution bookkeeping remain factory responsibilities.
- Agents remain confined to the target for their work; no external-plan write exception is needed.
- The planner continues to own plan contents and completion decisions. The factory need not parse task checkboxes or impose a new plan schema.
- A missing plan must not lead to implementation against an invented alternative path: planning must establish the authoritative plan before product work begins.

## Acceptance criteria

1. Starting a new run creates its implementation plan inside the target, separate from the seed. No authoritative implementation plan is created in the factory's run directory.
2. The planner, doer, and validator receive the same target-local plan location. The plan can be read and updated as appropriate without leaving the target.
3. Invoking the run again by name uses its existing plan and recorded progress rather than generating a replacement or referencing the former factory-owned location.
4. Two named runs using the same target have distinct plans; executing or resuming one leaves the other's plan unchanged.
5. The target's Git history preserves initial planning and, at each accepted task boundary, both the task's work and its updated completion status. Rejected attempts do not cause the task to be marked done.
6. A run with automated work complete but manual acceptance outstanding retains that distinction in its target-local record. After genuine acceptance evidence is recorded there, a resumed planner can use it without requiring a factory-side plan edit.
7. If planning fails to produce the authoritative plan, the factory reports the problem and does not start product implementation using a missing plan or a second, improvised location.
8. Unrelated repository changes and assembly-line configuration retain their existing protections. Adding plan history does not broaden what the factory may commit indiscriminately.

## Validation expectations

Demonstrate first-run planning, named-run resumption, task-completion history, and isolation between two runs in one target. Include an outstanding manual-acceptance case followed by target-local evidence and resumption. Check that all machines use the same plan and that no writes outside the target are required for progress or acceptance.

Use deterministic factory examples for the ownership and lifecycle contract. A real-agent demonstration may provide additional confidence, but it cannot substitute for actual device or human acceptance checks required by a target's seed.

## Boundaries

This is a value iteration, not an implementation plan. The assembly line owns the task breakdown, exact target-local path convention, and technical changes needed to deliver these outcomes.

Automatic migration of old factory-owned plans is outside this iteration. Existing runs must not silently lose progress or acquire two competing plans; if they cannot be resumed under the new contract, report that limitation clearly rather than starting over.

Also outside scope: a complete audit trail of every agent attempt, saved prompts and routing decisions, a new acceptance UI, changes to assembly-line routing, and performing Qubigo's outstanding real-device checks. Target-owned plan history improves the work record but does not by itself reconstruct every execution attempt.
