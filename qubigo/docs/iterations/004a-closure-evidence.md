# 004a — automatic competition finalization (4a)

Status: implemented in the working tree; awaiting independent review and factory commit. Task 4a remains open. No commits made by the doer.

## Delivered boundary

- Competition immutably freezes its results at/after the configured deadline. An unfinished associated regular game's boundary-turn player loses (0/3); a completed full board retains normal 3/1/0 scoring. Already recorded outcomes are retained. Pending invitations and unmatched members earn no fabricated result.
- `CloseCompetitions` discovers persisted unfinished STARTED identities, then reloads each aggregate inside the existing SQLite writer gate. Games journals are reconstructed using events strictly before closing, not the game's eventual post-deadline state. Competition decides the outcome; repositories only discover, reconstruct and store facts.
- `CompetitionCommandBeans` supplies the production command. `CompetitionClosureScheduling` enables Spring scheduling and registers a local 1000 ms fixed-delay task. Each invocation rediscovers persisted work, including overdue work after application startup. Failed processing is logged; work remains discoverable for a later invocation. This is not an exact-millisecond execution SLA, durable messaging system or general crash-recovery guarantee.
- `finalized` is nullable/default FALSE in the Flyway migration and defaults to false when reading old/null records. Finalized competitions are excluded from subsequent discovery.
- Associated move admission shares the same writer gate as closure, membership, editing and invitation acceptance. Inside that interval, the current game revision must still precede the proposed revision. A stale precomputed move fails without journal or competition writes. A valid move's event receives server time **inside the gate**, replacing its earlier construction timestamp. Only successfully persisted moves admitted strictly before closing contribute to competition outcomes; at/after-closing moves remain valid ordinary game activity but cannot change the boundary result. Returned/thrown required-write failures roll back the protected interval.
- Underlying Games behavior remains unchanged: regular initial piece, full-board completion, normal turns and post-deadline play. Games has no Competition dependency. No ranking, speed computation, reminder, result API or presentation redesign is included.

## Test-first and continuation record

This continuation reused the supplied partial finalization/domain/repository/admission implementation and its initial tests; it does not claim those were newly test-driven here.

1. Ran `cd qubigo-backend && ./gradlew :qubigo-servlet:test --tests '*CompetitionClosure*'` before adding the automatic trigger. It failed at test compilation: `Unresolved reference: CompetitionClosureScheduling` (three references in the existing scheduling test). This drove the missing production scheduling adapter and command bean wiring.
2. Reran with `--tests '*CompetitionClosure*' --tests '*SqliteCompetitionPlayTest'`: BUILD SUCCESSFUL. Initial finalization and boundary tests passed with the supplied partial behavior and the new trigger.
3. Added deterministic protected-interval regressions using the existing `PendingCompetitionMutation` fixture and request-bound EntityManagers, not sleeps or polling. The first two runs exposed stale incremental fixture bytecode (`AbstractMethodError` for the new `awaitingFinalization()` repository method), not an outcome defect. Forced recompilation with `--rerun-tasks`; all 55 then-selected examples passed. No production behavior was altered to conceal that build issue.
4. Added focused regressions for real trigger-to-SQLite finalization without player activity, legacy-null reconstruction/scalar discovery without aggregate hydration, stale precomputed move refusal, last-second turn loss, normal win/loss/draw closure, later completion replay, returned/thrown closure rollback and later rediscovery, and a subsequent competition starting after closure. These passed against the existing/new collaboration; no invented failing-rule history is claimed.

## Acceptance coverage

