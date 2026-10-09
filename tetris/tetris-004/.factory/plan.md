# Terminal Tetris implementation plan

## Seed requirements
- Build a playable Tetris game in the terminal.
- Launch it with `npm start`.
- Keep the entire display within 24 terminal rows, including the board, score, controls, borders, and game-over messages.
- Packages may be installed for terminal rendering or controls.

## Tasks
- [ ] Set up a Node.js project with an `npm start` entry point and automated test command.
- [ ] Implement game state: a 10-column, 20-row board, seven tetrominoes, randomized piece spawning, movement, rotation, collision detection, and locking.
- [ ] Implement automatic falling, soft and hard drop, completed-line clearing, scoring, and game-over detection when a piece cannot spawn.
- [ ] Implement terminal rendering with the board, score, control hints, and game-over message, always fitting within 24 rows. Use a fixed layout (22 board/border rows plus two status/control rows) and update it in place without accumulating output.
- [ ] Connect keyboard controls for horizontal movement, rotation, soft drop, hard drop, restart after game over, and quitting. Manage raw input and restore terminal state on exit or interruption.
- [ ] Add automated tests for movement and collision, rotation, locking, line clearing, scoring, spawning/game over, and the 24-row display limit across normal and game-over states.
- [ ] Document launch instructions and controls, then manually verify `npm start`, responsive input, automatic falling, line clearing, game over, restart, quitting, and terminal cleanup.

## Completion criteria
All tasks are checked off, automated tests pass, and a manual terminal session confirms playable Tetris launched by `npm start` with the complete display never exceeding 24 rows.
