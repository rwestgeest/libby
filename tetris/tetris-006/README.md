# Terminal Tetris

Requires Node.js 20 or newer and an interactive terminal. No packages need to be installed.

```sh
npm start
```

Use a terminal at least **61 columns × 24 rows**. The whole display occupies 24 rows, including score, borders, controls, and game-over messages. Smaller terminals show a resize warning and pause gravity; enlarge the terminal to resume.

## Controls

| Key | Action |
| --- | --- |
| Left / A, Right / D | Move sideways |
| Down / S | Soft drop |
| Up / X | Rotate clockwise |
| Z | Rotate counterclockwise |
| Space | Hard drop and lock |
| R | Restart (also during game over) |
| Q / Escape / Ctrl-C | Quit |

Complete rows disappear. Single/double/triple/four-line clears score 100/300/500/800 points times the current level. Soft drops earn one point per row and hard drops two. Every ten cleared lines increases the level and gravity speed. An obstructed spawn ends the game.

The game uses an alternate terminal screen and restores the cursor, wrapping, and input mode when exiting. Piped input/output is not supported.

## Checks

```sh
npm test
```

Tests cover game rules, rendering bounds, decoded keyboard input, gravity/resize behavior, and terminal cleanup.

For an interactive smoke test, run `npm start` in a 61×24 terminal: move with arrows, rotate with Up and Z, soft drop with Down, hard drop with Space, and restart with R. Shrink below 24 rows to see the warning, then enlarge to resume. Quit with Q and confirm the shell accepts and echoes input with a visible cursor. Relaunch and check Ctrl-C cleanup as well. Game-over messages and restart instructions must stay in the existing 24-row display.
