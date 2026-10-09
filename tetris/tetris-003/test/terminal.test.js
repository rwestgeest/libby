import assert from 'node:assert/strict';
import { EventEmitter } from 'node:events';
import { PassThrough } from 'node:stream';
import test from 'node:test';
import { Game } from '../src/game.js';
import { startGame } from '../src/terminal.js';

function session({ raw = false } = {}) {
  const input = new PassThrough();
  input.isTTY = true;
  input.isRaw = raw;
  input.setRawMode = value => { input.isRaw = value; };
  input.pause();
  const output = new EventEmitter();
  Object.assign(output, { isTTY: true, columns: 80, rows: 24 });
  let display = '';
  output.write = text => { display += text; };
  const signals = new EventEmitter();
  const exits = [];
  signals.exit = code => exits.push(code);
  const game = new Game();
  game.spawn('T');
  let tick;
  let cleared = 0;
  const stop = startGame({ game, input, output, signals,
    setTimer: callback => { tick = callback; return 42; },
    clearTimer: id => { assert.equal(id, 42); cleared++; },
  });
  return { input, output, signals, game, exits, stop,
    key: name => input.emit('keypress', '', { name }),
    tick: () => tick(),
    display: () => display,
    cleared: () => cleared,
  };
}

test('keyboard movement, rotation, soft/hard drop and gravity are playable', () => {
  const s = session();
  try {
    const x = s.game.active.x;
    s.key('left');
    assert.equal(s.game.active.x, x - 1);
    s.key('right');
    assert.equal(s.game.active.x, x);
    const shape = s.game.active.shape;
    s.key('up');
    assert.notDeepEqual(s.game.active.shape, shape);
    s.key('down');
    assert.equal(s.game.active.y, 1);
    s.tick();
    assert.equal(s.game.active.y, 2);
    s.key('space');
    assert.equal(s.game.board.flat().filter(Boolean).length, 4);
    assert.equal(s.game.active.y, 0);
    assert.match(s.display(), /Terminal Tetris/);
  } finally { s.stop(); }
});

for (const action of ['q', 'ctrl-c', 'SIGINT', 'SIGTERM', 'exit', 'end']) {
  test(`${action} restores terminal and removes game listeners`, () => {
    const s = session();
    assert.equal(s.input.isRaw, true);
    assert.match(s.display(), /\x1b\[\?25l/);
    if (action === 'q') s.key('q');
    else if (action === 'ctrl-c') s.input.emit('keypress', '\x03', { name: 'c', ctrl: true });
    else if (action === 'end') s.input.emit('end');
    else s.signals.emit(action);
    s.stop(); // Cleanup is idempotent.
    assert.equal(s.input.isRaw, false);
    assert.equal(s.input.isPaused(), true);
    assert.equal(s.cleared(), 1);
    assert.ok(s.display().endsWith('\x1b[?25h'));
    assert.equal(s.input.listenerCount('keypress'), 0);
    assert.equal(s.output.listenerCount('resize'), 0);
    for (const event of ['SIGINT', 'SIGTERM', 'exit']) {
      assert.equal(s.signals.listenerCount(event), 0);
    }
    assert.deepEqual(s.exits, action === 'q' ? [0]
      : ['ctrl-c', 'SIGINT'].includes(action) ? [130]
      : action === 'SIGTERM' ? [143] : []);
  });
}

test('restores pre-existing raw mode and cleans up on rendering failure', () => {
  const s = session({ raw: true });
  s.output.write = () => { throw new Error('broken output'); };
  assert.throws(() => s.output.emit('resize'), /broken output/);
  assert.equal(s.input.isRaw, true);
  assert.equal(s.cleared(), 1);
  assert.equal(s.signals.listenerCount('exit'), 0);
});

test('game over freezes gameplay but keeps quit available', () => {
  const s = session();
  s.game.gameOver = true;
  const active = s.game.active;
  s.key('space');
  s.tick();
  assert.equal(s.game.active, active);
  s.output.emit('resize');
  assert.match(s.display(), /GAME OVER/);
  s.key('q');
  assert.deepEqual(s.exits, [0]);
});
