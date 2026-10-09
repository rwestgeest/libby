# Terminal Tetris

## Run

Requires Node.js 18 or newer and npm. No third-party packages are needed.
From this directory, run:

```sh
npm start
```

Use an interactive terminal (both input and output must be TTYs), at least
80 columns wide and 24 rows tall, with ANSI cursor controls and Unicode
support. Piped input/output is not supported; it prints a helpful message
instead of starting the game.

## Controls

| Key | Action |
| --- | --- |
| Left / Right arrow | Move one column |
| Up arrow | Rotate clockwise (blocked rotations stay unchanged) |
| Down arrow | Soft drop one row; lock if landed |
| Space | Hard drop and lock |
| R | Restart, including after game over |
| Q or Ctrl+C | Quit |

Pieces fall automatically every half-second. Fill a horizontal row to clear
it. Clearing 1, 2, 3, or 4 rows at once scores 100, 300, 500, or 800 points.
The game ends when a new piece cannot spawn. There are no wall kicks or
speed increases.

The display uses exactly 24 rows: 20 board rows, two borders, one score/status
row (also used for game over), and one controls row. Quitting restores the
terminal's input mode and cursor visibility.

## Checks

```sh
npm test
```

For an interactive smoke test, run `npm start` in an 80×24 terminal. Check
that gravity moves the piece, arrows move/rotate/drop it, Space locks it,
and R clears the board and resets the score. Repeatedly hard-drop pieces
until game over; ensure its message and controls remain on the same 24 rows.
Restart, quit with Q, and launch again to check Ctrl+C. After either exit,
the cursor should be visible and normal line input restored.
