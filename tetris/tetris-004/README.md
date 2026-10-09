# Terminal Tetris

A 10-column, 20-row Tetris game for the terminal. Requires Node.js 20 or newer and npm; no additional packages are needed.

## Play

```sh
npm start
```

Use an interactive terminal at least **22 columns × 24 rows**. The board, borders, score, controls, and game-over message occupy exactly 24 rows. Smaller terminals show a resize prompt. Redirecting output prints one static snapshot instead of starting an interactive game.

| Key | Action |
| --- | --- |
| Left / Right | Move sideways |
| Up | Rotate clockwise |
| Down | Soft drop |
| Space | Hard drop and lock |
| R | Start a new game after game over |
| Q / Ctrl-C | Quit |

Pieces fall automatically once per second. Full rows disappear; clearing 1–4 rows earns 100/300/500/800 points. Soft and hard drops earn 1 and 2 points per cell, respectively. Rotations blocked by walls or other pieces are rejected (no wall kicks). The game ends when the next piece cannot spawn.

The game uses an alternate screen and restores the cursor and prior terminal input mode when quitting or receiving SIGINT, SIGTERM, or SIGHUP.

## Tests

```sh
npm test
```

Tests cover game rules, rendering within 24 rows, keyboard input, gravity, restart, and terminal lifecycle cleanup.
