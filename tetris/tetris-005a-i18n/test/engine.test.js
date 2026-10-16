import test from 'node:test';
import assert from 'node:assert/strict';
import { Game, WIDTH, HEIGHT, SHAPES, cells } from '../src/engine.js';

const game = () => new Game({ random: () => 0.5 });
const piece = (type, x, y) => ({
  type, x, y, matrix: SHAPES[type].map(row => [...row].map(Number)),
});

test('starts with an independent 10 by 20 board and a valid centered piece', () => {
  const g = game();
  assert.equal(g.board.length, HEIGHT);
  assert.ok(g.board.every(row => row.length === WIDTH && row.every(c => c === null)));
  assert.notEqual(g.board[0], g.board[1]);
  assert.equal(g.score, 0);
  assert.equal(g.lines, 0);
  assert.equal(g.level, 1);
  assert.equal(g.gameOver, false);
  assert.ok(g.fits(g.active));
  assert.equal(g.active.x, Math.floor((WIDTH - g.active.matrix.length) / 2));
});

test('seven-bag supplies each tetromino once per bag', () => {
  const g = game();
  const types = [g.active.type, g.next];
  for (let i = 0; i < 12; i++) types.push(g.draw());
  for (const bag of [types.slice(0, 7), types.slice(7)]) {
    assert.deepEqual([...bag].sort(), Object.keys(SHAPES).sort());
  }
  for (const type of Object.keys(SHAPES)) assert.equal(cells(piece(type, 0, 0)).length, 4);
});

test('movement respects both walls and settled blocks', () => {
  const g = game();
  g.active = piece('O', 0, 0);
  assert.equal(g.move(-1), false);
  assert.equal(g.move(1), true);
  g.board[0][3] = 'T';
  assert.equal(g.move(1), false);
  g.active = piece('O', 8, 0);
  assert.equal(g.move(1), false);
});

test('four clockwise rotations and inverse rotations restore each shape', () => {
  const g = game();
  for (const type of Object.keys(SHAPES)) {
    g.active = piece(type, 3, 3);
    const original = structuredClone(g.active);
    for (let i = 0; i < 4; i++) assert.equal(g.rotate(), true);
    assert.deepEqual(g.active, original);
    g.rotate();
    g.rotate(-1);
    assert.deepEqual(g.active, original);
  }
});

test('rotation kicks off walls and fails without mutating when obstructed', () => {
  const g = game();
  g.active = piece('I', 0, 4);
  g.rotate();
  g.move(-1);
  g.move(-1);
  assert.equal(g.rotate(), true);
  assert.ok(g.fits(g.active));
  g.active = piece('T', 3, 4);
  g.board = g.board.map(row => row.map(() => 'O'));
  for (const { x, y } of cells(g.active)) g.board[y][x] = null;
  const before = structuredClone(g.active);
  assert.equal(g.rotate(), false);
  assert.deepEqual(g.active, before);
});

test('gravity and soft drop move down, only soft drop awards points', () => {
  const g = game();
  const y = g.active.y;
  assert.equal(g.tick(), true);
  assert.equal(g.active.y, y + 1);
  assert.equal(g.score, 0);
  g.softDrop();
  assert.equal(g.active.y, y + 2);
  assert.equal(g.score, 1);
});

test('gravity locks on the floor and spawns the preview piece', () => {
  const g = game();
  g.active = piece('O', 4, 18);
  const next = g.next;
  g.tick();
  assert.deepEqual(g.board[18].slice(4, 6), ['O', 'O']);
  assert.deepEqual(g.board[19].slice(4, 6), ['O', 'O']);
  assert.equal(g.active.type, next);
  assert.equal(g.active.y, 0);
});

test('hard drop stops above a stack and awards two points per row', () => {
  const g = game();
  g.active = piece('O', 4, 0);
  g.board[19][4] = 'J';
  g.hardDrop();
  assert.equal(g.score, 34);
  assert.equal(g.board[17][4], 'O');
  assert.equal(g.board[18][4], 'O');
  assert.equal(g.board[19][4], 'J');
});

for (const count of [1, 2, 3, 4]) {
  test(`locking clears ${count} lines simultaneously and scores correctly`, () => {
    const g = game();
    for (let y = HEIGHT - count; y < HEIGHT; y++) {
      g.board[y].fill('J');
      g.board[y][5] = null;
    }
    g.board[10][0] = 'Z';
    g.active = piece('I', 3, 16);
    g.rotate(); // Vertical I occupies column 5 and rows 16–19.
    g.tick();
    assert.equal(g.lines, count);
    assert.equal(g.score, [0, 100, 300, 500, 800][count]);
    assert.equal(g.board.length, HEIGHT);
    assert.equal(g.board[10 + count][0], 'Z');
    assert.ok(g.board.slice(0, count).every(row => row.every(c => c === null)));
  });
}

test('line score uses pre-clear level and gravity accelerates with level', () => {
  const g = game();
  g.lines = 9;
  const speed = g.gravityMs;
  g.board[19].fill('Z');
  g.clearLines();
  assert.equal(g.level, 2);
  assert.equal(g.score, 100);
  assert.ok(g.gravityMs < speed);
  g.board[19].fill('Z');
  g.clearLines();
  assert.equal(g.score, 300);
});

test('blocked spawn ends game and actions become no-ops; reset starts fresh', () => {
  const g = game();
  g.board[0].fill('Z');
  g.board[1].fill('Z');
  assert.equal(g.spawn(), false);
  assert.equal(g.gameOver, true);
  const before = JSON.stringify(g);
  for (const action of [() => g.move(1), () => g.rotate(), () => g.tick(),
    () => g.softDrop(), () => g.hardDrop(), () => g.lock(), () => g.spawn()]) {
    assert.equal(action(), false);
    assert.equal(JSON.stringify(g), before);
  }
  g.reset();
  assert.equal(g.gameOver, false);
  assert.equal(g.score, 0);
  assert.equal(g.lines, 0);
  assert.ok(g.board.every(row => row.every(c => c === null)));
  assert.ok(g.fits(g.active));
});

test('locking above the ceiling ends the game without writing outside board', () => {
  const g = game();
  g.active = piece('O', 4, -1);
  g.board[1][4] = 'J';
  g.tick();
  assert.equal(g.gameOver, true);
  assert.equal(g.active, null);
  assert.equal(g.board.length, HEIGHT);
});
