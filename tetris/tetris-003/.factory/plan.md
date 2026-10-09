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
- [x] Add automated tests for core game rules and the rendering row limit, including game-over output.
  - [x] Test all seven pieces in all four orientations against boundaries and settled cells.
  - [x] Test a four-line clear through gravity and verify the rendered score and cleared board.
  - [x] Test deterministic gravity through actual game over, checking every frame's 24-row limit and final output.
  - [x] Run `npm test`: all 39 tests passed.
  - Validator findings: none.
- [ ] Run tests and manually verify `npm start`, playable controls, scoring, game over, clean exit, and the 24-row display limit.
  - [x] Run `npm test`: all 39 tests passed.
  - [x] Exercise `npm start` in a 48×24 pseudo-terminal with arrow movement, rotation, soft drop, timed gravity, and Space hard drop; repeated drops reached GAME OVER.
  - [x] Verify scoring through the production keyboard loop using a controlled single-line-gap fixture: Space cleared one line and displayed Score: 100 and Lines: 1.
  - [x] Verify Q after game over exits with code 0 and Ctrl-C during play exits with code 130; both restored original terminal settings and cursor visibility.
  - [x] Check all 23 captured terminal frames across the live and fixture sessions: each used exactly 24 rows and no more than 48 columns.
  - Validator findings: none. Verification used scripted pseudo-terminal interaction, not a human visual playthrough.

## Completion criteria
All tasks above are finished, tests pass, and the game can be played in a terminal using `npm start` without exceeding 24 display rows.
