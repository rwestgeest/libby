# Terminal Tetris

```sh
npm ci
npm start
```

Use Node.js 18+ (Node.js 22+ recommended) and an interactive ANSI-compatible terminal with **at least 60 columns and 22 rows**. Redirected input/output is not supported. The complete game occupies 22 rows, including borders, score, preview, controls, and status—within the 24-row limit. Shrinking the terminal suspends play and shows a resize message; enlarging it resumes play unless paused.

## Controls

| Key | Action |
| --- | --- |
| Left / Right or A / D | Move |
| Up, W, or X | Rotate clockwise |
| Z | Rotate counterclockwise |
| Down or S | Soft drop |
| Space | Hard drop and lock |
| P | Pause / resume |
| R | Restart (also after game over) |
| Q or Ctrl-C | Quit |

Fill horizontal rows to clear them. Clearing 1/2/3/4 rows earns 100/300/500/800 points multiplied by the current level. Soft drops earn one point per row; hard drops earn two. Every ten cleared rows increases the level and gravity speed. Pieces come from shuffled seven-piece bags; rotations use simple horizontal wall kicks, not full guideline SRS. A blocked spawn or locking above the board ends the game.

Quit and handled interruptions restore input mode, cursor visibility, and the previous screen. As with other terminal programs, forced termination (SIGKILL) cannot run cleanup.

## Validation

```sh
npm test                       # Node.js 22+ for mock timer tests
python3 test/pty-smoke.py        # Optional: POSIX + Python 3
```

Automated tests cover game rules, keyboard decoding, gravity, pause/restart, resize suspension, terminal cleanup, and display bounds for every preview and game state. The pseudo-terminal smoke test launches `npm start` at 60×22, plays through game over/restart/quit, and checks SIGINT/SIGTERM/SIGHUP cleanup and restored terminal attributes.

## Messages

English text is centralized in `src/messages.js`. Replace that catalogue or pass a compatible `messages` object to `startApp`/`render` for localization; functions receive score, level, and size parameters. Messages are sanitized to single lines and clipped by terminal columns without splitting grapheme clusters. Keep translations short to avoid clipping controls. There is no command-line language selector.
