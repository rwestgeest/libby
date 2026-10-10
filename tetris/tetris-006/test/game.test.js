import test from 'node:test';
import assert from 'node:assert/strict';
import { Game, WIDTH, HEIGHT, SHAPES, rotateMatrix } from '../src/game.js';

const makeGame = () => new Game({ random: () => 0 });
function setPiece(game, type, x, y) {
  game.active = { type, matrix: SHAPES[type].map(row => [...row]), x, y };
}

test('initial state has independent empty rows and a valid piece', () => {
  const game = makeGame();
  assert.equal(game.board.length, HEIGHT);
  assert.ok(game.board.every(row => row.length === WIDTH && row.every(cell => cell === null)));
  assert.notEqual(game.board[0], game.board[1]);
  assert.equal(game.collides(game.active), false);
  assert.equal(game.score, 0);
  assert.equal(game.level, 1);
});

test('each seven-piece bag contains all tetrominoes', () => {
  const game = makeGame();
  const first = [game.active.type, ...Array.from({ length: 6 }, () => game.nextType())];
  const second = Array.from({ length: 7 }, () => game.nextType());
  assert.deepEqual(first.sort(), Object.keys(SHAPES).sort());
  assert.deepEqual(second.sort(), first);
});

test('movement rejects walls, floor, and occupied cells without changing state', () => {
  const game = makeGame();
  setPiece(game, 'O', 0, 18);
  assert.equal(game.move(-1), false);
  assert.equal(game.move(0, 1), false);
  game.board[18][2] = 'T';
  assert.equal(game.move(1), false);
  assert.equal(game.active.x, 0);
  setPiece(game, 'O', 8, 0);
  assert.equal(game.move(1), false);
});

test('four rotations restore a matrix; reverse rotation undoes clockwise', () => {
  for (const matrix of Object.values(SHAPES)) {
    let rotated = matrix;
    for (let i = 0; i < 4; i++) rotated = rotateMatrix(rotated);
    assert.deepEqual(rotated, matrix);
    assert.deepEqual(rotateMatrix(rotateMatrix(matrix), false), matrix);
  }
});

test('rotation kicks away from wall and rejects fully blocked rotation', () => {
  const game = makeGame();
  setPiece(game, 'I', -2, 0);
  game.active.matrix = rotateMatrix(game.active.matrix);
  assert.equal(game.collides(game.active), false);
  assert.equal(game.rotate(), true);
  assert.equal(game.active.x, 0);
  setPiece(game, 'T', 3, 0);
  game.board = game.board.map(row => row.map(() => 'J'));
  const before = structuredClone(game.active);
  assert.equal(game.rotate(), false);
  assert.deepEqual(game.active, before);
});

test('gravity locks a grounded piece and spawns the next', () => {
  const game = makeGame();
  setPiece(game, 'O', 0, 18);
  game.tick();
  assert.deepEqual(game.board[18].slice(0, 2), ['O', 'O']);
  assert.deepEqual(game.board[19].slice(0, 2), ['O', 'O']);
  assert.equal(game.active.y, 0);
  assert.equal(game.score, 0);
});

test('soft and hard drops award points only for travelled rows', () => {
  const game = makeGame();
  setPiece(game, 'O', 0, 0);
  assert.equal(game.softDrop(), true);
  assert.equal(game.score, 1);
  assert.equal(game.hardDrop(), 17);
  assert.equal(game.score, 35);
  assert.equal(game.board[19][0], 'O');
});

test('locking clears two lines, shifts cells, and scores', () => {
  const game = makeGame();
  game.board[17][5] = 'T';
  for (const y of [18, 19]) game.board[y].fill('J', 2);
  setPiece(game, 'O', 0, 18);
  game.lock();
  assert.equal(game.lines, 2);
  assert.equal(game.score, 300);
  assert.equal(game.board[19][5], 'T');
  assert.ok(game.board[0].every(cell => cell === null));
  assert.equal(game.board.length, HEIGHT);
});

test('four-line scoring uses the level before the clear', () => {
  const game = makeGame();
  game.lines = 9;
  for (let y = 16; y < HEIGHT; y++) game.board[y].fill('J');
  assert.equal(game.clearLines(), 4);
  assert.equal(game.score, 800);
  assert.equal(game.level, 2);
  assert.equal(game.gravityMs, 740);
});

test('spawn obstruction ends game; controls are inert until restart', () => {
  const game = makeGame();
  game.board[0].fill('J');
  game.board[1].fill('J');
  assert.equal(game.spawn(), false);
  assert.equal(game.gameOver, true);
  assert.equal(game.active, null);
  const board = structuredClone(game.board);
  assert.equal(game.move(1), false);
  assert.equal(game.rotate(), false);
  assert.equal(game.tick(), false);
  assert.equal(game.softDrop(), false);
  assert.equal(game.hardDrop(), 0);
  assert.deepEqual(game.board, board);
  game.restart();
  assert.equal(game.gameOver, false);
  assert.equal(game.lines, 0);
  assert.equal(game.score, 0);
  assert.ok(game.active);
});

test('locking above the board tops out without partial writes', () => {
  const game = makeGame();
  setPiece(game, 'O', 0, -1);
  game.lock();
  assert.equal(game.gameOver, true);
  assert.ok(game.board.flat().every(cell => cell === null));
});

test('display board overlays the active piece without mutating locked cells', () => {
  const game = makeGame();
  setPiece(game, 'O', 0, 0);
  const display = game.displayBoard();
  assert.equal(display[0][0], 'O');
  assert.equal(game.board[0][0], null);
  display[2][2] = 'J';
  assert.equal(game.board[2][2], null);
});
