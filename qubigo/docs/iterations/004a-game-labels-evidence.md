# 004a / 3e — identify competition games

Implementation ready for independent review; task remains open. No commits made (factory owns commits).

## Delivered boundary

Existing authenticated game GET and caller-specific list GET now compose the current saved competition name from the persisted game association. Responses add only an optional `competitionName`, not competition aggregates, rosters, accounts or management URLs. Ordinary games have no name/label. No new endpoint, input or access policy. Existing read authorization is unchanged, including the existing authenticated game-by-ID visibility contract (no invented spectator policy).

`GetGameCompetitionNames` coordinates CompetitionRepository's scalar association lookup and aggregate/name read; failure of either propagates rather than confirming an ordinary game or partial list. Controllers translate those read facts to DTOs. Games domain behavior remains unaware of Competition. No storage schema, matching, scoring or invitation changes. The existing scalar-only association query and protected completion path are untouched.

Frontend DTO conversion carries the optional name into both active/completed list items and the game presentation model. The existing list component renders `Competition — <name>` alongside opponent, score and actions. GamePage shows the same text above its existing online/tabletop component. Names use textContent, not HTML. The screen reserves remaining height for the existing board. Separate reloads retrieve current names, not historical snapshots; no live rename synchronization is promised.

Read failures show unavailable/revisit guidance without displaying the previous identity or a successful empty list. Per-opening/read identities guard stale success, failure and busy/error completion in shared modules. Actual Router regressions cover newer openings and navigation to Profile. Normal move responses remain ordinary game data: the frontend retains the identity already retrieved for that game, and ignores a move completion from an older opening. Loading a notification refresh hides (rather than destroys) the existing board while facts are unavailable.

## Architecture trace

Re-read backend AGENTS and ADRs 0002/0005/0007/0008, frontend AGENTS and the responsibility lens. Traced existing `asValidMoveParameters` field-specific Position/Piece conversions → success-only controller delegation → MakeMove's game/user loading → immutable Game.move/withMove state-dependent turn/occupation/piece checks → SQLite event-journal reconstruction/storage. Position's constructor still permits out-of-board values; Game.move checks range. CompetitionName construction enforces its intrinsic character/trim rules. This increment adds no submitted values or new Domain Primitives, and does not alter those rules or introduce a mutating SQL use case.

## Test-first record

Commands ran from the named service directories; logs mentioned here are local `/tmp/004a-label-*.log` transcripts, not committed artifacts.

- `./gradlew :qubigo-servlet:test --tests '*GameCompetitionNamesTest'`: first failed compilation on missing GetGameCompetitionNames. Added the narrow read query. Its four examples cover current saved names, ordinary absence, association failure and name failure.
- `./gradlew :qubigo-servlet:test --tests '*GameCompetitionNamesTest' --tests '*GameControllerGetGameTests'`: 10 examples, two failures before HTTP composition (`$.competitionName` absent; failed association still returned 200). Added required read composition and optional DTO name; focused query/controller checks then passed.
- `npm test -- --runInBand test/games/components/game-list-component.test.js`: three new active/opponent/completed cases failed on missing `Competition` (28 existing passed). Added conversion/model fields and text-only list presentation: 31 passed.
- `npm test -- --runInBand test/games/pages/game-competition-page.test.js`: first three runnable screen tests failed on missing identity. Added game conversion and page presentation: three passed. Harness uses real GamePage, CurrentGameModule, DTO converter and OnlineGameComponent; only the WebGL GameField and icon boundary are substituted.
- The same screen command then reproduced three stale/failure cases (old game overwrote module, old error polluted new opening, failed revisit retained previous identity). Added opening/read guards and unavailable presentation; focused screen/current-module checks passed (23 examples at that stage).
- `npm test -- --runInBand test/games/pages/game-list-competition-page.test.js`: three failures before list read/opening isolation and unavailable presentation. After fixes, this suite plus existing list-page/module suites passed: 36 examples.
- Screen move regression first failed when loading-state presentation destroyed a component during observer notification (`Cannot set properties of null (setting 'disabled')`). Keep the board on normal moves and preserve the retrieved name. Notification-refresh regression later reproduced the same destruction mechanism on a read; hide rather than destroy the board while loading. Both passed after the respective changes.
- Two late-move examples first failed (old game replaced current state; old failure set current error), then passed with opening-scoped move completion.

Additional focused regression evidence: real SQLite fresh reads distinguish completed and active games belonging to two competitions and exclude an ordinary game. Controller list examples cover both players' turn categories and completed entries; regular-user access remains successful and existing anonymous requests remain denied. The stored-name screen response example uses a regular online game. Existing ordinary DTO/controller/game tests remain passing.

## Final verification

- `./gradlew :qubigo-servlet:test --tests '*GameCompetitionNamesTest' --tests '*GameControllerGetGame*' --tests '*SqliteBasedCompetitionRepositoryTest'`: passed.
- Full `./gradlew test`: final run exit 0; XML totals **953 servlet + 18 CLI**, zero failures/errors/skips. An earlier full run passed servlet but hit SQLite shared-cache migration contention in CLI context startup; the immediate full rerun passed, and the final run after refactoring executed both suites successfully.
- `npm test -- --runInBand`: **866 passed, 11 skipped**, 74 passing suites / one skipped. No ignored game-page tests are claimed as evidence; the new runnable screen suite has 12 passing cases.
- `npm run build`: exit 0.
- Scoped `git diff --check`: exit 0. Inspected tracked staged/unstaged HEAD diff and all untracked product/evidence files, excluding factory/assembly metadata.

## Validator follow-up — cancelled retrieval busy state

The supplied finding reproduced in the existing real-Router page suites: navigation invalidated a pending read, so its guarded finally could never release busyInOperation. Both cancelListLoad and cancelGameLoad now release busy state when invalidating an existing opening. Repeated cancellation without an opening is a no-op; stale completion still cannot clear a newer request's busy state.

- Before production changes, `npm test -- --runInBand test/games/pages/game-list-competition-page.test.js test/games/pages/game-competition-page.test.js`: **6 failed, 16 passed**, with `Expected: false; Received: true` for busy state after navigation and late completion.
- After the minimal cancellation changes, the same command: **22 passed**. Coverage includes immediate cancellation cleanup, late success/failure remaining idle, a real InvitedByPage using the shared GamesModule with its actual throbber unblocked, and older completion leaving a newer pending list/game busy.
- Full `npm test -- --runInBand`: **870 passed, 11 skipped**. `npm run build`: exit 0.
- Backend unchanged in this follow-up; `./gradlew test`: **BUILD SUCCESSFUL**, 3 tasks executed / 19 up-to-date.
- Scoped `git diff --check`: exit 0. Task 3e remains open for independent acceptance and factory commit.

No external input is missing for 3e. No device, OS link, store, email or push attestation claimed. Closure, ranking, progress and platform readiness remain later tasks.
