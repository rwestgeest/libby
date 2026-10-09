# Terminal Tetris plan

## Requirements

Build a playable terminal Tetris game launched with `npm start`. Keep the entire display within 24 terminal rows, including the board, score, controls, borders, and game-over messages.

## Tasks

- [x] Set up a Node.js project with an `npm start` entry point and a test command.
- [x] Implement the game model: a 10-column, 20-row board, seven tetrominoes, spawning, movement, rotation, collision detection, gravity, locking, completed-line removal, scoring, and game-over detection.
- [x] Implement terminal rendering with a fixed layout of at most 24 rows. Include the board and borders, score, controls, and an in-layout game-over message; redraw without accumulating output.
- [x] Add interactive keyboard controls for moving, rotating, dropping pieces, and quitting. Run gravity on a timer and restore terminal state on exit or interruption.
- [ ] Add automated tests covering collision boundaries, rotation, piece locking, line clearing, scoring, game over, and the 24-row display limit across gameplay and game-over states.
- [ ] Verify `npm start` in an interactive terminal, confirm controls and gameplay work, and document startup and controls.

## Completion criteria

All tasks are checked, automated tests pass, and the game is playable through `npm start` with its complete display remaining within 24 terminal rows.
