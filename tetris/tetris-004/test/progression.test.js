import assert from 'node:assert/strict';
import test from 'node:test';
import { Game, BOARD_HEIGHT, BOARD_WIDTH } from '../src/game.js';

function withPiece(type = 'O') {
  const game = new Game({ random: () => 0.5 });
  game.active = null;
  game.spawn(type);
  return game;
}

function fillExcept(game, row, holes) {
  game.board[row] = Array.from({ length: BOARD_WIDTH }, (_, x) => holes.includes(x) ? null : 'J');
}

test('elapsed time drives gravity at a fixed interval without drop points', () => {
  const game = withPiece();
  game.update(999);
  assert.equal(game.active.y, 0);
  game.update(1);
  assert.equal(game.active.y, 1);
  game.update(2500);
  assert.equal(game.active.y, 3);
  assert.equal(game.elapsed, 500);
  assert.equal(game.score, 0);
  assert.throws(() => game.update(-1), RangeError);
  assert.throws(() => game.update(Infinity), RangeError);
  assert.throws(() => new Game({ fallInterval: 0 }), RangeError);
});

test('gravity locks at the floor and spawns the next piece', () => {
  const game = withPiece();
  game.update(19000);
  assert.equal(game.board[19][4], 'O');
  assert.equal(game.board[18][5], 'O');
  assert.equal(game.active.y, 0);
  assert.equal(game.score, 0);
  assert.equal(game.lines, 0);
});

test('soft drop scores only successful steps and locks on blocked descent', () => {
  const game = withPiece();
  game.update(500);
  assert.equal(game.softDrop(), true);
  assert.equal(game.score, 1);
  assert.equal(game.elapsed, 0);
  while (game.move(0, 1)) { /* reach floor without scoring */ }
  assert.equal(game.softDrop(), false);
  assert.equal(game.score, 1);
  assert.equal(game.board[19][4], 'O');
  assert.equal(game.active.y, 0);
});

test('hard drop stops on a stack, scores distance, locks and spawns immediately', () => {
  const game = withPiece();
  game.board[19][4] = 'J';
  game.update(500);
  assert.equal(game.hardDrop(), 17);
  assert.equal(game.score, 34);
  assert.equal(game.elapsed, 0);
  assert.equal(game.board[17][4], 'O');
  assert.equal(game.board[18][5], 'O');
  assert.equal(game.board[19][4], 'J');
  assert.equal(game.active.y, 0);
});

test('zero-distance hard drop still locks and spawns without drop points', () => {
  const game = withPiece();
  while (game.move(0, 1)) { /* reach floor */ }
  assert.equal(game.hardDrop(), 0);
  assert.equal(game.score, 0);
  assert.equal(game.active.y, 0);
  assert.equal(game.board[19][5], 'O');
});

test('locking clears one through four lines with fixed-level scoring', () => {
  for (const [count, points] of [[1, 100], [2, 300], [3, 500], [4, 800]]) {
    const game = withPiece('I');
    game.rotate();
    const column = game.active.x + 2;
    for (let y = BOARD_HEIGHT - count; y < BOARD_HEIGHT; y++) fillExcept(game, y, [column]);
    game.board[10][0] = 'T';
    while (game.move(0, 1)) { /* gravity movement without drop points */ }
    assert.equal(game.lock(), true);
    assert.equal(game.score, points);
    assert.equal(game.lines, count);
    assert.equal(game.board.length, BOARD_HEIGHT);
    assert.equal(game.board[10 + count][0], 'T');
    assert.ok(game.board.slice(0, count).every(row => row.every(cell => cell === null)));
    game.board[0][0] = 'Z';
    assert.equal(game.board[1][0], null);
    assert.equal(game.clearLines(), 0);
    assert.equal(game.score, points);
  }
});

test('nonadjacent completed rows collapse in order and drop points add to line points', () => {
  const game = withPiece('I');
  game.rotate();
  const column = game.active.x + 2;
  fillExcept(game, 19, [column]);
  fillExcept(game, 17, [column]);
  game.board[18][0] = 'T';
  assert.equal(game.hardDrop(), 16);
  assert.equal(game.score, 332);
  assert.equal(game.lines, 2);
  assert.equal(game.board[19][0], 'T');
  assert.equal(game.board[19][column], 'I');
});

test('failed spawn sets game over and all progression and control actions are inert', () => {
  const game = withPiece();
  game.active = null;
  game.board[0][4] = 'J';
  assert.equal(game.spawn('O'), false);
  assert.equal(game.gameOver, true);
  assert.equal(game.active, null);
  const before = structuredClone({ board: game.board, bag: game.bag, score: game.score, lines: game.lines });
  assert.equal(game.spawn(), false);
  assert.equal(game.move(1), false);
  assert.equal(game.rotate(), false);
  assert.equal(game.lock(), false);
  assert.equal(game.tick(), false);
  assert.equal(game.softDrop(), false);
  assert.equal(game.hardDrop(), 0);
  game.update(100000);
  assert.deepEqual({ board: game.board, bag: game.bag, score: game.score, lines: game.lines }, before);
});

test('automatic locking detects blocked next spawn and stops accumulated gravity', () => {
  const game = withPiece();
  while (game.move(0, 1)) { /* reach floor */ }
  game.board[0].fill('J');
  game.board[0][0] = null; // Block all centered spawns without creating a completed line.
  game.board[1].fill('J');
  game.board[1][0] = null;
  game.update(100000);
  assert.equal(game.gameOver, true);
  assert.equal(game.active, null);
  assert.equal(game.elapsed, 0);
  assert.equal(game.board[19][4], 'O');
});
