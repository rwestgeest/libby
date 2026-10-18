# Qubigo 004a — competition run brief

This is the seed for a fresh run of iteration 004, not a new product iteration.
The [iteration-004 specification](004-competitions.md) remains the full product
requirement. Read both documents. This brief defines how we begin; it does not
waive later requirements or claim iteration completion.

## First releasable capability

An admin can create a competition and join it. Joining records membership.
Keep the emerging feature admin-only (including backend access) so we can release
this limited capability without exposing an unfinished journey to other players.
Public enrollment remains part of the eventual product, not this increment.

Use the specification's relevant creation fields and rules. Discuss the minimal
creation/joining journey before implementation. Games, invitations, matchmaking,
move processing, standings, drafting and a later start action are deferred from
this first capability; keep them visible in the remaining journey outline.

## Planning and implementation

- Start with the `careful-plan` design checkpoint. Walk through the journey,
  domain events, reacting policies and owning bounded contexts with us. Surface
  new dependencies and guarantees beyond the specification, with reason and
  cost. Do not turn speculative failures into mandatory machinery.
- Detail only the next capability: actor, outcome, relevant rules, acceptance
  examples and deferrals. Keep later capabilities brief; their sequence may
  change as we learn.
- Follow backend guidance and ADRs [0002](../../qubigo-backend/docs/adr/0002-command-classes-between-controllers-and-aggregates.md),
  [0005](../../qubigo-backend/docs/adr/0005-commands-delegate-to-aggregates-not-sibling-commands.md),
  [0007](../../qubigo-backend/docs/adr/0007-domain-primitives-and-validated-domain-inputs.md)
  and [0008](../../qubigo-backend/docs/adr/0008-immutable-aggregates-with-behavior.md).
  Test-drive bounded behavior; let collaborators and repository ports emerge.
  Outside-in and inside-out are both appropriate.
- Later, a likely next capability is one game updating the competition through
  an event policy. Games need not know about Competitions. This is a direction
  for discussion, not a prescribed implementation or fixed next task.

## Run boundaries and learning

Inspect main's actual baseline before planning; reuse suitable existing work
without merging the unfinished iteration-004 branch. Use a fresh working branch
and run `qubigo-004a`; preserve the original run and competition branch.

After accepted work, continue routine tasks within the recorded decisions.
Return to discussion only for new scope, dependency or guarantee decisions.
Keep all unimplemented requirements open. A released increment is not completion
of iteration 004. Use this attempt to observe whether the kaizen adjustments
produce collaborative decisions, clean domain design and test-driven increments.

Creating this seed does not launch or resume a run.
