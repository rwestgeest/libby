# 004a — organizer participant view (3a)

Status: implemented; awaiting acceptance and factory commit. Task 3a remains open.

## Delivered boundary

Any signed-in ADMIN can open a saved competition management URL and choose
**View participants**. The read-only view displays existing player names and the
total count, or an explicit zero/empty state for a draft or open competition.
Revisiting and choosing the view again retrieves membership anew. Names are
inserted with `textContent`, never interpreted as markup.

`GET /api/admin/competitions/{id}/participants` returns only
`{count, participants: [{id, name}]}`. Existing backend ADMIN path protection
covers the endpoint; direct regular-user and anonymous requests are rejected.
There is no creator-only check. Participant preview/joined-list DTOs remain
unchanged and disclose no rosters.

A missing competition or failed member lookup fails the read rather than
silently omitting someone or reporting zero. The page shows unavailability,
without a count/list, and advises retry by revisit. Late success/failure cannot
replace another route or another competition's roster. No live refresh,
concurrency guarantee, roster editing, start/play, or new integration machinery
is included. Physical platform evidence remains deferred to task 6.

## Responsibility trace

Read root/service instructions, responsibility lens, the exact seed/plan and
resolved discussion, full iteration specification, and backend ADRs
0002/0005/0007/0008. Traced current Games from `GameDto.asValidMoveParameters`
(field-specific construction of Position/Piece), through controller success-only
command invocation and `MakeMove` coordination, to immutable `Game.move` /
`withMove` rules and journal reconstruction/storage in
`SqliteBasedGameRepository`.

This increment is a query, not a mutation. Thin `CompetitionsController` delegates
to `CompetitionParticipants`, which retrieves Competition-owned membership via
`CompetitionRepository.byId` and resolves each member through the existing Users
`UserRepository.byId` contract. The query returns immutable, narrow participant
values using existing ID/Name types; the controller explicitly maps those to
serializable presentation DTOs. A failed name lookup fails the whole result with
a generic unavailable message. Count is derived from the complete resolved list.
No new submitted settings, primitive-construction rules, state-dependent rules,
aggregate transitions, repository protocol or SQL business logic are introduced.
Existing immutable Competition joining/opening/editing behavior remains untouched.

Frontend repository transport and draft-module collaboration retrieve the roster
without mutating draft/error state. The page owns presentation and checks both
opening identity and whether its roster section is still connected before
applying asynchronous results. The saved management screen provides the control;
regular users cannot load that screen or trigger its roster request.

## Test-first record

Commands were run from the respective service directories.

1. `./gradlew :qubigo-servlet:test --tests '*CompetitionParticipantsTest'`
   initially failed compilation because the new query/result types did not yet
   exist. After introducing a minimal empty-query scaffold, the same command ran
   four tests: three failed (enrolled names, missing competition, unresolved
   member); the empty-draft example passed. Implemented membership resolution,
   missing/failure propagation and all-or-nothing lookup.
2. Added controller examples and extended existing authorization parameterization.
   `./gradlew :qubigo-servlet:test --tests '*CompetitionParticipantsTest' --tests '*CompetitionsControllerTest'`
   ran 23 tests: two controller failures for the not-yet-delivered roster route;
   all query examples passed. Added route, narrow DTO mapping and bean wiring;
   the same focused command then exited 0.
3. `npm test -- --runInBand test/competitions/pages/competition-participants.test.js`
   ran 12 examples: 11 failed because `page.showParticipants` did not exist;
   the regular-user denial example already passed. Added the minimal management
   control, transport, loading/presentation/failure behavior and stale-result
   guards. Focused roster + existing draft-page checks then passed (42 tests).
4. Refactored the query's typed fold initialization and reused `PageThatRenders`
   for the navigation fixture. Strengthened existing SQLite membership
   reconstruction coverage to include two members and a different competition's
   unrelated member. That is coverage of already-working storage, not a claimed
   adapter behavior change or fabricated red test.

## Acceptance coverage

- Names/count and read-only outcome: `CompetitionParticipantsTest`, narrow/exact
  controller response-shape assertion, and frontend roster presentation example.
  Unconfigured unrelated user lookups would fail the query tests rather than
  silently include a previewer/organizer. Existing `JoinCompetitionTest` guards
  explicit immutable membership and idempotent repeats; existing
  `CompetitionParticipationTest` guards preview without enrollment. These were
  reused rather than duplicating join behavior in roster tests.
- Persistence/revisit: strengthened
  `SqliteBasedCompetitionRepositoryTest` checks reconstruction of exactly two
  members while excluding another competition's membership; a separate frontend
  revisit example verifies a new roster request and presentation after reopening.
- Empty/competition-specific: query empty-draft example; frontend empty draft
  and open examples and selected-competition isolation; SQLite reconstruction.
- Privacy/authorization: another-admin controller example checks the exact
  response contains only count/participant IDs/names; direct regular/anonymous
  roster requests are included in the existing security parameterization.
  Existing participation controller regressions verify preview/list omit rosters.
  Frontend regular-user check verifies no management control/request.
- Honest failure/lifecycle: missing-competition and unresolved-member query
  examples, controller failed-read response, frontend unavailable state without
  count/list, text-not-markup regression, and real-Router deferred success/failure
  regressions for Profile navigation and opening another competition.

## Final verification

- `cd qubigo-backend && ./gradlew test` — exit 0; 809 servlet tests (94 competition),
  18 CLI tests; zero failures/errors/skips. Counts taken from current
  `build/reports/{qubigo-servlet,qubigo-cli}/test-results/test/TEST-*.xml`.
- `cd qubigo-frontend && npm test -- --runInBand` — exit 0; 813 passed, 11 skipped;
  71 suites passed, one skipped.
- `cd qubigo-frontend && npm run build` — exit 0.
- `git diff --check` — exit 0.

No commits made. The pre-existing change to
`.assembly-lines/.machines/design_walkthrough/config.json` was left untouched.
No device, store, email-delivery or production-release evidence is claimed.
