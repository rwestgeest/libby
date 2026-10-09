import { emitKeypressEvents } from 'node:readline';
import { performance } from 'node:perf_hooks';
import { Game } from './game.js';
import { TerminalRenderer } from './render.js';

// Dependencies are injectable so lifecycle and timing can be tested without a TTY.
export function startSession({
  input = process.stdin,
  output = process.stdout,
  host = process,
  createGame = () => new Game(),
  renderer = new TerminalRenderer(output),
  now = () => performance.now(),
  schedule = setInterval,
  cancel = clearInterval,
  prepareInput = emitKeypressEvents,
} = {}) {
  let game = createGame();
  if (!input.isTTY || !output.isTTY) {
    renderer.render(game);
    return { get game() { return game; }, stop() {} };
  }

  const wasRaw = Boolean(input.isRaw);
  const wasFlowing = input.readableFlowing === true;
  let timer;
  let stopped = false;
  let screenEntered = false;
  let lastTime = now();
  const signals = { SIGINT: 130, SIGTERM: 143, SIGHUP: 129 };
  const signalHandlers = Object.fromEntries(Object.entries(signals).map(([signal, code]) =>
    [signal, () => { host.exitCode = code; stop(); }]));

  function stop() {
    if (stopped) return;
    stopped = true;
    if (timer !== undefined) cancel(timer);
    input.removeListener('keypress', onKey);
    input.removeListener('end', stop);
    input.removeListener('error', fail);
    output.removeListener('resize', redraw);
    output.removeListener('error', fail);
    host.removeListener('exit', stop);
    for (const [signal, handler] of Object.entries(signalHandlers)) {
      host.removeListener(signal, handler);
    }
    // Restore input even if output has closed, and vice versa.
    try {
      input.setRawMode(wasRaw);
    } finally {
      if (!wasFlowing) input.pause();
      if (screenEntered) output.write('\x1b[?25h\x1b[?1049l');
    }
  }

  function fail(error) {
    host.exitCode = 1;
    try { stop(); } finally { host.stderr.write(`Tetris: ${error.message}\n`); }
  }

  function safely(action) {
    if (stopped) return;
    try { action(); } catch (error) { fail(error); }
  }

  function redraw() {
    safely(() => renderer.render(game));
  }

  function advance() {
    const time = now();
    game.update(Math.max(0, time - lastTime));
    lastTime = time;
  }

  function onKey(text, key = {}) {
    safely(() => {
      if ((key.ctrl && key.name === 'c') || key.name === 'q' || text?.toLowerCase() === 'q') {
        stop();
        return;
      }
      advance();
      if (key.name === 'r' || text?.toLowerCase() === 'r') {
        if (game.gameOver) {
          game = createGame();
          lastTime = now();
        }
      } else {
        switch (key.name) {
          case 'left': game.move(-1); break;
          case 'right': game.move(1); break;
          case 'up': game.rotate(); break;
          case 'down': game.softDrop(); break;
          case 'space': game.hardDrop(); break;
          default: if (text === ' ') game.hardDrop();
        }
      }
      renderer.render(game);
    });
  }

  try {
    prepareInput(input);
    input.on('keypress', onKey);
    input.on('end', stop);
    input.on('error', fail);
    output.on('resize', redraw);
    output.on('error', fail);
    host.on('exit', stop);
    for (const [signal, handler] of Object.entries(signalHandlers)) host.on(signal, handler);
    input.setRawMode(true);
    input.resume();
    screenEntered = true;
    output.write('\x1b[?1049h\x1b[?25l\x1b[2J');
    renderer.render(game);
    timer = schedule(() => safely(() => {
      advance();
      renderer.render(game);
    }), 50);
  } catch (error) {
    fail(error);
  }
  return { get game() { return game; }, stop };
}
