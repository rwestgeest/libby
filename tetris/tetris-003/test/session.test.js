import assert from 'node:assert/strict';
import { EventEmitter } from 'node:events';
import { test } from 'node:test';
import { Game } from '../src/game.js';
import { startSession } from '../src/session.js';

function fixture(overrides = {}) {
  const input = new EventEmitter();
  Object.assign(input, {
    isTTY: true, isRaw: false,
    setRawMode(raw) { this.isRaw = raw; },
    resume() { this.resumed = true; },
    pause() { this.resumed = false; },
  });
  const output = new EventEmitter();
  output.isTTY = true;
  output.text = '';
  output.write = text => { output.text += text; };
  const host = new EventEmitter();
  const game = new Game({ random: () => 0 });
  let tick;
  let cancellations = 0;
  let frames = 0;
  const stop = startSession({
    input, output, host, game,
    prepareInput() {},
    schedule(callback, ms) { assert.equal(ms, 700); tick = callback; return 42; },
    cancel(id) { assert.equal(id, 42); cancellations++; },
    draw() { frames++; },
    ...overrides,
  });
  return {
    input, output, host, game, stop,
    key(name, extra = {}) { input.emit('keypress', undefined, { name, ...extra }); },
    tick() { tick(); },
    get cancellations() { return cancellations; },
    get frames() { return frames; },
  };
}

test('arrow keys rotate and move; down and space drop; timer supplies gravity', () => {
  const f = fixture();
  try {
    const x = f.game.active.x;
    f.key('left');
    assert.equal(f.game.active.x, x - 1);
    f.key('right');
    assert.equal(f.game.active.x, x);
    const shape = f.game.active.shape;
    f.key('up');
    assert.notDeepEqual(f.game.active.shape, shape);
    f.key('down');
    assert.equal(f.game.active.y, 1);
    assert.equal(f.game.score, 1);
    f.tick();
    assert.equal(f.game.active.y, 2);
    f.key('space');
    assert.ok(f.game.board.some(row => row.some(Boolean)));
    assert.equal(f.game.active.y, 0);
    assert.ok(f.game.score > 1);
  } finally { f.stop(); }
});

test('quit restores terminal and input, removes listeners, and cancels gravity once', () => {
  const f = fixture();
  assert.equal(f.input.isRaw, true);
  assert.match(f.output.text, /\x1b\[\?1049h/);
  f.key('q');
  f.stop();
  assert.equal(f.input.isRaw, false);
  assert.equal(f.input.resumed, false);
  assert.equal(f.input.listenerCount('keypress'), 0);
  assert.equal(f.host.eventNames().length, 0);
  assert.equal(f.cancellations, 1);
  assert.ok(f.output.text.endsWith('\x1b[?7h\x1b[?25h\x1b[?1049l'));
});

for (const [signal, code] of [['SIGINT', 130], ['SIGTERM', 143], ['SIGHUP', 129]]) {
  test(`${signal} restores terminal and sets exit status`, () => {
    const f = fixture();
    f.host.emit(signal);
    assert.equal(f.host.exitCode, code);
    assert.equal(f.input.isRaw, false);
    assert.equal(f.cancellations, 1);
  });
}

test('raw-mode Ctrl-C exits safely', () => {
  const f = fixture();
  f.key('c', { ctrl: true });
  assert.equal(f.host.exitCode, 130);
  assert.equal(f.input.isRaw, false);
});

test('game over stops gravity but keeps the final frame and quit controls', () => {
  const f = fixture();
  f.game.gameOver = true;
  f.tick();
  assert.equal(f.cancellations, 1);
  const frames = f.frames;
  f.key('left');
  assert.equal(f.frames, frames);
  assert.equal(f.input.isRaw, true);
  f.key('q');
  assert.equal(f.input.isRaw, false);
  assert.equal(f.cancellations, 1);
});

test('uncaught errors and input closure restore the terminal', () => {
  for (const event of ['uncaughtExceptionMonitor', 'exit', 'end', 'error']) {
    const f = fixture();
    (event === 'end' || event === 'error' ? f.input : f.host).emit(event, new Error('test'));
    assert.equal(f.input.isRaw, false);
    assert.equal(f.cancellations, 1);
  }
});

test('startup failures restore raw mode before rethrowing', () => {
  const input = new EventEmitter();
  Object.assign(input, {
    isTTY: true, isRaw: false,
    setRawMode(raw) { this.isRaw = raw; }, resume() {}, pause() {},
  });
  const output = new EventEmitter();
  Object.assign(output, { isTTY: true, write() {} });
  const host = new EventEmitter();
  assert.throws(() => startSession({ input, output, host, prepareInput() {},
    draw() { throw new Error('render failed'); },
  }), /render failed/);
  assert.equal(input.isRaw, false);
  assert.equal(host.eventNames().length, 0);
});
