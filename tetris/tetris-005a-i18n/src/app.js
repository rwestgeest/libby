import { emitKeypressEvents } from 'node:readline';
import { Game } from './engine.js';
import { render, terminalFits } from './render.js';
import { english } from './messages.js';

export function startApp({ input = process.stdin, output = process.stdout, game = new Game(), messages = english } = {}) {
  if (!input.isTTY || !output.isTTY || typeof input.setRawMode !== 'function') {
    throw new Error(messages.interactiveRequired);
  }
  let paused = false;
  let stopped = false;
  let timer;
  let screenEntered = false;
  const wasRaw = Boolean(input.isRaw);
  // A fresh stdin has readableFlowing === null, not isPaused() === true.
  // Do not leave it resumed on quit: that would keep the CLI alive.
  const wasPaused = input.readableFlowing !== true;
  const size = () => ({ columns: output.columns ?? 80, rows: output.rows ?? 24 });
  const playable = () => {
    const { columns, rows } = size();
    return !paused && !game.gameOver && terminalFits(columns, rows);
  };
  const draw = () => {
    // Explicit cursor positioning avoids newline translation and bottom-row scrolling.
    const lines = render(game, { paused, messages, ...size() });
    output.write('\x1b[H\x1b[2J' + lines.map((line, i) => `\x1b[${i + 1};1H${line}`).join(''));
  };
  const schedule = () => {
    clearTimeout(timer);
    if (!stopped && playable()) timer = setTimeout(() => {
      game.tick();
      draw();
      schedule();
    }, game.gravityMs);
  };
  function stop() {
    if (stopped) return;
    stopped = true;
    clearTimeout(timer);
    input.removeListener('keypress', onKey);
    input.removeListener('end', stop);
    output.removeListener('resize', onResize);
    process.removeListener('exit', stop);
    for (const signal of Object.keys(signals)) process.removeListener(signal, signals[signal]);
    try {
      input.setRawMode(wasRaw);
      if (wasPaused) input.pause();
    } finally {
      if (screenEntered) output.write('\x1b[0m\x1b[?25h\x1b[?1049l');
    }
  }
  const signals = Object.fromEntries(['SIGINT', 'SIGTERM', 'SIGHUP'].map((signal, i) =>
    [signal, () => { stop(); process.exit( [130, 143, 129][i]); }]));
  function onResize() { draw(); schedule(); }
  function onKey(text, key = {}) {
    const name = (key.name ?? text ?? '').toLowerCase();
    if (name === 'q' || (key.ctrl && name === 'c')) { stop(); return; }
    if (name === 'r') {
      game.reset(); paused = false; draw(); schedule(); return;
    }
    if (name === 'p' && !game.gameOver) {
      paused = !paused; draw(); schedule(); return;
    }
    if (!playable()) return;
    const active = game.active;
    switch (name) {
      case 'left': case 'a': game.move(-1); break;
      case 'right': case 'd': game.move(1); break;
      case 'up': case 'w': case 'x': game.rotate(); break;
      case 'z': game.rotate(-1); break;
      case 'down': case 's': game.softDrop(); break;
      case 'space': case ' ': game.hardDrop(); break;
      default: return;
    }
    draw();
    // A newly spawned piece gets a full gravity interval, but moving/rotating
    // cannot postpone gravity indefinitely.
    if (game.gameOver || (game.active !== active && game.active?.y === 0 &&
        (name === 'space' || name === ' ' || name === 'down' || name === 's'))) schedule();
  }
  try {
    emitKeypressEvents(input);
    input.setRawMode(true);
    input.resume();
    input.on('keypress', onKey);
    input.on('end', stop);
    output.on('resize', onResize);
    process.on('exit', stop);
    for (const signal of Object.keys(signals)) process.on(signal, signals[signal]);
    screenEntered = true;
    output.write('\x1b[?1049h\x1b[?25l');
    draw();
    schedule();
  } catch (error) {
    stop();
    throw error;
  }
  return { stop, game };
}
