# 004a / 3d — normal game completion advances Competition

Implemented; awaiting independent review, acceptance and factory commit. Task 3d remains open. No device evidence or iteration-completion claim.

## Delivered boundary

The existing authenticated move journey now persists an associated regular game's normal completion, records one immutable Competition outcome (3/0, 0/3 or 1/1), and supplies the next existing invitation when eligible. The full board, not a line, completes a game. Completed games continue to consume allowance; the next pending reservation consumes its own place. With allowance exhausted, players wait without another invitation.

No new endpoint or frontend presentation was needed: the existing competition invitation list/detail/accept journey already supplies the selected first mover's usable action. Normal game construction and notification behavior remain in use. Labels on game screens, closure, speed, standings, reminders and physical platform evidence remain deferred.

## Responsibilities and storage

Read backend AGENTS, ADRs 0002/0005/0007/0008 (and 0006), architecture/domain-module guidance, the responsibility lens, and the resolved owner discussion. Re-traced `GameDto.asValidMoveParameters` field-keyed conversions → success-only `GamesController.postMakeMove` → `MakeMove` → immutable `Game.move`/`withMove` → journal storage. Existing Position construction permits out-of-board values; aggregate range validation remains unchanged. No new submitted inputs or Domain Primitives were introduced; `CompetitionResult` is a closed enum, not arbitrary submitted points.

- Games still owns moves, player scores, full-board completion and timestamped journals. It imports no Competition types. `MovePersistence` is its narrow required-write port; MakeMove propagates its QResult before sending the existing notification, rather than discarding a save failure through `onSuccess`.
- `CompetitionMovePersistence` is application coordination, wired into the production move command. It resolves association from storage, saves an associated move and reads persisted game/journal facts before applying Competition behavior. Ordinary unassociated games only save their move.
- `Competition.recordCompletion` owns association/player/type/completion/deadline checks, immutable result recording and repeat suppression. Points are derived from recorded outcomes, not additive side effects. Original aggregates stay unchanged.
- Existing Competition matching owns eligibility, slot/allowance accounting, new-opponent preference and direction balance. Existing invitation lifecycle/presentation is reused unchanged.
- Result/reservation/invitation writes and the associated move use the same SQLite protected transaction as join/open/edit/accept. Returned failures and thrown failures roll back. Repository changes only map/store the nullable result enum; Flyway adds a nullable `result` column with a NULL default, retaining older match records.
- Normal scoring uses the persisted final event's timestamp. Matching separately uses reaction time. A completion at/after closing gets no normal finalized outcome; regular moves remain ordinary. A reaction at/after closing cannot supply another invitation. Authoritative boundary ordering/closure is still 4a; no scheduler, durable delivery or automatic retry guarantee was added.

## Observed test-first sequence

1. `./gradlew :qubigo-servlet:test --tests '*CompetitionCompletionTest'` failed compilation on missing `recordCompletion`/`pointsFor`. Added the minimal immutable transition and closed result values; the command then passed. Named examples cover 8/5, 5/8, 5/5, repeat processing, a line without a full board, an unassociated game and exact/after-deadline refusal.
2. `./gradlew :qubigo-servlet:test --tests '*SqliteCompetitionPlayTest.final move*'` first failed compilation on missing `CompetitionMovePersistence` and the move persistence parameter. Added the port/application collaboration. The next run failed the runtime assertion at the fresh-repository result check (`assertNotNull`, then line 333): the result was not mapped to SQLite. Added the nullable migration and adapter mapping; the same real move-to-invitation example passed.
3. Added focused regressions for stored points/reload, next invitation acceptance, exhausted allowance, repeat reaction, new opponent, unfinished/ordinary games, deadline moves, required write rollback, protected overlaps and invalid/unauthorized moves. The preparation accepts a real invitation and persists 25 actual moves after regular creation's initial piece; the tested final move fills the board without a fabricated 27-player-event requirement.
4. First full backend run had one failure: the duplicate-reaction test assumed *every* contending request succeeds. SQLite reported `[SQLITE_LOCKED_SHAREDCACHE] Contention with a different database connection that shares the cache (database table is locked)`. The established request contract permits honest contention failure and promises no automatic retries. The assertion now requires a successful reaction and one persisted result/next reservation/actionable invitation, not universal request success. The final version races first processing of an already persisted completion (not merely replay after an outcome already exists). Full backend reruns passed.

