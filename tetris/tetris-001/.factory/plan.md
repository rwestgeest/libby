# Terminal Tetris plan

## Goal
Build a playable terminal Tetris game launched with `npm start`. The complete display, including board, borders, score, controls, and game-over messages, must fit within 24 terminal rows.

## Tasks
- [ ] Set up a Node.js project with `npm start` and implement a testable game engine: 10×20 board, seven tetrominoes, spawning, collision detection, movement, rotation, gravity, hard drop, line clearing, scoring, and game-over detection. Add automated engine tests.
- [ ] Implement terminal rendering and interactive controls with timed gravity, left/right movement, rotation, soft/hard drop, and quit. Keep every rendered frame within 24 rows, including score, controls, and game-over status, and restore terminal state on exit.
- [ ] Add restart support and user documentation, test display size and terminal lifecycle, and verify the complete game with automated tests and an interactive terminal smoke test.

## Validation
Use Node.js's built-in test runner for engine and display tests. Verify `npm start` in a pseudo-terminal, keyboard interaction, game-over/restart behavior, graceful exit, and the 24-row display limit.
