# 004a increment 1c — open for joining

Implementation complete; awaiting factory acceptance/commit. This is not completion of iteration 004. Increment 1c remains unchecked in the designated plan.

## Delivered boundary

- Any existing admin can explicitly open a saved draft through its management page or `POST /api/admin/competitions/{id}/open`. Existing admin security matchers protect this new endpoint; regular and anonymous callers are denied.
- `Competition.openForJoining(now)` returns a new immutable OPEN competition only when its closing instant is strictly after now. The command loads, supplies its Clock fact, delegates, and saves only successful transitions. Missing competitions, elapsed deadlines and storage failures are not successful openings.
- The saved page immediately shows “Open for joining — play has not started”; revisiting loads persisted OPEN status with the same identity and settings. Viewing a draft does not open it. No participation, games, invitations, scheduler or other context integration was introduced.
- `Competition.editSettings` now returns a QResult. While OPEN it rejects the whole edit if closing time or games per participant differs; unchanged fixed settings permit name/minimum edits. Drafts retain unrestricted four-setting editing, including past deadlines. The UI makes the fixed fields read-only, explains why, and retains exact unchanged deadline transport rather than reducing fractional precision through Date conversion.
- Opening disables management buttons while pending, reports failures without replacing the draft with a success confirmation, and reuses the existing page/module session isolation. Real-Router deferred success/failure tests cover navigation away; another regression covers reopening a form with a pending save while an old opening fails.

## Responsibilities checked

Read backend AGENTS, ADRs 0002/0005/0007/0008, frontend AGENTS, the responsibility-driven lens, the full specification, seed, exact external plan and resolved owner discussion. Consulted `yx list --ready` (existing walkthrough yak; no task changes).

Traced Games: `GameDto.asValidMoveParameters` performs field-keyed construction of typed inputs; `GamesController.postMakeMove` delegates only on conversion success; `MakeMove` coordinates loading and delegates to immutable `Game.move`/`withMove`, where state-dependent rules live; `SqliteBasedGameRepository` reconstructs and persists journal events.

Changed Competition path retains `CompetitionSettingsDto` → `asValidCompetitionParameters` → valid CompetitionName/count primitives and Instant → EditCompetition → aggregate-owned fixed-setting rule → repository storage. Opening has no submitted settings DTO; only the existing identity path parameter is needed. OpenCompetition coordinates repository and Clock; Competition owns deadline/transition rules. Existing repository mapping already stores and reconstructs status by name, so no schema or adapter business-rule change was needed. SQLite tests independently verify OPEN reconstruction and accepted open-setting edits. No command invokes a sibling command.

## Test-first record

Commands below were run from their respective service directories; temporary logs are execution aids, not required product artifacts.

1. `./gradlew :qubigo-servlet:test --tests '*OpenCompetitionTest'`: initial compilation failed for absent `openForJoining` / OPEN. Added the smallest immutable transition; rerun passed.
2. Added exact-deadline/after-deadline cases; same command reported **3 tests, 2 failed** with unconditional opening. Added the aggregate's strict future check; rerun passed.
3. `./gradlew :qubigo-servlet:test --tests '*EditCompetitionTest'`: **8 tests, 3 failed** for accepting mutable fixed settings and the missing QResult outcome. Added aggregate rejection and changed EditCompetition to save only successful results; adjusted existing expectations for the new result protocol; rerun passed.
4. Added OpenCompetition coordination tests; focused command failed compilation for absent OpenCompetition. Implemented load → aggregate using Clock instant → save. Rerun passed, including refusal without save, missing identity and storage failure.
5. `./gradlew :qubigo-servlet:test --tests '*CompetitionsControllerTest' --tests '*SqliteBasedCompetitionRepositoryTest'`: **21 tests, 1 failed**, because explicit opening lacked an endpoint. Added controller delegation and production bean. `./gradlew :qubigo-servlet:test --tests '*competitions*'` passed. Existing string status mapping already satisfied new persistence tests.
6. `npm test -- --runInBand test/competitions/pages/competition-draft-page.test.js`: **9 failed, 21 passed**, exposing absent opening action/status/read-only policy. Added repository request, session-aware module operation and page behavior. Rerun: **30 passed**.
7. Added a command-level fixed-settings refusal regression confirming no save on aggregate rejection. Final full runs below include it.

## Final verification

- Backend `./gradlew test`: exit 0, BUILD SUCCESSFUL. Current XML results under `build/reports/*/test-results/test`: **774 servlet tests (59 competition tests), 18 CLI tests**, no failures.
- Frontend `npm test -- --runInBand`: exit 0, **734 passed, 11 skipped**, 66 suites passed / 1 skipped.
- Frontend `npm run build`: exit 0.
- `git diff --check`: exit 0.

No device attestation or unavailable external evidence is required for 1c. Joining, public pre-join views, links/QR, onboarding, listings, start/play, automatic closure and concurrency/retry guarantees remain deferred. Factory handles commits; no commit was made here.
