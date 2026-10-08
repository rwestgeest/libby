import test from 'node:test';
import assert from 'node:assert/strict';
import { Game } from '../src/engine.js';
import { render, handleKey, startTerminal, SCREEN_ROWS, MIN_COLUMNS } from '../src/terminal.js';
import { PassThrough } from 'node:stream';
import { EventEmitter } from 'node:events';

test('playing and game-over frames fit 24 rows and include controls and score', () => {
  const game = new Game();
  for (const over of [false, true]) {
    game.gameOver = over;
    const frame = render(game);
    assert.equal(frame.split('\n').length, SCREEN_ROWS);
    assert.equal(SCREEN_ROWS, 24);
    assert.ok(frame.split('\n').every(line => line.length <= MIN_COLUMNS));
    assert.match(frame, /Score 0/);
    assert.match(frame, /Q:quit/);
    assert.match(frame, /R: reset/);
    if (over) assert.match(frame, /GAME OVER/);
  }
});

test('render overlays the active piece without mutating the board', () => {
  const game = new Game();
  const before = structuredClone(game.board);
  assert.equal(render(game).match(/\[\]/g).length, 4);
  assert.deepEqual(game.board, before);
});

test('keyboard controls move, rotate, drop, and quit', () => {
  const game = new Game();
  game.spawn('T');
  const x = game.active.x;
  handleKey(game, { name: 'left' });
  assert.equal(game.active.x, x - 1);
  handleKey(game, { name: 'right' });
  assert.equal(game.active.x, x);
  const shape = structuredClone(game.active.shape);
  handleKey(game, { name: 'up' });
  assert.notDeepEqual(game.active.shape, shape);
  handleKey(game, { name: 'down' });
  assert.equal(game.active.y, 1);
  assert.equal(game.score, 1);
  handleKey(game, { name: 'space' });
  assert.equal(game.board.flat().filter(Boolean).length, 4);
  assert.equal(handleKey(game, { name: 'q' }), 'quit');
  assert.equal(handleKey(game, { name: 'c', ctrl: true }), 'quit');
});


test('restart clears the board and score both during play and after game over', () => {
  const game = new Game();
  for (const over of [false, true]) {
    game.hardDrop();
    game.lines = 20;
    game.gameOver = over;
    assert.equal(handleKey(game, { name: 'r' }), 'restart');
    assert.equal(game.score, 0);
    assert.equal(game.lines, 0);
    assert.equal(game.level, 1);
    assert.equal(game.gameOver, false);
    assert.ok(game.active);
    assert.ok(game.board.flat().every(cell => cell === null));
  }
});

function terminal({ raw = false, flowing = false } = {}) {
  const input = new PassThrough();
  input.isTTY = true;
  input.isRaw = raw;
  input.setRawMode = value => { input.isRaw = value; };
  if (flowing) input.resume();
  const output = new EventEmitter();
  Object.assign(output, { isTTY: true, columns: 50, rows: 24, writes: [] });
  output.write = text => output.writes.push(text);
  return { input, output, signals: new EventEmitter() };
}

for (const exit of ['quit', 'ctrl-c', 'SIGINT', 'SIGTERM', 'exit', 'stop']) {
  test(`terminal restores state and removes listeners on ${exit}`, () => {
    const io = terminal();
    const stop = startTerminal(io);
    assert.equal(io.input.isRaw, true);
    assert.equal(io.input.readableFlowing, true);
    assert.equal(io.output.writes[0], '\x1b[?1049h\x1b[?25l');
    if (exit === 'quit') io.input.emit('keypress', 'q', { name: 'q' });
    else if (exit === 'ctrl-c') io.input.emit('keypress', '', { name: 'c', ctrl: true });
    else if (exit === 'stop') stop();
    else io.signals.emit(exit);
    assert.equal(io.input.isRaw, false);
    assert.equal(io.input.readableFlowing, false);
    assert.equal(io.input.listenerCount('keypress'), 0);
    assert.equal(io.output.listenerCount('resize'), 0);
    for (const signal of ['SIGINT', 'SIGTERM', 'exit']) {
      assert.equal(io.signals.listenerCount(signal), 0);
    }
    assert.equal(io.output.writes.at(-1), '\x1b[?25h\x1b[?1049l');
    const count = io.output.writes.length;
    stop();
    assert.equal(io.output.writes.length, count);
  });
}

test('cleanup preserves an already raw and flowing input', () => {
  const io = terminal({ raw: true, flowing: true });
  startTerminal(io)();
  assert.equal(io.input.isRaw, true);
  assert.equal(io.input.readableFlowing, true);
  io.input.destroy();
});

test('small terminal prompts and resize redraws a full 24-row frame', () => {
  const io = terminal();
  io.output.columns = 30;
  io.output.rows = 10;
  const stop = startTerminal(io);
  try {
    assert.match(io.output.writes.at(-1), /Resize to 50x24/);
    io.output.columns = 50;
    io.output.rows = 24;
    io.output.emit('resize');
    const frame = io.output.writes.at(-1).replace(/\x1b\[[0-9;]*[HJ]/g, '');
    assert.equal(frame.split('\r\n').length, 24);
    assert.ok(frame.split('\r\n').every(line => line.length <= 50));
    assert.ok(!frame.endsWith('\n'));
  } finally { stop(); }
});

test('non-interactive streams are rejected without changing terminal state', () => {
  for (const stream of ['input', 'output']) {
    const io = terminal();
    io[stream].isTTY = false;
    assert.throws(() => startTerminal(io), /interactive terminal/);
    assert.equal(io.input.isRaw, false);
    assert.deepEqual(io.output.writes, []);
  }
});

test('draw errors clean up terminal state', () => {
  const io = terminal();
  const write = io.output.write;
  io.output.write = text => {
    if (text.startsWith('\x1b[H')) throw new Error('draw failed');
    write(text);
  };
  assert.throws(() => startTerminal(io), /draw failed/);
  assert.equal(io.input.isRaw, false);
  assert.equal(io.input.listenerCount('keypress'), 0);
  assert.equal(io.output.writes.at(-1), '\x1b[?25h\x1b[?1049l');
});

test('restart resumes gravity after game over without duplicate timers', async () => {
  const io = terminal();
  const game = new Game();
  Object.defineProperty(game, 'gravityMs', { value: 15 });
  let ticks = 0;
  game.tick = () => { ticks++; game.gameOver = true; };
  game.gameOver = true;
  const stop = startTerminal({ ...io, game });
  try {
    await new Promise(resolve => setTimeout(resolve, 40));
    assert.equal(ticks, 0);
    io.input.emit('keypress', 'r', { name: 'r' });
    io.input.emit('keypress', 'r', { name: 'r' });
    assert.equal(game.gameOver, false);
    await new Promise(resolve => setTimeout(resolve, 60));
    assert.equal(ticks, 1);
    assert.equal(game.gameOver, true);
    io.input.emit('keypress', 'r', { name: 'r' });
    await new Promise(resolve => setTimeout(resolve, 60));
    assert.equal(ticks, 2);
  } finally { stop(); }
});
