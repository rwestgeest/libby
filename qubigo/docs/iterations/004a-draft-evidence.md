# 004a — complete competition draft (increment 1a)

Implemented; awaiting independent review, factory commit and planner acceptance.
No editing, opening, joining, listing, game creation or participation is implemented.
No native-device evidence is required or claimed for this increment.

## Delivered journey

An existing admin opens **Profile → Create competition draft**, enters all four
settings and saves. The saved URL displays the trimmed name, counts, local closing
time with device timezone, and a draft-only explanation. Bookmark/copy that URL to
revisit after navigation/reload or share with another organizer. There is no draft
listing in this increment. Anonymous and regular users cannot access either API
entry point, and regular users have no management link or form.

API: `POST /api/admin/competitions`, `GET /api/admin/competitions/{id}`.
Counts accept whole-number decimal transport values (numbers or strings), use
signed 64-bit storage, and are returned as strings to retain precision in JS.
There is no business maximum. Closing time requires an explicit offset in input;
storage/output use an instant. Past deadlines are valid for drafts.

## Responsibility trace

`CreateCompetitionDto` → `validateParameters` constructs `CompetitionName`,
`Instant`, `GamesPerParticipant`, `SuggestedMinimumParticipants` → complete typed
`CompetitionSettings` → `CreateCompetition` obtains identity and delegates to
`Competition.draft` → repository stores the immutable outcome. Read uses
`GetCompetition`; SQLite reconstructs primitives, settings and draft status.
Conversion failures are field-specific and never invoke creation. Persistence
failure propagates as failure, not success. Competition has no Games, Invitations
or enrollment collaborator. No future state transitions/rules are implemented.

The existing Games path was traced before changing work: `asValidMoveParameters`
constructs typed inputs; `GamesController` maps conversion success to `MakeMove`;
that command loads facts and delegates to immutable `Game.move`/`withMove`; the
SQLite adapter reconstructs/appends events. Draft persistence does not require
copying Games' journal representation.

## Test-first record

Commands below run in `qubigo-backend/`, except npm commands in `qubigo-frontend/`.
Each focused backend command was `./gradlew :qubigo-servlet:test --tests '<pattern>'`.

| Behavior / pattern | Observed initial failure | Subsequent result |
| --- | --- | --- |
| `*CompetitionNameTest` | Missing `CompetitionName` | Pass: trim, ASCII restriction, single letter |
| `*CompetitionConversionTest` | Missing DTO/converter | Pass: complete inputs, all missing fields, fractional/too-small counts, malformed deadline, invalid name, large whole counts |
| `*CreateCompetitionTest` | Missing command/aggregate/repository | Pass: past deadline produces draft; storage failure propagates |
| `*CompetitionsControllerTest` | Missing controller | Pass: admin creation/read, another organizer's read, no creation on invalid input, both endpoints denied to regular/anonymous callers |
| `*SqliteBasedCompetitionRepositoryTest` | Missing persistent adapter | Pass against Flyway SQLite: clear persistence context and reconstruct settings, past deadline, duplicate names, missing identity |
| `TZ=Europe/Amsterdam npm test -- --runInBand test/competitions` | Missing draft page | Pass; intermediate failures caught lost field errors and locale-dependent 12-hour presentation |
| `npm test -- --runInBand test/pages/profile-page.test.js` | Admin entry link missing | Pass after adding guarded link |
| same profile command | Profile arriving after open did not reveal admin link | Pass after updating the link when profile changes |

Frontend journey tests use the real API repository/module with a fake HTTP port.
In the deterministic Europe/Amsterdam environment, 7 November 2026 16:00 is sent
as `2026-11-07T15:00:00.000Z`, read back as `15:00:00Z`, and displayed as 16:00.
Server field failures retain input and do not navigate; storage failure also
never shows saved success. Jest's configured timezone makes this repeatable.

The first full frontend run reported passing assertions but exited with a late,
unawaited existing Jumper assertion (`2.0000000000000004` versus `2`). Replacing
polling with explicit clock/fake-timer advancement and awaited stop promises
exposed a repeated-stop `undefined.liftTo` error. A one-line guard preserves
idempotent stop completion. This verification finding is resolved under 1a in
the designated plan; no broader animation refactor was made.

## Validator follow-up — reopening during pending requests

The shared page now gives every opening a distinct lifecycle token and invalidates
it on close. Page continuations check their opening before rendering or navigating.
The module also scopes draft, error and busy-state updates to that session, so an
old request cannot overwrite the currently opened form's state. Requests already
sent may finish on the server; this change does not promise cancellation.

`cd qubigo-frontend && npm test -- --runInBand test/competitions` first failed
three new deferred-promise regressions: the old load removed the reopened form,
the old save navigated away, and the old save failure set the current error.
After the fix all 10 tests pass. The tests resolve/reject held HTTP responses
directly without sleeps or polling, including an old failure arriving while a
new save is pending.

## Validator follow-up — navigation during a pending opening

A real Router regression navigates to a saved draft, holds its HTTP load, navigates
to Profile, and then resolves the old load. It first failed: the URL remained
`#/profile` but `STALE DRAFT` replaced Profile. No manual page close is used.
The regression also navigates onward and verifies Profile is closed, guarding
against a stale completion reclaiming the router's current-page ownership.

The router now records pending openings before calling asynchronous `open`,
invalidates and closes the previous opening on navigation (including unmatched
routes), and promotes an opening to current only if it is still active. The draft
page's existing close/session protection can therefore reject the stale load.

- `cd qubigo-frontend && npm test -- --runInBand test/competitions` — initial regression failed; 10 existing tests passed.
- `cd qubigo-frontend && npm test -- --runInBand test/competitions test/router.test.js` — 19 passed after the fix.

## Final checks

- `cd qubigo-backend && ./gradlew test` — exit 0; 911 servlet and 18 CLI tests, zero failures/errors/skips in the prior run; follow-up invocation also exits 0 (test tasks up-to-date).
- `cd qubigo-frontend && npm test -- --runInBand` — exit 0; 66 suites passed, one skipped; 715 tests passed, 11 skipped.
- `cd qubigo-frontend && npm run build` — exit 0.
- `git diff --check` — clean.

Existing skipped frontend tests remain skipped; no acceptance depends on them.
Unrelated pre-existing `.idea/` workspace state was left untouched. No commits
were made by the doer, and increment 1a remains open for planner acceptance.
