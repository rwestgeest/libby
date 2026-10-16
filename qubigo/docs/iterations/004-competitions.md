# Iteration 004 — Event competitions

Status: Product specification for review, based on the product-owner interview. Consolidated on 2026-10-16. Scope and core behavior have been agreed through the interview; the consolidated document has not received final review. Implementation has not started.

## Purpose and deadline

Support competitions at Spellenspektakel. Short-term objective: more accounts; medium-term: returning players; long-term: physical game sales. This supports `docs/vision.md`: help visitors enjoy Qubigo digitally and eventually buy the physical game.

The hard deadline is the 7–8 November 2026 event at Jaarbeurs Utrecht (https://spellenspektakel.nl/, checked 2026-10-16). No ticketing or event-site integration is requested.

Play happens digitally in the native app or browser during the event. Competitions close before the event ends. Ordinary play remains available afterward. Only one competition runs at a time; sequential competitions are within scope.

## Administration and lifecycle

- An admin creates a competition with a closing date/time, game allowance per participant, and suggested minimum participant count.
- Admins see enrolled participants and start the competition manually. The minimum is advisory: admins may start below it. Reaching it does not automatically start anything.
- The game allowance and closing time are fixed once participants can join. The owner explicitly removed adjustments after considering their consequences for existing games and results.
- Closure happens automatically at the configured time. Reopening a closed competition is not requested.
- Use the existing admin role for competition creation, participant viewing, manual start, and access to the stand leaderboard. Enforce these permissions on the backend as well as in the interface. Exact layout and pre-publication editing are implementation-planning details.

## Joining and onboarding

1. Scan a QR code linking to a specific competition.
2. Open the native app if installed; otherwise open the competition invitation in the web app.
3. Register, verify email, and sign in as necessary, retaining the intended competition through the existing account flow.
4. Show rules, game allowance, and closing time before an explicit “Join competition” action. Opening the link alone does not enroll someone.
5. Wait until the admin starts the competition. Once running, automatic matching applies. Late enrollment is allowed, including near closing.

Native installation is optional. Use the current app banner to encourage it; no additional installation funnel is requested. Verify existing link behavior and retention through onboarding. A voluntary store installation must not be assumed to preserve competition context without evidence.

Reuse the existing Qubigo tutorial for learning the game. Before joining, show a short rules summary covering points, speed ranking, game allowance, and automatic loss if it is the player's turn at closure.

Keep the existing invitation and game lists. Add the competition name and a “Competition” label to its invitations, game-list entries, and game screen. A separate competition-game list is deferred; choose the simplest existing presentation first.

## Pairing and invitations

- Pair randomly among eligible participants, avoiding repeat opponents where possible. Allow a rematch if only a previous opponent is available.
- Balance who moves first by balancing invitation direction: the invited player makes the first move.
- Automatically arrange the next game invitation after a game finishes, until the participant reaches their allowance or the competition closes.
- Each participant plays at most one competition game at a time. Invitations must not cause a participant to exceed their allowance.
- If no eligible opponent is available, wait for another entrant. Do not give existing participants extra games to accommodate a late entrant.
- Reuse the existing invitation mechanism. No two-player readiness confirmation. After the invited player moves, the other player receives the existing move notification when supported by their app/settings.
- Unanswered-invitation expiry, release, and re-pairing are explicitly deferred. Do not invent these workflows; a pending invitation may leave a participant waiting.

Source inspection confirms `AcceptInvitation` invokes `CreateGame`: an unaccepted invitation has not created its game. Consequently, closure's automatic game-loss rule applies only once a game exists. An unaccepted invitation earns neither participant a result, consistent with the rule that waiting without an assigned game earns no points. Do not accept a competition invitation after closing or create a new competition game from it; invitation expiry and re-pairing during play remain deferred.

## Results and points

A normally completed game contains all 27 moves. Achieving a line does not end it early. Reuse normal game winner/draw rules, verifying the implementation during planning.

| Result | Competition points |
|---|---:|
| Win | 3 |
| Draw | 1 |
| Loss | 0 |

Finishing the full allowance is not required for prize eligibility. Fewer games means fewer opportunities to score; do not normalize points by games played.

There is no inactivity-based turn timeout. The deadline is competition closure.

At closure, whoever has the turn in an unfinished game automatically loses and the opponent wins (0/3 points). The owner accepted that this also applies when the last move was immediately before closing.

Waiting without an assigned game earns no win or loss. Finalize each result once, define authoritative ordering for moves at closure, and prevent later moves from altering competition results. The owner requested “nothing special” for the underlying game: preserve existing game behavior rather than adding special termination or conversion to ordinary play. Competition finalization must not depend on later game activity.

## Ranking and response time

Order by total competition points descending, then average personal response time ascending. Speed only breaks equal points; it never outweighs a points difference. Equal points and equal speed produce shared ranks, without another tiebreak.

Use the existing game event journal, subject to verifying timestamp semantics and completeness:

- First response: time from `GameCreation` to the first move.
- Subsequent responses: time between successive moves, attributed to the responding player.
- Include all normally completed competition games, irrespective of win, draw, or loss.
- For games ended by automatic loss at closure, include both players’ actual responses. Add the unanswered waiting time until closure as one extra counted turn for the player who loses.
- Average personal response time = total included personal response time / total counted turns. Aggregate across games; do not average per-game averages.
- With equal points, a player with zero counted turns ranks behind a player with measured speed. Missing timing is never treated as instantaneous play.

Example: 120 seconds across six turns gives a 20-second average. A participant with equal points and a 15-second average ranks higher. More points always ranks above either.

Closure example: the last move was at 15:50 and closing is at 16:00. Add 600 seconds and one counted turn to the player due to move; do not fabricate a response interval for the opponent.

Live speed standings include finalized games only, not recorded responses from games still underway. Final standings include all games finalized at closure. Timing precision and shared-rank numbering are implementation details to specify explicitly.

## Notifications

Send one automatic push reminder after 30 minutes without a move on the current turn. Reset reminder eligibility when the turn changes. Do not repeatedly remind for the same turn or send a stale turn reminder after the game or competition ends.

Reuse the existing push mechanism. Manual reminders and additional delivery channels are not requested. Verify browser/native support and permission behavior during planning; participation and scoring must not depend on successful delivery.

The original journey calls for inviting all players to the stand for the in-person prize presentation. The owner deferred this feature to later: do not require a presentation-time field or implement an automatic presentation invitation in this iteration.

## Player screen and stand leaderboard

On the player’s screen show competition name, closing time, completed games out of allowance, personal points, and status: waiting for opponent, your turn, opponent’s turn, allowance completed, or competition finished. Pre-start and pending-invitation presentation should fit the existing invitation flow.

The leaderboard is only displayed at the stand. An admin opens a full-screen display that refreshes automatically. Show rank, player name, points, games completed out of allowance, and live/final status. No personally highlighted row is assumed on this shared display. Refresh cadence and error presentation are implementation-planning details; a failed refresh must not present stale results as newly finalized.

Organizers decide at the venue how many participants receive prizes and present them in person. Shared ranks remain shared; no software prize-allocation algorithm is requested.

## Out of scope and deferrals

- Elimination rounds, advancement, and elimination messages.
- Player-created competitions, invitations to future competitions, direct chat, and group chat.
- Mandatory installation or another installation prompt.
- Concurrent competitions or extra games beyond the allowance for late entrants.
- Turn-timeout losses before closure or two-player readiness confirmation.
- Unanswered-invitation expiry and automatic re-pairing.
- Adjusting game allowance or closing time after enrollment opens.
- Prize-presentation invitation scheduling and delivery (deferred to later).

## Source findings and implementation-planning decisions

- `invitations/domain/commands/AcceptInvitation.kt` creates the game on acceptance, through `games/domain/commands/CreateGame.kt`. Preserve this boundary for competition timing.
- `games/domain/GameEvents.kt` and `games/adapters/repositories/SqliteBasedGameRepository.kt` expose and persist game creation/move timestamps. Verify all reconstruction and clock semantics when implementing response-time calculations.
- Existing regular-game creation places an initial piece on the board. The owner's “27 moves” describes a full-board game; preserve existing completion logic rather than introducing a hardcoded requirement for 27 player-generated move events. Count only actual timed player responses for speed.
- Admin roles already exist in the user model and backend security configuration. Extend existing authorization conventions rather than adding a new organizer-role system.
- Specify shared-rank numbering and timing precision, first-player balancing, invitation slot reservation, creation fields (including a display name), refresh cadence, and platform-specific link/notification acceptance checks during implementation planning.
- The rule that an unaccepted invitation earns no points at closure follows from the inspected game-creation boundary and the agreed no-game/no-result rule; include it explicitly in consolidated product review.

## Acceptance examples for implementation planning

- New and existing players can follow the competition link, authenticate as necessary, and explicitly join via browser or native app without losing the competition context.
- Participants wait for manual start. An admin may start below the suggested minimum; late entry is allowed while running.
- Matching uses the repeat fallback and balanced starting assignments, without concurrent competition games or exceeding allowances.
- Completed games score 3/1/0. At closure unfinished games score 0/3 based on whose turn it is. Waiting alone earns nothing.
- Points dominate speed; response-time aggregation includes all finalized outcomes and closure waiting. Missing timing has no zero-time advantage. Exact ties share ranks.
- One reminder becomes due after 30 minutes on an unanswered turn. Moving, finishing, or closing prevents stale reminders. Existing move notifications continue working.
- Closure freezes final competition results and stops further competition matchmaking. Ordinary play remains available afterward.
- Pending invitations cannot create competition games after closure and earn no result without an existing game. Existing underlying game behavior is preserved, with competition outcomes frozen.
- Player screens label competition invitations and games in the existing lists. Only admins can manage competitions and open the automatically refreshing stand display.

## Planning context and handoff

Iteration 002 addresses onboarding guidance; do not assume it has shipped. Iteration 003 addresses session continuity through backend deployment; verify implementation rather than inferring competition recovery guarantees. `docs/backlog.md` links here.

Before planning implementation, read applicable frontend/backend instructions and inspect link routing, invitations, game creation/move journals, normal scoring, notifications, persistence, and admin authorization. Record any mismatch with this specification before inventing product behavior.

The required task-discovery command `yx list --ready` was attempted but unavailable (`yx: command not found`); no task state was inferred.

No product code was changed or tests run during this interview. Implementation planning must define durable closure/reminder processing, ranking regression examples, and representative device acceptance checks. This document records the agreed interview direction and source findings; it does not claim final document approval, implementation, or production validation. Publishing and app-store releases are separate work.
