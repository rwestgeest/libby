# 004a / 4b — protected standings snapshot

Implementation ready for independent review; task 4b remains open for planner acceptance and factory commit. No commits made by the doer.

## Delivered boundary

`GET /api/admin/competitions/{id}/standings` returns the competition identity/name, fixed games-per-participant allowance, `LIVE`/`FINAL` status and ranked rows (current player name, rank, points, completed games, nullable average personal response seconds). Existing ADMIN backend authorization protects the endpoint, including against regular/anonymous direct requests. Another organizer has access; no creator restriction or public standings API.

Competition domain behavior includes only matches with recorded results. All enrolled members remain represented, including waiting members with zero completions/points and missing speed. A read does not finalize, match, join, start or persist anything. Required missing/failed journals or player-name reads fail the whole snapshot, rather than omit rows or fabricate missing timing. Missing competition retains its existing failure.

Points sort descending before personal response time. ResponseTime retains integer nanosecond totals and counted turns; comparison cross-multiplies BigIntegers, with no division/rounding before ordering or equality. Equal points and mathematically equal averages share ranks using 1, 1, 3 numbering. Missing speed sorts behind measured speed at equal points. DTO average seconds are presentation-only doubles and cannot alter ranks.

The domain replays stored creation/move facts in journal order, attributes intervals using Game.playerHavingTurn before each actual move, and does not count the initially placed piece. Across finalized games it sums durations and counts, not per-game averages. For boundary-unfinished games, only the boundary-turn loser receives the last-event-to-closure interval and one count. Events at/after closure and subsequent completion are excluded. Normally completed games add no closure wait. Game rules, admission, result freezing and existing association ownership are unchanged. The journal query now explicitly orders by persisted event ID, preserving insertion order even for identical recorded timestamps.

## Precision observation

The existing SQLite/JDBC journal round trip preserves milliseconds, not arbitrary nanoseconds. Initial integration probes observed:

- expected `ResponseTime(nanoseconds=120000000001, turns=13)`, actual `120000000000`;
- expected `120123456789`, actual `120123000000`.

The acceptance policy is recorded timestamp precision, with nanosecond duration arithmetic where supported. No new timing storage/service was introduced. The final SQLite assertion uses a representable 120.123-second response; pure domain tests verify 120.000000001-second intervals, fractional-nanosecond averages and exact equality from different totals/counts. Persisted precision is not advertised as nanosecond precision.

## Test-first and verification record

Commands below ran from `qubigo-backend/` on 2026-10-19. Output logs are local `/tmp/004a-4b-*.log` artifacts, not product files.

1. `./gradlew :qubigo-servlet:test --tests '*CompetitionRankingTest'` initially failed compilation for missing ResponseTime/CompetitionStanding/rankStandings (`ranking-red.log`). Added exact comparison/ranking behavior; same command passed (`ranking-green.log`). Covers points dominance, speed, mathematically shared ranks and missing timing; later regressions distinguish one-nanosecond and fractional-nanosecond differences.
2. `./gradlew :qubigo-servlet:test --tests '*CompetitionStandingsTest'` initially failed compilation for missing `standings` (`timing-red.log`). Added Competition-owned journal timing; focused ranking/timing checks passed (`timing-green.log`). Added normal win/draw/loss, multi-game accumulation, 180 seconds/eight turns arithmetic, no-move closure, pending/unfinished exclusion and post-deadline reconstruction regressions (`domain.log`, later focused/full runs).
3. `./gradlew :qubigo-servlet:test --tests '*GetCompetitionStandingsTest'` initially failed for missing query (`query-red.log`), then passed after read coordination (`query-green.log`). Added missing competition/name/journal, thrown storage read, empty-live and no-write checks. A thrown name read initially escaped as IllegalStateException (`name-red.log`); handling it as an all-or-nothing QResult failure made the same command pass (`name-green.log`).
4. `./gradlew :qubigo-servlet:test --tests '*CompetitionsControllerTest'` initially ran 26 tests with one failure: new admin standings request returned 404 (`http-red.log`). Added route, DTO and bean; same command passed (`http-green.log`). Later coverage checks LIVE weighted 22.5 seconds, FINAL, missing speed, response privacy, honest failure, 403 for regular users and 401 for anonymous callers.
5. `./gradlew :qubigo-servlet:test --tests '*SqliteCompetitionStandingsTest'` exposed existing storage precision as described above (`storage.log`, `storage-precision.log`). Representable-precision tests pass; fresh entity/repository reads cover actual names, stored normal results, final timing despite later normal completion, competition-specific inclusion and unchanged frozen results/journal.
6. `./gradlew :qubigo-servlet:test --tests '*CompetitionStandingsTest'` exposed acceptance of a backwards interval when prior accumulated time remained positive (`duration-red.log`: expected IllegalArgumentException, none thrown). ResponseTime now rejects each negative interval before accumulation; focused/full verification passes.
7. Focused command: `./gradlew :qubigo-servlet:test --tests '*CompetitionRankingTest' --tests '*CompetitionStandingsTest' --tests '*GetCompetitionStandingsTest' --tests '*CompetitionsControllerTest' --tests '*SqliteCompetitionStandingsTest'` passed (`focused-final.log`).
8. First `./gradlew test` exposed ambiguous production GameRepository wiring (`expected single matching bean but found 2: sqliteBasedGameRepository,gameRepository`), then its test JVM exhausted heap after repeated failed contexts (`full.log`). Renamed the bean parameter to the existing canonical `gameRepository` selection used by adjacent beans. The next full run created contexts and reached 1016 servlet tests, with three SQLite setup/cleanup lock failures (`full-fixed.log`).
9. Isolated uncontrolled background closure processing from deterministic shared-database tests. `./gradlew :qubigo-servlet:test --tests '*CompetitionClosureSchedulingTest'` first failed the new disabled-processing assertion (`scheduling-red.log`). Added a default-enabled scheduling property, set false only in test resources. Existing direct real production-callback-to-SQLite and controlled registration tests remain runnable. `./gradlew test` passed with 1017 servlet and 18 CLI tests (`full-isolated.log`).
10. After fixture/readability refactoring, **`./gradlew test --rerun-tasks` passed**, all 22 tasks executed (`final-verification.log`): **1017 servlet + 18 CLI tests, zero failures/errors/skips**. No sleeps, polling or physical device claims. `git diff --check` passed.

## Architecture and deferrals

Read backend AGENTS, ADRs 0002/0005/0007/0008, architecture/domain-module guidance, the responsibility lens, exact seed/full specification/plan and resolved discussion. Retraced Games' field-keyed DTO conversions → typed MakeMoveParameters → coordinating MakeMove/required persistence → immutable Game.move/withMove → SQLite journal storage. Existing Position construction does not enforce board range; Game owns that state/range check. Enum failures remain field-specific. CompetitionName enforces its own construction rules. This read adds no submitted form fields or command mutation.

GetCompetitionStandings coordinates Competition storage, Games journals and Users names; Competition's pure behavior owns inclusion, timing and ranking. ResponseTime enforces intrinsic nonnegative totals/counts and intervals. Repositories retrieve/map ordered facts, not ranking decisions. No SQL-owned use case, sibling command composition or new bounded-context dependency.

Frontend unchanged: no frontend tests/build rerun for this backend-only increment, as scoped by 4b. Stand/player UI, refresh/stale display, reminders, physical evidence, historical snapshots, exports and caching remain deferred. Human device evidence remains task 6, not claimed here. Task 4b remains unchecked pending acceptance/commit.
