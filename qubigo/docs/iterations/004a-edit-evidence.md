# 004a — edit saved competition drafts (increment 1b)

Implemented; awaiting validator review, factory commit and planner acceptance.
Task 1b remains open. No device or human attestation is required for this scope.

## Delivered journey and boundaries

Any existing signed-in admin can reopen the saved competition URL, choose
**Edit draft**, change the four settings and save. Success immediately displays
the saved draft at the same URL; reopening retrieves its updated settings.
There is no creator ownership restriction. Creation remains a separate operation.

API: `PUT /api/admin/competitions/{id}` with the same complete settings as creation.
Existing security matchers deny regular users (403) and anonymous callers (401).
The UI checks the loaded profile before requesting a draft or offering editing.
Missing competitions fail rather than becoming new competitions.

Name trimming/intrinsic rules, field-specific conversion, whole-number count
rules without business ceilings, and device-local deadline presentation are
reused from 1a. Past deadlines and unchanged settings are valid. An unchanged
closing-time input preserves the original instant, including precision beyond
JavaScript's milliseconds. Failed saves retain submitted values and show errors,
not saved success.

Editing preserves identity and DRAFT status; it introduces no enrollment, games,
invitations or other feature collaborators. Opening/joining, additional states,
lists, concurrent-editor conflict detection, audit history and request cancellation
remain deferred. Already-sent server requests can complete after navigation; their
late frontend completions cannot replace or redirect the active page or overwrite
another opening's draft, error or busy state.

## Responsibility trace

Read backend AGENTS, architecture/domain-module guidance and ADRs 0002/0005/0007/
0008, frontend AGENTS and the responsibility-driven-design lens. Traced the Games
example in current code: DTO conversions construct typed Position/Piece inputs;
GamesController invokes MakeMove only on conversion success; MakeMove loads facts;
Game.move owns turn/occupation/piece rules and withMove returns new state;
SqliteBasedGameRepository reconstructs/appends the journal.

For editing: the shared `CompetitionSettingsDto` (renamed from
`CreateCompetitionDto`) uses the existing field-keyed `validateParameters`
conversions to construct CompetitionName, Instant and validated count primitives.
The controller invokes `EditCompetition` only after successful conversion. That
command loads through CompetitionRepository, asks immutable Competition to
`editSettings`, and saves the returned aggregate, propagating storage failures.
The existing SQLite adapter already replaces records under their identity; its
new adapter test proves replacement and reconstruction after clearing JPA state.
It does not coordinate the use case or decide editing rules. Only DRAFT exists
in this scope; restrictions for future states belong in Competition when those
states are delivered, not speculative adapter/controller checks now.

## Test-first record

Commands ran in the respective service directory. No sleeps/polling or external
services/devices were used.

| Command / behavior | Observed failure before production change | Passing evidence |
| --- | --- | --- |
| `./gradlew :qubigo-servlet:test --tests '*EditCompetitionTest'` | Unresolved EditCompetition/editSettings | 5 tests: immutable changed draft, command save under same identity, unchanged past settings, missing draft without save, storage failure |
| `./gradlew :qubigo-servlet:test --tests '*CompetitionsControllerTest' --tests '*SqliteBasedCompetitionRepositoryTest'` | Both authorized PUT tests failed (unsupported method before mapping existed); adapter replacement already passed with the existing adapter | PUT delegates validated input; invalid input never invokes edit; regular/anonymous denied; SQLite replacement preserves one identity |
| `./gradlew :qubigo-servlet:test --tests '*competitions*'` | — | Passed after endpoint/wiring addition; reused intrinsic/conversion tests |
| `npm test -- --runInBand test/competitions` — prefill | Edit button missing (`null.click`) | Saved settings prefill the existing form in Europe/Amsterdam |
| same — update journey | Expected HTTP PUT had zero calls (creation was invoked) | PUT sends Updated Event, games 5/minimum 4, local 17:00 as 16:00Z under the original identity, with no POST |
| same — immediate success | Form remained after successful save because navigating to the same hash does not open the saved view | Successful edit renders the returned draft directly, then survives Router reload |
| same — unchanged deadline | Expected `2020-11-07T15:00:12.123456Z`, received truncated `.123Z` | Untouched deadline retains the original instant/precision |
| same — failure/lifecycle regressions | Existing session protections reused | 20 tests passed: retained field/storage errors, missing draft, authorization, real-Router pending load and pending edit success/failure, old edit failure while another opening saves |

Frontend journey tests exercise the real page/module/API adapter through a fake
HTTP port; Router collaboration uses the actual Router and explicitly held
promises. Backend security/controller, pure domain/command and Flyway SQLite
adapter tests supply complementary evidence rather than claiming a browser or
native-device end-to-end run.

## Final checks

- `cd qubigo-backend && ./gradlew test` — exit 0, BUILD SUCCESSFUL; XML reports: 757 servlet tests and 18 CLI tests, no failures/errors/skips. Competition tests: 42 passing across six classes.
- `cd qubigo-frontend && npm test -- --runInBand` — exit 0; 66 suites passed, one skipped; 724 tests passed, 11 skipped.
- `cd qubigo-frontend && npm run build` — exit 0.
- `git diff --check` — clean.

## Validator revision — focused save/reopen tests

The validator identified a test-design failure: the save journey asserted immediate
confirmation, then reloaded and asserted again. The existing page suite passed
20 tests before revision; this was a structural finding, not a new product defect.
Split it into immediate saved confirmation and reopening updated settings, sharing
`prepareUpdatedDraftEdit` setup. Each test now has one action followed by assertions;
the reopening test also verifies a fresh GET of the original identity. No production
code changed for this revision.

Verification after the split:
- `cd qubigo-frontend && npm test -- --runInBand test/competitions` — 21 passed.
- `cd qubigo-frontend && npm test -- --runInBand` — 66 suites passed, one skipped; 725 tests passed, 11 skipped.
- `cd qubigo-frontend && npm run build` — exit 0.
- `cd qubigo-backend && ./gradlew test` — exit 0, BUILD SUCCESSFUL.
- `git diff --check` — clean.

No task was marked complete and no commit was made by the doer.
