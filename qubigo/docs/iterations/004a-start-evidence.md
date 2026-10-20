# 004a — organizer manual start (3b)

Status: implemented; awaiting acceptance and factory commit. Task 3b remains open.

## Delivered boundary

Any existing ADMIN can explicitly start an open competition through management
and `POST /api/admin/competitions/{id}/start`, including another organizer's
competition. Starting is separate from opening and joining. Zero or one member,
and membership below the suggested minimum, are permitted. Drafts, missing
competitions and first starts at/after closing are refused. Repeating a still-
running start succeeds unchanged without another save.

The persisted `STARTED` distinction survives reload, permitted edits, opening
again and late joining. Deadline/games remain fixed; name/minimum remain editable.
Regular users can preview/join during running, but cannot start or manage.
Existing membership remains accessible after closing; new membership is refused
at/after that instant.

Only one competition can occupy the running interval. A prior started competition
ceases to occupy it exactly at its configured deadline, without a scheduler or
result finalization. Management and participant DTOs retain `status: STARTED` and
provide server-derived `running`; revisit screens show running/started or
closing-time-reached honestly. They explicitly say matches/results are not yet
available. Starting creates no games, invitations or results and enrolls nobody.
Sharing remains available during running. No automatic refresh is promised.

## Responsibility trace

Read the exact seed, full specification, supplied plan/discussion, root and both
service guides, responsibility lens, backend architecture/domain-module guidance,
and ADRs 0002/0005/0007/0008. Traced current Games from
`GameDto.asValidMoveParameters` field-specific typed construction, through
success-only controller invocation and `MakeMove` coordination, to immutable
`Game.move` / `withMove` rules and journal reconstruction/storage in
`SqliteBasedGameRepository`.

Start submits no settings/new primitive values. Existing settings still pass
through `asValidCompetitionParameters` into intrinsically valid CompetitionName,
GamesPerParticipant, SuggestedMinimumParticipants and an unambiguous Instant;
invalid fields never invoke edit/create commands. No input rule changes.

`StartCompetition` coordinates an exclusive start interval, reads the selected
aggregate and stored started competitions, supplies time to `Competition.start`,
and saves only a changed successful outcome. `Competition` owns draft/deadline,
advisory-minimum and single-running eligibility, plus immutable joining/editing/
opening behavior. Other competitions answer `isRunningAt(now)` using their own
state. No sibling command invocation or SQL-owned business use case.

`CompetitionRepository.started` retrieves/maps facts, including ended intervals.
`CompetitionStartGate` is a narrow capability-specific transaction port, not a
general locking platform. Start, join, open and edit all use it around their
complete aggregate read/decide/save interval, so a concurrent settings or
membership save cannot overwrite STARTED with a pre-start snapshot. Creation
only inserts a fresh DRAFT identity and does not affect running eligibility. Its SQLite adapter reserves the database writer by
updating one migration-created lock row **before reading eligibility**, then
executes the command's read/decide/save callback in the same transaction. Failed
outcomes roll back; transaction/commit exceptions become failures. Separate
requests therefore cannot both commit competing starts from an earlier snapshot.
It contains no eligibility/time/minimum rules. The existing status column stores
the new enum value without a new nullable field/backfill.

Frontend transport/module/page collaboration reuses opening-session guards and
the management state-action presentation. Actual-Router deferred-result tests
cover both leaving the page and opening another competition, for success and
failure. Existing join/list/navigation coverage remains in place.

## Test-first record

Commands below were run from the relevant service directory. Initial red logs
were kept in `/tmp/004a-start-*.log` during this work; this document records the
observed results, not a claim that all tests were added individually.

1. `./gradlew :qubigo-servlet:test --tests '*StartCompetitionTest'` first failed
   compilation for missing start/status behavior. With a minimal `Success(this)`
   start scaffold and STARTED enum, it ran 15 examples, 13 failing: no transition,
   draft/deadline/other-running refusal, started opening/joining/preview/fixed
   settings. Implemented immutable rules, then added command coordination tests.
2. `./gradlew :qubigo-servlet:test --tests '*StartCompetition*Test'` failed
   compilation for the missing command, start gate and started-facts port. Added
   coordination and those narrow contracts. Storage test compilation then failed
   for the not-yet-existing SQLite gate. Added the transaction adapter and lock
   migration. `./gradlew :qubigo-servlet:test --tests '*StartCompetition*Test'
   --tests '*SqliteCompetitionStartGateTest'` exited 0: domain/command plus real
   SQLite start round-trip, later-request refusal, exact-deadline handoff and
   competing requests. The competing-request example starts two worker requests
   at a barrier and asserts exactly one success and exactly one stored start;
   it uses no sleeps, polling or JVM synchronization as the production guarantee.
