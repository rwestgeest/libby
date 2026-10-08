import test from 'node:test';
import assert from 'node:assert/strict';
import { Game, SHAPES, WIDTH, HEIGHT } from '../src/engine.js';

const game = () => new Game({ random: () => 0.5 });

test('initial state is a 10×20 empty board with an active piece', () => {
  const g = game();
  assert.equal(g.board.length, HEIGHT);
  assert.ok(g.board.every(row => row.length === WIDTH && row.every(c => c === null)));
  assert.equal(g.score, 0);
  assert.equal(g.lines, 0);
  assert.equal(g.gameOver, false);
  assert.equal(g.collides(g.active), false);
  g.board[0][0] = 'I';
  assert.equal(g.board[1][0], null);
});

test('each seven-piece bag includes every tetromino once', () => {
  const g = game();
  const first = [g.active.type, ...Array.from({ length: 6 }, () => g.nextType())];
  const second = Array.from({ length: 7 }, () => g.nextType());
  assert.deepEqual(first.sort(), Object.keys(SHAPES).sort());
  assert.deepEqual(second.sort(), first);
});

for (const type of Object.keys(SHAPES)) {
  test(`${type} spawns with four cells and returns to its shape after four rotations`, () => {
    const g = game();
    g.spawn(type);
    const original = structuredClone(g.active.shape);
    assert.equal(original.flat().filter(Boolean).length, 4);
    for (let i = 0; i < 4; i++) assert.equal(g.rotate(), true);
    assert.deepEqual(g.active.shape, original);
    assert.equal(g.collides(g.active), false);
  });
}

test('movement stops at both walls and at occupied cells', () => {
  const g = game();
  g.spawn('O');
  while (g.move(-1)) {}
  assert.equal(g.active.x, 0);
  assert.equal(g.move(-1), false);
  while (g.move(1)) {}
  assert.equal(g.active.x, 8);
  g.board[2][8] = 'T';
  assert.equal(g.move(0, 1), false);
});

test('rotation can kick away from a wall', () => {
  const g = game();
  g.spawn('I');
  g.rotate();
  while (g.move(-1)) {}
  assert.equal(g.active.x, -2);
  assert.equal(g.rotate(), true);
  assert.equal(g.active.x, 0);
  assert.equal(g.collides(g.active), false);
});

test('blocked rotation preserves the active piece', () => {
  const g = game();
  g.spawn('T');
  g.active.y = 5;
  const before = structuredClone(g.active);
  g.board = g.board.map(row => row.map(() => 'J'));
  before.shape.forEach((row, dy) => row.forEach((cell, dx) => {
    if (cell) g.board[before.y + dy][before.x + dx] = null;
  }));
  assert.equal(g.rotate(), false);
  assert.deepEqual(g.active, before);
});

test('gravity moves a piece, then locks and spawns when it hits the floor', () => {
  const g = game();
  g.spawn('O');
  assert.equal(g.tick(), true);
  assert.equal(g.active.y, 1);
  for (let i = 0; i < 17; i++) g.tick();
  assert.equal(g.active.y, 18);
  assert.equal(g.tick(), false);
  assert.equal(g.board.flat().filter(Boolean).length, 4);
  assert.equal(g.active.y, 0);
  assert.equal(g.score, 0);
});

test('soft drop scores one point per row and locks at the floor', () => {
  const g = game();
  g.spawn('O');
  assert.equal(g.softDrop(), true);
  assert.equal(g.score, 1);
  g.active.y = 18;
  assert.equal(g.softDrop(), false);
  assert.equal(g.score, 1);
  assert.equal(g.board[19][4], 'O');
});

test('hard drop scores twice its distance and immediately locks', () => {
  const g = game();
  g.spawn('O');
  assert.equal(g.hardDrop(), 18);
  assert.equal(g.score, 36);
  assert.equal(g.board[18][4], 'O');
  assert.equal(g.board[19][5], 'O');
  assert.equal(g.active.y, 0);
});

for (const [count, score] of [[1, 100], [2, 300], [3, 500], [4, 800]]) {
  test(`clearing ${count} lines awards ${score} points and preserves other rows`, () => {
    const g = game();
    g.board[HEIGHT - count - 1][0] = 'T';
    for (let y = HEIGHT - count; y < HEIGHT; y++) g.board[y].fill('I');
    assert.equal(g.clearLines(), count);
    assert.equal(g.lines, count);
    assert.equal(g.score, score);
    assert.equal(g.board.length, HEIGHT);
    assert.equal(g.board[19][0], 'T');
    assert.ok(g.board.slice(0, count).every(row => row.every(c => c === null)));
  });
}

test('locking a piece completes and clears a line', () => {
  const g = game();
  g.board[19].fill('J');
  g.board[19][4] = g.board[19][5] = null;
  g.spawn('O');
  g.hardDrop();
  assert.equal(g.lines, 1);
  assert.equal(g.score, 136);
  assert.equal(g.board[19][4], 'O');
  assert.equal(g.board[19][0], null);
});

test('line thresholds increase level, scoring multiplier, and gravity speed', () => {
  const g = game();
  const speed = g.gravityMs;
  g.lines = 10;
  assert.equal(g.level, 2);
  assert.ok(g.gravityMs < speed);
  g.board[19].fill('I');
  g.clearLines();
  assert.equal(g.score, 200);
  g.lines = 1000;
  assert.equal(g.gravityMs, 80);
});

test('blocked spawn ends the game and all gameplay input becomes inert', () => {
  const g = game();
  g.board[0][4] = 'Z';
  assert.equal(g.spawn('O'), false);
  assert.equal(g.gameOver, true);
  const before = structuredClone({ board: g.board, active: g.active, score: g.score });
  assert.equal(g.move(1), false);
  assert.equal(g.rotate(), false);
  assert.equal(g.tick(), false);
  assert.equal(g.softDrop(), false);
  assert.equal(g.hardDrop(), 0);
  assert.deepEqual({ board: g.board, active: g.active, score: g.score }, before);
});

test('locking above the top ends the game without writing outside the board', () => {
  const g = game();
  g.spawn('O');
  g.active.y = -1;
  g.lock();
  assert.equal(g.gameOver, true);
  assert.ok(g.board.every(row => row.every(c => c === null)));
});

test('reset restores a fresh playable game', () => {
  const g = game();
  g.hardDrop();
  g.lines = 20;
  g.gameOver = true;
  g.reset();
  assert.equal(g.score, 0);
  assert.equal(g.lines, 0);
  assert.equal(g.gameOver, false);
  assert.equal(g.collides(g.active), false);
  assert.ok(g.board.every(row => row.every(c => c === null)));
});
