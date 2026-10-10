import { emitKeypressEvents } from 'node:readline';
import { Game, HEIGHT, WIDTH } from './game.js';

export const CONTROLS = 'Arrows:move Up/X:rotate Z:reverse Space:drop R:restart Q:quit';
export const MIN_COLUMNS = Math.max(WIDTH * 2 + 2, CONTROLS.length);
export const MIN_ROWS = HEIGHT + 4;

/** Plain, bounded rows: no newline after the final row, even on a 24-row TTY. */
export function renderRows(game, columns, rows) {
  columns = Math.max(0, Math.floor(columns || 0));
  rows = Math.max(0, Math.floor(rows || 0));
  if (!columns || !rows) return [];
  if (columns < MIN_COLUMNS || rows < MIN_ROWS) {
    return [`Resize to ${MIN_COLUMNS}x${MIN_ROWS}; Q:quit`.slice(0, columns)];
  }
  const status = `${game.gameOver ? 'GAME OVER | ' : ''}Score:${game.score} Lines:${game.lines} Level:${game.level}${game.gameOver ? ' | R:restart' : ''}`;
  const border = `+${'-'.repeat(WIDTH * 2)}+`;
  return [
    status.slice(0, columns),
    border,
    ...game.displayBoard().map(row => `|${row.map(cell => cell ? '[]' : '  ').join('')}|`),
    border,
    CONTROLS,
  ];
}

export function handleKey(game, key = {}) {
  if ((key.ctrl && key.name === 'c') || key.name === 'q' || key.name === 'escape') return 'quit';
  switch (key.name) {
    case 'left': case 'a': game.move(-1); break;
    case 'right': case 'd': game.move(1); break;
    case 'down': case 's': game.softDrop(); break;
    case 'up': case 'x': game.rotate(); break;
    case 'z': game.rotate(false); break;
    case 'space': game.hardDrop(); break;
    case 'r': game.restart(); return 'restart';
    default: return 'ignored';
  }
  return 'changed';
}

// Alternate screen preserves the shell; disabling autowrap protects the last row.
export const ENTER_SCREEN = '\x1b[?1049h\x1b[?25l\x1b[?7l\x1b[2J';
export const LEAVE_SCREEN = '\x1b[?7h\x1b[?25h\x1b[?1049l';

export function startTerminal({
  input = process.stdin,
  output = process.stdout,
  runtime = process,
  game = new Game(),
} = {}) {
  if (!input.isTTY || !output.isTTY || typeof input.setRawMode !== 'function') {
    throw new Error('Tetris needs an interactive terminal. Run npm start in a terminal (no pipes).');
  }
  const wasRaw = Boolean(input.isRaw);
  // A fresh stdin is not explicitly paused, but also was not flowing. Restore
  // that inactive state so the readline data listener cannot keep Node alive.
  const wasInactive = input.readableFlowing !== true;
  let timer;
  let stopped = false;
  let screenEntered = false;
  const fits = () => output.columns >= MIN_COLUMNS && output.rows >= MIN_ROWS;
  const draw = () => {
    const rows = renderRows(game, output.columns, output.rows);
    output.write('\x1b[H\x1b[2J' + rows.map((row, i) => `\x1b[${i + 1};1H${row}`).join(''));
  };
  const schedule = () => {
    clearTimeout(timer);
    if (!stopped && !game.gameOver && fits()) {
      timer = setTimeout(() => {
        try {
          game.tick();
          draw();
          schedule();
        } catch (error) { fail(error); }
      }, game.gravityMs);
    }
  };
  const stop = () => {
    if (stopped) return;
    stopped = true;
    clearTimeout(timer);
    input.removeListener('keypress', onKey);
    output.removeListener('resize', onResize);
    input.removeListener('error', fail);
    output.removeListener('error', fail);
    runtime.removeListener('SIGINT', interrupt);
    runtime.removeListener('SIGTERM', terminate);
    runtime.removeListener('exit', stop);
    runtime.removeListener('uncaughtException', fail);
    try { input.setRawMode(wasRaw); } finally {
      if (wasInactive) input.pause();
      if (screenEntered) output.write(LEAVE_SCREEN);
    }
  };
  const fail = error => {
    stop();
    runtime.exitCode = 1;
    runtime.stderr.write(`Tetris: ${error.message}\n`);
  };
  const interrupt = () => { stop(); runtime.exitCode = 130; };
  const terminate = () => { stop(); runtime.exitCode = 143; };
  const onKey = (_text, key) => {
    try {
      // Quit/restart work at any size; pause other controls while too small.
      if (!fits() && !['q', 'escape', 'c', 'r'].includes(key?.name)) return;
      const result = handleKey(game, key);
      if (result === 'quit') return stop();
      if (result !== 'ignored') draw();
      if (result === 'restart') schedule();
    } catch (error) { fail(error); }
  };
  const onResize = () => {
    try { draw(); schedule(); } catch (error) { fail(error); }
  };
  try {
    emitKeypressEvents(input);
    input.setRawMode(true);
    input.on('keypress', onKey);
    input.on('error', fail);
    output.on('error', fail);
    output.on('resize', onResize);
    runtime.on('SIGINT', interrupt);
    runtime.on('SIGTERM', terminate);
    runtime.on('exit', stop);
    runtime.on('uncaughtException', fail);
    screenEntered = true;
    output.write(ENTER_SCREEN);
    input.resume();
    draw();
    schedule();
  } catch (error) {
    stop();
    throw error;
  }
  return { game, stop };
}
