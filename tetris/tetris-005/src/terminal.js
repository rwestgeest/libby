import { emitKeypressEvents } from 'node:readline';
import { BOARD_HEIGHT, BOARD_WIDTH, Game } from './game.js';
import { startGravity } from './gravity.js';

export const CONTROLS = '←/→ move  ↑ rotate  ↓ drop  Space slam  R restart  Q quit';

// Twenty board rows, two borders, one status row and one controls row.
export const DISPLAY_ROWS = BOARD_HEIGHT + 4;

// No trailing newline: the controls occupy the last row without scrolling.
export function render(game) {
  const cells = game.board.map(row => row.map(cell => cell ? '[]' : '  '));
  if (game.active) {
    const { matrix, x, y } = game.active;
    matrix.forEach((row, dy) => row.forEach((cell, dx) => {
      if (cell) cells[y + dy][x + dx] = '[]';
    }));
  }
  const border = `+${'-'.repeat(BOARD_WIDTH * 2)}+`;
  return [
    border,
    ...cells.map(row => `|${row.join('')}|`),
    border,
    `Score: ${game.score}  Lines: ${game.lines}${game.gameOver ? '  GAME OVER — R restart' : ''}`,
    CONTROLS,
  ].join('\n');
}

export function startTerminal({
  input = process.stdin,
  output = process.stdout,
  createGame = () => new Game(),
  gravity = startGravity,
  onQuit = () => {},
} = {}) {
  let game = createGame();
  let stopped = false;
  // Explicit carriage returns also keep rows aligned when raw-mode output
  // does not translate newlines. Do not advance past the 24th row.
  const draw = () => output.write(`\x1b[H\x1b[2J${render(game).replaceAll('\n', '\r\n')}`);
  let stopGravity = () => {};
  const previousRaw = Boolean(input.isRaw);
  const previousFlowing = input.readableFlowing;

  const quit = () => {
    if (stopped) return;
    stopped = true;
    stopGravity();
    input.removeListener('keypress', onKey);
    input.setRawMode(previousRaw);
    if (previousFlowing) input.resume();
    else input.pause();
    output.write('\x1b[?25h\n');
    onQuit();
  };

  function fail(error) {
    quit();
    throw error;
  }

  function guarded(action) {
    try { return action(); }
    catch (error) { return fail(error); }
  }

  const beginGravity = () => gravity(game, {
    onUpdate: () => {
      if (!stopped) guarded(draw);
    },
    onError: fail,
  });

  function onKey(text, key = {}) {
    if (stopped) return;
    guarded(() => handleKey(text, key));
  }

  function handleKey(text, key) {
    const name = key.name ?? text?.toLowerCase();
    if (name === 'q' || (key.ctrl && name === 'c')) {
      quit();
      return;
    }
    if (name === 'r') {
      stopGravity();
      game = createGame();
      stopGravity = beginGravity();
      draw();
      return;
    }
    if (game.gameOver) return;
    switch (name) {
      case 'left': game.move(-1, 0); break;
      case 'right': game.move(1, 0); break;
      case 'up': game.rotate(); break;
      case 'down': game.step(); break;
      case 'space':
        while (game.move(0, 1)) { /* Move to the last legal row. */ }
        game.step(); // Lock, clear rows and spawn exactly one new piece.
        game.gravityElapsed = 0;
        break;
      default: return;
    }
    if (game.gameOver) stopGravity();
    draw();
  }

  guarded(() => {
    emitKeypressEvents(input);
    input.on('keypress', onKey);
    input.setRawMode(true);
    input.resume();
    output.write('\x1b[?25l');
    stopGravity = beginGravity();
    draw();
  });
  return { quit, get game() { return game; } };
}
