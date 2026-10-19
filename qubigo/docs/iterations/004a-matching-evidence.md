# 004a / 3c — arranged invitations and regular-game acceptance

## Status and boundary

Implemented for review; **task 3c remains open for planner acceptance and the factory commit**. No commit made by the doer. This replaces the earlier domain/storage-only status: start and late join now have production arrangement callers, and their persisted invitations are usable through the existing invitation journey.

Completion-triggered matching, scoring, game-screen/list labels, authoritative closure/results, reminders and physical platform evidence remain deferred. No scheduler, broker, expiry/re-pairing, extra allowance or two-player readiness workflow was introduced.

## Delivered behavior and responsibility trace

Read backend AGENTS, architecture/domain-module guidance, ADRs 0002/0005/0007/0008, the responsibility lens, frontend AGENTS, seed/full specification and the exact external plan.

The Games trace remains: `GameDto.asValidMoveParameters` constructs typed inputs with field-keyed enum/conversion failures; `GamesController.postMakeMove` invokes only after conversion succeeds; `MakeMove` loads/co-ordinates; immutable `Game.move` owns turn/occupation/piece rules; `SqliteBasedGameRepository` reconstructs and appends journal events. Existing Position construction permits out-of-board values; Game rejects those. This work does not copy that construction gap into a new input type or rewrite unrelated primitives.

Changed path:

- `StartCompetition` and `JoinCompetition` require the arrangement collaborator and call it inside their existing protected read/decide/save interval. No production no-op fallback. Opening, viewing and pre-start joining do not arrange games.
- `Competition.arrangeMatches` owns eligibility, deadline, allowance accounting, disjoint reservations, random candidate ordering, new-opponent preference, repeat fallback and first/second balance. It returns immutable state. Completed game status is supplied as a fact by the application collaborator when start/late join runs; finishing a game does not itself trigger arrangement in this increment.
- Each new reservation gets a generated invitation identity. Ordered SQLite history retains direction, invitation ID and optional game ID; invitation identity is unique. Nullable IDs allow reconstruction of historical fixtures/rows; all newly arranged production reservations receive IDs.
- `CompetitionMatchArrangement` coordinates existing Users name lookup and Invitations storage, without moving pairing rules into either adapter or Invitations. Both places are reserved before either player can acquire another competition match. Unrelated ordinary games do not occupy these slots.
- Invitations carries stored source identity/name as presentation context only. Explicit DTOs expose that context in existing list/detail responses. Ordinary creation accepts only the existing invitee input and cannot submit a competition reservation or source identity.
- The existing direct `PUT /api/invitations/{id}` acceptance command routes arranged invitations through the narrow `ArrangedInvitationAcceptance` port. Its Competition-side application collaborator reloads the invitation inside the shared gate, resolves the stored competition/match identity and reserved players, delegates invitation lifecycle to Invitation and deadline/authorized reservation conversion to `Competition.associateGame`, constructs a regular game directly through `Game.createRegularGameFrom`, then stores all outcomes. This is source-specific coordination, not delegation through `CreateGame` for the new path. Invitations contains no competition eligibility/allowance/deadline rules. The unrelated ordinary command path is preserved.
- `GameRepositoryWithView` now propagates a returned view-save failure instead of discarding it. This was exposed by the new atomic acceptance test; the surrounding Competition gate can therefore roll back the journal, list, invitation and association together.
- Competition deadlines and identities are resolved from storage, not caller-supplied labels/context. No new submitted fields or conversion boundary was added. Reused ID construction rejects empty/non-UUID-shaped values; competition name/count primitives retain their construction checks. `CompetitionMatch` rejects self-pairing. State-dependent rules remain Competition behavior.

## Acceptance evidence map

**A–C: arrangement and usable invitations**

- `CompetitionMatchingTest`: all available disjoint pairs, odd/lone waiting, busy pending/active slots, exhaustion, strict deadline, before-start exclusion, new-opponent preference, repeat/direction balance and controlled random alternatives.
- `CompetitionMatchTriggersTest`: start supplies the selected first mover's stored invitation, late join supplies a lone member's match, repeated start retains the invitation, and late entry supplies completed-game facts without implementing a completion policy.
- `SqliteBasedCompetitionRepositoryTest`: ordered direction/game/invitation history survives clearing the persistence context.
- `SqliteCompetitionPlayTest`: start supplies a reloadable invitation without a game; an unfinished ordinary game does not block arrangement; competing late joins cannot reserve the waiting member twice. SQLite may honestly refuse a competing writer; no automatic retry or guarantee that both joins succeed is claimed.
- Existing invitation-list component and adapter tests cover `Competition`, source name, opponent, start action and text-only names. The reserved opponent has a waiting entry rather than an actionable accept entry. Running competition pages now direct members to Invitations in Games instead of claiming matching is unavailable.

