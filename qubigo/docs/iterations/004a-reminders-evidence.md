# 004a — unanswered competition turn reminders (5a)

Implementation ready for independent acceptance and factory commit; task 5a remains open.

## Delivered behavior

Started, unfinalized competitions are discovered through the existing scalar-ID query. Inside the shared SQLite move/closure gate, the command loads the current competition and each unfinished associated game's stored journal. Competition owns the rules: regular associated game, matching participants, unfinished game/result, strictly before closing, and at least 30 minutes since creation or the last successful stored move. The initial placed piece is not a timed response. Pending invitations and unmatched members have no game timer; ordinary games are not discovered.

A nullable `reminded_revision` on each competition match records the attempted turn. NULL means no attempt; game revision zero is a legitimate first turn. The immutable transition leaves the original Competition unchanged. Repeated processing and fresh collaborators retain suppression; subsequent revisions have their own eligibility. Storage only maps this fact; neither SQL nor Notifications decides eligibility.

The attempt transaction must commit successfully before dispatch. A second protected admission re-reads the competition and game and checks the exact revision, recipient, unfinished state and current deadline. Thus a move or closure between recording and dispatch suppresses the obsolete message. Dispatch occurs within that shared interval so a competing move/closure cannot slip between revalidation and application dispatch. Discovery never hydrates the competition outside the gate.

This is a best-effort attempt, not an outbox or receipt guarantee. A process interruption after recording can lose a reminder. Missing registrations, provider failures and thrown dispatch exceptions consume the attempt without retries. A failed required save/transaction does not dispatch. Already dispatched messages can arrive after the turn changes; OS delivery cannot be recalled.

Production wiring supplies RemindCompetitionTurns using the existing SendNotification. A default-enabled local scheduler invokes it every minute, rediscovering persisted overdue turns on each invocation. Tests disable uncontrolled background scheduling and directly invoke the real registered callback with controlled time. Fresh-collaborator tests are not a claim of a physical process restart or exact-time execution SLA.

## Notification/platform boundary

No frontend changes. Inspected `qubigo-frontend/app/adapters/push-notification-service.js`: initialization requires the Capacitor PushNotifications plugin; registration/listeners are installed only after `requestPermissions()` returns `receive === 'granted'`. There is no ordinary browser web-push implementation. Existing `GAME_MOVE` plus a game reference provides tap navigation to `#/game/<id>` and foreground refresh. Reminder title/body explicitly describe the unanswered competition turn, not a fictitious opponent move. The existing Firebase adapter maps the reference to `objectReference` and the notification type to `type`.

SendNotification already skips absent device profiles and logs returned provider failures. Existing notification tests and ordinary move/completion regressions ran in the full backend suite. No skipped frontend push tests are claimed as passing evidence; payload/navigation and permission handling are unchanged. Physical Android/iOS permission, registration, receipt and tap outcomes remain task 6 evidence for the owner/device tester, not supplied by these controlled tests.

## Architecture trace

Re-read backend AGENTS and ADRs 0002/0005/0007/0008 and traced the current Games path: field-keyed `GameDto.asValidMoveParameters` constructs typed Piece/Position inputs; GamesController calls MakeMove only after successful conversion; MakeMove coordinates current game/user, immutable Game.move/withMove, required persistence, then immediate notification; SQLite stores/reconstructs creation/move journal facts. Enum construction errors identify their submitted fields; Position's existing range check belongs to Game. CompetitionName enforces its own intrinsic construction rules. This increment adds no submitted inputs or endpoint and preserves those boundaries. Competition owns time/state eligibility and attempt accounting; the command loads, delegates and persists; Games remains independent of Competition; Notifications retains registration/provider ownership. No new architectural departure or external dependency.

## Test-first record

Commands were run from `qubigo-backend`:

- `./gradlew :qubigo-servlet:test --tests '*CompetitionReminderTest'`: failed compilation on the missing reminder API (`reminderDueFor`, `TurnReminder`, `recordReminderAttempt`); after the minimal pure behavior, passed seven cases. Later journal examples verify a 14:10 move makes the next turn due at 14:40, not the previous turn's 14:30 threshold.
- `./gradlew :qubigo-servlet:test --tests '*CompetitionReminderProcessingTest'`: failed with `Unresolved reference: RemindCompetitionTurns`; after command coordination, all six processing cases passed. These cover correct recipient/game/wording, fresh-processor suppression, returned save failure, failed transaction completion despite a successful save return, an intervening move before dispatch, and provider exception suppression.
- `./gradlew :qubigo-servlet:test --tests '*SqliteCompetitionRemindersTest'`: initially failed with missing CompetitionReminderScheduling; after adding the registered callback, six tests ran and three failed (persisted suppression, automatic callback dispatch, competing processors). The marker was not yet mapped/stored. After nullable migration/mapping, all six passed. An intermediate run rejected a duplicate Flyway version; the new migration now has the distinct version V20261024120000.
- `./gradlew :qubigo-servlet:test --tests '*CompetitionReminderSchedulingTest'`: failed with `Unresolved reference: remindCompetitionTurns`; production bean wiring then made it pass. Registration/disable tests use controlled callbacks, not clock waits.

Real SQLite examples exercise committed suppression across fresh repositories, registered callback → storage → dispatch, required-write rollback, two overlapping processors, reminder admission waiting behind a successful move, and closure between recording and dispatch. Interleavings use the existing PendingCompetitionMutation fixture/latches, without sleeps. Pure examples cover threshold boundaries, exact threshold/closing equality, full-board completion, pending invitation exclusion, immutable accounting and later turns of the same player.

## Final verification

- `./gradlew :qubigo-servlet:test --tests '*CompetitionReminder*Test' --tests '*SqliteCompetitionRemindersTest'`: exit 0, 24 reminder cases.
- `./gradlew test`: exit 0 (initial full run).
- `./gradlew test --rerun-tasks`: exit 0; all 22 tasks executed. XML totals: **1041 servlet + 18 CLI tests, zero failures/errors/skips**.
- `git diff --check -- . ':(exclude).factory' ':(exclude).assembly-lines'`: exit 0.
- Frontend unchanged; frontend tests/build not rerun. No device/provider/network delivery claim, no new endpoint, no manual reminders, no retries or browser push.
