# Terminal Tetris implementation plan

## Seed requirements
- Build a playable Tetris game in the terminal.
- Launch it with `npm start`.
- Keep the entire display within 24 terminal rows, including the board, score, controls, borders, and game-over messages.
- Packages may be installed for terminal rendering or controls.

## Tasks
- [x] Set up a Node.js project with an `npm start` entry point and automated test command.
- [x] Implement game state: a 10-column, 20-row board, seven tetrominoes, randomized piece spawning, movement, rotation, collision detection, and locking.
  - [x] Add `src/game.js` with independent board rows, seven immutable shape templates, and randomized seven-bag spawning.
  - [x] Implement collision-checked movement, clockwise rotation (without wall kicks), and resting-piece locking.
  - [x] Verify state behavior with automated tests; `npm test` passes all 10 tests. No validator findings were supplied.
  - [x] Fix validator finding (`src/game.js:48-49`): rejected spawning with an active piece consumes a random tetromino. Moved `nextType()` after the active-piece guard; regression test verifies unchanged active piece, bag, random-call count (including an empty bag), and next successfully spawned piece across two seven-bags. `npm test` passes all 11 tests.
- [x] Implement automatic falling, soft and hard drop, completed-line clearing, scoring, and game-over detection when a piece cannot spawn.
  - [x] Add elapsed-time gravity (`update`/`tick`) with a default 1000 ms interval; blocked descent locks and spawns the next piece. Terminal-loop wiring remains in the controls task.
  - [x] Add soft/hard drop (1/2 points per descended cell), completed-line collapse, cumulative line count, and fixed-level line scoring (100/300/500/800).
  - [x] Detect blocked spawning as game over and stop movement, rotation, drops, and gravity afterward.
  - [x] Add nine progression regression tests; `npm test` passes all 20 tests. No validator findings were supplied.
- [x] Implement terminal rendering with the board, score, control hints, and game-over message, always fitting within 24 rows. Use a fixed layout (22 board/border rows plus two status/control rows) and update it in place without accumulating output.
  - [x] Fix validator finding (`src/render.js:13-22`): renderer reconstructed occupied cells from active-piece geometry. Moved non-mutating cell projection into `Game.visibleCells()`; renderer only translates that snapshot into glyphs. Added regression tests for independent snapshots, movement/rotation, clipping, and rendering without board/active access. `npm test` passes all 29 tests.
  - [x] Add `src/render.js` with a non-mutating 24-row snapshot showing active/locked cells, score, lines, control hints, and an inline game-over message.
  - [x] Update TTY frames at fixed cursor positions, erase stale row content, and omit trailing newlines to avoid scrolling; shorten status/hints for narrow terminals and show a bounded resize prompt for terminals smaller than 22x24.
  - [x] Connect the initial frame to `src/index.js`; interactive loop and terminal lifecycle remain in the next task.
  - [x] Add six rendering regression tests covering layout, cells, game over, narrow/undersized terminals, and in-place updates. `npm test` passes all 26 tests. No validator findings were supplied.
- [x] Connect keyboard controls for horizontal movement, rotation, soft drop, hard drop, restart after game over, and quitting. Manage raw input and restore terminal state on exit or interruption.
  - [x] Add `src/session.js` and wire `src/index.js` to a 50 ms elapsed-time gravity loop with arrow keys, Space, R (only after game over), Q, and Ctrl-C; retain a single snapshot for non-TTY execution.
  - [x] Manage raw mode, alternate screen, cursor visibility, resize events, and idempotent cleanup on quit, input end, process exit, SIGINT/SIGTERM/SIGHUP, and runtime errors; restore prior input state and remove session listeners/timers.
  - [x] Add 11 session regression tests for controls, gravity, restart, actual escape-sequence decoding, lifecycle cleanup, resize, rendering failures, and prior raw-mode restoration. Fix the restart test to compare the original spawn position rather than assuming every shape starts at column 3.
  - [x] Verify `npm test` passes all 40 tests and a 24x80 pseudo-terminal `npm start` smoke check accepts input and quits with terminal attributes and cursor/screen restored. No validator findings were supplied.
- [x] Add automated tests for movement and collision, rotation, locking, line clearing, scoring, spawning/game over, and the 24-row display limit across normal and game-over states.
  - [x] Review existing unit coverage for movement/collision, rotation, locking, one-to-four-line clears, scoring, seven-bag spawning, and blocked-spawn game over.
  - [x] Add `test/acceptance.test.js` with boundary and four-cell locking checks for every rotated tetromino, successive clears with cumulative line/drop scores, and a deterministic play-through from a Tetris clear to actual blocked-spawn game over.
  - [x] Assert 24-row snapshots and in-place TTY cursor positions at widths 22, 30, and 80 throughout play and after game over; verify controls and gravity cannot mutate the final state.
  - [x] Run `npm test`: all 50 tests pass. No validator findings were supplied.
- [ ] Document launch instructions and controls, then manually verify `npm start`, responsive input, automatic falling, line clearing, game over, restart, quitting, and terminal cleanup.
  - [x] Add `README.md` with requirements, `npm start`, terminal dimensions, controls, scoring, game-over/restart behavior, terminal cleanup, and `npm test`.
  - [x] Exercise `npm start` in an 80x24 real pseudo-terminal with scripted keyboard input: movement, rotation, gravity, soft/hard drop, actual line clearing, blocked-spawn game over, restart, and Q. Verify all rendered cursor rows remain within 1–24 and original terminal attributes/cursor/screen are restored.
  - [x] Verify terminal restoration on separate Ctrl-C and SIGTERM launches; run `npm test` (50 passing tests). Record scope and results in `.factory/verification.md`. No validator findings were supplied.

## Completion criteria
All tasks are checked off, automated tests pass, and a manual terminal session confirms playable Tetris launched by `npm start` with the complete display never exceeding 24 rows.