**D–E: authorized conversion and regular play**

- `CompetitionAcceptanceTest`: selected first mover converts a single allowance-1 reservation to its game without adding a place; opponent/unknown/consumed invitations and exact/after-deadline conversion are refused. The original aggregate is retained.
- `CompetitionInvitationAcceptanceTest`: actual aggregate construction, selected first turn, regular game type, accepted invitation and associated reservation; returned game-storage failure is not success.
- `CompetitionInvitationAcceptanceControllerTest`: real acceptance coordination behind the existing direct endpoint, selected first mover success, reserved opponent/unrelated signed-in caller refusal, anonymous rejection and exact/after-deadline refusal.
- `AcceptInvitationTest`: ordinary acceptance cannot bypass competition coordination for an arranged invitation; existing ordinary acceptance tests remain passing.
- `SqliteCompetitionPlayTest`: accepted invitation, association and regular game survive fresh repositories. Creation at 14:00Z and the actual first move at 14:02Z are separate stored journal timestamps, preserving the 120-second interval. The normal regular-game factory and its initial piece/distribution/full-board semantics are reused, not an empty-board variant or a hardcoded player-event count.

**F: atomicity and overlap**

`SqliteCompetitionPlayTest` uses the real Flyway/SQLite/JPA adapters and `SqliteCompetitionStartGate`, not only in-memory repositories:

- Returned failure after writing the journal/list rolls acceptance back.
- Returned game-list failure rolls the journal back (this test originally failed).
- Returned association-save failure after invitation/game writes rolls all outcomes back.
- Thrown invitation-write failure after its real save rolls invitation/game back.
- Returned reservation-save failure rolls arrangement/invitation writes back.
- Thrown arrangement-invitation failure leaves neither an orphan invitation nor changed competition.
- Each rollback assertion reads Competition, Invitation and Games through newly constructed repositories; acceptance rollback also checks the persistent game list is empty.
- Competing accepts yield at most one consumed reservation/game; competing late joins retain mutually consistent membership and one reservation for the waiting member.
- Deterministically paused JOIN/OPEN/EDIT requests overlapping acceptance preserve STARTED, membership and association. `PendingCompetitionMutation` is extracted to test fixtures and reused by the existing start-overlap tests rather than copying its latch/repository helper. Latches/barriers create the interleaving; no sleeps or polling.

**G: presentation and lifecycle**

`competition-invitation.test.js` uses the actual Router and page/module collaboration. Separate examples cover source/opponent text, selected first mover action, reserved-opponent waiting, game navigation and honest acceptance failure. Deferred load and acceptance success/failure cannot replace Profile, navigate away from it or corrupt a newer invitation opening/error state. No manual close substitutes for Router behavior. Ordinary invitation create/list/accept/page tests remain passing. No cancellation guarantee is claimed.

## Test-first record

Inherited domain/storage increment (previous attempt): new arrangement API first failed compilation; four-person/repeated arrangement tests then failed because only one pair was arranged and reserved players were reused; completed-history examples drove supplied facts/history accounting. Repository round-trip failed because matches reconstructed empty. Those behaviors and storage were implemented before this continuation. Controlled first-index/last-index random collaborators replaced an unsuitable same-seed-pair comparison. The earlier full checks were 872 servlet/18 CLI and 830 frontend passing/11 skipped.

This continuation, commands from `qubigo-backend` unless noted:

