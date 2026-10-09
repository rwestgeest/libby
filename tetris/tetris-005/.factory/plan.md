# Terminal Tetris plan

Status: in progress; first five tasks completed.

## Seed requirements

- Build a playable Tetris game in the terminal.
- Launch it with `npm start`.
- Keep the entire display within 24 terminal rows, including the board, score, controls, borders, and game-over messages.
- Terminal rendering or input packages may be installed if useful.

## Implementation tasks

- [x] Create a Node.js project with an `npm start` entry point and an automated test command.
- [x] Implement a 10-column, 20-row board, the seven tetrominoes, piece spawning, movement, rotation, collision detection, and piece locking.
- [x] Implement timed gravity, completed-row clearing, score updates, and game over when a new piece cannot spawn.
- [x] Implement terminal keyboard input for left/right movement, rotation, soft drop, hard drop, restart, and quit; show the controls on screen.
  - [x] Resolve validator finding in `test/terminal.test.js`: inject rendering or game-operation errors after raw mode is enabled; ensure exception cleanup stops gravity, restores input settings, and shows the cursor. Added coverage for initial rendering, keypress operations, gravity rendering/operations, and restart failures with both prior raw-mode states. Verified with `npm test` (35 tests passed).
  - [x] Resolve validator finding in `test/terminal.test.js`: add automated render assertions that the complete playing and game-over displays stay within 24 rows, including board, borders, status, controls, and game-over information. Verified with `npm test` (25 tests passed).
- [x] Render the board and all supporting information in a fixed layout of at most 24 rows: 22 rows for board and borders, one for score/status, and one for controls. Show game-over information in the status row rather than adding rows.
  - [x] Make layout dimensions explicit and use carriage-return/newline pairs for aligned raw-terminal redraws without a trailing newline.
  - [x] Verify playing and game-over layouts, 80-column fit, and successive terminal redraws with status and controls in fixed rows. `npm test`: 36 tests passed.
- [ ] Restore terminal input settings and cursor visibility on quit, interruption, and errors; handle unsupported non-interactive input clearly.
  - [x] Resolve validator finding in `test/lifecycle.test.js`: add a real subprocess regression combining an uncaught exception with failed raw-mode restoration, and preserve the original exception report rather than replacing it with the cleanup failure. Reproduced exit status 7 before the fix; both prior raw-mode states now report the original exception with exit status 1 and complete remaining cleanup. `npm test`: 65 tests passed.
  - [x] Add cleanup for SIGINT, SIGTERM, SIGHUP, input EOF, stream errors, and uncaught exceptions without suppressing the original error; remove lifecycle listeners on shutdown.
  - [x] Attempt all cleanup steps even if one fails, restoring prior raw/flowing settings, stopping gravity, and showing the cursor; keep shutdown idempotent.
  - [x] Verify lifecycle cleanup across prior input states and assert a clear non-interactive launch message without terminal escape sequences. `npm test`: 63 tests passed. No validator findings were supplied.
- [ ] Add automated tests for movement and rotation collisions, locking, line clearing, scoring, spawning/game over, and the display's 24-row limit in both playing and game-over states.
- [ ] Document launch requirements and controls; run automated tests and manually smoke-test `npm start` in an interactive terminal.

## Completion criteria

All tasks are checked off, tests pass, and an interactive smoke test demonstrates a playable game started with `npm start`, with the complete display never exceeding 24 terminal rows.
