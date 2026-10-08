import { emitKeypressEvents } from 'node:readline';
import { Game, WIDTH, HEIGHT } from './engine.js';

export const CONTROLS = 'Arrows:move/rotate/drop Space:slam R: reset Q:quit';
export const MIN_COLUMNS = 50;
export const SCREEN_ROWS = HEIGHT + 4;

export function render(game) {
  const cells = game.board.map(row => [...row]);
  if (!game.gameOver && game.active) {
    const { shape, x, y, type } = game.active;
    shape.forEach((row, dy) => row.forEach((cell, dx) => {
      if (cell && y + dy >= 0 && y + dy < HEIGHT && x + dx >= 0 && x + dx < WIDTH) {
        cells[y + dy][x + dx] = type;
      }
    }));
  }
  const status = game.gameOver ? 'GAME OVER' : 'Tetris';
  const border = `+${'-'.repeat(WIDTH * 2)}+`;
  return [
    `${status} | Score ${game.score} | Lines ${game.lines} | Lv ${game.level}`,
    border,
    ...cells.map(row => `|${row.map(cell => cell ? '[]' : '  ').join('')}|`),
    border,
    CONTROLS,
  ].join('\n');
}

export function handleKey(game, key = {}) {
  if (key.name === 'q' || (key.ctrl && key.name === 'c')) return 'quit';
  switch (key.name) {
    case 'r': game.reset(); return 'restart';
    case 'left': game.move(-1); break;
    case 'right': game.move(1); break;
    case 'up': game.rotate(); break;
    case 'down': game.softDrop(); break;
    case 'space': game.hardDrop(); break;
  }
}

export function startTerminal({ game = new Game(), input = process.stdin,
  output = process.stdout, signals = process } = {}) {
  if (!input.isTTY || !output.isTTY) {
    throw new Error('Tetris needs an interactive terminal. Run npm start in a terminal.');
  }
  const wasRaw = Boolean(input.isRaw);
  const wasPaused = input.readableFlowing !== true;
  let timer;
  let stopped = false;

  function draw() {
    const columns = output.columns || 80;
    const rows = output.rows || 24;
    const frame = columns < MIN_COLUMNS || rows < SCREEN_ROWS
      ? `Resize to ${MIN_COLUMNS}x${SCREEN_ROWS}. Q quits.`
      : render(game);
    // No trailing newline: writing the final row must not scroll the display.
    const visible = frame.split('\n').slice(0, rows)
      .map(line => line.slice(0, columns)).join('\r\n');
    output.write(`\x1b[H\x1b[2J${visible}`);
  }

  function stop() {
    if (stopped) return;
    stopped = true;
    clearTimeout(timer);
    input.removeListener('keypress', onKey);
    output.removeListener('resize', onResize);
    signals.removeListener('SIGINT', stop);
    signals.removeListener('SIGTERM', stop);
    signals.removeListener('exit', stop);
    try {
      input.setRawMode(wasRaw);
      if (wasPaused) input.pause();
    } finally {
      output.write('\x1b[?25h\x1b[?1049l');
    }
  }

  function safely(action) {
    try { action(); } catch (error) { stop(); throw error; }
  }

  function schedule() {
    if (stopped || game.gameOver) return;
    timer = setTimeout(() => safely(() => {
      game.tick();
      draw();
      schedule();
    }), game.gravityMs);
  }

  function onKey(_text, key) {
    safely(() => {
      const action = handleKey(game, key);
      if (action === 'quit') { stop(); return; }
      if (action === 'restart') {
        clearTimeout(timer);
        schedule();
      }
      if (game.gameOver) clearTimeout(timer);
      draw();
    });
  }
  function onResize() { safely(draw); }

  try {
    emitKeypressEvents(input);
    input.setRawMode(true);
    input.resume();
    input.on('keypress', onKey);
    output.on('resize', onResize);
    signals.on('SIGINT', stop);
    signals.on('SIGTERM', stop);
    signals.on('exit', stop);
    output.write('\x1b[?1049h\x1b[?25l');
    draw();
    schedule();
  } catch (error) {
    stop();
    throw error;
  }
  return stop;
}
