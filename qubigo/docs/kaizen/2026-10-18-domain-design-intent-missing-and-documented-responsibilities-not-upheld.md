# Problem: domain design intent is partly missing and documented responsibilities are not upheld

Date: 2026-10-18

## Context

Collaborative investigation with Rob of `qubigo-004`, line `careful-plan`.
Live branch `feature/004-competitions`, preserved implementation `221d7bb3`.
Comparison uses Games on local main `df120f51`; inspection only, no tests run.
This note records findings agreed in conversation, not a completed root-cause
analysis or an implemented countermeasure.

## Expected standard

Rob clarified the intended design:

- Domain Primitives for primitive values in the domain, following the semantics
  of *Secure by Design* by Dan Bergh Johnsson, Daniel Deogun and Daniel Sawano.
- Immutable aggregates with behavior: functional state transitions expressed
  through objects, without domain I/O.
- Controller-side DTO mapping and validation construct clean, typed parameter
  objects before entering the domain. Input validity and state-dependent business
  rules are distinct responsibilities; aggregates own the latter.
- Commands fetch needed state, delegate behavior to aggregates/domain objects,
  then persist through ports. Persistence adapters do not take over the use case.

Not all of this was already explicit in documentation. The relevant written
standards are [backend AGENTS.md](../../qubigo-backend/AGENTS.md),
[ADR 0002](../../qubigo-backend/docs/adr/0002-command-classes-between-controllers-and-aggregates.md)
and [ADR 0005](../../qubigo-backend/docs/adr/0005-commands-delegate-to-aggregates-not-sibling-commands.md).
Source links require the live repository, not the sharing snapshot.

## What happened

Rob identified that the new competition flow does not consistently preserve the
established Games flow. Main's `GamesController.postMakeMove` converts a DTO via
`asValidMoveParameters` and `validateParameters` into typed `Position`/`Piece`
parameters. `MakeMove` coordinates loading, `Game.move`, persistence and
notification; `Game.move` owns turn, position and piece rules.

In the competition implementation:

- `CompetitionDraftController` takes raw `JsonNode`; `asDraftFields` manually
  checks JSON types. `DraftFields` retains raw name/allowance/minimum values,
  which can still be invalid; validation happens later. Edit revision validation
  is separately inlined in the controller.
- Draft creation/edit do delegate to `Competition.draft` / `edit` and save their
  results. The architecture is not wholly absent.
- Publication delegates to `repository.publishDraft(id, transition)`. Its SQL
  adapter controls loading and transition execution and enforces the rule that
  another competition must not already be open.
- `StartCompetition` is a wrapper around `assignments.start(id, clock)`; the SQL
  adapter loads state, invokes domain behavior, persists it and coordinates
  matching/invitation creation. Joining similarly delegates orchestration to a
  persistence adapter.

Packages and command names resemble the expected structure, while some
responsibilities have moved into SQL adapters.

## Evidence: documentation coverage

- `qubigo-backend/AGENTS.md:38` explicitly requires immutable domain data classes
  with behavior, pure transitions and persistence in commands.
- ADR 0002's Decision specifies fetch → domain mutation → save. ADR 0005's
  Decision reinforces coordinate → delegate to aggregate/domain function → save.
- Searches of backend docs, ADRs and backend AGENTS.md found no explicit Domain
  Primitives / Secure by Design rule, nor an explanation of `validateParameters`
  or the clean-domain-input boundary. Thin controllers and DTO conversion are
  documented, but do not fully state Rob's intended validation semantics.
- Local Libby `factory/src/main/kotlin/MachineRunner.kt:107–109` disables automatic
  repository context loading (`project_doc_max_bytes=0` for Codex,
  `--no-context-files` for pi); machine instructions are supplied separately.
- Current doer/RDD-validator instructions explicitly reference backend guidance
  for test design but do not similarly direct architecture checks to those ADRs.
- Local `factory/runs/qubigo-004/plan.md:28` claims the backend guidance and
  architecture/domain-module documents were read. The output record does not
  establish which passages were read or understood by each invocation.

## Impact and what allowed it to happen

Quality and maintainability risk: primitive-value validation and use-case
ownership diverge from the application's intended design, making the new flow
harder to reason about and test in the established way.

Two distinct gaps are supported: some intent was insufficiently documented;
some responsibilities were documented but not consistently upheld. It is not
established that machines simply failed to find the documents. Adding prose
alone cannot be assumed to fix the second gap. The exact discovery, application
and review failure mechanisms remain open.

## Open questions

- How do machines select and apply the relevant architectural decisions, and
  how does review distinguish a command wrapper from actual use-case ownership?
- Did transaction prescriptions displace the command/aggregate collaboration?
  The sequence is suggestive, but this causal link needs further investigation.
- What concise examples and explicit statements would capture Domain Primitives,
  clean input conversion and immutable behavior without an exhaustive rulebook?
- How would a collaborative event/policy walkthrough expose these departures
  before implementation, and what observation would demonstrate improvement?

## Possible prevention ideas

Document the missing design intent with the established Games example, and
separately examine how the factory applies already documented responsibilities.
The proposed collaborative event-storming/responsibility walkthrough may expose
boundary changes early. These are candidates, not fixes or active experiments.
