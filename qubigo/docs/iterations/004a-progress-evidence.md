# 004a — participant progress (5b)

Status: implemented in working tree; awaiting independent acceptance and factory commit. Task 5b remains open. No commits made by the doer.

## Delivered behavior

The existing authenticated competition preview now includes personal progress only for the enrolled caller. The join response retrieves the same projection after successful membership/matching. The existing participant page displays points, completed games/allowance, name and timezone-labelled deadline, with manual revisit/reload for refresh.

Competition-owned pure behavior supplies waiting-for-start, waiting-for-opponent, selected-invitee start, waiting-for-game, personal/opponent turn, allowance-completed and finished statuses. A completed game awaiting its recorded outcome is described as result-pending rather than an invented turn. Existing Games and Invitations routes are reused; only the selected first mover gets the Invitations action, and associated-game links target the existing game page.

At the deadline, finished takes precedence. A started competition awaiting automatic finalization explicitly presents provisional totals/results being finalized. Persisted frozen outcomes supply final totals without consulting subsequent ordinary play. An expired never-started competition does not promise future play; unjoined expired previews no longer offer joining.

## Responsibilities and access

- Read backend AGENTS and ADRs 0002, 0005, 0007 and 0008; retraced Games DTO field-specific conversions → typed MakeMoveParameters → coordinating MakeMove → immutable Game.move/withMove → journal reconstruction/storage. Position currently leaves board-range checks to Game; that existing caveat was not copied into new input types.
- No new submitted fields or primitives: existing authenticated identity and competition-id contract are reused. DTOs explicitly serialize presentation fields, not Competition, its membership or matches.
- Competition.progressFor owns counting/status/closure rules as a pure domain extension. It returns a value without changing Competition or Games. Existing pointsFor is reused, not ranking or journal score recalculation.
- CompetitionParticipation coordinates caller identity, protected competition retrieval, and only the caller's required associated-game read. Both aggregate hydration and game reads occur inside the existing shared gate; no pre-gate aggregate discovery/cache is introduced. Adapters continue mapping/storing facts, not deciding status.
- Unjoined visitors get terms without personal totals, assignment or roster. Existing authenticated preview/join authorization and admin management/standings restrictions remain unchanged. Arbitrary submitted identity does not choose progress.
- Required read failures return an error, not zero totals or confirmed waiting. Existing opening tokens protect the page against late success/error after navigation or reopening.

## Test-first record

1. `cd qubigo-backend && ./gradlew :qubigo-servlet:test --tests '*CompetitionProgressTest'`: failed compilation on missing progressFor/CompetitionProgress/PersonalPlayStatus. Added the pure projection. The first execution exposed invalid short test fixture IDs; replaced those with valid IDs. The next focused execution passed all 8 initial examples. Later regressions cover opponent turn, deadline precedence over a game and membership refusal (11 total).
2. `./gradlew :qubigo-servlet:test --tests '*CompetitionParticipationTest'`: failed compilation on the new query dependencies/projection. Implemented protected coordination and caller-only game lookup; focused run passed. Added regressions for no game read on unjoined preview and no pre-gate aggregate read (9 total).
3. `./gradlew :qubigo-servlet:test --tests '*CompetitionParticipationControllerTest'`: failed `preview returns only caller owned progress` with PathNotFoundException for `$.progress.points` (10 tests, 1 failure). Added explicit nullable progress DTO mapping and post-join retrieval; the focused controller/domain run passed.
4. `cd qubigo-frontend && npm test -- --runInBand test/competitions/pages/competition-page.test.js`: 10 new presentation cases failed, 20 existing cases passed. Added totals/status/navigation and expired-preview presentation; all 30 then passed. Added coverage for immediate post-join progress, failed reads and actual-Router late success/error isolation for Profile and another competition (36 page tests in final suite). Existing lifecycle machinery already passed those regressions; no new cancellation machinery was needed.
5. Extended existing SQLite read tests, reusing their storage/journal helpers rather than copying them: fresh collaborators retain the win/draw totals, use the authenticated person's identity, and retain a frozen deadline outcome despite a later full-board normal completion. These use the real gate and cleared persistence context. Existing scoring/standings tests remain unchanged in behavior.

## Verification

- First full `cd qubigo-backend && ./gradlew test` stopped in CLI contextLoads migrating V20261015120000__create_refresh_tokens.sql: `[SQLITE_LOCKED_SHAREDCACHE] ... (database table is locked: sqlite_master)`. CLI test configuration still enabled automatic competition processors against the shared in-memory database. Disabled those processors only in CLI test resources, matching servlet isolation. The next full run passed. This verification finding is recorded once and resolved under 5b in the designated plan.
- Full `cd qubigo-backend && ./gradlew test --rerun-tasks`: exit 0, all 22 tasks executed; **1060 servlet + 18 CLI tests**, zero failures/errors/skips. After extracting the shared Competition-owned finished predicate for both progress and preview mapping, `./gradlew test` passed again (16 tasks executed, 6 up-to-date). Production scheduling and controlled scheduler/storage tests remain enabled/unchanged.
- Final `cd qubigo-frontend && npm test -- --runInBand`: exit 0; **886 passed, 11 skipped**, 74 passing suites and one skipped suite. Skipped push tests are not claimed as evidence.
- Final `cd qubigo-frontend && npm run build`: exit 0.
- Scoped tracked HEAD diff and untracked discovery inspected directly; `git diff --check` passes.

## Boundary

No public standings, rank/speed display, auto-refresh, separate competition-game list, reminder/push changes, schema changes, invitation expiry or result mutation on reads. Physical OS/link/mail/store/push evidence remains task 6 and is not claimed by this application increment. Stand display remains 5c. No unavailable external input blocks 5b.