3. `./gradlew :qubigo-servlet:test --tests '*CompetitionsControllerTest'` ran
   23 examples, two failing for the missing start route. Added thin endpoint and
   production bean. Existing backend ADMIN path security rejected both regular
   and anonymous start callers even before route delivery. Added deterministic
   DTO deadline examples; their first compile failed for missing running/time
   mapping, then implemented both management and participant presentation maps.
4. `npm test -- --runInBand test/competitions/pages/competition-draft-page.test.js`
   ran 41 examples, 10 failing for missing start collaboration/presentation and
   editable started deadline. Added transport/module/start control, guarded
   state-action reuse and STARTED fixed-field/status handling.
5. `npm test -- --runInBand test/competitions/pages/competition-draft-page.test.js
   test/competitions/pages/competition-page.test.js` then ran 61 examples, four
   failing only for participant started/late-join/closed presentation; management
   passed. Implemented participant status presentation. Focused competition
   checks then passed (121 frontend examples at that point; backend competition
   selection exited 0).
6. Extended existing deadline/repeat-membership, SQLite reconstruction/editing,
   share-QR and permitted-edit UI examples to STARTED rather than copying join
   suites. Added persisted late joining coverage. These were already-working
   regression coverage, not new red behavior. Split management/participant DTO
   deadline examples for glanceable separate presentation contracts.

## Acceptance coverage

- A: pure start examples cover zero/one/two members below minimum four, exact
  preserved aggregate, immutable original; command/save and SQLite round-trip;
  separate UI immediate confirmation, management reload and participant revisit.
- B: draft/deadline domain refusal, missing and storage-failure coordination,
  unchanged/no-save repeated start, opening preserving STARTED, honest endpoint
  and management error handling. No game collaborators exist in this command.
- C: another running competition refusal, exact previous-deadline handoff;
  real SQLite independent-request and competing-request tests. Both DTOs check
  before/exactly/after closing rather than pretending finalization was delivered.
- D: started preview/join/domain deadline and repeat-membership examples;
  reconstruction of started members/settings and persisted late joining;
  frontend late entry, retained edit state and share URL/QR. Existing joined-list
  routing and caller-specific membership tests are reused.
- E: another-admin endpoint example; direct regular/anonymous denial with no
  command invocation; regular UI cannot invoke start; actual-Router deferred
  success/failure cannot overwrite Profile or a different competition's state.

## Validator regression — stale aggregate writes

The supplied finding was reproduced before production changes using
`./gradlew :qubigo-servlet:test --tests '*SqliteCompetitionStartGateTest'`:
7 examples ran, 3 failed (JOIN, OPEN, EDIT at the persisted status assertion).
The deterministic interleaving pauses an unprotected mutation after reading
OPEN, commits start in a separate request, then resumes the stale save. For a
protected mutation it pauses before gate acquisition instead, allowing start
to commit before the mutation reads. Both schedules use latches, not sleeps.
The gate decorator delegates to the real SQLite gate; the repository decorator
only controls the read schedule, and storage remains the real SQLite adapter.

Added the required gate dependency to join/open/edit and their production beans;
no no-op production default is available. Existing unit tests explicitly supply
an immediate gate. Aggregate rules and validated input conversion remain
unchanged: commands still coordinate facts, immutable Competition behavior
owns state rules, and the gate owns only the protected transaction boundary.

`./gradlew :qubigo-servlet:test --tests '*competitions*'` then exited 0,
including all three interleaved regressions and competing starts. Full checks
were rerun afterward as recorded below. Task 3b remains open for acceptance
and factory commit.

## Verification

- `cd qubigo-backend && ./gradlew test` — final rerun exit 0; 854 servlet tests
  including 139 competition tests, 18 CLI; zero failures/errors/skips. Counts
  read from current `build/reports/{qubigo-servlet,qubigo-cli}/test-results/test`
  XML reports. The earlier full run before the DTO test split also passed
  (848 servlet / 133 competition / 18 CLI).
- `cd qubigo-frontend && npm test -- --runInBand` — exit 0; 830 passed,
  11 skipped; 71 suites passed, one skipped.
- `cd qubigo-frontend && npm run build` — exit 0.
- `git diff --check` — exit 0.

No commits made. Existing `.assembly-lines/.machines/design_walkthrough/config.json`
modification left untouched. Matching/game creation, closure scoring/results,
notifications and all later journey tasks remain deferred. No physical-device,
store, email or production-release evidence is claimed; task 6 remains open.
