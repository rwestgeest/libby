import { emitKeypressEvents } from 'node:readline';
import { Game } from './game.js';
import { drawFrame } from './render.js';

export function startGame({
  game = new Game(),
  input = process.stdin,
  output = process.stdout,
  signals = process,
  setTimer = setInterval,
  clearTimer = clearInterval,
  gravityMs = 600,
} = {}) {
  if (!game.active && !game.gameOver) game.spawn();
  // Piped launches remain finite and useful as a plain-text snapshot.
  if (!input.isTTY || !output.isTTY) {
    drawFrame(game, output);
    return () => {};
  }

  const wasRaw = Boolean(input.isRaw);
  const wasPaused = input.isPaused();
  let timer;
  let stopped = false;
  function stop() {
    if (stopped) return;
    stopped = true;
    if (timer !== undefined) clearTimer(timer);
    input.removeListener('keypress', onKey);
    input.removeListener('end', stop);
    output.removeListener('resize', redraw);
    signals.removeListener('SIGINT', onInterrupt);
    signals.removeListener('SIGTERM', onTerminate);
    signals.removeListener('exit', stop);
    try {
      input.setRawMode(wasRaw);
    } finally {
      if (wasPaused) input.pause();
      output.write('\x1b[?25h');
    }
  }
  function quit(code) {
    stop();
    signals.exit(code);
  }
  function onInterrupt() { quit(130); }
  function onTerminate() { quit(143); }
  function redraw() {
    if (stopped) return;
    try {
      drawFrame(game, output);
    } catch (error) {
      stop();
      throw error;
    }
  }
  function onKey(text, key = {}) {
    if (stopped) return;
    const name = key.name ?? text?.toLowerCase();
    if (name === 'q' || (key.ctrl && name === 'c')) {
      quit(key.ctrl ? 130 : 0);
      return;
    }
    if (game.gameOver) return;
    try {
      switch (name) {
        case 'left': game.move(-1); break;
        case 'right': game.move(1); break;
        case 'up': game.rotate(); break;
        case 'down': game.tick(); break;
        case 'space':
          while (game.move(0, 1)) { /* Drop to the last free row. */ }
          game.tick();
          break;
        default: return;
      }
      redraw();
    } catch (error) {
      stop();
      throw error;
    }
  }
  try {
    emitKeypressEvents(input);
    input.on('keypress', onKey);
    input.on('end', stop);
    output.on('resize', redraw);
    signals.on('SIGINT', onInterrupt);
    signals.on('SIGTERM', onTerminate);
    signals.on('exit', stop);
    input.setRawMode(true);
    input.resume();
    output.write('\x1b[?25l');
    redraw();
    timer = setTimer(() => {
      if (stopped || game.gameOver) return;
      try {
        game.tick();
        redraw();
      } catch (error) {
        stop();
        throw error;
      }
    }, gravityMs);
  } catch (error) {
    stop();
    throw error;
  }
  return stop;
}
