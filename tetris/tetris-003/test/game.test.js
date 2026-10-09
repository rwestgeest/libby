import assert from 'node:assert/strict';
import { test } from 'node:test';
import { Game, HEIGHT, WIDTH, TETROMINOES } from '../src/game.js';

const newGame = () => new Game({ random: () => 0.5 });

test('empty 10 by 20 board and all seven spawn shapes contain four cells', () => {
  const game = newGame();
  assert.equal(game.board.length, HEIGHT);
  assert.ok(game.board.every(row => row.length === WIDTH && row.every(cell => cell === null)));
  assert.equal(Object.keys(TETROMINOES).length, 7);
  for (const type of Object.keys(TETROMINOES)) {
    assert.equal(game.spawn(type), true);
    assert.equal(game.active.shape.flat().filter(Boolean).length, 4);
    assert.equal(game.canPlace(game.active), true);
  }
});

test('seven-bag generation includes each piece once per bag', () => {
  const game = newGame();
  const first = [game.active.type, ...Array.from({ length: 6 }, () => game.nextType())];
  const second = Array.from({ length: 7 }, () => game.nextType());
  assert.equal(new Set(first).size, 7);
  assert.equal(new Set(second).size, 7);
});

test('movement rejects walls, ceiling, floor and occupied cells without mutation', () => {
  const game = newGame();
  game.spawn('O');
  assert.equal(game.move(0, -1), false);
  while (game.move(-1)) {}
  assert.equal(game.active.x, 0);
  const before = game.active;
  assert.equal(game.move(-1), false);
  assert.equal(game.active, before);
  while (game.move(1)) {}
  assert.equal(game.active.x, 8);
  game.board[2][8] = 'T';
  assert.equal(game.move(0, 1), false);
  game.board[2][8] = null;
  while (game.move(0, 1)) {}
  assert.equal(game.active.y, 18);
  assert.equal(game.move(0, 1), false);
});

test('four rotations restore each shape and opposite rotations cancel', () => {
  const game = newGame();
  for (const type of Object.keys(TETROMINOES)) {
    game.spawn(type);
    const original = structuredClone(game.active);
    for (let i = 0; i < 4; i++) assert.equal(game.rotate(), true);
    assert.deepEqual(game.active, original);
    game.rotate();
    game.rotate(-1);
    assert.deepEqual(game.active, original);
  }
});

test('rotation kicks away from a wall and fails when every candidate is blocked', () => {
  const game = newGame();
  game.spawn('I');
  game.rotate();
  while (game.move(-1)) {}
  assert.equal(game.active.x, -2);
  assert.equal(game.rotate(), true);
  assert.equal(game.canPlace(game.active), true);

  game.spawn('T');
  game.board = Array.from({ length: HEIGHT }, () => Array(WIDTH).fill('J'));
  const before = game.active;
  assert.equal(game.rotate(), false);
  assert.equal(game.active, before);
});

test('gravity moves down, then locks four cells at the floor and spawns', () => {
  const game = newGame();
  game.spawn('O');
  game.tick();
  assert.equal(game.active.y, 1);
  for (let i = 0; i < 18; i++) game.tick();
  assert.equal(game.board.flat().filter(Boolean).length, 4);
  assert.deepEqual(game.board[19].slice(4, 6), ['O', 'O']);
  assert.equal(game.active.y, 0);
  assert.equal(game.score, 0);
});

test('drop scoring rewards soft and hard drop distance and locks on landing', () => {
  const game = newGame();
  game.spawn('O');
  game.softDrop();
  assert.equal(game.score, 1);
  assert.equal(game.hardDrop(), 17);
  assert.equal(game.score, 35);
  assert.equal(game.board.flat().filter(Boolean).length, 4);
});

test('clearing one through four rows awards points and preserves remaining rows', () => {
  for (const [count, points] of [[1, 100], [2, 300], [3, 500], [4, 800]]) {
    const game = newGame();
    game.board[HEIGHT - count - 1][0] = 'T';
    for (let y = HEIGHT - count; y < HEIGHT; y++) game.board[y].fill('I');
    assert.equal(game.clearLines(), count);
    assert.equal(game.score, points);
    assert.equal(game.lines, count);
    assert.equal(game.board[19][0], 'T');
    assert.ok(game.board.slice(0, count).every(row => row.every(cell => cell === null)));
    assert.notEqual(game.board[0], game.board[1]);
    assert.equal(game.clearLines(), 0);
    assert.equal(game.score, points);
  }
});

test('locking completes a row before checking the next spawn', () => {
  const game = newGame();
  game.spawn('I');
  game.board[19].fill('J');
  game.board[19].fill(null, 3, 7);
  assert.equal(game.hardDrop(), 18);
  assert.equal(game.lines, 1);
  assert.equal(game.score, 136);
  assert.ok(game.board.every(row => row.every(cell => cell === null)));
  assert.equal(game.gameOver, false);
});

test('blocked spawn ends the game and later actions are no-ops', () => {
  const game = newGame();
  game.board[0][4] = 'J';
  assert.equal(game.spawn('O'), false);
  assert.equal(game.gameOver, true);
  assert.equal(game.active, null);
  const before = structuredClone(game.board);
  assert.equal(game.move(1), false);
  assert.equal(game.rotate(), false);
  assert.equal(game.tick(), false);
  assert.equal(game.softDrop(), false);
  assert.equal(game.hardDrop(), 0);
  assert.equal(game.lock(), false);
  assert.deepEqual(game.board, before);
  assert.equal(game.score, 0);
});

test('lock detects game over when the following piece cannot spawn', () => {
  const game = newGame();
  game.spawn('O');
  game.move(0, 18);
  game.board[0].fill('J');
  game.board[1].fill('J');
  game.bag = ['T'];
  game.lock();
  // Full rows clear before spawning, so use a non-full obstruction instead.
  assert.equal(game.gameOver, false);
  game.spawn('O');
  game.move(0, 16);
  game.board[0][4] = 'J';
  game.bag = ['O'];
  game.lock();
  assert.equal(game.gameOver, true);
});
