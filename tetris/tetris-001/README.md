# Terminal Tetris

Requires Node.js 18 or newer; no dependencies need installing.

```sh
npm start
```

Use an interactive terminal at least **50 columns × 24 rows**. The entire
10×20 board, score, borders, controls, and game-over message fit in 24 rows.
If the terminal is smaller, a resize prompt replaces the board (gravity still
runs). The game restores the cursor and terminal mode when you quit.

## Controls

- Left / Right: move
- Up: rotate clockwise
- Down: soft drop
- Space: hard drop
- R: start a new game, including after game over
- Q or Ctrl-C: quit

Complete horizontal lines to clear them. Clearing 1–4 lines awards
100/300/500/800 points multiplied by the current level. Soft drops award one
point per cell; hard drops award two. Every ten cleared lines increases the
level and falling speed. The game ends when a new piece cannot spawn.

## Tests

```sh
npm test
```

Tests cover the game engine, display dimensions, keyboard controls, restart,
and terminal setup and cleanup. No packages or network access are required.
