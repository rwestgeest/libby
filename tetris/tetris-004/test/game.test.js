import assert from 'node:assert/strict';
import test from 'node:test';
import { BOARD_HEIGHT, BOARD_WIDTH, Game, TETROMINOES, createBoard } from '../src/game.js';

function withPiece(type) {
  const game = new Game({ random: () => 0.5 });
  game.active = null;
  assert.equal(game.spawn(type), true);
  return game;
}

function cells(board) {
  return board.flat().filter(cell => cell !== null);
}

test('board has 20 independent rows of 10 empty cells', () => {
  const board = createBoard();
  assert.equal(board.length, BOARD_HEIGHT);
  assert.ok(board.every(row => row.length === BOARD_WIDTH && row.every(cell => cell === null)));
  board[0][0] = 'T';
  assert.equal(board[1][0], null);
});

test('all seven shapes spawn centered with four cells and independent matrices', () => {
  assert.equal(Object.keys(TETROMINOES).length, 7);
  for (const type of Object.keys(TETROMINOES)) {
    const game = withPiece(type);
    assert.equal(game.active.shape.flat().filter(Boolean).length, 4);
    assert.equal(game.active.x, Math.floor((10 - game.active.shape.length) / 2));
    assert.equal(game.active.y, 0);
    assert.equal(game.collides(game.active), false);
    game.active.shape[0][0] = 9;
    assert.notEqual(TETROMINOES[type][0][0], 9);
    assert.equal(cells(game.board).length, 0);
  }
});

test('random spawning uses shuffled bags with each type exactly once per bag', () => {
  const game = new Game({ random: () => 0.25 });
  const types = [game.active.type];
  for (let i = 1; i < 14; i++) {
    game.active = null;
    assert.equal(game.spawn(), true);
    types.push(game.active.type);
  }
  const expected = Object.keys(TETROMINOES).sort();
  assert.deepEqual(types.slice(0, 7).sort(), expected);
  assert.deepEqual(types.slice(7).sort(), expected);
  const other = new Game({ random: () => 0.75 });
  assert.notEqual(other.active.type, types[0]);
});

test('rejected spawning preserves the next random piece and seven-bag sequence', () => {
  let randomCalls = 0;
  const game = new Game({ random: () => { randomCalls++; return 0.25; } });
  const control = new Game({ random: () => 0.25 });
  const types = [];
  for (let i = 0; i < 14; i++) {
    types.push(game.active.type);
    assert.equal(game.active.type, control.active.type);
    const active = game.active;
    const bag = [...game.bag];
    const callsBefore = randomCalls;
    assert.equal(game.spawn(), false);
    assert.equal(game.active, active);
    assert.deepEqual(game.bag, bag);
    assert.equal(randomCalls, callsBefore);
    game.active = null;
    control.active = null;
    assert.equal(game.spawn(), true);
    assert.equal(control.spawn(), true);
    assert.equal(game.active.type, control.active.type);
  }
  const expected = Object.keys(TETROMINOES).sort();
  assert.deepEqual(types.slice(0, 7).sort(), expected);
  assert.deepEqual(types.slice(7).sort(), expected);
});

test('movement stops at both walls and floor without changing the board', () => {
  const game = withPiece('O');
  assert.equal(game.move(-1), true);
  while (game.move(-1)) { /* move to the left wall */ }
  assert.equal(game.active.x, 0);
  const atLeft = game.active;
  assert.equal(game.move(-1), false);
  assert.equal(game.active, atLeft);
  while (game.move(1)) { /* move to the right wall */ }
  assert.equal(game.active.x, 8);
  while (game.move(0, 1)) { /* move to the floor */ }
  assert.equal(game.active.y, 18);
  assert.equal(game.move(0, 1), false);
  assert.equal(cells(game.board).length, 0);
});

test('collision detects occupied cells and upper boundary but ignores empty shape cells', () => {
  const game = withPiece('O');
  game.board[0][game.active.x - 1] = 'J';
  const before = game.active;
  assert.equal(game.move(-1), false);
  assert.equal(game.active, before);
  assert.equal(game.move(0, -1), false);
  const line = withPiece('I');
  line.board[0][line.active.x] = 'J';
  assert.equal(line.collides(line.active), false);
  line.board[1][line.active.x] = 'J';
  assert.equal(line.collides(line.active), true);
});

test('four clockwise rotations restore every shape without changing templates', () => {
  for (const type of Object.keys(TETROMINOES)) {
    const game = withPiece(type);
    const original = structuredClone(game.active);
    for (let i = 0; i < 4; i++) assert.equal(game.rotate(), true);
    assert.deepEqual(game.active, original);
    assert.deepEqual(game.active.shape, TETROMINOES[type]);
  }
});

test('blocked rotations leave the active piece unchanged', () => {
  const game = withPiece('I');
  assert.equal(game.rotate(), true);
  while (game.move(-1)) { /* vertical I can sit flush against the wall */ }
  const before = game.active;
  assert.equal(game.rotate(), false);
  assert.equal(game.active, before);

  const blocked = withPiece('T');
  blocked.board[2][blocked.active.x + 1] = 'O';
  const original = blocked.active;
  assert.equal(blocked.rotate(), false);
  assert.equal(blocked.active, original);
});

test('locking deposits four typed cells only when resting and allows another spawn', () => {
  const game = withPiece('O');
  assert.equal(game.lock(), false);
  while (game.move(0, 1)) { /* descend */ }
  assert.equal(game.lock(), true);
  assert.equal(game.active, null);
  assert.deepEqual(cells(game.board), ['O', 'O', 'O', 'O']);
  assert.equal(game.board[18][4], 'O');
  assert.equal(game.board[19][5], 'O');
  assert.equal(game.lock(), false);
  assert.equal(game.move(1), false);
  assert.equal(game.rotate(), false);
  assert.equal(game.spawn('O'), true);
  while (game.move(0, 1)) { /* stop on the previous piece */ }
  assert.equal(game.active.y, 16);
  assert.equal(game.lock(), true);
  assert.equal(cells(game.board).length, 8);
});

test('spawning refuses an occupied spawn or replacement of an active piece', () => {
  const game = withPiece('O');
  const before = game.active;
  assert.equal(game.spawn('T'), false);
  assert.equal(game.active, before);
  game.active = null;
  game.board[0][4] = 'Z';
  const boardBefore = structuredClone(game.board);
  assert.equal(game.spawn('O'), false);
  assert.equal(game.active, null);
  assert.deepEqual(game.board, boardBefore);
  assert.throws(() => game.spawn('invalid'), RangeError);
});
