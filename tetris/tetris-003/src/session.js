import { emitKeypressEvents } from 'node:readline';
import { Game } from './game.js';
import { drawFrame } from './render.js';

export function startSession({
  game = new Game(),
  input = process.stdin,
  output = process.stdout,
  host = process,
  gravityMs = 700,
  schedule = setInterval,
  cancel = clearInterval,
  prepareInput = emitKeypressEvents,
  draw = drawFrame,
} = {}) {
  // Piped invocations remain finite and do not emit terminal control codes.
  if (!input.isTTY || !output.isTTY) {
    draw(game, output);
    return () => {};
  }

  const wasRaw = Boolean(input.isRaw);
  let timer;
  let stopped = false;
  const signals = { SIGINT: 130, SIGTERM: 143, SIGHUP: 129 };
  const handlers = new Map();

  function stop() {
    if (stopped) return;
    stopped = true;
    if (timer !== undefined) cancel(timer);
    input.removeListener('keypress', onKey);
    input.removeListener('end', stop);
    input.removeListener('error', onError);
    output.removeListener('error', onError);
    host.removeListener('exit', stop);
    host.removeListener('uncaughtExceptionMonitor', stop);
    for (const [signal, handler] of handlers) host.removeListener(signal, handler);
    // Restore the input mode even if terminal output is no longer writable.
    try {
      input.setRawMode(wasRaw);
    } finally {
      input.pause();
      output.write('\x1b[?7h\x1b[?25h\x1b[?1049l');
    }
  }

  function onError() {
    host.exitCode = 1;
    stop();
  }

  function redraw() {
    draw(game, output);
    if (game.gameOver && timer !== undefined) {
      cancel(timer);
      timer = undefined;
    }
  }

  function onKey(text, key = {}) {
    if (stopped) return;
    if (key.name === 'q' || text?.toLowerCase() === 'q' ||
        (key.ctrl && key.name === 'c') || text === '\u0003') {
      if (key.ctrl && key.name === 'c' || text === '\u0003') host.exitCode = 130;
      stop();
      return;
    }
    if (game.gameOver) return;
    switch (key.name) {
      case 'left': game.move(-1); break;
      case 'right': game.move(1); break;
      case 'up': game.rotate(); break;
      case 'down': game.softDrop(); break;
      case 'space': game.hardDrop(); break;
      default: return;
    }
    redraw();
  }

  try {
    prepareInput(input);
    input.on('keypress', onKey);
    input.on('end', stop);
    input.on('error', onError);
    output.on('error', onError);
    host.on('exit', stop);
    host.on('uncaughtExceptionMonitor', stop);
    for (const [signal, code] of Object.entries(signals)) {
      const handler = () => { host.exitCode = code; stop(); };
      handlers.set(signal, handler);
      host.on(signal, handler);
    }
    input.setRawMode(true);
    input.resume();
    // Alternate screen, hidden cursor, and no automatic line wrapping.
    output.write('\x1b[?1049h\x1b[?25l\x1b[?7l');
    redraw();
    if (!game.gameOver) {
      timer = schedule(() => {
        try {
          game.tick();
          redraw();
        } catch (error) {
          stop();
          throw error;
        }
      }, gravityMs);
    }
  } catch (error) {
    stop();
    throw error;
  }
  return stop;
}
