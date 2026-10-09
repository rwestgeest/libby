import { emitKeypressEvents } from 'node:readline';
import { Game } from './game.js';
import { startGravity } from './gravity.js';

export const CONTROLS = '←/→ move  ↑ rotate  ↓ drop  Space slam  R restart  Q quit';

// A compact initial display; board, status and controls share one screen.
export function render(game) {
  const cells = game.board.map(row => row.map(cell => cell ? '[]' : '  '));
  if (game.active) {
    const { matrix, x, y } = game.active;
    matrix.forEach((row, dy) => row.forEach((cell, dx) => {
      if (cell) cells[y + dy][x + dx] = '[]';
    }));
  }
  const border = `+${'-'.repeat(20)}+`;
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
  const draw = () => output.write(`\x1b[H\x1b[2J${render(game)}`);
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