## Acceptance coverage

- Pure `CompetitionCompletionTest`: normal 3/1/0 outcomes, immutable original, once-only outcome, non-full-board and unassociated refusal, exact/after deadline.
- Extended real SQLite `SqliteCompetitionPlayTest`: production MakeMove → required persistence → stored facts → policy → next invitation; fresh adapter result/points reads; next invitation acceptance via the existing coordinator; allowance 1 waits; allowance 2 rematches with reversed first mover; available new opponent preference; unfinished/unassociated/deadline moves do not advance; a pre-deadline stored completion reacted to at closing can score but cannot arrange another invitation.
- Failure injection after actual writes: returned result/reservation-save failure, thrown next-invitation-save failure, returned move-save failure. Fresh repositories verify unchanged competition, still-playing game at the previous revision and no orphan actionable invitation for either player. The move itself rolls back when required advancement fails.
- Deterministic overlaps reuse `PendingCompetitionMutation`: join/open/edit around completion retain STARTED, members, points and reservations; completion replay around next acceptance preserves both game associations. Concurrent first duplicate reactions retain one result/next invitation, allowing honest SQLite contention failure. Existing 3c domain examples continue guarding busy/exhausted candidates and allowance charging.
- `MakeMoveTests`: failed default save returns failure without notification; invalid/unauthorized moves never reach persistence reactions. Existing direct move authorization, conversion, normal move notifications and ordinary invitation/game suites pass. No new HTTP inputs bypass existing validation.

## Final verification

- Focused backend: `./gradlew :qubigo-servlet:test --tests '*SqliteCompetitionPlayTest' --tests '*CompetitionCompletionTest' --tests '*MakeMoveTests*'` — passed (before the final additional concurrency regressions, subsequently included in full runs).
- Final `cd qubigo-backend && ./gradlew test` — exit 0; XML totals: **939 servlet tests, 18 CLI tests**, zero failures/errors/skips. CLI was up-to-date on the final rerun, executed successfully on the preceding full run.
- `cd qubigo-frontend && npm test -- --runInBand` — exit 0: **845 passed, 11 skipped**; frontend unchanged.
- `cd qubigo-frontend && npm run build` — exit 0.
- Scoped `git diff --check` — passed. Inspected tracked HEAD diff (staged/unstaged) and discovered/read all untracked product files, excluding `.factory` and `.assembly-lines` metadata.

## Validator follow-up — request-bound persistence context

Recorded the finding once under task 3d in the designated plan. Association discovery previously called `started()`, hydrating CompetitionRecord before the gate. It now uses a scalar JPQL competition-ID projection via `CompetitionRepository.associatedWith`; both move persistence and replay load the aggregate only inside the existing gate. Ordinary games retain their unprotected ordinary-save path. No aggregate rules moved into storage.

Test-first evidence:
- Added request-bound EntityManager interleavings using the existing pending-gate fixture: a final move overlapping a join retains the third member; a completion replay overlapping next-invitation acceptance retains both game associations. These examples already passed against the previous implementation in this adapter setup; they are regression coverage, not a claimed reproduction of lost writes.
- Added `game association lookup returns identity without caching the aggregate` and missing-association coverage. Initially failed compilation (`Unresolved reference: associatedWith`). With the original `started()`-based lookup behind the new port, the cache assertion failed at runtime: `expected: <0> but was: <1>`. The scalar projection passes, directly guarding the pre-gate cache-hydration mechanism independently of Hibernate's merge behavior in the interleavings.
- `./gradlew :qubigo-servlet:test --tests '*SqliteBasedCompetitionRepositoryTest' --tests '*SqliteCompetitionPlayTest' --tests '*CompetitionCompletionTest' --tests '*MakeMoveTests*' --rerun-tasks` passed (72 examples). A forced rebuild was needed after incremental compilation retained a fixture delegator without the new repository method.
- Final `cd qubigo-backend && ./gradlew test`: exit 0; **942 servlet + 18 CLI tests**, zero failures/errors/skips.
- Frontend unchanged: `npm test -- --runInBand` passed (**845 passed, 11 skipped**); `npm run build` passed.
- Scoped tracked HEAD diff, untracked product files and `git diff --check` inspected; diff checks pass.

No new owner choice or unavailable external input is needed. No commits made by the doer. Task 3d remains open for planner acceptance and factory commit.