| Plan criterion | Executable evidence |
| --- | --- |
| A — deadline result | `CompetitionClosureTest`: before-deadline unchanged, exact-deadline turn loss in both directions, normal 3/1/0 full-board results, immutable original, pending/unmatched/no-start outcomes. `last second ordinary move makes the next player lose at closure` exercises actual stored journal timing. |
| B — automatic/catch-up | Scheduling registration test captures/runs the configured callback; production command configuration supplies `CloseCompetitions`. SQLite `configured automatic trigger finalizes persisted work without player activity` runs that adapter with the real command/gate/repositories and a controlled clock. Fresh collaborators rediscover overdue persisted work after an at-closing final move. Repetition retains results. Returned and thrown save failures roll back, remain discoverable and succeed on later processing. |
| C — authoritative admission | SQLite examples cover completion before closing, at-closing move before delayed closure, closure before the at-closing move, construction time replaced on admission, and a request constructed pre-deadline paused before its gate while closure wins. Stored timestamp and fresh frozen outcomes are asserted. Stale precomputed revision is refused without appending. |
| D — freeze/existing journeys | Ordinary final move after closure retains frozen results; later replay cannot supply another invitation. Pending/unmatched members receive no result; open-never-started remains unchanged. Subsequent competition starts. Existing full-suite membership/acceptance deadline, authorization and ordinary invitation/game tests still pass. |
| E — persistence overlap | Deterministic SQLite overlap: closure waiting on a pre-deadline final move; move waiting across closure; edit waiting across closure; discovery waiting while invitation acceptance records its game. Request-bound EntityManagers plus fresh reads retain finalized outcomes, membership and associations. Scalar discovery explicitly leaves entity cache empty. Real SQLite returned/thrown required-save rollback checks remain passing. |

The “fresh collaborators” example is **not** an actual stopped/restarted process test. It proves persisted discovery/reconstruction using new repository/command objects; the separately verified production callback supplies automatic startup/catch-up invocation. No real-time waiting or process orchestration is claimed or required by the scoped plan.

## Observed verification

- Final focused command: `cd qubigo-backend && ./gradlew :qubigo-servlet:test --tests '*CompetitionClosure*' --tests '*SqliteCompetitionPlayTest' --tests '*SqliteBasedCompetitionRepositoryTest'` — BUILD SUCCESSFUL, 82 examples (10 closure, 2 scheduling, 53 SQLite play, 17 repository), zero failures/errors.
- Final `cd qubigo-backend && ./gradlew test` — BUILD SUCCESSFUL. XML totals: 985 servlet tests and 18 CLI tests; zero failures, errors or skips. Servlet tests executed on the final run; unchanged CLI tests were up-to-date after the earlier passing full run.
- `cd qubigo-frontend && npm test -- --runInBand` — 870 passed, 11 skipped; 74 passing suites, one skipped. Frontend production code is unchanged.
- `cd qubigo-frontend && npm run build` — exit 0, Vite production build successful.
- Scoped `git diff --check` — exit 0. Inspected the required HEAD diff (staged and unstaged) and all untracked product/evidence files, excluding `.factory` and `.assembly-lines`.

## Validator follow-up — focused freeze regression

- Split `repeated closure cannot overwrite frozen outcome` from completion replay, keeping one action/behavior per test.
- Added `pre deadline completion replay cannot overwrite frozen deadline loss`: replay supplies the same game's full-board DONE state with the deadline loser now winning normally (8/5), timestamped one second before closing. The frozen aggregate must remain unchanged. This is a test-only correction; it passes against the existing production protections, not a newly observed production failure.
- `cd qubigo-backend && ./gradlew :qubigo-servlet:test --tests '*CompetitionClosureTest'` — BUILD SUCCESSFUL (11 cases).
- `cd qubigo-backend && ./gradlew test` — BUILD SUCCESSFUL; servlet tests executed, CLI up-to-date. Current XML under `build-reports/` contains 911 servlet and 18 CLI cases, zero failures/errors/skips. These are the observed follow-up report totals, not a repeat of the earlier totals above.
- Scoped `git diff --check` passes. Frontend and production code unchanged in this follow-up; frontend suite/build were not rerun (prior results above).
- Recorded the validator finding under 4a in the designated external plan and resolved that subtask only. Task 4a remains open; no commit made.

## Architecture and deferrals

Read backend AGENTS and ADRs 0002/0005/0007/0008. Re-traced field-keyed `GameDto.asValidMoveParameters` → success-only controller → typed `MakeMoveParameters` → coordinating `MakeMove`/required persistence → immutable `Game.move`/`withMove` → journal adapter. Enum conversion failures identify their submitted fields; existing Position construction allows out-of-range values and Game rejects them. CompetitionName enforces its own intrinsic validity. No new submitted input or input-boundary rewrite is introduced here. Deadline/turn/result rules stay with Competition; Games supplies journal/turn/score facts. The scheduling adapter only invokes the command, and SQL owns no finalization use case.

No external input blocks 4a. Physical OS/email/store/push evidence remains unresolved in task 6 and is not inferred from these tests. Ranking, timing aggregation, reminders, progress/stand presentation, reopening and invitation expiry/re-pairing remain deferred.
