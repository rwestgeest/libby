# Terminal Tetris plan

## Status

Project/game logic and terminal input/bounded rendering tasks are completed. Tests/documentation and final validation remain unfinished. The target now has a Node.js project, terminal-independent game engine, and interactive terminal interface. This plan was derived from `/home/rob/dev/libby/tetris/spec.md`. The designated factory plan does not exist; this local copy stays within the permitted target directory.

## Requirements

- Implement a playable terminal Tetris game.
- Launch with `npm start`.
- Keep the complete display within 24 terminal rows, including the board, score, controls, borders, and game-over messages.
- Packages may be installed for terminal rendering or input.

## Implementation steps

1. Create a Node.js project with an `npm start` entry point and a test command.
2. Implement game state independently of terminal I/O: a 10-by-20 board, seven tetrominoes, spawning, movement, rotation, collision detection, gravity, locking, line clearing, scoring, and game-over detection.
3. Add interactive terminal input for left/right movement, rotation, soft drop, hard drop, restart, and quit. Document these keys in the display.
4. Render a fixed-height screen: one score/status row, one top border, 20 board rows, one bottom border, and one controls row (24 total). Put game-over and restart instructions in existing status/control rows, never additional rows. Suppress terminal wrapping; show a compact size warning instead of the board when the terminal is too small.
5. Manage terminal lifecycle: raw input, cursor visibility, redraws, resize handling, and cleanup on quit, interruption, or error. Handle non-interactive input with a clear diagnostic.
6. Add automated tests for collisions, rotations, locking, line clears, scoring, spawn obstruction, and rendered row count for playing and game-over states. Verify terminal cleanup and keyboard controls in an interactive smoke test.
7. Add concise launch and control instructions, then run tests and an `npm start` smoke test.

## Completion criteria

- `npm start` starts a playable game from this target directory.
- Movement, rotation, gravity, drops, line clearing, scoring, game over, restart, and quit work.
- All full-game frames occupy no more than 24 terminal rows without wrapping.
- Automated tests pass and an interactive smoke test confirms usable input and terminal restoration.

## Progress

- [x] Read seed and prepare plan.
- [x] Create project and game logic.
  - [x] Add dependency-free Node.js project, `npm start` entry point, and `npm test` command.
  - [x] Implement 10-by-20 board, seven-piece bag, movement, rotation with horizontal wall kicks, collisions, gravity, locking, drops, line clearing, scoring, levels, game over, and restart.
  - [x] Add 12 engine tests; `npm test` passes all 12.
  - [x] Smoke-test `npm start`; it loads the engine and reports that the terminal interface is pending the next task.
  - Validator findings: none supplied.
- [x] Implement terminal input and bounded rendering.
  - [x] Add arrow-key movement, clockwise/reverse rotation, soft/hard drops, restart, quit, and on-screen controls.
  - [x] Render exactly 24 rows for playing/game-over states; disable wrapping and use a clipped warning below the minimum terminal size (61 columns by 24 rows).
  - [x] Add gravity scheduling, resize handling, alternate-screen/cursor/raw-mode restoration, signal/error cleanup, and a non-interactive diagnostic.
  - [x] Add six terminal tests; all 18 tests pass with `npm test`.
  - [x] Fix PTY smoke finding: quitting restored the screen but initially left stdin flowing, preventing process exit. Restore previously inactive stdin and assert it is paused after quit.
  - [x] Verify `npm start` through a real PTY: 24-row frame, raw input, arrow sequences, hard drop, restart, small-size warning, clean quit, and original terminal attributes restored.
  - Validator findings: none supplied.
- [ ] Add tests and documentation.
- [ ] Validate all completion criteria.
