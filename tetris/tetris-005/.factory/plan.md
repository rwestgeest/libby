# Terminal Tetris plan

Status: in progress; first three tasks completed.

## Seed requirements

- Build a playable Tetris game in the terminal.
- Launch it with `npm start`.
- Keep the entire display within 24 terminal rows, including the board, score, controls, borders, and game-over messages.
- Terminal rendering or input packages may be installed if useful.

## Implementation tasks

- [x] Create a Node.js project with an `npm start` entry point and an automated test command.
- [x] Implement a 10-column, 20-row board, the seven tetrominoes, piece spawning, movement, rotation, collision detection, and piece locking.
- [x] Implement timed gravity, completed-row clearing, score updates, and game over when a new piece cannot spawn.
- [ ] Implement terminal keyboard input for left/right movement, rotation, soft drop, hard drop, restart, and quit; show the controls on screen.
- [ ] Render the board and all supporting information in a fixed layout of at most 24 rows: 22 rows for board and borders, one for score/status, and one for controls. Show game-over information in the status row rather than adding rows.
- [ ] Restore terminal input settings and cursor visibility on quit, interruption, and errors; handle unsupported non-interactive input clearly.
- [ ] Add automated tests for movement and rotation collisions, locking, line clearing, scoring, spawning/game over, and the display's 24-row limit in both playing and game-over states.
- [ ] Document launch requirements and controls; run automated tests and manually smoke-test `npm start` in an interactive terminal.

## Completion criteria

All tasks are checked off, tests pass, and an interactive smoke test demonstrates a playable game started with `npm start`, with the complete display never exceeding 24 terminal rows.
