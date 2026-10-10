import test from 'node:test';
import assert from 'node:assert/strict';
import { EventEmitter } from 'node:events';
import { PassThrough } from 'node:stream';
import { Game } from '../src/game.js';
import { renderRows, handleKey, startTerminal, MIN_COLUMNS, LEAVE_SCREEN } from '../src/terminal.js';

function terminal() {
  const input = new PassThrough();
  input.isTTY = true;
  input.isRaw = false;
  input.setRawMode = value => { input.isRaw = value; };
  const output = new EventEmitter();
  Object.assign(output, { isTTY: true, columns: MIN_COLUMNS, rows: 24, text: '' });
  output.write = text => { output.text += text; };
  const runtime = new EventEmitter();
  runtime.stderr = { write: text => { runtime.error = text; } };
  return { input, output, runtime };
}

test('playing and game-over frames have exactly 24 bounded rows', () => {
  const game = new Game();
  for (const gameOver of [false, true]) {
    game.gameOver = gameOver;
    const rows = renderRows(game, MIN_COLUMNS, 24);
    assert.equal(rows.length, 24);
    assert.ok(rows.every(row => row.length <= MIN_COLUMNS && !/[\r\n]/.test(row)));
    assert.match(rows[23], /R:restart Q:quit/);
    if (gameOver) assert.match(rows[0], /GAME OVER/);
  }
});

test('small terminals get a clipped single-row warning, including tiny sizes', () => {
  const game = new Game();
  for (const [columns, height] of [[MIN_COLUMNS - 1, 24], [80, 23], [1, 1]]) {
    const rows = renderRows(game, columns, height);
    assert.equal(rows.length, 1);
    assert.ok(rows[0].length <= columns);
  }
  assert.deepEqual(renderRows(game, 0, 0), []);
});

test('keyboard maps movement, both rotations, drops, restart and quit', () => {
  const calls = [];
  const game = Object.fromEntries(['move', 'rotate', 'softDrop', 'hardDrop', 'restart']
    .map(name => [name, (...args) => calls.push([name, ...args])]));
  for (const name of ['left', 'right', 'up', 'z', 'down', 'space', 'r']) handleKey(game, { name });
  assert.deepEqual(calls, [['move', -1], ['move', 1], ['rotate'], ['rotate', false], ['softDrop'], ['hardDrop'], ['restart']]);
  for (const key of [{ name: 'q' }, { name: 'escape' }, { name: 'c', ctrl: true }]) {
    assert.equal(handleKey(game, key), 'quit');
  }
});

test('terminal startup, input, resize, quit and idempotent restoration', () => {
  const env = terminal();
  const session = startTerminal(env);
  try {
    assert.equal(env.input.isRaw, true);
    assert.match(env.output.text, /\x1b\[\?7l/);
    const x = session.game.active.x;
    env.input.emit('keypress', '', { name: 'left' });
    assert.equal(session.game.active.x, x - 1);
    env.output.rows = 10;
    env.output.emit('resize');
    assert.match(env.output.text, /Resize to/);
    const y = session.game.active.y;
    env.input.emit('keypress', '', { name: 'down' });
    assert.equal(session.game.active.y, y);
    env.input.emit('keypress', '', { name: 'q' });
    assert.equal(env.input.isRaw, false);
    assert.equal(env.input.isPaused(), true);
    assert.ok(env.output.text.endsWith(LEAVE_SCREEN));
    assert.equal(env.output.listenerCount('resize'), 0);
    assert.equal(env.runtime.listenerCount('SIGINT'), 0);
    const text = env.output.text;
    session.stop();
    assert.equal(env.output.text, text);
  } finally { session.stop(); }
});

test('signals and runtime errors restore the terminal', () => {
  for (const event of ['SIGINT', 'SIGTERM', 'uncaughtException']) {
    const env = terminal();
    const session = startTerminal(env);
    try {
      env.runtime.emit(event, new Error('test failure'));
      assert.equal(env.input.isRaw, false);
      assert.ok(env.output.text.endsWith(LEAVE_SCREEN));
      assert.equal(env.runtime.exitCode, event === 'SIGINT' ? 130 : event === 'SIGTERM' ? 143 : 1);
    } finally { session.stop(); }
  }
});

test('non-interactive streams are rejected without entering raw mode', () => {
  const env = terminal();
  env.input.isTTY = false;
  assert.throws(() => startTerminal(env), /interactive terminal/);
  assert.equal(env.input.isRaw, false);
  assert.equal(env.output.text, '');
});
