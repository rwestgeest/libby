import assert from 'node:assert/strict';
import test from 'node:test';
import { Game, createBoard, TETROMINOES } from '../src/game.js';

function gameWith(type) {
  const game = new Game({ random: () => 0 });
  game.active = null;
  assert.equal(game.spawn(type), true);
  return game;
}

function occupied(board) {
  return board.flat().filter(Boolean).length;
}

test('board has 20 independent rows of 10 empty cells', () => {
  const board = createBoard();
  assert.equal(board.length, 20);
  assert.ok(board.every(row => row.length === 10 && row.every(cell => cell === null)));
  board[0][0] = 'T';
  assert.equal(board[1][0], null);
});

test('all seven tetrominoes spawn centered, with four cells and independent matrices', () => {
  assert.deepEqual(Object.keys(TETROMINOES).sort(), ['I', 'J', 'L', 'O', 'S', 'T', 'Z']);
  for (const type of Object.keys(TETROMINOES)) {
    const game = gameWith(type);
    assert.equal(game.active.x, Math.floor((10 - game.active.matrix.length) / 2));
    assert.equal(game.active.y, 0);
    assert.equal(occupied(game.active.matrix), 4);
    assert.equal(game.collides(game.active), false);
    assert.equal(occupied(game.board), 0);
    game.active.matrix[0][0] = 9;
    assert.notEqual(TETROMINOES[type][0][0], 9);
  }
});

test('random source selects the piece; invalid types and double spawning are safe', () => {
  assert.equal(new Game({ random: () => 0.999 }).active.type, 'L');
  const game = gameWith('T');
  const active = game.active;
  assert.equal(game.spawn('O'), false);
  assert.equal(game.active, active);
  game.active = null;
  assert.throws(() => game.spawn('bad'), RangeError);
});

test('movement stops at both walls and floor without altering the board', () => {
  const game = gameWith('O');
  while (game.move(-1, 0)) {}
  assert.equal(game.active.x, 0);
  let before = game.active;
  assert.equal(game.move(-1, 0), false);
  assert.equal(game.active, before);
  while (game.move(1, 0)) {}
  assert.equal(game.active.x, 8);
  while (game.move(0, 1)) {}
  assert.equal(game.active.y, 18);
  before = game.active;
  assert.equal(game.move(0, 1), false);
  assert.equal(game.active, before);
  assert.equal(occupied(game.board), 0);
  assert.throws(() => game.move(0, 2), RangeError);
});

test('movement collides with settled pieces and the top boundary', () => {
  const game = gameWith('O');
  game.board[2][4] = 'I';
  const before = game.active;
  assert.equal(game.move(0, 1), false);
  assert.equal(game.move(0, -1), false);
  assert.equal(game.active, before);
  assert.equal(game.move(-1, 0), true);
});

test('four clockwise rotations restore every tetromino', () => {
  for (const type of Object.keys(TETROMINOES)) {
    const game = gameWith(type);
    const original = structuredClone(game.active);
    for (let i = 0; i < 4; i++) assert.equal(game.rotate(), true);
    assert.deepEqual(game.active, original);
  }
});

test('rotations into the wall, floor, or settled cells are rejected atomically', () => {
  const wall = gameWith('I');
  wall.rotate();
  while (wall.move(-1, 0)) {}
  const floor = gameWith('I');
  while (floor.move(0, 1)) {}
  const pile = gameWith('T');
  pile.board[2][4] = 'J';
  for (const game of [wall, floor, pile]) {
    const before = game.active;
    assert.equal(game.rotate(), false);
    assert.equal(game.active, before);
  }
});

test('grounded pieces lock exactly four cells, preserving previously locked cells', () => {
  for (const type of Object.keys(TETROMINOES)) {
    const game = gameWith(type);
    assert.equal(game.lock(), false);
    game.board[19][0] = 'O';
    while (game.move(0, 1)) {}
    assert.equal(game.lock(), true);
    assert.equal(game.active, null);
    assert.equal(occupied(game.board), 5);
    assert.equal(game.board.flat().filter(cell => cell === type).length, type === 'O' ? 5 : 4);
    assert.equal(game.board[19][0], 'O');
    assert.equal(game.lock(), false);
    assert.equal(game.move(1, 0), false);
    assert.equal(game.rotate(), false);
    assert.equal(game.spawn('T'), true);
  }
});

test('a piece can lock on a stack and a blocked spawn is rejected', () => {
  const game = gameWith('O');
  game.board[5][4] = 'J';
  while (game.move(0, 1)) {}
  assert.equal(game.active.y, 3);
  assert.equal(game.lock(), true);
  assert.equal(game.board[4][4], 'O');
  game.board[0][4] = 'J';
  const before = structuredClone(game.board);
  assert.equal(game.spawn('O'), false);
  assert.equal(game.active, null);
  assert.deepEqual(game.board, before);
});
