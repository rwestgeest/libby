# Terminal Tetris plan

## Goal
Build a playable terminal Tetris game launched with `npm start`. The complete display, including board, score, controls, borders, and game-over messages, must fit within 24 terminal rows.

## Tasks
- [x] Set up a Node.js project with an `npm start` entry point and a test command.
- [x] Implement the board, seven tetrominoes, piece spawning, movement, rotation, collision detection, and locking.
- [x] Implement gravity, line clearing, scoring, and game-over detection.
- [x] Implement terminal rendering with a fixed layout of at most 24 rows, including the board, score, controls, borders, and game-over messages.
- [x] Add keyboard controls for movement, rotation, dropping, and quitting; safely restore terminal state on exit or interruption.
  - [x] Wire arrow keys, Space hard drop, Q/Ctrl-C quit, and timed gravity into the terminal entry point.
  - [x] Restore raw mode, input state, and cursor visibility; remove timers/listeners on exit, interruption, or rendering failure.
  - [x] Test controls, game-over quitting, and cleanup paths (`npm test`: 30 tests passed).
  - Validator findings: none.
- [ ] Add automated tests for core game rules and the rendering row limit, including game-over output.
- [ ] Run tests and manually verify `npm start`, playable controls, scoring, game over, clean exit, and the 24-row display limit.

## Completion criteria
All tasks above are finished, tests pass, and the game can be played in a terminal using `npm start` without exceeding 24 display rows.
