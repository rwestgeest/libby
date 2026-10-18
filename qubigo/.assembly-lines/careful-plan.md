# Careful plan assembly line

Run from the factory checkout:

```sh
bin/factory --run qubigo-careful-plan --target q --line careful-plan --seed q/docs/iterations/002-clearer-onboarding.md
```

The authoritative runnable line is `.assembly-lines/careful-plan.dot`. Machine
`config.json` files select harness/models; their `AGENTS.md` files carry the jobs.

The initial planner (`plan_initializer`) creates a short capability sequence,
or reads an existing plan. Incomplete work goes to `design_walkthrough`, which
prepares a journey/event/policy sketch for the initial journey/collaboration or a new unresolved choice at
`<exact plan path>.discussion.md`. It stops with `blocked: true` until the owner
has discussed the sketch and recorded scoped decisions. This is the checkpoint
for added guarantees and changed bounded-context dependencies.

From Libby's root, ask Codex or pi:

> Pick up the open design discussion for run <run>.

The `discuss-factory-design` skill conducts that conversation. It does not resume
the factory. After decisions are recorded, explicitly rerun the same factory
command. Execution begins at the start, preserves the discussion, and the
checkpoint proceeds only for resolved, matching scope.

Revision then elaborates the current capability. Three independent reviewers
must approve the same plan hash before implementation. Ordinary review defects
repeat revision; newly unagreed guarantees/boundaries block for owner discussion.
Parallel reviewers write only their own reports; the interactive session records
their questions in the shared discussion. Revision also preserves unresolved
reports when blocking. A resolved document covers its recorded scope, which may span several tasks.

The doer/RDD validator loop remains unchanged for ordinary defects. After accepted
work, the planner records completion. The planner returns complete plus needs_design_discussion. Completion routes
to finish first. Routine tasks within recorded decisions route directly to the
doer; a new scope/dependency/guarantee question routes through the walkthrough,
revision and review. A new task alone does not require human input. The RDD validator can also block if repair needs a new owner
design decision. Prior decisions remain available and apply to all work within their recorded
scope; they are not blanket approval of unrelated design changes.

Reviewer models are configured individually and override the shared CLI model. Pi needs access to OpenAI Codex and OpenRouter. Reviewers share the same review criteria; only model and report filename differ. Report sidecars live beside the run plan, outside product commits. The readable lens lives under `.assembly-lines/lenses/`; the RDD validator
instructions include it in their `AGENTS.md`.

Plan review repeats until unanimous approval; the factory's `--max-attempts` bounds implementation attempts, not plan revision rounds. Stop the run manually if reviewers cannot converge. Consensus and report writing rely on the configured agents following their instructions. No real-agent run is performed by installing this line.

## Human input and blocked runs

The factory must support `blocked: true` machine results (the current factory
implements this in `TaskLifecycle.receiveResult`). A doer, validator or planning
machine uses this result when progress needs unavailable external input, access
or human attestation. Findings name the missing input, its supplier and the
evidence needed to resume. This stops the run before routing, acceptance or
commit; the task stays unfinished and working changes remain available. This is
not a validator rejection and does not retry the doer. Ordinary fixable defects
still follow the rejection edge and attempt limit.

Planning identifies agent-verifiable criteria and human-attested gates without
weakening the seed. Supply the missing input, then rerun the factory command with
the same run name, target, line and seed to revisit the unfinished task. A blocked
run is not completion; only actual evidence can close the gate. Keep one unresolved
finding per missing input instead of appending a narrative on every attempt.

## Verification limit

The graph and handoff instructions can be checked without a real-agent run.
Owner-decision recognition and question quality remain agent responsibilities;
there is no factory parser enforcing discussion-file status. Observe first-stop, discussion pickup, unchanged-scope rerun and new-question
blocking in 004a. No effectiveness claim follows from installing instructions.

## Capability detail and TDD

Keep an overall journey outline, but detail only the next releasable capability:
actor, outcome, relevant rules, acceptance examples and deferrals. The doer
uses TDD to discover collaborators within the agreed ADRs; a lightweight plan is
intentional. Reviewers do not demand later functionality or implementation
recipes. Effectiveness of this adjustment will be checked in 004a.

The design walkthrough explicitly uses `openai-codex/gpt-6.1-sol:medium` rather
than inheriting the run/harness model. Task completion alone does not reopen
its discussion. Planner routing checks `complete` first, then
`needs_design_discussion`; return both booleans in the planner result.
