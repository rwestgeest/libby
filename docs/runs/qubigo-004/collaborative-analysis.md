# Qubigo 004 — collaborative investigation

**Start with the [short visual summary](five-whys-summary.md).**

Captured 18 October 2026 from the discussion with Rob. Investigation in progress.
This is the primary account of findings agreed together. Earlier agent-only
kaizen assessments are provisional evidence and hypotheses, not a completed
collaborative analysis. No countermeasure, redesign or rewrite is approved here.

## Observation: what prompted the investigation

Rob explored the code while the run was struggling and found:

- Competition knowledge cluttering `MakeMove`, coupling Games to Competitions.
- A competition move protocol implemented directly through SQL, SQL processing
  in `SqliteBasedGameRepository` and other new repositories, and accidental
  transaction machinery such as `CompetitionWriteBoundary`.
- Missing tests for the collaboration, apparent difficulty testing it, and a
  lack of TDD.
- Plan/validator remarks about transactional integrity whose necessity he did
  not recognize.

These are Rob's observations and design concerns. Absence of TDD across the
whole run has not been independently established. The question is whether the
machinery is needed, before asking how to prove it correct.

## Product intent clarified by Rob

The competition owns registration and coordinating invitations/game creation.
Games should publish domain events such as `MoveWasMade` (and potentially game
creation). A domain policy reacts to those events to update the competition or
leaderboard. Games need not know about Competitions.

An unfinished underlying game can continue after competition closure. The
competition ignores moves after closure; its interpretation and result freeze.
Existing game-creation (initial/zero move) and move timestamps supply speed data.
Checking timestamp semantics was not an instruction to redesign admission or
move persistence. Durable closure/reminders do not in themselves call for
complicated transactional coordination.

Rob would initially use the existing execution context and database connection.
Moves are append-only, their known concurrency issues have been addressed, and
other existing operations are view updates or atomic actions. He reports no
observed view glitches. Stronger transaction handling may be possible, but he
judges the current risk low; retries are a separate concern that can come later.
This is the owner's stated engineering judgment, not a measured absence of all
possible failures.

## Agreed finding 1: speculative risks acquired requirement status

The [specification](../../../q/docs/iterations/004-competitions.md:63) requires
one frozen result and authoritative ordering at closure. It does not prescribe
cross-context atomic updates, `BEGIN IMMEDIATE`, or the chosen transaction split.
See also its existing-journal direction at line 69 and durable processing at 140.

The local [machine record](../../../factory/runs/qubigo-004/record.jsonl:5)
shows this progression:

| Record line | Observation |
| --- | --- |
| 5 | First revision says it strengthened cutoff recovery, timestamp admission and timing-error checks; no reviewer reports existed yet. |
| 20 | An initial reviewer endorses atomic-boundary decisions by reference to source structure. |
| 139–173 | Revisions add rollback, concurrent finalization, crash recovery, sub-precision ordering and serialized matching requirements. |
| 187 | A reviewer correctly finds contradictory requirements for preserving versus rolling back game events on result failure. |
| 229 | Revision chooses separate event/result transactions and recovery. |
| 249 | That choice exposes another contradiction concerning notification suppression. |

The process challenged whether proposed machinery was internally correct, but
insufficiently challenged whether it was necessary. A conceivable failure became
an implicit requirement; its countermeasure introduced further obligations.
Rob recognizes this pattern from discussions with human engineers too: it is
not specific to agents.

The exact originating reasoning remains unknown: the record contains summaries,
not all historical plan versions or full agent sessions. We cannot attribute
every extra layer to one reviewer or instruction.

## Agreed finding 2: intended boundaries were not checked with their author

Rob wrote most of the existing application manually. Some design intent is
obvious to him but not explicit in ADRs: bounded contexts should collaborate
loosely, while selected narrow dependencies are deliberate. Games sending
notifications through an interface implemented by Notifications and connected
in Games wiring is one such accepted dependency; explicit domain policies are
not used everywhere.

