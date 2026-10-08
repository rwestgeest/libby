# Terminal Tetris plan

## Goal
Build a playable terminal Tetris game launched with `npm start`. The complete display, including board, borders, score, controls, and game-over messages, must fit within 24 terminal rows.

## Tasks
- [x] Set up a Node.js project with `npm start` and implement a testable game engine: 10×20 board, seven tetrominoes, spawning, collision detection, movement, rotation, gravity, hard drop, line clearing, scoring, and game-over detection. Add automated engine tests.
- [x] Implement terminal rendering and interactive controls with timed gravity, left/right movement, rotation, soft/hard drop, and quit. Keep every rendered frame within 24 rows, including score, controls, and game-over status, and restore terminal state on exit.
- [ ] Add restart support and user documentation, test display size and terminal lifecycle, and verify the complete game with automated tests and an interactive terminal smoke test.

## Progress
- Task 1 complete: dependency-free Node.js project and game engine implemented in `src/engine.js`, with 24 passing tests in `test/engine.test.js` (`npm test`). `npm start` successfully runs the temporary entry point; interactive play remains task 2.

- Task 2 complete: `src/terminal.js` renders 24-row frames, handles arrow keys/Space/Q/Ctrl-C, applies timed gravity, checks terminal size, and restores raw mode, cursor, and alternate screen on exit. `src/main.js` launches interactive play. All 27 tests pass (`npm test`); a pseudo-terminal smoke test of `npm start`, gravity, keyboard input, quit, and terminal restoration passed. Restart, documentation, and expanded lifecycle testing remain task 3.

## Validation
Use Node.js's built-in test runner for engine and display tests. Verify `npm start` in a pseudo-terminal, keyboard interaction, game-over/restart behavior, graceful exit, and the 24-row display limit.