1. `./gradlew :qubigo-servlet:test --tests '*CompetitionAcceptanceTest'`: failed compilation for missing invitation identity/association behavior. Added immutable authorized/deadline-checked conversion; all 6 examples passed.
2. `... --tests '*SqliteBasedCompetitionRepositoryTest'`: 12 tests, 1 failure because invitation identity reconstructed as null. Added mapping and additive migration; passed with the acceptance tests.
3. `... --tests '*CompetitionInvitationAcceptanceTest'`: failed compilation for missing coordination/context. Added the source-specific application collaborator with actual Game/Invitation/Competition behavior and failure propagation; passed. Later refactored behind the explicit Invitations port with tests green.
4. `... --tests '*invitations.domain.commands.AcceptInvitationTest'`: new bypass regression failed because ordinary creation was used. Added stored-context dispatch with a fail-closed default; passed. Production wiring supplies the source-specific collaborator.
5. `... --tests '*SqliteBasedInvitationRepositoryTest' --tests '*invitations.domain.commands.AcceptInvitationTest'`: 16 tests, 1 failure because source context was lost. Persisted/map/reconstructed source identity/name; passed.
6. `... --tests '*CompetitionMatchTriggersTest'`: failed compilation for missing arrangement collaboration/constructor dependencies. Added required start/join collaboration and stored invitation creation. Existing matching comparison was adapted to compare direction rather than newly generated invitation identity. Focused trigger/matching/acceptance/context tests passed. The later completed-game collaborator example also first failed compilation, then passed after supplying stored game facts.
7. Atomic adapter examples initially passed as regressions of the chosen transaction boundary. The first competing-join expectation that *both* writers must succeed failed with SQLite shared-cache contention; changed it to verify the promised invariant and consistency of successful membership, not invent an automatic-retry guarantee.
8. `... --tests '*SqliteCompetitionPlayTest'`: adding returned game-list-failure injection failed because `GameRepositoryWithView` discarded the failure. Propagated it; all atomic examples passed. Added direct-route identity/deadline, timestamp and stale-write regressions; all passed.
9. `... --tests '*Invitation*DtoConversionTest'`: failed compilation for missing explicit context DTOs. Added list/detail conversion; passed in the full suite.
10. Frontend `npm test -- --runInBand test/invitations/components/invitation-list-component.test.js`: new source presentation test failed (`Competition` absent, opponent parsed as markup). Added context presentation with text-only names; passed.
11. Frontend adapter test: 2 failures because list/detail context was dropped. Preserved context in both domain mappings; passed.
12. Frontend actual-Router tests: after supplying the existing throbber component fixture, 6 of 9 examples failed for missing presentation/action/refusal state and stale navigation/error handling. Added per-opening module/page guards, profile-aware action and honest failure display; all passed. Added later/new-opening regressions and separated presentation/navigation behaviors; passing.
13. Updated running-status expectations first produced 2 frontend failures because pages still said no matches were available. Added accurate invitation guidance and the existing Games-list link; passed.
14. Initial full backend run exposed ambiguous GameRepository bean selection and subsequent test-JVM memory failures. Selected the existing `gameRepository` bean by the established wiring convention; subsequent complete runs pass.

## Final verification

- `cd qubigo-backend && ./gradlew test`: **passed**. Current XML totals: **911 servlet tests, 18 CLI tests**, zero failures/errors/skips.
- `cd qubigo-frontend && npm test -- --runInBand`: **845 passed, 11 skipped**; 72 suites passed, 1 skipped.
- `cd qubigo-frontend && npm run build`: **passed**.
- `git diff --check`: **passed**.

## Continuation verification

Inspected the existing uncommitted 3c implementation directly using the scoped HEAD diff and all 16 untracked product/evidence files. No previous validator findings were supplied and no additional defect was identified; production behavior and tests were preserved rather than reimplemented. Rechecked the Games input/command/aggregate/storage trace and the changed Competition-owned matching/association path against the required ADRs.

Observed commands in this continuation:

- Backend `./gradlew test`: exit 0, test tasks up-to-date; then `./gradlew test --rerun-tasks`: exit 0, all tasks executed. XML under `qubigo-backend/build-reports/*/test-results/test` reports 911 servlet and 18 CLI tests, no failures/errors/skips.
- Frontend `npm test -- --runInBand`: exit 0, 845 passed and 11 skipped.
- Frontend `npm run build`: exit 0.
- Scoped `git diff --check`: exit 0.

This continuation updates verification evidence only; the earlier test-first record above remains the record driving the inherited implementation. Task 3c stays open for independent acceptance and factory commit.

These are automated local application/storage results, not physical mail, native OS, camera, store-install or push attestation. Those later readiness inputs are neither supplied nor required for 3c. Task 3c itself is not marked done; planner/factory acceptance and commit remain outstanding.