The missing safeguard was not exhaustive advance documentation. Introducing
competition knowledge into Games was a boundary decision that needed to be
made visible and discussed with Rob. It proceeded without that check. Incomplete
written intent and speculative guarantees combined to permit the unwanted
collaboration structure.

## Instruction evidence and working explanation

Current planning instructions already forbid speculative infrastructure;
reviewers already say not to invent requirements or demand speculative design.
The design lens calls for the simplest design for today's behavior. Therefore
another generic instruction to avoid overengineering is not an adequate finding.

At the same time, planners must enumerate error/edge cases, reviewers identify
blocking findings, and revision must address concrete findings. There is no
explicit step requiring added guarantees to be justified against product needs,
observed problems or an agreed risk judgment. Our working explanation is that
technical concreteness gave a scenario authority as a blocker; after entering
the plan it became a contract for subsequent reviews. This explanation is
supported by the sequence, but instruction wording alone does not prove cause.

References: [initializer](../../../q/.assembly-lines/.machines/plan_initializer/AGENTS.md:15),
[revision](../../../q/.assembly-lines/.machines/plan_revision/AGENTS.md:7),
[reviewer](../../../q/.assembly-lines/.machines/plan_reviewer_1/AGENTS.md:1),
[design lens](../../../q/.assembly-lines/lenses/responsibility-driven-design.md).
These are local live files; their history was inspected, but exact invocation
prompts are not captured by these links.

## Candidate countermeasure agreed for consideration

Before detailed planning of cross-context behavior, hold a short collaborative
walkthrough of domain events and policies, with a responsibility/collaboration
sketch. Rob confirmed that such a sketch could have exposed the unwanted
direction and suggested an event-storming walkthrough of the new domain events.

Start from what happens in the domain, which policy reacts, which bounded
context owns the response, and what closure changes. Make proposed boundary
changes and added guarantees visible before choosing SQL/transaction machinery.
This is a candidate intervention, not a requirement to approve every code detail.
No effectiveness claim, implementation or active experiment follows from this
capture.

## Still open

- Continue the causal investigation with Rob; do not mark it complete on the
  strength of agent-only notes.
- Decide how the walkthrough and risk discussion should fit the factory process,
  and how to check whether they prevent the observed drift.
- Investigate testability/TDD and the persistence design further as needed;
  event-driven collaboration is Rob's intended direction, not yet a tested design.
- Keep the unreadable JSON handoff as a separate causal branch. Earlier proposed
  format repair remains provisional and is not prioritized by this discussion.
- Decide what to retain or simplify only after this investigation. No rewrite,
  factory fix, branch switch, or run restart has been selected here.

## Additional agreed finding: design guidance and its application

Rob confirmed that the competition flow differs from the intended Games
structure: Domain Primitives, validated typed inputs at the controller boundary,
immutable aggregates owning behavior, and commands coordinating domain work and
persistence. Inspection found Domain Primitives and the clean-input boundary
insufficiently explicit, while immutability and command responsibilities are
already documented. Discovery failure alone does not explain nonconformance;
the plan claims architecture guidance was read. Exact causes remain open.

Captured with code/documentation evidence and open questions in the
[kaizen note](../../../qubigo/docs/kaizen/2026-10-18-domain-design-intent-missing-and-documented-responsibilities-not-upheld.md).
No documentation rule, production code or machine behavior has been changed.
Rob also clarified the likely TDD starting point: creating a competition after
an event-storming/user-journey walkthrough, rather than starting with moves.

## Rob's implementation approach: releasable capabilities and design through TDD

Captured from the subsequent discussion. This describes how Rob would approach
implementation; it is not an approved 004a plan or a fixed task sequence.

### First capability: create and join, accessible only to admins

Use the keystone principle in the absence of feature flags: keep the emerging
feature admin-only so intermediate capabilities can be released without
confusing other players. An admin can create a competition and join it. Joining
records membership; it does not yet arrange games or invitations.

Rob described an outside-in TDD route:

