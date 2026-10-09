import assert from 'node:assert/strict';
import { EventEmitter } from 'node:events';
import { PassThrough } from 'node:stream';
import test from 'node:test';
import { startSession } from '../src/session.js';

function fixture(options = {}) {
  const input = new PassThrough();
  input.isTTY = true;
  input.isRaw = false;
  input.setRawMode = value => { input.isRaw = value; };
  const output = new EventEmitter();
  Object.assign(output, { isTTY: true, columns: 80, rows: 24, text: '',
    write(text) { this.text += text; } });
  const host = new EventEmitter();
  host.stderr = { write(text) { host.error = text; } };
  let time = 0;
  let tick;
  let canceled = 0;
  const session = startSession({ input, output, host,
    now: () => time,
    schedule(callback) { tick = callback; return 42; },
    cancel(id) { assert.equal(id, 42); canceled++; },
    ...options,
  });
  return { input, output, host, session,
    key(name, text) { input.emit('keypress', text, { name }); },
    advance(ms) { time += ms; tick(); },
    get canceled() { return canceled; },
  };
}

test('arrows, space and elapsed-time gravity control the game', t => {
  const f = fixture();
  t.after(() => f.session.stop());
  const { game } = f.session;
  const { x, y } = game.active;
  f.key('left');
  assert.equal(game.active.x, x - 1);
  f.key('right');
  assert.equal(game.active.x, x);
  const shape = game.active.shape;
  f.key('up');
  assert.notEqual(game.active.shape, shape);
  f.key('down');
  assert.equal(game.active.y, y + 1);
  assert.equal(game.score, 1);
  f.advance(999);
  assert.equal(game.active.y, y + 1);
  f.advance(1);
  assert.equal(game.active.y, y + 2);
  f.key('space');
  assert.ok(game.board.flat().some(Boolean));
  assert.equal(game.active.y, 0);
  assert.ok(game.score > 1);
});

test('restart is allowed only after game over and resets state and gravity', t => {
  const f = fixture();
  t.after(() => f.session.stop());
  const old = f.session.game;
  f.key('r');
  assert.equal(f.session.game, old);
  old.gameOver = true;
  const oldX = old.active.x;
  old.score = 123;
  f.advance(800);
  f.key('left');
  assert.equal(old.active.x, oldX);
  f.key('r');
  assert.notEqual(f.session.game, old);
  assert.equal(f.session.game.score, 0);
  assert.equal(f.session.game.gameOver, false);
  f.advance(999);
  assert.equal(f.session.game.active.y, 0);
  f.advance(1);
  assert.equal(f.session.game.active.y, 1);
});

test('real keypress decoder handles escape sequences, space, and Ctrl-C', () => {
  const f = fixture();
  const x = f.session.game.active.x;
  f.input.write('\x1b[D');
  assert.equal(f.session.game.active.x, x - 1);
  f.input.write(' ');
  assert.ok(f.session.game.score > 0);
  f.input.write('\x03');
  assert.equal(f.canceled, 1);
  assert.equal(f.input.isRaw, false);
  assert.equal(f.input.isPaused(), true);
  assert.ok(f.output.text.endsWith('\x1b[?25h\x1b[?1049l'));
});

for (const event of ['q', 'SIGINT', 'SIGTERM', 'SIGHUP', 'exit', 'end']) {
  test(`${event} cleans up the terminal and listeners exactly once`, () => {
    const f = fixture();
    assert.equal(f.input.isRaw, true);
    assert.ok(f.output.text.startsWith('\x1b[?1049h\x1b[?25l'));
    if (event === 'q') f.key('q');
    else if (event === 'end') f.input.emit('end');
    else f.host.emit(event);
    f.session.stop();
    assert.equal(f.canceled, 1);
    assert.equal(f.input.isRaw, false);
    assert.equal(f.input.isPaused(), true);
    assert.equal(f.input.listenerCount('keypress'), 0);
    assert.equal(f.output.listenerCount('resize'), 0);
    assert.equal(f.host.listenerCount('SIGINT'), 0);
    assert.equal(f.host.listenerCount('exit'), 0);
    assert.ok(f.output.text.endsWith('\x1b[?25h\x1b[?1049l'));
  });
}

test('resize redraws and rendering failures restore terminal state', () => {
  let frames = 0;
  const f = fixture({ renderer: { render() {
    if (++frames === 3) throw new Error('render failed');
  } } });
  f.output.emit('resize');
  assert.equal(frames, 2);
  f.advance(50);
  assert.equal(f.host.exitCode, 1);
  assert.match(f.host.error, /render failed/);
  assert.equal(f.input.isRaw, false);
  assert.equal(f.canceled, 1);
});

test('cleanup preserves preexisting raw mode', () => {
  const input = new PassThrough();
  input.isTTY = true;
  input.isRaw = true;
  input.setRawMode = value => { input.isRaw = value; };
  input.pause();
  const f = fixture({ input });
  f.session.stop();
  assert.equal(input.isRaw, true);
  assert.equal(input.isPaused(), true);
});
