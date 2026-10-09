import assert from 'node:assert/strict';
import test from 'node:test';
import {
  BOARD_HEIGHT, BOARD_WIDTH, Game, PIECE_TYPES, TETROMINOES,
  collides, createBoard, pieceCells, rotateShape,
} from '../src/game.js';

test('board is empty, 10 by 20, with independent rows', () => {
  const board = createBoard();
  assert.equal(board.length, BOARD_HEIGHT);
  assert.ok(board.every(row => row.length === BOARD_WIDTH && row.every(cell => cell === null)));
  board[0][0] = 'T';
  assert.equal(board[1][0], null);
  assert.equal(createBoard()[0][0], null);
});

test('all seven tetrominoes spawn centered with four cells and rotate in a cycle', () => {
  assert.equal(PIECE_TYPES.length, 7);
  for (const type of PIECE_TYPES) {
    const game = new Game();
    assert.equal(game.spawn(type), true);
    assert.equal(pieceCells(game.active).length, 4);
    assert.equal(game.active.x, Math.floor((BOARD_WIDTH - game.active.shape.length) / 2));
    assert.equal(game.active.y, 0);
    const original = structuredClone(game.active.shape);
    for (let i = 0; i < 4; i++) assert.equal(game.rotate(), true);
    assert.deepEqual(game.active.shape, original);
    assert.deepEqual(rotateShape(rotateShape(original), -1), original);
    assert.deepEqual(TETROMINOES[type], original);
  }
});

test('seven-bag supplies each piece once per bag', () => {
  const game = new Game({ random: () => 0.5 });
  for (let bag = 0; bag < 3; bag++) {
    const types = Array.from({ length: 7 }, () => game.nextType());
    assert.deepEqual(types.sort(), [...PIECE_TYPES].sort());
  }
});

test('movement respects walls, floor, ceiling, and occupied cells', () => {
  const game = new Game();
  game.spawn('O');
  assert.equal(game.move(-4), true);
  const left = structuredClone(game.active);
  assert.equal(game.move(-1), false);
  assert.deepEqual(game.active, left);
  assert.equal(game.move(0, -1), false);
  game.board[0][2] = 'J';
  assert.equal(game.move(8), false); // Cannot tunnel through the obstacle.
  assert.deepEqual(game.active, left);
  game.board[0][2] = null;
  assert.equal(game.move(8), true);
  assert.equal(game.move(1), false);
  assert.equal(game.move(0, 18), true);
  assert.equal(game.move(0, 1), false);
  assert.equal(collides(game.board, game.active), false);
});

test('rotation kicks away from walls and rejects blocked rotations without mutation', () => {
  const game = new Game();
  game.spawn('I');
  game.rotate();
  assert.equal(game.move(-5), true);
  assert.equal(game.rotate(), true);
  assert.ok(pieceCells(game.active).every(({ x }) => x >= 0));

  const blocked = new Game();
  blocked.spawn('T');
  const before = structuredClone(blocked.active);
  const activeCells = new Set(pieceCells(before).map(({ x, y }) => `${x},${y}`));
  for (let y = 0; y < BOARD_HEIGHT; y++) {
    for (let x = 0; x < BOARD_WIDTH; x++) {
      if (!activeCells.has(`${x},${y}`)) blocked.board[y][x] = 'J';
    }
  }
  assert.equal(blocked.rotate(), false);
  assert.deepEqual(blocked.active, before);
});

test('grounded pieces lock into the board and subsequent pieces collide with them', () => {
  const game = new Game();
  assert.equal(game.lock(), false);
  game.spawn('O');
  assert.equal(game.lock(), false);
  assert.equal(game.spawn('T'), false);
  while (game.move(0, 1)) { /* descend to the floor */ }
  const cells = pieceCells(game.active);
  assert.equal(game.lock(), true);
  assert.equal(game.active, null);
  for (const { x, y } of cells) assert.equal(game.board[y][x], 'O');
  game.spawn('O');
  while (game.move(0, 1)) { /* descend onto the locked piece */ }
  assert.equal(game.active.y, 16);
  assert.equal(game.lock(), true);
  assert.equal(game.board.flat().filter(Boolean).length, 8);
});

test('blocked spawns leave no active piece and do not alter the board', () => {
  const game = new Game();
  game.board[0][4] = 'Z';
  const before = structuredClone(game.board);
  assert.equal(game.spawn('O'), false);
  assert.equal(game.active, null);
  assert.deepEqual(game.board, before);
  assert.throws(() => game.spawn('invalid'), RangeError);
});