1. Test-drive the controller, DTO mapping/validation and typed parameter object
   as needed to express its interaction with the `CreateCompetition` application
   service.
2. Test-drive that application service and the definitions of Domain Primitives.
   A small `CompetitionAggregate` emerges; business rules belong there.
3. Test-drive further aggregate behavior as it becomes necessary.
4. Test the repository implementations, probably in-memory and persistent. The
   repository interface and in-memory implementation likely emerge earlier from
   command tests.

This is not a mandatory layer-by-layer recipe. Rob could also start inside-out
because he knows where the business rules belong. The commitments are TDD,
clean domain inputs, aggregate-owned behavior and command-owned coordination.
Persistence details follow the behavior being built.

### Likely next capability, subject to learning

After creation and membership, probably demonstrate one game updating the
competition: test-drive competition move behavior and the policy connection
from Games' move event to the competition. Rob explicitly said it is hard to
know the exact next step in advance; this remains a likely direction.

Game/invitation creation logic would then be introduced in the competition
aggregate. Drafting a competition and starting it later can be further features
of that same aggregate, rather than prerequisites for the first increment.
Monitor the aggregate's size and responsibilities as it grows.

### Learning for the factory investigation

A useful increment may intentionally deliver only a limited capability. It need
not anticipate all the machinery of the finished feature. Rob would choose a
small capability, test-drive it within the established design, and use what is
learned to select the next increment. The factory instead prescribed substantial
future machinery before proving the basic domain collaboration.

Next investigation thread: why did instructions calling for small independently
verifiable increments still produce that upfront elaboration? This question is
open. No implementation, workflow change, experiment or restart is authorized
by recording this approach.

## Countermeasure for threads 01–02: checkpoint connected

Implemented on live Qubigo `main`: `careful-plan` now routes each unfinished
capability through `design_walkthrough` before detailed revision/review. The
machine creates `<exact plan path>.discussion.md` and blocks for the owner's
journey/event/policy discussion and decisions on added guarantees or dependencies.
The shared `discuss-factory-design` skill picks up that document from Libby's
root. Revision and reviewers block again for newly unagreed design choices;
parallel reviewers keep questions in their own reports for the interactive
session to bring into the discussion. An unchanged resolved scope can proceed
on an explicitly requested rerun; a new capability requires its own discussion.

See [workflow and verification limits](../../../qubigo/.assembly-lines/careful-plan.md)
and [interactive skill](../../../.agents/skills/discuss-factory-design/SKILL.md).
The graph passes the factory's `--check-line` validation; the skill validator
passes. The factory test command also completed successfully (full non-real-agent
suite: 1966 steps passed; the requested name filter did not limit execution).
Owner-decision recognition still relies on machine instructions, not a
factory parser. A real planning-only conversation/rerun trial remains pending;
installation is not evidence of effectiveness. No application implementation
or factory run was launched. Qubigo main also received the two documentation-only
investigation commits; unfinished competition code stays on its feature branch.

## Countermeasure for thread 03: explicit domain design and application

On Qubigo main, [ADR 0007](../../../q/qubigo-backend/docs/adr/0007-domain-primitives-and-validated-domain-inputs.md)
records Domain Primitives and controller-boundary conversion to clean typed inputs.
[ADR 0008](../../../q/qubigo-backend/docs/adr/0008-immutable-aggregates-with-behavior.md)
records immutable aggregate behavior; both reference command decisions 0002/0005. Backend guidance
and the architecture overview link it. Walkthrough/revision, implementation and
review instructions explicitly read the relevant ADRs and trace a changed path
through those responsibilities. Source links require the live target checkout.
The [kaizen note](../../../qubigo/docs/kaizen/2026-10-18-domain-design-intent-missing-and-documented-responsibilities-not-upheld.md)
records implementation and the pending effectiveness check; ledger status is Open.
No product code changed. Documentation links/ledger consistency and whitespace
were checked; machine conformance still needs observation in a later increment.
