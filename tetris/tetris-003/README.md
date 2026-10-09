# Terminal Tetris

## Start

Use Node.js 20 or newer. No external packages are required.

```sh
npm start
```

Run in an interactive terminal at least **80 columns by 24 rows**. The complete game uses 24 rows and redraws in place. It restores the previous screen, cursor visibility, and input mode when you quit. Piped output prints one static frame and exits; it is not interactive.

## Controls

| Key | Action |
| --- | --- |
| Left / Right arrow | Move sideways |
| Up arrow | Rotate clockwise |
| Down arrow | Drop one row |
| Space | Drop to the bottom and lock |
| Q / Ctrl-C | Quit |

Pieces fall automatically every 700 ms. Fill a horizontal row to clear it. Clearing one, two, three, or four rows awards 100, 300, 500, or 800 points. Down awards one point per row; Space awards two points per row dropped.

When the next piece cannot spawn, **GAME OVER** appears in the status row. Press Q to exit, then run `npm start` again to play a new game.

## Tests

```sh
npm test
```

Startup was also verified through `npm start` in an 80×24 pseudo-terminal: arrow movement, rotation, both drops, gravity, locking, scoring, game over, and quitting with Q and Ctrl-C. All captured game frames contained exactly 24 rows. Both exit paths restored terminal input mode and emitted screen/cursor restoration sequences.
