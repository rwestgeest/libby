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
- [ ] Implement terminal rendering with the board, score, control hints, and game-over message, always fitting within 24 rows. Use a fixed layout (22 board/border rows plus two status/control rows) and update it in place without accumulating output.
- [ ] Connect keyboard controls for horizontal movement, rotation, soft drop, hard drop, restart after game over, and quitting. Manage raw input and restore terminal state on exit or interruption.
- [ ] Add automated tests for movement and collision, rotation, locking, line clearing, scoring, spawning/game over, and the 24-row display limit across normal and game-over states.
- [ ] Document launch instructions and controls, then manually verify `npm start`, responsive input, automatic falling, line clearing, game over, restart, quitting, and terminal cleanup.

## Completion criteria
All tasks are checked off, automated tests pass, and a manual terminal session confirms playable Tetris launched by `npm start` with the complete display never exceeding 24 rows.
