import test from 'node:test';
import assert from 'node:assert/strict';
import { PassThrough } from 'node:stream';
import { EventEmitter } from 'node:events';
import { startApp } from '../src/app.js';
import { Game } from '../src/engine.js';

function fixture(t, { raw = false, paused = true } = {}) {
  t.mock.timers.enable({ apis: ['setTimeout'] });
  const input = new PassThrough();
  input.isTTY = true;
  input.isRaw = raw;
  input.setRawMode = value => { input.isRaw = value; };
  if (paused === true) input.pause(); else if (paused === false) input.resume();
  const output = new EventEmitter();
  Object.assign(output, { isTTY: true, columns: 60, rows: 22, frames: [] });
  output.write = text => { output.frames.push(text); return true; };
  const game = new Game({ random: () => 0 });
  const events = ['exit', 'SIGINT', 'SIGTERM', 'SIGHUP'];
  const listeners = events.map(event => process.listenerCount(event));
  const app = startApp({ input, output, game });
  t.after(() => { app.stop(); input.destroy(); });
  const key = (name, extra = {}) => input.emit('keypress', '', { name, ...extra });
  const clean = () => {
    assert.equal(input.isRaw, raw);
    assert.equal(input.isPaused(), paused !== false);
    assert.equal(input.listenerCount('keypress'), 0);
    assert.equal(input.listenerCount('end'), 0);
    assert.equal(output.listenerCount('resize'), 0);
    assert.deepEqual(events.map(event => process.listenerCount(event)), listeners);
    assert.equal(output.frames.at(-1), '\x1b[0m\x1b[?25h\x1b[?1049l');
    const count = output.frames.length;
    t.mock.timers.tick(10000);
    app.stop();
    assert.equal(output.frames.length, count);
  };
  return { input, output, game, app, key, clean };
}

test('keyboard actions, gravity, pause, and undersized resize suspension', t => {
  const { input, output, game, key } = fixture(t);
  assert.equal(input.isRaw, true);
  assert.ok(output.frames[0].includes('\x1b[?1049h\x1b[?25l'));
  const x = game.active.x;
  input.write('\x1b[D'); // Exercise readline decoding, not only synthetic key events.
  assert.equal(game.active.x, x - 1);
  key('d');
  assert.equal(game.active.x, x);
  const matrix = game.active.matrix;
  key('up'); key('z');
  assert.deepEqual(game.active.matrix, matrix);
  t.mock.timers.tick(400);
  key('left');
  t.mock.timers.tick(400);
  assert.equal(game.active.y, 1, 'movement must not reset gravity');
  key('s');
  assert.equal(game.score, 1);
  key('p');
  const frozen = structuredClone(game.active);
  key('space'); t.mock.timers.tick(5000);
  assert.deepEqual(game.active, frozen);
  assert.ok(output.frames.at(-1).includes('PAUSED'));
  key('p');
  output.rows = 21; output.emit('resize');
  key('down'); t.mock.timers.tick(5000);
  assert.deepEqual(game.active, frozen);
  output.rows = 22; output.emit('resize');
  t.mock.timers.tick(800);
  assert.equal(game.active.y, frozen.y + 1);
  key('space');
  assert.ok(game.board.some(row => row.some(Boolean)));
  assert.equal(game.active.y, 0);
});

test('game over disables play and gravity; restart clears the game', t => {
  const { game, key, output } = fixture(t);
  for (let i = 0; i < 30 && !game.gameOver; i++) key('space');
  assert.equal(game.gameOver, true);
  assert.ok(output.frames.at(-1).includes('GAME OVER'));
  const board = structuredClone(game.board);
  key('a'); key('p'); t.mock.timers.tick(5000);
  assert.deepEqual(game.board, board);
  key('r');
  assert.equal(game.gameOver, false);
  assert.equal(game.score, 0);
  assert.ok(game.board.every(row => row.every(cell => cell === null)));
  t.mock.timers.tick(800);
  assert.equal(game.active.y, 1);
});

for (const method of ['q', 'ctrl-c', 'end', 'stop']) {
  test(`${method} restores terminal state and removes lifecycle handlers`, t => {
    const { key, input, app, clean } = fixture(t);
    if (method === 'ctrl-c') key('c', { ctrl: true });
    else if (method === 'end') input.emit('end');
    else if (method === 'stop') app.stop();
    else key('q');
    clean();
  });
}

test('stops a fresh stdin that was neither explicitly paused nor flowing', t => {
  const { key, clean } = fixture(t, { paused: null });
  key('q'); clean();
});

test('preserves an already raw, flowing input stream', t => {
  const { app, clean } = fixture(t, { raw: true, paused: false });
  app.stop(); clean();
});

test('startup draw failure still restores terminal and handlers', t => {
  const { app, input, output, clean } = fixture(t);
  app.stop();
  const write = output.write;
  output.write = text => {
    if (text.startsWith('\x1b[H')) throw new Error('draw failed');
    return write(text);
  };
  assert.throws(() => startApp({ input, output }), /draw failed/);
  clean();
});
